package com.combinator.ability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

/** Shared helpers: sounds, particles, line of sight, explosions and erasing blocks. */
final class Fx {
	/** Change a block and tell the client, but do not wake up the neighbouring blocks. Much faster for big changes. */
	static final int QUIET = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
	static final BlockState AIR = Blocks.AIR.getDefaultState();

	private static final Map<String, RegistryEntry<EntityAttribute>> ATTRIBUTES = new HashMap<>();

	private Fx() {
	}

	static void sound(World world, Vec3d pos, SoundEvent sound, float volume, float pitch) {
		world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
	}

	static void sound(World world, Vec3d pos, RegistryEntry<SoundEvent> sound, float volume, float pitch) {
		world.playSound(null, pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, volume, pitch);
	}

	static void particles(ServerWorld world, ParticleEffect particle, Vec3d pos, int count, double spread, double speed) {
		world.spawnParticles(particle, pos.x, pos.y, pos.z, count, spread, spread, spread, speed);
	}

	/** A line of particles from one point to another. */
	static void line(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to) {
		double length = from.distanceTo(to);
		int steps = (int) Math.min(120, Math.max(1, length * 2));
		Vec3d step = to.subtract(from).multiply(1.0 / steps);
		Vec3d at = from;
		for (int i = 0; i < steps; i++) {
			at = at.add(step);
			world.spawnParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** The player's line of sight up to the first solid block. Fluids are ignored. */
	static BlockHitResult ray(PlayerEntity player, double range) {
		Vec3d start = player.getEyePos();
		Vec3d end = start.add(player.getRotationVec(1.0F).multiply(range));
		return player.getWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
	}

	/** Like ray, but water and lava also stop the line of sight. */
	static BlockHitResult rayWithFluids(PlayerEntity player, double range) {
		Vec3d start = player.getEyePos();
		Vec3d end = start.add(player.getRotationVec(1.0F).multiply(range));
		return player.getWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
	}

	/** The point the player looks at. If nothing is in range: the ground below the end of the line of sight. */
	static Vec3d lookPoint(ServerWorld world, PlayerEntity player, double range) {
		BlockHitResult hit = ray(player, range);
		if (hit.getType() == HitResult.Type.BLOCK) {
			return hit.getPos();
		}
		BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(hit.getPos()));
		return new Vec3d(hit.getPos().x, top.getY(), hit.getPos().z);
	}

	/** The living thing the player looks at, or null. Walls block the view. */
	static LivingEntity lookEntity(ServerWorld world, PlayerEntity player, double range) {
		Vec3d start = player.getEyePos();
		Vec3d direction = player.getRotationVec(1.0F);
		BlockHitResult wall = ray(player, range);
		double reach = wall.getType() == HitResult.Type.BLOCK ? wall.getPos().distanceTo(start) : range;
		Vec3d end = start.add(direction.multiply(reach));
		Box box = player.getBoundingBox().stretch(direction.multiply(reach)).expand(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityCollision(world, player, start, end, box,
				entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator());
		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			return living;
		}
		return null;
	}

	/**
	 * The first free block above the ground at x, z: where something standing on the ground would be.
	 * Under the open sky this is the highest ground. Under a roof (the Nether) it is the ground near the given height.
	 */
	static BlockPos surface(ServerWorld world, double x, double z, double nearY) {
		BlockPos column = BlockPos.ofFloored(x, nearY, z);
		if (!world.getDimension().hasCeiling()) {
			return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
		}
		for (int dy = 8; dy >= -16; dy--) {
			BlockPos pos = column.up(dy);
			if (pos.getY() <= world.getBottomY()) {
				break;
			}
			if (world.getBlockState(pos).isAir() && !world.getBlockState(pos.down()).isAir()) {
				return pos;
			}
		}
		return column;
	}

	/** Like surface, but if the ground there is far above or below the given height, the given height is used. */
	static BlockPos groundNear(ServerWorld world, double x, double z, double nearY, int maxStep) {
		BlockPos top = surface(world, x, z, nearY);
		if (Math.abs(top.getY() - nearY) > maxStep) {
			return BlockPos.ofFloored(x, nearY, z);
		}
		return top;
	}

	/** True if a structure may put a block here: nothing is there, or only grass, flowers, snow, water and the like. */
	static boolean canFill(BlockState state) {
		return state.isAir() || state.isReplaceable();
	}

	/** True if a block can be torn loose and thrown around: not air, not a liquid, not too hard, no chest or furnace. */
	static boolean loose(ServerWorld world, BlockPos pos, BlockState state) {
		if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) {
			return false;
		}
		float hardness = state.getHardness(world, pos);
		return hardness >= 0.0F && hardness < 50.0F;
	}

