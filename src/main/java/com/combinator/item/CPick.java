package com.combinator.item;

import com.combinator.ability.UseAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A combined pickaxe. */
public class CPick extends PickaxeItem {
	public CPick(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
		super(material, settings.attributeModifiers(
				MiningToolItem.createAttributeModifiers(material, attackDamage - 1.0F, attackSpeed)));
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ActionResult result = UseAbilities.useOnBlock(context);
		return result != ActionResult.PASS ? result : super.useOnBlock(context);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		TypedActionResult<ItemStack> result = UseAbilities.use(world, user, hand);
		return result.getResult() != ActionResult.PASS ? result : super.use(world, user, hand);
	}
}
