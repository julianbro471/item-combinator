package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.screen.BackpackScreenHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Right-click abilities of the everyday items: things made from wood, stone, string, buckets and the like.
 * Every method returns true if the ability did something (so the item's cooldown starts).
 */
public final class Gadgets {
	/** Marks the glowing ore outlines of the Divining Rod, so they can be found and removed again. */
	private static final String SIGHT_TAG = "combinator_ore_sight";
	private static final int SIGHT_TICKS = 200;
	/** Where the Waystone remembers its place inside the item. */
	private static final String WAYPOINT_KEY = "combinator_waypoint";
	private static final RegistryKey<LootTable> FISHING_LOOT = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.ofVanilla("gameplay/fishing"));

	private Gadgets() {
	}

	public static void register() {
		// Safety net: ore outlines that are still there after the game was closed and opened again are removed.
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getTime() % 100L != 0L) {
				return;
			}
			try {
				for (DisplayEntity.BlockDisplayEntity display : world.getEntitiesByType(
						TypeFilter.instanceOf(DisplayEntity.BlockDisplayEntity.class),
						entity -> entity.getCommandTags().contains(SIGHT_TAG) && entity.age > SIGHT_TICKS + 60)) {
					display.discard();
				}
			} catch (Throwable t) {
				ItemCombinator.error("Removing old ore outlines failed", t);
			}
		});
	}

	// ------------------------------------------------------------------ throwing and shooting

	/** Boomerang: hurts every mob on a straight line and brings back dropped items and XP that lie on it. */
	static boolean boomerang(ServerWorld world, ServerPlayerEntity player, int range, float damage) {
		Vec3d start = player.getEyePos();
		Vec3d end = Fx.ray(player, range).getPos();
		Box box = new Box(start, end).expand(1.5);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e.getBoundingBox().expand(0.5).raycast(start, end).isPresent())) {
			living.damage(world.getDamageSources().playerAttack(player), damage);
		}
		Vec3d home = player.getPos().add(0.0, 0.3, 0.0);
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box,
				e -> e.isAlive() && e.getBoundingBox().expand(1.0).raycast(start, end).isPresent())) {
			item.setPosition(home.x, home.y, home.z);
			item.setVelocity(Vec3d.ZERO);
			item.setPickupDelay(0);
		}
		for (ExperienceOrbEntity orb : world.getEntitiesByClass(ExperienceOrbEntity.class, box,
				e -> e.isAlive() && e.getBoundingBox().expand(1.0).raycast(start, end).isPresent())) {
			orb.setPosition(home.x, home.y, home.z);
		}
		Fx.line(world, ParticleTypes.CRIT, start.add(0.0, -0.2, 0.0), end);
		Fx.sound(world, start, SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 0.6F);
		return true;
	}

	/** The torch for a block face, or null if no torch can go there. side is the face that was hit. */
	static BlockState torchFor(World world, BlockPos hitBlock, Direction side) {
		if (side == Direction.DOWN) {
			return null;
		}
		BlockPos place = hitBlock.offset(side);
		BlockState torch = side == Direction.UP
				? Blocks.TORCH.getDefaultState()
				: Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, side);
		if (!world.getBlockState(place).isAir() || !torch.canPlaceAt(world, place)) {
			return null;
		}
		return torch;
	}

	/** Torch Bow: puts a torch on the block the player looks at, also far away. */
	static boolean torchShot(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockState torch = torchFor(world, hit.getBlockPos(), hit.getSide());
		if (torch == null) {
			return false;
		}
		BlockPos place = hit.getBlockPos().offset(hit.getSide());
		world.setBlockState(place, torch);
		Fx.line(world, ParticleTypes.FLAME, player.getEyePos().add(0.0, -0.2, 0.0), Vec3d.ofCenter(place));
		Fx.sound(world, player.getEyePos(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
		Fx.sound(world, Vec3d.ofCenter(place), SoundEvents.BLOCK_WOOD_PLACE, 1.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ ropes and hooks

	/** Grappling Hook: flings the player towards the block they look at. */
	static boolean grapple(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		Vec3d to = hit.getPos().subtract(player.getEyePos());
		double distance = to.length();
		if (distance < 1.5) {
			return false;
		}
		double speed = Math.min(3.2, 0.6 + distance * 0.12);
		Vec3d pull = to.normalize().multiply(speed).add(0.0, 0.3, 0.0);
		player.setVelocity(pull);
		player.velocityModified = true;
		player.fallDistance = 0.0F;
		Fx.line(world, ParticleTypes.CRIT, player.getEyePos().add(0.0, -0.3, 0.0), hit.getPos());
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1.0F, 0.6F);
		return true;
	}

	/** Lasso: pulls the mob the player looks at over to the player. */
	static boolean yank(ServerWorld world, ServerPlayerEntity player, int range) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null || target instanceof PlayerEntity) {
			return false;
		}
		Vec3d to = player.getPos().subtract(target.getPos());
		double distance = to.length();
		double speed = Math.min(3.0, 0.4 + distance * 0.15);
		target.setVelocity(to.normalize().multiply(speed).add(0.0, 0.35, 0.0));
		target.velocityModified = true;
		target.fallDistance = 0.0F;
		Fx.line(world, ParticleTypes.CRIT, player.getEyePos().add(0.0, -0.3, 0.0), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0));
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 0.8F);
		return true;
	}

	/** Angler's Rod: one catch from the game's own fishing loot, at once, when the player looks at water. */
	static boolean fish(ServerWorld world, ServerPlayerEntity player, ItemStack rod, int range) {
		BlockHitResult hit = Fx.rayWithFluids(player, range);
		if (hit.getType() != HitResult.Type.BLOCK || !world.getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
			return false;
		}
		LootTable table = world.getServer().getReloadableRegistries().getLootTable(FISHING_LOOT);
		LootContextParameterSet parameters = new LootContextParameterSet.Builder(world)
				.add(LootContextParameters.ORIGIN, hit.getPos())
				.add(LootContextParameters.TOOL, rod)
				.add(LootContextParameters.THIS_ENTITY, player)
				.luck(player.getLuck())
				.build(LootContextTypes.FISHING);
		List<ItemStack> loot = table.generateLoot(parameters);
		if (loot.isEmpty()) {
			return false;
		}
		for (ItemStack stack : loot) {
			player.getInventory().offerOrDrop(stack);
		}
		ExperienceOrbEntity.spawn(world, player.getPos(), 1 + world.random.nextInt(3));
		Fx.particles(world, ParticleTypes.SPLASH, hit.getPos().add(0.0, 0.2, 0.0), 30, 0.3, 0.1);
		Fx.sound(world, hit.getPos(), SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, 0.6F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ camp and travel

	/** Backpack: its own chest window. The things in it are stored inside the item. */
	static boolean backpack(ServerPlayerEntity player, Hand hand, int rows) {
		ItemStack bag = player.getStackInHand(hand);
		if (bag.getCount() != 1) {
			return false;
		}
		BackpackScreenHandler.open(player, bag, rows >= 6 ? 6 : 3);
		Fx.sound(player.getWorld(), player.getPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 0.8F, 1.0F);
		return true;
	}

	/** Pocket Furnace: smelts up to 8 items of the stack in the other hand. */
	static boolean smeltHand(ServerWorld world, ServerPlayerEntity player, Hand furnaceHand) {
		Hand other = furnaceHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack input = player.getStackInHand(other);
		if (input.isEmpty()) {
			player.sendMessage(Text.literal("Hold the item to smelt in your other hand."), true);
			return false;
		}
		int amount = Math.min(8, input.getCount());
		ItemStack result = MiningAbilities.smelt(world, input.copyWithCount(amount));
		if (result.isEmpty()) {
			player.sendMessage(Text.literal("This cannot be smelted."), true);
			return false;
		}
		input.decrement(amount);
		player.getInventory().offerOrDrop(result);
		Fx.particles(world, ParticleTypes.FLAME, player.getEyePos().add(player.getRotationVec(1.0F).multiply(0.6)), 12, 0.15, 0.01);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, 1.0F, 1.0F);
		return true;
	}

	/** Bedroll: the player respawns right here from now on. No bed needed. */
	static boolean setSpawn(ServerWorld world, ServerPlayerEntity player) {
		player.setSpawnPoint(world.getRegistryKey(), player.getBlockPos(), player.getYaw(), true, false);
		player.sendMessage(Text.literal("You will wake up here.").formatted(Formatting.AQUA), true);
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, player.getPos().add(0.0, 1.0, 0.0), 12, 0.5, 0.0);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_WOOL_PLACE, 1.0F, 0.8F);
		return true;
	}

	/** Waystone, sneaking: remembers this spot inside the item. */
	static boolean waypointSet(ServerWorld world, ServerPlayerEntity player, ItemStack stone) {
		NbtCompound spot = new NbtCompound();
		spot.putDouble("x", player.getX());
		spot.putDouble("y", player.getY());
		spot.putDouble("z", player.getZ());
		spot.putFloat("yaw", player.getYaw());
		spot.putString("world", world.getRegistryKey().getValue().toString());
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stone, nbt -> nbt.put(WAYPOINT_KEY, spot));
		stone.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		BlockPos pos = player.getBlockPos();
		player.sendMessage(Text.literal("The Waystone remembers " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ".").formatted(Formatting.AQUA), true);
		Fx.particles(world, ParticleTypes.ENCHANT, player.getPos().add(0.0, 1.0, 0.0), 40, 0.5, 0.5);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
		return true;
	}

	/** Waystone: brings the player back to the remembered spot, also from another dimension. */
	static boolean waypointGo(ServerWorld world, ServerPlayerEntity player, ItemStack stone) {
		NbtComponent data = stone.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		if (!data.contains(WAYPOINT_KEY)) {
			player.sendMessage(Text.literal("Sneak and right-click first to remember a spot."), true);
			return false;
		}
		NbtCompound spot = data.copyNbt().getCompound(WAYPOINT_KEY);
		ServerWorld destination = world.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(spot.getString("world"))));
		if (destination == null) {
			player.sendMessage(Text.literal("The remembered place no longer exists."), true);
			return false;
		}
		Fx.particles(world, ParticleTypes.PORTAL, player.getPos().add(0.0, 1.0, 0.0), 30, 0.4, 0.2);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.9F);
		player.teleport(destination, spot.getDouble("x"), spot.getDouble("y"), spot.getDouble("z"), spot.getFloat("yaw"), player.getPitch());
		player.fallDistance = 0.0F;
		Fx.sound(destination, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.9F);
		return true;
	}

	/** Smoke Bomb: the player turns invisible and every mob nearby loses track of them. */
	static boolean smoke(ServerWorld world, ServerPlayerEntity player) {
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, true));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 120, 1, false, false, true));
		for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(14.0), e -> e.isAlive())) {
			if (mob.getTarget() == player) {
				mob.setTarget(null);
				mob.getNavigation().stop();
			}
		}
		Vec3d at = player.getPos().add(0.0, 1.0, 0.0);
		Fx.particles(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, at, 60, 1.2, 0.01);
		Fx.particles(world, ParticleTypes.LARGE_SMOKE, at, 120, 1.5, 0.02);
		Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.2F, 0.7F);
		return true;
	}

	// ------------------------------------------------------------------ animals and helpers

	/** Hedge Shears: shears every sheep (and anything else that can be sheared) nearby, and cuts leaves and grass loose. */
	static boolean shearArea(ServerWorld world, ServerPlayerEntity player, int range) {
		int done = 0;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range), e -> e.isAlive())) {
			if (living instanceof Shearable shearable && shearable.isShearable()) {
				shearable.sheared(SoundCategory.PLAYERS);
				done++;
			}
		}
		BlockPos center = player.getBlockPos();
		for (BlockPos pos : BlockPos.iterate(center.add(-3, -1, -3), center.add(3, 4, 3))) {
			BlockState state = world.getBlockState(pos);
			if (state.isIn(BlockTags.LEAVES) || state.isOf(Blocks.VINE) || state.isOf(Blocks.SHORT_GRASS) || state.isOf(Blocks.FERN)
					|| state.isOf(Blocks.DEAD_BUSH) || state.isOf(Blocks.COBWEB)) {
				BlockPos fixed = pos.toImmutable();
				world.setBlockState(fixed, Fx.AIR, Block.NOTIFY_ALL);
				Block.dropStack(world, fixed, new ItemStack(state.getBlock()));
				done++;
			}
		}
		if (done == 0) {
			return false;
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_SHEEP_SHEAR, 1.0F, 1.0F);
		return true;
	}

	/** Wolf Whistle: tame wolves that belong to the player. */
	static boolean summonWolves(ServerWorld world, ServerPlayerEntity player, int count) {
		Random random = world.random;
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			Entity entity = Spells.spawn(world, EntityType.WOLF,
					player.getX() + (random.nextDouble() - 0.5) * 3.0, player.getY() + 0.2, player.getZ() + (random.nextDouble() - 0.5) * 3.0);
			if (entity instanceof WolfEntity wolf) {
				wolf.setOwner(player);
				wolf.setHealth(wolf.getMaxHealth());
				spawned++;
			}
		}
		Fx.particles(world, ParticleTypes.HEART, player.getPos().add(0.0, 1.0, 0.0), 6, 0.8, 0.0);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_WOLF_HOWL, 1.0F, 1.0F);
		return spawned > 0;
	}

	/** Steed Horn: a tame, saddled, fast horse next to the player. */
	static boolean summonHorse(ServerWorld world, ServerPlayerEntity player) {
		Entity entity = Spells.spawn(world, EntityType.HORSE, player.getX() + 1.0, player.getY() + 0.2, player.getZ() + 1.0);
		if (!(entity instanceof HorseEntity horse)) {
			return false;
		}
		horse.setTame(true);
		horse.setOwnerUuid(player.getUuid());
		horse.saddle(new ItemStack(Items.SADDLE), null);
		setBase(horse, "generic.movement_speed", 0.3);
		setBase(horse, "generic.jump_strength", 0.9);
		setBase(horse, "generic.max_health", 30.0);
		horse.setHealth(horse.getMaxHealth());
		Fx.particles(world, ParticleTypes.POOF, horse.getPos().add(0.0, 1.0, 0.0), 30, 0.6, 0.02);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_HORSE_AMBIENT, 1.2F, 1.0F);
		return true;
	}

	private static void setBase(LivingEntity entity, String attributeId, double value) {
		RegistryEntry<EntityAttribute> attribute = Fx.attribute(attributeId);
		if (attribute == null) {
			return;
		}
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/** Trader's Token: a wandering trader shows up next to the player and stays for two game days. */
	static boolean summonTrader(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Entity entity = Spells.spawn(world, EntityType.WANDERING_TRADER, player.getX() + look.x * 2.0, player.getY() + 0.2, player.getZ() + look.z * 2.0);
		if (!(entity instanceof WanderingTraderEntity trader)) {
			return false;
		}
		trader.setDespawnDelay(48000);
		Fx.particles(world, ParticleTypes.POOF, trader.getPos().add(0.0, 1.0, 0.0), 30, 0.5, 0.02);
		Fx.sound(world, trader.getPos(), SoundEvents.ENTITY_WANDERING_TRADER_REAPPEARED, 1.0F, 1.0F);
		return true;
	}

	/** Shepherd's Crook: farm animals nearby walk after the player for 20 seconds. */
	static boolean lure(ServerWorld world, ServerPlayerEntity player, int range) {
		List<AnimalEntity> herd = new ArrayList<>(world.getEntitiesByClass(AnimalEntity.class, player.getBoundingBox().expand(range), e -> e.isAlive()));
		if (herd.isEmpty()) {
			player.sendMessage(Text.literal("No animals nearby."), true);
			return false;
		}
		for (AnimalEntity animal : herd) {
			Fx.particles(world, ParticleTypes.NOTE, animal.getPos().add(0.0, animal.getHeight() + 0.3, 0.0), 1, 0.1, 0.0);
		}
		Tasks.repeat(40, 10, i -> {
			if (!player.isAlive() || player.getWorld() != world) {
				return;
			}
			for (AnimalEntity animal : herd) {
				if (animal.isAlive() && animal.getWorld() == world && animal.squaredDistanceTo(player) > 6.0) {
					animal.getNavigation().startMovingTo(player, 1.25);
				}
			}
		});
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE, 1.5F, 1.2F);
		return true;
	}

	// ------------------------------------------------------------------ finding things

	/** Divining Rod: every ore nearby gets a glowing outline that shows through walls for 10 seconds. */
	static boolean oreSight(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos center = player.getBlockPos();
		int minY = Math.max(world.getBottomY(), center.getY() - range);
		int maxY = Math.min(world.getTopY() - 1, center.getY() + range);
		List<Entity> outlines = new ArrayList<>();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		scan:
		for (int y = minY; y <= maxY; y++) {
			for (int dx = -range; dx <= range; dx++) {
				for (int dz = -range; dz <= range; dz++) {
					pos.set(center.getX() + dx, y, center.getZ() + dz);
					BlockState state = world.getBlockState(pos);
					if (!state.isIn(MiningAbilities.ORES)) {
						continue;
					}
					Entity outline = outline(world, pos.toImmutable(), state);
					if (outline == null) {
						break scan;
					}
					world.spawnEntity(outline);
					outlines.add(outline);
					if (outlines.size() >= 96) {
						break scan;
					}
				}
			}
		}
		if (outlines.isEmpty()) {
			player.sendMessage(Text.literal("No ore within " + range + " blocks."), true);
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.5F);
			return true;
		}
		player.sendMessage(Text.literal(outlines.size() + " ore blocks nearby.").formatted(Formatting.GOLD), true);
		Tasks.later(SIGHT_TICKS, () -> {
			for (Entity outline : outlines) {
				outline.discard();
			}
		});
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
		return true;
	}

	/**
	 * A "block display": a picture of a block that is not a real block. It glows, so it is seen through walls.
	 * It is described as data (the same form the /summon command uses) and then turned into an entity.
	 */
	private static Entity outline(ServerWorld world, BlockPos pos, BlockState state) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "minecraft:block_display");
		nbt.put("block_state", NbtHelper.fromBlockState(state));
		nbt.putInt("glow_color_override", oreColor(state));
		nbt.putBoolean("Glowing", true);
		NbtCompound light = new NbtCompound();
		light.putInt("sky", 15);
		light.putInt("block", 15);
		nbt.put("brightness", light);
		// A hair bigger than the real block, so the two pictures do not flicker where they overlap.
		NbtCompound shape = new NbtCompound();
		shape.put("translation", floats(-0.002F, -0.002F, -0.002F));
		shape.put("scale", floats(1.004F, 1.004F, 1.004F));
		shape.put("left_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
		shape.put("right_rotation", floats(0.0F, 0.0F, 0.0F, 1.0F));
		nbt.put("transformation", shape);
		NbtList tags = new NbtList();
		tags.add(NbtString.of(SIGHT_TAG));
		nbt.put("Tags", tags);
		return EntityType.loadEntityWithPassengers(nbt, world, entity -> {
			entity.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
			return entity;
		});
	}

	private static NbtList floats(float... values) {
		NbtList list = new NbtList();
		for (float value : values) {
			list.add(NbtFloat.of(value));
		}
		return list;
	}

	/** The outline colour for an ore, roughly the colour of what it drops. */
	private static int oreColor(BlockState state) {
		String name = state.getBlock().getTranslationKey();
		if (name.contains("diamond")) {
			return 0x49DCCF;
		}
		if (name.contains("emerald")) {
			return 0x2ED572;
		}
		if (name.contains("gold")) {
			return 0xF6CF38;
		}
		if (name.contains("redstone")) {
			return 0xE63030;
		}
		if (name.contains("lapis")) {
			return 0x3D68E0;
		}
		if (name.contains("copper")) {
			return 0xDF7849;
		}
		if (name.contains("coal")) {
			return 0x45454D;
		}
		if (name.contains("debris")) {
			return 0x7A4A3A;
		}
		if (name.contains("quartz")) {
			return 0xFFFFFF;
		}
		return 0xE3C0A0; // iron and everything else
	}

	/** Tome of Chance: costs 3 levels and turns into a random enchanted book. */
	static boolean enchantBook(ServerWorld world, ServerPlayerEntity player) {
		if (!player.isCreative() && player.experienceLevel < 3) {
			player.sendMessage(Text.literal("You need 3 experience levels."), true);
			return false;
		}
		// The game's own enchanting at level 15 (half of the best an enchanting table can do),
		// but from the full list of enchantments: treasure such as Mending and curses are possible.
		// Very rarely the roll comes up empty, so it is tried a few times.
		ItemStack book = ItemStack.EMPTY;
		for (int attempt = 0; attempt < 8; attempt++) {
			book = EnchantmentHelper.enchant(world.random, new ItemStack(Items.BOOK), 15, world.getRegistryManager(), Optional.empty());
			if (book.isOf(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasEnchantments(book)) {
				break;
			}
			book = ItemStack.EMPTY;
		}
		if (book.isEmpty()) {
			return false;
		}
		if (!player.isCreative()) {
			player.addExperienceLevels(-3);
		}
		player.getInventory().offerOrDrop(book);
		Fx.particles(world, ParticleTypes.ENCHANT, player.getEyePos(), 60, 0.6, 0.8);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
		return true;
	}
}
