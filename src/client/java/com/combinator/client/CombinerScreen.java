package com.combinator.client;

import com.combinator.recipe.ComboRecipes;
import com.combinator.screen.CombinerScreenHandler;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The Combiner Table window.
 * When exactly one input slot is filled, a panel on the right shows every item it combines with.
 * Hover over one of them to see what the pair makes.
 */
public class CombinerScreen extends HandledScreen<CombinerScreenHandler> {
	private static final Identifier TEXTURE = Identifier.of("combinator", "textures/gui/combiner.png");
	private static final int HINT_COLUMNS = 4;
	private static final int HINT_MAX = 32;

	public CombinerScreen(CombinerScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawHints(context, mouseX, mouseY);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	private void drawHints(DrawContext context, int mouseX, int mouseY) {
		ItemStack a = this.handler.getSlot(CombinerScreenHandler.SLOT_A).getStack();
		ItemStack b = this.handler.getSlot(CombinerScreenHandler.SLOT_B).getStack();
		if (a.isEmpty() == b.isEmpty()) {
			return; // hints only when exactly one slot is filled
		}
		Item have = (a.isEmpty() ? b : a).getItem();
		List<ComboRecipes.Combo> combos = ComboRecipes.partners(have);

		int left = this.x + this.backgroundWidth + 6;
		int top = this.y + 4;
		if (combos.isEmpty()) {
			context.drawText(this.textRenderer, Text.translatable("gui.combinator.hint_none"), left, top, 0xFFAAAAAA, true);
			return;
		}
		context.drawText(this.textRenderer, Text.translatable("gui.combinator.hints"), left, top, 0xFFFFFFFF, true);

		ComboRecipes.Combo hovered = null;
		int shown = Math.min(combos.size(), HINT_MAX);
		for (int i = 0; i < shown; i++) {
			ComboRecipes.Combo combo = combos.get(i);
			int cx = left + (i % HINT_COLUMNS) * 18;
			int cy = top + 12 + (i / HINT_COLUMNS) * 18;
			context.fill(cx - 1, cy - 1, cx + 17, cy + 17, 0xA0000000);
			context.drawItem(new ItemStack(combo.partnerOf(have)), cx, cy);
			if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
				hovered = combo;
			}
		}
		if (hovered != null) {
			ItemStack partner = new ItemStack(hovered.partnerOf(have));
			ItemStack result = hovered.createResult();
			Text line1 = Text.literal("+ ").append(partner.getName());
			Text line2 = Text.literal("= ").append(result.getName()).formatted(Formatting.GOLD);
			context.drawTooltip(this.textRenderer, List.of(line1, line2), mouseX, mouseY);
		}
	}
}
