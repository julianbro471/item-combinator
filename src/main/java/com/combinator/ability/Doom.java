package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * The Armageddon Clock: the end of the world in five acts, around the spot where it was started.
 * <ol>
 * <li>10 seconds of countdown. The sky turns to thunder, a bell tolls every second.</li>
 * <li>The earth splits: rings of ground fly up, glowing cracks run out from the middle.</li>
 * <li>Fire rain: meteors and fires everywhere.</li>
 * <li>The sky breaks: lightning without pause, lava geysers.</li>
 * <li>Collapse: a black hole over the middle, then a blast that erases everything within 18 blocks.</li>
 * </ol>
 * A boss bar shows how far it has come. The player who started it can call it off by sneaking and right-clicking.
 */
public final class Doom {
	static final int COUNTDOWN = 200;
	static final int QUAKE_END = 400;
	static final int FIRE_END = 600;
	static final int STORM_END = 800;
	static final int COLLAPSE_END = 1000;
	/** Ticks from the start until everything is over, the final blast included. */
	public static final int TOTAL = COLLAPSE_END + 20;
	/** How far from the middle the meteors, lightning and geysers reach. */
	private static final double RADIUS = 28.0;
	private static final double HEAR = 160.0;

	/** One running Armageddon. */
	private static final class Run {
		final ServerWorld world;
		final ServerPlayerEntity owner;
		final Vec3d center;
		final ServerBossBar bar;
		final List<Crack> cracks = new ArrayList<>();
		int tick;
		boolean over;

		Run(ServerWorld world, ServerPlayerEntity owner, Vec3d center) {
			this.world = world;
			this.owner = owner;
			this.center = center;
			this.bar = new ServerBossBar(Text.literal("Armageddon").formatted(Formatting.DARK_RED), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
			this.bar.setDarkenSky(true);
			this.bar.setThickenFog(true);
		}
	}

	/** A crack in the ground that runs outward from the middle, one block per step. */
	private static final class Crack {
		final double dx;
		final double dz;
		int length = 2;

		Crack(double angle) {
			this.dx = Math.cos(angle);
			this.dz = Math.sin(angle);
		}
	}

	private static final Map<UUID, Run> RUNS = new HashMap<>();

