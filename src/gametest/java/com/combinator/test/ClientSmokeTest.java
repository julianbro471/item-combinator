package com.combinator.test;

import com.combinator.ItemCombinator;
import com.combinator.ability.Charms;
import com.combinator.ability.Worlds;
import com.combinator.client.CombinerScreen;
import com.combinator.item.CWings;
import com.combinator.item.ComboItems;
import com.combinator.recipe.ComboRecipes;
import com.combinator.screen.CombinerScreenHandler;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the real game (with a window), makes a flat world, opens the Combiner Table, combines two items,
 * and takes screenshots of the table, of every item in the inventory, and of armor and wings on the player.
 * It also checks that every item has a name, a description, a model and a texture.
 *
 * It only runs when the game is started with -Dcombinator.clientTest (GitHub does that after every change).
 * The screenshots land in the "screenshots" folder, the verdict in "client-test-result.txt".
 */
public class ClientSmokeTest implements ClientModInitializer {
	private static final Logger LOG = LoggerFactory.getLogger("combinator-client-test");
	private final List<String> problems = new ArrayList<>();

	/**
	 * Pretend key presses. While "tick" is 0 or more, the plan is asked once per game tick which keys are down,
	 * and the highest point the player reaches is remembered. Counting game ticks (not seconds) keeps this exact
	 * even when the test computer is slow.
	 */
	private static volatile int flightTick = -1;
	private static volatile IntConsumer flightPlan;
	private static volatile double flightStartY;
	private static volatile double flightMaxRise;

