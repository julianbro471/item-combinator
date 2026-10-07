package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.joml.Vector3f;

/** Teleportation: wormholes, the way back to your grave, out of caves, between floors, to friends, far away, and pets home. */
public final class Travel {
	/** A wormhole end: the free block in front of a wall, floor or ceiling, and which way it faces. */
	private record Portal(RegistryKey<World> world, BlockPos pos, Direction side) {
	}

	private static final Map<UUID, Portal[]> WORMHOLES = new HashMap<>();
	/** Things that just came out of a wormhole cannot go in again for a moment. Entity id to the tick when they may. */
	private static final Map<UUID, Integer> JUST_MOVED = new HashMap<>();
	private static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.2F, 0.6F, 1.0F), 1.2F);
	private static final DustParticleEffect ORANGE = new DustParticleEffect(new Vector3f(1.0F, 0.55F, 0.1F), 1.2F);
	private static int ticks = 0;

	private Travel() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Travel::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			WORMHOLES.clear();
			JUST_MOVED.clear();
		});
	}

	private static void go(ServerPlayerEntity player, ServerWorld destination, Vec3d spot) {
		if (player.getWorld() instanceof ServerWorld from) {
			Fx.particles(from, ParticleTypes.PORTAL, player.getPos().add(0.0, 1.0, 0.0), 40, 0.4, 0.3);
			Fx.sound(from, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
		}
		Worlds.move(player, destination, spot, player.getYaw(), player.getPitch());
		Fx.particles(destination, ParticleTypes.PORTAL, spot.add(0.0, 1.0, 0.0), 40, 0.4, 0.3);
		Fx.sound(destination, spot, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
	}

	// ------------------------------------------------------------------ the Wormhole Gun

	/** Wormhole Gun: right-click puts the blue end where you look, sneaking the orange end. */
	static boolean shootPortal(ServerWorld world, ServerPlayerEntity player, int range, boolean orange) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.literal("Aim at a wall, a floor or a ceiling."), true);
			return false;
		}
		BlockPos pos = hit.getBlockPos().offset(hit.getSide());
		if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
			return false;
		}
		Fx.line(world, orange ? ORANGE : BLUE, player.getEyePos(), hit.getPos());
		setPortal(player, world, pos, hit.getSide(), orange);
		return true;
	}

	/** Opens one end of the player's wormhole. The tests call this directly. */
	public static void setPortal(PlayerEntity player, ServerWorld world, BlockPos pos, Direction side, boolean orange) {
		Portal[] pair = WORMHOLES.computeIfAbsent(player.getUuid(), id -> new Portal[2]);
		pair[orange ? 1 : 0] = new Portal(world.getRegistryKey(), pos.toImmutable(), side);
		Fx.sound(world, Vec3d.ofCenter(pos), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0F, orange ? 0.8F : 1.4F);
		boolean open = pair[0] != null && pair[1] != null;
		player.sendMessage(Text.literal((orange ? "Orange" : "Blue") + " end placed." + (open ? " The wormhole is open." : ""))
				.formatted(orange ? Formatting.GOLD : Formatting.AQUA), true);
	}

	/** True when both ends of the player's wormhole are open. */
	public static boolean wormholeOpen(PlayerEntity player) {
		Portal[] pair = WORMHOLES.get(player.getUuid());
		return pair != null && pair[0] != null && pair[1] != null;
	}

	public static void closeWormhole(PlayerEntity player) {
		WORMHOLES.remove(player.getUuid());
	}

	/** The thin slice of the free block that touches the wall. Whatever enters it goes through. */
	private static Box mouth(Portal portal) {
		BlockPos p = portal.pos();
		Direction side = portal.side();
		boolean wall = side.getAxis().isHorizontal();
		Box box = new Box(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + (wall ? 2 : 1), p.getZ() + 1);
		double depth = 0.4;
		double inset = 0.15;
		return switch (side) {
			case UP -> new Box(box.minX + inset, box.minY, box.minZ + inset, box.maxX - inset, box.minY + depth, box.maxZ - inset);
			case DOWN -> new Box(box.minX + inset, box.maxY - depth, box.minZ + inset, box.maxX - inset, box.maxY, box.maxZ - inset);
			case SOUTH -> new Box(box.minX + inset, box.minY, box.minZ, box.maxX - inset, box.maxY, box.minZ + depth);
			case NORTH -> new Box(box.minX + inset, box.minY, box.maxZ - depth, box.maxX - inset, box.maxY, box.maxZ);
			case EAST -> new Box(box.minX, box.minY, box.minZ + inset, box.minX + depth, box.maxY, box.maxZ - inset);
			case WEST -> new Box(box.maxX - depth, box.minY, box.minZ + inset, box.maxX, box.maxY, box.maxZ - inset);
		};
	}

	private static void tick(MinecraftServer server) {
		ticks++;
		if (WORMHOLES.isEmpty()) {
			return;
		}
		try {
			JUST_MOVED.values().removeIf(until -> until < ticks);
			for (Portal[] pair : new ArrayList<>(WORMHOLES.values())) {
				for (int i = 0; i < 2; i++) {
					Portal from = pair[i];
					if (from == null) {
						continue;
					}
					ServerWorld world = server.getWorld(from.world());
					if (world == null || !world.isChunkLoaded(from.pos())) {
						continue;
					}
					if (ticks % 2 == 0) {
						ring(world, from, i == 0 ? BLUE : ORANGE);
					}
					Portal to = pair[1 - i];
					if (to == null) {
						continue;
					}
					ServerWorld exitWorld = server.getWorld(to.world());
					if (exitWorld == null) {
						continue;
					}
					List<Entity> entering = world.getOtherEntities(null, mouth(from), e -> e.isAlive() && !e.isSpectator()
							&& !e.hasVehicle() && !JUST_MOVED.containsKey(e.getUuid())
							&& (e instanceof LivingEntity || e instanceof ItemEntity || e instanceof ProjectileEntity
							|| e instanceof ExperienceOrbEntity || e instanceof AbstractMinecartEntity
							|| e instanceof net.minecraft.entity.FallingBlockEntity || e instanceof net.minecraft.entity.TntEntity));
					for (Entity entity : entering) {
						if (Worlds.isSmiler(entity) && exitWorld != world) {
							continue; // Smilers stay in the Backrooms
						}
						through(entity, exitWorld, to);
					}
				}
			}
		} catch (Throwable t) {
			ItemCombinator.error("Wormhole tick failed", t);
		}
	}

	/** Speedy thing goes in, speedy thing comes out: the speed is kept, the direction is the way the exit faces. */
	private static void through(Entity entity, ServerWorld exitWorld, Portal exit) {
		Direction side = exit.side();
		BlockPos q = exit.pos();
		double speed = Math.max(0.35, entity.getVelocity().length());
		Vec3d spot;
		if (side == Direction.UP) {
			spot = new Vec3d(q.getX() + 0.5, q.getY() + 0.05, q.getZ() + 0.5);
		} else if (side == Direction.DOWN) {
			spot = new Vec3d(q.getX() + 0.5, q.getY() + 1.0 - entity.getHeight() - 0.05, q.getZ() + 0.5);
		} else {
			spot = new Vec3d(q.getX() + 0.5 + side.getOffsetX() * 0.1, q.getY() + 0.01, q.getZ() + 0.5 + side.getOffsetZ() * 0.1);
		}
		float yaw = side.getAxis().isHorizontal() ? side.asRotation() : entity.getYaw();
		Vec3d velocity = Vec3d.of(side.getVector()).multiply(speed);
		Entity moved = Worlds.move(entity, exitWorld, spot, yaw, entity.getPitch());
		if (moved == null) {
			return;
		}
		moved.setVelocity(velocity);
		moved.velocityModified = true;
		moved.fallDistance = 0.0F;
		JUST_MOVED.put(moved.getUuid(), ticks + 15);
		Fx.sound(exitWorld, spot, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6F, 1.6F);
	}

	private static void ring(ServerWorld world, Portal portal, DustParticleEffect color) {
		Direction side = portal.side();
		boolean wall = side.getAxis().isHorizontal();
		Vec3d center = Vec3d.ofCenter(portal.pos()).add(0.0, wall ? 0.5 : 0.0, 0.0)
				.subtract(Vec3d.of(side.getVector()).multiply(0.45));
		for (int k = 0; k < 14; k++) {
			double a = k * Math.PI * 2.0 / 14.0;
			double u = Math.cos(a) * 0.45;
			double v = Math.sin(a) * (wall ? 0.95 : 0.45);
			Vec3d at = switch (side.getAxis()) {
				case Y -> center.add(u, 0.0, v);
				case X -> center.add(0.0, v, u);
				case Z -> center.add(u, v, 0.0);
			};
			world.spawnParticles(color, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	// ------------------------------------------------------------------ simple jumps

	/** Grave Compass: to the spot where you last died, in any world. */
	static boolean graveWarp(ServerWorld world, ServerPlayerEntity player) {
		Optional<GlobalPos> death = player.getLastDeathPos();
		if (death.isEmpty()) {
			player.sendMessage(Text.literal("You have not died yet. Lucky you."), true);
			return false;
		}
		ServerWorld destination = world.getServer().getWorld(death.get().dimension());
		if (destination == null) {
			player.sendMessage(Text.literal("The world where you died is gone."), true);
			return false;
		}
		BlockPos pos = death.get().pos();
		destination.getChunk(pos);
		BlockPos feet = pos.getY() > destination.getBottomY() && Fx.hasRoom(destination, pos) ? pos
				: Worlds.landing(destination, pos.getX(), pos.getZ(), pos.getY(), Blocks.COBBLESTONE);
		go(player, destination, Vec3d.ofBottomCenter(feet));
		player.sendMessage(Text.literal("This is where you died.").formatted(Formatting.GRAY), true);
		return true;
	}

	/** Escape Rope: straight up to the open sky. In the Nether that is the roof. */
	static boolean escape(ServerWorld world, ServerPlayerEntity player) {
		BlockPos feet = player.getBlockPos();
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ());
		if (top <= feet.getY() + 1) {
			player.sendMessage(Text.literal("There is nothing above you but the sky."), true);
			return false;
		}
		BlockPos spot = new BlockPos(feet.getX(), top, feet.getZ());
		if (!Fx.hasRoom(world, spot)) {
			spot = Worlds.landing(world, feet.getX(), feet.getZ(), top, Blocks.COBBLESTONE);
		}
		Fx.line(world, ParticleTypes.CRIT, player.getPos(), Vec3d.ofBottomCenter(spot));
		go(player, world, Vec3d.ofBottomCenter(spot));
		return true;
	}

	/** Elevator Pearl: up through the ceiling to the next place to stand, or (sneaking) down through the floor. */
	static boolean elevator(ServerWorld world, ServerPlayerEntity player, boolean up, int range) {
		BlockPos feet = player.getBlockPos();
		int top = world.getBottomY() + world.getDimension().logicalHeight() - 2;
		if (up) {
			boolean pastCeiling = false;
			for (int dy = 2; dy <= range; dy++) {
				BlockPos pos = feet.up(dy);
				if (pos.getY() > top) {
					break;
				}
				if (!pastCeiling) {
					pastCeiling = !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
					continue;
				}
				if (Fx.hasRoom(world, pos)) {
					go(player, world, Vec3d.ofBottomCenter(pos));
					return true;
				}
			}
			player.sendMessage(Text.literal("No floor to stand on above you."), true);
		} else {
			for (int dy = 2; dy <= range; dy++) {
				BlockPos pos = feet.down(dy);
				if (pos.getY() <= world.getBottomY()) {
					break;
				}
				if (Fx.hasRoom(world, pos)) {
					go(player, world, Vec3d.ofBottomCenter(pos));
					return true;
				}
			}
			player.sendMessage(Text.literal("No room to stand in below you."), true);
		}
		return false;
	}

	/** The nearest other player: in this world if there is one, else anywhere. */
	static ServerPlayerEntity nearestPlayer(ServerPlayerEntity player) {
		ServerPlayerEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		MinecraftServer server = player.getServer();
		if (server == null) {
			return null;
		}
		for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
			if (other == player || !other.isAlive() || other.isSpectator()) {
				continue;
			}
			double distance = other.getWorld() == player.getWorld() ? other.squaredDistanceTo(player) : 1.0E30;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = other;
			}
		}
		return best;
	}

	/** Friendship Bracelet: to the nearest player, or (sneaking) the nearest player comes to you. */
	static boolean bracelet(ServerWorld world, ServerPlayerEntity player, boolean pull) {
		ServerPlayerEntity friend = nearestPlayer(player);
		if (friend == null) {
			player.sendMessage(Text.literal("Nobody else is here. The bracelet feels lonely."), true);
			return false;
		}
		if (pull) {
			go(friend, world, player.getPos());
			friend.sendMessage(Text.literal(player.getName().getString() + " pulled you over with a Friendship Bracelet.")
					.formatted(Formatting.LIGHT_PURPLE), false);
		} else {
			go(player, (ServerWorld) friend.getWorld(), friend.getPos());
			friend.sendMessage(Text.literal(player.getName().getString() + " came to visit.").formatted(Formatting.LIGHT_PURPLE), false);
		}
		Fx.particles((ServerWorld) player.getWorld(), ParticleTypes.HEART, player.getEyePos(), 6, 0.5, 0.0);
		return true;
	}

	/** Wanderlust Atlas: 1000 to 4000 blocks away in a random direction, onto dry land if there is any. */
	static boolean wander(ServerWorld world, ServerPlayerEntity player) {
		Random random = world.random;
		int bottom = world.getBottomY() + 1;
		BlockPos found = null;
		int x = player.getBlockX();
		int z = player.getBlockZ();
		for (int attempt = 0; attempt < 6 && found == null; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 1000.0 + random.nextDouble() * 3000.0;
			x = MathHelper.floor(player.getX() + Math.cos(angle) * distance);
			z = MathHelper.floor(player.getZ() + Math.sin(angle) * distance);
			if (!world.getWorldBorder().contains(x, z)) {
				continue;
			}
			if (world.getDimension().hasCeiling()) {
				found = Worlds.landing(world, x, z, player.getBlockY(), Blocks.NETHERRACK);
				break;
			}
			Chunk chunk = world.getChunk(x >> 4, z >> 4);
			int top = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
			BlockPos feet = new BlockPos(x, top, z);
			if (top > bottom && Fx.hasRoom(world, feet)) {
				found = feet;
			}
		}
		if (found == null) {
			if (!world.getWorldBorder().contains(x, z)) {
				player.sendMessage(Text.literal("The world border is too close."), true);
				return false;
			}
			found = Worlds.landing(world, x, z, 64, Blocks.OAK_PLANKS); // a raft on the ocean
		}
		int far = (int) Math.round(Math.sqrt(player.getBlockPos().getSquaredDistance(found)));
		go(player, world, Vec3d.ofBottomCenter(found));
		player.sendMessage(Text.literal("You wandered " + far + " blocks, to " + found.getX() + " " + found.getY() + " " + found.getZ() + ".")
				.formatted(Formatting.GOLD), false);
		return true;
	}

	/** Pet Whistle: every tame animal of yours, in every loaded place of every world, comes to you. */
	static boolean petRecall(ServerWorld world, ServerPlayerEntity player) {
		List<Entity> pets = new ArrayList<>();
		UUID me = player.getUuid();
		for (ServerWorld each : world.getServer().getWorlds()) {
			for (Entity entity : each.iterateEntities()) {
				boolean mine = entity instanceof TameableEntity tame && tame.isTamed() && me.equals(tame.getOwnerUuid())
						|| entity instanceof AbstractHorseEntity horse && horse.isTame() && me.equals(horse.getOwnerUuid());
				if (mine && entity.isAlive() && entity.distanceTo(player) > 3.0F) {
					pets.add(entity);
				}
			}
		}
		if (pets.isEmpty()) {
			player.sendMessage(Text.literal("None of your pets is far away. Or you have none."), true);
			return false;
		}
		Random random = world.random;
		for (Entity pet : pets) {
			if (pet instanceof TameableEntity tame) {
				tame.setSitting(false);
			}
			Vec3d spot = player.getPos().add(random.nextDouble() * 3.0 - 1.5, 0.0, random.nextDouble() * 3.0 - 1.5);
			Worlds.move(pet, world, spot, pet.getYaw(), 0.0F);
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_WOLF_AMBIENT, 1.2F, 1.3F);
		player.sendMessage(Text.literal(pets.size() + (pets.size() == 1 ? " pet came" : " pets came") + " running.").formatted(Formatting.GREEN), true);
		return true;
	}
}
