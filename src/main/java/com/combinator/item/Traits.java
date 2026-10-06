package com.combinator.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * The special abilities of one combined item. Every combined item has one Traits object.
 * The ability code (package com.combinator.ability) only reads these fields.
 */
public final class Traits {
	/** A status effect with a duration in ticks (20 ticks = 1 second) and an amplifier (0 = level I). */
	public record Fx(RegistryEntry<StatusEffect> effect, int ticks, int amplifier) {
	}

	/**
	 * One change to an attribute of the player.
	 * id is the game's attribute id without "minecraft:", for example "generic.scale".
	 * op: 0 = add the value, 1 = add value x base, 2 = multiply the total by (1 + value).
	 */
	public record Attr(String id, double value, int op) {
	}

	/** Where an item must be for its passive abilities to work. */
	public enum Where {
		/** In the main hand or off hand. */
		HELD,
		/** Anywhere in the hotbar, or in the off hand. */
		HOTBAR,
		/** Worn as armor. */
		WORN
	}

	// --- mining ---
	public int areaRadius;
	public boolean areaCube;
	public int veinLimit;
	public int treeLimit;
	public boolean smelt;
	public boolean magnetDrops;
	public boolean doubleOres;
	/** Explosion power when a block is mined, 0 = none. */
	public float mineBoom;
	/** Blocks drop themselves, like with the Silk Touch enchantment. */
	public boolean silk;
	/** Dirt, sand and gravel sometimes hide small finds. */
	public boolean sift;
	/** Chance that a mined stone block also drops gold nuggets, 0 = never. */
	public float stoneNuggets;

	// --- right-click on a block ---
	public boolean torch;
	public int harvestRadius;
	public int bonemealRadius;
	/** Turns blocks and mobs into gold. */
	public boolean midas;
	/** Upgrades a block to a more valuable one. */
	public boolean transmute;
	/** Builds a house where it is used. */
	public boolean house;
	/** Tills a 3x3 patch and plants the seeds the player carries. */
	public boolean sow;
	/** Hangs ladders down a wall. */
	public boolean ropeLadder;
	/** Places water and never runs dry. */
	public boolean endlessWater;
	/** Catches a mob with a right-click and lets it out again on a block. */
	public boolean capture;

	// --- combat ---
	public int igniteSeconds;
	public final List<Fx> hitEffects = new ArrayList<>();
	public boolean freeze;
	public float lifesteal;
	public float lightningDamage;
	public float blastRadius;
	public float blastDamage;
	public float sweepRadius;
	public float sweepFraction;
	public float knockUp;
	public float knockback;
	public float magicDamage;
	public float headChance;
	public int nuggets;
	public int bonusXp;
	public int strikeCooldown;
	public boolean instakill;
	public float chainRadius;
	public float chainDamage;
	/** Power of a real explosion on hit, 0 = none. */
	public float boomHit;
	/** The target keeps losing health for this many seconds. */
	public int bleedSeconds;
	/** Killed mobs drop their loot twice. */
	public boolean doubleLoot;

	// --- right-click in the air ---
	public Use use = Use.NONE;
	public Use sneakUse = Use.NONE;
	public int range = 16;
	public float power = 1.0F;
	public int cooldown;
	public int sneakCooldown;
	public int cost;
	public boolean consume;

	// --- passive ---
	public Where where = Where.HOTBAR;
	public final List<Fx> passive = new ArrayList<>();
	public boolean noFall;
	public boolean mend;
	public boolean flight;
	public boolean boost;
	public int pullRadius;
	public int totemCooldown;
	public float thornsDamage;
	public int thornsFire;
	public boolean hasThorns;
	/** Attackers get an explosion in the face (no block damage), 0 = none. */
	public float thornsBoom;
	/** Phantoms never come for this player. */
	public boolean noPhantoms;

