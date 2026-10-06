package com.combinator.ability;

import com.combinator.item.ComboItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/** The random events of the Chaos Orb and Pandora's Box. Some are good, some are silly, some are bad. */
public final class Chaos {
	/** How many different events there are. */
	public static final int EVENTS = 24;

	private Chaos() {
	}

	/** Pandora's Box: six events, one per second. */
	static boolean pandora(ServerWorld world, ServerPlayerEntity player) {
		Tasks.repeat(6, 20, i -> {
			if (player.isAlive() && player.getWorld() == world) {
				one(world, player);
			}
		});
		return true;
	}

	/** Chaos Orb: one random event. */
	static boolean one(ServerWorld world, ServerPlayerEntity player) {
		return event(world, player, world.random.nextInt(EVENTS));
	}

	/** Runs the event with the given number (0 to EVENTS - 1). The automatic tests use this to try every event. */
	public static boolean event(ServerWorld world, ServerPlayerEntity player, int number) {
		Random random = world.random;
		Vec3d here = player.getPos();
		String name;
		switch (number) {
			case 0 -> {
				name = "It rains diamonds";
				rain(world, here, Items.DIAMOND, 6);
			}
			case 1 -> {
				name = "It rains gold";
				rain(world, here, Items.GOLD_INGOT, 14);
			}
			case 2 -> {
				name = "You feel great";
				player.setHealth(player.getMaxHealth());
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3));
			}
			case 3 -> {
				name = "Sudden wisdom";
				player.addExperienceLevels(10);
			}
			case 4 -> {
				name = "Jackpot: a random combined item";
				Item prize = ComboItems.ALL.get(random.nextInt(ComboItems.ALL.size()));
				player.getInventory().offerOrDrop(new ItemStack(prize));
			}
			case 5 -> {
				name = "Fowl weather";
				Spells.chickenStorm(world, here);
			}
			case 6 -> {
				name = "Up you go";
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 600, 0));
				player.addVelocity(0.0, 3.5, 0.0);
				player.velocityModified = true;
			}
			case 7 -> {
				name = "Thunderstorm";
				for (int i = 0; i < 6; i++) {
					UseAbilities.strike(world, player, groundNear(world, here, 10.0));
				}
			}
			case 8 -> {
				name = "Everything is a farm animal now";
				for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(16.0),
						e -> e != player && e.isAlive() && !(e instanceof PlayerEntity))) {
					Spells.polymorph(world, living);
				}
			}
			case 9 -> {
				name = "Anvils. Run";
				Spells.anvilStorm(world, here);
			}
			case 10 -> {
				name = "Gravity looks the other way";
				Spells.gravity(world, player, 16, true);
			}
			case 11 -> {
				name = "Half a day passes";
				ServerWorld overworld = world.getServer().getOverworld();
				overworld.setTimeOfDay(overworld.getTimeOfDay() + 12000L);
			}
			case 12 -> {
				Vec3d spot = standingSpotNear(world, player, 80.0);
				if (spot != null) {
					name = "Somewhere else";
					player.requestTeleport(spot.x, spot.y, spot.z);
					player.fallDistance = 0.0F;
				} else {
					name = "Nothing happens. Lucky you";
				}
			}
			case 13 -> {
				name = "Iron friends";
				Spells.summonGolems(world, player, 2);
			}
			case 14 -> {
				name = "Boom";
				Fx.boom(world, player, here, 4.0F, false, true);
			}
			case 15 -> {
				name = "Creepers";
				for (int i = 0; i < 4; i++) {
					Vec3d spot = groundNear(world, here, 8.0);
					Spells.spawn(world, EntityType.CREEPER, spot.x, spot.y, spot.z);
				}
			}
			case 16 -> {
				name = "TNT from above";
				for (int i = 0; i < 5; i++) {
					TntEntity tnt = new TntEntity(world, here.x + (random.nextDouble() - 0.5) * 10.0, here.y + 14.0,
							here.z + (random.nextDouble() - 0.5) * 10.0, null);
					tnt.setFuse(50 + random.nextInt(30));
					world.spawnEntity(tnt);
				}
			}
			case 17 -> {
				name = "You feel sick";
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 160, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 1));
			}
			case 18 -> {
				name = "A black hole opens";
				Vec3d look = player.getRotationVec(1.0F);
				Spells.blackHole(world, player.getEyePos().add(look.x * 12.0, 2.0, look.z * 12.0));
			}
			case 19 -> {
				name = "You grow";
				PassiveAbilities.grantBuff(player, ComboItems.EAT_ME_CAKE, 60);
			}
			case 20 -> {
				name = "Golden apples";
				rain(world, here, Items.GOLDEN_APPLE, 3);
			}
			case 21 -> {
				name = "Sudden winter";
				Spells.freezeArea(world, player, 10);
			}
			case 22 -> {
				name = "The ground turns to gold";
				Spells.goldBlocks(world, player.getBlockPos().down());
			}
			default -> {
				name = "Time stops";
				Spells.timeStop(world, player, 100);
			}
		}
		player.sendMessage(Text.literal("Chaos: " + name + "!").formatted(Formatting.LIGHT_PURPLE), true);
		Fx.particles(world, ParticleTypes.WITCH, here.add(0.0, 1.0, 0.0), 40, 0.6, 0.1);
		Fx.sound(world, here, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
		return true;
	}

	private static void rain(ServerWorld world, Vec3d center, Item item, int count) {
		Random random = world.random;
		for (int i = 0; i < count; i++) {
			world.spawnEntity(new ItemEntity(world, center.x + (random.nextDouble() - 0.5) * 6.0, center.y + 8.0 + random.nextDouble() * 4.0,
					center.z + (random.nextDouble() - 0.5) * 6.0, new ItemStack(item)));
		}
	}

	/**
	 * A random spot within the given distance where the player has room to stand, or null if none was found.
	 * Under the open sky this is the surface. In the Nether and in other places with a roof it is a spot
	 * at about the player's own height, so nobody ends up on top of the bedrock roof.
	 */
	private static Vec3d standingSpotNear(ServerWorld world, ServerPlayerEntity player, double distance) {
		Random random = world.random;
		Vec3d center = player.getPos();
		boolean roofed = world.getDimension().hasCeiling();
		for (int attempt = 0; attempt < 24; attempt++) {
			int x = (int) Math.floor(center.x + (random.nextDouble() - 0.5) * 2.0 * distance);
			int z = (int) Math.floor(center.z + (random.nextDouble() - 0.5) * 2.0 * distance);
			if (!roofed) {
				BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
				if (top.getY() > world.getBottomY() && hasRoom(world, top)) {
					return Vec3d.ofBottomCenter(top);
				}
				continue;
			}
			for (int dy = 12; dy >= -12; dy--) {
				BlockPos feet = new BlockPos(x, (int) Math.floor(center.y) + dy, z);
				if (feet.getY() > world.getBottomY() + 1 && feet.getY() < world.getTopY() - 2 && hasRoom(world, feet)) {
					return Vec3d.ofBottomCenter(feet);
				}
			}
		}
		return null;
	}

	/** True if a player fits at this position: something solid below, and two blocks without walls or liquids. */
	private static boolean hasRoom(ServerWorld world, BlockPos feet) {
		return world.getBlockState(feet.down()).isSolidBlock(world, feet.down())
				&& world.getBlockState(feet).getCollisionShape(world, feet).isEmpty() && world.getFluidState(feet).isEmpty()
				&& world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty() && world.getFluidState(feet.up()).isEmpty();
	}

	/** A random spot on the surface within the given distance. */
	private static Vec3d groundNear(ServerWorld world, Vec3d center, double distance) {
		Random random = world.random;
		double x = center.x + (random.nextDouble() - 0.5) * 2.0 * distance;
		double z = center.z + (random.nextDouble() - 0.5) * 2.0 * distance;
		BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(x, center.y, z));
		return new Vec3d(x, top.getY(), z);
	}
}
