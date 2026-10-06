package com.combinator.client;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.screen.BackpackScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/** Client-only setup: the table screen, tooltips, visible wings and the elytra booster. */
public class ItemCombinatorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(ItemCombinator.COMBINER_SCREEN, CombinerScreen::new);

		// Gray description lines under the item name.
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			Item item = stack.getItem();
			int count = ComboItems.tipLines(item);
			if (count <= 0) {
				return;
			}
			String key = item.getTranslationKey();
			for (int i = 0; i < count; i++) {
				Text line = Text.translatable(key + ".tip" + (i + 1)).formatted(Formatting.GRAY);
				lines.add(Math.min(lines.size(), 1 + i), line);
			}
			// Backpacks say how full they are.
			ContainerComponent contents = stack.get(DataComponentTypes.CONTAINER);
			if (contents != null && BackpackScreenHandler.isBackpack(stack)) {
				long used = contents.streamNonEmpty().count();
				lines.add(Math.min(lines.size(), 1 + count), Text.literal("Holds " + used + (used == 1 ? " stack" : " stacks")).formatted(Formatting.GOLD));
			}
		});

		// Wings on the player's back for the elytra-type chest items.
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
				registrationHelper.register(new WingsFeatureRenderer(playerRenderer, context.getModelLoader()));
			}
		});

		// Double jump, pogo bounce and wall climbing.
		ClientTickEvents.END_CLIENT_TICK.register(MovementTricks::tick);

		// Rocket Elytra and Seraph Wings: hold sneak while gliding to speed up.
		// Player movement is decided on the client, so the push is applied here.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientPlayerEntity player = client.player;
			if (player == null || !player.isFallFlying() || !player.isSneaking()) {
				return;
			}
			Traits traits = ComboItems.traits(player.getEquippedStack(EquipmentSlot.CHEST).getItem());
			if (traits == null || !traits.boost) {
				return;
			}
			Vec3d look = player.getRotationVector();
			Vec3d velocity = player.getVelocity();
			double speed = 1.4;
			player.setVelocity(velocity.add(
					look.x * 0.1 + (look.x * speed - velocity.x) * 0.5,
					look.y * 0.1 + (look.y * speed - velocity.y) * 0.5,
					look.z * 0.1 + (look.z * speed - velocity.z) * 0.5));
		});
	}
}
