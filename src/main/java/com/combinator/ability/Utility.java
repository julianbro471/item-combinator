package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

/** Utility: a quarry in a box, a sorting wand, mail between players, light for dark caves, and Almond Water. */
public final class Utility {
	/** The quarry keeps everything except these. */
	private static final Set<Item> JUNK = Set.of(Items.STONE, Items.COBBLESTONE, Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.DIRT,
			Items.GRASS_BLOCK, Items.GRAVEL, Items.SAND, Items.RED_SAND, Items.SANDSTONE, Items.RED_SANDSTONE, Items.TUFF, Items.GRANITE,
			Items.DIORITE, Items.ANDESITE, Items.CALCITE, Items.NETHERRACK, Items.BLACKSTONE, Items.BASALT, Items.SMOOTH_BASALT,
			Items.END_STONE, Items.FLINT, Items.SOUL_SAND, Items.SOUL_SOIL, Items.MUD, Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE,
			Items.MOSS_BLOCK, Items.TERRACOTTA);
	private static final int QUARRY_CHESTS = 6;

	private Utility() {
	}

	// ------------------------------------------------------------------ putting things into chests

	/** Puts as much of the stack as fits into the inventory: first onto stacks of the same item, then into empty slots. */
	static ItemStack insert(Inventory inventory, ItemStack stack) {
		for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
			for (int i = 0; i < inventory.size() && !stack.isEmpty(); i++) {
				ItemStack there = inventory.getStack(i);
				if (!inventory.isValid(i, stack)) {
					continue;
				}
				int limit = Math.min(inventory.getMaxCountPerStack(), stack.getMaxCount());
				if (pass == 0 && !there.isEmpty() && ItemStack.areItemsAndComponentsEqual(there, stack) && there.getCount() < limit) {
					int moved = Math.min(limit - there.getCount(), stack.getCount());
					there.increment(moved);
					stack.decrement(moved);
				} else if (pass == 1 && there.isEmpty()) {
					int moved = Math.min(limit, stack.getCount());
					inventory.setStack(i, stack.split(moved));
				}
			}
		}
		inventory.markDirty();
		return stack;
	}

	/** Merges equal stacks and orders them by item. Returns the new list, which is never longer than the old one. */
	private static List<ItemStack> sorted(List<ItemStack> stacks) {
		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack rest = stack.copy();
			for (ItemStack into : merged) {
				if (rest.isEmpty()) {
					break;
				}
				if (ItemStack.areItemsAndComponentsEqual(into, rest) && into.getCount() < into.getMaxCount()) {
					int moved = Math.min(into.getMaxCount() - into.getCount(), rest.getCount());
					into.increment(moved);
					rest.decrement(moved);
				}
			}
			if (!rest.isEmpty()) {
				merged.add(rest);
			}
		}
		merged.sort(Comparator.<ItemStack>comparingInt(s -> Registries.ITEM.getRawId(s.getItem())).thenComparing(s -> -s.getCount()));
		return merged;
	}

	private static void sortSlots(Inventory inventory, int from, int to) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = from; i < to; i++) {
			if (!inventory.getStack(i).isEmpty()) {
				stacks.add(inventory.getStack(i));
			}
		}
		List<ItemStack> result = sorted(stacks);
		for (int i = from; i < to; i++) {
			int k = i - from;
			inventory.setStack(i, k < result.size() ? result.get(k) : ItemStack.EMPTY);
		}
		inventory.markDirty();
	}

	// ------------------------------------------------------------------ Sorting Wand

	/**
	 * Sneak + right-click a chest: everything you carry (not the hotbar) that the chest already holds goes into it, then the chest
	 * is sorted. Without sneaking a right-click opens the chest as usual, so this only works when sneaking.
	 */
	public static ActionResult sortBlock(ItemUsageContext context, PlayerEntity player, ItemStack wand) {
		World world = context.getWorld();
		if (!player.isSneaking()) {
			return ActionResult.PASS;
		}
		Inventory inventory = HopperBlockEntity.getInventoryAt(world, context.getBlockPos());
		if (inventory == null) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (inventory.size() < 9) {
			player.sendMessage(Text.literal("Only chests, barrels and shulker boxes can be sorted."), true);
			return ActionResult.FAIL;
		}
		int moved = quickStack(player.getInventory(), inventory);
		sortSlots(inventory, 0, inventory.size());
		player.sendMessage(Text.literal("Sorted." + (moved > 0 ? " " + moved + " items put away." : "")).formatted(Formatting.GREEN), true);
		Fx.sound(world, Vec3d.ofCenter(context.getBlockPos()), SoundEvents.BLOCK_BARREL_CLOSE, 1.0F, 1.4F);
		if (world instanceof ServerWorld server) {
			Fx.particles(server, ParticleTypes.ENCHANT, Vec3d.ofCenter(context.getBlockPos()).add(0.0, 0.8, 0.0), 30, 0.4, 0.3);
		}
		return ActionResult.SUCCESS;
	}

	/** Moves every item of the player's main inventory (not the hotbar) that the chest already holds into the chest. */
	private static int quickStack(PlayerInventory from, Inventory to) {
		int moved = 0;
		for (int i = 9; i < PlayerInventory.MAIN_SIZE; i++) {
			ItemStack stack = from.getStack(i);
			if (stack.isEmpty() || !holds(to, stack)) {
				continue;
			}
			int before = stack.getCount();
			ItemStack rest = insert(to, stack);
			moved += before - rest.getCount();
			from.setStack(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
		}
		return moved;
	}

	private static boolean holds(Inventory inventory, ItemStack stack) {
		for (int i = 0; i < inventory.size(); i++) {
			if (ItemStack.areItemsAndComponentsEqual(inventory.getStack(i), stack)) {
				return true;
			}
		}
		return false;
	}

	/** Sorting Wand in the air: sorts your own inventory. The hotbar stays as it is. */
	static boolean sortSelf(ServerPlayerEntity player) {
		sortSlots(player.getInventory(), 9, PlayerInventory.MAIN_SIZE);
		player.playerScreenHandler.sendContentUpdates();
		player.sendMessage(Text.literal("Your inventory is sorted.").formatted(Formatting.GREEN), true);
		Fx.sound(player.getWorld(), player.getPos(), SoundEvents.ITEM_BUNDLE_INSERT, 1.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Quarry in a Box

	/** The spot where a quarry keeps its first chest. The tests look there. */
	public static BlockPos quarryChest(BlockPos center, int radius) {
		return center.add(-radius - 2, 1, 0);
	}

	/** Quarry in a Box: digs a square pit down to the bedrock, layer by layer. Everything worth keeping goes into chests. */
	static boolean quarry(ServerWorld world, ServerPlayerEntity player, int range, int radius) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.literal("Aim at the ground."), true);
			return false;
		}
		BlockPos center = hit.getBlockPos();
		Quarry quarry = new Quarry(world, center, radius, player.getUuid());
		quarry.chest(0);
		quarry.layer(center.getY());
		Tasks.later(1, () -> quarry.run(center.getY() - 1));
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.BLOCK_PISTON_EXTEND, 1.0F, 0.6F);
		player.sendMessage(Text.literal("The quarry starts digging.").formatted(Formatting.GOLD), true);
		return true;
	}

	private static final class Quarry {
		private final ServerWorld world;
		private final BlockPos center;
		private final int radius;
		private final UUID owner;
		private final ItemStack pick = new ItemStack(Items.NETHERITE_PICKAXE);
		private int chests = 1;
		private int kept = 0;

		Quarry(ServerWorld world, BlockPos center, int radius, UUID owner) {
			this.world = world;
			this.center = center;
			this.radius = radius;
			this.owner = owner;
		}

		/** The n-th chest. It is put back if it was taken away. */
		Inventory chest(int n) {
			BlockPos pos = quarryChest(this.center, this.radius).add(0, 0, n);
			if (!this.world.getBlockState(pos).isOf(Blocks.CHEST)) {
				this.world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(net.minecraft.block.ChestBlock.FACING, Direction.EAST));
			}
			return this.world.getBlockEntity(pos) instanceof Inventory inventory ? inventory : null;
		}

		void run(int y) {
			try {
				int bottom = this.world.getBottomY() + 1;
				for (int step = 0; step < 2 && y >= bottom; step++, y--) {
					this.layer(y);
				}
				if (y >= bottom) {
					int next = y;
					Tasks.later(1, () -> this.run(next));
				} else {
					this.finish(bottom);
				}
			} catch (Throwable t) {
				ItemCombinator.error("Quarry failed", t);
			}
		}

		void layer(int y) {
			PlayerEntity player = this.world.getServer().getPlayerManager().getPlayer(this.owner);
			for (int dx = -this.radius; dx <= this.radius; dx++) {
				for (int dz = -this.radius; dz <= this.radius; dz++) {
					BlockPos pos = new BlockPos(this.center.getX() + dx, y, this.center.getZ() + dz);
					BlockState state = this.world.getBlockState(pos);
					if (state.isAir() || state.getHardness(this.world, pos) < 0.0F) {
						continue;
					}
					if (!(state.getBlock() instanceof FluidBlock)) { // water and lava just go away
						BlockEntity entity = this.world.getBlockEntity(pos);
						List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(state, this.world, pos, entity, player, this.pick));
						if (entity instanceof Inventory inside) {
							for (int i = 0; i < inside.size(); i++) {
								drops.add(inside.removeStack(i));
							}
						}
						for (ItemStack drop : drops) {
							if (!drop.isEmpty() && !JUNK.contains(drop.getItem())) {
								this.keep(drop);
							}
						}
					}
					this.world.setBlockState(pos, Fx.AIR, Block.NOTIFY_LISTENERS);
				}
			}
			Vec3d middle = Vec3d.ofCenter(new BlockPos(this.center.getX(), y, this.center.getZ()));
			Fx.particles(this.world, ParticleTypes.CLOUD, middle, 12, this.radius * 0.4, 0.02);
			if (y % 3 == 0) {
				Fx.sound(this.world, middle, SoundEvents.BLOCK_STONE_BREAK, 1.0F, 0.6F);
			}
		}

		private void keep(ItemStack stack) {
			int count = stack.getCount();
			for (int n = 0; n < QUARRY_CHESTS && !stack.isEmpty(); n++) {
				if (n >= this.chests) {
					this.chests = n + 1;
				}
				Inventory chest = this.chest(n);
				if (chest != null) {
					stack = insert(chest, stack);
				}
			}
			if (!stack.isEmpty()) {
				BlockPos at = quarryChest(this.center, this.radius).up();
				Block.dropStack(this.world, at, stack);
			}
			this.kept += count;
		}

		/** A ladder down one wall of the pit, and a word to the owner. */
		private void finish(int bottom) {
			int x = this.center.getX();
			int z = this.center.getZ() - this.radius;
			BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
			for (int y = bottom; y <= this.center.getY(); y++) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockPos wall = pos.north();
				if (this.world.getBlockState(pos).isAir() && this.world.getBlockState(wall).isSideSolidFullSquare(this.world, wall, Direction.SOUTH)) {
					this.world.setBlockState(pos, ladder);
				}
			}
			PlayerEntity player = this.world.getServer().getPlayerManager().getPlayer(this.owner);
			if (player != null) {
				player.sendMessage(Text.literal("The quarry is done. " + this.kept + " items are in the chests.").formatted(Formatting.GOLD), false);
			}
		}
	}

	// ------------------------------------------------------------------ Ender Mail

	/**
	 * Ender Mail: the stack in the other hand flies to another player, in any world. If the mail was renamed on an anvil to a
	 * player's name, it goes to that player. Otherwise to the nearest one.
	 */
	static boolean mail(ServerWorld world, ServerPlayerEntity player, Hand hand) {
		ItemStack mail = player.getStackInHand(hand);
		Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack parcel = player.getStackInHand(otherHand);
		if (parcel.isEmpty()) {
			player.sendMessage(Text.literal("Hold what you want to send in your other hand."), true);
			return false;
		}
		ServerPlayerEntity to;
		if (mail.contains(DataComponentTypes.CUSTOM_NAME)) {
			String name = mail.getName().getString();
			to = world.getServer().getPlayerManager().getPlayer(name);
			if (to == null || to == player) {
				player.sendMessage(Text.literal("Nobody called " + name + " is here."), true);
				return false;
			}
		} else {
			to = Travel.nearestPlayer(player);
			if (to == null) {
				player.sendMessage(Text.literal("There is nobody to send it to."), true);
				return false;
			}
		}
		ItemStack sent = parcel.copy();
		player.setStackInHand(otherHand, ItemStack.EMPTY);
		Fx.particles(world, ParticleTypes.PORTAL, player.getEyePos(), 40, 0.4, 0.4);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_ENDER_EYE_DEATH, 1.0F, 1.2F);
		to.getInventory().offerOrDrop(sent);
		if (to.getWorld() instanceof ServerWorld there) {
			Fx.particles(there, ParticleTypes.PORTAL, to.getEyePos(), 40, 0.4, 0.4);
			Fx.sound(there, to.getPos(), SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 0.8F);
		}
		to.sendMessage(Text.literal(player.getName().getString() + " sent you " + sent.getCount() + " " + sent.getName().getString() + ".")
				.formatted(Formatting.LIGHT_PURPLE), false);
		player.sendMessage(Text.literal("Sent to " + to.getName().getString() + ".").formatted(Formatting.LIGHT_PURPLE), true);
		return true;
	}

	// ------------------------------------------------------------------ Lumen Orb

	/** Lumen Orb: an invisible light on every dark floor within the radius. Monsters cannot spawn there anymore. */
	static boolean lightUp(ServerWorld world, ServerPlayerEntity player, int radius) {
		BlockPos c = player.getBlockPos();
		BlockState light = Blocks.LIGHT.getDefaultState();
		int placed = 0;
		for (int dx = -radius; dx <= radius; dx += 5) {
			for (int dz = -radius; dz <= radius; dz += 5) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				for (int dy = -12; dy <= 12; dy++) {
					BlockPos pos = c.add(dx, dy, dz);
					if (!world.isInBuildLimit(pos) || !world.getBlockState(pos).isAir()
							|| !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
							|| world.getLightLevel(LightType.BLOCK, pos) >= 8) {
						continue;
					}
					world.setBlockState(pos, light);
					world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.01);
					placed++;
				}
			}
		}
		if (placed == 0) {
			player.sendMessage(Text.literal("It is bright enough here."), true);
			return false;
		}
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0F, 1.5F);
		player.sendMessage(Text.literal(placed + " lights hung up.").formatted(Formatting.YELLOW), true);
		return true;
	}

	// ------------------------------------------------------------------ Almond Water

	/** Almond Water: every bad effect goes away, and the Smilers nearby forget you. */
	public static void cleanse(ServerPlayerEntity player) {
		List<StatusEffectInstance> bad = new ArrayList<>();
		for (StatusEffectInstance effect : player.getStatusEffects()) {
			if (effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
				bad.add(effect);
			}
		}
		for (StatusEffectInstance effect : bad) {
			player.removeStatusEffect(effect.getEffectType());
		}
		for (EndermanEntity enderman : player.getWorld().getEntitiesByClass(EndermanEntity.class, player.getBoundingBox().expand(32.0),
				e -> e.getTarget() == player)) {
			enderman.setTarget(null);
		}
	}
}
