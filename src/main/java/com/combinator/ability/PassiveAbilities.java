package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Abilities that work without clicking: effects from charms and armor, body changes (size, health, reach),
 * the item magnet, slow repair, and creative-style flight.
 */
public final class PassiveAbilities {
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final EntityAttributeModifier.Operation[] OPERATIONS = {
			EntityAttributeModifier.Operation.ADD_VALUE,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE,
			EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
	};

	/** Players that currently fly because of this mod (so flight is only taken away from them). */
	private static final Set<UUID> FLYERS = new HashSet<>();
	/** Timed body changes from food: player, then item, then the server tick at which it ends. */
	private static final Map<UUID, Map<Item, Integer>> BUFFS = new HashMap<>();
	/** Every item that changes attributes, found once on first use. */
	private static List<Item> attributeItems;

	private PassiveAbilities() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(PassiveAbilities::tick);
	}

	/** Calls the visitor for every combined item of the player that is in the right place to be active. */
	public interface Visitor {
		/** Return true to stop looking. */
		boolean visit(ItemStack stack, Traits traits);
	}

	public static void forEachActive(PlayerEntity player, Visitor visitor) {
		if (check(player.getMainHandStack(), true, true, false, visitor)) {
			return;
		}
		if (check(player.getOffHandStack(), true, true, false, visitor)) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (i == inventory.selectedSlot) {
				continue; // already handled as the main hand
			}
			if (check(inventory.getStack(i), false, true, false, visitor)) {
				return;
			}
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (check(player.getEquippedStack(slot), false, false, true, visitor)) {
				return;
			}
		}
	}

	private static boolean check(ItemStack stack, boolean held, boolean hotbar, boolean worn, Visitor visitor) {
		if (stack.isEmpty()) {
			return false;
		}
		Traits traits = ComboItems.traits(stack.getItem());
		if (traits == null) {
			return false;
		}
		boolean active = switch (traits.where) {
			case HELD -> held;
			case HOTBAR -> hotbar;
			case WORN -> worn;
		};
		return active && visitor.visit(stack, traits);
	}

	/** True if the player has an active combined item that matches the test. */
	public static boolean has(PlayerEntity player, Predicate<Traits> test) {
		return find(player, test) != null;
	}

	/** The first active combined item that matches the test, or null. */
	public static ItemStack find(PlayerEntity player, Predicate<Traits> test) {
		ItemStack[] found = new ItemStack[1];
		forEachActive(player, (stack, traits) -> {
			if (test.test(traits)) {
				found[0] = stack;
				return true;
			}
			return false;
		});
		return found[0];
	}

	/** Starts (or restarts) the timed body change of a food item. */
	public static void grantBuff(ServerPlayerEntity player, Item item, int seconds) {
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		BUFFS.computeIfAbsent(player.getUuid(), k -> new HashMap<>()).put(item, server.getTicks() + seconds * 20);
		applyAttributes(player, server.getTicks());
	}

	private static void tick(MinecraftServer server) {
		int ticks = server.getTicks();
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			try {
				tickPlayer(player, ticks);
			} catch (Throwable t) {
				ItemCombinator.error("Passive ability failed", t);
			}
		}
	}

	private static void tickPlayer(ServerPlayerEntity player, int ticks) {
		Spells.ghostSafety(player);
		if (player.isSpectator() || !player.isAlive()) {
			return;
		}
		tickFlight(player);
		if (ticks % 10 == 0) {
			applyAttributes(player, ticks);
		}

		boolean effectsNow = ticks % 20 == 0;
		boolean pullNow = ticks % 5 == 0;
		boolean mendNow = ticks % 40 == 0;
		if (!effectsNow && !pullNow && !mendNow) {
			return;
		}

		int[] pull = {0};
		boolean[] mend = {false};
		boolean[] rested = {false};
		forEachActive(player, (stack, traits) -> {
			if (effectsNow) {
				for (Traits.Fx fx : traits.passive) {
					// Night vision flickers when it is about to run out, so it gets a longer time.
					int duration = fx.effect() == StatusEffects.NIGHT_VISION ? 320 : 100;
					player.addStatusEffect(new StatusEffectInstance(fx.effect(), duration, fx.amplifier(), true, false, true));
				}
			}
			pull[0] = Math.max(pull[0], traits.pullRadius);
			mend[0] |= traits.mend;
			rested[0] |= traits.noPhantoms;
			return false;
		});

		if (effectsNow && rested[0]) {
			// Phantoms come for players who have not slept for three days. This makes the game think the player just slept.
			player.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
		}

		if (pullNow && pull[0] > 0) {
			pullItems(player, pull[0]);
		}
		if (mendNow && mend[0]) {
			PlayerInventory inventory = player.getInventory();
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (!stack.isEmpty() && stack.isDamaged()) {
					stack.setDamage(stack.getDamage() - 1);
				}
			}
		}
	}

	/**
	 * Adds or removes attribute changes (size, health, reach ...) so they match the items that are active right now.
	 * The changes are "temporary modifiers": the game forgets them when the player logs out, and this method
	 * puts them back, so nothing can get stuck.
	 */
	private static void applyAttributes(ServerPlayerEntity player, int ticks) {
		if (attributeItems == null) {
			attributeItems = new ArrayList<>();
			for (Item item : ComboItems.ALL) {
				Traits traits = ComboItems.traits(item);
				if (traits != null && !traits.attrs.isEmpty()) {
					attributeItems.add(item);
				}
			}
		}
		Set<Item> active = new HashSet<>();
		forEachActive(player, (stack, traits) -> {
			if (!traits.attrs.isEmpty() && traits.eatBuffSeconds == 0) {
				active.add(stack.getItem());
			}
			return false;
		});
		Map<Item, Integer> buffs = BUFFS.get(player.getUuid());
		if (buffs != null) {
			for (Iterator<Map.Entry<Item, Integer>> it = buffs.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<Item, Integer> entry = it.next();
				if (entry.getValue() > ticks) {
					active.add(entry.getKey());
				} else {
					it.remove();
				}
			}
		}

		for (Item item : attributeItems) {
			Traits traits = ComboItems.traits(item);
			boolean wanted = active.contains(item);
			String path = Registries.ITEM.getId(item).getPath();
			for (int i = 0; i < traits.attrs.size(); i++) {
				Traits.Attr attr = traits.attrs.get(i);
				RegistryEntry<EntityAttribute> attribute = Fx.attribute(attr.id());
				if (attribute == null) {
					continue;
				}
				EntityAttributeInstance instance = player.getAttributeInstance(attribute);
				if (instance == null) {
					continue;
				}
				Identifier id = Identifier.of(ItemCombinator.MOD_ID, path + "_" + i);
				boolean present = instance.hasModifier(id);
				if (wanted && !present) {
					instance.addTemporaryModifier(new EntityAttributeModifier(id, attr.value(), OPERATIONS[attr.op()]));
				} else if (!wanted && present) {
					instance.removeModifier(id);
				}
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void pullItems(ServerPlayerEntity player, int radius) {
		ServerWorld world = player.getServerWorld();
		Box box = player.getBoundingBox().expand(radius);
		double x = player.getX();
		double y = player.getY() + 0.2;
		double z = player.getZ();
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, box, e -> e.isAlive() && !e.cannotPickup())) {
			item.setPosition(x, y, z);
			item.setVelocity(Vec3d.ZERO);
		}
		for (ExperienceOrbEntity orb : world.getEntitiesByClass(ExperienceOrbEntity.class, box, e -> e.isAlive())) {
			orb.setPosition(x, y, z);
		}
	}

	/** Gives or removes creative-style flight depending on whether a flight item is active. */
	private static void tickFlight(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		if (player.isCreative()) {
			FLYERS.remove(id);
			return;
		}
		boolean shouldFly = has(player, traits -> traits.flight);
		if (shouldFly) {
			FLYERS.add(id);
			if (!player.getAbilities().allowFlying) {
				player.getAbilities().allowFlying = true;
				player.sendAbilitiesUpdate();
			}
		} else if (FLYERS.remove(id)) {
			player.getAbilities().allowFlying = false;
			player.getAbilities().flying = false;
			player.sendAbilitiesUpdate();
		}
	}
}
