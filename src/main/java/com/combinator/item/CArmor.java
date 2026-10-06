package com.combinator.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;

/** A combined armor piece. Its effects are handled in PassiveAbilities and CombatAbilities. */
public class CArmor extends ArmorItem {
	public CArmor(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Item.Settings settings) {
		super(material, type, settings);
	}
}