	// --- movement tricks and odd charms ---
	/** How many extra jumps the player has in the air. */
	public int airJumps;
	/** Pogo stick: every landing throws the player up again. */
	public boolean bounce;
	/** The player climbs walls by walking against them. */
	public boolean wallClimb;
	/** The player floats up while holding the item. */
	public boolean balloon;
	/** A light follows the player. */
	public boolean lantern;
	/** A shot that hits the player hits the shooter instead. The number is the pause between two such shots, in ticks. */
	public int reflectCooldown;
	/** Flowers grow where the player walks, crops nearby grow faster. */
	public boolean meadow;
	/** Lava hardens under the player's feet for a moment. */
	public boolean lavaWalk;
	/** Right-click a mob to ride it. */
	public boolean mount;
	/** Eats food from the inventory when the player is hungry. */
	public boolean autoEat;
	/** Shows position, biome and time while held. */
	public boolean almanac;
	/** Right-click on a door or trapdoor opens it, also an iron one. */
	public boolean unlock;
	/** Right-click on a block pushes it away, sneaking pulls it. */
	public boolean push;
	/** Explodes in the inventory of whoever carries it for too long. */
	public boolean hotPotato;
	/** While worn, lightning strikes monsters nearby. */
	public boolean stormCrown;
	/** While worn, everything alive nearby gets poisoned and withers. */
	public boolean plagueAura;
	/** Places lava and never runs dry. */
	public boolean endlessLava;
	/** A gremlin plays pranks on the player now and then. */
	public boolean gremlin;
	/** Changes to the player's body: size, health, reach and so on. */
	public final List<Attr> attrs = new ArrayList<>();

	// --- food ---
	/** After eating, the attribute changes above last this many seconds. 0 = not a buff food. */
	public int eatBuffSeconds;
	/** After eating, the player is thrown upward with this speed. */
	public float eatLaunch;

	public Traits area(int radius, boolean cube) {
		this.areaRadius = radius;
		this.areaCube = cube;
		return this;
	}

	public Traits vein(int limit) {
		this.veinLimit = limit;
		return this;
	}

	public Traits tree(int limit) {
		this.treeLimit = limit;
		return this;
	}

	public Traits smelt() {
		this.smelt = true;
		return this;
	}

	public Traits magnetDrops() {
		this.magnetDrops = true;
		return this;
	}

	public Traits silk() {
		this.silk = true;
		return this;
	}

	public Traits sift() {
		this.sift = true;
		return this;
	}

	public Traits stoneNuggets(float chance) {
		this.stoneNuggets = chance;
		return this;
	}

	public Traits sow() {
		this.sow = true;
		return this;
	}

	public Traits ropeLadder() {
		this.ropeLadder = true;
		return this;
	}

	public Traits endlessWater() {
		this.endlessWater = true;
		return this;
	}

	public Traits capture() {
		this.capture = true;
		return this;
	}

	public Traits bleed(int seconds) {
		this.bleedSeconds = seconds;
		return this;
	}

	public Traits doubleLoot() {
		this.doubleLoot = true;
		return this;
	}

	public Traits noPhantoms() {
		this.noPhantoms = true;
		return this;
	}

	public Traits airJumps(int jumps) {
		this.airJumps = jumps;
		return this;
	}

	public Traits bounce() {
		this.bounce = true;
		return this;
	}

	public Traits wallClimb() {
		this.wallClimb = true;
		return this;
	}

	public Traits balloon() {
		this.balloon = true;
		return this;
	}

	public Traits lantern() {
		this.lantern = true;
		return this;
	}

	public Traits reflect(int cooldownTicks) {
		this.reflectCooldown = cooldownTicks;
		return this;
	}

	public Traits meadow() {
		this.meadow = true;
		return this;
	}

	public Traits lavaWalk() {
		this.lavaWalk = true;
		return this;
	}

	public Traits mount() {
		this.mount = true;
		return this;
	}

	public Traits autoEat() {
		this.autoEat = true;
		return this;
	}

	public Traits almanac() {
		this.almanac = true;
		return this;
	}

	public Traits unlock() {
		this.unlock = true;
		return this;
	}

	public Traits hotPotato() {
		this.hotPotato = true;
		return this;
	}

	public Traits stormCrown() {
		this.stormCrown = true;
		return this;
	}

	public Traits plagueAura() {
		this.plagueAura = true;
		return this;
	}

	public Traits endlessLava() {
		this.endlessLava = true;
		return this;
	}

	public Traits gremlin() {
		this.gremlin = true;
		return this;
	}

	public Traits push() {
		this.push = true;
		return this;
	}

	public Traits doubleOres() {
		this.doubleOres = true;
		return this;
	}

	public Traits torch() {
		this.torch = true;
		return this;
	}

	public Traits harvest(int radius) {
		this.harvestRadius = radius;
		return this;
	}

	public Traits bonemeal(int radius) {
		this.bonemealRadius = radius;
		return this;
	}

	public Traits ignite(int seconds) {
		this.igniteSeconds = seconds;
		return this;
	}

	public Traits hit(RegistryEntry<StatusEffect> effect, int ticks, int amplifier) {
		this.hitEffects.add(new Fx(effect, ticks, amplifier));
		return this;
	}

	public Traits freeze() {
		this.freeze = true;
		return this;
	}

	public Traits lifesteal(float fraction) {
		this.lifesteal = fraction;
		return this;
	}

