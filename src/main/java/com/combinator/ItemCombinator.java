package com.combinator;

import com.combinator.ability.Capture;
import com.combinator.ability.CombatAbilities;
import com.combinator.ability.Gadgets;
import com.combinator.ability.MiningAbilities;
import com.combinator.ability.PassiveAbilities;
import com.combinator.ability.Tasks;
import com.combinator.block.CombinerTableBlock;
import com.combinator.item.ComboItems;
import com.combinator.recipe.ComboRecipes;
import com.combinator.screen.CombinerScreenHandler;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry point. Registers the Combiner Table, all combined items, the recipes and the abilities. */
public class ItemCombinator implements ModInitializer {
	public static final String MOD_ID = "combinator";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Block COMBINER_TABLE = new CombinerTableBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE));
	public static final Item COMBINER_TABLE_ITEM = new BlockItem(COMBINER_TABLE, new Item.Settings());

	public static final ScreenHandlerType<CombinerScreenHandler> COMBINER_SCREEN =
			new ScreenHandlerType<>(CombinerScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

	/** Every different ability error that was caught so far. The automatic tests read this list. */
	public static final List<String> ERRORS = new CopyOnWriteArrayList<>();
	private static final Set<String> SEEN_ERRORS = ConcurrentHashMap.newKeySet();

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	/** Reports a mistake in the mod's own data, such as a combination that names an item that does not exist. */
	public static void problem(String message) {
		if (SEEN_ERRORS.size() < 200 && SEEN_ERRORS.add(message)) {
			ERRORS.add(message);
			LOGGER.error(message);
		}
	}

	/**
	 * Reports an error inside an ability. The game keeps running.
	 * The same error is written to the log only once, so a bug that repeats every tick cannot flood the log.
	 */
	public static void error(String what, Throwable t) {
		StackTraceElement[] trace = t.getStackTrace();
		String key = what + ": " + t + (trace.length > 0 ? " at " + trace[0] : "");
		if (SEEN_ERRORS.size() < 200 && SEEN_ERRORS.add(key)) {
			ERRORS.add(key);
			LOGGER.error(what + " (the same error is not logged again)", t);
		}
	}

	@Override
	public void onInitialize() {
		Registry.register(Registries.BLOCK, id("combiner_table"), COMBINER_TABLE);
		Registry.register(Registries.ITEM, id("combiner_table"), COMBINER_TABLE_ITEM);
		Registry.register(Registries.SCREEN_HANDLER, id("combiner"), COMBINER_SCREEN);

		ComboItems.init();
		ComboRecipes.init();

		ItemGroup group = FabricItemGroup.builder()
				.icon(() -> new ItemStack(COMBINER_TABLE_ITEM))
				.displayName(Text.translatable("itemGroup.combinator.main"))
				.entries((context, entries) -> {
					entries.add(COMBINER_TABLE_ITEM);
					for (Item item : ComboItems.ALL) {
						entries.add(item);
					}
				})
				.build();
		Registry.register(Registries.ITEM_GROUP, id("main"), group);

		Tasks.register();
		MiningAbilities.register();
		CombatAbilities.register();
		PassiveAbilities.register();
		Gadgets.register();
		Capture.register();

		LOGGER.info("Item Combinator loaded: {} new items, {} combinations", ComboItems.ALL.size(), ComboRecipes.ALL.size());
	}
}
