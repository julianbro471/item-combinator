package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.World;

/**
 * The odd charms: items that do something by themselves while the player carries, holds or wears them.
 * Double jump, wall climbing, the balloon, the light that follows the player, flower boots, lava boots,
 * the pocket mirror, the lunchbox, the almanac, the rodeo saddle, and the memory of the Rewind Watch.
 */
public final class Charms {
	/** Numbers of the movement tricks in a {@link TrickPayload}. */
	public static final int TRICK_DOUBLE_JUMP = 1;
	public static final int TRICK_CLIMB = 2;
	/** How many movement tricks the server has accepted. The automatic test of the real game reads this. */
	public static volatile int tricksSeen = 0;

	private static final TagKey<DamageType> IS_PROJECTILE = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.ofVanilla("is_projectile"));
	private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
			Blocks.SHORT_GRASS, Blocks.SHORT_GRASS};
	/** What lava turns into under the Strider Boots, and for how long. */
	private static final Block CRUST = Blocks.SMOOTH_BASALT;
	private static final int CRUST_TICKS = 60;
	/** The Rewind Watch looks at the player every 5 ticks and remembers 21 looks: 5 seconds. */
	private static final int HISTORY_SIZE = 21;

	/** The invisible light block that follows a player with a Firefly Jar. */
	private record Light(RegistryKey<World> world, BlockPos pos) {
	}

	/** Where a player was and how healthy, at one moment. */
	private record Moment(RegistryKey<World> world, Vec3d pos, float yaw, float pitch, float health) {
	}

	/** Which charms of a player are active right now. */
	private static final class Active {
		boolean balloon;
		boolean lantern;
		boolean meadow;
		boolean lavaWalk;
		boolean autoEat;
		boolean almanac;
		boolean rewind;
	}

	private static final Map<UUID, Light> LIGHTS = new HashMap<>();
	private static final Map<UUID, ArrayDeque<Moment>> HISTORY = new HashMap<>();
	private static final Map<UUID, BlockPos> LAST_STEP = new HashMap<>();
	/** Hardened lava per world: the place, and the world time at which it melts again. */
	private static final Map<RegistryKey<World>, Map<BlockPos, Long>> CRUSTS = new HashMap<>();

	private Charms() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(TrickPayload.ID, TrickPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TrickPayload.ID, (payload, context) -> {
			try {
				trick(context.player(), payload.trick());
			} catch (Throwable t) {
				ItemCombinator.error("Movement trick failed", t);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Charms::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Charms::allowDamage);
		UseEntityCallback.EVENT.register(Charms::mount);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			try {
				forget(server, handler.player.getUuid());
			} catch (Throwable t) {
				ItemCombinator.error("Cleaning up after a player failed", t);
			}
		});
		// Nothing of this may stay in the world when it is closed: the lights go out and the lava is lava again.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			try {
				for (Light light : LIGHTS.values()) {
					removeLight(server, light);
				}
				meltCrust(server, true);
			} catch (Throwable t) {
				ItemCombinator.error("Cleaning up the charms failed", t);
			}
			LIGHTS.clear();
			HISTORY.clear();
			LAST_STEP.clear();
			CRUSTS.clear();
		});
	}

	// ------------------------------------------------------------------ movement tricks

	/**
	 * The player's game client says: a movement trick just happened. The jump itself was already done there.
	 * The server only forgets the fall height (so the landing does not hurt more than it should) and shows the effect.
	 */
	public static void trick(ServerPlayerEntity player, int trick) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		ServerWorld world = player.getServerWorld();
		if (trick == TRICK_DOUBLE_JUMP && PassiveAbilities.has(player, traits -> traits.airJumps > 0)) {
			player.fallDistance = 0.0F;
			Fx.particles(world, ParticleTypes.CLOUD, player.getPos(), 12, 0.25, 0.02);
			Fx.sound(world, player.getPos(), SoundEvents.ENTITY_PHANTOM_FLAP, 0.6F, 1.6F);
			tricksSeen++;
		} else if (trick == TRICK_CLIMB && PassiveAbilities.has(player, traits -> traits.wallClimb)) {
			player.fallDistance = 0.0F;
			tricksSeen++;
		}
	}

	// ------------------------------------------------------------------ once per tick

	private static void tick(MinecraftServer server) {
		int ticks = server.getTicks();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			try {
				tickPlayer(server, player, ticks);
			} catch (Throwable t) {
				ItemCombinator.error("Charm failed", t);
			}
		}
		if (ticks % 10 == 0) {
			try {
				meltCrust(server, false);
			} catch (Throwable t) {
				ItemCombinator.error("Melting hardened lava failed", t);
			}
		}
	}

	private static void tickPlayer(MinecraftServer server, ServerPlayerEntity player, int ticks) {
		Active active = new Active();
		if (player.isAlive() && !player.isSpectator()) {
			PassiveAbilities.forEachActive(player, (stack, traits) -> {
				active.balloon |= traits.balloon;
				active.lantern |= traits.lantern;
				active.meadow |= traits.meadow;
				active.lavaWalk |= traits.lavaWalk;
				active.autoEat |= traits.autoEat;
				active.almanac |= traits.almanac;
				active.rewind |= traits.use == Use.REWIND;
				return false;
			});
		}
		ServerWorld world = player.getServerWorld();

		if (active.balloon && ticks % 5 == 0) {
			balloon(player);
		}
		if (ticks % 2 == 0 || !active.lantern) {
			light(server, player, active.lantern);
		}
		if (active.meadow && ticks % 4 == 0) {
			meadow(world, player, ticks % 20 == 0);
		} else if (!active.meadow) {
			LAST_STEP.remove(player.getUuid());
		}
		if (active.lavaWalk) {
			lavaWalk(world, player);
		}
		if (active.rewind) {
			if (ticks % 5 == 0) {
				ArrayDeque<Moment> history = HISTORY.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
				history.addLast(new Moment(world.getRegistryKey(), player.getPos(), player.getYaw(), player.getPitch(), player.getHealth()));
				while (history.size() > HISTORY_SIZE) {
					history.removeFirst();
				}
			}
		} else {
			HISTORY.remove(player.getUuid());
		}
		if (active.autoEat && ticks % 20 == 0) {
			autoEat(player);
		}
		if (active.almanac && ticks % 20 == 0) {
			player.sendMessage(almanacText(world, player), true);
		}
	}

	private static void forget(MinecraftServer server, UUID player) {
		Light light = LIGHTS.remove(player);
		if (light != null) {
			removeLight(server, light);
		}
		HISTORY.remove(player);
		LAST_STEP.remove(player);
	}

	// ------------------------------------------------------------------ Puffer Balloon

	/** Up while the balloon is held, slowly down while sneaking. Letting go of it leaves 5 seconds of soft falling. */
	private static void balloon(ServerPlayerEntity player) {
		if (player.isSneaking()) {
			player.removeStatusEffect(StatusEffects.LEVITATION);
		} else {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 15, 0, true, false, true));
		}
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0, true, false, true));
	}

	// ------------------------------------------------------------------ Firefly Jar

	/**
	 * Keeps one invisible light block at the player's head and moves it along.
	 * The game has such a block built in (it is what map makers use to light rooms without a lamp).
	 */
	private static void light(MinecraftServer server, ServerPlayerEntity player, boolean on) {
		UUID id = player.getUuid();
		Light old = LIGHTS.get(id);
		if (!on && old == null) {
			return;
		}
		ServerWorld world = player.getServerWorld();
		BlockPos want = on ? BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ()) : null;
		boolean same = old != null && want != null && old.world().equals(world.getRegistryKey()) && old.pos().equals(want);
		if (same && world.getBlockState(want).isOf(Blocks.LIGHT)) {
			return; // the light is where it should be
		}
		if (old != null) {
			removeLight(server, old);
			LIGHTS.remove(id);
		}
		// Only plain air is replaced. Water, plants and other players' lights stay as they are.
		if (want != null && world.isInBuildLimit(want) && world.getBlockState(want).isAir()) {
			world.setBlockState(want, Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, 13), Block.NOTIFY_LISTENERS);
			LIGHTS.put(id, new Light(world.getRegistryKey(), want));
		}
	}

	private static void removeLight(MinecraftServer server, Light light) {
		ServerWorld world = server.getWorld(light.world());
		if (world != null && world.getBlockState(light.pos()).isOf(Blocks.LIGHT)) {
			world.setBlockState(light.pos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
		}
	}

	// ------------------------------------------------------------------ Meadow Boots

	private static void meadow(ServerWorld world, ServerPlayerEntity player, boolean growCrops) {
		BlockPos feet = player.getBlockPos();
		BlockPos last = LAST_STEP.put(player.getUuid(), feet);
		// A flower only on a step to a new block, so standing still does not bury the player in flowers.
		if (!feet.equals(last) && world.getBlockState(feet).isAir() && world.getBlockState(feet.down()).isOf(Blocks.GRASS_BLOCK)
				&& world.random.nextFloat() < 0.35F) {
			world.setBlockState(feet, FLOWERS[world.random.nextInt(FLOWERS.length)].getDefaultState(), Block.NOTIFY_ALL);
		}
		if (!growCrops) {
			return;
		}
		for (BlockPos pos : BlockPos.iterate(feet.add(-2, -1, -2), feet.add(2, 1, 2))) {
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CropBlock crop && !crop.isMature(state) && world.random.nextFloat() < 0.05F) {
				world.setBlockState(pos.toImmutable(), crop.withAge(crop.getAge(state) + 1), Block.NOTIFY_LISTENERS);
			}
		}
	}

	// ------------------------------------------------------------------ Strider Boots

	/** Like the Frost Walker enchantment, but for lava: still lava near the feet of a standing player hardens. */
	private static void lavaWalk(ServerWorld world, ServerPlayerEntity player) {
		BlockPos below = BlockPos.ofFloored(player.getX(), player.getY() - 0.2, player.getZ());
		boolean standing = player.isOnGround() || !world.getBlockState(below).getCollisionShape(world, below).isEmpty();
		if (!standing) {
			return;
		}
		Map<BlockPos, Long> crust = CRUSTS.computeIfAbsent(world.getRegistryKey(), k -> new HashMap<>());
		long melts = world.getTime() + CRUST_TICKS;
		for (BlockPos pos : BlockPos.iterate(below.add(-2, 0, -2), below.add(2, 0, 2))) {
			double dx = pos.getX() + 0.5 - player.getX();
			double dz = pos.getZ() + 0.5 - player.getZ();
			if (dx * dx + dz * dz > 2.6 * 2.6) {
				continue;
			}
			BlockState state = world.getBlockState(pos);
			if (crust.containsKey(pos)) {
				if (state.isOf(CRUST)) {
					crust.put(pos.toImmutable(), melts); // the player is still near: it stays hard
				}
			} else if (state.isOf(Blocks.LAVA) && state.getFluidState().isStill() && world.getBlockState(pos.up()).isAir()) {
				world.setBlockState(pos.toImmutable(), CRUST.getDefaultState(), Block.NOTIFY_ALL);
				crust.put(pos.toImmutable(), melts);
			}
		}
	}

	/** Turns hardened lava back into lava when its time is up. everything = true: all of it, at once. */
	private static void meltCrust(MinecraftServer server, boolean everything) {
		for (Map.Entry<RegistryKey<World>, Map<BlockPos, Long>> entry : CRUSTS.entrySet()) {
			ServerWorld world = server.getWorld(entry.getKey());
			if (world == null) {
				entry.getValue().clear();
				continue;
			}
			long now = world.getTime();
			for (Iterator<Map.Entry<BlockPos, Long>> it = entry.getValue().entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<BlockPos, Long> spot = it.next();
				if (!everything && spot.getValue() > now) {
					continue;
				}
				if (world.getBlockState(spot.getKey()).isOf(CRUST)) {
					world.setBlockState(spot.getKey(), Blocks.LAVA.getDefaultState(), Block.NOTIFY_ALL);
				}
				it.remove();
			}
		}
	}

	// ------------------------------------------------------------------ Rewind Watch

	/** Puts the player back where they were 5 seconds ago, with the health of then if that was more. */
	static boolean rewind(ServerWorld world, ServerPlayerEntity player) {
		ArrayDeque<Moment> history = HISTORY.get(player.getUuid());
		if (history == null || history.size() < 9) {
			player.sendMessage(Text.literal("The watch has to be in your hotbar for a few seconds first."), true);
			return false;
		}
		Moment then = history.peekFirst();
		ServerWorld destination = world.getServer().getWorld(then.world());
		if (destination == null) {
			return false;
		}
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, player.getPos().add(0.0, 1.0, 0.0), 40, 0.4, 0.05);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8F, 1.6F);
		player.teleport(destination, then.pos().x, then.pos().y, then.pos().z, then.yaw(), then.pitch());
		player.setVelocity(Vec3d.ZERO);
		player.velocityModified = true;
		player.fallDistance = 0.0F;
		player.extinguish();
		if (then.health() > player.getHealth()) {
			player.setHealth(Math.min(then.health(), player.getMaxHealth()));
		}
		history.clear();
		Fx.particles(destination, ParticleTypes.REVERSE_PORTAL, player.getPos().add(0.0, 1.0, 0.0), 40, 0.4, 0.05);
		Fx.sound(destination, player.getPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8F, 1.6F);
		return true;
	}

	// ------------------------------------------------------------------ Lunchbox

	/**
	 * Eats one piece of plain food from the inventory if the player is hungry enough that nothing of it is wasted.
	 * Food with side effects (golden apples, rotten flesh, the special foods of this mod) is never eaten this way.
	 */
	private static void autoEat(ServerPlayerEntity player) {
		int foodLevel = player.getHungerManager().getFoodLevel();
		int missing = 20 - foodLevel;
		if (missing <= 0 || player.isCreative()) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		int best = -1;
		int bestNutrition = 0;
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			FoodComponent food = stack.get(DataComponentTypes.FOOD);
			if (food == null || !food.effects().isEmpty() || isSpecialFood(stack)) {
				continue;
			}
			boolean fits = food.nutrition() <= missing || foodLevel <= 6;
			if (fits && food.nutrition() > bestNutrition) {
				best = i;
				bestNutrition = food.nutrition();
			}
		}
		if (best < 0) {
			return;
		}
		// finishUsing is what the game calls when a player has eaten up. It feeds the player and gives the bowl back.
		ItemStack left = inventory.getStack(best).finishUsing(player.getWorld(), player);
		inventory.setStack(best, left);
	}

	private static boolean isSpecialFood(ItemStack stack) {
		if (stack.isOf(Items.CHORUS_FRUIT) || stack.isOf(Items.HONEY_BOTTLE) || stack.isOf(Items.SUSPICIOUS_STEW)) {
			return true;
		}
		Traits traits = ComboItems.traits(stack.getItem());
		return traits != null && (traits.eatBuffSeconds > 0 || traits.eatLaunch > 0);
	}

	// ------------------------------------------------------------------ Explorer's Almanac

	/** One line with the player's position, biome, day and time, light, and whether slimes can spawn here. */
	public static Text almanacText(ServerWorld world, ServerPlayerEntity player) {
		BlockPos pos = player.getBlockPos();
		long time = world.getTimeOfDay();
		long day = time / 24000L + 1L;
		int hour = (int) (((time % 24000L) / 1000L + 6L) % 24L);
		int minute = (int) ((time % 1000L) * 60L / 1000L);
		String biome = world.getBiome(pos).getKey().map(key -> key.getValue().getPath()).orElse("unknown").replace('_', ' ');
		ChunkPos chunk = new ChunkPos(pos);
		boolean slimes = world.getRegistryKey() == World.OVERWORLD
				&& ChunkRandom.getSlimeRandom(chunk.x, chunk.z, world.getSeed(), 987234911L).nextInt(10) == 0;
		String text = "X " + pos.getX() + "  Y " + pos.getY() + "  Z " + pos.getZ() + "  |  " + biome
				+ "  |  Day " + day + ", " + (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute
				+ "  |  Light " + world.getLightLevel(pos) + (slimes ? "  |  Slime chunk" : "");
		return Text.literal(text).formatted(Formatting.GOLD);
	}

	// ------------------------------------------------------------------ Pocket Mirror

	/** Returning false means: this damage does not happen. */
	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		try {
			if (!(entity instanceof ServerPlayerEntity player) || !source.isIn(IS_PROJECTILE)) {
				return true;
			}
			ItemStack mirror = PassiveAbilities.find(player, candidate -> candidate.reflectCooldown > 0);
			if (mirror == null || player.getItemCooldownManager().isCoolingDown(mirror.getItem())) {
				return true;
			}
			Traits traits = ComboItems.traits(mirror.getItem());
			player.getItemCooldownManager().set(mirror.getItem(), traits.reflectCooldown);
			ServerWorld world = player.getServerWorld();
			Entity shot = source.getSource();
			Entity shooter = source.getAttacker();
			if (shooter instanceof LivingEntity living && shooter != player && shooter.isAlive()) {
				living.damage(world.getDamageSources().thorns(player), amount);
				Fx.line(world, ParticleTypes.END_ROD, player.getEyePos(), living.getPos().add(0.0, living.getHeight() * 0.6, 0.0));
			}
			if (shot != null && shot != shooter && !(shot instanceof LivingEntity)) {
				shot.discard(); // the arrow is gone, it does not stick in the player
			}
			Fx.sound(world, player.getPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, 1.0F, 1.4F);
			return false;
		} catch (Throwable t) {
			ItemCombinator.error("Pocket Mirror failed", t);
			return true;
		}
	}

	// ------------------------------------------------------------------ Rodeo Saddle

	/** Runs on a right-click on any entity. PASS means: not our business, the game goes on as usual. */
	private static ActionResult mount(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hit) {
		ItemStack stack = player.getStackInHand(hand);
		Traits traits = ComboItems.traits(stack.getItem());
		if (traits == null || !traits.mount || player.isSpectator() || player.hasVehicle() || player.isSneaking()
				|| !(entity instanceof LivingEntity) || entity instanceof PlayerEntity || !entity.isAlive() || entity.hasPassengers()
				|| entity instanceof EnderDragonEntity || entity instanceof WitherEntity) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		try {
			if (player.startRiding(entity, true)) {
				stack.damage(1, player, LivingEntity.getSlotForHand(hand));
				Fx.sound(world, entity.getPos(), SoundEvents.ENTITY_HORSE_SADDLE, 1.0F, 1.0F);
				return ActionResult.SUCCESS;
			}
		} catch (Throwable t) {
			ItemCombinator.error("Rodeo Saddle failed", t);
		}
		return ActionResult.PASS;
	}
}
