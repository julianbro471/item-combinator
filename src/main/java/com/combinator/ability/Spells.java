package com.combinator.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;

/**
 * The big, wild abilities: bombs, meteors, black holes, time stop, shape changing, instant buildings.
 * Every method returns true if the ability did something (so the item's cooldown starts).
 */
public final class Spells {
	private static final TagKey<Block> DIRT = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("dirt"));
	private static final Identifier RAY_ID = Identifier.of("combinator", "size_ray");
	private static final String GHOST_TAG = "combinator_ghost";
	private static final Map<UUID, GameMode> GHOSTS = new HashMap<>();

	private static final EntityType<?>[] FARM_ANIMALS = {
			EntityType.CHICKEN, EntityType.PIG, EntityType.SHEEP, EntityType.COW, EntityType.RABBIT, EntityType.FROG, EntityType.BAT
	};

	/** Philosopher's Stone: each block turns into the next one in this list. */
	private static final Block[] VALUE_CHAIN = {
			Blocks.COBBLESTONE, Blocks.COAL_BLOCK, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK,
			Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.NETHERITE_BLOCK
	};
	/** Blocks that count as the start of the list. */
	private static final Block[] CHEAP_BLOCKS = {
			Blocks.STONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.DIRT, Blocks.NETHERRACK,
			Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.TUFF, Blocks.SAND, Blocks.GRAVEL, Blocks.END_STONE
	};

	private Spells() {
	}

	/** Puts a new entity of the given kind into the world. Returns null if the game refuses to create it. */
	static Entity spawn(ServerWorld world, EntityType<?> type, double x, double y, double z) {
		Entity entity = type.create(world);
		if (entity == null) {
			return null;
		}
		entity.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360.0F, 0.0F);
		world.spawnEntity(entity);
		return entity;
	}

	// ------------------------------------------------------------------ bombs

	/** Throws a visible TNT block. After the fuse, the TNT is removed and "detonate" decides what happens there. */
	static void throwBomb(ServerWorld world, ServerPlayerEntity player, float speed, int fuse, Consumer<Vec3d> detonate) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		TntEntity tnt = new TntEntity(world, eye.x + look.x, eye.y - 0.3, eye.z + look.z, player);
		tnt.setFuse(fuse + 200); // it never explodes by itself
		tnt.setVelocity(look.multiply(speed));
		world.spawnEntity(tnt);
		Fx.sound(world, eye, SoundEvents.ENTITY_TNT_PRIMED, 1.0F, 0.7F);
		Tasks.later(fuse, () -> {
			Vec3d at = tnt.getPos();
			tnt.discard();
			detonate.accept(at);
		});
	}

	/** Mega Dynamite: one very large normal explosion. It hurts the thrower too. */
	static boolean megaBomb(ServerWorld world, ServerPlayerEntity player, float power) {
		throwBomb(world, player, 1.3F, 60, at -> Fx.boom(world, null, at, power, false, true));
		return true;
	}

	/** Pocket Nuke and Doomsday Device. */
	static boolean nuke(ServerWorld world, ServerPlayerEntity player, float radius) {
		throwBomb(world, player, 1.4F, 80, at -> bigBlast(world, player, at, radius));
		return true;
	}

	/**
	 * Erases a ball of the given radius, growing outward 2 blocks per tick so the game does not freeze.
	 * Everything alive inside takes heavy damage, the owner included.
	 */
	static void bigBlast(ServerWorld world, ServerPlayerEntity owner, Vec3d center, double radius) {
		Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 12.0F, 0.5F);
		Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, center, 8, radius * 0.25, 0.0);
		int steps = (int) Math.ceil(radius / 2.0);
		Tasks.repeat(steps, 1, i -> {
			double inner = i * 2.0;
			double outer = Math.min(radius, inner + 2.0);
			Fx.erase(world, center, inner, outer);
			Box box = Box.of(center, outer * 2.0, outer * 2.0, outer * 2.0);
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box,
					e -> e.isAlive() && e.squaredDistanceTo(center) <= outer * outer)) {
				living.damage(world.getDamageSources().indirectMagic(owner, owner), 60.0F);
			}
			if (i % 2 == 0) {
				Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, center, 4, outer * 0.5, 0.0);
				Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 8.0F, 0.6F + world.random.nextFloat() * 0.3F);
			}
		});
	}

	// ------------------------------------------------------------------ disasters

	static boolean meteor(ServerWorld world, ServerPlayerEntity player, Vec3d target, float power) {
		Random random = world.random;
		Vec3d start = target.add((random.nextDouble() - 0.5) * 16.0, 55.0, (random.nextDouble() - 0.5) * 16.0);
		Vec3d direction = target.subtract(start).normalize();
		FireballEntity meteor = new FireballEntity(world, player, direction, Math.round(power));
		meteor.setPosition(start.x, start.y, start.z);
		world.spawnEntity(meteor);
		Fx.sound(world, target, SoundEvents.ENTITY_GHAST_SHOOT, 4.0F, 0.5F);
		return true;
	}

	static boolean meteorShower(ServerWorld world, ServerPlayerEntity player, Vec3d target) {
		Tasks.repeat(14, 4, i -> {
			Random random = world.random;
			meteor(world, player, target.add((random.nextDouble() - 0.5) * 22.0, 0.0, (random.nextDouble() - 0.5) * 22.0), 3.0F);
		});
		return true;
	}

	/** A black hole for 10 seconds: pulls everything in, crushes what reaches the middle, eats the blocks around it. */
	static boolean blackHole(ServerWorld world, Vec3d center) {
		double pull = 16.0;
		Fx.sound(world, center, SoundEvents.BLOCK_PORTAL_TRAVEL, 1.5F, 0.4F);
		Tasks.repeat(100, 2, i -> {
			Box box = Box.of(center, pull * 2.0, pull * 2.0, pull * 2.0);
			for (Entity entity : world.getOtherEntities(null, box, e -> e.isAlive() && !e.isSpectator())) {
				Vec3d to = center.subtract(entity.getPos());
				double distance = to.length();
				if (distance > pull) {
					continue;
				}
				if (distance < 1.8) {
					if (entity instanceof LivingEntity living) {
						living.damage(world.getDamageSources().magic(), 12.0F);
					} else {
						entity.discard();
					}
					continue;
				}
				Vec3d push = to.normalize().multiply(0.12 + 0.4 * (1.0 - distance / pull));
				entity.addVelocity(push.x, push.y, push.z);
				entity.velocityModified = true;
			}
			if (i % 5 == 0) {
				Fx.erase(world, center, 0.0, 1.5 + 5.5 * i / 100.0);
			}
			Fx.particles(world, ParticleTypes.PORTAL, center, 60, 0.3, 2.0);
			Fx.particles(world, ParticleTypes.SQUID_INK, center, 6, 0.2, 0.02);
			if (i == 99) {
				Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, center, 1, 0.0, 0.0);
				Fx.sound(world, center, SoundEvents.ENTITY_GENERIC_EXPLODE, 3.0F, 1.6F);
			}
		});
		return true;
	}

	/** Earthshaker: nearby mobs are hurt and thrown up, and chunks of the ground fly into the air. */
	static boolean quake(ServerWorld world, ServerPlayerEntity player) {
		Random random = world.random;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(12.0),
				e -> e != player && e.isAlive())) {
			living.damage(world.getDamageSources().indirectMagic(player, player), 10.0F);
			living.addVelocity(0.0, 1.1, 0.0);
			living.velocityModified = true;
		}
		BlockPos base = player.getBlockPos();
		int thrown = 0;
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				int flat = dx * dx + dz * dz;
				if (flat > 36 || flat <= 2 || random.nextInt(3) == 0) {
					continue; // outside the circle, under the player, or randomly left alone
				}
				for (int dy = 2; dy >= -3; dy--) {
					BlockPos pos = base.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					float hardness = state.getHardness(world, pos);
					boolean surface = world.getBlockState(pos.up()).isAir();
					if (surface && !state.hasBlockEntity() && hardness >= 0.0F && hardness < 50.0F && state.getFluidState().isEmpty()) {
						FallingBlockEntity falling = FallingBlockEntity.spawnFromBlock(world, pos, state);
						falling.setVelocity((random.nextDouble() - 0.5) * 0.5, 0.5 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.5);
						falling.velocityModified = true;
						thrown++;
					}
					break; // only the top block of each column
				}
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.5F);
		Fx.particles(world, ParticleTypes.EXPLOSION, player.getPos(), 12, 4.0, 0.0);
		return true;
	}

	/** Ocean Orb: fills the air around the target with water. */
	static boolean flood(ServerWorld world, Vec3d target) {
		BlockPos center = BlockPos.ofFloored(target).up();
		BlockState water = Blocks.WATER.getDefaultState();
		int r = 6;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					BlockPos pos = center.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					if (state.isAir() || (state.isReplaceable() && state.getFluidState().isEmpty())) {
						world.setBlockState(pos, water, Block.NOTIFY_ALL);
					}
				}
			}
		}
		Fx.sound(world, target, SoundEvents.ITEM_BUCKET_EMPTY, 2.0F, 0.6F);
		return true;
	}

	/** Ocean Orb, sneaking: removes all water and lava around the player. */
	static boolean drain(ServerWorld world, ServerPlayerEntity player) {
		BlockPos center = player.getBlockPos();
		int r = 12;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dy * dy + dz * dz > r * r) {
						continue;
					}
					BlockPos pos = center.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					if (state.getFluidState().isEmpty()) {
						continue;
					}
					if (state.contains(Properties.WATERLOGGED)) {
						world.setBlockState(pos, state.with(Properties.WATERLOGGED, false), Fx.QUIET);
					} else {
						world.setBlockState(pos, Fx.AIR, Fx.QUIET);
					}
				}
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.ITEM_BUCKET_FILL, 2.0F, 0.6F);
		return true;
	}

	/** Winter Globe: ice, obsidian, snow, and frozen mobs. */
	static boolean freezeArea(ServerWorld world, ServerPlayerEntity player, int r) {
		BlockPos center = player.getBlockPos();
		BlockState snow = Blocks.SNOW.getDefaultState();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r) {
					continue;
				}
				for (int dy = 6; dy >= -6; dy--) {
					BlockPos pos = center.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					if (state.isOf(Blocks.WATER)) {
						world.setBlockState(pos, Blocks.ICE.getDefaultState(), Block.NOTIFY_LISTENERS);
					} else if (state.isOf(Blocks.LAVA)) {
						Block cooled = state.getFluidState().isStill() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
						world.setBlockState(pos, cooled.getDefaultState(), Block.NOTIFY_LISTENERS);
					} else if (state.isAir() && snow.canPlaceAt(world, pos)) {
						world.setBlockState(pos, snow, Block.NOTIFY_LISTENERS);
					}
				}
			}
		}
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(r),
				e -> e != player && e.isAlive())) {
			living.damage(world.getDamageSources().indirectMagic(player, player), 6.0F);
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 9), player);
			living.setFrozenTicks(living.getMinFreezeDamageTicks() + 300);
		}
		Fx.particles(world, ParticleTypes.SNOWFLAKE, player.getPos().add(0.0, 2.0, 0.0), 400, r * 0.5, 0.05);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_GLASS_BREAK, 2.0F, 0.5F);
		return true;
	}

	static boolean anvilStorm(ServerWorld world, Vec3d target) {
		Tasks.repeat(18, 2, i -> {
			Random random = world.random;
			BlockPos pos = BlockPos.ofFloored(target.x + (random.nextDouble() - 0.5) * 10.0,
					target.y + 22.0 + random.nextDouble() * 6.0, target.z + (random.nextDouble() - 0.5) * 10.0);
			if (!world.getBlockState(pos).isAir()) {
				return;
			}
			FallingBlockEntity anvil = FallingBlockEntity.spawnFromBlock(world, pos, Blocks.ANVIL.getDefaultState());
			anvil.setHurtEntities(2.0F, 40);
		});
		Fx.sound(world, target, SoundEvents.BLOCK_ANVIL_LAND, 2.0F, 0.6F);
		return true;
	}

	static boolean chickenStorm(ServerWorld world, Vec3d target) {
		Tasks.repeat(24, 1, i -> {
			Random random = world.random;
			spawn(world, EntityType.CHICKEN, target.x + (random.nextDouble() - 0.5) * 12.0,
					target.y + 16.0 + random.nextDouble() * 8.0, target.z + (random.nextDouble() - 0.5) * 12.0);
		});
		Fx.sound(world, target, SoundEvents.ENTITY_CHICKEN_EGG, 2.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ mobs

	private static boolean isBoss(Entity entity) {
		return entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
	}

	/** Turns the mob the player looks at into a random farm animal. */
	static boolean polymorph(ServerWorld world, ServerPlayerEntity player, int range) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null) {
			return false;
		}
		return polymorph(world, target);
	}

	static boolean polymorph(ServerWorld world, LivingEntity target) {
		if (target instanceof PlayerEntity || target instanceof EnderDragonEntity) {
			return false;
		}
		Vec3d at = target.getPos();
		EntityType<?> type = FARM_ANIMALS[world.random.nextInt(FARM_ANIMALS.length)];
		target.discard();
		spawn(world, type, at.x, at.y, at.z);
		Fx.particles(world, ParticleTypes.POOF, at.add(0.0, 0.5, 0.0), 30, 0.4, 0.05);
		Fx.sound(world, at, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 0.6F, 1.8F);
		return true;
	}

	/** Size Ray: makes the mob the player looks at tiny and weak, or huge and strong. The change is permanent. */
	static boolean resize(ServerWorld world, ServerPlayerEntity player, int range, boolean grow) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null || target instanceof PlayerEntity) {
			return false;
		}
		setRayModifier(target, "generic.scale", grow ? 2.0 : -0.75);
		setRayModifier(target, "generic.attack_damage", grow ? 1.0 : -0.7);
		Fx.line(world, ParticleTypes.END_ROD, player.getEyePos(), target.getPos().add(0.0, 0.5, 0.0));
		Fx.sound(world, target.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, grow ? 0.6F : 1.8F);
		return true;
	}

	private static void setRayModifier(LivingEntity target, String attributeId, double value) {
		RegistryEntry<EntityAttribute> attribute = Fx.attribute(attributeId);
		if (attribute == null) {
			return;
		}
		EntityAttributeInstance instance = target.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(RAY_ID);
		instance.addPersistentModifier(new EntityAttributeModifier(RAY_ID, value, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	/** Bell of Madness: every mob nearby gets the nearest other mob as its target. */
	static boolean madness(ServerWorld world, ServerPlayerEntity player, int range) {
		List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(range), e -> e.isAlive());
		for (MobEntity mob : mobs) {
			MobEntity nearest = null;
			double best = Double.MAX_VALUE;
			for (MobEntity other : mobs) {
				if (other == mob) {
					continue;
				}
				double distance = mob.squaredDistanceTo(other);
				if (distance < best) {
					best = distance;
					nearest = other;
				}
			}
			if (nearest != null) {
				mob.setTarget(nearest);
				Fx.particles(world, ParticleTypes.ANGRY_VILLAGER, mob.getPos().add(0.0, mob.getHeight() + 0.3, 0.0), 2, 0.2, 0.0);
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BELL_USE, 2.0F, 0.5F);
		return true;
	}

	static boolean summonGolems(ServerWorld world, ServerPlayerEntity player, int count) {
		Random random = world.random;
		for (int i = 0; i < count; i++) {
			Entity entity = spawn(world, EntityType.IRON_GOLEM,
					player.getX() + (random.nextDouble() - 0.5) * 5.0, player.getY() + 0.5, player.getZ() + (random.nextDouble() - 0.5) * 5.0);
			if (entity instanceof IronGolemEntity golem) {
				golem.setPlayerCreated(true); // golems made by a player never attack that player
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 2.0F, 0.6F);
		return true;
	}

	/** Gravity Staff: sends nearby mobs into the sky (lift) or smashes them into the ground (slam). */
	static boolean gravity(ServerWorld world, ServerPlayerEntity player, int range, boolean lift) {
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range),
				e -> e != player && e.isAlive() && !(e instanceof PlayerEntity))) {
			if (lift) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 80, 24), player);
			} else {
				living.removeStatusEffect(StatusEffects.LEVITATION);
				living.setVelocity(0.0, -4.0, 0.0);
				living.velocityModified = true;
				living.damage(world.getDamageSources().indirectMagic(player, player), 12.0F);
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5F, lift ? 1.6F : 0.5F);
		return true;
	}

	static boolean swap(ServerWorld world, ServerPlayerEntity player, int range) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null) {
			return false;
		}
		Vec3d mine = player.getPos();
		Vec3d theirs = target.getPos();
		target.requestTeleport(mine.x, mine.y, mine.z);
		player.requestTeleport(theirs.x, theirs.y, theirs.z);
		player.fallDistance = 0.0F;
		Fx.sound(world, mine, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.4F);
		Fx.sound(world, theirs, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.4F);
		return true;
	}

	// ------------------------------------------------------------------ time and ghosts

	/** Freezes the whole world except players, using the game's own "tick freeze". */
	static boolean timeStop(ServerWorld world, ServerPlayerEntity player, int ticks) {
		MinecraftServer server = world.getServer();
		if (server.getTickManager().isFrozen()) {
			return false;
		}
		server.getTickManager().setFrozen(true);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, 2.0F, 0.5F);
		player.sendMessage(Text.literal("Time stands still.").formatted(Formatting.AQUA), true);
		Tasks.later(ticks, () -> {
			server.getTickManager().setFrozen(false);
			player.sendMessage(Text.literal("Time moves again.").formatted(Formatting.AQUA), true);
		});
		return true;
	}

	/** Ghost Cloak: spectator mode for a short time, then back to the mode the player had. */
	static boolean ghost(ServerWorld world, ServerPlayerEntity player, int ticks) {
		GameMode previous = player.interactionManager.getGameMode();
		if (previous == GameMode.SPECTATOR) {
			return false;
		}
		UUID id = player.getUuid();
		MinecraftServer server = world.getServer();
		GHOSTS.put(id, previous);
		player.addCommandTag(GHOST_TAG);
		Fx.particles(world, ParticleTypes.POOF, player.getPos().add(0.0, 1.0, 0.0), 30, 0.4, 0.02);
		player.changeGameMode(GameMode.SPECTATOR);
		Tasks.later(ticks, () -> {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(id);
			GameMode mode = GHOSTS.remove(id);
			if (online != null) {
				online.removeCommandTag(GHOST_TAG);
				online.changeGameMode(mode == null ? GameMode.SURVIVAL : mode);
			}
		});
		return true;
	}

	/**
	 * Safety net for the Ghost Cloak: a player who is still marked as a ghost although no ghost timer is running
	 * (for example after the game was closed mid-ghost) is put back into survival mode.
	 */
	static void ghostSafety(ServerPlayerEntity player) {
		if (player.getCommandTags().contains(GHOST_TAG) && !GHOSTS.containsKey(player.getUuid())) {
			player.removeCommandTag(GHOST_TAG);
			player.changeGameMode(GameMode.SURVIVAL);
		}
	}

	// ------------------------------------------------------------------ shooting

	/** A beam that hurts and burns everything on a straight line. */
	static boolean beam(ServerWorld world, ServerPlayerEntity player, int range, float damage) {
		Vec3d start = player.getEyePos();
		BlockHitResult wall = Fx.ray(player, range);
		Vec3d end = wall.getPos();
		Box box = new Box(start, end).expand(1.5);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e.getBoundingBox().expand(0.6).raycast(start, end).isPresent())) {
			living.timeUntilRegen = 0;
			living.damage(world.getDamageSources().indirectMagic(player, player), damage);
			living.setOnFireFor(5.0F);
		}
		Fx.line(world, ParticleTypes.END_ROD, start.add(0.0, -0.2, 0.0), end);
		Fx.sound(world, start, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.7F, 1.6F);
		return true;
	}

	static boolean arrowBurst(ServerWorld world, ServerPlayerEntity player, int count) {
		for (int i = 0; i < count; i++) {
			ArrowEntity arrow = new ArrowEntity(world, player, new ItemStack(Items.ARROW), null);
			arrow.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 3.0F, 7.0F);
			arrow.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
			arrow.setDamage(3.0);
			world.spawnEntity(arrow);
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.2F);
		return true;
	}

	static boolean witherSkull(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		WitherSkullEntity skull = new WitherSkullEntity(world, player, look);
		skull.setPosition(eye.x + look.x, eye.y - 0.2, eye.z + look.z);
		world.spawnEntity(skull);
		Fx.sound(world, eye, SoundEvents.ENTITY_WITHER_SHOOT, 0.7F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ cheats

	static boolean xp(ServerWorld world, ServerPlayerEntity player, int levels) {
		player.addExperienceLevels(levels);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6F, 1.0F);
		return true;
	}

	/** Mirror of Duplication: copies the stack in the other hand. */
	static boolean duplicate(ServerWorld world, ServerPlayerEntity player, Hand mirrorHand) {
		Hand other = mirrorHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack original = player.getStackInHand(other);
		if (original.isEmpty()) {
			player.sendMessage(Text.literal("Hold the item to copy in your other hand."), true);
			return false;
		}
		player.getInventory().offerOrDrop(original.copy());
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
		return true;
	}

	// ------------------------------------------------------------------ building and digging

	static boolean bridge(ServerWorld world, ServerPlayerEntity player, int length) {
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockPos start = player.getBlockPos().down();
		BlockState planks = Blocks.OAK_PLANKS.getDefaultState();
		int placed = 0;
		for (int i = 1; i <= length; i++) {
			for (int w = -1; w <= 1; w++) {
				BlockPos pos = start.offset(forward, i).offset(right, w);
				BlockState state = world.getBlockState(pos);
				if (state.isAir() || state.isReplaceable()) {
					world.setBlockState(pos, planks, Block.NOTIFY_ALL);
					placed++;
				}
			}
		}
		if (placed > 0) {
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_WOOD_PLACE, 1.5F, 0.8F);
		}
		return placed > 0;
	}

	/** The 3x3 square of positions around a centre, flat against the given direction. */
	private static List<BlockPos> square(BlockPos center, Direction direction) {
		List<BlockPos> result = new ArrayList<>(9);
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				switch (direction.getAxis()) {
					case X -> result.add(center.add(0, a, b));
					case Y -> result.add(center.add(a, 0, b));
					case Z -> result.add(center.add(a, b, 0));
				}
			}
		}
		return result;
	}

	private record Saved(BlockPos pos, BlockState state) {
	}

	/** Portable Hole: removes a 3x3 tunnel and puts the blocks back later. */
	static boolean hole(ServerWorld world, ServerPlayerEntity player, int depth, int ticks) {
		BlockHitResult hit = Fx.ray(player, 6.0);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		Vec3d look = player.getRotationVec(1.0F);
		Direction direction = Direction.getFacing(look.x, look.y, look.z);
		List<Saved> saved = new ArrayList<>();
		for (int i = 0; i < depth; i++) {
			for (BlockPos pos : square(hit.getBlockPos().offset(direction, i), direction)) {
				BlockState state = world.getBlockState(pos);
				if (state.isAir() || state.hasBlockEntity() || state.getHardness(world, pos) < 0.0F) {
					continue;
				}
				saved.add(new Saved(pos, state));
				world.setBlockState(pos, Fx.AIR, Fx.QUIET);
			}
		}
		if (saved.isEmpty()) {
			return false;
		}
		Fx.sound(world, hit.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
		Tasks.later(ticks, () -> {
			for (Saved s : saved) {
				BlockState now = world.getBlockState(s.pos());
				if (now.isAir() || now.isReplaceable()) {
					world.setBlockState(s.pos(), s.state(), Block.NOTIFY_ALL);
				}
			}
			Fx.sound(world, hit.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
		});
		return true;
	}

	/** Mining Laser: digs a 3x3 tunnel in the direction the player looks. The drops go to the player. */
	static boolean laserDrill(ServerWorld world, ServerPlayerEntity player, int length) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		Direction direction = Direction.getFacing(look.x, look.y, look.z);
		BlockPos start = BlockPos.ofFloored(eye).offset(direction, 1);
		ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE); // decides what the blocks drop
		int mined = 0;
		for (int i = 0; i < length; i++) {
			for (BlockPos pos : square(start.offset(direction, i), direction)) {
				BlockState state = world.getBlockState(pos);
				if (state.isAir() || state.hasBlockEntity() || state.getHardness(world, pos) < 0.0F) {
					continue;
				}
				if (state.isLiquid()) {
					world.setBlockState(pos, Fx.AIR, Block.NOTIFY_LISTENERS);
					continue;
				}
				List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, player, tool);
				world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
				for (ItemStack drop : drops) {
					player.getInventory().insertStack(drop);
					if (!drop.isEmpty()) {
						Block.dropStack(world, player.getBlockPos(), drop);
					}
				}
				mined++;
			}
		}
		Vec3d end = eye.add(Vec3d.of(direction.getVector()).multiply(length));
		Fx.line(world, ParticleTypes.FLAME, eye.add(0.0, -0.3, 0.0), end);
		Fx.sound(world, eye, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.5F);
		return mined > 0;
	}

	/** Diamond Dowser: finds the nearest diamond ore or ancient debris and tells the player where it is. */
	static boolean dowse(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos center = player.getBlockPos();
		int minY = Math.max(world.getBottomY(), center.getY() - range);
		int maxY = Math.min(world.getTopY() - 1, center.getY() + range);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		BlockPos found = null;
		double best = Double.MAX_VALUE;
		for (int y = minY; y <= maxY; y++) {
			for (int dx = -range; dx <= range; dx++) {
				for (int dz = -range; dz <= range; dz++) {
					pos.set(center.getX() + dx, y, center.getZ() + dz);
					BlockState state = world.getBlockState(pos);
					if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE) || state.isOf(Blocks.ANCIENT_DEBRIS)) {
						double distance = pos.getSquaredDistance(center);
						if (distance < best) {
							best = distance;
							found = pos.toImmutable();
						}
					}
				}
			}
		}
		if (found == null) {
			player.sendMessage(Text.literal("No diamonds or ancient debris within " + range + " blocks."), false);
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.5F);
			return true;
		}
		String name = world.getBlockState(found).isOf(Blocks.ANCIENT_DEBRIS) ? "Ancient debris" : "Diamond ore";
		player.sendMessage(Text.literal(name + " is " + (int) Math.round(Math.sqrt(best)) + " blocks away, at "
				+ found.getX() + " " + found.getY() + " " + found.getZ() + ".").formatted(Formatting.AQUA), false);
		Fx.line(world, ParticleTypes.END_ROD, player.getEyePos(), Vec3d.ofCenter(found));
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
		return true;
	}

	/** Staff of the Wild: full-grown trees at once. */
	static boolean forest(ServerWorld world, Vec3d target) {
		Block[] saplings = {Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.CHERRY_SAPLING};
		Random random = world.random;
		int planted = 0;
		for (int i = 0; i < 16; i++) {
			BlockPos column = BlockPos.ofFloored(target.x + (random.nextDouble() - 0.5) * 20.0, target.y, target.z + (random.nextDouble() - 0.5) * 20.0);
			BlockPos pos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
			if (Math.abs(pos.getY() - target.y) > 10.0 || !world.getBlockState(pos.down()).isIn(DIRT) || !world.getBlockState(pos).isReplaceable()) {
				continue;
			}
			Block sapling = saplings[random.nextInt(saplings.length)];
			if (!(sapling instanceof SaplingBlock saplingBlock)) {
				continue;
			}
			// A sapling in its second growth stage turns into a tree the next time it grows.
			BlockState state = sapling.getDefaultState().with(SaplingBlock.STAGE, 1);
			world.setBlockState(pos, state, Block.NOTIFY_ALL);
			saplingBlock.generate(world, pos, state, random);
			planted++;
		}
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, target.add(0.0, 1.0, 0.0), 60, 5.0, 0.0);
		Fx.sound(world, target, SoundEvents.ITEM_BONE_MEAL_USE, 2.0F, 0.6F);
		return planted > 0;
	}

	/** House in a Box: a 7x7 wooden house with a door, windows, bed, chest, furnace and crafting table. */
	static void buildHouse(ServerWorld world, PlayerEntity player, BlockPos ground) {
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockState planks = Blocks.OAK_PLANKS.getDefaultState();
		BlockState log = Blocks.OAK_LOG.getDefaultState();
		BlockState glass = Blocks.GLASS.getDefaultState();
		// u = left to right (0..6), v = front to back (0..6), h = height (0 = floor .. 4 = roof)
		for (int u = 0; u <= 6; u++) {
			for (int v = 0; v <= 6; v++) {
				for (int h = 0; h <= 4; h++) {
					BlockPos pos = ground.offset(right, u - 3).offset(forward, v).up(h);
					boolean edgeU = u == 0 || u == 6;
					boolean edgeV = v == 0 || v == 6;
					BlockState state;
					if (h == 0 || h == 4) {
						state = planks;
					} else if (edgeU && edgeV) {
						state = log;
					} else if (edgeU || edgeV) {
						boolean window = h == 2 && ((edgeU && v == 3) || (v == 6 && u == 3));
						state = window ? glass : planks;
					} else {
						state = Fx.AIR;
					}
					world.setBlockState(pos, state, Block.NOTIFY_ALL);
				}
			}
		}
		// door in the middle of the front wall
		BlockPos door = ground.offset(forward, 0).up(1);
		BlockState doorState = Blocks.OAK_DOOR.getDefaultState().with(DoorBlock.FACING, forward);
		world.setBlockState(door, doorState.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), Block.NOTIFY_ALL);
		world.setBlockState(door.up(), doorState.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
		// furniture
		BlockPos bedFoot = ground.offset(right, -2).offset(forward, 4).up(1);
		BlockState bed = Blocks.RED_BED.getDefaultState().with(HorizontalFacingBlock.FACING, forward);
		world.setBlockState(bedFoot, bed.with(BedBlock.PART, BedPart.FOOT), Block.NOTIFY_ALL);
		world.setBlockState(bedFoot.offset(forward), bed.with(BedBlock.PART, BedPart.HEAD), Block.NOTIFY_ALL);
		world.setBlockState(ground.offset(right, 2).offset(forward, 5).up(1), Blocks.CRAFTING_TABLE.getDefaultState(), Block.NOTIFY_ALL);
		world.setBlockState(ground.offset(right, 2).offset(forward, 4).up(1),
				Blocks.FURNACE.getDefaultState().with(AbstractFurnaceBlock.FACING, right.getOpposite()), Block.NOTIFY_ALL);
		world.setBlockState(ground.offset(right, 2).offset(forward, 3).up(1),
				Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, right.getOpposite()), Block.NOTIFY_ALL);
		world.setBlockState(ground.offset(right, -2).offset(forward, 1).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
		world.setBlockState(ground.offset(right, 2).offset(forward, 1).up(1), Blocks.TORCH.getDefaultState(), Block.NOTIFY_ALL);
		Fx.sound(world, Vec3d.ofCenter(ground), SoundEvents.BLOCK_WOOD_PLACE, 2.0F, 0.6F);
		Fx.particles(world, ParticleTypes.POOF, Vec3d.ofCenter(ground.offset(forward, 3).up(2)), 80, 3.0, 0.02);
	}

	// ------------------------------------------------------------------ gold and transmutation

	/** Midas Glove on a block: it and up to 26 connected blocks of the same kind become gold blocks. */
	static boolean goldBlocks(ServerWorld world, BlockPos origin) {
		BlockState first = world.getBlockState(origin);
		Block kind = first.getBlock();
		if (first.isAir() || kind == Blocks.GOLD_BLOCK || first.hasBlockEntity() || first.getHardness(world, origin) < 0.0F) {
			return false;
		}
		BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
		List<BlockPos> todo = new ArrayList<>();
		todo.add(origin);
		world.setBlockState(origin, gold, Block.NOTIFY_ALL);
		int changed = 1;
		for (int index = 0; index < todo.size() && changed < 27; index++) {
			BlockPos current = todo.get(index);
			for (Direction direction : Direction.values()) {
				BlockPos next = current.offset(direction);
				if (changed < 27 && world.getBlockState(next).getBlock() == kind) {
					world.setBlockState(next, gold, Block.NOTIFY_ALL);
					todo.add(next);
					changed++;
				}
			}
		}
		Fx.particles(world, ParticleTypes.WAX_ON, Vec3d.ofCenter(origin), 40, 1.2, 0.0);
		Fx.sound(world, Vec3d.ofCenter(origin), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5F, 0.7F);
		return true;
	}

	/** Midas Glove on a mob: the mob is gone and a gold block stands in its place. Bosses and players are safe. */
	static boolean goldify(ServerWorld world, LivingEntity victim) {
		if (victim instanceof PlayerEntity || isBoss(victim)) {
			return false;
		}
		BlockPos pos = victim.getBlockPos();
		BlockState there = world.getBlockState(pos);
		if (there.isAir() || there.isReplaceable()) {
			world.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
		} else {
			world.spawnEntity(new ItemEntity(world, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.GOLD_BLOCK)));
		}
		Fx.particles(world, ParticleTypes.WAX_ON, victim.getPos().add(0.0, 0.5, 0.0), 30, 0.5, 0.0);
		Fx.sound(world, victim.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.5F, 0.7F);
		victim.discard();
		return true;
	}

	/** Philosopher's Stone: forward = more valuable, backward = less valuable. */
	static boolean transmute(ServerWorld world, BlockPos pos, boolean backward) {
		Block block = world.getBlockState(pos).getBlock();
		int index = -1;
		for (int i = 0; i < VALUE_CHAIN.length; i++) {
			if (VALUE_CHAIN[i] == block) {
				index = i;
			}
		}
		if (index < 0) {
			for (Block cheap : CHEAP_BLOCKS) {
				if (cheap == block) {
					index = 0;
				}
			}
		}
		if (index < 0) {
			return false;
		}
		int next = backward ? index - 1 : index + 1;
		if (next < 0 || next >= VALUE_CHAIN.length) {
			return false;
		}
		world.setBlockState(pos, VALUE_CHAIN[next].getDefaultState(), Block.NOTIFY_ALL);
		Fx.particles(world, ParticleTypes.ENCHANT, Vec3d.ofCenter(pos), 40, 0.6, 0.5);
		Fx.sound(world, Vec3d.ofCenter(pos), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, backward ? 0.7F : 1.3F);
		return true;
	}
}
