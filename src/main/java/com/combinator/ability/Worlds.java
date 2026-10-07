package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * The five worlds of this mod and the travel between worlds.
 * Pocket Dimension: a private furnished room for every player, under the stars.
 * Backrooms: endless yellow rooms, humming lamps, something with eyes in the dark.
 * Sky Realm: floating islands with nothing below. Fall off and you fall back into your own world.
 * Moon: grey dust, craters, a black sky and a sixth of the gravity.
 * The Other Side: a second copy of the world's own land, the same seed, never touched by anyone.
 */
public final class Worlds {
	public static final RegistryKey<World> POCKET = key("pocket");
	public static final RegistryKey<World> BACKROOMS = key("backrooms");
	public static final RegistryKey<World> SKY_REALM = key("sky_realm");
	public static final RegistryKey<World> MOON = key("moon");
	public static final RegistryKey<World> PARALLEL = key("parallel");
	private static final List<RegistryKey<World>> MOD_WORLDS = List.of(POCKET, BACKROOMS, SKY_REALM, MOON, PARALLEL);

	/** Where a player came from when they entered a world of this mod. Survives a restart and death. */
	private static final AttachmentType<GlobalPos> RETURN_POINT = AttachmentRegistry.<GlobalPos>builder()
			.persistent(GlobalPos.CODEC)
			.copyOnDeath()
			.buildAndRegister(ItemCombinator.id("return_point"));

	private static final Identifier MOON_GRAVITY = ItemCombinator.id("moon_gravity");
	/** A block at height 0 in the corner of a chunk of the Backrooms or the Moon: this chunk was furnished already. */
	private static final BlockState DONE_MARK = Blocks.REINFORCED_DEEPSLATE.getDefaultState();
	private static final String SMILER_TAG = "combinator_smiler";

	/** Pocket rooms: floor height and half size (the room is 17 x 17 blocks with the walls). */
	private static final int ROOM_Y = 64;
	private static final int ROOM_HALF = 8;
	/** Backrooms: the floor is at height 3, the free space from 4 to 7, the ceiling with the lamps at 8. */
	private static final int HALL_FLOOR = 3;
	private static final int HALL_CEILING = 8;
	/** Moon: the top of the dust. */
	private static final int MOON_SURFACE = 48;

	private static int ticks = 0;

	private Worlds() {
	}