	/** Turns a block into a falling block with the given speed. The block's place becomes empty. */
	static FallingBlockEntity fling(ServerWorld world, BlockPos pos, BlockState state, double vx, double vy, double vz) {
		FallingBlockEntity falling = FallingBlockEntity.spawnFromBlock(world, pos, state);
		falling.setVelocity(vx, vy, vz);
		falling.velocityModified = true;
		return falling;
	}

	/** A lightning bolt that only looks and sounds real: it starts no fires and hurts nobody by itself. */
	static void fakeBolt(ServerWorld world, Vec3d at) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt != null) {
			bolt.refreshPositionAfterTeleport(at);
			bolt.setCosmetic(true);
			world.spawnEntity(bolt);
		}
	}

	/** A random spot within the given distance where a player has room to stand, or null if none was found. */
	static BlockPos standingSpot(ServerWorld world, Vec3d center, double distance) {
		Random random = world.random;
		for (int attempt = 0; attempt < 24; attempt++) {
			double x = center.x + (random.nextDouble() - 0.5) * 2.0 * distance;
			double z = center.z + (random.nextDouble() - 0.5) * 2.0 * distance;
			BlockPos feet = surface(world, x, z, center.y);
			if (feet.getY() > world.getBottomY() + 1 && hasRoom(world, feet)) {
				return feet;
			}
		}
		return null;
	}

	/** True if a player fits at this position: something solid below, and two blocks without walls or liquids. */
	static boolean hasRoom(ServerWorld world, BlockPos feet) {
		return world.getBlockState(feet.down()).isSolidBlock(world, feet.down())
				&& world.getBlockState(feet).getCollisionShape(world, feet).isEmpty() && world.getFluidState(feet).isEmpty()
				&& world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty() && world.getFluidState(feet.up()).isEmpty();
	}

	/** Looks up a game attribute such as "generic.scale". Returns null if the game does not have it. */
	static RegistryEntry<EntityAttribute> attribute(String id) {
		if (!ATTRIBUTES.containsKey(id)) {
			ATTRIBUTES.put(id, Registries.ATTRIBUTE.getEntry(Identifier.ofVanilla(id)).orElse(null));
		}
		return ATTRIBUTES.get(id);
	}

	/**
	 * A real explosion.
	 * immune: an entity that takes no damage and no knockback from it (may be null). It also gets the credit for kills.
	 * breakBlocks: false = hurts mobs only.
	 * Dropped items and XP are never destroyed by it.
	 */
	static void boom(ServerWorld world, Entity immune, Vec3d pos, float power, boolean fire, boolean breakBlocks) {
		world.createExplosion(immune, (DamageSource) null, new SafeBlast(immune), pos.x, pos.y, pos.z, power, fire,
				breakBlocks ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);
	}

	private static final class SafeBlast extends ExplosionBehavior {
		private final Entity immune;

		SafeBlast(Entity immune) {
			this.immune = immune;
		}

		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return entity != this.immune && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity);
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return entity == this.immune ? 0.0F : 1.0F;
		}
	}

	/**
	 * Deletes every breakable block whose distance from the centre is at least inner and less than outer.
	 * Nothing drops. Bedrock and other unbreakable blocks stay. Returns the number of deleted blocks.
	 */
	static int erase(ServerWorld world, Vec3d center, double inner, double outer) {
		BlockPos c = BlockPos.ofFloored(center);
		int r = (int) Math.ceil(outer);
		double inner2 = inner * inner;
		double outer2 = outer * outer;
		int minY = Math.max(world.getBottomY(), c.getY() - r);
		int maxY = Math.min(world.getTopY() - 1, c.getY() + r);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int count = 0;
		for (int y = minY; y <= maxY; y++) {
			int dy = y - c.getY();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d2 = dx * dx + dy * dy + dz * dz;
					if (d2 < inner2 || d2 >= outer2) {
						continue;
					}
					pos.set(c.getX() + dx, y, c.getZ() + dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir() || state.getHardness(world, pos) < 0.0F) {
						continue;
					}
					world.setBlockState(pos.toImmutable(), AIR, QUIET);
					count++;
				}
			}
		}
		return count;
	}
}
