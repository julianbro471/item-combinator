package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The Mob Net: right-click a mob to put it into the net, right-click a block to let it out again.
 * The whole mob (health, name, trades, what it carries) is stored inside the item.
 */
public final class Capture {
	private static final String KEY = "combinator_mob";

	private Capture() {
	}

	public static void register() {
		// This event runs before the mob's own right-click action, so villagers do not open their trade window instead.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);
			Traits traits = ComboItems.traits(stack.getItem());
			if (traits == null || !traits.capture || player.isSpectator() || isFull(stack) || !canHold(entity)) {
				return ActionResult.PASS;
			}
			if (world.isClient) {
				return ActionResult.SUCCESS;
			}
			try {
				if (capture(player, stack, (LivingEntity) entity)) {
					return ActionResult.SUCCESS;
				}
			} catch (Throwable t) {
				ItemCombinator.error("Mob Net failed", t);
			}
			return ActionResult.PASS;
		});
	}

	/** True if the net already holds a mob. */
	public static boolean isFull(ItemStack stack) {
		return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).contains(KEY);
	}

	private static boolean canHold(Entity entity) {
		return entity instanceof LivingEntity && entity.isAlive() && !(entity instanceof PlayerEntity)
				&& !(entity instanceof EnderDragonEntity) && !(entity instanceof WitherEntity);
	}

	/** Puts the mob into the net. Returns false if the game cannot save this mob. */
	public static boolean capture(PlayerEntity player, ItemStack stack, LivingEntity mob) {
		if (!(mob.getWorld() instanceof ServerWorld world) || isFull(stack) || !canHold(mob)) {
			return false;
		}
		mob.stopRiding();
		mob.removeAllPassengers();
		NbtCompound saved = new NbtCompound();
		if (!mob.saveSelfNbt(saved)) {
			return false;
		}
		saved.remove("UUID"); // it gets a new one when it comes out, so two mobs can never share one
		Text name = mob.getName();
		Vec3d at = mob.getPos().add(0.0, mob.getHeight() * 0.5, 0.0);
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(KEY, saved));
		stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
				Text.literal("Holds: ").append(name).styled(style -> style.withItalic(false).withColor(Formatting.GOLD)))));
		mob.discard();
		Fx.particles(world, ParticleTypes.POOF, at, 20, 0.3, 0.02);
		Fx.sound(world, at, SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 0.7F);
		return true;
	}

	/** Right-click on a block: lets the mob out next to it. PASS if the net is empty. */
	static ActionResult release(ItemUsageContext context, PlayerEntity player, ItemStack stack) {
		if (!isFull(stack)) {
			return ActionResult.PASS;
		}
		if (!(context.getWorld() instanceof ServerWorld world)) {
			return ActionResult.SUCCESS;
		}
		NbtCompound saved = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getCompound(KEY);
		BlockPos spot = context.getBlockPos().offset(context.getSide());
		Entity mob = EntityType.loadEntityWithPassengers(saved, world, entity -> {
			entity.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYaw() + 180.0F, 0.0F);
			return entity;
		});
		if (mob == null) {
			player.sendMessage(Text.literal("The mob in this net cannot live here."), true);
			return ActionResult.FAIL;
		}
		world.spawnNewEntityAndPassengers(mob);
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(KEY));
		stack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
		stack.remove(DataComponentTypes.LORE);
		Vec3d at = Vec3d.ofBottomCenter(spot).add(0.0, 0.5, 0.0);
		Fx.particles(world, ParticleTypes.POOF, at, 20, 0.3, 0.02);
		Fx.sound(world, at, SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, 1.3F);
		// The net wears out when a mob leaves it, never while one is inside. So a net cannot break with a mob in it.
		stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
		return ActionResult.SUCCESS;
	}
}
