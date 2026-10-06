package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
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
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Mass destruction: orbital strikes, bombs of every kind, termites, a death ray, a tsunami, a tornado,
 * the snap, a volcano and more. Plus the Hot Potato, the Storm Crown and the Plague Mask, which work without clicking.
 */
public final class Mayhem {
	private static final TagKey<Block> WOODEN = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("mineable/axe"));
	private static final Identifier KAIJU_ID = Identifier.of(ItemCombinator.MOD_ID, "kaiju");
	/** How long a player can carry the Hot Potato before it goes off, in ticks. */
	public static final int POTATO_FUSE = 300;

	/** How long each player has carried a Hot Potato without a break. */
	private static final Map<UUID, Integer> POTATO = new HashMap<>();

	private static final EntityType<?>[] KAIJU = {
			EntityType.ZOMBIE, EntityType.SPIDER, EntityType.SLIME, EntityType.SILVERFISH, EntityType.CAVE_SPIDER,
			EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.ENDERMITE, EntityType.CHICKEN, EntityType.RABBIT
	};

	private record Saved(BlockPos pos, BlockState state) {
	}

	private Mayhem() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Mayhem::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> POTATO.clear());
	}

	// ------------------------------------------------------------------ things that work without clicking

	private static void tick(MinecraftServer server) {
		int ticks = server.getTicks();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			try {
				tickPotato(player);
				if (!player.isAlive() || player.isSpectator()) {
					continue;
				}
				if (ticks % 60 == 0 && PassiveAbilities.has(player, traits -> traits.stormCrown)) {
					stormStrike(player);
				}
				if (ticks % 20 == 0 && PassiveAbilities.has(player, traits -> traits.plagueAura)) {
					plague(player);
				}
			} catch (Throwable t) {
				ItemCombinator.error("Mayhem passive ability failed", t);
			}
		}
	}

	/** How many ticks this player has carried a Hot Potato. 0 if none. The tests read this. */
	public static int potatoTime(PlayerEntity player) {
		return POTATO.getOrDefault(player.getUuid(), 0);
	}

	private static void tickPotato(ServerPlayerEntity player) {
		if (!player.isAlive() || player.isSpectator() || player.getInventory().count(ComboItems.HOT_POTATO) == 0) {
			POTATO.remove(player.getUuid());
			return;
		}
		int held = POTATO.merge(player.getUuid(), 1, Integer::sum);
		int left = POTATO_FUSE - held;
		ServerWorld world = player.getServerWorld();
		if (left > 0 && left % 20 == 0) {
			int seconds = left / 20;
			player.sendMessage(Text.literal("Hot Potato! " + seconds + " ... throw it away!").formatted(seconds <= 5 ? Formatting.RED : Formatting.GOLD), true);
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_HAT, 1.0F, 0.6F + (15 - Math.min(15, seconds)) * 0.1F);
		}
		if (left <= 0) {
			POTATO.remove(player.getUuid());
			for (int i = 0; i < player.getInventory().size(); i++) {
				ItemStack stack = player.getInventory().getStack(i);
				if (stack.isOf(ComboItems.HOT_POTATO)) {
					stack.decrement(1);
					break;
				}
			}
			player.sendMessage(Text.literal("The Hot Potato went off in your pocket.").formatted(Formatting.RED), true);
			Fx.boom(world, null, player.getPos().add(0.0, 1.0, 0.0), 3.0F, true, true);
		}
	}

	/** Storm Crown: a lightning bolt hits the nearest monster. It only looks like real lightning, so it never hits the wearer. */
	private static void stormStrike(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		LivingEntity target = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(16.0),
				e -> e.isAlive() && e instanceof Monster)) {
			double distance = living.squaredDistanceTo(player);
			if (distance < best) {
				best = distance;
				target = living;
			}
		}
		if (target == null) {
			return;
		}
		Fx.fakeBolt(world, target.getPos());
		target.damage(world.getDamageSources().indirectMagic(player, player), 10.0F);
		target.setOnFireFor(4.0F);
	}

	/** Plague Mask: everything alive near the wearer gets poison and wither for a moment. Tame pets are spared. */
	private static void plague(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(6.0),
				e -> e != player && e.isAlive() && !(e instanceof ArmorStandEntity) && !(e instanceof TameableEntity pet && pet.isTamed()))) {
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 1), player);
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0), player);
			Fx.particles(world, ParticleTypes.SNEEZE, living.getPos().add(0.0, living.getHeight() * 0.7, 0.0), 4, 0.3, 0.02);
		}
	}

	// ------------------------------------------------------------------ bombs

	/** Orbital Strike Remote: a marker for two seconds, then a beam from space burns a deep hole. */
	static boolean orbitalStrike(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d target = Fx.lookPoint(world, player, range);
		BlockPos ground = BlockPos.ofFloored(target);
		player.sendMessage(Text.literal("Orbital strike incoming. Get away from the light!").formatted(Formatting.RED), true);
		Fx.sound(world, target, SoundEvents.BLOCK_BEACON_ACTIVATE, 4.0F, 0.6F);
		int aim = 40;
		Tasks.repeat(aim / 2, 2, i -> {
			double radius = 4.0 - i * 0.15;
			for (int k = 0; k < 8; k++) {
				double angle = i * 0.4 + k * Math.PI / 4.0;
				world.spawnParticles(ParticleTypes.FLAME, target.x + Math.cos(angle) * radius, target.y + 0.2, target.z + Math.sin(angle) * radius,
						1, 0.0, 0.0, 0.0, 0.0);
			}
			Fx.line(world, ParticleTypes.END_ROD, target.add(0.0, 40.0, 0.0), target);
		});
		int depth = 44;
		int perTick = 4;
		Tasks.later(aim, () -> {
			Fx.sound(world, target, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 8.0F, 0.5F);
			Fx.fakeBolt(world, target);
			Tasks.repeat(depth / perTick, 1, step -> {
				for (int j = 0; j < perTick; j++) {
					int y = ground.getY() + 4 - step * perTick - j;
					if (y <= world.getBottomY()) {
						return;
					}
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							if (dx * dx + dz * dz > 17) {
								continue;
							}
							BlockPos pos = new BlockPos(ground.getX() + dx, y, ground.getZ() + dz);
							BlockState state = world.getBlockState(pos);
							if (!state.isAir() && state.getHardness(world, pos) >= 0.0F) {
								world.setBlockState(pos, Fx.AIR, Fx.QUIET);
							}
						}
					}
				}
				Vec3d now = new Vec3d(target.x, ground.getY() + 4 - step * perTick, target.z);
				Box column = new Box(target.x - 4.5, now.y - 6.0, target.z - 4.5, target.x + 4.5, target.y + 40.0, target.z + 4.5);
				for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, column, e -> e.isAlive())) {
					living.timeUntilRegen = 0;
					living.damage(world.getDamageSources().indirectMagic(player, player), 40.0F);
					living.setOnFireFor(8.0F);
				}
				Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, now, 2, 1.5, 0.0);
				Fx.particles(world, ParticleTypes.FLAME, now, 60, 3.0, 0.05);
				if (step % 3 == 0) {
					Fx.boom(world, null, now, 3.0F, true, true);
				}
			});
		});
		return true;
	}

	/** Cluster Bomb: one bomb in the air, eight lit TNT blocks on the ground. */
	static boolean clusterBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 30, at -> {
			Random random = world.random;
			Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 1.6F);
			Fx.particles(world, ParticleTypes.EXPLOSION, at, 3, 0.5, 0.0);
			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4.0 + random.nextDouble() * 0.5;
				TntEntity tnt = new TntEntity(world, at.x, at.y + 0.5, at.z, player);
				tnt.setVelocity(Math.cos(angle) * 0.6, 0.55 + random.nextDouble() * 0.3, Math.sin(angle) * 0.6);
				tnt.setFuse(25 + random.nextInt(25));
				world.spawnEntity(tnt);
			}
		});
		return true;
	}

	static boolean gravityGrenade(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 40, at -> gravityBurst(world, at, 5));
		return true;
	}

	/** Every block in a ball that is not too hard flies into the sky and comes down somewhere else. Everything alive flies too. */
	static void gravityBurst(ServerWorld world, Vec3d at, int radius) {
		Random random = world.random;
		BlockPos center = BlockPos.ofFloored(at);
		int thrown = 0;
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
			if (thrown >= 220) {
				break;
			}
			if (pos.getSquaredDistance(center) > radius * radius) {
				continue;
			}
			BlockState state = world.getBlockState(pos);
			if (!Fx.loose(world, pos, state)) {
				continue;
			}
			Fx.fling(world, pos.toImmutable(), state, (random.nextDouble() - 0.5) * 0.6, 0.9 + random.nextDouble() * 0.9, (random.nextDouble() - 0.5) * 0.6);
			thrown++;
		}
		for (Entity entity : world.getOtherEntities(null, Box.of(at, radius * 3.0, radius * 3.0, radius * 3.0),
				e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
			entity.addVelocity(0.0, 1.6, 0.0);
			entity.velocityModified = true;
		}
		Fx.sound(world, at, SoundEvents.BLOCK_BEACON_ACTIVATE, 3.0F, 2.0F);
		Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.6F);
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, at, 200, radius * 0.5, 0.3);
	}

	/** Scatter Bomb: everything alive and every dropped item near the blast is teleported somewhere random. */
	static boolean scatterBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 30, at -> {
			Fx.sound(world, at, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 3.0F, 0.6F);
			Fx.particles(world, ParticleTypes.PORTAL, at, 200, 2.0, 0.6);
			for (Entity entity : world.getOtherEntities(null, Box.of(at, 20.0, 20.0, 20.0),
					e -> e.isAlive() && !e.isSpectator() && !(e instanceof EnderDragonEntity) && (e instanceof LivingEntity || e instanceof ItemEntity))) {
				BlockPos spot = Fx.standingSpot(world, entity.getPos(), 32.0);
				if (spot != null) {
					entity.requestTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
					entity.fallDistance = 0.0F;
					Fx.particles(world, ParticleTypes.PORTAL, Vec3d.ofBottomCenter(spot).add(0.0, 1.0, 0.0), 30, 0.4, 0.3);
				}
			}
		});
		return true;
	}

	/** Gold Bomb: blocks and mobs near the blast become gold blocks. */
	static boolean goldBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 40, at -> {
			BlockPos center = BlockPos.ofFloored(at);
			BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
			int changed = 0;
			for (BlockPos pos : BlockPos.iterateOutwards(center, 5, 5, 5)) {
				if (changed >= 600) {
					break;
				}
				if (pos.getSquaredDistance(center) > 25) {
					continue;
				}
				BlockState state = world.getBlockState(pos);
				if (Fx.loose(world, pos, state) && !state.isOf(Blocks.GOLD_BLOCK)) {
					world.setBlockState(pos.toImmutable(), gold, Block.NOTIFY_LISTENERS);
					changed++;
				}
			}
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, Box.of(at, 10.0, 10.0, 10.0), e -> e.isAlive())) {
				Spells.goldify(world, living);
			}
			Fx.particles(world, ParticleTypes.WAX_ON, at, 200, 3.0, 0.2);
			Fx.particles(world, ParticleTypes.EXPLOSION, at, 4, 1.0, 0.0);
			Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 1.4F);
			Fx.sound(world, at, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 3.0F, 0.6F);
		});
		return true;
	}

	/** Sheep Bomb: sixteen sheep in random colours burst out. One of them is jeb_ and shines in every colour. */
	static boolean sheepBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 30, at -> {
			Random random = world.random;
			DyeColor[] colors = DyeColor.values();
			for (int i = 0; i < 16; i++) {
				Entity entity = Spells.spawn(world, EntityType.SHEEP, at.x, at.y + 0.5, at.z);
				if (entity instanceof SheepEntity sheep) {
					sheep.setColor(colors[random.nextInt(colors.length)]);
					if (i == 0) {
						sheep.setCustomName(Text.literal("jeb_"));
					}
					double angle = random.nextDouble() * Math.PI * 2.0;
					sheep.setVelocity(Math.cos(angle) * 0.7, 0.6 + random.nextDouble() * 0.5, Math.sin(angle) * 0.7);
					sheep.velocityModified = true;
				}
			}
			Fx.particles(world, ParticleTypes.EXPLOSION, at, 2, 0.5, 0.0);
			Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 1.8F);
			Fx.sound(world, at, SoundEvents.ENTITY_SHEEP_AMBIENT, 3.0F, 1.0F);
		});
		return true;
	}

	/** Cobweb Grenade: cobwebs fill the air around the blast. */
	static boolean cobwebBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 30, at -> {
			Random random = world.random;
			BlockPos center = BlockPos.ofFloored(at);
			for (BlockPos pos : BlockPos.iterateOutwards(center, 4, 3, 4)) {
				if (pos.getSquaredDistance(center) <= 16 && world.getBlockState(pos).isAir() && random.nextFloat() < 0.55F) {
					world.setBlockState(pos.toImmutable(), Blocks.COBWEB.getDefaultState(), Block.NOTIFY_ALL);
				}
			}
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, Box.of(at, 12.0, 8.0, 12.0), e -> e.isAlive())) {
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
			}
			Fx.particles(world, ParticleTypes.POOF, at, 60, 2.0, 0.05);
			Fx.sound(world, at, SoundEvents.ENTITY_SPIDER_AMBIENT, 2.0F, 0.6F);
		});
		return true;
	}

	/** Hive Grenade: ten angry bees. */
	static boolean hiveGrenade(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.1F, 30, at -> bees(world, at, 10));
		return true;
	}

	/** Angry bees that go for the nearest living thing that is not a bee. */
	static void bees(ServerWorld world, Vec3d at, int count) {
		Random random = world.random;
		Fx.sound(world, at, SoundEvents.BLOCK_BEEHIVE_EXIT, 2.0F, 0.8F);
		Fx.particles(world, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BEEHIVE.getDefaultState()), at, 30, 0.4, 0.1);
		for (int i = 0; i < count; i++) {
			Entity entity = Spells.spawn(world, EntityType.BEE, at.x + (random.nextDouble() - 0.5) * 2.0, at.y + 0.5, at.z + (random.nextDouble() - 0.5) * 2.0);
			if (!(entity instanceof BeeEntity bee)) {
				continue;
			}
			LivingEntity target = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, bee.getBoundingBox().expand(16.0),
					e -> e.isAlive() && !(e instanceof BeeEntity) && !e.isSpectator() && !(e instanceof ArmorStandEntity))) {
				double distance = living.squaredDistanceTo(bee);
				if (distance < best) {
					best = distance;
					target = living;
				}
			}
			if (target != null) {
				bee.setAngerTime(400 + random.nextInt(200));
				bee.setAngryAt(target.getUuid());
				bee.setTarget(target);
			}
		}
	}

	/** Hot Potato: throws it. It explodes three seconds later where it lies, unless someone picks it up. */
	static boolean throwPotato(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		ItemEntity potato = new ItemEntity(world, eye.x + look.x, eye.y - 0.2, eye.z + look.z, new ItemStack(ComboItems.HOT_POTATO));
		potato.setVelocity(look.multiply(1.1));
		potato.setPickupDelay(20);
		world.spawnEntity(potato);
		Fx.sound(world, eye, SoundEvents.ENTITY_SNOWBALL_THROW, 1.0F, 0.6F);
		Tasks.later(60, () -> {
			if (potato.isAlive()) {
				Vec3d at = potato.getPos();
				potato.discard();
				Fx.boom(world, null, at, 3.0F, true, true);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ disasters

	private static boolean isWood(BlockState state) {
		return state.isIn(WOODEN) && !state.hasBlockEntity();
	}

	/** Termite Jar: the termites eat the wooden block the player looks at, and every wooden block connected to it. */
	static boolean termites(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK || !isWood(world.getBlockState(hit.getBlockPos()))) {
			player.sendMessage(Text.literal("Termites only eat wood. Point the jar at something wooden."), true);
			return false;
		}
		infest(world, hit.getBlockPos(), 1500);
		Fx.sound(world, hit.getPos(), SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 1.2F);
		return true;
	}

	/** Eats up to six wooden blocks per tick, always the ones next to what was eaten before. */
	static void infest(ServerWorld world, BlockPos start, int limit) {
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		frontier.add(start.toImmutable());
		seen.add(start.toImmutable());
		int[] eaten = {0};
		Runnable[] chew = new Runnable[1];
		chew[0] = () -> {
			int bites = 0;
			while (bites < 6 && !frontier.isEmpty() && eaten[0] < limit) {
				BlockPos pos = frontier.poll();
				BlockState state = world.getBlockState(pos);
				if (!isWood(state)) {
					continue;
				}
				world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
						10, 0.3, 0.3, 0.3, 0.05);
				world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
				eaten[0]++;
				bites++;
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos next = pos.add(dx, dy, dz);
							if ((dx != 0 || dy != 0 || dz != 0) && seen.add(next) && isWood(world.getBlockState(next))) {
								frontier.add(next);
							}
						}
					}
				}
			}
			if (bites > 0 && world.random.nextInt(3) == 0) {
				BlockPos last = frontier.peek();
				if (last != null) {
					Fx.sound(world, Vec3d.ofCenter(last), SoundEvents.BLOCK_WOOD_BREAK, 0.8F, 0.6F + world.random.nextFloat() * 0.6F);
				}
			}
			if (!frontier.isEmpty() && eaten[0] < limit) {
				Tasks.later(1, chew[0]);
			}
		};
		chew[0].run();
	}

	/** Solar Death Ray: a beam for a few seconds that follows the player's view and melts what it touches. */
	static boolean deathRay(ServerWorld world, ServerPlayerEntity player, int range, int ticks) {
		Map<BlockPos, Integer> heat = new HashMap<>();
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5F, 1.8F);
		IntConsumer burn = i -> {
			if (!player.isAlive() || player.getWorld() != world || player.isSpectator()) {
				return;
			}
			Vec3d eye = player.getEyePos();
			BlockHitResult hit = Fx.rayWithFluids(player, range);
			Vec3d end = hit.getPos();
			Fx.line(world, ParticleTypes.FLAME, eye.add(0.0, -0.25, 0.0), end);
			Box box = new Box(eye, end).expand(1.0);
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box,
					e -> e != player && e.isAlive() && e.getBoundingBox().expand(0.4).raycast(eye, end).isPresent())) {
				living.timeUntilRegen = 0;
				living.damage(world.getDamageSources().indirectMagic(player, player), 2.0F);
				living.setOnFireFor(4.0F);
			}
			if (hit.getType() == HitResult.Type.BLOCK) {
				melt(world, hit.getBlockPos(), hit.getSide(), heat);
			}
			if (i % 6 == 0) {
				Fx.sound(world, end, SoundEvents.BLOCK_FIRE_AMBIENT, 1.5F, 1.5F);
			}
		};
		burn.accept(0);
		Tasks.repeat(ticks - 1, 1, i -> burn.accept(i + 1));
		return true;
	}

	/** What the death ray does to one block. Every tick on the same block makes it hotter. */
	private static void melt(ServerWorld world, BlockPos pos, Direction side, Map<BlockPos, Integer> heat) {
		BlockState state = world.getBlockState(pos);
		Vec3d at = Vec3d.ofCenter(pos);
		Fx.particles(world, ParticleTypes.LARGE_SMOKE, at, 2, 0.3, 0.01);
		if (state.isOf(Blocks.WATER)) {
			world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
			Fx.particles(world, ParticleTypes.CLOUD, at, 8, 0.4, 0.02);
			return;
		}
		if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE) || state.isOf(Blocks.SNOW_BLOCK)) {
			world.setBlockState(pos, world.getDimension().ultrawarm() ? Fx.AIR : Blocks.WATER.getDefaultState(), Block.NOTIFY_ALL);
			return;
		}
		if (state.isOf(Blocks.SNOW)) {
			world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
			return;
		}
		if (state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)) {
			world.setBlockState(pos, Blocks.GLASS.getDefaultState(), Block.NOTIFY_ALL);
			return;
		}
		float hardness = state.getHardness(world, pos);
		if (state.isAir() || state.isOf(Blocks.LAVA) || hardness < 0.0F || hardness >= 50.0F || state.hasBlockEntity()) {
			return;
		}
		int hot = heat.merge(pos.toImmutable(), 1, Integer::sum);
		BlockPos front = pos.offset(side);
		if (hot == 3 && world.getBlockState(front).isAir() && AbstractFireBlock.canPlaceAt(world, front, side)) {
			world.setBlockState(front, AbstractFireBlock.getState(world, front), Block.NOTIFY_ALL);
		}
		if (hot == 6) {
			if (isWood(state) || state.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("wool"))) || state.isIn(TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("leaves")))) {
				world.setBlockState(pos, AbstractFireBlock.getState(world, pos), Block.NOTIFY_ALL);
			} else {
				world.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
			}
			Fx.particles(world, ParticleTypes.LAVA, at, 4, 0.3, 0.0);
		} else if (hot == 12 && state.isOf(Blocks.MAGMA_BLOCK)) {
			world.setBlockState(pos, Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
			Fx.sound(world, at, SoundEvents.BLOCK_LAVA_POP, 1.0F, 1.0F);
		}
	}

	/** Tsunami Horn: a wall of water rolls away from the player. It pushes everything along and washes small things away. */
	static boolean tsunami(ServerWorld world, ServerPlayerEntity player, int length) {
		Direction forward = player.getHorizontalFacing();
		Direction right = forward.rotateYClockwise();
		BlockPos origin = player.getBlockPos();
		boolean steam = world.getDimension().ultrawarm();
		List<BlockPos> last = new ArrayList<>();
		BlockState water = Blocks.WATER.getDefaultState();
		IntConsumer step = i -> {
			for (BlockPos pos : last) {
				if (world.getBlockState(pos).isOf(Blocks.WATER)) {
					world.setBlockState(pos, Fx.AIR, Fx.QUIET);
				}
			}
			last.clear();
			if (i >= length) {
				return;
			}
			int height = Math.min(6, 2 + i / 3);
			if (i > length - 6) {
				height = Math.max(1, height - (i - (length - 6)));
			}
			BlockPos front = origin.offset(forward, 2 + i);
			for (int w = -6; w <= 6; w++) {
				BlockPos column = front.offset(right, w);
				for (int h = 0; h < height; h++) {
					BlockPos pos = column.up(h);
					BlockState state = world.getBlockState(pos);
					if (!state.isAir()) {
						boolean small = state.isReplaceable() || state.getHardness(world, pos) <= 0.2F;
						if (!small || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
							continue;
						}
						world.breakBlock(pos, true);
					}
					if (steam) {
						world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.02);
					} else {
						world.setBlockState(pos, water, Fx.QUIET);
						last.add(pos.toImmutable());
					}
				}
			}
			Vec3d a = Vec3d.of(front.offset(right, -7));
			Vec3d b = Vec3d.of(front.offset(right, 8).up(height + 1));
			for (Entity entity : world.getOtherEntities(null, new Box(a, b).expand(1.5),
					e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
				entity.setVelocity(forward.getOffsetX() * 1.1, 0.35, forward.getOffsetZ() * 1.1);
				entity.velocityModified = true;
				entity.fallDistance = 0.0F;
				entity.extinguish();
				if (entity instanceof LivingEntity living && living != player) {
					living.damage(world.getDamageSources().drown(), 1.0F);
				}
			}
			Vec3d crest = Vec3d.ofCenter(front.up(height));
			Fx.particles(world, ParticleTypes.SPLASH, crest, 60, 5.0, 0.2);
			if (i % 4 == 0) {
				Fx.sound(world, crest, SoundEvents.ENTITY_GENERIC_SPLASH, 3.0F, 0.5F);
			}
		};
		step.accept(0);
		Tasks.repeat(length, 2, i -> step.accept(i + 1));
		return true;
	}

	/** Tornado in a Bottle: a tornado touches down where the player looks and wanders away from the player. */
	static boolean tornado(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d start = Fx.lookPoint(world, player, range);
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d heading = new Vec3d(look.x, 0.0, look.z);
		heading = heading.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : heading.normalize();
		tornadoAt(world, start, heading, 300, 7.0);
		player.sendMessage(Text.literal("Tornado! Get out of its way.").formatted(Formatting.GRAY), true);
		return true;
	}

	/**
	 * A tornado for the given number of ticks. Everything alive and every dropped item within the radius is spun around,
	 * lifted, and sometimes thrown out of the top. The ground under it is torn up.
	 */
	static void tornadoAt(ServerWorld world, Vec3d start, Vec3d heading, int ticks, double radius) {
		Vec3d[] at = {start};
		Vec3d[] drift = {heading.multiply(0.15)};
		int[] ripped = {0};
		IntConsumer step = i -> {
			Random random = world.random;
			if (i % 20 == 0) {
				double turn = (random.nextDouble() - 0.5) * 0.8;
				Vec3d d = drift[0];
				drift[0] = new Vec3d(d.x * Math.cos(turn) - d.z * Math.sin(turn), 0.0, d.x * Math.sin(turn) + d.z * Math.cos(turn));
			}
			Vec3d next = at[0].add(drift[0]);
			// Over a hole (often one it tore itself) the tornado keeps its height. It must never creep upwards.
			BlockPos ground = Fx.groundNear(world, next.x, next.z, next.y, 5);
			Vec3d c = new Vec3d(next.x, ground.getY(), next.z);
			at[0] = c;
			// the funnel: wider at the top
			for (int h = 0; h < 20; h += 2) {
				double r = 0.6 + h * 0.3;
				for (int k = 0; k < 3; k++) {
					double angle = random.nextDouble() * Math.PI * 2.0;
					world.spawnParticles(ParticleTypes.CLOUD, c.x + Math.cos(angle) * r, c.y + h, c.z + Math.sin(angle) * r, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			world.spawnParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5, c.z, 6, 1.0, 0.2, 1.0, 0.02);
			// spin, lift and throw
			for (Entity entity : world.getOtherEntities(null, Box.of(c.add(0.0, 10.0, 0.0), radius * 2.0, 26.0, radius * 2.0),
					e -> e.isAlive() && !e.isSpectator() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
				Vec3d rel = entity.getPos().subtract(c);
				double flat = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
				if (flat > radius || rel.y < -2.0 || rel.y > 22.0) {
					continue;
				}
				Vec3d inward = flat > 0.01 ? new Vec3d(-rel.x / flat, 0.0, -rel.z / flat) : Vec3d.ZERO;
				Vec3d around = new Vec3d(-inward.z, 0.0, inward.x);
				Vec3d push;
				if (rel.y > 16.0 && random.nextInt(20) == 0) {
					push = inward.multiply(-1.8).add(0.0, 0.6, 0.0);
				} else {
					push = around.multiply(0.55).add(inward.multiply(flat > 2.5 ? 0.18 : -0.05)).add(0.0, rel.y > 16.0 ? -0.05 : 0.32, 0.0);
				}
				entity.setVelocity(entity.getVelocity().multiply(0.5).add(push));
				entity.velocityModified = true;
			}
			// tear up the ground
			for (int k = 0; k < 2 && ripped[0] < 500; k++) {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double distance = random.nextDouble() * 3.0;
				BlockPos top = Fx.surface(world, c.x + Math.cos(angle) * distance, c.z + Math.sin(angle) * distance, c.y + 1.0).down();
				if (Math.abs(top.getY() - c.y) > 4) {
					continue;
				}
				BlockState state = world.getBlockState(top);
				if (Fx.loose(world, top, state) && state.getHardness(world, top) < 2.5F) {
					Fx.fling(world, top, state, -Math.sin(angle) * 0.5, 1.0 + random.nextDouble() * 0.5, Math.cos(angle) * 0.5);
					ripped[0]++;
				}
			}
			if (i % 15 == 0) {
				Fx.sound(world, c, SoundEvents.ITEM_ELYTRA_FLYING, 3.0F, 0.5F);
			}
		};
		step.accept(0);
		Tasks.repeat(ticks - 1, 1, i -> step.accept(i + 1));
	}

	/** Snap Gauntlet: half of everything alive nearby turns to dust. Players and the Ender Dragon are spared. */
	static boolean snap(ServerWorld world, ServerPlayerEntity player, int range) {
		List<LivingEntity> all = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range),
				e -> e.isAlive() && !(e instanceof PlayerEntity) && !(e instanceof EnderDragonEntity) && !(e instanceof ArmorStandEntity)));
		if (all.isEmpty()) {
			player.sendMessage(Text.literal("Nothing here to snap away."), true);
			return false;
		}
		Random random = world.random;
		for (int i = all.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			LivingEntity swap = all.get(i);
			all.set(i, all.get(j));
			all.set(j, swap);
		}
		int victims = (all.size() + 1) / 2;
		for (int i = 0; i < victims; i++) {
			LivingEntity victim = all.get(i);
			Runnable dust = () -> {
				if (!victim.isAlive()) {
					return;
				}
				Vec3d at = victim.getPos().add(0.0, victim.getHeight() * 0.5, 0.0);
				Fx.particles(world, ParticleTypes.ASH, at, 80, 0.4, 0.02);
				Fx.particles(world, ParticleTypes.SMOKE, at, 20, 0.3, 0.02);
				Fx.sound(world, at, SoundEvents.BLOCK_SAND_BREAK, 1.0F, 0.6F);
				victim.discard();
			};
			if (i == 0) {
				dust.run();
			} else {
				Tasks.later(2 + random.nextInt(60), dust);
			}
		}
		player.sendMessage(Text.literal("Perfectly balanced. " + victims + " of " + all.size() + " turned to dust.").formatted(Formatting.GOLD), true);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 2.0F, 0.5F);
		return true;
	}

	/** Creeper Cannon: shoots a lit creeper. A charged one makes a much bigger hole. */
	static boolean creeperCannon(ServerWorld world, ServerPlayerEntity player, boolean charged) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		Entity entity = Spells.spawn(world, EntityType.CREEPER, eye.x + look.x * 1.5, eye.y - 0.6 + look.y * 1.5, eye.z + look.z * 1.5);
		if (!(entity instanceof CreeperEntity creeper)) {
			return false;
		}
		if (charged) {
			LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
			if (bolt != null) {
				creeper.onStruckByLightning(world, bolt);
			}
			creeper.extinguish();
			creeper.setHealth(creeper.getMaxHealth());
		}
		creeper.setVelocity(look.x * 1.6, look.y * 1.6 + 0.25, look.z * 1.6);
		creeper.velocityModified = true;
		creeper.ignite();
		Fx.sound(world, eye, SoundEvents.ENTITY_CREEPER_PRIMED, 1.5F, 1.0F);
		Fx.sound(world, eye, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5F, 0.6F);
		Fx.particles(world, ParticleTypes.POOF, eye.add(look.multiply(1.5)), 10, 0.2, 0.05);
		return true;
	}

	/** The Floor Is Lava: the ground around the player turns to lava for a while, except the block under the player. */
	static boolean floorIsLava(ServerWorld world, ServerPlayerEntity player, int radius, int ticks) {
		BlockPos feet = player.getBlockPos();
		List<Saved> saved = new ArrayList<>();
		BlockState lava = Blocks.LAVA.getDefaultState();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius || (dx == 0 && dz == 0)) {
					continue;
				}
				for (int dy = 1; dy >= -4; dy--) {
					BlockPos pos = feet.add(dx, dy, dz);
					BlockState state = world.getBlockState(pos);
					if (!state.isSolidBlock(world, pos)) {
						continue;
					}
					BlockPos above = pos.up();
					if (world.getBlockState(above).isSolidBlock(world, above)) {
						break; // under a wall: leave it
					}
					if (!state.hasBlockEntity() && state.getHardness(world, pos) >= 0.0F) {
						saved.add(new Saved(pos.toImmutable(), state));
						world.setBlockState(pos, lava, Fx.QUIET);
					}
					break;
				}
			}
		}
		if (saved.isEmpty()) {
			return false;
		}
		player.sendMessage(Text.literal("The floor is lava! Do not move.").formatted(Formatting.GOLD), true);
		int[] melody = {0, 4, 7, 12, 7, 4, 0, 4, 7, 12, 16, 12};
		Tasks.repeat(ticks / 4, 4, i -> {
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, 1.5F, (float) Math.pow(2.0, (melody[i % melody.length] - 12) / 12.0));
			if (i % 5 == 0) {
				Fx.particles(world, ParticleTypes.LAVA, player.getPos(), 10, radius * 0.4, 0.0);
			}
		});
		Tasks.later(ticks, () -> {
			for (Saved s : saved) {
				if (world.getBlockState(s.pos()).isOf(Blocks.LAVA)) {
					world.setBlockState(s.pos(), s.state(), Block.NOTIFY_ALL);
				}
			}
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_LAVA_EXTINGUISH, 2.0F, 1.0F);
		});
		return true;
	}

	/** Pocket Volcano: at least 10 blocks away from the player. */
	static boolean volcano(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d target = Fx.lookPoint(world, player, range);
		if (Math.hypot(target.x - player.getX(), target.z - player.getZ()) < 10.0) {
			Vec3d look = player.getRotationVec(1.0F);
			Vec3d flat = new Vec3d(look.x, 0.0, look.z);
			flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
			target = Vec3d.ofBottomCenter(Fx.groundNear(world, player.getX() + flat.x * 12.0, player.getZ() + flat.z * 12.0, player.getY(), 8));
		}
		volcanoAt(world, BlockPos.ofFloored(target), 7, 10, 200);
		player.sendMessage(Text.literal("The ground starts to rumble...").formatted(Formatting.GOLD), true);
		return true;
	}

	/** A volcano: a cone of basalt, blackstone and magma that grows out of the ground, then erupts. */
	static void volcanoAt(ServerWorld world, BlockPos base, int radius, int height, int eruptTicks) {
		IntConsumer layer = h -> {
			Random random = world.random;
			double r = radius * (1.0 - (double) h / (height + 1)) + 1.0;
			int ri = (int) Math.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) {
				for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > r) {
						continue;
					}
					BlockPos pos = base.add(dx, h, dz);
					if (!Fx.canFill(world.getBlockState(pos))) {
						continue;
					}
					boolean crater = h >= height - 2 && d < 1.8;
					BlockState state;
					if (crater) {
						state = Blocks.LAVA.getDefaultState();
					} else {
						float pick = random.nextFloat();
						state = (pick < 0.4F ? Blocks.BASALT : pick < 0.8F ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK).getDefaultState();
					}
					world.setBlockState(pos, state, crater ? Block.NOTIFY_ALL : Block.NOTIFY_LISTENERS);
				}
			}
			// what stands on the growing cone rides up with it
			Box box = new Box(base.getX() - r, base.getY() + h - 1.0, base.getZ() - r, base.getX() + r + 1.0, base.getY() + h + 1.0, base.getZ() + r + 1.0);
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator())) {
				living.requestTeleport(living.getX(), base.getY() + h + 1.0, living.getZ());
			}
			Fx.sound(world, Vec3d.ofCenter(base.up(h)), SoundEvents.BLOCK_BASALT_PLACE, 2.0F, 0.5F);
			Fx.particles(world, ParticleTypes.LARGE_SMOKE, Vec3d.ofCenter(base.up(h)), 20, r * 0.5, 0.02);
		};
		layer.accept(0);
		Tasks.repeat(height - 1, 2, i -> layer.accept(i + 1));
		BlockPos crater = base.up(height);
		Tasks.later(height * 2 + 2, () -> {
			Fx.sound(world, Vec3d.ofCenter(crater), SoundEvents.ENTITY_GENERIC_EXPLODE, 6.0F, 0.4F);
			Tasks.repeat(eruptTicks / 2, 2, i -> {
				Random random = world.random;
				Vec3d top = Vec3d.ofBottomCenter(crater).add(0.0, 1.0, 0.0);
				BlockPos spot = crater.up(2 + random.nextInt(2));
				if (world.getBlockState(spot).isAir()) {
					FallingBlockEntity bomb = Fx.fling(world, spot, Blocks.MAGMA_BLOCK.getDefaultState(),
							(random.nextDouble() - 0.5) * 1.2, 1.1 + random.nextDouble() * 0.8, (random.nextDouble() - 0.5) * 1.2);
					bomb.setHurtEntities(2.0F, 20);
				}
				Fx.particles(world, ParticleTypes.LAVA, top, 12, 0.6, 0.2);
				Fx.particles(world, ParticleTypes.LARGE_SMOKE, top.add(0.0, 2.0, 0.0), 8, 0.8, 0.05);
				if (i % 10 == 0) {
					double angle = random.nextDouble() * Math.PI * 2.0;
					double distance = 6.0 + random.nextDouble() * 8.0;
					BlockPos blast = Fx.groundNear(world, crater.getX() + Math.cos(angle) * distance, crater.getZ() + Math.sin(angle) * distance, base.getY(), 10);
					Fx.boom(world, null, Vec3d.ofBottomCenter(blast), 2.0F, true, true);
				}
				for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, Box.of(top, 8.0, 8.0, 8.0), e -> e.isAlive())) {
					living.setOnFireFor(4.0F);
				}
			});
		});
	}

	/** Mjolnir: twelve lightning bolts in a ring, and a shock wave that throws everything away. */
	static boolean lightningRing(ServerWorld world, ServerPlayerEntity player, int radius, float damage) {
		Vec3d here = player.getPos();
		for (int i = 0; i < 12; i++) {
			double angle = i * Math.PI / 6.0;
			BlockPos ground = Fx.groundNear(world, here.x + Math.cos(angle) * radius * 0.6, here.z + Math.sin(angle) * radius * 0.6, here.y, 6);
			UseAbilities.strike(world, player, Vec3d.ofBottomCenter(ground));
		}
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(radius), e -> e != player && e.isAlive())) {
			Vec3d away = living.getPos().subtract(here);
			living.damage(world.getDamageSources().indirectMagic(player, player), damage);
			living.takeKnockback(2.5, -away.x, -away.z);
			living.addVelocity(0.0, 0.6, 0.0);
			living.velocityModified = true;
		}
		Fx.particles(world, ParticleTypes.ELECTRIC_SPARK, here.add(0.0, 1.0, 0.0), 120, radius * 0.4, 0.3);
		Fx.sound(world, here, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 4.0F, 0.8F);
		return true;
	}

	/** Railgun: a shot that goes straight through blocks and every mob on the line. Obsidian, bedrock and chests stop it. */
	static boolean railgun(ServerWorld world, ServerPlayerEntity player, int range, float damage) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		Vec3d end = eye.add(look.multiply(range));
		BlockPos lastPos = null;
		for (double d = 1.0; d < range; d += 0.25) {
			BlockPos pos = BlockPos.ofFloored(eye.add(look.multiply(d)));
			if (pos.equals(lastPos)) {
				continue;
			}
			lastPos = pos;
			BlockState state = world.getBlockState(pos);
			if (state.isAir() || state.isLiquid()) {
				continue;
			}
			float hardness = state.getHardness(world, pos);
			if (hardness < 0.0F || hardness >= 50.0F || state.hasBlockEntity()) {
				end = eye.add(look.multiply(d));
				break;
			}
			world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
		}
		Vec3d stop = end;
		Box box = new Box(eye, stop).expand(1.0);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e.getBoundingBox().expand(0.5).raycast(eye, stop).isPresent())) {
			living.timeUntilRegen = 0;
			living.damage(world.getDamageSources().indirectMagic(player, player), damage);
		}
		Fx.line(world, ParticleTypes.ELECTRIC_SPARK, eye.add(0.0, -0.2, 0.0), stop);
		Fx.line(world, ParticleTypes.END_ROD, eye.add(0.0, -0.2, 0.0), stop);
		Fx.sound(world, eye, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 2.0F);
		Fx.sound(world, stop, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 2.0F, 1.5F);
		return true;
	}

	/** Flamethrower: small fireballs in a cone for a few seconds. */
	static boolean flamethrower(ServerWorld world, ServerPlayerEntity player, int ticks) {
		IntConsumer puff = i -> {
			if (!player.isAlive() || player.getWorld() != world || player.isSpectator()) {
				return;
			}
			Random random = world.random;
			Vec3d look = player.getRotationVec(1.0F);
			Vec3d eye = player.getEyePos();
			for (int k = 0; k < 2; k++) {
				Vec3d direction = look.add((random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25, (random.nextDouble() - 0.5) * 0.25).normalize();
				SmallFireballEntity fire = new SmallFireballEntity(world, player, direction);
				fire.setPosition(eye.x + look.x * 1.2, eye.y - 0.25 + look.y * 1.2, eye.z + look.z * 1.2);
				world.spawnEntity(fire);
			}
			Fx.particles(world, ParticleTypes.FLAME, eye.add(look.multiply(2.0)), 10, 0.3, 0.05);
			if (i % 5 == 0) {
				Fx.sound(world, eye, SoundEvents.ITEM_FIRECHARGE_USE, 1.0F, 0.8F + random.nextFloat() * 0.4F);
			}
		};
		puff.accept(0);
		Tasks.repeat(ticks - 1, 1, i -> puff.accept(i + 1));
		return true;
	}

	/** Pig Missile: the player rides a pig that flies where the player looks, and explodes when it hits something. */
	static boolean pigMissile(ServerWorld world, ServerPlayerEntity player, int ticks) {
		if (player.hasVehicle()) {
			return false;
		}
		Entity entity = Spells.spawn(world, EntityType.PIG, player.getX(), player.getY(), player.getZ());
		if (!(entity instanceof PigEntity pig)) {
			return false;
		}
		pig.setInvulnerable(true);
		pig.setYaw(player.getYaw());
		if (!player.startRiding(pig, true)) {
			pig.discard();
			return false;
		}
		player.sendMessage(Text.literal("Pig missile! It flies where you look.").formatted(Formatting.GOLD), true);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 0.6F);
		int[] flown = {0};
		Runnable[] fly = new Runnable[1];
		fly[0] = () -> {
			if (pig.isRemoved() || !pig.isAlive()) {
				return;
			}
			flown[0]++;
			Entity rider = pig.getFirstPassenger();
			boolean crash = flown[0] > 5 && (pig.horizontalCollision || (flown[0] > 8 && pig.isOnGround()));
			if (flown[0] >= ticks || crash || rider == null) {
				Vec3d at = pig.getPos();
				pig.removeAllPassengers();
				if (rider instanceof LivingEntity living) {
					living.addVelocity(0.0, 1.0, 0.0);
					living.velocityModified = true;
					living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
				}
				pig.discard();
				Fx.boom(world, rider, at, 4.0F, true, true);
				world.spawnEntity(new ItemEntity(world, at.x, at.y + 1.0, at.z, new ItemStack(Items.COOKED_PORKCHOP, 3)));
				return;
			}
			Vec3d look = rider.getRotationVec(1.0F);
			pig.setVelocity(look.x * 1.2, look.y * 1.2 + 0.1, look.z * 1.2);
			pig.velocityModified = true;
			pig.fallDistance = 0.0F;
			pig.setYaw(rider.getYaw());
			pig.setBodyYaw(rider.getYaw());
			Vec3d tail = pig.getPos().subtract(look.multiply(0.8)).add(0.0, 0.4, 0.0);
			Fx.particles(world, ParticleTypes.FLAME, tail, 6, 0.1, 0.02);
			Fx.particles(world, ParticleTypes.LARGE_SMOKE, tail, 2, 0.1, 0.01);
			Tasks.later(1, fly[0]);
		};
		Tasks.later(1, fly[0]);
		return true;
	}

	/** Kaiju Egg: a random monster hatches. It is six times as big and hunts the player who hatched it. */
	static boolean kaiju(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		flat = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
		BlockPos ground = Fx.groundNear(world, player.getX() + flat.x * 10.0, player.getZ() + flat.z * 10.0, player.getY(), 10);
		EntityType<?> type = KAIJU[world.random.nextInt(KAIJU.length)];
		Entity entity = Spells.spawn(world, type, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
		if (!(entity instanceof LivingEntity monster)) {
			return false;
		}
		addModifier(monster, "generic.scale", 5.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		addModifier(monster, "generic.max_health", 200.0, EntityAttributeModifier.Operation.ADD_VALUE);
		addModifier(monster, "generic.attack_damage", 12.0, EntityAttributeModifier.Operation.ADD_VALUE);
		addModifier(monster, "generic.step_height", 4.0, EntityAttributeModifier.Operation.ADD_VALUE);
		monster.setHealth(monster.getMaxHealth());
		monster.setCustomName(Text.literal("KAIJU").formatted(Formatting.DARK_RED, Formatting.BOLD));
		monster.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 600, 0));
		if (monster instanceof MobEntity mob) {
			mob.setPersistent();
			mob.setTarget(player);
		}
		Fx.particles(world, ParticleTypes.EXPLOSION_EMITTER, monster.getPos(), 2, 1.0, 0.0);
		Fx.sound(world, monster.getPos(), SoundEvents.ENTITY_RAVAGER_ROAR, 4.0F, 0.5F);
		Fx.sound(world, monster.getPos(), SoundEvents.ENTITY_TURTLE_EGG_HATCH, 4.0F, 0.5F);
		player.sendMessage(Text.literal("A KAIJU hatched!").formatted(Formatting.DARK_RED), true);
		return true;
	}

	/** True if the mob was made giant by a Kaiju Egg. The tests use this. */
	public static boolean isKaiju(LivingEntity living) {
		RegistryEntry<EntityAttribute> scale = Fx.attribute("generic.scale");
		EntityAttributeInstance instance = scale == null ? null : living.getAttributeInstance(scale);
		return instance != null && instance.hasModifier(KAIJU_ID);
	}

	private static void addModifier(LivingEntity living, String attributeId, double value, EntityAttributeModifier.Operation operation) {
		RegistryEntry<EntityAttribute> attribute = Fx.attribute(attributeId);
		if (attribute == null) {
			return;
		}
		EntityAttributeInstance instance = living.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(KAIJU_ID);
		instance.addPersistentModifier(new EntityAttributeModifier(KAIJU_ID, value, operation));
	}

	/** Ring of Fire: a ring of fire on netherrack around the player. Fire on netherrack never goes out. */
	static boolean ringOfFire(ServerWorld world, ServerPlayerEntity player, int radius) {
		BlockPos feet = player.getBlockPos();
		int lit = 0;
		int points = radius * 8;
		for (int i = 0; i < points; i++) {
			double angle = i * Math.PI * 2.0 / points;
			int x = (int) Math.floor(feet.getX() + 0.5 + Math.cos(angle) * radius);
			int z = (int) Math.floor(feet.getZ() + 0.5 + Math.sin(angle) * radius);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = new BlockPos(x, feet.getY() + dy, z);
				BlockPos below = pos.down();
				BlockState ground = world.getBlockState(below);
				if (!world.getBlockState(pos).isAir() || !ground.isSolidBlock(world, below)) {
					continue;
				}
				if (!ground.hasBlockEntity() && ground.getHardness(world, below) >= 0.0F) {
					world.setBlockState(below, Blocks.NETHERRACK.getDefaultState(), Block.NOTIFY_ALL);
				}
				world.setBlockState(pos, AbstractFireBlock.getState(world, pos), Block.NOTIFY_ALL);
				lit++;
				break;
			}
		}
		if (lit == 0) {
			return false;
		}
		Fx.sound(world, player.getPos(), SoundEvents.ITEM_FIRECHARGE_USE, 2.0F, 0.6F);
		Fx.particles(world, ParticleTypes.FLAME, player.getPos(), 80, radius * 0.6, 0.02);
		return true;
	}

	/** Glacier Staff: ice spikes burst out of the ground in a line. */
	static boolean iceSpikes(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		Vec3d dir = flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
		Vec3d start = player.getPos();
		BlockState ice = Blocks.PACKED_ICE.getDefaultState();
		IntConsumer spike = i -> {
			Vec3d at = start.add(dir.multiply(2.5 + i * 2.0));
			BlockPos base = Fx.groundNear(world, at.x, at.z, start.y, 6);
			int height = 3 + world.random.nextInt(4);
			for (int h = 0; h < height; h++) {
				BlockPos pos = base.up(h);
				if (Fx.canFill(world.getBlockState(pos))) {
					world.setBlockState(pos, ice, Block.NOTIFY_ALL);
				}
				if (h < height / 2) {
					for (Direction side : Direction.Type.HORIZONTAL) {
						BlockPos thick = pos.offset(side);
						if (Fx.canFill(world.getBlockState(thick))) {
							world.setBlockState(thick, ice, Block.NOTIFY_ALL);
						}
					}
				}
			}
			for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, new Box(base).expand(1.5, height, 1.5), e -> e != player && e.isAlive())) {
				living.damage(world.getDamageSources().indirectMagic(player, player), 8.0F);
				living.addVelocity(0.0, 1.0, 0.0);
				living.velocityModified = true;
				living.setFrozenTicks(living.getMinFreezeDamageTicks() + 100);
			}
			Fx.particles(world, ParticleTypes.SNOWFLAKE, Vec3d.ofCenter(base.up(2)), 30, 1.0, 0.05);
			Fx.sound(world, Vec3d.ofCenter(base), SoundEvents.BLOCK_GLASS_BREAK, 1.5F, 0.5F + world.random.nextFloat() * 0.3F);
		};
		int count = Math.max(1, range / 2);
		spike.accept(0);
		Tasks.repeat(count - 1, 1, i -> spike.accept(i + 1));
		return true;
	}

	/** Force Field: for a while nothing can come close to the player. Mobs are pushed away, shots vanish. */
	static boolean forceField(ServerWorld world, ServerPlayerEntity player, int ticks, double radius) {
		IntConsumer pulse = i -> {
			if (!player.isAlive() || player.getWorld() != world) {
				return;
			}
			Random random = world.random;
			Vec3d center = player.getPos().add(0.0, 1.0, 0.0);
			for (Entity entity : world.getOtherEntities(player, Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0),
					e -> e.isAlive() && !e.isSpectator() && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrbEntity)
							&& !(e instanceof FallingBlockEntity))) {
				Vec3d away = entity.getPos().subtract(center);
				double distance = away.length();
				if (distance > radius) {
					continue;
				}
				if (entity instanceof ProjectileEntity) {
					Fx.particles(world, ParticleTypes.ELECTRIC_SPARK, entity.getPos(), 8, 0.1, 0.05);
					entity.discard();
					continue;
				}
				Vec3d push = distance < 0.1 ? new Vec3d(0.0, 1.0, 0.0) : away.multiply(1.0 / distance);
				entity.setVelocity(push.x * 0.9, Math.max(0.25, push.y * 0.9), push.z * 0.9);
				entity.velocityModified = true;
			}
			if (i % 2 == 0) {
				for (int k = 0; k < 16; k++) {
					double theta = random.nextDouble() * Math.PI * 2.0;
					double phi = Math.acos(random.nextDouble() * 2.0 - 1.0);
					world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x + radius * Math.sin(phi) * Math.cos(theta),
							center.y + radius * Math.cos(phi), center.z + radius * Math.sin(phi) * Math.sin(theta), 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
			if (i % 20 == 0) {
				Fx.sound(world, center, SoundEvents.BLOCK_BEACON_AMBIENT, 2.0F, 1.5F);
			}
		};
		pulse.accept(0);
		Tasks.repeat(ticks - 1, 1, i -> pulse.accept(i + 1));
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0F, 1.6F);
		return true;
	}
}
