package com.combinator.screen;

import com.combinator.ItemCombinator;
import com.combinator.recipe.ComboRecipes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Two input slots and one output slot.
 * Slot numbers: 0 and 1 = inputs, 2 = output, 3-29 = player inventory, 30-38 = hotbar.
 */
public class CombinerScreenHandler extends ScreenHandler {
	public static final int SLOT_A = 0;
	public static final int SLOT_B = 1;
	public static final int SLOT_OUT = 2;
	private static final int INV_START = 3;
	private static final int HOTBAR_START = 30;
	private static final int END = 39;

	private final ScreenHandlerContext context;
	private final PlayerEntity player;
	private final CraftingResultInventory result = new CraftingResultInventory();
	private final SimpleInventory input = new SimpleInventory(2) {
		@Override
		public void markDirty() {
			super.markDirty();
			CombinerScreenHandler.this.onContentChanged(this);
		}
	};

	/** Client side constructor. */
	public CombinerScreenHandler(int syncId, PlayerInventory playerInventory) {
		this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
	}

	public CombinerScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(ItemCombinator.COMBINER_SCREEN, syncId);
		this.context = context;
		this.player = playerInventory.player;

		this.addSlot(new Slot(this.input, 0, 35, 35));
		this.addSlot(new Slot(this.input, 1, 71, 35));
		this.addSlot(new Slot(this.result, 0, 133, 35) {
			@Override
			public boolean canInsert(ItemStack stack) {
				return false;
			}

			@Override
			public void onTakeItem(PlayerEntity player, ItemStack stack) {
				CombinerScreenHandler.this.consumeInputs(player);
				super.onTakeItem(player, stack);
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
		}
	}

	@Override
	public void onContentChanged(Inventory inventory) {
		super.onContentChanged(inventory);
		if (inventory == this.input) {
			this.updateResult();
		}
	}

	private void updateResult() {
		this.result.setStack(0, ComboRecipes.craft(this.input.getStack(0), this.input.getStack(1)));
	}

	/** Uses up one item from each input slot. Gives back containers such as empty bottles. */
	private void consumeInputs(PlayerEntity taker) {
		for (int i = 0; i < 2; i++) {
			ItemStack in = this.input.getStack(i);
			if (in.isEmpty()) {
				continue;
			}
			Item remainder = in.getItem().getRecipeRemainder();
			this.input.removeStack(i, 1);
			if (remainder != null && !taker.getWorld().isClient) {
				taker.getInventory().offerOrDrop(new ItemStack(remainder));
			}
		}
		this.context.run((world, pos) -> world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
				SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 0.8F, 1.2F));
	}

	@Override
	public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.context.run((world, pos) -> this.dropInventory(player, this.input));
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return canUse(this.context, player, ItemCombinator.COMBINER_TABLE);
	}

	@Override
	public ItemStack quickMove(PlayerEntity player, int index) {
		ItemStack copy = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasStack()) {
			return copy;
		}
		ItemStack stack = slot.getStack();
		copy = stack.copy();

		if (index == SLOT_OUT) {
			if (!this.insertItem(stack, INV_START, END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickTransfer(stack, copy);
		} else if (index == SLOT_A || index == SLOT_B) {
			if (!this.insertItem(stack, INV_START, END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.insertItem(stack, SLOT_A, SLOT_OUT, false)) {
			// Inputs are full: move between inventory and hotbar instead.
			if (index < HOTBAR_START) {
				if (!this.insertItem(stack, HOTBAR_START, END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.insertItem(stack, INV_START, HOTBAR_START, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setStack(ItemStack.EMPTY);
		} else {
			slot.markDirty();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTakeItem(player, stack);
		if (index == SLOT_OUT && !stack.isEmpty()) {
			// The result did not fully fit into the inventory: drop the rest instead of losing it.
			player.dropItem(stack, false);
		}
		return copy;
	}
}