	@Override
	public void onInitializeClient() {
		if (System.getProperty("combinator.clientTest") == null) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			int tick = flightTick;
			if (tick < 0 || client.player == null) {
				return;
			}
			flightPlan.accept(tick);
			flightMaxRise = Math.max(flightMaxRise, client.player.getY() - flightStartY);
			flightTick = tick + 1;
		});
		Thread thread = new Thread(() -> {
			try {
				this.run();
			} catch (Throwable t) {
				LOG.error("The client test crashed", t);
				this.problems.add("the client test stopped early: " + t);
			}
			this.writeResult();
			try {
				MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().scheduleStop());
				Thread.sleep(30000L);
			} catch (Throwable ignored) {
				// the game is already closing
			}
			Runtime.getRuntime().halt(this.problems.isEmpty() ? 0 : 1);
		}, "Combinator client test");
		thread.setDaemon(true);
		thread.start();
	}

	// ------------------------------------------------------------------ helpers

	private void problem(String text) {
		this.problems.add(text);
		LOG.error("PROBLEM: {}", text);
	}

	private void check(boolean ok, String textIfNot) {
		if (!ok) {
			this.problem(textIfNot);
		}
	}

	private void writeResult() {
		try {
			StringBuilder text = new StringBuilder();
			text.append(this.problems.isEmpty() ? "PASSED" : "FAILED with " + this.problems.size() + " problem(s)").append('\n');
			for (String problem : this.problems) {
				text.append("- ").append(problem).append('\n');
			}
			File file = new File(MinecraftClient.getInstance().runDirectory, "client-test-result.txt");
			Files.writeString(file.toPath(), text.toString(), StandardCharsets.UTF_8);
			LOG.info("Client test result: {}", text);
		} catch (Throwable t) {
			LOG.error("Could not write the result file", t);
		}
	}

	/** Runs something on the game's own thread and waits for it. */
	private static <T> T onClient(Function<MinecraftClient, T> action) {
		MinecraftClient client = MinecraftClient.getInstance();
		Supplier<T> supplier = () -> action.apply(client);
		return client.submit(supplier).join();
	}

	private static void onClientDo(Consumer<MinecraftClient> action) {
		onClient(client -> {
			action.accept(client);
			return Boolean.TRUE;
		});
	}

	/** Runs something on the thread of the world (the built-in server) and waits for it. */
	private static <T> T onServer(Function<ServerPlayerEntity, T> action) {
		MinecraftServer server = MinecraftClient.getInstance().getServer();
		if (server == null) {
			throw new IllegalStateException("no world is running");
		}
		Supplier<T> supplier = () -> action.apply(server.getPlayerManager().getPlayerList().get(0));
		return server.submit(supplier).join();
	}

	private static void onServerDo(Consumer<ServerPlayerEntity> action) {
		onServer(player -> {
			action.accept(player);
			return Boolean.TRUE;
		});
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void waitFor(String what, int seconds, Predicate<MinecraftClient> done) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		while (!onClient(done::test)) {
			if (System.currentTimeMillis() > end) {
				throw new RuntimeException("waited " + seconds + " seconds for: " + what);
			}
			sleep(250L);
		}
		LOG.info("Done waiting for: {}", what);
	}

	private static void screenshot(String name) {
		sleep(1500L); // let the picture settle
		onClientDo(client -> ScreenshotRecorder.saveScreenshot(client.runDirectory, name + ".png", client.getFramebuffer(), message -> { }));
		sleep(500L);
		LOG.info("Screenshot {}", name);
	}

	private static String name(Item item) {
		return Registries.ITEM.getId(item).getPath();
	}

	// ------------------------------------------------------------------ the test

	private void run() {
		waitFor("the game to finish loading", 600, client -> client.getOverlay() == null && client.currentScreen != null);
		onClientDo(client -> {
			client.options.pauseOnLostFocus = false;
			client.options.getViewDistance().setValue(4);
		});
		sleep(2000L);
		screenshot("0_title_screen");

		this.checkTexts();

		// A new flat world in creative mode.
		onClientDo(client -> client.createIntegratedServerLoader().createAndStart(
				"combinator_test",
				new LevelInfo("Combinator Test", GameMode.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), DataConfiguration.SAFE_MODE),
				new GeneratorOptions(0L, false, false),
				registries -> registries.get(RegistryKeys.WORLD_PRESET).entryOf(WorldPresets.FLAT).value().createDimensionsRegistryHolder(),
				client.currentScreen));
		waitFor("the world to open", 600, client -> client.world != null && client.player != null && client.currentScreen == null
				&& client.getOverlay() == null && client.getServer() != null);
		sleep(4000L);

		// Noon, clear sky, the player on a fixed spot, a Combiner Table two blocks ahead.
		BlockPos tablePos = onServer(player -> {
			MinecraftServer server = player.getServer();
			ServerWorld world = player.getServerWorld();
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), "gamerule doDaylightCycle false");
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), "gamerule doWeatherCycle false");
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), "time set noon");
			server.getCommandManager().executeWithPrefix(server.getCommandSource(), "weather clear");
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, 0, 0);
			player.teleport(world, 0.5, y, 0.5, 0.0F, 30.0F);
			BlockPos pos = new BlockPos(0, y, 2);
			world.setBlockState(pos, ItemCombinator.COMBINER_TABLE.getDefaultState());
			world.setBlockState(pos.add(-1, 0, 0), ItemCombinator.COMBINER_TABLE.getDefaultState());
			return pos;
		});
		sleep(3000L);
		screenshot("1_table_in_the_world");
		this.checkModels();

		// Right-click the table, like a player does.
		onClientDo(client -> client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
				new BlockHitResult(Vec3d.ofCenter(tablePos).add(0.0, 0.5, 0.0), Direction.UP, tablePos, false)));
		waitFor("the Combiner Table window", 30, client -> client.currentScreen instanceof CombinerScreen);
		screenshot("2_table_empty");

		// One item in: the recipe help appears.
		Item first = Items.DIAMOND_PICKAXE;
		List<ComboRecipes.Combo> partners = ComboRecipes.partners(first);
		this.check(!partners.isEmpty(), "the diamond pickaxe has no combinations");
		ComboRecipes.Combo combo = partners.isEmpty() ? ComboRecipes.ALL.get(0) : partners.get(0);
		Item a = partners.isEmpty() ? combo.a() : first;
		Item b = combo.partnerOf(a);
		onServerDo(player -> player.currentScreenHandler.getSlot(CombinerScreenHandler.SLOT_A).setStack(new ItemStack(a)));
		sleep(1000L);
		screenshot("3_table_recipe_help");

		// Second item in: the result appears.
		onServerDo(player -> player.currentScreenHandler.getSlot(CombinerScreenHandler.SLOT_B).setStack(new ItemStack(b)));
		sleep(1000L);
		boolean shown = onClient(client -> client.player.currentScreenHandler.getSlot(CombinerScreenHandler.SLOT_OUT).getStack().isOf(combo.result()));
		this.check(shown, "the game window does not show " + name(combo.result()) + " as the result of " + name(a) + " + " + name(b));
		screenshot("4_table_result");

		// Take the result with a click and put it into the inventory with a second click.
		onClientDo(client -> client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,
				CombinerScreenHandler.SLOT_OUT, 0, SlotActionType.PICKUP, client.player));
		sleep(500L);
		onClientDo(client -> client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId,
				3, 0, SlotActionType.PICKUP, client.player));
		sleep(1000L);
		int made = onServer(player -> player.getInventory().count(combo.result()));
		boolean inputsGone = onServer(player -> player.currentScreenHandler.getSlot(CombinerScreenHandler.SLOT_A).getStack().isEmpty()
				&& player.currentScreenHandler.getSlot(CombinerScreenHandler.SLOT_B).getStack().isEmpty());
		this.check(made == combo.count(), "after clicking the result the player has " + made + "x " + name(combo.result()));
		this.check(inputsGone, "the two inputs were not used up");
		screenshot("5_table_result_taken");
		onClientDo(client -> client.player.closeHandledScreen());
		sleep(500L);

		// Every item, 36 at a time, in the normal inventory window.
		onServerDo(player -> player.changeGameMode(GameMode.SURVIVAL));
		sleep(500L);
		List<Item> all = ComboItems.ALL;
		int perPage = 35;
		int pages = (all.size() + perPage - 1) / perPage;
		for (int page = 0; page < pages; page++) {
			int from = page * perPage;
			onServerDo(player -> {
				player.getInventory().clear();
				// Slots 9-35 are the three upper rows, slots 0-8 the hotbar. Filling in this order keeps the items in reading order.
				int next = from;
				for (int i = 0; i < 36 && next < all.size(); i++) {
					int slot = i < 27 ? 9 + i : i - 27;
					if (slot == 13) {
						continue; // the mouse pointer rests on this slot; the tooltip of an item there would hide the others
					}
					player.getInventory().setStack(slot, new ItemStack(all.get(next++)));
				}
			});
			sleep(1000L);
			onClientDo(client -> client.setScreen(new InventoryScreen(client.player)));
			waitFor("the inventory window", 30, client -> client.currentScreen instanceof InventoryScreen);
			screenshot("6_items_page_" + (page + 1));
			onClientDo(client -> client.setScreen(null));
			sleep(300L);
		}

		this.movementTricks();

		// Armor and a weapon, seen from the front.
		onServerDo(player -> {
			player.getInventory().clear();
			boolean sword = false;
			for (Item item : all) {
				if (item instanceof ArmorItem armor && !(item instanceof CWings)) {
					EquipmentSlot slot = armor.getType().getEquipmentSlot();
					if (player.getEquippedStack(slot).isEmpty()) {
						player.equipStack(slot, new ItemStack(item));
					}
				} else if (item instanceof SwordItem && !sword) {
					player.getInventory().selectedSlot = 0;
					player.getInventory().setStack(0, new ItemStack(item));
					sword = true;
				}
			}
			player.teleport(player.getServerWorld(), 0.5, player.getY(), -3.5, 0.0F, 0.0F);
		});
		sleep(1000L);
		onClientDo(client -> client.options.setPerspective(Perspective.THIRD_PERSON_FRONT));
		screenshot("7_armor_from_the_front");

		// Wings, seen from behind.
		onServerDo(player -> {
			for (Item item : all) {
				if (item instanceof CWings) {
					player.equipStack(EquipmentSlot.CHEST, new ItemStack(item));
					break;
				}
			}
		});
		sleep(1000L);
		onClientDo(client -> client.options.setPerspective(Perspective.THIRD_PERSON_BACK));
		screenshot("8_wings_from_behind");

		// Items in both hands, first person.
		onServerDo(player -> {
			player.getInventory().clear();
			player.getInventory().selectedSlot = 0;
			player.getInventory().setStack(0, new ItemStack(ComboItems.EXCAVATOR));
			player.setStackInHand(Hand.OFF_HAND, new ItemStack(ComboItems.CHAOS_ORB));
		});
		sleep(1000L);
		onClientDo(client -> client.options.setPerspective(Perspective.FIRST_PERSON));
		screenshot("9_items_in_hand");

		this.otherWorlds();

		// The creative inventory tab of the mod lists the table and every item.
		int inTab = onClient(client -> {
			ItemGroup group = Registries.ITEM_GROUP.get(ItemCombinator.id("main"));
			if (group == null) {
				return -1;
			}
			ItemGroups.updateDisplayContext(client.player.networkHandler.getEnabledFeatures(), true, client.world.getRegistryManager());
			return group.getDisplayStacks().size();
		});
		this.check(inTab == all.size() + 1, "the creative tab shows " + inTab + " items instead of " + (all.size() + 1));

		this.check(ItemCombinator.ERRORS.isEmpty(), "the mod reported errors while the game was running: " + ItemCombinator.ERRORS);
	}

	private static int count(ServerWorld world, BlockPos from, BlockPos to, Block block) {
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(from, to)) {
			if (world.getBlockState(pos).isOf(block)) {
				count++;
			}
		}
		return count;
	}

	/** Right-clicks with the item, on the game's server side, the way a real right-click does. Runs on the server thread. */
	private static ActionResult useNow(ServerPlayerEntity player, Item item) {
		ItemStack stack = new ItemStack(item);
		player.getInventory().selectedSlot = 0;
		player.getInventory().setStack(0, stack);
		player.getItemCooldownManager().remove(item);
		return player.interactionManager.interactItem(player, player.getServerWorld(), stack, Hand.MAIN_HAND);
	}

	private static String use(Item item) {
		return onServer(player -> useNow(player, item).toString());
	}

	/** Waits until the game shows the given world, and a few seconds more so the land around is drawn. */
	private static void arriveIn(RegistryKey<World> key) {
		waitFor("arriving in " + key.getValue(), 90, client -> client.world != null && client.player != null
				&& client.world.getRegistryKey() == key && client.currentScreen == null);
		sleep(4000L);
	}

	private static String where() {
		return onServer(player -> player.getWorld().getRegistryKey().getValue() + " " + player.getBlockPos().toShortString());
	}

	/**
	 * The five worlds of the mod: the Pocket Dimension, the Backrooms, the Sky Realm, the Moon and the Other Side.
	 * The game's test server only makes the Overworld, the Nether and the End, so they are tested here, in a normal world.
	 */
	private void otherWorlds() {
		onServerDo(player -> {
			player.getInventory().clear();
			player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
			player.changeGameMode(GameMode.SURVIVAL);
			player.setInvulnerable(true);
		});
		List<RegistryKey<World>> keys = List.of(Worlds.POCKET, Worlds.BACKROOMS, Worlds.SKY_REALM, Worlds.MOON, Worlds.PARALLEL);
		List<String> missing = onServer(player -> {
			List<String> list = new ArrayList<>();
			for (RegistryKey<World> key : keys) {
				if (player.getServer().getWorld(key) == null) {
					list.add(key.getValue().toString());
				}
			}
			return list;
		});
		this.check(missing.isEmpty(), "a new world does not have the worlds of the mod: " + missing);
		if (!missing.isEmpty()) {
			return;
		}

		// ---- the Pocket Dimension: in, look at the workshop wall, out over the lodestone
		BlockPos home = onServer(player -> player.getBlockPos());
		use(ComboItems.POCKET_CUBE);
		arriveIn(Worlds.POCKET);
		String pocket = onServer(player -> {
			ServerWorld world = player.getServerWorld();
			BlockPos room = Worlds.pocketRoom(player.getUuid());
			int walls = count(world, room.add(-8, 0, -8), room.add(8, 9, 8), Blocks.QUARTZ_BRICKS);
			int tables = count(world, room.add(-8, 0, -8), room.add(8, 2, 8), Blocks.CRAFTING_TABLE);
			player.teleport(world, room.getX() + 0.5, room.getY() + 1.0, room.getZ() + 4.5, 180.0F, 10.0F);
			return walls > 200 && tables == 1 ? null : "the pocket room has " + walls + " wall blocks and " + tables + " crafting tables";
		});
		this.check(pocket == null, String.valueOf(pocket));
		sleep(1500L);
		screenshot("10_world_pocket_dimension");
		onServerDo(player -> {
			BlockPos room = Worlds.pocketRoom(player.getUuid());
			player.teleport(player.getServerWorld(), room.getX() + 6.5, room.getY() + 1.0, room.getZ() + 6.5, 0.0F, 0.0F);
		});
		arriveIn(World.OVERWORLD);
		this.check(onServer(player -> player.getBlockPos().getSquaredDistance(home) < 9.0), "the lodestone in the pocket room did not lead home: " + where());

		// ---- the Backrooms: walls, lamps and a Smiler
		use(ComboItems.NOCLIP_PEARL);
		arriveIn(Worlds.BACKROOMS);
		String hall = onServer(player -> {
			ServerWorld world = player.getServerWorld();
			BlockPos feet = player.getBlockPos();
			int walls = count(world, new BlockPos(feet.getX() - 20, 4, feet.getZ() - 20), new BlockPos(feet.getX() + 20, 7, feet.getZ() + 20), Blocks.SMOOTH_SANDSTONE);
			int lamps = count(world, new BlockPos(feet.getX() - 20, 8, feet.getZ() - 20), new BlockPos(feet.getX() + 20, 8, feet.getZ() + 20), Blocks.REDSTONE_LAMP);
			EndermanEntity smiler = Worlds.spawnSmiler(world, player);
			boolean invisible = smiler != null && smiler.hasStatusEffect(StatusEffects.INVISIBILITY);
			if (smiler != null) {
				smiler.discard();
			}
			return feet.getY() == 4 && walls > 40 && lamps > 10 && invisible ? null
					: "the Backrooms are wrong: standing at " + feet.toShortString() + ", " + walls + " wall blocks, " + lamps + " lamps, a Smiler: " + invisible;
		});
		this.check(hall == null, String.valueOf(hall));
		onServerDo(player -> {
			// look along the longest free way, so the picture shows the rooms and not a wall
			ServerWorld world = player.getServerWorld();
			float best = 0.0F;
			int longest = -1;
			for (Direction direction : Direction.Type.HORIZONTAL) {
				int free = 0;
				while (free < 24 && world.getBlockState(player.getBlockPos().up().offset(direction, free + 1)).isAir()) {
					free++;
				}
				if (free > longest) {
					longest = free;
					best = direction.asRotation();
				}
			}
			player.teleport(world, player.getX(), player.getY(), player.getZ(), best, 5.0F);
		});
		sleep(1500L);
		screenshot("11_world_backrooms");
		use(ComboItems.NOCLIP_PEARL);
		arriveIn(World.OVERWORLD);

		// ---- the Sky Realm: an island to stand on, and falling off it
		use(ComboItems.CLOUD_KEY);
		arriveIn(Worlds.SKY_REALM);
		this.check(onServer(player -> !player.getServerWorld().getBlockState(player.getBlockPos().down()).isAir()),
				"in the Sky Realm the player stands on nothing: " + where());
		screenshot("12_world_sky_realm");
		onServerDo(player -> player.teleport(player.getServerWorld(), player.getX(), -20.0, player.getZ(), player.getYaw(), 0.0F));
		arriveIn(World.OVERWORLD);
		this.check(onServer(player -> player.hasStatusEffect(StatusEffects.SLOW_FALLING) && player.getY() > 200.0),
				"falling off the Sky Realm did not drop the player high into the Overworld with Slow Falling: " + where());
		onServerDo(player -> {
			player.clearStatusEffects();
			ServerWorld world = player.getServerWorld();
			player.teleport(world, home.getX() + 0.5, world.getTopY(Heightmap.Type.MOTION_BLOCKING, home.getX(), home.getZ()), home.getZ() + 0.5, 0.0F, 0.0F);
		});
		sleep(1500L);

		// ---- the Moon: a soft landing, then a jump of several blocks
		use(ComboItems.MOON_ROCKET);
		arriveIn(Worlds.MOON);
		this.check(onServer(Worlds::hasLowGravity), "the player is not lighter on the Moon");
		waitFor("landing on the Moon", 60, client -> client.player.isOnGround());
		sleep(500L);
		screenshot("13_world_moon");
		double moonJump = fly(100, tick -> MinecraftClient.getInstance().options.jumpKey.setPressed(tick < 2));
		LOG.info("Movement: a jump on the Moon went {} blocks up", moonJump);
		this.check(moonJump > 3.0, "a jump on the Moon went only " + moonJump + " blocks up");
		use(ComboItems.MOON_ROCKET);
		arriveIn(World.OVERWORLD);
		this.check(!onServer(Worlds::hasLowGravity), "the player is still light after coming home from the Moon");

		// ---- the Other Side
		use(ComboItems.LOOKING_GLASS);
		arriveIn(Worlds.PARALLEL);
		this.check(onServer(player -> !player.getServerWorld().getBlockState(player.getBlockPos().down()).isAir()
				&& player.getServerWorld().getFluidState(player.getBlockPos()).isEmpty()), "on the Other Side the player stands badly: " + where());
		screenshot("14_world_other_side");
		use(ComboItems.LOOKING_GLASS);
		arriveIn(World.OVERWORLD);

		// ---- the Banishing Wand sends a pig to the Backrooms
		String banish = onServer(player -> {
			ServerWorld world = player.getServerWorld();
			player.teleport(world, player.getX(), player.getY(), player.getZ(), 0.0F, 20.0F);
			// The flat world has its own animals. One standing in the line of sight would be banished instead of the pig.
			for (Entity other : world.getOtherEntities(player, player.getBoundingBox().expand(12.0), e -> e instanceof LivingEntity)) {
				other.discard();
			}
			PigEntity pig = EntityType.PIG.create(world);
			if (pig == null) {
				return "no pig";
			}
			pig.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ() + 3.0, 180.0F, 0.0F);
			pig.setAiDisabled(true);
			boolean spawned = world.spawnEntity(pig);
			ActionResult result = useNow(player, ComboItems.BANISHING_WAND);
			int inBackrooms = 0;
			for (Entity entity : player.getServer().getWorld(Worlds.BACKROOMS).iterateEntities()) {
				inBackrooms += entity instanceof PigEntity ? 1 : 0;
			}
			LOG.info("Banishing: {} pigs are now loaded in the Backrooms", inBackrooms);
			return pig.isRemoved() ? null : "the Banishing Wand did not banish the pig: right-click " + result + ", pig spawned " + spawned
					+ " at " + pig.getBlockPos().toShortString() + ", player at " + player.getBlockPos().toShortString() + " looking " + player.getYaw()
					+ " / " + player.getPitch() + ", pigs in the Backrooms: " + inBackrooms;
		});
		this.check(banish == null, String.valueOf(banish));

		// ---- the Dimension Hopper: through all eight worlds and home again
		String hops = onServer(player -> {
			List<String> seen = new ArrayList<>();
			int worlds = 0;
			for (ServerWorld ignored : player.getServer().getWorlds()) {
				worlds++;
			}
			for (int hop = 0; hop < worlds; hop++) {
				useNow(player, ComboItems.DIMENSION_HOPPER);
				seen.add(player.getWorld().getRegistryKey().getValue().toString());
			}
			LOG.info("The Dimension Hopper went through {}", seen);
			return seen.size() == worlds && seen.stream().distinct().count() == worlds && player.getWorld().getRegistryKey() == World.OVERWORLD
					? null : "the Dimension Hopper went through " + seen;
		});
		this.check(hops == null, String.valueOf(hops));
		arriveIn(World.OVERWORLD);

		onServerDo(player -> {
			player.getInventory().clear();
			player.setInvulnerable(false);
		});
	}

	/** Runs the key plan for the given number of game ticks and returns how high the player got above the start. */
	private static double fly(int ticks, IntConsumer plan) {
		onClientDo(client -> {
			flightStartY = client.player.getY();
			flightMaxRise = 0.0;
			flightPlan = plan;
			flightTick = 0;
		});
		long end = System.currentTimeMillis() + 60000L;
		while (flightTick < ticks && System.currentTimeMillis() < end) {
			sleep(50L);
		}
		boolean finished = flightTick >= ticks;
		onClientDo(client -> {
			flightTick = -1;
			client.options.jumpKey.setPressed(false);
			client.options.forwardKey.setPressed(false);
		});
		if (!finished) {
			throw new RuntimeException("the game did not run " + ticks + " ticks in a minute");
		}
		return flightMaxRise;
	}

	/** Puts the player on a free, flat spot, looking along +Z, with exactly these items. */
	private static void standReady(ItemStack mainHand, ItemStack boots) {
		onServerDo(player -> {
			ServerWorld world = player.getServerWorld();
			player.getInventory().clear();
			player.getInventory().selectedSlot = 0;
			player.getInventory().setStack(0, mainHand);
			player.equipStack(EquipmentSlot.FEET, boots);
			player.setHealth(player.getMaxHealth());
			int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, 0, -8);
			player.teleport(world, 0.5, y, -7.5, 0.0F, 0.0F);
		});
		sleep(1500L); // the items and the new place have to reach the game client
	}

	/**
	 * Double jump, pogo stick and wall climbing. The player's own computer works these out, so the server tests
	 * cannot see them. Here the real game gets pretend key presses and the test measures how high the player gets.
	 */
	private void movementTricks() {
		// First without any item. A normal jump is 1.25 blocks high. If this fails, the pretend keys do not work.
		standReady(ItemStack.EMPTY, ItemStack.EMPTY);
		double plain = fly(30, tick -> MinecraftClient.getInstance().options.jumpKey.setPressed(tick < 2));
		LOG.info("Movement: a normal jump went {} blocks up", plain);
		this.check(plain > 1.0 && plain < 1.5, "test setup: a normal jump went " + plain + " blocks up, expected about 1.25");

		// Double jump: jump, let go, and press jump again at the top.
		standReady(new ItemStack(ComboItems.DUST_DEVIL), ItemStack.EMPTY);
		int seenBefore = Charms.tricksSeen;
		double doubled = fly(45, tick -> MinecraftClient.getInstance().options.jumpKey.setPressed(tick < 2 || (tick >= 7 && tick < 9)));
		sleep(500L);
		LOG.info("Movement: a double jump went {} blocks up", doubled);
		this.check(doubled > 2.0, "the Bottled Dust Devil gave no second jump: the player got " + doubled + " blocks up (a normal jump is 1.25)");
		this.check(Charms.tricksSeen > seenBefore, "the server was not told about the double jump");

		// Pogo stick: one jump, then it bounces by itself, higher each time.
		standReady(new ItemStack(ComboItems.POGO_STICK), ItemStack.EMPTY);
		double bounced = fly(90, tick -> MinecraftClient.getInstance().options.jumpKey.setPressed(tick < 2));
		LOG.info("Movement: the pogo stick went {} blocks up", bounced);
		this.check(bounced > 1.8, "the Pogo Stick did not bounce higher than a normal jump: " + bounced + " blocks");
		float healthAfterPogo = onServer(player -> player.getHealth() / player.getMaxHealth());
		this.check(healthAfterPogo >= 1.0F, "bouncing on the Pogo Stick hurt the player");

		// Wall climbing: a stone wall right in front of the player, and the forward key held down.
		standReady(ItemStack.EMPTY, new ItemStack(ComboItems.STICKY_BOOTS));
		onServerDo(player -> {
			ServerWorld world = player.getServerWorld();
			BlockPos feet = player.getBlockPos();
			for (BlockPos pos : BlockPos.iterate(feet.add(-1, 0, 1), feet.add(1, 8, 1))) {
				world.setBlockState(pos.toImmutable(), Blocks.STONE.getDefaultState());
			}
		});
		sleep(1000L);
		int climbSeenBefore = Charms.tricksSeen;
		double climbed = fly(35, tick -> MinecraftClient.getInstance().options.forwardKey.setPressed(true));
		LOG.info("Movement: the sticky boots went {} blocks up the wall", climbed);
		this.check(climbed > 2.5, "the Sticky Boots did not climb the wall: the player got " + climbed + " blocks up");
		this.check(Charms.tricksSeen > climbSeenBefore, "the server was not told about the climbing");
		onServerDo(player -> {
			ServerWorld world = player.getServerWorld();
			BlockPos base = new BlockPos(0, world.getTopY(Heightmap.Type.MOTION_BLOCKING, 0, -9), -7);
			for (BlockPos pos : BlockPos.iterate(base.add(-1, -1, 0), base.add(1, 12, 2))) {
				if (world.getBlockState(pos).isOf(Blocks.STONE) && pos.getY() >= base.getY()) {
					world.setBlockState(pos.toImmutable(), Blocks.AIR.getDefaultState());
				}
			}
			player.getInventory().clear();
			player.teleport(world, 0.5, base.getY(), -7.5, 0.0F, 0.0F);
			player.setHealth(player.getMaxHealth());
		});
		sleep(1000L);
	}

	/** Every item needs a name and its description lines in the language file. */
	private void checkTexts() {
		List<String> missing = onClient(client -> {
			List<String> list = new ArrayList<>();
			for (Item item : ComboItems.ALL) {
				String key = item.getTranslationKey();
				if (!I18n.hasTranslation(key)) {
					list.add(key);
				}
				for (int i = 1; i <= ComboItems.tipLines(item); i++) {
					if (!I18n.hasTranslation(key + ".tip" + i)) {
						list.add(key + ".tip" + i);
					}
				}
			}
			for (String key : new String[] {
					ItemCombinator.COMBINER_TABLE.getTranslationKey(), "itemGroup.combinator.main", "gui.combinator.hints", "gui.combinator.hint_none"}) {
				if (!I18n.hasTranslation(key)) {
					list.add(key);
				}
			}
			return list;
		});
		this.check(missing.isEmpty(), missing.size() + " texts are missing in the language file: " + missing);
	}

	/** Every item needs a model with a real texture, and a tooltip that can be built without an error. */
	private void checkModels() {
		List<String> bad = onClient(client -> {
			List<String> list = new ArrayList<>();
			BakedModel missingModel = client.getBakedModelManager().getMissingModel();
			for (Item item : ComboItems.ALL) {
				String id = name(item);
				try {
					ItemStack stack = new ItemStack(item);
					BakedModel model = client.getItemRenderer().getModel(stack, client.world, client.player, 0);
					if (model == missingModel) {
						list.add(id + ": no model");
					} else if (model.getParticleSprite().getContents().getId().getPath().contains("missingno")) {
						list.add(id + ": no texture");
					}
					List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC);
					if (tooltip.size() < 1 + ComboItems.tipLines(item)) {
						list.add(id + ": the tooltip has " + tooltip.size() + " lines, expected at least " + (1 + ComboItems.tipLines(item)));
					}
				} catch (Throwable t) {
					list.add(id + ": " + t);
				}
			}
			BakedModel table = client.getBlockRenderManager().getModel(ItemCombinator.COMBINER_TABLE.getDefaultState());
			if (table == missingModel) {
				list.add("combiner_table block: no model");
			} else if (table.getParticleSprite().getContents().getId().getPath().contains("missingno")) {
				list.add("combiner_table block: no texture");
			}
			BakedModel tableItem = client.getItemRenderer().getModel(new ItemStack(ItemCombinator.COMBINER_TABLE_ITEM), client.world, client.player, 0);
			if (tableItem == missingModel) {
				list.add("combiner_table item: no model");
			}
			if (client.getResourceManager().getResource(Identifier.of("combinator", "textures/gui/combiner.png")).isEmpty()) {
				list.add("the picture of the table window (textures/gui/combiner.png) is missing");
			}
			return list;
		});
		this.check(bad.isEmpty(), bad.size() + " items have a broken model, texture or tooltip: " + bad);
	}
}