	public Traits lightning(float damage) {
		this.lightningDamage = damage;
		return this;
	}

	public Traits blast(float radius, float damage) {
		this.blastRadius = radius;
		this.blastDamage = damage;
		return this;
	}

	public Traits sweep(float radius, float fraction) {
		this.sweepRadius = radius;
		this.sweepFraction = fraction;
		return this;
	}

	public Traits knockUp(float strength) {
		this.knockUp = strength;
		return this;
	}

	public Traits knockback(float strength) {
		this.knockback = strength;
		return this;
	}

	public Traits magic(float damage) {
		this.magicDamage = damage;
		return this;
	}

	public Traits heads(float chance) {
		this.headChance = chance;
		return this;
	}

	public Traits nuggets(int max) {
		this.nuggets = max;
		return this;
	}

	public Traits bonusXp(int amount) {
		this.bonusXp = amount;
		return this;
	}

	public Traits strikeCooldown(int ticks) {
		this.strikeCooldown = ticks;
		return this;
	}

	public Traits use(Use use) {
		this.use = use;
		return this;
	}

	public Traits sneakUse(Use use, int cooldownTicks) {
		this.sneakUse = use;
		this.sneakCooldown = cooldownTicks;
		return this;
	}

	public Traits range(int blocks) {
		this.range = blocks;
		return this;
	}

	public Traits power(float power) {
		this.power = power;
		return this;
	}

	public Traits cooldown(int ticks) {
		this.cooldown = ticks;
		return this;
	}

	public Traits cost(int durability) {
		this.cost = durability;
		return this;
	}

	public Traits consume() {
		this.consume = true;
		return this;
	}

	public Traits at(Where where) {
		this.where = where;
		return this;
	}

	public Traits fx(RegistryEntry<StatusEffect> effect, int amplifier) {
		this.passive.add(new Fx(effect, 0, amplifier));
		return this;
	}

	public Traits noFall() {
		this.noFall = true;
		return this;
	}

	public Traits mend() {
		this.mend = true;
		return this;
	}

	public Traits flight() {
		this.flight = true;
		return this;
	}

	public Traits boost() {
		this.boost = true;
		return this;
	}

	public Traits pull(int radius) {
		this.pullRadius = radius;
		return this;
	}

	public Traits totem(int cooldownTicks) {
		this.totemCooldown = cooldownTicks;
		return this;
	}

	public Traits thorns(float damage, int fireSeconds) {
		this.hasThorns = true;
		this.thornsDamage = damage;
		this.thornsFire = fireSeconds;
		return this;
	}

	public Traits mineBoom(float power) {
		this.mineBoom = power;
		return this;
	}

	public Traits midas() {
		this.midas = true;
		return this;
	}

	public Traits transmute() {
		this.transmute = true;
		return this;
	}

	public Traits house() {
		this.house = true;
		return this;
	}

	public Traits instakill() {
		this.instakill = true;
		return this;
	}

	public Traits chain(float radius, float damage) {
		this.chainRadius = radius;
		this.chainDamage = damage;
		return this;
	}

	public Traits boomHit(float power) {
		this.boomHit = power;
		return this;
	}

	public Traits thornsBoom(float power) {
		this.hasThorns = true;
		this.thornsBoom = power;
		return this;
	}

	public Traits attr(String id, double value, int op) {
		this.attrs.add(new Attr(id, value, op));
		return this;
	}

	public Traits eatBuff(int seconds) {
		this.eatBuffSeconds = seconds;
		return this;
	}

	public Traits eatLaunch(float speed) {
		this.eatLaunch = speed;
		return this;
	}

	public boolean hasMining() {
		return this.areaRadius > 0 || this.veinLimit > 0 || this.treeLimit > 0 || this.smelt || this.magnetDrops || this.doubleOres || this.mineBoom > 0
				|| this.silk || this.sift || this.stoneNuggets > 0;
	}

	public boolean hasCombat() {
		return this.igniteSeconds > 0 || !this.hitEffects.isEmpty() || this.freeze || this.lifesteal > 0 || this.lightningDamage > 0
				|| this.blastRadius > 0 || this.sweepRadius > 0 || this.knockUp > 0 || this.knockback > 0 || this.magicDamage > 0
				|| this.instakill || this.midas || this.chainRadius > 0 || this.boomHit > 0 || this.bleedSeconds > 0;
	}

	public boolean hasKillBonus() {
		return this.headChance > 0 || this.nuggets > 0 || this.bonusXp > 0 || this.lifesteal > 0 || this.doubleLoot;
	}
}
