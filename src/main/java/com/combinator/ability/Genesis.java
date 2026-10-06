package com.combinator.ability;

import com.combinator.item.ComboItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Instant creation: a floating island, a beanstalk, a castle, a pyramid, a mountain, a crater lake, a rainbow bridge,
 * portals, splitting mobs, a cannon full of animals, a parted sea, a snowman army, walls and a farm.
 * Structures only fill empty space (air, grass, flowers, water), unless they say otherwise.
 */
public final class Genesis {
	private static final Block[] RAINBOW = {
			Blocks.RED_WOOL, Blocks.ORANGE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.LIGHT_BLUE_WOOL, Blocks.PURPLE_WOOL
	};

	/** What the Menagerie Cannon can fire. The warden is in the list once, out of almost fifty. */
	private static final EntityType<?>[] ZOO = {
			EntityType.BEE, EntityType.CAT, EntityType.CHICKEN, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.WOLF,
			EntityType.FOX, EntityType.PANDA, EntityType.POLAR_BEAR, EntityType.LLAMA, EntityType.GOAT, EntityType.FROG,
			EntityType.CAMEL, EntityType.SNIFFER, EntityType.ARMADILLO, EntityType.TURTLE, EntityType.PARROT, EntityType.OCELOT,
			EntityType.RABBIT, EntityType.MOOSHROOM, EntityType.HORSE, EntityType.DONKEY, EntityType.SLIME, EntityType.MAGMA_CUBE,
			EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.ENDERMAN, EntityType.WITCH,
			EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.VILLAGER,
			EntityType.ALLAY, EntityType.BLAZE, EntityType.HOGLIN, EntityType.STRIDER, EntityType.GLOW_SQUID, EntityType.DOLPHIN,
			EntityType.AXOLOTL, EntityType.BAT, EntityType.RAVAGER, EntityType.GHAST, EntityType.WARDEN
	};

	private record Saved(BlockPos pos, BlockState state) {
	}

	private Genesis() {
	}

	private static void put(ServerWorld world, BlockPos pos, BlockState state) {
		if (Fx.canFill(world.getBlockState(pos))) {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		}
	}

	/** Runs step 0 at once and the rest one after another, the given number of ticks apart. */
	private static void stepwise(int steps, int interval, IntConsumer step) {
		step.accept(0);
		if (steps > 1) {
			Tasks.repeat(steps - 1, interval, i -> step.accept(i + 1));
		}
	}

	private static Vec3d flatLook(ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		return flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
	}

	/** The free block on the ground where the player looks. */
	private static BlockPos lookGround(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos pos = BlockPos.ofFloored(Fx.lookPoint(world, player, range));
		return Fx.canFill(world.getBlockState(pos)) ? pos : pos.up();
	}

	// ------------------------------------------------------------------ Genesis Seed

	/** A floating island 16 blocks above the spot the player looks at. */
	static boolean skyIsland(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos ground = lookGround(world, player, range);
		int top = Math.min(ground.getY() + 16, world.getTopY() - 12);
		island(world, new BlockPos(ground.getX(), top, ground.getZ()), 8, 10);
		player.sendMessage(Text.literal("Let there be land.").formatted(Formatting.GREEN), true);
		return true;
	}

	/** A small repeatable wobble (-1 .. 1), so the island edge is not a perfect circle. */
	private static double wobble(long seed, int x, int z) {
		long h = seed ^ (x * 73856093L) ^ (z * 19349663L);
		h = (h ^ (h >>> 13)) * 0x5bd1e995L;
		return ((h >>> 8) & 0xFFFF) / 32768.0 - 1.0;
	}

