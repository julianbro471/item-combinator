package com.combinator.ability;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Creation tools: real structures of the game, biome painting, a building wand and an express railway. */
public final class Builders {
	private static final String BLUEPRINT_KEY = "combinator_blueprint";
	private static final String BIOME_KEY = "combinator_biome";

	/** What the Architect's Blueprint can build. "random" picks one of the others. */
	public static final String[] STRUCTURES = {"random", "village_plains", "village_desert", "village_savanna", "village_snowy",
			"village_taiga", "pillager_outpost", "desert_pyramid", "jungle_pyramid", "igloo", "swamp_hut", "ruined_portal", "shipwreck",
			"trail_ruins", "woodland_mansion", "ancient_city", "trial_chambers", "stronghold", "monument", "fortress",
			"bastion_remnant", "end_city"};
	/** What the Biome Brush can paint. */
	public static final String[] BIOMES = {"cherry_grove", "mushroom_fields", "desert", "snowy_plains", "jungle", "badlands",
			"dark_forest", "swamp", "ice_spikes", "flower_forest", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas",
			"the_end", "deep_dark", "plains"};

	private static String forcedStructure = null;

	private Builders() {
	}

	/** The tests use this so they know which structure comes next. */
	public static void forceNextStructure(String id) {
		forcedStructure = id;
	}

	private static int choice(ItemStack stack, String key) {
		NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
		return data.contains(key) ? data.copyNbt().getInt(key) : 0;
	}

	private static String pretty(String id) {
		String text = id.replace('_', ' ');
		return Character.toUpperCase(text.charAt(0)) + text.substring(1);
	}

	/** Sneaking with the Blueprint or the Brush: the next choice. */
	static boolean pick(ServerPlayerEntity player, ItemStack stack, boolean biome) {
		String key = biome ? BIOME_KEY : BLUEPRINT_KEY;
		String[] list = biome ? BIOMES : STRUCTURES;
		int next = (choice(stack, key) + 1) % list.length;
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(key, next));
		player.sendMessage(Text.literal((biome ? "Biome: " : "Blueprint: ") + pretty(list[next])).formatted(Formatting.AQUA), true);
		Fx.sound(player.getWorld(), player.getPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
		return true;
	}

	/** Runs a command of the game as an operator would, but without any chat output. Throws if the command fails. */
	private static int command(ServerPlayerEntity player, String command) throws CommandSyntaxException {
		ServerCommandSource source = player.getCommandSource().withLevel(2).withSilent();
		return player.getServer().getCommandManager().getDispatcher().execute(command, source);
	}

	// ------------------------------------------------------------------ Architect's Blueprint

