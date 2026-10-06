package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Mining abilities: area mining, vein mining, tree felling, auto-smelting, double ores and
 * sending drops straight to the inventory.
 *
 * How it works: when a player is about to break a block with a combined tool, this class
 * cancels the normal break and does the break itself. That way it knows exactly which item
 * drops belong to which block and is able to change them.
 */
public final class MiningAbilities {
	/** data/combinator/tags/block/ores.json */
	static final TagKey<Block> ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("combinator", "ores"));
	private static final TagKey<Block> STONE = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("base_stone_overworld"));
	private static final TagKey<Block> NETHER_STONE = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("base_stone_nether"));
	private static final RegistryKey<Enchantment> SILK_TOUCH = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("silk_touch"));

	/** What the Sifting Shovel finds in dirt, sand and gravel, with how often (bigger number = more often). */
	private static final Object[][] SIFT_FINDS = {
			{Items.FLINT, 60}, {Items.IRON_NUGGET, 60}, {Items.CLAY_BALL, 45}, {Items.GOLD_NUGGET, 36}, {Items.BONE, 30},
			{Items.WHEAT_SEEDS, 30}, {Items.COAL, 24}, {Items.RAW_COPPER, 18}, {Items.STRING, 15}, {Items.RAW_IRON, 12},
			{Items.EMERALD, 1}, {Items.DIAMOND, 1}
	};
	private static final TagKey<Block> LOGS = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("logs"));
	private static final TagKey<Block> LEAVES = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("leaves"));
	private static final TagKey<Block> WART_BLOCKS = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("wart_blocks"));

	/** True while this class is breaking blocks itself, so its own breaks do not start a chain reaction. */
	private static boolean busy = false;

	private MiningAbilities() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register(MiningAbilities::beforeBreak);
	}

	/** Returning false cancels the normal block break. */
	private static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (busy || world.isClient || !(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
			return true;
		}
		ItemStack tool = serverPlayer.getMainHandStack();
		Traits traits = ComboItems.traits(tool.getItem());
		if (traits == null || !traits.hasMining() || serverPlayer.isCreative()) {
			return true;
		}

		Item toolItem = tool.getItem();
		boolean originBroken = false;
		// Silk tools carry the Silk Touch enchantment for the moment of the break, so the game itself decides what drops.
		ItemEnchantmentsComponent enchantmentsBefore = null;
		if (traits.silk) {
			Optional<RegistryEntry.Reference<Enchantment>> silkTouch = serverWorld.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(SILK_TOUCH);
			if (silkTouch.isPresent() && tool.getEnchantments().getLevel(silkTouch.get()) == 0) {
				enchantmentsBefore = tool.getEnchantments();
				tool.addEnchantment(silkTouch.get(), 1);
			}
		}
		busy = true;
		try {
			List<BlockPos> extra = serverPlayer.isSneaking()
					? List.of()
					: collectExtra(serverWorld, serverPlayer, pos.toImmutable(), state, traits, tool);

			if (!serverPlayer.interactionManager.tryBreakBlock(pos)) {
				return false;
			}
			originBroken = true;
			processDrops(serverWorld, serverPlayer, pos, state, traits);
			if (traits.mineBoom > 0 && !serverPlayer.isSneaking()) {
				// The miner is immune and dropped items survive, so this is safe to stand next to.
				Fx.boom(serverWorld, serverPlayer, Vec3d.ofCenter(pos), traits.mineBoom, false, true);
			}

			for (BlockPos target : extra) {
				ItemStack current = serverPlayer.getMainHandStack();
				if (current.isEmpty() || current.getItem() != toolItem) {
					break; // the tool broke
				}
				BlockState targetState = serverWorld.getBlockState(target);
				if (targetState.isAir()) {
					continue;
				}
				if (serverPlayer.interactionManager.tryBreakBlock(target)) {
					processDrops(serverWorld, serverPlayer, target, targetState, traits);
				}
			}
		} catch (Throwable t) {
			ItemCombinator.error("Mining ability failed", t);
			if (!originBroken) {
				return true;
			}
		} finally {
			busy = false;
			if (enchantmentsBefore != null) {
				tool.set(DataComponentTypes.ENCHANTMENTS, enchantmentsBefore);
			}
		}
		return false;
	}

	/** The additional blocks to break. The block the player mined is not part of this list. */
	private static List<BlockPos> collectExtra(ServerWorld world, ServerPlayerEntity player, BlockPos origin, BlockState state, Traits traits, ItemStack tool) {
		List<BlockPos> result = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(origin);

		if (traits.veinLimit > 0 && state.isIn(ORES)) {
			Block block = state.getBlock();
			flood(world, origin, traits.veinLimit, seen, result, s -> s.getBlock() == block);
		}

		if (traits.treeLimit > 0 && state.isIn(LOGS)) {
			List<BlockPos> logs = new ArrayList<>();
			Set<BlockPos> logSeen = new HashSet<>();
			logSeen.add(origin);
			flood(world, origin, traits.treeLimit, logSeen, logs, s -> s.isIn(LOGS));
			logs.add(origin);
			if (isNaturalTree(world, logs)) {
				for (BlockPos log : logs) {
					if (seen.add(log)) {
						result.add(log);
					}
				}
			}
		}

		if (traits.areaRadius > 0 && tool.isSuitableFor(state)) {
			float originHardness = state.getHardness(world, origin);
			float limit = Math.max(originHardness * 4.0F, 4.0F);
			int r = traits.areaRadius;
			// Which axis the flat (non-cube) area is thin along: the one the player looks along.
			Direction.Axis thin;
			if (Math.abs(player.getPitch()) > 50.0F) {
				thin = Direction.Axis.Y;
			} else {
				thin = player.getHorizontalFacing().getAxis();
			}
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						if (!traits.areaCube) {
							if ((thin == Direction.Axis.X && dx != 0) || (thin == Direction.Axis.Y && dy != 0) || (thin == Direction.Axis.Z && dz != 0)) {
								continue;
							}
						}
						BlockPos target = origin.add(dx, dy, dz);
						if (seen.contains(target)) {
							continue;
						}
						BlockState targetState = world.getBlockState(target);
						if (targetState.isAir() || targetState.hasBlockEntity() || !tool.isSuitableFor(targetState)) {
							continue;
						}
						float hardness = targetState.getHardness(world, target);
						if (hardness < 0.0F || hardness > limit) {
							continue; // unbreakable, or much harder than the block that was mined
						}
						seen.add(target);
						result.add(target);
					}
				}
			}
		}
		return result;
	}

	/** Finds connected blocks (including diagonals) that match the test, starting next to the origin. */
	private static void flood(ServerWorld world, BlockPos origin, int limit, Set<BlockPos> seen, List<BlockPos> out, java.util.function.Predicate<BlockState> test) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		while (!queue.isEmpty() && out.size() < limit) {
			BlockPos current = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.add(dx, dy, dz);
						if (out.size() >= limit || seen.contains(next)) {
							continue;
						}
						if (test.test(world.getBlockState(next))) {
							seen.add(next);
							out.add(next);
							queue.add(next);
						}
					}
				}
			}
		}
	}

	/** A tree counts as natural if any of its logs touches leaves that were not placed by a player. */
	private static boolean isNaturalTree(ServerWorld world, List<BlockPos> logs) {
		for (BlockPos log : logs) {
			for (Direction direction : Direction.values()) {
				BlockState neighbor = world.getBlockState(log.offset(direction));
				if (neighbor.isIn(WART_BLOCKS)) {
					return true;
				}
				if (neighbor.isIn(LEAVES) && (!neighbor.contains(LeavesBlock.PERSISTENT) || !neighbor.get(LeavesBlock.PERSISTENT))) {
					return true;
				}
			}
		}
		return false;
	}

	/** Changes the item drops of a block that was just broken at pos. */
	private static void processDrops(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState brokenState, Traits traits) {
		// Extra finds come first, so a magnet tool picks them up like the normal drops.
		if (traits.sift && isSiftable(brokenState) && world.random.nextFloat() < 0.25F) {
			Block.dropStack(world, pos, siftFind(world));
		}
		if (traits.stoneNuggets > 0 && (brokenState.isIn(STONE) || brokenState.isIn(NETHER_STONE)) && world.random.nextFloat() < traits.stoneNuggets) {
			Block.dropStack(world, pos, new ItemStack(Items.GOLD_NUGGET, 1 + world.random.nextInt(2)));
		}
		if (!traits.smelt && !traits.magnetDrops && !traits.doubleOres) {
			return;
		}
		Box box = new Box(pos);
		List<ItemEntity> drops = world.getEntitiesByClass(ItemEntity.class, box,
				entity -> entity.isAlive() && entity.age == 0 && entity.getBlockPos().equals(pos));
		Item ownItem = brokenState.getBlock().asItem();
		boolean ore = brokenState.isIn(ORES);

		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getStack().copy();
			if (traits.doubleOres && ore && !stack.isOf(ownItem)) {
				stack.setCount(Math.min(stack.getMaxCount(), stack.getCount() * 2));
			}
			if (traits.smelt) {
				ItemStack smelted = smelt(world, stack);
				if (!smelted.isEmpty()) {
					stack = smelted;
				}
			}
			if (traits.magnetDrops) {
				player.getInventory().insertStack(stack);
				if (stack.isEmpty()) {
					drop.discard();
					continue;
				}
			}
			drop.setStack(stack);
		}

		if (traits.magnetDrops) {
			List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, box.expand(0.5),
					entity -> entity.isAlive() && entity.age == 0);
			for (ExperienceOrbEntity orb : orbs) {
				orb.setPosition(player.getX(), player.getY() + 0.5, player.getZ());
			}
		}
	}

	private static boolean isSiftable(BlockState state) {
		return state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND) || state.isOf(Blocks.DIRT)
				|| state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.ROOTED_DIRT)
				|| state.isOf(Blocks.MUD) || state.isOf(Blocks.SOUL_SAND);
	}

	private static ItemStack siftFind(ServerWorld world) {
		int total = 0;
		for (Object[] find : SIFT_FINDS) {
			total += (Integer) find[1];
		}
		int roll = world.random.nextInt(total);
		for (Object[] find : SIFT_FINDS) {
			roll -= (Integer) find[1];
			if (roll < 0) {
				return new ItemStack((Item) find[0]);
			}
		}
		return new ItemStack(Items.FLINT);
	}

	/** The furnace result for a stack, or an empty stack if it cannot be smelted. */
	static ItemStack smelt(ServerWorld world, ItemStack input) {
		Optional<RecipeEntry<SmeltingRecipe>> recipe = world.getRecipeManager()
				.getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world);
		if (recipe.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = recipe.get().value().getResult(world.getRegistryManager());
		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}
		int count = Math.min(result.getMaxCount(), input.getCount() * result.getCount());
		return result.copyWithCount(count);
	}
}
