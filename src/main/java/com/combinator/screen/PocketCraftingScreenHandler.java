package com.combinator.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;

/** A crafting grid that stays open without a crafting table block nearby (Pocket Workbench). */
public class PocketCraftingScreenHandler extends CraftingScreenHandler {
	public PocketCraftingScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(syncId, playerInventory, context);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return true;
	}
}