	static void island(ServerWorld world, BlockPos center, int radius, int depth) {
		long seed = world.random.nextLong();
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.BLOCK_BEACON_ACTIVATE, 3.0F, 1.2F);
		stepwise(depth, 1, k -> {
			Random random = world.random;
			double r = radius * Math.pow(1.0 - (double) k / depth, 0.7) + 0.5;
			int ri = (int) Math.ceil(r + 1.0);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					if (Math.sqrt(dx * dx + dz * dz) > r + wobble(seed, dx, dz) * 1.2) {
						continue;
					}
					put(world, center.add(dx, -k, dz), islandBlock(random, k));
				}
			}
			Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(center.down(k)), 20, r * 0.5, 0.0);
		});
		Tasks.later(depth + 2, () -> decorate(world, center, radius));
	}

	private static BlockState islandBlock(Random random, int layer) {
		if (layer == 0) {
			return Blocks.GRASS_BLOCK.getDefaultState();
		}
		if (layer <= 2) {
			return Blocks.DIRT.getDefaultState();
		}
		float pick = random.nextFloat();
		Block block = pick < 0.012F ? Blocks.DIAMOND_ORE
				: pick < 0.017F ? Blocks.EMERALD_ORE
				: pick < 0.04F ? Blocks.GOLD_ORE
				: pick < 0.09F ? Blocks.IRON_ORE
				: pick < 0.15F ? Blocks.COAL_ORE
				: pick < 0.17F ? Blocks.LAPIS_ORE
				: pick < 0.19F ? Blocks.REDSTONE_ORE
				: pick < 0.25F ? Blocks.ANDESITE
				: pick < 0.3F ? Blocks.GRANITE
				: Blocks.STONE;
		return block.getDefaultState();
	}

	/** Trees, a pond, a waterfall over the edge, flowers and a few animals. */
	private static void decorate(ServerWorld world, BlockPos center, int radius) {
		Random random = world.random;
		// a pond
		for (int dx = 1; dx <= 3; dx++) {
			for (int dz = -3; dz <= -1; dz++) {
				BlockPos pos = center.add(dx, 0, dz);
				if (world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK) && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
					world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
		// a waterfall: a spring at the edge that pours down forever
		for (int dx = radius; dx > 0; dx--) {
			BlockPos pos = center.add(-dx, 0, 0);
			if (world.getBlockState(pos).isOf(Blocks.GRASS_BLOCK)) {
				world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
				break;
			}
		}
		// two trees
		growTree(world, center.add(-3, 1, 3), (SaplingBlock) Blocks.OAK_SAPLING, random);
		growTree(world, center.add(3, 1, 3), (SaplingBlock) Blocks.BIRCH_SAPLING, random);
		// flowers and grass
		Block[] plants = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.SHORT_GRASS, Blocks.SHORT_GRASS, Blocks.AZURE_BLUET};
		for (int i = 0; i < 24; i++) {
			BlockPos pos = center.add(random.nextInt(radius * 2 + 1) - radius, 1, random.nextInt(radius * 2 + 1) - radius);
			if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)) {
				world.setBlockState(pos, plants[random.nextInt(plants.length)].getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		// animals
		Spells.spawn(world, EntityType.SHEEP, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 1.5);
		Spells.spawn(world, EntityType.SHEEP, center.getX() - 1.5, center.getY() + 1.0, center.getZ() - 1.5);
		Spells.spawn(world, EntityType.COW, center.getX() + 1.5, center.getY() + 1.0, center.getZ() + 3.5);
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.ITEM_BONE_MEAL_USE, 3.0F, 0.6F);
	}

	private static void growTree(ServerWorld world, BlockPos pos, SaplingBlock sapling, Random random) {
		if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK)) {
			return;
		}
		BlockState state = sapling.getDefaultState().with(SaplingBlock.STAGE, 1);
		world.setBlockState(pos, state, Block.NOTIFY_ALL);
		sapling.generate(world, pos, state, random);
	}

	// ------------------------------------------------------------------ Magic Beans

	static boolean beanstalk(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos base = lookGround(world, player, range);
		int height = Math.min(64, world.getTopY() - base.getY() - 8);
		if (height < 16) {
			player.sendMessage(Text.literal("No room for a beanstalk here."), true);
			return false;
		}
		Fx.sound(world, Vec3d.ofCenter(base), SoundEvents.ITEM_BONE_MEAL_USE, 2.0F, 0.5F);
		stepwise((height + 1) / 2, 1, i -> {
			for (int h = i * 2; h < i * 2 + 2 && h < height; h++) {
				stalk(world, base, h);
			}
		});
		Tasks.later(height / 2 + 2, () -> cloud(world, base.up(height), player));
		return true;
	}

	/** The stalk is 2 x 2 blocks of moss that winds around. Vines grow on it, so it can be climbed. */
	private static void stalk(ServerWorld world, BlockPos base, int h) {
		double angle = h * 0.18;
		BlockPos core = base.add((int) Math.round(Math.cos(angle) * 1.5), h, (int) Math.round(Math.sin(angle) * 1.5));
		BlockState moss = Blocks.MOSS_BLOCK.getDefaultState();
		for (int dx = 0; dx <= 1; dx++) {
			for (int dz = 0; dz <= 1; dz++) {
				put(world, core.add(dx, 0, dz), moss);
			}
		}
		vine(world, core.add(-1, 0, 0), VineBlock.EAST);
		vine(world, core.add(-1, 0, 1), VineBlock.EAST);
		vine(world, core.add(2, 0, 0), VineBlock.WEST);
		vine(world, core.add(2, 0, 1), VineBlock.WEST);
		vine(world, core.add(0, 0, -1), VineBlock.SOUTH);
		vine(world, core.add(1, 0, 2), VineBlock.NORTH);
		if (h % 7 == 3) {
			BlockState leaves = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
			for (int dx = -2; dx <= 3; dx++) {
				for (int dz = -2; dz <= 3; dz++) {
					if ((dx < 0 || dx > 1 || dz < 0 || dz > 1) && Math.abs(dx - 0.5) + Math.abs(dz - 0.5) < 3.5) {
						BlockPos pos = core.add(dx, 0, dz);
						if (world.getBlockState(pos).isAir()) {
							world.setBlockState(pos, leaves, Block.NOTIFY_LISTENERS);
						}
					}
				}
			}
		}
		if (h % 4 == 0) {
			Fx.sound(world, Vec3d.ofCenter(core), SoundEvents.BLOCK_MOSS_PLACE, 1.5F, 0.6F + h * 0.01F);
		}
	}

	private static void vine(ServerWorld world, BlockPos pos, BooleanProperty side) {
		if (world.getBlockState(pos).isAir()) {
			world.setBlockState(pos, Blocks.VINE.getDefaultState().with(side, true), Block.NOTIFY_LISTENERS);
		}
	}

	/** The cloud at the top: a chest with treasure and a sleeping giant. */
	private static void cloud(ServerWorld world, BlockPos top, ServerPlayerEntity player) {
		Random random = world.random;
		BlockState wool = Blocks.WHITE_WOOL.getDefaultState();
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -9; dz <= 9; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > 8.0 + random.nextDouble()) {
					continue;
				}
				put(world, top.add(dx, 0, dz), wool);
				if (d > 6.0 && random.nextInt(3) == 0) {
					put(world, top.add(dx, 1, dz), wool);
				}
				if (d < 5.0 && random.nextInt(4) == 0) {
					put(world, top.add(dx, -1, dz), wool);
				}
			}
		}
		BlockPos chestPos = top.add(2, 1, 2);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.NORTH), Block.NOTIFY_ALL);
		if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			ItemStack egg = new ItemStack(Items.EGG);
			egg.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Golden Egg").formatted(Formatting.GOLD));
			chest.setStack(0, new ItemStack(Items.GOLD_INGOT, 8 + random.nextInt(9)));
			chest.setStack(4, new ItemStack(Items.DIAMOND, 2 + random.nextInt(3)));
			chest.setStack(13, egg);
			chest.setStack(22, new ItemStack(Items.GOLDEN_APPLE, 2));
			chest.setStack(26, new ItemStack(Items.MUSIC_DISC_OTHERSIDE));
		}
		Entity giant = Spells.spawn(world, EntityType.GIANT, top.getX() - 2.5, top.getY() + 1.0, top.getZ() - 2.5);
		if (giant instanceof MobEntity mob) {
			mob.setPersistent();
			mob.setCustomName(Text.literal("The sleeping giant"));
		}
		Fx.sound(world, Vec3d.ofCenter(top), SoundEvents.ENTITY_RAVAGER_ROAR, 3.0F, 0.5F);
		if (player.isAlive()) {
			player.sendMessage(Text.literal("Fee-fi-fo-fum!").formatted(Formatting.GOLD), true);
		}
	}

	// ------------------------------------------------------------------ Castle in a Box

	static boolean castle(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos ground = lookGround(world, player, range);
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		Fx.sound(world, Vec3d.ofCenter(ground), SoundEvents.BLOCK_STONE_PLACE, 2.0F, 0.5F);
		stepwise(13, 2, h -> {
			Random random = world.random;
			for (int u = -10; u <= 10; u++) {
				for (int v = 0; v <= 20; v++) {
					BlockState state = castleBlock(u, v, h, random);
					if (state == null) {
						continue;
					}
					BlockPos pos = ground.offset(right, u).offset(forward, v).up(h);
					if (world.getBlockState(pos).getHardness(world, pos) < 0.0F || world.getBlockState(pos).hasBlockEntity()) {
						continue;
					}
					world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
				}
			}
			Fx.particles(world, ParticleTypes.POOF, Vec3d.ofCenter(ground.offset(forward, 10).up(h)), 30, 6.0, 0.02);
		});
		Tasks.later(30, () -> {
			// a golden throne in the keep
			world.setBlockState(ground.offset(forward, 16).up(1), Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(ground.offset(forward, 17).up(1), Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(ground.offset(forward, 17).up(2), Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			// torches beside the gate
			world.setBlockState(ground.offset(right, 2).offset(forward, 1).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(ground.offset(right, -2).offset(forward, 1).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
			// two guards
			for (int side : new int[] {-3, 3}) {
				BlockPos spot = ground.offset(right, side).offset(forward, 6).up(1);
				Entity golem = Spells.spawn(world, EntityType.IRON_GOLEM, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				if (golem instanceof IronGolemEntity guard) {
					guard.setPlayerCreated(true);
				}
			}
			Fx.sound(world, Vec3d.ofCenter(ground.offset(forward, 10)), SoundEvents.EVENT_RAID_HORN, 3.0F, 1.0F);
		});
		player.sendMessage(Text.literal("Your castle is being built.").formatted(Formatting.GOLD), true);
		return true;
	}

	private static BlockState brick(Random random) {
		float pick = random.nextFloat();
		return (pick < 0.12F ? Blocks.MOSSY_STONE_BRICKS : pick < 0.18F ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState();
	}

	/**
	 * The block of the castle at u (left to right, -10..10), v (front to back, 0..20) and h (height, 0 = ground).
	 * null means "leave it as it is".
	 */
	private static BlockState castleBlock(int u, int v, int h, Random random) {
		boolean towerU = u <= -6 || u >= 6;
		boolean towerV = v <= 4 || v >= 16;
		if (towerU && towerV) {
			int tu = u < 0 ? u + 8 : u - 8;  // -2..2 inside the tower
			int tv = v <= 4 ? v - 2 : v - 18;
			boolean ring = Math.abs(tu) == 2 || Math.abs(tv) == 2;
			if (h == 0 || h == 9) {
				return brick(random);
			}
			if (h < 9) {
				boolean slit = h == 6 && ring && (tu == 0 || tv == 0);
				return ring && !slit ? brick(random) : Fx.AIR;
			}
			if (h == 10) {
				return ring && (tu + tv) % 2 == 0 ? brick(random) : Fx.AIR;
			}
			return null;
		}
		boolean wall = Math.abs(u) == 10 || v == 0 || v == 20;
		if (wall) {
			if (h == 0) {
				return brick(random);
			}
			boolean gate = v == 0 && Math.abs(u) <= 1 && h <= 3;
			if (gate) {
				return Fx.AIR;
			}
			if (h <= 6) {
				return brick(random);
			}
			if (h == 7) {
				return (u + v) % 2 == 0 ? brick(random) : Fx.AIR;
			}
			return null;
		}
		// the walkway along the inside of the walls
		boolean walk = Math.abs(u) == 9 || v == 1 || v == 19;
		if (walk && h == 6) {
			return brick(random);
		}
		// the keep at the back
		boolean keep = Math.abs(u) <= 4 && v >= 11 && v <= 18;
		if (keep) {
			boolean ring = Math.abs(u) == 4 || v == 11 || v == 18;
			if (h == 0 || h == 8) {
				return brick(random);
			}
			if (h < 8) {
				boolean door = v == 11 && u == 0 && h <= 2;
				boolean window = h == 5 && ring && (u == 0 || v == 14 || v == 15);
				return ring && !door && !window ? brick(random) : Fx.AIR;
			}
			if (h == 9) {
				return Math.abs(u) == 4 || v == 11 || v == 18 ? ((u + v) % 2 == 0 ? brick(random) : Fx.AIR) : null;
			}
			return null;
		}
		// the courtyard: a cobblestone floor and empty air
		if (h == 0) {
			return random.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE.getDefaultState() : Blocks.COBBLESTONE.getDefaultState();
		}
		return h <= 8 ? Fx.AIR : null;
	}

	// ------------------------------------------------------------------ Pharaoh's Scarab

	static boolean pyramid(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos front = lookGround(world, player, range);
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		int half = 10;
		BlockPos middle = front.offset(forward, half);
		stepwise(half + 1, 2, h -> {
			Random random = world.random;
			int size = half - h;
			for (int u = -size; u <= size; u++) {
				for (int v = -size; v <= size; v++) {
					BlockPos pos = middle.offset(right, u).offset(forward, v).up(h);
					BlockState there = world.getBlockState(pos);
					if (there.getHardness(world, pos) < 0.0F || there.hasBlockEntity()) {
						continue;
					}
					boolean chamber = Math.abs(u) <= 2 && Math.abs(v) <= 2 && h >= 1 && h <= 3;
					boolean tunnel = u == 0 && v < -2 && h >= 1 && h <= 2;
					BlockState state;
					if (chamber || tunnel) {
						state = Fx.AIR;
					} else if (size == 0) {
						state = Blocks.GOLD_BLOCK.getDefaultState();
					} else if (h == 0) {
						state = Blocks.SMOOTH_SANDSTONE.getDefaultState();
					} else if (Math.abs(u) == size || Math.abs(v) == size) {
						state = (random.nextInt(4) == 0 ? Blocks.CUT_SANDSTONE : Blocks.SANDSTONE).getDefaultState();
					} else {
						state = Blocks.SANDSTONE.getDefaultState();
					}
					world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
				}
			}
			Fx.particles(world, ParticleTypes.POOF, Vec3d.ofCenter(middle.up(h)), 30, size * 0.5, 0.02);
			Fx.sound(world, Vec3d.ofCenter(middle.up(h)), SoundEvents.BLOCK_SAND_PLACE, 2.0F, 0.6F);
		});
		Tasks.later(half * 2 + 4, () -> treasure(world, middle, forward, right));
		player.sendMessage(Text.literal("A pyramid rises. Beware the curse.").formatted(Formatting.GOLD), true);
		return true;
	}

	/** The treasure chamber: a chest at the back, torches, and a trap: TNT under a pressure plate. */
	private static void treasure(ServerWorld world, BlockPos middle, Direction forward, Direction right) {
		Random random = world.random;
		BlockPos chestPos = middle.offset(forward, 2).up(1);
		world.setBlockState(chestPos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, forward.getOpposite()), Block.NOTIFY_ALL);
		if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
			chest.setStack(0, new ItemStack(Items.GOLD_INGOT, 10 + random.nextInt(12)));
			chest.setStack(2, new ItemStack(Items.EMERALD, 4 + random.nextInt(6)));
			chest.setStack(4, new ItemStack(random.nextInt(10) == 0 ? Items.ENCHANTED_GOLDEN_APPLE : Items.GOLDEN_APPLE));
			Item curse = ComboItems.ALL.get(random.nextInt(ComboItems.ALL.size()));
			chest.setStack(13, new ItemStack(curse));
		}
		for (int u = -1; u <= 1; u++) {
			for (int v = -1; v <= 1; v++) {
				BlockPos under = middle.offset(right, u).offset(forward, v).down();
				if (world.getBlockState(under).getHardness(world, under) >= 0.0F && !world.getBlockState(under).hasBlockEntity()) {
					world.setBlockState(under, Blocks.TNT.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
		}
		world.setBlockState(middle.up(1), Blocks.STONE_PRESSURE_PLATE.getDefaultState(), Block.NOTIFY_ALL);
		world.setBlockState(middle.offset(right, -2).offset(forward, -2).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
		world.setBlockState(middle.offset(right, 2).offset(forward, -2).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
	}

	// ------------------------------------------------------------------ Titan's Spade

	/** A mountain rises where the player looks. Whatever stands on it rides up. */
	static boolean mountain(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos base = lookGround(world, player, range);
		int radius = 10;
		int peak = 16;
		long seed = world.random.nextLong();
		int[][] heights = new int[radius * 2 + 1][radius * 2 + 1];
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / radius;
				double shape = d >= 1.0 ? 0.0 : Math.pow(Math.cos(d * Math.PI / 2.0), 1.5);
				heights[dx + radius][dz + radius] = (int) Math.round(peak * shape + wobble(seed, dx, dz) * 1.2);
			}
		}
		stepwise(peak + 2, 2, s -> {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int height = heights[dx + radius][dz + radius];
					if (s >= height) {
						continue;
					}
					BlockState state;
					if (s == height - 1) {
						state = height > 12 ? Blocks.SNOW_BLOCK.getDefaultState() : height > 9 ? Blocks.STONE.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState();
					} else if (s >= height - 3 && height <= 9) {
						state = Blocks.DIRT.getDefaultState();
					} else {
						state = Blocks.STONE.getDefaultState();
					}
					put(world, base.add(dx, s, dz), state);
				}
			}
			Box box = new Box(base.getX() - radius, base.getY() - 1.0, base.getZ() - radius, base.getX() + radius + 1.0, base.getY() + s + 1.0, base.getZ() + radius + 1.0);
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator())) {
				int dx = (int) Math.floor(living.getX()) - base.getX();
				int dz = (int) Math.floor(living.getZ()) - base.getZ();
				if (Math.abs(dx) > radius || Math.abs(dz) > radius) {
					continue;
				}
				int top = Math.min(heights[dx + radius][dz + radius], s + 1);
				if (living.getY() < base.getY() + top) {
					living.requestTeleport(living.getX(), base.getY() + top, living.getZ());
				}
			}
			if (s % 3 == 0) {
				Fx.sound(world, Vec3d.ofCenter(base.up(s)), SoundEvents.BLOCK_STONE_PLACE, 3.0F, 0.4F);
				Fx.particles(world, ParticleTypes.POOF, Vec3d.ofCenter(base.up(s)), 30, radius * 0.6, 0.02);
			}
		});
		return true;
	}

	/** Sneak: a crater, filled with water (in the Nether: with lava) up to one block below the rim. */
	static boolean crater(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos center = lookGround(world, player, range).down();
		int radius = 8;
		int depth = 6;
		BlockState fill = (world.getDimension().ultrawarm() ? Blocks.LAVA : Blocks.WATER).getDefaultState();
		List<BlockPos> dug = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / radius;
				if (d > 1.0) {
					continue;
				}
				int deep = (int) Math.round(depth * (1.0 - d * d));
				for (int y = 0; y < Math.max(1, deep); y++) {
					BlockPos pos = center.add(dx, -y, dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir() || state.getHardness(world, pos) < 0.0F || state.hasBlockEntity()) {
						continue;
					}
					world.setBlockState(pos, Fx.AIR, Fx.QUIET);
					dug.add(pos);
				}
				for (int y = 1; y <= 3; y++) {
					BlockPos above = center.add(dx, y, dz);
					BlockState state = world.getBlockState(above);
					if (!state.isAir() && state.isReplaceable()) {
						world.setBlockState(above, Fx.AIR, Fx.QUIET);
					}
				}
			}
		}
		for (BlockPos pos : dug) {
			if (pos.getY() <= center.getY() - 1) {
				world.setBlockState(pos, fill, Block.NOTIFY_LISTENERS);
			}
		}
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
		Fx.particles(world, ParticleTypes.POOF, Vec3d.ofCenter(center), 80, radius * 0.5, 0.05);
		return !dug.isEmpty();
	}

	// ------------------------------------------------------------------ Bifrost Staff

	/** A rainbow bridge in an arc to the spot the player looks at. Too close: 24 blocks straight ahead. */
	static boolean rainbowBridge(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d start = player.getPos().add(0.0, -1.0, 0.0);
		Vec3d target = Fx.lookPoint(world, player, range);
		double dx = target.x - start.x;
		double dz = target.z - start.z;
		double length = Math.sqrt(dx * dx + dz * dz);
		Vec3d dir;
		double endY;
		if (length < 8.0) {
			dir = flatLook(player);
			length = 24.0;
			endY = start.y;
		} else {
			dir = new Vec3d(dx / length, 0.0, dz / length);
			endY = Math.floor(target.y) - 1.0;
		}
		double rise = endY - start.y;
		double peak = length / 4.0;
		Vec3d side = new Vec3d(-dir.z, 0.0, dir.x);
		int steps = (int) Math.ceil(length * 2.0);
		double total = length;
		stepwise(steps / 8 + 1, 1, i -> {
			for (int s = i * 8; s < i * 8 + 8 && s <= steps; s++) {
				double t = s / (double) steps;
				double along = t * total;
				double y = start.y + rise * t + peak * 4.0 * t * (1.0 - t);
				for (int w = 0; w < RAINBOW.length; w++) {
					double off = w - 2.5;
					BlockPos pos = BlockPos.ofFloored(start.x + dir.x * along + side.x * off, y, start.z + dir.z * along + side.z * off);
					put(world, pos, RAINBOW[w].getDefaultState());
				}
			}
			if (i % 2 == 0) {
				Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.6F + i * 0.05F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ Portal Gun

	/** A lit Nether portal on the clicked block, facing the player. */
	static boolean netherPortal(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockPos base = hit.getBlockPos().offset(hit.getSide());
		Direction along = player.getHorizontalFacing().rotateYClockwise();
		for (int a = -1; a <= 2; a++) {
			for (int h = 0; h <= 4; h++) {
				BlockPos pos = base.offset(along, a).up(h);
				if (world.getBlockState(pos).getHardness(world, pos) < 0.0F || world.getBlockState(pos).hasBlockEntity()) {
					player.sendMessage(Text.literal("Something unbreakable is in the way."), true);
					return false;
				}
			}
		}
		for (int a = -1; a <= 2; a++) {
			for (int h = 0; h <= 4; h++) {
				boolean frame = a == -1 || a == 2 || h == 0 || h == 4;
				world.setBlockState(base.offset(along, a).up(h), frame ? Blocks.OBSIDIAN.getDefaultState() : Fx.AIR, Fx.QUIET);
			}
		}
		BlockState portal = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, along.getAxis());
		for (int a = 0; a <= 1; a++) {
			for (int h = 1; h <= 3; h++) {
				world.setBlockState(base.offset(along, a).up(h), portal, Fx.QUIET);
			}
		}
		Vec3d middle = Vec3d.ofCenter(base.up(2));
		Fx.sound(world, middle, SoundEvents.BLOCK_PORTAL_TRIGGER, 1.0F, 1.2F);
		Fx.particles(world, ParticleTypes.PORTAL, middle, 80, 1.0, 0.5);
		return true;
	}

	/** Sneak: the floor the player looks at becomes an End portal, framed like the real one. */
	static boolean endPortal(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK || hit.getSide() != Direction.UP) {
			player.sendMessage(Text.literal("Look at the floor to open the End portal."), true);
			return false;
		}
		BlockPos center = hit.getBlockPos();
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				BlockPos pos = center.add(dx, 0, dz);
				if (world.getBlockState(pos).getHardness(world, pos) < 0.0F || world.getBlockState(pos).hasBlockEntity()) {
					player.sendMessage(Text.literal("Something unbreakable is in the way."), true);
					return false;
				}
			}
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				boolean edgeX = Math.abs(dx) == 2;
				boolean edgeZ = Math.abs(dz) == 2;
				if (edgeX && edgeZ) {
					continue;
				}
				BlockState state;
				if (edgeX || edgeZ) {
					Direction facing = dx == -2 ? Direction.EAST : dx == 2 ? Direction.WEST : dz == -2 ? Direction.SOUTH : Direction.NORTH;
					state = Blocks.END_PORTAL_FRAME.getDefaultState().with(EndPortalFrameBlock.FACING, facing).with(EndPortalFrameBlock.EYE, true);
				} else {
					state = Blocks.END_PORTAL.getDefaultState();
				}
				world.setBlockState(center.add(dx, 0, dz), state, Block.NOTIFY_ALL);
			}
		}
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.BLOCK_END_PORTAL_SPAWN, 2.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Mitosis Ray

	static boolean mitosis(ServerWorld world, ServerPlayerEntity player, int range) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null || target instanceof PlayerEntity || target instanceof EnderDragonEntity) {
			return false;
		}
		if (!split(world, target)) {
			return false;
		}
		Fx.line(world, ParticleTypes.ITEM_SLIME, player.getEyePos(), target.getPos().add(0.0, target.getHeight() * 0.5, 0.0));
		return true;
	}

	/** Sneak: every mob near the player splits. At most 16. */
	static boolean mitosisBurst(ServerWorld world, ServerPlayerEntity player, int radius) {
		int split = 0;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius),
				e -> e.isAlive() && !(e instanceof PlayerEntity) && !(e instanceof EnderDragonEntity))) {
			if (split >= 16) {
				break;
			}
			if (split(world, living)) {
				split++;
			}
		}
		return split > 0;
	}

	/** A perfect copy of the mob appears next to it: same health, name, gear and everything else. */
	static boolean split(ServerWorld world, LivingEntity target) {
		NbtCompound saved = new NbtCompound();
		if (!target.saveSelfNbt(saved)) {
			return false;
		}
		saved.remove("UUID");
		saved.remove("Passengers");
		Vec3d at = target.getPos();
		Entity copy = EntityType.loadEntityWithPassengers(saved, world, entity -> {
			entity.refreshPositionAndAngles(at.x, at.y, at.z, target.getYaw(), target.getPitch());
			return entity;
		});
		if (copy == null) {
			return false;
		}
		world.spawnNewEntityAndPassengers(copy);
		double angle = world.random.nextDouble() * Math.PI * 2.0;
		target.addVelocity(Math.cos(angle) * 0.3, 0.2, Math.sin(angle) * 0.3);
		target.velocityModified = true;
		copy.addVelocity(-Math.cos(angle) * 0.3, 0.2, -Math.sin(angle) * 0.3);
		copy.velocityModified = true;
		Fx.particles(world, ParticleTypes.ITEM_SLIME, at.add(0.0, target.getHeight() * 0.5, 0.0), 20, 0.4, 0.05);
		Fx.sound(world, at, SoundEvents.ENTITY_SLIME_SQUISH, 1.0F, 0.8F);
		return true;
	}

	// ------------------------------------------------------------------ Menagerie Cannon

	static boolean menagerie(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		EntityType<?> type = ZOO[world.random.nextInt(ZOO.length)];
		Entity entity = Spells.spawn(world, type, eye.x + look.x * 1.5, eye.y - 0.4 + look.y * 1.5, eye.z + look.z * 1.5);
		if (entity == null) {
			return false;
		}
		entity.setVelocity(look.x * 1.4, look.y * 1.4 + 0.3, look.z * 1.4);
		entity.velocityModified = true;
		Fx.particles(world, ParticleTypes.POOF, eye.add(look.multiply(1.5)), 12, 0.3, 0.05);
		Fx.sound(world, eye, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.6F, 1.8F);
		player.sendMessage(Text.literal("Fired: ").append(entity.getName()).formatted(Formatting.YELLOW), true);
		return true;
	}

	// ------------------------------------------------------------------ Staff of the Red Sea

	/** The water in front of the player parts: a path 5 wide down to the sea floor. It comes back after a while. */
	static boolean partSea(ServerWorld world, ServerPlayerEntity player, int length, int restoreTicks) {
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockPos origin = player.getBlockPos();
		List<Saved> saved = new ArrayList<>();
		IntConsumer part = i -> {
			for (int s = i * 4; s < i * 4 + 4 && s < length; s++) {
				BlockPos line = origin.offset(forward, 1 + s);
				for (int w = -2; w <= 2; w++) {
					BlockPos column = line.offset(right, w);
					for (int dy = 6; dy >= -24; dy--) {
						BlockPos pos = column.up(dy);
						if (pos.getY() <= world.getBottomY()) {
							break;
						}
						BlockState state = world.getBlockState(pos);
						if (state.getFluidState().isEmpty() || state.isOf(Blocks.LAVA)) {
							if (dy <= 0 && !state.isAir()) {
								break; // the sea floor
							}
							continue;
						}
						saved.add(new Saved(pos.toImmutable(), state));
						if (state.contains(net.minecraft.state.property.Properties.WATERLOGGED)) {
							world.setBlockState(pos, state.with(net.minecraft.state.property.Properties.WATERLOGGED, false), Fx.QUIET);
						} else {
							world.setBlockState(pos, Fx.AIR, Fx.QUIET);
						}
					}
				}
				if (s % 4 == 0) {
					Fx.particles(world, ParticleTypes.SPLASH, Vec3d.ofCenter(line), 40, 2.0, 0.3);
				}
			}
		};
		part.accept(0);
		if (saved.isEmpty()) {
			player.sendMessage(Text.literal("There is no sea in front of you."), true);
			return false;
		}
		Tasks.repeat((length + 3) / 4 - 1, 1, i -> part.accept(i + 1));
		Fx.sound(world, player.getPos(), SoundEvents.ITEM_BUCKET_FILL, 3.0F, 0.5F);
		player.sendMessage(Text.literal("The sea parts before you. It comes back in " + restoreTicks / 20 + " seconds.").formatted(Formatting.AQUA), true);
		Tasks.later(restoreTicks, () -> {
			for (int k = saved.size() - 1; k >= 0; k--) {
				Saved s = saved.get(k);
				BlockState now = world.getBlockState(s.pos());
				if (now.isAir() || now.isReplaceable() || now.getBlock() == s.state().getBlock()) {
					world.setBlockState(s.pos(), s.state(), Fx.QUIET);
				}
			}
			Fx.sound(world, player.getPos(), SoundEvents.ITEM_BUCKET_EMPTY, 3.0F, 0.5F);
		});
		return true;
	}

	// ------------------------------------------------------------------ Snowman Horn

	static boolean snowArmy(ServerWorld world, ServerPlayerEntity player, int count) {
		Random random = world.random;
		for (int i = 0; i < count; i++) {
			double angle = i * Math.PI * 2.0 / count;
			BlockPos spot = Fx.groundNear(world, player.getX() + Math.cos(angle) * 3.0, player.getZ() + Math.sin(angle) * 3.0, player.getY(), 3);
			Spells.spawn(world, EntityType.SNOW_GOLEM, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		}
		BlockState snow = Blocks.SNOW.getDefaultState();
		for (int i = 0; i < 30; i++) {
			BlockPos pos = Fx.groundNear(world, player.getX() + (random.nextDouble() - 0.5) * 12.0, player.getZ() + (random.nextDouble() - 0.5) * 12.0, player.getY(), 3);
			if (world.getBlockState(pos).isAir() && snow.canPlaceAt(world, pos)) {
				world.setBlockState(pos, snow, Block.NOTIFY_ALL);
			}
		}
		Fx.particles(world, ParticleTypes.SNOWFLAKE, player.getPos().add(0.0, 1.0, 0.0), 200, 4.0, 0.05);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_SNOW_GOLEM_AMBIENT, 2.0F, 0.8F);
		return true;
	}

	// ------------------------------------------------------------------ Fortress Staff

	/** A wall 9 wide and 5 high rises 3 blocks in front of the player. */
	static boolean stoneWall(ServerWorld world, ServerPlayerEntity player) {
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockPos center = player.getBlockPos().offset(forward, 3);
		stepwise(5, 2, h -> {
			Random random = world.random;
			for (int w = -4; w <= 4; w++) {
				BlockPos pos = center.offset(right, w).up(h);
				put(world, pos, (random.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE).getDefaultState());
			}
			Fx.sound(world, Vec3d.ofCenter(center.up(h)), SoundEvents.BLOCK_STONE_PLACE, 1.5F, 0.6F);
		});
		return true;
	}

	/** Sneak: a stone dome with a glass top closes over the player. */
	static boolean stoneDome(ServerWorld world, ServerPlayerEntity player) {
		BlockPos center = player.getBlockPos();
		int placed = 0;
		for (int dx = -6; dx <= 6; dx++) {
			for (int dy = -1; dy <= 6; dy++) {
				for (int dz = -6; dz <= 6; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
					if (d < 4.5 || d > 5.5) {
						continue;
					}
					BlockPos pos = center.add(dx, dy, dz);
					if (Fx.canFill(world.getBlockState(pos))) {
						world.setBlockState(pos, (dy >= 4 ? Blocks.GLASS : Blocks.STONE_BRICKS).getDefaultState(), Block.NOTIFY_LISTENERS);
						placed++;
					}
				}
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_STONE_PLACE, 2.0F, 0.5F);
		return placed > 0;
	}

	// ------------------------------------------------------------------ Farm in a Box

	/** A ripe 9 x 9 farm with water in the middle and a scarecrow at the corner. */
	static boolean farm(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos center = lookGround(world, player, range).down();
		CropBlock[] crops = {(CropBlock) Blocks.WHEAT, (CropBlock) Blocks.CARROTS, (CropBlock) Blocks.POTATOES, (CropBlock) Blocks.BEETROOTS};
		BlockState farmland = Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7);
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				BlockPos pos = center.add(dx, 0, dz);
				BlockState there = world.getBlockState(pos);
				if (there.getHardness(world, pos) < 0.0F || there.hasBlockEntity()) {
					continue;
				}
				for (int up = 1; up <= 2; up++) {
					BlockPos above = pos.up(up);
					if (!world.getBlockState(above).isAir() && Fx.canFill(world.getBlockState(above))) {
						world.setBlockState(above, Fx.AIR, Block.NOTIFY_LISTENERS);
					}
				}
				if (dx == 0 && dz == 0) {
					world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS);
					continue;
				}
				world.setBlockState(pos, farmland, Block.NOTIFY_LISTENERS);
				CropBlock crop = crops[((dz + 4) / 3 + (dx > 0 ? 1 : 0)) % crops.length];
				if (world.getBlockState(pos.up()).isAir()) {
					world.setBlockState(pos.up(), crop.withAge(crop.getMaxAge()), Block.NOTIFY_LISTENERS);
				}
			}
		}
		// the scarecrow
		BlockPos scarecrow = center.add(5, 1, 5);
		if (Fx.canFill(world.getBlockState(scarecrow)) && Fx.canFill(world.getBlockState(scarecrow.up()))) {
			world.setBlockState(scarecrow, Blocks.HAY_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(scarecrow.up(), Blocks.CARVED_PUMPKIN.getDefaultState(), Block.NOTIFY_ALL);
		}
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(center.up()), 80, 3.0, 0.0);
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.ITEM_HOE_TILL, 2.0F, 0.8F);
		return true;
	}
}