	private static RegistryKey<World> key(String path) {
		return RegistryKey.of(RegistryKeys.WORLD, ItemCombinator.id(path));
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Worlds::tick);
		// Weight changes at once when something enters or leaves the Moon, whatever moved it.
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
				lowGravity(player, destination.getRegistryKey() == MOON));
		ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((original, moved, origin, destination) -> {
			if (moved instanceof LivingEntity living) {
				lowGravity(living, destination.getRegistryKey() == MOON);
			}
		});
	}

	public static boolean isModWorld(World world) {
		return MOD_WORLDS.contains(world.getRegistryKey());
	}

	// ------------------------------------------------------------------ moving things between worlds

	/** Moves anything to any place in any world. Returns the moved entity (a new object if a mob changed worlds), or null. */
	static Entity move(Entity entity, ServerWorld destination, Vec3d pos, float yaw, float pitch) {
		if (entity.hasVehicle()) {
			entity.stopRiding();
		}
		if (entity instanceof ServerPlayerEntity player) {
			player.teleport(destination, pos.x, pos.y, pos.z, yaw, pitch);
			player.fallDistance = 0.0F;
			return player;
		}
		entity.removeAllPassengers();
		if (entity.getWorld() == destination) {
			entity.requestTeleport(pos.x, pos.y, pos.z);
			entity.setYaw(yaw);
			entity.fallDistance = 0.0F;
			return entity;
		}
		Entity moved = entity.teleportTo(new TeleportTarget(destination, pos, Vec3d.ZERO, yaw, pitch, TeleportTarget.NO_OP));
		if (moved != null) {
			moved.fallDistance = 0.0F;
		}
		return moved;
	}

	static ServerWorld world(MinecraftServer server, RegistryKey<World> key, ServerPlayerEntity player) {
		ServerWorld world = server.getWorld(key);
		if (world == null && player != null) {
			player.sendMessage(Text.literal("This world has no " + key.getValue().getPath()
					+ ". It was probably made before this version of the mod. Make a new world.").formatted(Formatting.RED), false);
		}
		return world;
	}

	private static GlobalPos returnPoint(PlayerEntity player) {
		return ((AttachmentTarget) player).getAttached(RETURN_POINT);
	}

	/** Remembers where the player is now, unless they are already in one of the mod's worlds. */
	private static void rememberHome(ServerPlayerEntity player) {
		if (!isModWorld(player.getWorld())) {
			((AttachmentTarget) player).setAttached(RETURN_POINT, GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos()));
		}
	}

	/** Sends the player back to where they came from: the remembered spot, or else their spawn point. */
	public static boolean goHome(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return false;
		}
		GlobalPos home = returnPoint(player);
		((AttachmentTarget) player).removeAttached(RETURN_POINT);
		ServerWorld destination = home == null ? null : server.getWorld(home.dimension());
		BlockPos spot;
		if (destination != null && !isModWorld(destination)) {
			spot = home.pos();
		} else if (player.getSpawnPointPosition() != null && server.getWorld(player.getSpawnPointDimension()) != null
				&& !MOD_WORLDS.contains(player.getSpawnPointDimension())) {
			destination = server.getWorld(player.getSpawnPointDimension());
			spot = player.getSpawnPointPosition().up();
		} else {
			destination = server.getOverworld();
			spot = destination.getSpawnPos();
		}
		destination.getChunk(spot);
		if (!Fx.hasRoom(destination, spot)) {
			spot = landing(destination, spot.getX(), spot.getZ(), spot.getY(), Blocks.COBBLESTONE);
		}
		departure(player);
		move(player, destination, Vec3d.ofBottomCenter(spot), player.getYaw(), player.getPitch());
		arrival(destination, player.getPos());
		player.sendMessage(Text.literal("Home again.").formatted(Formatting.GREEN), true);
		return true;
	}

	private static void departure(Entity entity) {
		if (entity.getWorld() instanceof ServerWorld world) {
			Fx.particles(world, ParticleTypes.REVERSE_PORTAL, entity.getPos().add(0.0, 1.0, 0.0), 60, 0.5, 0.3);
			Fx.sound(world, entity.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.6F);
		}
	}

	private static void arrival(ServerWorld world, Vec3d at) {
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, at.add(0.0, 1.0, 0.0), 60, 0.5, 0.3);
		Fx.sound(world, at, SoundEvents.BLOCK_PORTAL_TRAVEL, 0.25F, 1.6F);
	}

	/**
	 * A spot where a player can stand at x, z, close to the given height. Loads the land there first.
	 * If there is no such spot (the void, the open sea), a small platform of the given block is built.
	 */
	static BlockPos landing(ServerWorld world, int x, int z, int nearY, Block platform) {
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		int bottom = world.getBottomY() + 1;
		int top = world.getBottomY() + world.getDimension().logicalHeight() - 3;
		int surface = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
		if (!world.getDimension().hasCeiling() && surface > bottom && surface <= top) {
			BlockPos feet = new BlockPos(x, surface, z);
			if (Fx.hasRoom(world, feet) && safeGround(world.getBlockState(feet.down()))) {
				return feet;
			}
		}
		int start = MathHelper.clamp(nearY, bottom, top);
		for (int d = 0; d <= 96; d++) {
			for (int sign = 1; sign >= -1; sign -= 2) {
				int y = start + d * sign;
				if (y < bottom || y > top || (d == 0 && sign < 0)) {
					continue;
				}
				BlockPos feet = new BlockPos(x, y, z);
				if (Fx.hasRoom(world, feet) && safeGround(world.getBlockState(feet.down()))) {
					return feet;
				}
			}
		}
		// Nothing to stand on: build a platform, on the sea if there is one.
		int y = !world.getDimension().hasCeiling() && surface > bottom && surface <= top ? surface : start;
		BlockPos feet = new BlockPos(x, y, z);
		for (BlockPos pos : BlockPos.iterate(feet.add(-1, -1, -1), feet.add(1, -1, 1))) {
			world.setBlockState(pos.toImmutable(), platform.getDefaultState());
		}
		for (BlockPos pos : BlockPos.iterate(feet.add(-1, 0, -1), feet.add(1, 1, 1))) {
			if (world.getBlockState(pos).getHardness(world, pos) >= 0.0F) {
				world.setBlockState(pos.toImmutable(), Fx.AIR);
			}
		}
		return feet;
	}

	private static boolean safeGround(BlockState ground) {
		return !ground.isOf(Blocks.MAGMA_BLOCK) && !ground.isOf(Blocks.CACTUS) && !ground.isOf(Blocks.CAMPFIRE)
				&& !ground.isOf(Blocks.SOUL_CAMPFIRE) && !ground.isOf(Blocks.POWDER_SNOW) && !ground.isOf(Blocks.BEDROCK);
	}

	/**
	 * The shared rule of the world items: in their own world they bring you home. Anywhere else they take you there,
	 * and from a normal world they remember where you were.
	 */
	private static boolean enter(ServerWorld world, ServerPlayerEntity player, RegistryKey<World> key, Arrival arrival) {
		if (world.getRegistryKey() == key) {
			return goHome(player);
		}
		ServerWorld destination = world(world.getServer(), key, player);
		if (destination == null) {
			return false;
		}
		Vec3d spot = arrival.spot(destination, player);
		if (spot == null) {
			return false;
		}
		rememberHome(player);
		departure(player);
		move(player, destination, spot, player.getYaw(), 0.0F);
		arrival(destination, spot);
		return true;
	}

	private interface Arrival {
		Vec3d spot(ServerWorld destination, ServerPlayerEntity player);
	}

	// ------------------------------------------------------------------ the Pocket Dimension

	/** The middle of the floor of the player's own room. Every player has a different one. */
	public static BlockPos pocketRoom(UUID owner) {
		long bits = owner.getMostSignificantBits() ^ owner.getLeastSignificantBits();
		int hash = (int) (bits ^ (bits >>> 32));
		int gx = (hash & 1023) - 512;
		int gz = ((hash >>> 10) & 1023) - 512;
		return new BlockPos(gx * 64, ROOM_Y, gz * 64);
	}

	/** Pocket Dimension Cube: into your own room. Sneaking takes along everything alive within 4 blocks. */
	static boolean pocket(ServerWorld world, ServerPlayerEntity player, boolean group) {
		List<Entity> company = new ArrayList<>();
		if (group) {
			for (Entity entity : world.getOtherEntities(player, player.getBoundingBox().expand(4.0),
					e -> e.isAlive() && (e instanceof LivingEntity || e instanceof ItemEntity) && !e.hasVehicle())) {
				company.add(entity);
			}
		}
		if (world.getRegistryKey() == POCKET) {
			Vec3d from = player.getPos();
			if (!goHome(player)) {
				return false;
			}
			bring(company, (ServerWorld) player.getWorld(), player.getPos(), from);
			return true;
		}
		ServerWorld pocket = world(world.getServer(), POCKET, player);
		if (pocket == null) {
			return false;
		}
		BlockPos room = pocketRoom(player.getUuid());
		pocket.getChunk(room);
		buildRoom(pocket, room);
		for (Entity entity : company) {
			if (entity instanceof ServerPlayerEntity other) {
				rememberHome(other);
			}
		}
		Vec3d from = player.getPos();
		rememberHome(player);
		departure(player);
		Vec3d spot = Vec3d.ofBottomCenter(room.up());
		move(player, pocket, spot, 180.0F, 0.0F);
		arrival(pocket, spot);
		bring(company, pocket, spot, from);
		player.sendMessage(Text.literal("Your Pocket Dimension. Use the cube again, or step on the lodestone, to leave.")
				.formatted(Formatting.LIGHT_PURPLE), true);
		return true;
	}

	/** Moves the company along, keeping where each one stood around the player. */
	private static void bring(List<Entity> company, ServerWorld destination, Vec3d center, Vec3d oldCenter) {
		for (Entity entity : company) {
			Vec3d offset = entity.getPos().subtract(oldCenter);
			Vec3d spot = center.add(MathHelper.clamp(offset.x, -3.0, 3.0), 0.0, MathHelper.clamp(offset.z, -3.0, 3.0));
			if (entity instanceof ServerPlayerEntity other) {
				departure(other);
			}
			move(entity, destination, spot, entity.getYaw(), entity.getPitch());
		}
	}

	/** Builds the room the first time. Later visits find it as it was left. */
	private static void buildRoom(ServerWorld world, BlockPos c) {
		if (!world.getBlockState(c).isAir() || !world.getBlockState(c.add(ROOM_HALF, 0, ROOM_HALF)).isAir()) {
			return;
		}
		int h = ROOM_HALF;
		for (int dx = -h; dx <= h; dx++) {
			for (int dz = -h; dz <= h; dz++) {
				boolean edge = Math.abs(dx) == h || Math.abs(dz) == h;
				boolean cross = dx == 0 || dz == 0;
				put(world, c.add(dx, 0, dz), edge ? Blocks.QUARTZ_BRICKS : cross ? Blocks.PURPUR_BLOCK : Blocks.SMOOTH_QUARTZ);
				put(world, c.add(dx, 9, dz), edge ? Blocks.QUARTZ_BRICKS : Blocks.GLASS);
				if (edge) {
					for (int y = 1; y <= 8; y++) {
						boolean lamp = y == 4 && (dx % 4 == 0 || dz % 4 == 0) && !(Math.abs(dx) == h && Math.abs(dz) == h);
						put(world, c.add(dx, y, dz), lamp ? Blocks.SEA_LANTERN : Blocks.QUARTZ_BRICKS);
					}
				}
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				put(world, c.add(4 * sx, 0, 4 * sz), Blocks.SEA_LANTERN);
			}
		}
		// along the north wall: workshops
		Block[] workshop = {Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.STONECUTTER,
				Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE, Blocks.ANVIL};
		for (int i = 0; i < workshop.length; i++) {
			BlockState state = workshop[i].getDefaultState();
			if (state.contains(net.minecraft.state.property.Properties.HORIZONTAL_FACING)) {
				state = state.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, Direction.SOUTH);
			}
			world.setBlockState(c.add(-5 + i, 1, -h + 1), state, Fx.QUIET);
		}
		// along the east wall: barrels to store things, and an Ender Chest
		for (int dz = -4; dz <= 1; dz++) {
			for (int y = 1; y <= 2; y++) {
				world.setBlockState(c.add(h - 1, y, dz), Blocks.BARREL.getDefaultState()
						.with(net.minecraft.state.property.Properties.FACING, Direction.WEST), Fx.QUIET);
			}
		}
		world.setBlockState(c.add(h - 1, 1, 3), Blocks.ENDER_CHEST.getDefaultState()
				.with(net.minecraft.state.property.Properties.HORIZONTAL_FACING, Direction.WEST), Fx.QUIET);
		// along the west wall: an enchanting table with books
		world.setBlockState(c.add(-h + 3, 1, 0), Blocks.ENCHANTING_TABLE.getDefaultState(), Fx.QUIET);
		for (int dz = -2; dz <= 2; dz++) {
			for (int y = 1; y <= 2; y++) {
				put(world, c.add(-h + 1, y, dz), Blocks.BOOKSHELF);
			}
		}
		// a bed by the south wall
		BlockState bed = Blocks.PURPLE_BED.getDefaultState().with(BedBlock.FACING, Direction.SOUTH);
		world.setBlockState(c.add(-3, 1, h - 2), bed.with(BedBlock.PART, BedPart.FOOT), Fx.QUIET);
		world.setBlockState(c.add(-3, 1, h - 1), bed.with(BedBlock.PART, BedPart.HEAD), Fx.QUIET);
		// the way out
		put(world, c.add(h - 2, 0, h - 2), Blocks.LODESTONE);
		put(world, c.add(h - 2, 8, h - 2), Blocks.SEA_LANTERN);
	}

	private static void put(ServerWorld world, BlockPos pos, Block block) {
		world.setBlockState(pos, block.getDefaultState(), Fx.QUIET);
	}

	// ------------------------------------------------------------------ the Backrooms

	/** Noclip Pearl: through the floor of reality into the Backrooms. */
	static boolean backrooms(ServerWorld world, ServerPlayerEntity player) {
		boolean entered = enter(world, player, BACKROOMS, (hall, p) -> hallSpot(hall, p.getBlockX(), p.getBlockZ()));
		if (entered && player.getWorld().getRegistryKey() == BACKROOMS) {
			player.sendMessage(Text.literal("You clipped through the floor. Do not look at the eyes.").formatted(Formatting.YELLOW), true);
		}
		return entered;
	}

	/** A free spot in the Backrooms near x, z. The rooms around it are furnished first. */
	private static Vec3d hallSpot(ServerWorld hall, int x, int z) {
		ChunkPos middle = new ChunkPos(new BlockPos(x, 0, z));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				hall.getChunk(middle.x + dx, middle.z + dz);
				decorateBackrooms(hall, new ChunkPos(middle.x + dx, middle.z + dz));
			}
		}
		for (int r = 0; r <= 6; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					BlockPos feet = new BlockPos(x + dx, HALL_FLOOR + 1, z + dz);
					if (Fx.hasRoom(hall, feet)) {
						return Vec3d.ofBottomCenter(feet);
					}
				}
			}
		}
		BlockPos feet = new BlockPos(x, HALL_FLOOR + 1, z);
		hall.setBlockState(feet, Fx.AIR);
		hall.setBlockState(feet.up(), Fx.AIR);
		return Vec3d.ofBottomCenter(feet);
	}

	/** Walls with doorways, humming lamps, now and then Almond Water, and very rarely a way out. */
	static void decorateBackrooms(ServerWorld world, ChunkPos chunk) {
		BlockPos mark = new BlockPos(chunk.getStartX(), 0, chunk.getStartZ());
		if (!world.getBlockState(mark).isOf(Blocks.BEDROCK)) {
			return;
		}
		world.setBlockState(mark, DONE_MARK, Fx.QUIET);
		Random random = Random.create(world.getSeed() ^ chunk.toLong() * 341873128712L);
		int x0 = chunk.getStartX();
		int z0 = chunk.getStartZ();
		BlockState wall = Blocks.SMOOTH_SANDSTONE.getDefaultState();
		// the west and the north edge of every chunk are walls with one or two doorways
		for (int side = 0; side < 2; side++) {
			if (random.nextFloat() < 0.2F) {
				continue; // a wide open hall
			}
			boolean[] open = new boolean[16];
			int doors = 1 + random.nextInt(2);
			for (int d = 0; d < doors; d++) {
				int at = 1 + random.nextInt(12);
				int width = 2 + random.nextInt(3);
				for (int i = at; i < Math.min(16, at + width); i++) {
					open[i] = true;
				}
			}
			for (int i = 0; i < 16; i++) {
				if (!open[i]) {
					for (int y = HALL_FLOOR + 1; y < HALL_CEILING; y++) {
						world.setBlockState(side == 0 ? new BlockPos(x0, y, z0 + i) : new BlockPos(x0 + i, y, z0), wall, Fx.QUIET);
					}
				}
			}
		}
		// a wall in the middle of the room, and a pillar or two
		if (random.nextBoolean()) {
			boolean alongX = random.nextBoolean();
			int line = 4 + random.nextInt(8);
			int from = 1 + random.nextInt(6);
			int length = 4 + random.nextInt(8);
			for (int i = from; i < Math.min(16, from + length); i++) {
				for (int y = HALL_FLOOR + 1; y < HALL_CEILING; y++) {
					world.setBlockState(alongX ? new BlockPos(x0 + i, y, z0 + line) : new BlockPos(x0 + line, y, z0 + i), wall, Fx.QUIET);
				}
			}
		}
		for (int p = random.nextInt(3); p > 0; p--) {
			int px = x0 + 2 + random.nextInt(12);
			int pz = z0 + 2 + random.nextInt(12);
			for (int y = HALL_FLOOR + 1; y < HALL_CEILING; y++) {
				world.setBlockState(new BlockPos(px, y, pz), wall, Fx.QUIET);
			}
		}
		// lamps in the ceiling, except in the dark rooms
		boolean dark = random.nextFloat() < 0.1F;
		BlockState lamp = Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true);
		for (int i = 2; i < 16; i += 4) {
			for (int j = 2; j < 16; j += 4) {
				if (!dark && random.nextFloat() < 0.85F) {
					world.setBlockState(new BlockPos(x0 + i, HALL_CEILING, z0 + j), lamp, Fx.QUIET);
				}
			}
		}
		// very rarely a way out: a lodestone in the floor under a green light
		if (random.nextInt(40) == 0) {
			BlockPos exit = new BlockPos(x0 + 4 + random.nextInt(8), HALL_FLOOR, z0 + 4 + random.nextInt(8));
			world.setBlockState(exit, Blocks.LODESTONE.getDefaultState(), Fx.QUIET);
			world.setBlockState(exit.up(HALL_CEILING - HALL_FLOOR), Blocks.VERDANT_FROGLIGHT.getDefaultState(), Fx.QUIET);
			for (int y = HALL_FLOOR + 1; y < HALL_CEILING; y++) {
				world.setBlockState(exit.up(y - HALL_FLOOR), Fx.AIR, Fx.QUIET);
			}
		}
		// Almond Water on the floor
		if (random.nextInt(5) == 0) {
			BlockPos spot = new BlockPos(x0 + 2 + random.nextInt(12), HALL_FLOOR + 1, z0 + 2 + random.nextInt(12));
			if (world.getBlockState(spot).isAir()) {
				ItemEntity water = new ItemEntity(world, spot.getX() + 0.5, spot.getY() + 0.1, spot.getZ() + 0.5,
						new ItemStack(ComboItems.ALMOND_WATER));
				water.setNeverDespawn();
				water.setVelocity(Vec3d.ZERO);
				world.spawnEntity(water);
			}
		}
	}

	private static void tickBackrooms(ServerWorld world, ServerPlayerEntity player) {
		furnishAround(world, player, 4, true);
		Random random = world.random;
		// The lamps flicker.
		if (ticks % 3 == 0 && random.nextInt(3) == 0) {
			int x = player.getBlockX() + random.nextInt(25) - 12;
			int z = player.getBlockZ() + random.nextInt(25) - 12;
			x = (x & ~3) + 2;
			z = (z & ~3) + 2;
			BlockPos pos = new BlockPos(x, HALL_CEILING, z);
			BlockState state = world.getBlockState(pos);
			if (state.isOf(Blocks.REDSTONE_LAMP) && state.get(RedstoneLampBlock.LIT)) {
				world.setBlockState(pos, state.with(RedstoneLampBlock.LIT, false), Fx.QUIET);
				Tasks.later(2 + random.nextInt(8), () -> {
					BlockState now = world.getBlockState(pos);
					if (now.isOf(Blocks.REDSTONE_LAMP) && !now.get(RedstoneLampBlock.LIT)) {
						world.setBlockState(pos, now.with(RedstoneLampBlock.LIT, true), Fx.QUIET);
					}
				});
			}
		}
		// Something smiles in the dark.
		if (ticks % 200 == 0 && random.nextFloat() < 0.35F) {
			int near = world.getEntitiesByClass(EndermanEntity.class, player.getBoundingBox().expand(48.0),
					e -> e.getCommandTags().contains(SMILER_TAG)).size();
			if (near < 3) {
				spawnSmiler(world, player);
			}
		}
	}

	/** A Smiler: an invisible enderman. Only its eyes can be seen. Do not look at them. */
	public static EndermanEntity spawnSmiler(ServerWorld world, PlayerEntity near) {
		Random random = world.random;
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 10.0 + random.nextDouble() * 10.0;
			BlockPos feet = BlockPos.ofFloored(near.getX() + Math.cos(angle) * distance, HALL_FLOOR + 1, near.getZ() + Math.sin(angle) * distance);
			if (!world.isChunkLoaded(feet) || !Fx.hasRoom(world, feet) || !world.getBlockState(feet.up(2)).getCollisionShape(world, feet.up(2)).isEmpty()) {
				continue;
			}
			EndermanEntity smiler = EntityType.ENDERMAN.create(world);
			if (smiler == null) {
				return null;
			}
			smiler.refreshPositionAndAngles(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
			smiler.setCustomName(Text.literal("Smiler"));
			smiler.setCustomNameVisible(false);
			smiler.addCommandTag(SMILER_TAG);
			smiler.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, StatusEffectInstance.INFINITE, 0, false, false));
			world.spawnEntity(smiler);
			Fx.sound(world, smiler.getPos(), SoundEvents.AMBIENT_CAVE.value(), 1.0F, 0.6F);
			return smiler;
		}
		return null;
	}

	// ------------------------------------------------------------------ the Sky Realm

	/** Cloud Key: up to the floating islands of the Sky Realm. */
	static boolean skyRealm(ServerWorld world, ServerPlayerEntity player) {
		boolean entered = enter(world, player, SKY_REALM, (sky, p) -> Vec3d.ofBottomCenter(skyLanding(sky, p.getBlockX(), p.getBlockZ())));
		if (entered && player.getWorld().getRegistryKey() == SKY_REALM) {
			player.sendMessage(Text.literal("The Sky Realm. Mind the edge.").formatted(Formatting.AQUA), true);
		}
		return entered;
	}

	/** The nearest island top near x, z. With no island in reach: a cloud to stand on. */
	private static BlockPos skyLanding(ServerWorld sky, int x, int z) {
		for (int ring = 0; ring <= 3; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					int tx = x + dx * 40;
					int tz = z + dz * 40;
					Chunk chunk = sky.getChunk(tx >> 4, tz >> 4);
					int top = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, tx & 15, tz & 15) + 1;
					if (top > 8) {
						BlockPos feet = new BlockPos(tx, top, tz);
						if (Fx.hasRoom(sky, feet)) {
							return feet;
						}
					}
				}
			}
		}
		BlockPos feet = new BlockPos(x, 120, z);
		for (BlockPos pos : BlockPos.iterate(feet.add(-3, -1, -3), feet.add(3, -1, 3))) {
			if (Math.abs(pos.getX() - x) + Math.abs(pos.getZ() - z) <= 4) {
				sky.setBlockState(pos.toImmutable(), Blocks.WHITE_WOOL.getDefaultState());
			}
		}
		return feet;
	}

	/** Below the islands there is nothing. Who falls off, falls back into their own world, high above the ground. */
	private static void fallOutOfTheSky(ServerWorld sky, ServerPlayerEntity player) {
		MinecraftServer server = sky.getServer();
		GlobalPos home = returnPoint(player);
		((AttachmentTarget) player).removeAttached(RETURN_POINT);
		ServerWorld destination = home == null ? null : server.getWorld(home.dimension());
		if (destination == null || isModWorld(destination)) {
			destination = server.getOverworld();
		}
		int x = player.getBlockX();
		int z = player.getBlockZ();
		Vec3d spot;
		if (destination.getDimension().hasCeiling()) {
			spot = Vec3d.ofBottomCenter(landing(destination, x, z, 64, Blocks.NETHERRACK));
		} else {
			destination.getChunk(x >> 4, z >> 4);
			int top = destination.getBottomY() + destination.getDimension().logicalHeight() - 8;
			spot = new Vec3d(x + 0.5, top, z + 0.5);
		}
		move(player, destination, spot, player.getYaw(), player.getPitch());
		player.setVelocity(Vec3d.ZERO);
		player.velocityModified = true;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 900, 0));
		player.sendMessage(Text.literal("You fell out of the Sky Realm.").formatted(Formatting.AQUA), true);
	}

	// ------------------------------------------------------------------ the Moon

	/** Moon Rocket: lift-off, and a soft landing on the Moon. */
	static boolean moon(ServerWorld world, ServerPlayerEntity player) {
		Vec3d start = player.getPos();
		boolean entered = enter(world, player, MOON, (moon, p) -> {
			BlockPos feet = moonLanding(moon, p.getBlockX(), p.getBlockZ());
			return Vec3d.ofBottomCenter(feet.up(12));
		});
		if (!entered) {
			return false;
		}
		Fx.particles(world, ParticleTypes.FLAME, start, 120, 0.6, 0.2);
		Fx.particles(world, ParticleTypes.CAMPFIRE_COSY_SMOKE, start, 60, 1.0, 0.05);
		Fx.sound(world, start, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 3.0F, 0.5F);
		if (player.getWorld().getRegistryKey() == MOON) {
			lowGravity(player, true);
			player.sendMessage(Text.literal("The Moon. You weigh a sixth. Use the rocket again to fly home.").formatted(Formatting.GRAY), true);
		}
		return true;
	}

	private static BlockPos moonLanding(ServerWorld moon, int x, int z) {
		ChunkPos middle = new ChunkPos(new BlockPos(x, 0, z));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				moon.getChunk(middle.x + dx, middle.z + dz);
				decorateMoon(moon, new ChunkPos(middle.x + dx, middle.z + dz));
			}
		}
		return landing(moon, x, z, MOON_SURFACE + 1, Blocks.LIGHT_GRAY_CONCRETE);
	}

	/** Craters of every size, some with a meteorite at the bottom. */
	static void decorateMoon(ServerWorld world, ChunkPos chunk) {
		BlockPos mark = new BlockPos(chunk.getStartX(), 0, chunk.getStartZ());
		if (!world.getBlockState(mark).isOf(Blocks.BEDROCK)) {
			return;
		}
		world.setBlockState(mark, DONE_MARK, Fx.QUIET);
		Random random = Random.create(world.getSeed() * 31L + chunk.toLong());
		int craters = random.nextFloat() < 0.5F ? 1 + (random.nextFloat() < 0.3F ? 1 : 0) : 0;
		BlockState dust = Blocks.LIGHT_GRAY_CONCRETE_POWDER.getDefaultState();
		for (int c = 0; c < craters; c++) {
			int radius = 2 + random.nextInt(6);
			int cx = chunk.getStartX() + radius + random.nextInt(16 - 2 * radius);
			int cz = chunk.getStartZ() + radius + random.nextInt(16 - 2 * radius);
			double depth = Math.max(1.0, radius * (0.4 + random.nextDouble() * 0.3));
			int bottom = MOON_SURFACE;
			for (int dx = -radius - 1; dx <= radius + 1; dx++) {
				for (int dz = -radius - 1; dz <= radius + 1; dz++) {
					int x = cx + dx;
					int z = cz + dz;
					if (x < chunk.getStartX() || x > chunk.getEndX() || z < chunk.getStartZ() || z > chunk.getEndZ()) {
						continue;
					}
					double d = Math.sqrt(dx * dx + dz * dz);
					int top = MOON_SURFACE + 4;
					while (top > 1 && world.getBlockState(new BlockPos(x, top, z)).isAir()) {
						top--;
					}
					if (d < radius) {
						int dig = (int) Math.round(depth * (1.0 - (d * d) / (radius * radius)));
						for (int y = top; y > top - dig && y > 1; y--) {
							world.setBlockState(new BlockPos(x, y, z), Fx.AIR, Fx.QUIET);
						}
						int floor = Math.max(1, top - dig);
						world.setBlockState(new BlockPos(x, floor, z), dust, Fx.QUIET);
						bottom = Math.min(bottom, floor);
					} else if (d < radius + 1.2 && random.nextFloat() < 0.7F) {
						world.setBlockState(new BlockPos(x, top + 1, z), dust, Fx.QUIET);
					}
				}
			}
			if (radius >= 4 && random.nextFloat() < 0.35F) {
				meteorite(world, new BlockPos(cx, bottom + 1, cz), random);
			}
		}
	}

	private static void meteorite(ServerWorld world, BlockPos at, Random random) {
		Block[] cores = {Blocks.RAW_IRON_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.ANCIENT_DEBRIS, Blocks.DIAMOND_ORE, Blocks.EMERALD_ORE,
				Blocks.CRYING_OBSIDIAN, Blocks.AMETHYST_BLOCK};
		for (BlockPos pos : BlockPos.iterate(at.add(-1, 0, -1), at.add(1, 1, 1))) {
			if (random.nextFloat() < 0.75F) {
				world.setBlockState(pos.toImmutable(), (random.nextBoolean() ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK).getDefaultState(), Fx.QUIET);
			}
		}
		world.setBlockState(at, cores[random.nextInt(cores.length)].getDefaultState(), Fx.QUIET);
	}

	/** On the Moon everything alive weighs a sixth and falls do not hurt. */
	public static boolean hasLowGravity(LivingEntity living) {
		RegistryEntry<EntityAttribute> gravity = Fx.attribute("generic.gravity");
		EntityAttributeInstance instance = gravity == null ? null : living.getAttributeInstance(gravity);
		return instance != null && instance.hasModifier(MOON_GRAVITY);
	}

	private static void lowGravity(LivingEntity living, boolean on) {
		modifier(living, "generic.gravity", on, -0.83, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		modifier(living, "generic.safe_fall_distance", on, 40.0, EntityAttributeModifier.Operation.ADD_VALUE);
	}

	private static void modifier(LivingEntity living, String id, boolean on, double value, EntityAttributeModifier.Operation operation) {
		RegistryEntry<EntityAttribute> attribute = Fx.attribute(id);
		EntityAttributeInstance instance = attribute == null ? null : living.getAttributeInstance(attribute);
		if (instance == null) {
			return;
		}
		if (on && !instance.hasModifier(MOON_GRAVITY)) {
			instance.addTemporaryModifier(new EntityAttributeModifier(MOON_GRAVITY, value, operation));
		} else if (!on && instance.hasModifier(MOON_GRAVITY)) {
			instance.removeModifier(MOON_GRAVITY);
		}
	}

	// ------------------------------------------------------------------ the Other Side

	/** Looking Glass: through to the Other Side, the same land as this world, untouched, at eternal dusk. */
	static boolean parallel(ServerWorld world, ServerPlayerEntity player) {
		boolean entered = enter(world, player, PARALLEL, (other, p) ->
				Vec3d.ofBottomCenter(landing(other, p.getBlockX(), p.getBlockZ(), p.getBlockY(), Blocks.GLASS)));
		if (entered && player.getWorld().getRegistryKey() == PARALLEL) {
			player.sendMessage(Text.literal("The Other Side. The same land, but nobody has ever been here.").formatted(Formatting.DARK_AQUA), true);
		}
		return entered;
	}

	// ------------------------------------------------------------------ every world

	/** Dimension Hopper: on to the next world, at the same spot on the map. */
	static boolean hop(ServerWorld world, ServerPlayerEntity player) {
		MinecraftServer server = world.getServer();
		List<ServerWorld> worlds = new ArrayList<>();
		for (ServerWorld each : server.getWorlds()) {
			worlds.add(each);
		}
		worlds.sort(Comparator.comparingInt(Worlds::order).thenComparing(w -> w.getRegistryKey().getValue().toString()));
		if (worlds.size() < 2) {
			return false;
		}
		ServerWorld next = worlds.get((worlds.indexOf(world) + 1) % worlds.size());
		RegistryKey<World> key = next.getRegistryKey();
		if (key == POCKET) {
			return pocket(world, player, false);
		}
		if (key == BACKROOMS) {
			return backrooms(world, player);
		}
		if (key == SKY_REALM) {
			return skyRealm(world, player);
		}
		if (key == MOON) {
			return moon(world, player);
		}
		if (key == PARALLEL) {
			return parallel(world, player);
		}
		double scale = world.getDimension().coordinateScale() / next.getDimension().coordinateScale();
		int x = MathHelper.floor(player.getX() * scale);
		int z = MathHelper.floor(player.getZ() * scale);
		Block platform = next.getRegistryKey() == World.NETHER ? Blocks.NETHERRACK : next.getRegistryKey() == World.END ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
		BlockPos feet = landing(next, x, z, MathHelper.clamp(player.getBlockY(), 32, 100), platform);
		departure(player);
		move(player, next, Vec3d.ofBottomCenter(feet), player.getYaw(), player.getPitch());
		arrival(next, player.getPos());
		player.sendMessage(Text.literal("Hop: " + key.getValue()).formatted(Formatting.LIGHT_PURPLE), true);
		return true;
	}

	private static int order(ServerWorld world) {
		RegistryKey<World> key = world.getRegistryKey();
		if (key == World.OVERWORLD) {
			return 0;
		}
		if (key == World.NETHER) {
			return 1;
		}
		if (key == World.END) {
			return 2;
		}
		return isModWorld(world) ? 3 + MOD_WORLDS.indexOf(key) : 10;
	}

	/** Banishing Wand: the mob you look at is sent to the Backrooms, far away. */
	static boolean banish(ServerWorld world, ServerPlayerEntity player, int range) {
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target == null || target instanceof PlayerEntity) {
			player.sendMessage(Text.literal("Look at a mob."), true);
			return false;
		}
		ServerWorld hall = world(world.getServer(), BACKROOMS, player);
		if (hall == null) {
			return false;
		}
		Random random = world.random;
		double angle = random.nextDouble() * Math.PI * 2.0;
		double distance = 500.0 + random.nextDouble() * 4500.0;
		int x = ((int) (target.getX() + Math.cos(angle) * distance) & ~15) + 4 + random.nextInt(8);
		int z = ((int) (target.getZ() + Math.sin(angle) * distance) & ~15) + 4 + random.nextInt(8);
		Vec3d spot = new Vec3d(x + 0.5, HALL_FLOOR + 1, z + 0.5);
		Vec3d from = target.getPos();
		Fx.particles(world, ParticleTypes.REVERSE_PORTAL, from.add(0.0, target.getHeight() / 2.0, 0.0), 80, 0.6, 0.4);
		Fx.particles(world, ParticleTypes.SMOKE, from.add(0.0, 0.2, 0.0), 30, 0.4, 0.02);
		Fx.sound(world, from, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);
		Entity moved = move(target, hall, spot, target.getYaw(), 0.0F);
		if (moved == null) {
			return false;
		}
		if (moved instanceof net.minecraft.entity.mob.MobEntity mob) {
			mob.setPersistent();
		}
		player.sendMessage(Text.literal("Banished to the Backrooms.").formatted(Formatting.YELLOW), true);
		return true;
	}

	// ------------------------------------------------------------------ every tick

	private static void tick(MinecraftServer server) {
		ticks++;
		try {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (!(player.getWorld() instanceof ServerWorld world) || !player.isAlive()) {
					continue;
				}
				RegistryKey<World> key = world.getRegistryKey();
				if (ticks % 10 == 0) {
					lowGravity(player, key == MOON);
				}
				if (!MOD_WORLDS.contains(key)) {
					continue;
				}
				if ((key == POCKET || key == BACKROOMS) && !player.isSpectator()
						&& world.getBlockState(BlockPos.ofFloored(player.getX(), player.getY() - 0.2, player.getZ())).isOf(Blocks.LODESTONE)) {
					goHome(player);
					continue;
				}
				if (key == POCKET && player.getY() < -8.0) {
					BlockPos room = pocketRoom(player.getUuid());
					buildRoom(world, room);
					move(player, world, Vec3d.ofBottomCenter(room.up()), player.getYaw(), player.getPitch());
					player.setVelocity(Vec3d.ZERO);
					player.velocityModified = true;
				} else if (key == SKY_REALM && player.getY() < -10.0) {
					fallOutOfTheSky(world, player);
				} else if (key == BACKROOMS) {
					tickBackrooms(world, player);
				} else if (key == MOON) {
					furnishAround(world, player, 5, false);
				}
			}
			if (ticks % 10 == 5) {
				ServerWorld moon = server.getWorld(MOON);
				if (moon != null) {
					for (Entity entity : moon.iterateEntities()) {
						if (entity instanceof LivingEntity living && !(entity instanceof PlayerEntity)) {
							lowGravity(living, true);
						}
					}
				}
			}
		} catch (Throwable t) {
			ItemCombinator.error("Worlds tick failed", t);
		}
	}

	/** Furnishes the loaded chunks around the player that are still bare, a few per tick. */
	private static void furnishAround(ServerWorld world, ServerPlayerEntity player, int radius, boolean backrooms) {
		if (ticks % 2 != 0) {
			return;
		}
		ChunkPos center = player.getChunkPos();
		int budget = 6;
		for (int r = 0; r <= radius && budget > 0; r++) {
			for (int dx = -r; dx <= r && budget > 0; dx++) {
				for (int dz = -r; dz <= r && budget > 0; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r || !world.isChunkLoaded(center.x + dx, center.z + dz)) {
						continue;
					}
					ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
					if (!world.getBlockState(new BlockPos(chunk.getStartX(), 0, chunk.getStartZ())).isOf(Blocks.BEDROCK)) {
						continue;
					}
					if (backrooms) {
						decorateBackrooms(world, chunk);
					} else {
						decorateMoon(world, chunk);
					}
					budget--;
				}
			}
		}
	}
}
