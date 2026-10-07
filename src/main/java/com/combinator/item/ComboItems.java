package com.combinator.item;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * All combined items. GENERATED from tools/spec.py - change the table there, not this file.
 * Each line: id, rarity (1-3, sets the name colour), number of tooltip lines, the item itself, its abilities.
 */
public final class ComboItems {
	/** Every combined item in creative-tab order. */
	public static final List<Item> ALL = new ArrayList<>();
	private static final Map<Item, Traits> TRAITS = new IdentityHashMap<>();
	private static final Map<Item, Integer> TIP_LINES = new IdentityHashMap<>();
	private static final Map<Item, Integer> RARITIES = new IdentityHashMap<>();
	public static final Item IRON_HAMMER = add("iron_hammer", 1, 2,
			new CPick(ComboMaterial.of(750, 4.5F, "iron", 14), 5.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).area(1, false));
	public static final Item EXCAVATOR = add("excavator", 1, 2,
			new CPick(ComboMaterial.of(4000, 5.5F, "diamond", 14), 6.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).area(1, true));
	public static final Item MOLTEN_PICKAXE = add("molten_pickaxe", 1, 1,
			new CPick(ComboMaterial.of(2000, 8.0F, "diamond", 14), 5.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).smelt());
	public static final Item VEIN_PICKAXE = add("vein_pickaxe", 1, 2,
			new CPick(ComboMaterial.of(2000, 8.0F, "diamond", 14), 5.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).vein(48));
	public static final Item PROSPECTOR_PICKAXE = add("prospector_pickaxe", 1, 1,
			new CPick(ComboMaterial.of(1800, 8.0F, "diamond", 14), 5.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).doubleOres());
	public static final Item TORCH_PICKAXE = add("torch_pickaxe", 1, 2,
			new CPick(ComboMaterial.of(600, 6.0F, "iron", 14), 4.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).torch());
	public static final Item REDSTONE_DRILL = add("redstone_drill", 1, 2,
			new CPick(ComboMaterial.of(3000, 16.0F, "diamond", 14), 4.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).fx(StatusEffects.HASTE, 1));
	public static final Item PAXEL = add("paxel", 1, 1,
			new CPaxel(ComboMaterial.of(3000, 8.0F, "diamond", 14), 7.0F, -3.0F, props(1)),
			null);
	public static final Item MAGNETIC_PICKAXE = add("magnetic_pickaxe", 2, 1,
			new CPick(ComboMaterial.of(2500, 9.0F, "diamond", 18), 5.0F, -2.8F, props(2)),
			new Traits().at(Traits.Where.HELD).magnetDrops());
	public static final Item MAGMA_EXCAVATOR = add("magma_excavator", 2, 2,
			new CPick(ComboMaterial.of(6000, 7.0F, "netherite", 18), 7.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).area(1, true).smelt());
	public static final Item MOTHERLODE_PICKAXE = add("motherlode_pickaxe", 2, 2,
			new CPick(ComboMaterial.of(4000, 9.0F, "netherite", 18), 6.0F, -2.8F, props(2)),
			new Traits().at(Traits.Where.HELD).vein(96).doubleOres());
	public static final Item TUNNEL_BORE = add("tunnel_bore", 2, 2,
			new CPick(ComboMaterial.of(6000, 14.0F, "netherite", 18), 6.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).area(1, false).fx(StatusEffects.HASTE, 1));
	public static final Item WORLD_EATER = add("world_eater", 3, 4,
			new CPaxel(ComboMaterial.of(12000, 12.0F, "netherite", 22), 9.0F, -3.0F, props(3)),
			new Traits().at(Traits.Where.HELD).area(4, true).smelt().magnetDrops().doubleOres());
	public static final Item LUMBER_AXE = add("lumber_axe", 1, 2,
			new CAxe(ComboMaterial.of(3000, 8.0F, "diamond", 14), 9.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).tree(160));
	public static final Item EMBER_AXE = add("ember_axe", 1, 2,
			new CAxe(ComboMaterial.of(1800, 8.0F, "diamond", 14), 9.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).smelt().ignite(5));
	public static final Item BATTLE_AXE = add("battle_axe", 1, 1,
			new CAxe(ComboMaterial.of(2400, 8.0F, "diamond", 14), 12.0F, -3.1F, props(1)),
			new Traits().at(Traits.Where.HELD).sweep(2.5f, 0.5f));
	public static final Item EXECUTIONER_AXE = add("executioner_axe", 1, 2,
			new CAxe(ComboMaterial.of(2000, 8.0F, "diamond", 14), 10.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).heads(0.15f).bonusXp(3));
	public static final Item DEFORESTER = add("deforester", 2, 2,
			new CAxe(ComboMaterial.of(6000, 10.0F, "netherite", 18), 10.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).tree(512).magnetDrops());
	public static final Item WARLORD_AXE = add("warlord_axe", 2, 2,
			new CAxe(ComboMaterial.of(5000, 9.0F, "netherite", 18), 15.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).sweep(3.0f, 0.6f).heads(0.35f).bonusXp(6));
	public static final Item TRENCH_SHOVEL = add("trench_shovel", 1, 2,
			new CShovel(ComboMaterial.of(4000, 6.0F, "diamond", 14), 6.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).area(1, true));
	public static final Item KILN_SHOVEL = add("kiln_shovel", 1, 2,
			new CShovel(ComboMaterial.of(2000, 8.0F, "diamond", 14), 5.5F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).smelt());
	public static final Item TERRAFORMER = add("terraformer", 2, 2,
			new CPaxel(ComboMaterial.of(8000, 8.0F, "netherite", 18), 7.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).area(1, true));
	public static final Item HARVESTER_HOE = add("harvester_hoe", 1, 2,
			new CHoe(ComboMaterial.of(2500, 8.0F, "diamond", 14), 1.0F, 0.0F, props(1)),
			new Traits().at(Traits.Where.HELD).harvest(2));
	public static final Item VERDANT_STAFF = add("verdant_staff", 1, 2,
			new CItem(props(1).maxDamage(256)),
			new Traits().at(Traits.Where.HOTBAR).bonemeal(1));
	public static final Item FARMERS_SCYTHE = add("farmers_scythe", 2, 3,
			new CHoe(ComboMaterial.of(5000, 10.0F, "netherite", 18), 6.0F, -2.0F, props(2)),
			new Traits().at(Traits.Where.HELD).harvest(4).magnetDrops());
	public static final Item BLAZING_SWORD = add("blazing_sword", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).ignite(6));
	public static final Item FROST_BLADE = add("frost_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).hit(StatusEffects.SLOWNESS, 100, 2).freeze());
	public static final Item VENOM_BLADE = add("venom_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 7, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).hit(StatusEffects.POISON, 120, 1));
	public static final Item WITHERING_BLADE = add("withering_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).hit(StatusEffects.WITHER, 120, 1));
	public static final Item THUNDER_BLADE = add("thunder_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).lightning(5.0f).strikeCooldown(40));
	public static final Item VAMPIRE_BLADE = add("vampire_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 7, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).lifesteal(0.25f));
	public static final Item ENDER_BLADE = add("ender_blade", 1, 1,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).use(Use.BLINK).range(12).cooldown(40).cost(3));
	public static final Item GREATSWORD = add("greatsword", 1, 2,
			new CSword(ComboMaterial.of(3000, 1.0F, "diamond", 14), 12, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).sweep(3.0f, 0.5f));
	public static final Item GALE_SABER = add("gale_saber", 1, 2,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 7, -2.0F, props(1)),
			new Traits().at(Traits.Where.HELD).knockUp(0.9f));
	public static final Item MIDAS_SWORD = add("midas_sword", 1, 2,
			new CSword(ComboMaterial.of(1200, 1.0F, "diamond", 14), 7, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).nuggets(4).bonusXp(4));
	public static final Item CREEPER_CLEAVER = add("creeper_cleaver", 1, 2,
			new CSword(ComboMaterial.of(1800, 1.0F, "diamond", 14), 8, -2.6F, props(1)),
			new Traits().at(Traits.Where.HELD).boomHit(3.0f).strikeCooldown(15));
	public static final Item SLIME_BAT = add("slime_bat", 1, 1,
			new CSword(ComboMaterial.of(400, 1.0F, "wooden", 14), 3, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).knockback(7.0f));
	public static final Item FROSTFIRE_BLADE = add("frostfire_blade", 2, 2,
			new CSword(ComboMaterial.of(4000, 1.0F, "netherite", 18), 10, -2.4F, props(2)),
			new Traits().at(Traits.Where.HELD).ignite(8).hit(StatusEffects.SLOWNESS, 120, 3).freeze().magic(3.0f));
	public static final Item PLAGUE_BLADE = add("plague_blade", 2, 1,
			new CSword(ComboMaterial.of(4000, 1.0F, "netherite", 18), 9, -2.4F, props(2)),
			new Traits().at(Traits.Where.HELD).hit(StatusEffects.POISON, 160, 2).hit(StatusEffects.WITHER, 160, 2).hit(StatusEffects.WEAKNESS, 160, 1));
	public static final Item TEMPEST_BLADE = add("tempest_blade", 2, 3,
			new CSword(ComboMaterial.of(4000, 1.0F, "netherite", 18), 10, -2.2F, props(2)),
			new Traits().at(Traits.Where.HELD).lightning(8.0f).chain(10.0f, 6.0f).knockUp(0.8f).strikeCooldown(30));
	public static final Item BLOODTHIRST_GREATSWORD = add("bloodthirst_greatsword", 2, 2,
			new CSword(ComboMaterial.of(5000, 1.0F, "netherite", 18), 14, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).lifesteal(0.35f).sweep(3.0f, 0.6f));
	public static final Item VOID_BLADE = add("void_blade", 2, 2,
			new CSword(ComboMaterial.of(4000, 1.0F, "netherite", 18), 10, -2.4F, props(2)),
			new Traits().at(Traits.Where.HELD).use(Use.BLINK).range(28).cooldown(20).cost(2).magic(4.0f));
	public static final Item ELEMENTAL_BLADE = add("elemental_blade", 3, 3,
			new CSword(ComboMaterial.of(9000, 1.0F, "netherite", 22), 14, -2.2F, props(3)),
			new Traits().at(Traits.Where.HELD).ignite(8).hit(StatusEffects.SLOWNESS, 120, 3).freeze().lightning(8.0f).chain(12.0f, 8.0f).knockUp(0.6f).magic(4.0f).strikeCooldown(30).use(Use.BEAM).range(40).power(18.0f).cooldown(30));
	public static final Item SOUL_REAVER = add("soul_reaver", 3, 3,
			new CSword(ComboMaterial.of(9000, 1.0F, "netherite", 22), 17, -2.8F, props(3)),
			new Traits().at(Traits.Where.HELD).hit(StatusEffects.POISON, 200, 2).hit(StatusEffects.WITHER, 200, 2).hit(StatusEffects.WEAKNESS, 200, 1).lifesteal(0.5f).sweep(3.5f, 0.7f));
	public static final Item MAGNET = add("magnet", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).pull(7));
	public static final Item FIRE_WAND = add("fire_wand", 1, 1,
			new CItem(props(1).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SMALL_FIREBALL).cooldown(12).cost(1));
	public static final Item FROST_WAND = add("frost_wand", 1, 2,
			new CItem(props(1).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FROST_CONE).range(7).power(4.0f).cooldown(30).cost(1));
	public static final Item STORM_STAFF = add("storm_staff", 1, 2,
			new CItem(props(1).maxDamage(150)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LIGHTNING).range(48).cooldown(40).cost(1));
	public static final Item WARP_STAFF = add("warp_staff", 1, 2,
			new CItem(props(1).maxDamage(250)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BLINK).range(32).cooldown(20).cost(1));
	public static final Item GUST_STAFF = add("gust_staff", 1, 2,
			new CItem(props(1).maxDamage(300)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LEAP).power(1.6f).cooldown(25).cost(1).noFall());
	public static final Item HEALING_STAFF = add("healing_staff", 1, 2,
			new CItem(props(1).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HEAL).power(8.0f).cooldown(300).cost(1));
	public static final Item RECALL_COMPASS = add("recall_compass", 1, 2,
			new CItem(props(1).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.RECALL).cooldown(600).cost(1));
	public static final Item DYNAMITE = add("dynamite", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DYNAMITE).power(1.2f).cooldown(15).consume());
	public static final Item ENDER_POUCH = add("ender_pouch", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ENDER_POUCH));
	public static final Item POCKET_WORKBENCH = add("pocket_workbench", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WORKBENCH));
	public static final Item MENDING_CHARM = add("mending_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).mend());
	public static final Item FEATHER_CHARM = add("feather_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).noFall());
	public static final Item OBSIDIAN_CHARM = add("obsidian_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.FIRE_RESISTANCE, 0));
	public static final Item TIDE_CHARM = add("tide_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0));
	public static final Item HASTE_CHARM = add("haste_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.HASTE, 1));
	public static final Item SWIFT_CHARM = add("swift_charm", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.SPEED, 1));
	public static final Item SEER_SPYGLASS = add("seer_spyglass", 1, 2,
			new CItem(props(1).maxDamage(100)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.GLOW_SCAN).range(32).cooldown(100).cost(1));
	public static final Item ETERNAL_TOTEM = add("eternal_totem", 1, 3,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).totem(6000));
	public static final Item CHRONO_CLOCK = add("chrono_clock", 1, 2,
			new CItem(props(1).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TIME_DAY).sneakUse(Use.TIME_NIGHT, 200).cooldown(200).cost(1));
	public static final Item EXPLORER_CHARM = add("explorer_charm", 2, 2,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).noFall().fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1));
	public static final Item ELEMENTAL_CHARM = add("elemental_charm", 2, 3,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 0));
	public static final Item INFERNO_STAFF = add("inferno_staff", 2, 2,
			new CItem(props(2).maxDamage(300)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BIG_FIREBALL).power(4.0f).cooldown(30).cost(1));
	public static final Item TEMPEST_STAFF = add("tempest_staff", 2, 3,
			new CItem(props(2).maxDamage(400)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LIGHTNING_STORM).range(64).cooldown(60).cost(1).sneakUse(Use.LEAP, 25).power(1.8f).noFall());
	public static final Item RIFT_STAFF = add("rift_staff", 2, 2,
			new CItem(props(2).maxDamage(500)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BLINK).range(64).cooldown(15).cost(1).sneakUse(Use.RECALL, 300));
	public static final Item ANCIENT_CHARM = add("ancient_charm", 3, 4,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).attr("generic.max_health", 20.0, 0).noFall().fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1).fx(StatusEffects.HASTE, 1).fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 0));
	public static final Item ARCHMAGE_STAFF = add("archmage_staff", 3, 3,
			new CItem(props(3).maxDamage(1500)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BIG_FIREBALL).power(6.0f).cooldown(20).cost(1).sneakUse(Use.METEOR_SHOWER, 80).range(96).noFall());
	public static final Item ARMORED_ELYTRA = add("armored_elytra", 1, 2,
			new CWings(armorMaterial("armored_elytra", 8, 2.0F, 0.0F, "diamond", true), props(1).maxDamage(900)),
			null);
	public static final Item ROCKET_ELYTRA = add("rocket_elytra", 2, 2,
			new CWings(armorMaterial("rocket_elytra", 8, 2.0F, 0.0F, "diamond", true), props(2).maxDamage(1500)),
			new Traits().at(Traits.Where.WORN).boost());
	public static final Item SERAPH_WINGS = add("seraph_wings", 3, 2,
			new CWings(armorMaterial("seraph_wings", 8, 3.0F, 0.1F, "netherite", true), props(3).maxDamage(4000)),
			new Traits().at(Traits.Where.WORN).boost().flight());
	public static final Item MINER_HELMET = add("miner_helmet", 1, 1,
			new CArmor(armorMaterial("miner_helmet", 2, 0.0F, 0.0F, "iron", false), ArmorItem.Type.HELMET, props(1).maxDamage(240)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.NIGHT_VISION, 0));
	public static final Item DIVING_HELMET = add("diving_helmet", 1, 2,
			new CArmor(armorMaterial("diving_helmet", 2, 0.0F, 0.0F, "iron", false), ArmorItem.Type.HELMET, props(1).maxDamage(240)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.WATER_BREATHING, 0));
	public static final Item MAGMA_CHESTPLATE = add("magma_chestplate", 1, 2,
			new CArmor(armorMaterial("magma_chestplate", 8, 2.0F, 0.0F, "diamond", false), ArmorItem.Type.CHESTPLATE, props(1).maxDamage(600)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.FIRE_RESISTANCE, 0).thorns(0.0f, 5));
	public static final Item CACTUS_CHESTPLATE = add("cactus_chestplate", 1, 1,
			new CArmor(armorMaterial("cactus_chestplate", 6, 0.0F, 0.0F, "iron", false), ArmorItem.Type.CHESTPLATE, props(1).maxDamage(300)),
			new Traits().at(Traits.Where.WORN).thorns(3.0f, 0));
	public static final Item SWIFT_LEGGINGS = add("swift_leggings", 1, 1,
			new CArmor(armorMaterial("swift_leggings", 6, 2.0F, 0.0F, "diamond", false), ArmorItem.Type.LEGGINGS, props(1).maxDamage(560)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.SPEED, 1));
	public static final Item SPRING_BOOTS = add("spring_boots", 1, 2,
			new CArmor(armorMaterial("spring_boots", 2, 0.0F, 0.0F, "iron", false), ArmorItem.Type.BOOTS, props(1).maxDamage(260)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.JUMP_BOOST, 2).noFall());
	public static final Item SPELUNKER_HELMET = add("spelunker_helmet", 2, 2,
			new CArmor(armorMaterial("spelunker_helmet", 3, 2.0F, 0.0F, "diamond", false), ArmorItem.Type.HELMET, props(2).maxDamage(500)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.HASTE, 0));
	public static final Item INFERNO_THORNMAIL = add("inferno_thornmail", 2, 3,
			new CArmor(armorMaterial("inferno_thornmail", 9, 3.0F, 0.1F, "netherite", false), ArmorItem.Type.CHESTPLATE, props(2).maxDamage(900)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.RESISTANCE, 0).thorns(6.0f, 8).thornsBoom(2.5f));
	public static final Item HERMES_BOOTS = add("hermes_boots", 2, 2,
			new CArmor(armorMaterial("hermes_boots", 3, 2.0F, 0.0F, "diamond", false), ArmorItem.Type.BOOTS, props(2).maxDamage(600)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.SPEED, 2).fx(StatusEffects.JUMP_BOOST, 2).noFall());
	public static final Item GOLDEN_STEAK = add("golden_steak", 1, 1,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(10).saturationModifier(1.2F)
					.statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0), 1.0F).build())),
			null);
	public static final Item HONEY_TOAST = add("honey_toast", 1, 1,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(8).saturationModifier(0.8F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 0), 1.0F).build())),
			null);
	public static final Item MINERS_PIE = add("miners_pie", 1, 1,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(9).saturationModifier(0.6F)
					.statusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 3600, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 3600, 0), 1.0F).build())),
			null);
	public static final Item BLAZING_STEW = add("blazing_stew", 1, 2,
			new CItem(props(1).maxCount(1).food(new FoodComponent.Builder().nutrition(8).saturationModifier(0.8F)
					.statusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3600, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 900, 0), 1.0F).usingConvertsTo(Items.BOWL).build())),
			null);
	public static final Item GLOWING_CARROT = add("glowing_carrot", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(6).saturationModifier(1.2F)
					.statusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0), 1.0F).alwaysEdible().build())),
			null);
	public static final Item SUGAR_COOKIE = add("sugar_cookie", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.2F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400, 1), 1.0F).alwaysEdible().snack().build())),
			null);
	public static final Item HERO_SANDWICH = add("hero_sandwich", 1, 1,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(14).saturationModifier(0.9F).build())),
			null);
	public static final Item KELP_ROLL = add("kelp_roll", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(9).saturationModifier(0.8F)
					.statusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 2400, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 1200, 0), 1.0F).build())),
			null);
	public static final Item ROYAL_FEAST = add("royal_feast", 2, 2,
			new CItem(props(2).maxCount(16).food(new FoodComponent.Builder().nutrition(20).saturationModifier(1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 0), 1.0F).build())),
			null);
	public static final Item AMBROSIA = add("ambrosia", 3, 3,
			new CItem(props(3).maxCount(16).food(new FoodComponent.Builder().nutrition(20).saturationModifier(1.5F)
					.statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 600, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 2400, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0), 1.0F).alwaysEdible().build())),
			null);
	public static final Item TNT_PICKAXE = add("tnt_pickaxe", 2, 3,
			new CPick(ComboMaterial.of(3000, 8.0F, "diamond", 18), 5.0F, -2.8F, props(2)),
			new Traits().at(Traits.Where.HELD).mineBoom(3.5f));
	public static final Item MINING_LASER = add("mining_laser", 2, 2,
			new CItem(props(2).maxDamage(500)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LASER_DRILL).range(48).cooldown(30).cost(1));
	public static final Item EARTHSHAKER = add("earthshaker", 2, 3,
			new CPick(ComboMaterial.of(5000, 6.0F, "netherite", 18), 8.0F, -3.0F, props(2)),
			new Traits().at(Traits.Where.HELD).area(1, true).use(Use.QUAKE).cooldown(60).cost(5));
	public static final Item HOME_RUN_BAT = add("home_run_bat", 2, 1,
			new CSword(ComboMaterial.of(800, 1.0F, "wooden", 18), 4, -2.4F, props(2)),
			new Traits().at(Traits.Where.HELD).knockback(30.0f).knockUp(1.6f));
	public static final Item BEAM_SABER = add("beam_saber", 2, 3,
			new CSword(ComboMaterial.of(2500, 1.0F, "diamond", 18), 9, -2.2F, props(2)),
			new Traits().at(Traits.Where.HELD).ignite(4).use(Use.BEAM).range(40).power(18.0f).cooldown(25).cost(2));
	public static final Item OMNI_BLADE = add("omni_blade", 3, 2,
			new CSword(ComboMaterial.of(20000, 1.0F, "netherite", 22), 25, -2.0F, props(3)),
			new Traits().at(Traits.Where.HELD).ignite(10).hit(StatusEffects.SLOWNESS, 200, 3).hit(StatusEffects.POISON, 200, 2).hit(StatusEffects.WITHER, 200, 2).hit(StatusEffects.WEAKNESS, 200, 1).freeze().lifesteal(0.5f).sweep(4.0f, 0.8f).lightning(10.0f).chain(12.0f, 10.0f).knockUp(0.6f).magic(6.0f).strikeCooldown(20).use(Use.BEAM).range(48).power(30.0f).cooldown(20));
	public static final Item MEGA_DYNAMITE = add("mega_dynamite", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MEGA_BOMB).power(9.0f).cooldown(20).consume());
	public static final Item NUKE = add("nuke", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.NUKE).power(18.0f).cooldown(40).consume());
	public static final Item DOOMSDAY_DEVICE = add("doomsday_device", 3, 2,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.NUKE).power(32.0f).cooldown(200).consume());
	public static final Item METEOR_STAFF = add("meteor_staff", 3, 2,
			new CItem(props(3).maxDamage(300)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.METEOR).range(96).power(7.0f).cooldown(40).cost(1).sneakUse(Use.METEOR_SHOWER, 100));
	public static final Item SINGULARITY = add("singularity", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BLACK_HOLE).range(48).cooldown(60).consume());
	public static final Item OCEAN_ORB = add("ocean_orb", 2, 3,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FLOOD).range(32).cooldown(20).cost(1).sneakUse(Use.DRAIN, 20));
	public static final Item WINTER_GLOBE = add("winter_globe", 2, 3,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FREEZE_AREA).range(14).cooldown(60).cost(1));
	public static final Item ANVIL_STAFF = add("anvil_staff", 2, 1,
			new CItem(props(2).maxDamage(100)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ANVIL_STORM).range(64).cooldown(60).cost(1));
	public static final Item POULTRY_STAFF = add("poultry_staff", 2, 2,
			new CItem(props(2).maxDamage(100)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CHICKEN_STORM).range(64).cooldown(40).cost(1));
	public static final Item WITHER_WAND = add("wither_wand", 2, 1,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WITHER_SKULL).cooldown(8).cost(1));
	public static final Item ARROW_GATLING = add("arrow_gatling", 2, 2,
			new CItem(props(2).maxDamage(1000)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ARROW_BURST).power(10.0f).cooldown(4).cost(1));
	public static final Item MIDAS_GLOVE = add("midas_glove", 3, 3,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HELD).midas());
	public static final Item PHILOSOPHER_STONE = add("philosopher_stone", 3, 3,
			new CItem(props(3).maxDamage(256)),
			new Traits().at(Traits.Where.HELD).transmute());
	public static final Item POLYMORPH_WAND = add("polymorph_wand", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.POLYMORPH).range(24).cooldown(20).cost(1));
	public static final Item SIZE_RAY = add("size_ray", 2, 2,
			new CItem(props(2).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SHRINK).range(32).cooldown(10).cost(1).sneakUse(Use.GROW, 10));
	public static final Item EAT_ME_CAKE = add("eat_me_cake", 2, 3,
			new CItem(props(2).maxCount(16).food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6F).alwaysEdible().build())),
			new Traits().at(Traits.Where.HELD).eatBuff(120).attr("generic.scale", 3.0, 2).attr("generic.max_health", 40.0, 0).attr("generic.attack_damage", 10.0, 0).attr("generic.step_height", 2.0, 0).attr("player.block_interaction_range", 8.0, 0).attr("player.entity_interaction_range", 8.0, 0));
	public static final Item SHRINKING_MUSHROOM = add("shrinking_mushroom", 2, 3,
			new CItem(props(2).maxCount(64).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.2F).alwaysEdible().snack().build())),
			new Traits().at(Traits.Where.HELD).eatBuff(120).attr("generic.scale", -0.8, 2).attr("generic.safe_fall_distance", 20.0, 0).attr("generic.movement_speed", 0.5, 2));
	public static final Item TIME_STOPPER = add("time_stopper", 3, 2,
			new CItem(props(3).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TIME_STOP).power(10.0f).cooldown(400).cost(1));
	public static final Item MADNESS_BELL = add("madness_bell", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MADNESS).range(24).cooldown(100).cost(1));
	public static final Item GOLEM_HORN = add("golem_horn", 2, 2,
			new CItem(props(2).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SUMMON_GOLEMS).power(3.0f).cooldown(600).cost(1));
	public static final Item GRAVITY_STAFF = add("gravity_staff", 2, 2,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LIFT).range(16).cooldown(60).cost(1).sneakUse(Use.SLAM, 40));
	public static final Item SWAP_STAFF = add("swap_staff", 2, 1,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SWAP).range(48).cooldown(10).cost(1));
	public static final Item GHOST_CLOAK = add("ghost_cloak", 3, 2,
			new CItem(props(3).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.GHOST).power(8.0f).cooldown(400).cost(1));
	public static final Item PORTABLE_HOLE = add("portable_hole", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HOLE).range(12).cooldown(40).cost(1));
	public static final Item BRIDGE_STAFF = add("bridge_staff", 1, 2,
			new CItem(props(1).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BRIDGE).range(32).cooldown(20).cost(1));
	public static final Item HOUSE_BOX = add("house_box", 2, 2,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HELD).house());
	public static final Item FOREST_STAFF = add("forest_staff", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FOREST).range(32).cooldown(40).cost(1));
	public static final Item ORE_DOWSER = add("ore_dowser", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DOWSE).range(32).cooldown(40).cost(1));
	public static final Item TITAN_BELT = add("titan_belt", 3, 3,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).attr("generic.scale", 1.5, 2).attr("generic.max_health", 40.0, 0).attr("generic.attack_damage", 8.0, 0).attr("generic.step_height", 1.5, 0).attr("player.block_interaction_range", 4.0, 0).attr("player.entity_interaction_range", 4.0, 0));
	public static final Item LONG_ARM_GLOVE = add("long_arm_glove", 2, 2,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).attr("player.block_interaction_range", 28.0, 0).attr("player.entity_interaction_range", 29.0, 0));
	public static final Item COLOSSUS_HEART = add("colossus_heart", 3, 2,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).attr("generic.max_health", 80.0, 0).fx(StatusEffects.REGENERATION, 1));
	public static final Item MINER_GAUNTLET = add("miner_gauntlet", 3, 3,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).attr("player.block_break_speed", 9.0, 2).fx(StatusEffects.HASTE, 1));
	public static final Item FIST_OF_DOOM = add("fist_of_doom", 3, 2,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HELD).instakill().knockback(8.0f));
	public static final Item GODHOOD_CHARM = add("godhood_charm", 3, 3,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).flight().noFall().attr("generic.max_health", 80.0, 0).attr("player.block_interaction_range", 6.0, 0).attr("player.entity_interaction_range", 6.0, 0).attr("player.block_break_speed", 4.0, 2).fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1).fx(StatusEffects.HASTE, 1).fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 1).fx(StatusEffects.REGENERATION, 1));
	public static final Item MOON_BOOTS = add("moon_boots", 2, 2,
			new CArmor(armorMaterial("moon_boots", 3, 2.0F, 0.0F, "diamond", false), ArmorItem.Type.BOOTS, props(2).maxDamage(600)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.JUMP_BOOST, 3).noFall().attr("generic.gravity", -0.85, 2));
	public static final Item ROCKET_CHILI = add("rocket_chili", 2, 3,
			new CItem(props(2).maxCount(64).food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.4F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 600, 0), 1.0F).alwaysEdible().build())),
			new Traits().at(Traits.Where.HELD).eatLaunch(4.0f));
	public static final Item XP_TOME = add("xp_tome", 3, 2,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.XP).power(5.0f).cooldown(10));
	public static final Item DUPE_MIRROR = add("dupe_mirror", 3, 2,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DUPE).cooldown(40));
	public static final Item CHAOS_ORB = add("chaos_orb", 2, 3,
			new CItem(props(2).maxCount(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CHAOS).cooldown(10).consume());
	public static final Item PANDORA_BOX = add("pandora_box", 3, 2,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PANDORA).cooldown(140).consume());
	public static final Item FLINT_PICKAXE = add("flint_pickaxe", 1, 2,
			new CPick(ComboMaterial.of(180, 7.0F, "stone", 14), 3.0F, -2.8F, props(1)),
			null);
	public static final Item STONE_HAMMER = add("stone_hammer", 1, 2,
			new CPick(ComboMaterial.of(400, 3.5F, "stone", 14), 4.0F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).area(1, false));
	public static final Item GENTLE_PICKAXE = add("gentle_pickaxe", 1, 2,
			new CPick(ComboMaterial.of(350, 6.0F, "iron", 14), 4.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).silk());
	public static final Item GILDED_PICKAXE = add("gilded_pickaxe", 1, 2,
			new CPick(ComboMaterial.of(320, 12.0F, "iron", 14), 4.0F, -2.8F, props(1)),
			new Traits().at(Traits.Where.HELD).stoneNuggets(0.12f));
	public static final Item IRON_MULTITOOL = add("iron_multitool", 1, 1,
			new CPaxel(ComboMaterial.of(600, 6.0F, "iron", 14), 6.0F, -3.0F, props(1)),
			null);
	public static final Item TIMBER_AXE = add("timber_axe", 1, 2,
			new CAxe(ComboMaterial.of(300, 4.0F, "stone", 14), 9.0F, -3.2F, props(1)),
			new Traits().at(Traits.Where.HELD).tree(40));
	public static final Item SIFTING_SHOVEL = add("sifting_shovel", 1, 2,
			new CShovel(ComboMaterial.of(260, 4.0F, "stone", 14), 3.5F, -3.0F, props(1)),
			new Traits().at(Traits.Where.HELD).sift());
	public static final Item GOLDEN_SICKLE = add("golden_sickle", 1, 2,
			new CHoe(ComboMaterial.of(150, 12.0F, "gold", 14), 1.0F, 0.0F, props(1)),
			new Traits().at(Traits.Where.HELD).harvest(1));
	public static final Item PLANTER_HOE = add("planter_hoe", 1, 2,
			new CHoe(ComboMaterial.of(120, 2.0F, "wooden", 14), 1.0F, -1.0F, props(1)),
			new Traits().at(Traits.Where.HELD).sow());
	public static final Item QUARTERSTAFF = add("quarterstaff", 1, 2,
			new CSword(ComboMaterial.of(150, 1.0F, "wooden", 14), 5, -2.2F, props(1)),
			new Traits().at(Traits.Where.HELD).attr("player.entity_interaction_range", 2.0, 0).knockback(1.5f));
	public static final Item JAGGED_BLADE = add("jagged_blade", 1, 1,
			new CSword(ComboMaterial.of(220, 1.0F, "stone", 14), 6, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).bleed(6));
	public static final Item DUAL_BLADES = add("dual_blades", 1, 2,
			new CSword(ComboMaterial.of(500, 1.0F, "iron", 14), 4, -0.8F, props(1)),
			new Traits().at(Traits.Where.HELD).magic(1.0f));
	public static final Item BULWARK_BLADE = add("bulwark_blade", 1, 2,
			new CSword(ComboMaterial.of(600, 1.0F, "iron", 14), 7, -2.6F, props(1)),
			new Traits().at(Traits.Where.HELD).attr("generic.armor", 6.0, 0).attr("generic.knockback_resistance", 0.6, 0));
	public static final Item LOOTER_BLADE = add("looter_blade", 1, 1,
			new CSword(ComboMaterial.of(250, 1.0F, "gold", 14), 6, -2.4F, props(1)),
			new Traits().at(Traits.Where.HELD).doubleLoot());
	public static final Item BOOMERANG = add("boomerang", 1, 2,
			new CItem(props(1).maxDamage(200)),
			new Traits().at(Traits.Where.HELD).use(Use.BOOMERANG).range(18).power(5.0f).cooldown(16).cost(1));
	public static final Item VOLLEY_BOW = add("volley_bow", 1, 2,
			new CItem(props(1).maxDamage(300)),
			new Traits().at(Traits.Where.HELD).use(Use.ARROW_BURST).power(5.0f).cooldown(24).cost(1));
	public static final Item TORCH_BOW = add("torch_bow", 1, 2,
			new CItem(props(1).maxDamage(256)),
			new Traits().at(Traits.Where.HELD).use(Use.TORCH_SHOT).range(40).cooldown(6).cost(1));
	public static final Item GRAPPLING_HOOK = add("grappling_hook", 1, 2,
			new CItem(props(1).maxDamage(250)),
			new Traits().at(Traits.Where.HELD).use(Use.GRAPPLE).range(40).cooldown(20).cost(1).noFall());
	public static final Item LASSO = add("lasso", 1, 1,
			new CItem(props(1).maxDamage(200)),
			new Traits().at(Traits.Where.HELD).use(Use.YANK).range(24).cooldown(15).cost(1));
	public static final Item ANGLER_ROD = add("angler_rod", 1, 2,
			new CItem(props(1).maxDamage(64)),
			new Traits().at(Traits.Where.HELD).use(Use.FISH).range(12).cooldown(100).cost(1));
	public static final Item BACKPACK = add("backpack", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).use(Use.BACKPACK).power(3.0f));
	public static final Item BIG_BACKPACK = add("big_backpack", 2, 2,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HELD).use(Use.BACKPACK).power(6.0f));
	public static final Item POCKET_FURNACE = add("pocket_furnace", 1, 2,
			new CItem(props(1).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SMELT_HAND).cooldown(20).cost(1));
	public static final Item BEDROLL = add("bedroll", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SET_SPAWN).cooldown(40));
	public static final Item WAYSTONE = add("waystone", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WAYPOINT_GO).cooldown(200).sneakUse(Use.WAYPOINT_SET, 20));
	public static final Item ROPE_LADDER = add("rope_ladder", 1, 2,
			new CItem(props(1).maxDamage(32)),
			new Traits().at(Traits.Where.HELD).ropeLadder());
	public static final Item HEDGE_SHEARS = add("hedge_shears", 1, 2,
			new CItem(props(1).maxDamage(300)),
			new Traits().at(Traits.Where.HELD).use(Use.SHEAR_AREA).range(8).cooldown(20).cost(1));
	public static final Item SMOKE_BOMB = add("smoke_bomb", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SMOKE).cooldown(40).consume());
	public static final Item ENDLESS_BUCKET = add("endless_bucket", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).endlessWater());
	public static final Item WOLF_WHISTLE = add("wolf_whistle", 1, 2,
			new CItem(props(1).maxDamage(3)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SUMMON_WOLVES).power(1.0f).cooldown(1200).cost(1));
	public static final Item STEED_HORN = add("steed_horn", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SUMMON_HORSE).cooldown(100).consume());
	public static final Item TRADER_TOKEN = add("trader_token", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SUMMON_TRADER).cooldown(100).consume());
	public static final Item SHEPHERD_CROOK = add("shepherd_crook", 1, 2,
			new CItem(props(1).maxDamage(100)),
			new Traits().at(Traits.Where.HELD).use(Use.LURE).range(20).cooldown(100).cost(1));
	public static final Item MOB_NET = add("mob_net", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HELD).capture());
	public static final Item DIVINING_ROD = add("divining_rod", 1, 2,
			new CItem(props(1).maxDamage(32)),
			new Traits().at(Traits.Where.HELD).use(Use.ORE_SIGHT).range(8).cooldown(240).cost(1));
	public static final Item TOME_OF_CHANCE = add("tome_of_chance", 2, 2,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ENCHANT_BOOK).cooldown(10).consume());
	public static final Item HIKING_STAFF = add("hiking_staff", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).attr("generic.step_height", 0.5, 0).attr("generic.safe_fall_distance", 3.0, 0));
	public static final Item DREAMCATCHER = add("dreamcatcher", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).noPhantoms());
	public static final Item LUCKY_HORSESHOE = add("lucky_horseshoe", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).fx(StatusEffects.LUCK, 0));
	public static final Item EMERGENCY_CHICKEN = add("emergency_chicken", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).fx(StatusEffects.SLOW_FALLING, 0).noFall());
	public static final Item CLOUD_SLIPPERS = add("cloud_slippers", 1, 1,
			new CArmor(armorMaterial("cloud_slippers", 1, 0.0F, 0.0F, "leather", false), ArmorItem.Type.BOOTS, props(1).maxDamage(120)),
			new Traits().at(Traits.Where.WORN).noFall());
	public static final Item MERCHANT_CROWN = add("merchant_crown", 1, 1,
			new CArmor(armorMaterial("merchant_crown", 2, 0.0F, 0.0F, "gold", false), ArmorItem.Type.HELMET, props(1).maxDamage(150)),
			new Traits().at(Traits.Where.WORN).fx(StatusEffects.HERO_OF_THE_VILLAGE, 0));
	public static final Item CURED_JERKY = add("cured_jerky", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(5).saturationModifier(0.6F).snack().build())),
			null);
	public static final Item FISH_AND_CHIPS = add("fish_and_chips", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(12).saturationModifier(0.8F)
					.statusEffect(new StatusEffectInstance(StatusEffects.LUCK, 2400, 0), 1.0F).build())),
			null);
	public static final Item CANDY_APPLE = add("candy_apple", 1, 2,
			new CItem(props(1).maxCount(64).food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.4F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1), 1.0F).alwaysEdible().build())),
			null);
	public static final Item DUST_DEVIL = add("dust_devil", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).airJumps(1));
	public static final Item POGO_STICK = add("pogo_stick", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).bounce().noFall());
	public static final Item STICKY_BOOTS = add("sticky_boots", 1, 2,
			new CArmor(armorMaterial("sticky_boots", 1, 0.0F, 0.0F, "leather", false), ArmorItem.Type.BOOTS, props(1).maxDamage(120)),
			new Traits().at(Traits.Where.WORN).wallClimb());
	public static final Item PUFFER_BALLOON = add("puffer_balloon", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).balloon());
	public static final Item FIREFLY_JAR = add("firefly_jar", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).lantern());
	public static final Item POCKET_MIRROR = add("pocket_mirror", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).reflect(60));
	public static final Item LUNCHBOX = add("lunchbox", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).autoEat());
	public static final Item ALMANAC = add("almanac", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HELD).almanac());
	public static final Item MEADOW_BOOTS = add("meadow_boots", 1, 2,
			new CArmor(armorMaterial("meadow_boots", 1, 0.0F, 0.0F, "leather", false), ArmorItem.Type.BOOTS, props(1).maxDamage(120)),
			new Traits().at(Traits.Where.WORN).meadow());
	public static final Item STRIDER_BOOTS = add("strider_boots", 1, 2,
			new CArmor(armorMaterial("strider_boots", 2, 0.0F, 0.0F, "leather", false), ArmorItem.Type.BOOTS, props(1).maxDamage(160)),
			new Traits().at(Traits.Where.WORN).lavaWalk());
	public static final Item REWIND_WATCH = add("rewind_watch", 2, 3,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.REWIND).cooldown(600).cost(1));
	public static final Item SKELETON_KEY = add("skeleton_key", 1, 2,
			new CItem(props(1).maxDamage(64)),
			new Traits().at(Traits.Where.HELD).unlock());
	public static final Item DISCO_BALL = add("disco_ball", 2, 2,
			new CItem(props(2).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DISCO).range(10).cooldown(600).cost(1));
	public static final Item PIGGY_BANK = add("piggy_bank", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BANK_IN).cooldown(10).sneakUse(Use.BANK_OUT, 10));
	public static final Item PUSH_GLOVE = add("push_glove", 1, 2,
			new CItem(props(1).maxDamage(128)),
			new Traits().at(Traits.Where.HELD).push());
	public static final Item WEATHER_VANE = add("weather_vane", 2, 2,
			new CItem(props(2).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WEATHER).cooldown(200).cost(1));
	public static final Item MAGNIFYING_GLASS = add("magnifying_glass", 1, 2,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SUNBURN).range(6).cooldown(30));
	public static final Item RODEO_SADDLE = add("rodeo_saddle", 1, 2,
			new CItem(props(1).maxDamage(32)),
			new Traits().at(Traits.Where.HELD).mount());
	public static final Item STONE_SOUP = add("stone_soup", 1, 2,
			new CItem(props(1).maxCount(16).food(new FoodComponent.Builder().nutrition(10).saturationModifier(0.6F)
					.statusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 0), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 0), 1.0F).usingConvertsTo(Items.BOWL).build())),
			null);
	public static final Item CACTUS_JUICE = add("cactus_juice", 1, 2,
			new CItem(props(1).maxCount(16).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.3F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 600, 1), 1.0F)
					.statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 160, 0), 1.0F).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build())),
			null);
	public static final Item ARMAGEDDON_CLOCK = add("armageddon_clock", 3, 5,
			new CItem(props(3).maxDamage(8)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ARMAGEDDON).cooldown(200).cost(1).sneakUse(Use.DOOM_CANCEL, 20));
	public static final Item ORBITAL_REMOTE = add("orbital_remote", 3, 3,
			new CItem(props(3).maxDamage(24)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ORBITAL_STRIKE).range(128).cooldown(200).cost(1));
	public static final Item CLUSTER_BOMB = add("cluster_bomb", 2, 2,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CLUSTER_BOMB).cooldown(30).consume());
	public static final Item GRAVITY_GRENADE = add("gravity_grenade", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.GRAVITY_GRENADE).cooldown(30).consume());
	public static final Item SCATTER_BOMB = add("scatter_bomb", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SCATTER_BOMB).cooldown(30).consume());
	public static final Item GOLD_BOMB = add("gold_bomb", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.GOLD_BOMB).cooldown(40).consume());
	public static final Item TERMITE_JAR = add("termite_jar", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TERMITES).range(8).cooldown(40).consume());
	public static final Item DEATH_RAY = add("death_ray", 3, 3,
			new CItem(props(3).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DEATH_RAY).range(64).cooldown(80).cost(1));
	public static final Item TSUNAMI_HORN = add("tsunami_horn", 3, 3,
			new CItem(props(3).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TSUNAMI).range(32).cooldown(100).cost(1));
	public static final Item TORNADO_BOTTLE = add("tornado_bottle", 3, 3,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TORNADO).range(32).cooldown(300).cost(1));
	public static final Item SNAP_GAUNTLET = add("snap_gauntlet", 3, 3,
			new CItem(props(3).maxDamage(6)),
			new Traits().at(Traits.Where.HELD).use(Use.SNAP).range(64).cooldown(600).cost(1));
	public static final Item CREEPER_CANNON = add("creeper_cannon", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CREEPER_CANNON).cooldown(20).cost(1).sneakUse(Use.CHARGED_CREEPER, 60));
	public static final Item HIVE_GRENADE = add("hive_grenade", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HIVE_GRENADE).cooldown(30).consume());
	public static final Item HOT_POTATO = add("hot_potato", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HOT_POTATO).cooldown(10).consume().hotPotato());
	public static final Item FLOOR_IS_LAVA = add("floor_is_lava", 2, 3,
			new CItem(props(2).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FLOOR_IS_LAVA).range(9).cooldown(300).cost(1));
	public static final Item POCKET_VOLCANO = add("pocket_volcano", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.VOLCANO).range(48).cooldown(400).consume());
	public static final Item MJOLNIR = add("mjolnir", 3, 4,
			new CSword(ComboMaterial.of(6000, 1.0F, "netherite", 22), 16, -3.0F, props(3)),
			new Traits().at(Traits.Where.HELD).lightning(10.0f).chain(10.0f, 8.0f).knockUp(0.5f).strikeCooldown(20).use(Use.LIGHTNING_RING).range(10).power(10.0f).cooldown(80).cost(2));
	public static final Item GLASS_CANNON = add("glass_cannon", 2, 2,
			new CSword(ComboMaterial.of(1, 1.0F, "diamond", 18), 60, -2.4F, props(2)),
			null);
	public static final Item RAILGUN = add("railgun", 3, 3,
			new CItem(props(3).maxDamage(128)),
			new Traits().at(Traits.Where.HELD).use(Use.RAILGUN).range(128).power(30.0f).cooldown(60).cost(1));
	public static final Item FLAMETHROWER = add("flamethrower", 2, 2,
			new CItem(props(2).maxDamage(128)),
			new Traits().at(Traits.Where.HELD).use(Use.FLAMETHROWER).cooldown(50).cost(1));
	public static final Item BOOM_BOW = add("boom_bow", 2, 2,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HELD).use(Use.DYNAMITE).power(2.4f).cooldown(16).cost(1).sneakUse(Use.CLUSTER_BOMB, 60));
	public static final Item PIG_MISSILE = add("pig_missile", 2, 3,
			new CItem(props(2).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PIG_MISSILE).cooldown(100).cost(1));
	public static final Item KAIJU_EGG = add("kaiju_egg", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.KAIJU).cooldown(100).consume());
	public static final Item RING_OF_FIRE = add("ring_of_fire", 2, 2,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.RING_OF_FIRE).range(5).cooldown(100).cost(1));
	public static final Item STORM_CROWN = add("storm_crown", 3, 2,
			new CArmor(armorMaterial("storm_crown", 3, 1.0F, 0.0F, "gold", false), ArmorItem.Type.HELMET, props(3).maxDamage(400)),
			new Traits().at(Traits.Where.WORN).stormCrown());
	public static final Item PLAGUE_MASK = add("plague_mask", 2, 3,
			new CArmor(armorMaterial("plague_mask", 1, 0.0F, 0.0F, "leather", false), ArmorItem.Type.HELMET, props(2).maxDamage(200)),
			new Traits().at(Traits.Where.WORN).plagueAura());
	public static final Item ENDLESS_LAVA = add("endless_lava", 2, 2,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HELD).endlessLava());
	public static final Item GENESIS_SEED = add("genesis_seed", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SKY_ISLAND).range(64).cooldown(60).consume());
	public static final Item MAGIC_BEANS = add("magic_beans", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BEANSTALK).range(24).cooldown(60).consume());
	public static final Item CASTLE_BOX = add("castle_box", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CASTLE).range(32).cooldown(60).consume());
	public static final Item PHARAOH_SCARAB = add("pharaoh_scarab", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PYRAMID).range(32).cooldown(60).consume());
	public static final Item TITAN_SPADE = add("titan_spade", 3, 3,
			new CItem(props(3).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MOUNTAIN).range(48).cooldown(100).cost(1).sneakUse(Use.CRATER, 60));
	public static final Item BIFROST_STAFF = add("bifrost_staff", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.RAINBOW_BRIDGE).range(64).cooldown(40).cost(1));
	public static final Item PORTAL_GUN = add("portal_gun", 3, 3,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.NETHER_PORTAL).range(32).cooldown(100).cost(1).sneakUse(Use.END_PORTAL, 200));
	public static final Item MITOSIS_RAY = add("mitosis_ray", 3, 3,
			new CItem(props(3).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MITOSIS).range(32).cooldown(10).cost(1).sneakUse(Use.MITOSIS_BURST, 100));
	public static final Item MENAGERIE_CANNON = add("menagerie_cannon", 2, 3,
			new CItem(props(2).maxDamage(256)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MENAGERIE).cooldown(8).cost(1));
	public static final Item MOSES_STAFF = add("moses_staff", 3, 3,
			new CItem(props(3).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PART_SEA).range(48).cooldown(200).cost(1));
	public static final Item SNOWMAN_HORN = add("snowman_horn", 1, 2,
			new CItem(props(1).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SNOW_ARMY).power(8.0f).cooldown(400).cost(1));
	public static final Item FORTRESS_STAFF = add("fortress_staff", 1, 3,
			new CItem(props(1).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.STONE_WALL).cooldown(20).cost(1).sneakUse(Use.STONE_DOME, 100));
	public static final Item FARM_BOX = add("farm_box", 1, 3,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FARM).range(24).cooldown(20).consume());
	public static final Item WILD_STAFF = add("wild_staff", 3, 3,
			new CItem(props(3).maxDamage(100)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WILD_MAGIC).range(48).cooldown(40).cost(1));
	public static final Item DICE_OF_FATE = add("dice_of_fate", 2, 3,
			new CItem(props(2).maxDamage(20)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DICE).cooldown(100).cost(1));
	public static final Item MUSICAL_CHAIRS = add("musical_chairs", 2, 3,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MUSICAL_CHAIRS).range(32).cooldown(100).cost(1));
	public static final Item PARTY_CANNON = add("party_cannon", 2, 2,
			new CItem(props(2).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PARTY).cooldown(30).cost(1).sneakUse(Use.CAKE_RAIN, 200));
	public static final Item GREMLIN_JAR = add("gremlin_jar", 2, 3,
			new CItem(props(2).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).gremlin());
	public static final Item FORCE_FIELD = add("force_field", 2, 3,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FORCE_FIELD).range(5).cooldown(400).cost(1));
	public static final Item UPSIDE_DOWN_CAKE = add("upside_down_cake", 2, 3,
			new CItem(props(2).maxCount(16).food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.6F)
					.statusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 900, 0), 1.0F).alwaysEdible().build())),
			new Traits().at(Traits.Where.HELD).eatBuff(6).attr("generic.gravity", -1.5, 2));
	public static final Item GLACIER_STAFF = add("glacier_staff", 2, 3,
			new CItem(props(2).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ICE_SPIKES).range(24).cooldown(40).cost(1));
	public static final Item SHEEP_BOMB = add("sheep_bomb", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SHEEP_BOMB).cooldown(30).consume());
	public static final Item COBWEB_GRENADE = add("cobweb_grenade", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.COBWEB_BOMB).cooldown(30).consume());
	public static final Item BEDROCK_BREAKER = add("bedrock_breaker", 3, 3,
			new CPick(ComboMaterial.of(2000, 10.0F, "netherite", 22), 6.0F, -2.8F, props(3)),
			new Traits().at(Traits.Where.HELD).bedrockBreak());
	public static final Item FANG_STAFF = add("fang_staff", 2, 3,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FANGS).range(16).cooldown(30).cost(1).sneakUse(Use.FANG_RING, 60));
	public static final Item DRAGON_STAFF = add("dragon_staff", 3, 2,
			new CItem(props(3).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DRAGON_FIREBALL).cooldown(30).cost(1));
	public static final Item POSEIDON_WRATH = add("poseidon_wrath", 3, 2,
			new CItem(props(3).maxDamage(64)),
			new Traits().at(Traits.Where.HELD).use(Use.TRIDENT_STORM).range(64).cooldown(100).cost(1));
	public static final Item HORDE_HORN = add("horde_horn", 2, 3,
			new CItem(props(2).maxDamage(8)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HORDE).cooldown(200).cost(1));
	public static final Item PAINT_BOMB = add("paint_bomb", 1, 2,
			new CItem(props(1).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PAINT_BOMB).cooldown(30).consume());
	public static final Item WORLD_TREE_SEED = add("world_tree_seed", 2, 3,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WORLD_TREE).range(32).cooldown(60).consume());
	public static final Item COPY_WAND = add("copy_wand", 3, 3,
			new CItem(props(3).maxDamage(200)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.COPY_PASTE).range(48).cooldown(20).cost(1).sneakUse(Use.COPY_CORNER, 5));
	public static final Item SHULKER_BLASTER = add("shulker_blaster", 2, 2,
			new CItem(props(2).maxDamage(200)),
			new Traits().at(Traits.Where.HELD).use(Use.SHULKER_BULLETS).range(24).cooldown(30).cost(1));
	public static final Item HOURGLASS = add("hourglass", 3, 3,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.HOURGLASS).power(10.0f).cooldown(600).cost(1));
	public static final Item MONSTER_MAGNET = add("monster_magnet", 2, 2,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MONSTER_MAGNET).range(32).cooldown(100).cost(1));
	public static final Item CHUNK_ERASER = add("chunk_eraser", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CHUNK_ERASE).range(128).cooldown(100).consume());
	public static final Item REGION_ERASER = add("region_eraser", 3, 3,
			new CItem(props(3).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.REGION_ERASE).range(128).cooldown(200).consume());
	public static final Item CHUNK_INVERTER = add("chunk_inverter", 3, 3,
			new CItem(props(3).maxDamage(8)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CHUNK_INVERT).range(128).cooldown(200).cost(1));
	public static final Item CHUNK_LAUNCHER = add("chunk_launcher", 3, 3,
			new CItem(props(3).maxDamage(8)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CHUNK_LAUNCH).range(128).cooldown(200).cost(1));
	public static final Item CARPET_BOMBER = add("carpet_bomber", 3, 3,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.CARPET_BOMB).range(128).cooldown(200).cost(1));
	public static final Item ORBITAL_ANNIHILATOR = add("orbital_annihilator", 3, 3,
			new CItem(props(3).maxDamage(8)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ANNIHILATE).range(160).cooldown(400).cost(1));
	public static final Item TSAR_BOMBA = add("tsar_bomba", 3, 3,
			new CItem(props(3).maxCount(4)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.TSAR_BOMBA).range(160).cooldown(400).consume());
	public static final Item EVENT_HORIZON = add("event_horizon", 3, 3,
			new CItem(props(3).maxCount(4)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.EVENT_HORIZON).range(48).cooldown(400).consume());
	public static final Item FAULT_LINE = add("fault_line", 3, 3,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.FAULT_LINE).power(160.0f).cooldown(200).cost(1));
	public static final Item POCKET_CUBE = add("pocket_cube", 3, 4,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.POCKET_DIMENSION).cooldown(40).sneakUse(Use.POCKET_GROUP, 40));
	public static final Item NOCLIP_PEARL = add("noclip_pearl", 3, 7,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BACKROOMS).cooldown(40).sneakUse(Use.NOCLIP_THROW, 40));
	public static final Item CLOUD_KEY = add("cloud_key", 3, 4,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SKY_REALM).cooldown(40));
	public static final Item MOON_ROCKET = add("moon_rocket", 3, 5,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.MOON).cooldown(60));
	public static final Item LOOKING_GLASS = add("looking_glass", 3, 5,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PARALLEL).cooldown(40));
	public static final Item DIMENSION_HOPPER = add("dimension_hopper", 3, 4,
			new CItem(props(3).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.DIMENSION_HOP).cooldown(40).cost(1));
	public static final Item BANISHING_WAND = add("banishing_wand", 2, 3,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BANISH).range(24).cooldown(20).cost(1));
	public static final Item WORMHOLE_GUN = add("wormhole_gun", 3, 4,
			new CItem(props(3).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WORMHOLE_BLUE).range(64).cooldown(5).sneakUse(Use.WORMHOLE_ORANGE, 5));
	public static final Item GRAVE_COMPASS = add("grave_compass", 2, 2,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.GRAVE_WARP).cooldown(200).cost(1));
	public static final Item ESCAPE_ROPE = add("escape_rope", 1, 2,
			new CItem(props(1).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ESCAPE).cooldown(40).cost(1));
	public static final Item ELEVATOR_PEARL = add("elevator_pearl", 2, 3,
			new CItem(props(2).maxDamage(256)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ELEVATOR_UP).range(64).cooldown(10).cost(1).sneakUse(Use.ELEVATOR_DOWN, 10));
	public static final Item FRIENDSHIP_BRACELET = add("friendship_bracelet", 1, 3,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BRACELET_GO).cooldown(100).sneakUse(Use.BRACELET_PULL, 100));
	public static final Item WANDERLUST_ATLAS = add("wanderlust_atlas", 2, 3,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.WANDER).cooldown(200).cost(1));
	public static final Item PET_WHISTLE = add("pet_whistle", 1, 3,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.PET_RECALL).cooldown(100));
	public static final Item BLUEPRINT = add("blueprint", 3, 4,
			new CItem(props(3).maxDamage(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BLUEPRINT).range(48).cooldown(100).cost(1).sneakUse(Use.BLUEPRINT_PICK, 5));
	public static final Item BIOME_BRUSH = add("biome_brush", 2, 4,
			new CItem(props(2).maxDamage(128)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.BIOME_PAINT).range(48).cooldown(20).cost(1).sneakUse(Use.BIOME_PICK, 5));
	public static final Item BUILDER_WAND = add("builder_wand", 1, 4,
			new CItem(props(1).maxDamage(1024)),
			new Traits().at(Traits.Where.HOTBAR).builderWand());
	public static final Item RAIL_WAND = add("rail_wand", 2, 4,
			new CItem(props(2).maxDamage(32)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.RAIL_LINE).power(128.0f).cooldown(100).cost(1));
	public static final Item QUARRY_BOX = add("quarry_box", 2, 4,
			new CItem(props(2).maxCount(16)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.QUARRY).range(16).power(5.0f).cooldown(200).consume());
	public static final Item SORTING_WAND = add("sorting_wand", 1, 4,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.SORT_SELF).sortWand().cooldown(5));
	public static final Item ENDER_MAIL = add("ender_mail", 1, 4,
			new CItem(props(1).maxCount(1)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.ENDER_MAIL).cooldown(20));
	public static final Item LUMEN_ORB = add("lumen_orb", 2, 3,
			new CItem(props(2).maxDamage(64)),
			new Traits().at(Traits.Where.HOTBAR).use(Use.LIGHT_UP).range(24).cooldown(60).cost(1));
	public static final Item ALMOND_WATER = add("almond_water", 1, 3,
			new CItem(props(1).maxCount(16).food(new FoodComponent.Builder().nutrition(2).saturationModifier(0.5F)
					.statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1), 1.0F).alwaysEdible().usingConvertsTo(Items.GLASS_BOTTLE).build())),
			new Traits().at(Traits.Where.HELD).cleanse());

	private ComboItems() {
	}

	/** Loads this class, which registers all items. */
	public static void init() {
	}

	/** The abilities of an item, or null if it is not a combined item or has no abilities. */
	public static Traits traits(Item item) {
		return TRAITS.get(item);
	}

	/** How many tooltip lines an item has (lang keys item.combinator.ID.tip1, tip2, ...). */
	public static int tipLines(Item item) {
		Integer lines = TIP_LINES.get(item);
		return lines == null ? 0 : lines;
	}

	/** 1, 2 or 3 for combined items (3 = the wildest), 0 for everything else. */
	public static int rarity(Item item) {
		Integer rarity = RARITIES.get(item);
		return rarity == null ? 0 : rarity;
	}

	private static Item add(String id, int rarity, int tipLines, Item item, Traits traits) {
		Registry.register(Registries.ITEM, ItemCombinator.id(id), item);
		ALL.add(item);
		TIP_LINES.put(item, tipLines);
		RARITIES.put(item, rarity);
		if (traits != null) {
			TRAITS.put(item, traits);
		}
		return item;
	}

	/** Rarity 1 = yellow name, 2 = aqua, 3 = purple, glowing and fireproof. */
	private static Item.Settings props(int rarity) {
		Item.Settings settings = new Item.Settings().rarity(rarity <= 1 ? Rarity.UNCOMMON : rarity == 2 ? Rarity.RARE : Rarity.EPIC);
		if (rarity >= 3) {
			settings = settings.fireproof().component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return settings;
	}

	/** layer is the vanilla armor look that is shown when the piece is worn: "iron", "diamond" or "netherite". */
	private static RegistryEntry<ArmorMaterial> armorMaterial(String id, int protection, float toughness, float knockbackResistance, String layer, boolean wings) {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			defense.put(type, protection);
		}
		ArmorMaterial material = new ArmorMaterial(
				defense,
				15,
				wings ? SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA : SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
				() -> Ingredient.ofItems(Items.DIAMOND),
				List.of(new ArmorMaterial.Layer(Identifier.ofVanilla(layer))),
				toughness,
				knockbackResistance);
		return Registry.registerReference(Registries.ARMOR_MATERIAL, ItemCombinator.id(id), material);
	}
}