	/** Builds a real structure of the game, as the game itself would, where the player looks. */
	static boolean blueprint(ServerWorld world, ServerPlayerEntity player, ItemStack stack, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.literal("Aim at the ground."), true);
			return false;
		}
		String id = forcedStructure != null ? forcedStructure : STRUCTURES[choice(stack, BLUEPRINT_KEY) % STRUCTURES.length];
		forcedStructure = null;
		if (id.equals("random")) {
			id = STRUCTURES[1 + world.random.nextInt(STRUCTURES.length - 1)];
		}
		BlockPos at = hit.getBlockPos().up();
		try {
			command(player, "place structure minecraft:" + id + " " + at.getX() + " " + at.getY() + " " + at.getZ());
		} catch (CommandSyntaxException e) {
			player.sendMessage(Text.literal("The " + pretty(id) + " does not fit here: " + e.getRawMessage().getString()).formatted(Formatting.RED), true);
			return false;
		}
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(at), 80, 4.0, 0.1);
		Fx.sound(world, Vec3d.ofCenter(at), SoundEvents.BLOCK_ANVIL_USE, 1.0F, 0.8F);
		player.sendMessage(Text.literal("Built: " + pretty(id)).formatted(Formatting.GREEN), true);
		return true;
	}

	// ------------------------------------------------------------------ Biome Brush

	/** Paints the chosen biome on everything within 12 blocks of the spot the player looks at, from the bottom of the world to the top. */
	static boolean paintBiome(ServerWorld world, ServerPlayerEntity player, ItemStack stack, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		BlockPos center = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : player.getBlockPos();
		String biome = BIOMES[choice(stack, BIOME_KEY) % BIOMES.length];
		try {
			paint(player, center, 12, biome);
		} catch (CommandSyntaxException e) {
			player.sendMessage(Text.literal("The paint does not stick: " + e.getRawMessage().getString()).formatted(Formatting.RED), true);
			return false;
		}
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(center.up()), 120, 6.0, 0.1);
		Fx.particles(world, ParticleTypes.COMPOSTER, Vec3d.ofCenter(center.up()), 120, 6.0, 0.1);
		Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.ITEM_BONE_MEAL_USE, 1.0F, 0.7F);
		player.sendMessage(Text.literal("Painted: " + pretty(biome)).formatted(Formatting.GREEN), true);
		return true;
	}

	/**
	 * Paints a biome in a square around the centre, over the whole height of the world. The game's own /fillbiome does the work,
	 * in slices, because one call may change at most 32768 blocks. The tests also use this to paint the stage back.
	 */
	public static void paint(ServerPlayerEntity player, BlockPos center, int radius, String biome) throws CommandSyntaxException {
		World world = player.getWorld();
		// Biomes come in cubes of 4 blocks. Corners on that grid keep the counted size exact.
		int x1 = (center.getX() - radius) & ~3;
		int z1 = (center.getZ() - radius) & ~3;
		int x2 = x1 + 2 * radius;
		int z2 = z1 + 2 * radius;
		for (int y = world.getBottomY(); y < world.getTopY(); y += 48) {
			int y2 = Math.min(world.getTopY() - 1, y + 47);
			command(player, "fillbiome " + x1 + " " + y + " " + z1 + " " + x2 + " " + y2 + " " + z2 + " minecraft:" + biome);
		}
	}

	// ------------------------------------------------------------------ Builder's Wand

	/**
	 * Right-click a block: every connected block of the same kind on that face grows one block outwards. Up to 64 at once, using
	 * the blocks from the player's inventory (none in creative mode).
	 */
	public static ActionResult buildFace(ItemUsageContext context, PlayerEntity player, ItemStack wand) {
		World world = context.getWorld();
		BlockPos origin = context.getBlockPos();
		Direction side = context.getSide();
		BlockState state = world.getBlockState(origin);
		Item item = state.getBlock().asItem();
		if (item == Items.AIR || state.hasBlockEntity()) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		int available = player.isCreative() ? 64 : Math.min(64, player.getInventory().count(item));
		if (available == 0) {
			player.sendMessage(Text.literal("You have no " + item.getName().getString() + " to build with."), true);
			return ActionResult.FAIL;
		}
		List<BlockPos> targets = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && targets.size() < available) {
			BlockPos pos = queue.poll();
			if (!world.getBlockState(pos).equals(state)) {
				continue;
			}
			BlockPos front = pos.offset(side);
			if (!Fx.canFill(world.getBlockState(front)) || !world.getFluidState(front).isEmpty()
					|| !world.getOtherEntities(null, new net.minecraft.util.math.Box(front), e -> e instanceof LivingEntity).isEmpty()) {
				continue;
			}
			targets.add(front);
			for (Direction direction : Direction.values()) {
				if (direction.getAxis() == side.getAxis()) {
					continue;
				}
				BlockPos next = pos.offset(direction);
				if (next.getManhattanDistance(origin) <= 16 && seen.add(next)) {
					queue.add(next);
				}
			}
		}
		if (targets.isEmpty()) {
			return ActionResult.FAIL;
		}
		for (BlockPos pos : targets) {
			world.setBlockState(pos, state, Block.NOTIFY_ALL);
		}
		if (!player.isCreative()) {
			take(player.getInventory(), item, targets.size());
		}
		wand.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
		Fx.sound(world, Vec3d.ofCenter(origin), state.getSoundGroup().getPlaceSound(), 1.0F, 0.8F);
		if (world instanceof ServerWorld server) {
			for (BlockPos pos : targets) {
				server.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
			}
		}
		return ActionResult.SUCCESS;
	}

	private static void take(PlayerInventory inventory, Item item, int amount) {
		for (int i = 0; i < inventory.size() && amount > 0; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(item)) {
				int used = Math.min(amount, stack.getCount());
				stack.decrement(used);
				amount -= used;
			}
		}
	}

	// ------------------------------------------------------------------ Express Rail Wand

	/**
	 * A powered railway in the direction the player faces, level and straight: tunnels through hills, a track bed over valleys.
	 * A redstone block under every 8th rail keeps all of them powered. Then the player gets into a minecart on it.
	 */
	static boolean railLine(ServerWorld world, ServerPlayerEntity player, int length) {
		Direction direction = player.getHorizontalFacing();
		BlockPos start = player.getBlockPos().offset(direction);
		BlockState rail = Blocks.POWERED_RAIL.getDefaultState()
				.with(PoweredRailBlock.SHAPE, direction.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
		int laid = 0;
		for (int i = 0; i < length; i++) {
			BlockPos pos = start.offset(direction, i);
			if (!world.isInBuildLimit(pos.down()) || !world.isInBuildLimit(pos.up(2))
					|| unbreakable(world, pos) || unbreakable(world, pos.up()) || unbreakable(world, pos.up(2)) || unbreakable(world, pos.down())) {
				break;
			}
			world.setBlockState(pos.down(), (i % 8 == 0 ? Blocks.REDSTONE_BLOCK : Blocks.STONE_BRICKS).getDefaultState(), Block.NOTIFY_ALL);
			world.setBlockState(pos.up(), Fx.AIR, Block.NOTIFY_ALL);
			world.setBlockState(pos.up(2), Fx.AIR, Block.NOTIFY_ALL);
			world.setBlockState(pos, rail, Block.NOTIFY_ALL);
			laid++;
		}
		if (laid == 0) {
			player.sendMessage(Text.literal("No room for a railway here."), true);
			return false;
		}
		BlockPos end = start.offset(direction, laid);
		if (world.isInBuildLimit(end) && !unbreakable(world, end)) {
			world.setBlockState(end, Blocks.STONE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
		}
		MinecartEntity cart = EntityType.MINECART.create(world);
		if (cart != null) {
			cart.refreshPositionAndAngles(start.getX() + 0.5, start.getY() + 0.1, start.getZ() + 0.5, direction.asRotation(), 0.0F);
			world.spawnEntity(cart);
			player.startRiding(cart, true);
			cart.setVelocity(Vec3d.of(direction.getVector()).multiply(0.6));
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_MINECART_RIDING, 1.0F, 1.2F);
		player.sendMessage(Text.literal(laid + " blocks of railway. All aboard!").formatted(Formatting.GREEN), true);
		return true;
	}

	private static boolean unbreakable(ServerWorld world, BlockPos pos) {
		return world.getBlockState(pos).getHardness(world, pos) < 0.0F;
	}
}
