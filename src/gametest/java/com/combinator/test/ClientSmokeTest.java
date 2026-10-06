package com.combinator.test;

import com.combinator.ItemCombinator;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
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

	@Override
	public void onInitializeClient() {
		if (System.getProperty("combinator.clientTest") == null) {
			return;
		}
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