	private Doom() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> cancelAll());
	}

	/** True while an Armageddon started by this player is running. */
	public static boolean active(PlayerEntity player) {
		return RUNS.containsKey(player.getUuid());
	}

	/** Stops every running Armageddon at once. The tests use this, and it runs when the world is closed. */
	public static void cancelAll() {
		for (Run run : new ArrayList<>(RUNS.values())) {
			finish(run);
		}
		RUNS.clear();
	}

	/** Starts the countdown where the player stands. */
	static boolean start(ServerWorld world, ServerPlayerEntity player) {
		if (RUNS.containsKey(player.getUuid())) {
			player.sendMessage(Text.literal("Your Armageddon is already running. Sneak + right-click to call it off.").formatted(Formatting.RED), true);
			return false;
		}
		Run run = new Run(world, player, player.getPos());
		Random random = world.random;
		int cracks = 5 + random.nextInt(3);
		for (int i = 0; i < cracks; i++) {
			run.cracks.add(new Crack(i * Math.PI * 2.0 / cracks + random.nextDouble() * 0.5));
		}
		RUNS.put(player.getUuid(), run);
		world.setWeather(0, 6000, true, true);
		Fx.sound(world, run.center, SoundEvents.ENTITY_WITHER_SPAWN, 4.0F, 0.5F);
		Fx.sound(world, run.center, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 4.0F, 0.6F);
		Fx.particles(world, ParticleTypes.SOUL, run.center.add(0.0, 1.0, 0.0), 80, 2.0, 0.05);
		step(run);
		return true;
	}

	/** Sneak + right-click: calls the Armageddon of this player off. */
	static boolean cancel(ServerWorld world, ServerPlayerEntity player) {
		Run run = RUNS.get(player.getUuid());
		if (run == null) {
			player.sendMessage(Text.literal("No Armageddon of yours is running."), true);
			return false;
		}
		finish(run);
		title(run, Text.literal("Called off").formatted(Formatting.GREEN), Text.literal("The world is safe. For now."));
		Fx.sound(world, run.center, SoundEvents.BLOCK_BEACON_DEACTIVATE, 3.0F, 0.6F);
		return true;
	}

	private static void finish(Run run) {
		run.over = true;
		run.bar.clearPlayers();
		RUNS.remove(run.owner.getUuid(), run);
		run.world.setWeather(6000, 0, false, false);
	}

	/** One tick of the Armageddon. It plans the next tick itself, so only one task is waiting at any time. */
	private static void step(Run run) {
		if (run.over) {
			return;
		}
		try {
			act(run);
		} catch (Throwable t) {
			ItemCombinator.error("Armageddon failed", t);
			finish(run);
			return;
		}
		run.tick++;
		if (run.tick > TOTAL) {
			finish(run);
			title(run, Text.literal("THE END").formatted(Formatting.DARK_RED, Formatting.BOLD), Text.literal("Was it worth it?").formatted(Formatting.GRAY));
			return;
		}
		Tasks.later(1, () -> step(run));
	}

	private static void act(Run run) {
		int t = run.tick;
		ServerWorld world = run.world;
		Random random = world.random;
		if (t % 10 == 0) {
			updateBar(run);
		}
		if (t < COUNTDOWN) {
			if (t % 20 == 0) {
				int left = (COUNTDOWN - t) / 20;
				title(run, Text.literal(String.valueOf(left)).formatted(Formatting.DARK_RED, Formatting.BOLD),
						Text.literal("The end of the world is coming").formatted(Formatting.RED));
				Fx.sound(world, run.center, SoundEvents.BLOCK_BELL_USE, 6.0F, 0.5F);
			}
			Fx.particles(world, ParticleTypes.SOUL_FIRE_FLAME, run.center.add(0.0, 0.5, 0.0), 6, 1.5, 0.05);
			Fx.particles(world, ParticleTypes.ASH, run.center.add(0.0, 4.0, 0.0), 30, 12.0, 0.0);
		} else if (t < QUAKE_END) {
			int since = t - COUNTDOWN;
			if (since == 0) {
				title(run, Text.literal("THE EARTH SPLITS").formatted(Formatting.GOLD, Formatting.BOLD), Text.empty());
			}
			if (since % 20 == 0) {
				quakeRing(run, 3.0 + since / 20 * 3.0);
			}
			if (since % 2 == 0) {
				for (Crack crack : run.cracks) {
					extend(run, crack);
				}
			}
		} else if (t < FIRE_END) {
			if (t == QUAKE_END) {
				title(run, Text.literal("FIRE FALLS").formatted(Formatting.RED, Formatting.BOLD), Text.empty());
			}
			if (t % 4 == 0) {
				BlockPos ground = randomGround(run);
				Spells.meteor(world, run.owner, Vec3d.ofBottomCenter(ground), 3.0F);
			}
			if (t % 3 == 0) {
				BlockPos ground = randomGround(run);
				if (world.getBlockState(ground).isAir() && AbstractFireBlock.canPlaceAt(world, ground, net.minecraft.util.math.Direction.UP)) {
					world.setBlockState(ground, AbstractFireBlock.getState(world, ground), Block.NOTIFY_ALL);
				}
			}
		} else if (t < STORM_END) {
			if (t == FIRE_END) {
				title(run, Text.literal("THE SKY BREAKS").formatted(Formatting.YELLOW, Formatting.BOLD), Text.empty());
			}
			if (t % 3 == 0) {
				UseAbilities.strike(world, run.owner, Vec3d.ofBottomCenter(randomGround(run)));
			}
			if (t % 25 == 0) {
				geyser(world, randomGround(run));
			}
		} else if (t < COLLAPSE_END) {
			if (t == STORM_END) {
				title(run, Text.literal("THE SKY COLLAPSES").formatted(Formatting.DARK_PURPLE, Formatting.BOLD), Text.empty());
				Spells.blackHole(world, run.center.add(0.0, 6.0, 0.0));
			}
			if (t % 20 == 0) {
				Fx.sound(world, run.center, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 6.0F, 0.5F);
			}
		} else if (t == COLLAPSE_END) {
			Spells.bigBlast(world, run.owner, run.center, 18.0);
		}
		if (random.nextInt(40) == 0) {
			Fx.sound(world, run.center, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 6.0F, 0.5F + random.nextFloat() * 0.3F);
		}
	}

	private static void updateBar(Run run) {
		run.bar.setPercent(Math.max(0.0F, 1.0F - (float) run.tick / TOTAL));
		String act = run.tick < COUNTDOWN ? "Countdown: " + (COUNTDOWN - run.tick + 19) / 20
				: run.tick < QUAKE_END ? "The earth splits"
				: run.tick < FIRE_END ? "Fire falls"
				: run.tick < STORM_END ? "The sky breaks"
				: "The sky collapses";
		run.bar.setName(Text.literal("Armageddon - " + act).formatted(Formatting.DARK_RED));
		for (ServerPlayerEntity player : run.world.getPlayers()) {
			if (player.squaredDistanceTo(run.center) < HEAR * HEAR) {
				run.bar.addPlayer(player);
			}
		}
	}

	/** A big title on the screen of everybody near. */
	private static void title(Run run, Text title, Text subtitle) {
		for (ServerPlayerEntity player : run.world.getPlayers()) {
			if (player.squaredDistanceTo(run.center) > HEAR * HEAR) {
				continue;
			}
			player.networkHandler.sendPacket(new TitleFadeS2CPacket(3, 30, 8));
			player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
			player.networkHandler.sendPacket(new TitleS2CPacket(title));
		}
	}

	/** A random spot on the ground within the radius of the Armageddon. */
	private static BlockPos randomGround(Run run) {
		Random random = run.world.random;
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = Math.sqrt(random.nextDouble()) * RADIUS;
		return Fx.groundNear(run.world, run.center.x + Math.cos(angle) * distance, run.center.z + Math.sin(angle) * distance, run.center.y, 12);
	}

	/** A ring of ground around the middle jumps into the air. Everything standing on it jumps too. */
	private static void quakeRing(Run run, double radius) {
		ServerWorld world = run.world;
		Random random = world.random;
		int points = (int) Math.min(60, radius * Math.PI);
		for (int i = 0; i < points; i++) {
			double angle = i * Math.PI * 2.0 / points + random.nextDouble() * 0.1;
			BlockPos top = Fx.groundNear(world, run.center.x + Math.cos(angle) * radius, run.center.z + Math.sin(angle) * radius, run.center.y, 8).down();
			BlockState state = world.getBlockState(top);
			if (Fx.loose(world, top, state)) {
				Fx.fling(world, top, state, (random.nextDouble() - 0.5) * 0.3, 0.5 + random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.3);
			}
		}
		Box box = Box.of(run.center, radius * 2.0 + 4.0, 12.0, radius * 2.0 + 4.0);
		for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && !e.isSpectator())) {
			double flat = Math.sqrt(Math.pow(living.getX() - run.center.x, 2) + Math.pow(living.getZ() - run.center.z, 2));
			if (Math.abs(flat - radius) < 2.5) {
				living.damage(world.getDamageSources().indirectMagic(run.owner, run.owner), 4.0F);
				living.addVelocity(0.0, 0.8, 0.0);
				living.velocityModified = true;
			}
		}
		Fx.sound(world, run.center, SoundEvents.ENTITY_GENERIC_EXPLODE, 5.0F, 0.4F);
		Fx.particles(world, ParticleTypes.EXPLOSION, run.center, 10, radius * 0.5, 0.0);
	}

	/** The crack grows by one block: a trench 7 deep with glowing magma at the bottom. */
	private static void extend(Run run, Crack crack) {
		if (crack.length > RADIUS) {
			return;
		}
		ServerWorld world = run.world;
		double x = run.center.x + crack.dx * crack.length;
		double z = run.center.z + crack.dz * crack.length;
		crack.length++;
		BlockPos top = Fx.groundNear(world, x, z, run.center.y, 8);
		for (int depth = 1; depth <= 7; depth++) {
			BlockPos pos = top.down(depth);
			BlockState state = world.getBlockState(pos);
			if (state.isAir() || state.getHardness(world, pos) < 0.0F || state.hasBlockEntity()) {
				continue;
			}
			world.setBlockState(pos, depth == 7 ? Blocks.MAGMA_BLOCK.getDefaultState() : Fx.AIR, Block.NOTIFY_ALL);
		}
		Fx.particles(world, ParticleTypes.LAVA, Vec3d.ofCenter(top), 4, 0.4, 0.0);
	}

	/** Magma blocks shoot out of the ground for a second, like a fountain. */
	private static void geyser(ServerWorld world, BlockPos spot) {
		Fx.sound(world, Vec3d.ofCenter(spot), SoundEvents.BLOCK_LAVA_POP, 4.0F, 0.5F);
		Tasks.repeat(10, 2, i -> {
			Random random = world.random;
			BlockPos at = spot.up(1 + i % 2);
			if (!world.getBlockState(at).isAir()) {
				return;
			}
			FallingBlockEntity bomb = Fx.fling(world, at, Blocks.MAGMA_BLOCK.getDefaultState(),
					(random.nextDouble() - 0.5) * 0.4, 1.0 + random.nextDouble() * 0.8, (random.nextDouble() - 0.5) * 0.4);
			bomb.setHurtEntities(2.0F, 20);
			Fx.particles(world, ParticleTypes.LAVA, Vec3d.ofCenter(spot), 8, 0.3, 0.0);
		});
	}
}
