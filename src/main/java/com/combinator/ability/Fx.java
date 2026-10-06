package com.combinator.ability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
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
