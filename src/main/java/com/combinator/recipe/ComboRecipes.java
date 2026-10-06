package com.combinator.recipe;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * All combinations of the Combiner Table.
 * A combination is an unordered pair of items: A + B gives the same result as B + A.
 * Two stackable items that have no combination give a Chaos Orb instead.
 * The list itself lives in {@link ComboRecipeList}, which is generated from the design table.
 */
public final class ComboRecipes {
	/** One combination. rarity 0 = result is a vanilla item, 1-3 = result is a combined item (1 normal, 3 wildest). */
	public record Combo(Item a, Item b, Item result, int count, int rarity) {
		public Item partnerOf(Item item) {
			return item == this.a ? this.b : this.a;
		}

		public ItemStack createResult() {
			return new ItemStack(this.result, this.count);
		}
	}

	public static final List<Combo> ALL = new ArrayList<>();
	private static final Map<Item, Map<Item, Combo>> LOOKUP = new HashMap<>();
	private static final Map<Item, List<Combo>> BY_INPUT = new HashMap<>();

	private ComboRecipes() {
	}

	public static void init() {
		ALL.clear();
		LOOKUP.clear();
		BY_INPUT.clear();
		ComboRecipeList.registerAll();
	}

	/** Adds a combination. Ids are full ids such as "minecraft:diamond_pickaxe" or "combinator:excavator". */
	static void add(String a, String b, String result, int count, int rarity) {
		Item itemA = resolve(a);
		Item itemB = resolve(b);
		Item itemResult = resolve(result);
		if (itemA == null || itemB == null || itemResult == null) {
			ItemCombinator.problem("Skipping combination " + a + " + " + b + " -> " + result + ": unknown item id");
			return;
		}
		if (find(itemA, itemB) != null) {
			ItemCombinator.problem("Skipping duplicate combination " + a + " + " + b);
			return;
		}
		Combo combo = new Combo(itemA, itemB, itemResult, count, rarity);
		ALL.add(combo);
		LOOKUP.computeIfAbsent(itemA, k -> new HashMap<>()).put(itemB, combo);
		LOOKUP.computeIfAbsent(itemB, k -> new HashMap<>()).put(itemA, combo);
		BY_INPUT.computeIfAbsent(itemA, k -> new ArrayList<>()).add(combo);
		if (itemA != itemB) {
			BY_INPUT.computeIfAbsent(itemB, k -> new ArrayList<>()).add(combo);
		}
	}

	private static Item resolve(String id) {
		Identifier identifier = Identifier.of(id);
		if (!Registries.ITEM.containsId(identifier)) {
			return null;
		}
		return Registries.ITEM.get(identifier);
	}

	public static Combo find(Item a, Item b) {
		Map<Item, Combo> inner = LOOKUP.get(a);
		return inner == null ? null : inner.get(b);
	}

	/** All combinations that use the given item as one of the two inputs. */
	public static List<Combo> partners(Item item) {
		List<Combo> list = BY_INPUT.get(item);
		return list == null ? Collections.emptyList() : list;
	}

	/** The result for two input stacks, or an empty stack if they do not combine. */
	public static ItemStack craft(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty()) {
			return ItemStack.EMPTY;
		}
		Combo combo = find(a.getItem(), b.getItem());
		if (combo == null) {
			return chaosFallback(a, b);
		}
		ItemStack out = combo.createResult();
		carryEnchantments(a, b, out);
		return out;
	}

	/**
	 * Two items without a recipe make a Chaos Orb, but only if both are stackable.
	 * Tools, weapons and armor do not stack, so they are never used up by accident.
	 */
	private static ItemStack chaosFallback(ItemStack a, ItemStack b) {
		if (a.getMaxCount() > 1 && b.getMaxCount() > 1) {
			return new ItemStack(ComboItems.CHAOS_ORB);
		}
		return ItemStack.EMPTY;
	}

	/** Tools and armor keep the enchantments of both inputs (highest level wins) if they fit the result. */
	private static void carryEnchantments(ItemStack a, ItemStack b, ItemStack out) {
		if (!out.isDamageable()) {
			return;
		}
		ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
		boolean any = false;
		for (ItemStack in : new ItemStack[] {a, b}) {
			ItemEnchantmentsComponent enchantments = in.getEnchantments();
			for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
				int level = enchantments.getLevel(entry);
				if (level > builder.getLevel(entry) && entry.value().isAcceptableItem(out)) {
					builder.set(entry, level);
					any = true;
				}
			}
		}
		if (any) {
			out.set(DataComponentTypes.ENCHANTMENTS, builder.build());
		}
	}
}
