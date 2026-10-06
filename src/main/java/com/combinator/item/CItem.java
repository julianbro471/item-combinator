package com.combinator.item;

import com.combinator.ability.UseAbilities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A combined item that is not a tool or armor: wands, staffs, charms, food. */
public class CItem extends Item {
	public CItem(Item.Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ActionResult result = UseAbilities.useOnBlock(context);
		return result != ActionResult.PASS ? result : super.useOnBlock(context);
	}

	/** Called by the game when the item was eaten. Foods with special abilities trigger them here. */
	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		Item eaten = stack.getItem();
		ItemStack result = super.finishUsing(stack, world, user);
		UseAbilities.onEaten(eaten, world, user);
		return result;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		TypedActionResult<ItemStack> result = UseAbilities.use(world, user, hand);
		return result.getResult() != ActionResult.PASS ? result : super.use(world, user, hand);
	}
}
