package com.combinator.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/**
 * Cataclysms: destruction on the scale of whole chunks. Erasing chunks, turning a chunk upside down, throwing one into
 * the sky, carpet bombing, a hole into the void, the Tsar Bomba, the Event Horizon and a canyon to the bedrock.
 * Blocks in chunks that are not loaded are never touched, so nothing is generated in the middle of a blast.
 */
public final class Cataclysm {
	private Cataclysm() {
	}

	/** True if the chunk that holds this column is loaded. */
	private static boolean loaded(ServerWorld world, int x, int z) {
		return world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
	}

	/** The chunk the player looks at. */
	private static ChunkPos lookChunk(ServerWorld world, ServerPlayerEntity player, int range) {
		return new ChunkPos(BlockPos.ofFloored(Fx.lookPoint(world, player, range)));
	}

	/** Particles that everybody within 512 blocks sees, not only players close by. */
	private static void farParticles(ServerWorld world, ParticleEffect particle, Vec3d pos, int count, double spread, double speed) {
		for (ServerPlayerEntity viewer : world.getPlayers()) {
			if (viewer.squaredDistanceTo(pos) < 512.0 * 512.0) {
				world.spawnParticles(viewer, particle, true, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
			}
		}
	}

	/** The highest block in the square, so an eraser does not waste time on empty sky. */
	private static int highest(ServerWorld world, int minX, int minZ, int maxX, int maxZ) {
		int top = world.getBottomY();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (loaded(world, x, z)) {
					top = Math.max(top, world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z));
				}
			}
		}
		return Math.min(top, world.getTopY() - 1);
	}

	// ------------------------------------------------------------------ Chunk Eraser and Region Eraser

	/**
	 * Erases the chunk the player looks at, and the given number of chunks around it on every side, layer by layer
	 * from the top down to the bedrock. Nothing drops.
	 */
	static boolean eraseChunks(ServerWorld world, ServerPlayerEntity player, int range, int around) {
		ChunkPos chunk = lookChunk(world, player, range);
		int minX = chunk.getStartX() - around * 16;
		int minZ = chunk.getStartZ() - around * 16;
		int maxX = chunk.getEndX() + around * 16;
		int maxZ = chunk.getEndZ() + around * 16;
		int top = highest(world, minX, minZ, maxX, maxZ);
		int bottom = world.getBottomY();
		int perTick = around == 0 ? 4 : 2;
		int layers = top - bottom + 1;
		Vec3d middle = new Vec3d((minX + maxX + 1) / 2.0, top, (minZ + maxZ + 1) / 2.0);
		player.sendMessage(Text.literal("The ground starts to vanish.").formatted(Formatting.DARK_RED), true);
		Fx.sound(world, middle, SoundEvents.BLOCK_PORTAL_TRAVEL, 4.0F, 0.5F);
		int steps = (layers + perTick - 1) / perTick;
		IntConsumer step = i -> {
			Random random = world.random;
			for (int k = 0; k < perTick; k++) {
				int y = top - i * perTick - k;
				if (y < bottom) {
					return;
				}
				BlockPos.Mutable pos = new BlockPos.Mutable();
				for (int x = minX; x <= maxX; x++) {
					for (int z = minZ; z <= maxZ; z++) {
						if (!loaded(world, x, z)) {
							continue;
						}
						pos.set(x, y, z);
						BlockState state = world.getBlockState(pos);
						if (!state.isAir() && state.getHardness(world, pos) >= 0.0F) {
							world.setBlockState(pos, Fx.AIR, Fx.QUIET);
						}
					}
				}
				if (k == 0 && i % 3 == 0) {
					Vec3d at = new Vec3d(minX + random.nextDouble() * (maxX - minX), y, minZ + random.nextDouble() * (maxZ - minZ));
					farParticles(world, ParticleTypes.REVERSE_PORTAL, at, 40, 4.0, 0.2);
				}
			}
			if (i % 10 == 0) {
				Fx.sound(world, new Vec3d(middle.x, top - i * perTick, middle.z), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 4.0F, 0.5F);
			}
		};
		step.accept(0);
		Tasks.repeat(steps - 1, 1, i -> step.accept(i + 1));
		return true;
	}

	// ------------------------------------------------------------------ Chunk Inverter

	/** Turns the chunk the player looks at upside down, one row of 16 columns per tick. */
	static boolean invertChunk(ServerWorld world, ServerPlayerEntity player, int range) {
		ChunkPos chunk = lookChunk(world, player, range);
		Fx.sound(world, Vec3d.ofCenter(chunk.getCenterAtY(player.getBlockY())), SoundEvents.BLOCK_END_PORTAL_SPAWN, 3.0F, 0.6F);
		IntConsumer row = i -> {
			int x = chunk.getStartX() + i;
			for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
				if (loaded(world, x, z)) {
					flipColumn(world, x, z);
				}
			}
			farParticles(world, ParticleTypes.PORTAL, new Vec3d(x, player.getY(), chunk.getCenterZ()), 30, 3.0, 0.5);
		};
		row.accept(0);
		Tasks.repeat(15, 1, i -> row.accept(i + 1));
		player.sendMessage(Text.literal("The world turns upside down.").formatted(Formatting.LIGHT_PURPLE), true);
		return true;
	}

	/** Reverses one column between the bedrock and its highest block. Unbreakable blocks and chests stay where they are. */
	private static void flipColumn(ServerWorld world, int x, int z) {
		int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
		List<BlockPos> spots = new ArrayList<>();
		List<BlockState> states = new ArrayList<>();
		for (int y = world.getBottomY(); y <= top; y++) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = world.getBlockState(pos);
			if (state.getHardness(world, pos) < 0.0F || state.hasBlockEntity()) {
				continue;
			}
			spots.add(pos);
			states.add(state);
		}
		int size = spots.size();
		for (int k = 0; k < size; k++) {
			BlockState flipped = states.get(size - 1 - k);
			if (flipped != states.get(k)) {
				world.setBlockState(spots.get(k), flipped, Fx.QUIET);
			}
		}
	}

	// ------------------------------------------------------------------ Chunk Launcher

	/** The chunk the player looks at, with everything on it, jumps up by the given number of blocks. */
	static boolean launchChunk(ServerWorld world, ServerPlayerEntity player, int range, int lift) {
		ChunkPos chunk = lookChunk(world, player, range);
		int topLimit = world.getTopY() - 1;
		// whatever stands on the chunk goes up first and floats down onto its ground
		Box box = new Box(chunk.getStartX(), world.getBottomY(), chunk.getStartZ(), chunk.getEndX() + 1.0, topLimit, chunk.getEndZ() + 1.0);
		for (Entity entity : world.getOtherEntities(null, box, e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
			if (entity.getY() + lift < topLimit) {
				entity.requestTeleport(entity.getX(), entity.getY() + lift + 1.0, entity.getZ());
				entity.fallDistance = 0.0F;
				if (entity instanceof LivingEntity living) {
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
				}
			}
		}
		IntConsumer row = i -> {
			int x = chunk.getStartX() + i;
			for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
				if (!loaded(world, x, z)) {
					continue;
				}
				int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
				List<BlockPos> spots = new ArrayList<>();
				List<BlockState> states = new ArrayList<>();
				for (int y = world.getBottomY(); y <= top; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState state = world.getBlockState(pos);
					if (state.isAir() || state.getHardness(world, pos) < 0.0F || state.hasBlockEntity()) {
						continue;
					}
					spots.add(pos);
					states.add(state);
					world.setBlockState(pos, Fx.AIR, Fx.QUIET);
				}
				for (int k = 0; k < spots.size(); k++) {
					BlockPos to = spots.get(k).up(lift);
					if (to.getY() < topLimit && world.getBlockState(to).isAir()) {
						world.setBlockState(to, states.get(k), Fx.QUIET);
					}
				}
			}
			if (i % 4 == 0) {
				Fx.sound(world, new Vec3d(x, player.getY(), chunk.getCenterZ()), SoundEvents.BLOCK_PISTON_EXTEND, 4.0F, 0.5F);
			}
			farParticles(world, ParticleTypes.CLOUD, new Vec3d(x, player.getY(), chunk.getCenterZ()), 40, 4.0, 0.2);
		};
		row.accept(0);
		Tasks.repeat(15, 1, i -> row.accept(i + 1));
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, 4.0F, 0.4F);
		player.sendMessage(Text.literal("Lift-off!").formatted(Formatting.GOLD), true);
		return true;
	}

	// ------------------------------------------------------------------ Carpet Bomber

	/** Nine rows of nine TNT blocks, six blocks apart, fall from the sky one row after another. */
	static boolean carpetBomb(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d target = Fx.lookPoint(world, player, range);
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		Vec3d dir = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
		Vec3d side = new Vec3d(-dir.z, 0.0, dir.x);
		IntConsumer row = r -> {
			for (int k = -4; k <= 4; k++) {
				Vec3d at = target.add(dir.multiply((r - 4) * 6.0)).add(side.multiply(k * 6.0));
				if (!loaded(world, (int) Math.floor(at.x), (int) Math.floor(at.z))) {
					continue;
				}
				BlockPos ground = Fx.groundNear(world, at.x, at.z, target.y, 40);
				TntEntity tnt = new TntEntity(world, at.x, Math.max(ground.getY(), target.y) + 30.0, at.z, player);
				tnt.setFuse(80);
				world.spawnEntity(tnt);
			}
			Fx.sound(world, target.add(dir.multiply((r - 4) * 6.0)).add(0.0, 30.0, 0.0), SoundEvents.ENTITY_PHANTOM_FLAP, 6.0F, 0.5F);
		};
		row.accept(0);
		Tasks.repeat(8, 4, r -> row.accept(r + 1));
		player.sendMessage(Text.literal("Bombs away!").formatted(Formatting.RED), true);
		return true;
	}

	// ------------------------------------------------------------------ Orbital Annihilator

	/** A red ring for three seconds, then a beam 16 blocks wide burns down through everything, the bedrock included. */
	static boolean annihilate(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d target = Fx.lookPoint(world, player, range);
		BlockPos ground = BlockPos.ofFloored(target);
		int radius = 8;
		player.sendMessage(Text.literal("Annihilation in 3 seconds. Do not stand there.").formatted(Formatting.DARK_RED), true);
		Fx.sound(world, target, SoundEvents.BLOCK_BEACON_ACTIVATE, 6.0F, 0.5F);
		Tasks.repeat(30, 2, i -> {
			for (int k = 0; k < 24; k++) {
				double angle = k * Math.PI / 12.0 + i * 0.1;
				farParticles(world, ParticleTypes.SOUL_FIRE_FLAME, target.add(Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius), 1, 0.0, 0.0);
			}
			if (i % 5 == 0) {
				Fx.sound(world, target, SoundEvents.BLOCK_NOTE_BLOCK_BASS, 4.0F, 0.5F + i * 0.03F);
			}
		});
		int top = Math.min(world.getTopY() - 1, ground.getY() + 24);
		int bottom = world.getBottomY();
		int perTick = 8;
		Tasks.later(60, () -> {
			Fx.sound(world, target, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 10.0F, 0.4F);
			Fx.fakeBolt(world, target);
			Tasks.repeat((top - bottom) / perTick + 1, 1, step -> {
				for (int k = 0; k < perTick; k++) {
					int y = top - step * perTick - k;
					if (y < bottom) {
						break;
					}
					for (int dx = -radius; dx <= radius; dx++) {
						for (int dz = -radius; dz <= radius; dz++) {
							if (dx * dx + dz * dz > radius * radius + 1 || !loaded(world, ground.getX() + dx, ground.getZ() + dz)) {
								continue;
							}
							BlockPos pos = new BlockPos(ground.getX() + dx, y, ground.getZ() + dz);
							BlockState state = world.getBlockState(pos);
							// the bedrock goes too, but not unbreakable blocks with data (command blocks, structure blocks, gateways)
							if (!state.isAir() && !(state.hasBlockEntity() && state.getHardness(world, pos) < 0.0F)) {
								world.setBlockState(pos, Fx.AIR, Fx.QUIET);
							}
						}
					}
				}
				double y = top - step * perTick;
				Box column = new Box(target.x - radius - 0.5, y - 8.0, target.z - radius - 0.5, target.x + radius + 0.5, top + 40.0, target.z + radius + 0.5);
				for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, column, e -> e.isAlive())) {
					living.timeUntilRegen = 0;
					living.damage(world.getDamageSources().indirectMagic(player, player), 100.0F);
					living.setOnFireFor(10.0F);
				}
				farParticles(world, ParticleTypes.END_ROD, new Vec3d(target.x, y, target.z), 60, radius * 0.5, 0.05);
				farParticles(world, ParticleTypes.EXPLOSION_EMITTER, new Vec3d(target.x, y, target.z), 1, radius * 0.3, 0.0);
				if (step % 3 == 0) {
					Fx.sound(world, new Vec3d(target.x, y, target.z), SoundEvents.ENTITY_GENERIC_EXPLODE, 6.0F, 0.5F);
				}
			});
		});
		return true;
	}

	// ------------------------------------------------------------------ Tsar Bomba

	/** A missile flies in an arc to the spot the player looks at and erases everything within the radius. */
	static boolean tsarBomba(ServerWorld world, ServerPlayerEntity player, int range, double radius) {
		Vec3d target = Fx.lookPoint(world, player, range);
		Vec3d start = player.getEyePos();
		double distance = start.distanceTo(target);
		int flight = (int) Math.max(30.0, Math.min(120.0, distance * 0.6));
		TntEntity missile = new TntEntity(world, start.x, start.y, start.z, player);
		missile.setFuse(flight + 400); // it never explodes by itself
		missile.setNoGravity(true);
		world.spawnEntity(missile);
		if (distance < radius + 16.0) {
			player.sendMessage(Text.literal("You are inside the blast radius. RUN!").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
		} else {
			player.sendMessage(Text.literal("Missile away.").formatted(Formatting.RED), true);
		}
		Fx.sound(world, start, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 4.0F, 0.4F);
		double arc = distance * 0.3;
		Tasks.repeat(flight, 1, i -> {
			double t = (i + 1) / (double) flight;
			Vec3d at = start.lerp(target, t).add(0.0, Math.sin(t * Math.PI) * arc, 0.0);
			missile.setPosition(at);
			missile.setVelocity(Vec3d.ZERO);
			farParticles(world, ParticleTypes.FLAME, at, 6, 0.1, 0.02);
			farParticles(world, ParticleTypes.LARGE_SMOKE, at, 3, 0.1, 0.01);
			if (i % 10 == 0) {
				Fx.sound(world, at, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 3.0F, 0.5F);
			}
			if (i == flight - 1) {
				missile.discard();
				detonate(world, player, target, radius);
			}
		});
		return true;
	}

	/** The blast grows one block per tick. After it: a mushroom cloud, fallout, wildfires and black rain. */
	private static void detonate(ServerWorld world, ServerPlayerEntity owner, Vec3d center, double radius) {
		farParticles(world, ParticleTypes.EXPLOSION_EMITTER, center, 20, radius * 0.2, 0.0);
		Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 40.0F, 0.3F);
		Fx.sound(world, center, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 40.0F, 0.4F);
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(center) < 512.0 * 512.0) {
				player.sendMessage(Text.literal("TSAR BOMBA").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
			}
		}
		int steps = (int) Math.ceil(radius);
		Tasks.repeat(steps, 1, i -> {
			double outer = Math.min(radius, i + 1.0);
			eraseShell(world, center, i, outer);
			Box box = Box.of(center, outer * 2.0, outer * 2.0, outer * 2.0);
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e.squaredDistanceTo(center) <= outer * outer)) {
				living.timeUntilRegen = 0;
				living.damage(world.getDamageSources().indirectMagic(owner, owner), 100.0F);
			}
			if (i % 4 == 0) {
				Random random = world.random;
				Vec3d edge = center.add((random.nextDouble() - 0.5) * outer * 2.0, (random.nextDouble() - 0.5) * outer, (random.nextDouble() - 0.5) * outer * 2.0);
				farParticles(world, ParticleTypes.EXPLOSION_EMITTER, edge, 3, outer * 0.3, 0.0);
				Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 20.0F, 0.4F + random.nextFloat() * 0.2F);
			}
		});
		Tasks.later(steps + 1, () -> aftermath(world, center, radius));
	}

	private static void aftermath(ServerWorld world, Vec3d center, double radius) {
		Random random = world.random;
		// the mushroom cloud: a column of smoke with a wide cap
		Tasks.repeat(100, 2, i -> {
			for (int k = 0; k < 10; k++) {
				farParticles(world, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, center.add((random.nextDouble() - 0.5) * 6.0, k * 7.0, (random.nextDouble() - 0.5) * 6.0), 1, 0.5, 0.02);
			}
			for (int k = 0; k < 16; k++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double r = random.nextDouble() * 24.0;
				farParticles(world, ParticleTypes.LARGE_SMOKE, center.add(Math.cos(angle) * r, 70.0 + random.nextDouble() * 10.0, Math.sin(angle) * r), 2, 1.0, 0.02);
			}
			farParticles(world, ParticleTypes.ASH, center.add(0.0, 20.0, 0.0), 40, radius * 0.6, 0.0);
		});
		// fallout: everything alive near the crater is sick for a long time
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 3.0, radius * 3.0, radius * 3.0), e -> e.isAlive())) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 600, 1));
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 0));
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 1));
		}
		// wildfires around the crater
		for (int i = 0; i < 300; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double r = radius + random.nextDouble() * 16.0;
			double x = center.x + Math.cos(angle) * r;
			double z = center.z + Math.sin(angle) * r;
			if (!loaded(world, (int) Math.floor(x), (int) Math.floor(z))) {
				continue;
			}
			BlockPos spot = Fx.groundNear(world, x, z, center.y, 40);
			if (world.getBlockState(spot).isAir() && AbstractFireBlock.canPlaceAt(world, spot, Direction.UP)) {
				world.setBlockState(spot, AbstractFireBlock.getState(world, spot), Block.NOTIFY_ALL);
			}
		}
		// black rain
		world.setWeather(0, 6000, true, true);
	}

	/**
	 * Deletes every breakable block whose distance from the centre is at least inner and less than outer.
	 * Unlike Fx.erase it only visits the blocks of the shell, so it stays fast for very big balls.
	 */
	static int eraseShell(ServerWorld world, Vec3d center, double inner, double outer) {
		BlockPos c = BlockPos.ofFloored(center);
		int r = (int) Math.ceil(outer);
		double inner2 = inner * inner;
		double outer2 = outer * outer;
		int minY = Math.max(world.getBottomY(), c.getY() - r);
		int maxY = Math.min(world.getTopY() - 1, c.getY() + r);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int count = 0;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double flat = dx * dx + dz * dz;
				if (flat >= outer2 || !loaded(world, c.getX() + dx, c.getZ() + dz)) {
					continue;
				}
				// in this column only the part between the inner and the outer sphere is visited
				int dyMax = (int) Math.ceil(Math.sqrt(outer2 - flat));
				double restInner = inner2 - flat;
				int gap = restInner > 0.0 ? Math.max(0, (int) Math.floor(Math.sqrt(restInner)) - 1) : 0;
				for (int dy = -dyMax; dy <= dyMax; dy++) {
					if (gap > 0 && dy > -gap && dy < gap) {
						dy = gap - 1; // jump over the part that is already gone
						continue;
					}
					int y = c.getY() + dy;
					if (y < minY || y > maxY) {
						continue;
					}
					double d2 = flat + dy * dy;
					if (d2 < inner2 || d2 >= outer2) {
						continue;
					}
					pos.set(c.getX() + dx, y, c.getZ() + dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir() || state.getHardness(world, pos) < 0.0F) {
						continue;
					}
					world.setBlockState(pos, Fx.AIR, Fx.QUIET);
					count++;
				}
			}
		}
		return count;
	}

	// ------------------------------------------------------------------ Event Horizon

	/** A black hole that grows for 20 seconds: it pulls everything within 40 blocks and eats a ball up to 16 blocks. */
	static boolean eventHorizon(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d center = hit.getType() == HitResult.Type.BLOCK ? hit.getPos().subtract(look.multiply(2.0)) : hit.getPos();
		double pull = 40.0;
		double maxEat = 16.0;
		int steps = 200;
		double[] eaten = {0.0};
		Fx.sound(world, center, SoundEvents.BLOCK_PORTAL_TRAVEL, 4.0F, 0.3F);
		player.sendMessage(Text.literal("An event horizon opens. Nothing comes back.").formatted(Formatting.DARK_PURPLE), true);
		Tasks.repeat(steps, 2, i -> {
			double eat = maxEat * (i + 1) / steps;
			double crush = Math.max(1.8, eat * 0.6);
			for (Entity entity : world.getOtherEntities(null, Box.of(center, pull * 2.0, pull * 2.0, pull * 2.0), e -> e.isAlive() && !e.isSpectator())) {
				Vec3d to = center.subtract(entity.getPos());
				double distance = to.length();
				if (distance > pull) {
					continue;
				}
				if (distance < crush) {
					if (entity instanceof LivingEntity living) {
						living.timeUntilRegen = 0;
						living.damage(world.getDamageSources().magic(), 20.0F);
					} else {
						entity.discard();
					}
					continue;
				}
				Vec3d push = to.multiply((0.15 + 0.6 * (1.0 - distance / pull)) / distance);
				entity.addVelocity(push.x, push.y, push.z);
				entity.velocityModified = true;
			}
			if (i % 5 == 0 && eat > eaten[0]) {
				eraseShell(world, center, Math.max(0.0, eaten[0] - 0.5), eat);
				eaten[0] = eat;
			}
			farParticles(world, ParticleTypes.PORTAL, center, 80, 0.5, 2.5);
			farParticles(world, ParticleTypes.SQUID_INK, center, 10, eat * 0.15, 0.02);
			if (i % 20 == 0) {
				Fx.sound(world, center, SoundEvents.BLOCK_PORTAL_AMBIENT, 4.0F, 0.3F);
			}
			if (i == steps - 1) {
				farParticles(world, ParticleTypes.EXPLOSION_EMITTER, center, 6, 2.0, 0.0);
				Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 10.0F, 0.5F);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ Fault Line Spike

	/** A canyon tears open in front of the player, 5 wide, down to the bedrock, with lava at the bottom. */
	static boolean faultLine(ServerWorld world, ServerPlayerEntity player, int length) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		Vec3d dir = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
		Vec3d side = new Vec3d(-dir.z, 0.0, dir.x);
		Vec3d start = player.getPos().add(dir.multiply(3.0));
		int bottom = world.getBottomY();
		BlockState lava = Blocks.LAVA.getDefaultState();
		int perTick = 4;
		IntConsumer step = i -> {
			Random random = world.random;
			for (int s = i * perTick; s < i * perTick + perTick && s < length; s++) {
				double wobble = Math.sin(s * 0.15) * 3.0;
				for (int w = -2; w <= 2; w++) {
					Vec3d at = start.add(dir.multiply(s)).add(side.multiply(w + wobble));
					int x = (int) Math.floor(at.x);
					int z = (int) Math.floor(at.z);
					if (!loaded(world, x, z)) {
						continue;
					}
					int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
					for (int y = top; y > bottom; y--) {
						BlockPos pos = new BlockPos(x, y, z);
						BlockState state = world.getBlockState(pos);
						if (state.isAir() || state.getHardness(world, pos) < 0.0F) {
							continue;
						}
						world.setBlockState(pos, y <= bottom + 2 ? lava : Fx.AIR, Fx.QUIET);
					}
				}
				if (s % 6 == 0) {
					Vec3d at = start.add(dir.multiply(s));
					farParticles(world, ParticleTypes.EXPLOSION, at, 4, 2.0, 0.0);
					Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 3.0F, 0.4F + random.nextFloat() * 0.2F);
				}
			}
		};
		step.accept(0);
		Tasks.repeat((length + perTick - 1) / perTick - 1, 1, i -> step.accept(i + 1));
		player.sendMessage(Text.literal("The earth tears open.").formatted(Formatting.GOLD), true);
		return true;
	}
}
