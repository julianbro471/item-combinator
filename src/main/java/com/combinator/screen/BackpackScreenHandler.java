package com.combinator.screen;

import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The window of a Backpack. It looks like a chest (3 or 6 rows), so the game client needs nothing new.
 * The things in it are written into the backpack item itself after every change.
 * While the window is open no backpack and no shulker box can be moved, so neither can end up inside a backpack.
 */
public class BackpackScreenHandler extends GenericContainerScreenHandler {
	private final ItemStack bag;
	private final SimpleInventory contents;

	private BackpackScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, SimpleInventory contents, int rows, ItemStack bag) {
		super(type, syncId, playerInventory, contents, rows);
		this.bag = bag;
		this.contents = contents;
	}

	/** Opens the backpack the player holds. rows is 3 or 6. */
	public static void open(ServerPlayerEntity player, ItemStack bag, int rows) {
		SimpleInventory contents = new SimpleInventory(rows * 9);
		bag.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(contents.getHeldStacks());
		contents.addListener(changed -> save(bag, contents));
		ScreenHandlerType<?> type = rows == 6 ? ScreenHandlerType.GENERIC_9X6 : ScreenHandlerType.GENERIC_9X3;
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, opener) -> new BackpackScreenHandler(type, syncId, inventory, contents, rows, bag), bag.getName()));
	}

	private static void save(ItemStack bag, SimpleInventory contents) {
		bag.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(contents.getHeldStacks()));
	}

	/** True for every kind of backpack. */
	public static boolean isBackpack(ItemStack stack) {
		Traits traits = ComboItems.traits(stack.getItem());
		return traits != null && traits.use == Use.BACKPACK;
	}

	/**
	 * True for things that must not be moved while a backpack is open: backpacks, and boxes that hold
	 * items themselves (shulker boxes). Without this rule one item could hold an endless amount of items.
	 */
	public static boolean staysOut(ItemStack stack) {
		return !stack.isEmpty() && (isBackpack(stack) || !stack.getItem().canBeNested());
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex >= 0 && slotIndex < this.slots.size() && staysOut(this.slots.get(slotIndex).getStack())) {
			return;
		}
		if (actionType == SlotActionType.SWAP && button >= 0 && button < player.getInventory().size()
				&& staysOut(player.getInventory().getStack(button))) {
			return;
		}
		if (staysOut(this.getCursorStack())) {
			return;
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		// The window closes when the open backpack leaves the player's hands.
		return !this.bag.isEmpty() && (player.getMainHandStack() == this.bag || player.getOffHandStack() == this.bag);
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		save(this.bag, this.contents);
	}
}
