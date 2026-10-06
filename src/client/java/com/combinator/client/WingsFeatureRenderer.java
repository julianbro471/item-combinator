package com.combinator.client;

import com.combinator.item.CWings;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Draws elytra wings on a player who wears one of this mod's wing items.
 * The game only draws wings for the vanilla elytra item, so this repeats that drawing for ours.
 * It uses the game's own elytra texture.
 */
public class WingsFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier SKIN = Identifier.ofVanilla("textures/entity/elytra.png");
	private final ElytraEntityModel<AbstractClientPlayerEntity> elytra;

	public WingsFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context, EntityModelLoader loader) {
		super(context);
		this.elytra = new ElytraEntityModel<>(loader.getModelPart(EntityModelLayers.ELYTRA));
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity entity,
			float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		ItemStack stack = entity.getEquippedStack(EquipmentSlot.CHEST);
		if (!(stack.getItem() instanceof CWings)) {
			return;
		}
		matrices.push();
		matrices.translate(0.0F, 0.0F, 0.125F);
		this.getContextModel().copyStateTo(this.elytra);
		this.elytra.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
		VertexConsumer vertices = ItemRenderer.getArmorGlintConsumer(vertexConsumers, RenderLayer.getArmorCutoutNoCull(SKIN), stack.hasGlint());
		this.elytra.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}
}
