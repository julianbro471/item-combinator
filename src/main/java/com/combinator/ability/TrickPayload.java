package com.combinator.ability;

import com.combinator.ItemCombinator;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * A small message from the game client to the server: "the player just did a movement trick".
 * Jumping and climbing are worked out on the player's own computer (like all player movement), so the
 * server has to be told. It then resets the fall height and shows the cloud puff to everyone.
 */
public record TrickPayload(int trick) implements CustomPayload {
	public static final CustomPayload.Id<TrickPayload> ID = new CustomPayload.Id<>(ItemCombinator.id("trick"));
	public static final PacketCodec<RegistryByteBuf, TrickPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.VAR_INT, TrickPayload::trick, TrickPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
