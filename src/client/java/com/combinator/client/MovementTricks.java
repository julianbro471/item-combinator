package com.combinator.client;

import com.combinator.ability.Charms;
import com.combinator.ability.PassiveAbilities;
import com.combinator.ability.TrickPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Double jump, pogo bounce and wall climbing.
 * A player's movement is worked out on the player's own computer, not on the server, so these three live here.
 * This runs once at the end of every game tick (20 times a second).
 */
final class MovementTricks {
	private static boolean jumpWasDown;
	/** How many ticks in a row the player has been in the air. */
	private static int airTicks;
	private static int airJumpsUsed;
	/** The player's upward speed at the end of the last tick. Negative while falling. */
	private static double lastSpeedUp;
	private static int climbTicks;

	private MovementTricks() {
	}

	static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			jumpWasDown = false;
			airTicks = 0;
			airJumpsUsed = 0;
			lastSpeedUp = 0.0;
			return;
		}
		boolean jumpDown = client.options.jumpKey.isPressed();
		boolean jumpPressed = jumpDown && !jumpWasDown;
		jumpWasDown = jumpDown;

		boolean onGround = player.isOnGround();
		boolean wasInAir = airTicks > 0;
		double fallSpeed = lastSpeedUp;
		// In these cases the game has its own rules for moving, and the tricks stay out of the way.
		boolean busy = !player.isAlive() || player.isSpectator() || player.getAbilities().flying || player.isFallFlying()
				|| player.hasVehicle() || player.isTouchingWater() || player.isInLava() || player.isClimbing();
		if (onGround || busy) {
			airTicks = 0;
			airJumpsUsed = 0;
		} else {
			airTicks++;
		}
		lastSpeedUp = onGround ? 0.0 : player.getVelocity().y;
		if (busy || !ClientPlayNetworking.canSend(TrickPayload.ID)) {
			return; // canSend is false on a server that does not have this mod
		}

		int[] airJumps = {0};
		boolean[] bounce = {false};
		boolean[] climb = {false};
		PassiveAbilities.forEachActive(player, (stack, traits) -> {
			airJumps[0] = Math.max(airJumps[0], traits.airJumps);
			bounce[0] |= traits.bounce;
			climb[0] |= traits.wallClimb;
			return false;
		});
		Vec3d velocity = player.getVelocity();

		// Sticky Boots: pressing against a wall moves the player up it, like on a ladder. Sneaking holds on.
		if (climb[0] && player.horizontalCollision) {
			player.setVelocity(velocity.x, player.isSneaking() ? 0.0 : 0.12, velocity.z);
			player.fallDistance = 0.0F;
			airJumpsUsed = 0;
			if (climbTicks++ % 5 == 0) {
				ClientPlayNetworking.send(new TrickPayload(Charms.TRICK_CLIMB));
			}
			return;
		}
		climbTicks = 0;

		// Bottled Dust Devil: a fresh press of the jump key in the air is one more jump.
		// "wasInAir" keeps the normal jump from the ground from counting as an air jump in the same tick.
		if (jumpPressed && wasInAir && !onGround && airJumpsUsed < airJumps[0]) {
			StatusEffectInstance jumpBoost = player.getStatusEffect(StatusEffects.JUMP_BOOST);
			double up = 0.5 + (jumpBoost == null ? 0.0 : 0.1 * (jumpBoost.getAmplifier() + 1));
			player.setVelocity(velocity.x, up, velocity.z);
			player.fallDistance = 0.0F;
			airJumpsUsed++;
			ClientPlayNetworking.send(new TrickPayload(Charms.TRICK_DOUBLE_JUMP));
			return;
		}

		// Pogo Stick: the moment of landing throws the player up again, a bit faster than they came down.
		if (bounce[0] && onGround && wasInAir && fallSpeed < -0.1 && !player.isSneaking()) {
			double up = MathHelper.clamp(-fallSpeed + 0.1, 0.5, 1.25);
			player.setVelocity(velocity.x, up, velocity.z);
			player.setOnGround(false); // otherwise the game would replace the bounce by a normal jump if the jump key is held
			player.playSound(SoundEvents.BLOCK_SLIME_BLOCK_FALL, 0.8F, 1.2F);
		}
	}
}
