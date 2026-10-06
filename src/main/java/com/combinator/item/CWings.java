package com.combinator.item;

import net.fabricmc.fabric.api.entity.event.v1.FabricElytraItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;

/** Chest armor that also works as an elytra. FabricElytraItem makes the game let the wearer glide. */
public class CWings extends ArmorItem implements FabricElytraItem {
	public CWings(RegistryEntry<ArmorMaterial> material, Item.Settings settings) {
		super(material, ArmorItem.Type.CHESTPLATE, settings);
	}
}
