package com.combinator.item;

import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * Tool material of one combined tool. Attack damage is always 0 here because every tool
 * sets its own damage directly (see the C* item classes).
 */
public record ComboMaterial(int durability, float speed, TagKey<Block> inverseTag, int enchantability) implements ToolMaterial {
	/** level is "wooden", "stone", "iron", "diamond" or "netherite": what the tool is able to mine. */
	public static ComboMaterial of(int durability, float speed, String level, int enchantability) {
		TagKey<Block> tag = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("incorrect_for_" + level + "_tool"));
		return new ComboMaterial(durability, speed, tag, enchantability);
	}

	@Override
	public int getDurability() {
		return this.durability;
	}

	@Override
	public float getMiningSpeedMultiplier() {
		return this.speed;
	}

	@Override
	public float getAttackDamage() {
		return 0.0F;
	}

	@Override
	public TagKey<Block> getInverseTag() {
		return this.inverseTag;
	}

	@Override
	public int getEnchantability() {
		return this.enchantability;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.ofItems(Items.DIAMOND);
	}
}
