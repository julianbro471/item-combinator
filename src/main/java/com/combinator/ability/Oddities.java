package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The odd gadgets: right-click abilities that do not fit anywhere else.
 * Disco Ball, Piggy Bank, Weather Vane, Magnifying Glass, Skeleton Key and Push Glove.
 */
public final class Oddities {
	/** Marks a mob that dances right now. Its thinking is switched off for that time. */
	public static final String DANCE_TAG = "combinator_dancing";
	private static final int DANCE_TICKS = 160;
	/** Where the Piggy Bank keeps its experience points inside the item. */
	private static final String BANK_KEY = "combinator_xp";

	private Oddities() {
	}

	public static void register() {
		// A mob that was dancing when the world was closed would stand frozen forever.
		// So every marked mob gets its thinking back the moment it is loaded.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			try {
				if (entity instanceof MobEntity mob && mob.getCommandTags().contains(DANCE_TAG)) {
					stopDancing(mob);
				}
			} catch (Throwable t) {
				ItemCombinator.error("Waking up a dancing mob failed", t);
			}
		});
	}

	// ------------------------------------------------------------------ Disco Ball

	/** Every mob nearby stops what it is doing and spins on the spot for 8 seconds. Bosses do not dance. */
	static boolean disco(ServerWorld world, ServerPlayerEntity player, int range) {
		List<MobEntity> crowd = new ArrayList<>(world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(range),
				mob -> mob.isAlive() && !mob.isAiDisabled() && !(mob instanceof EnderDragonEntity) && !(mob instanceof WitherEntity)));
		if (crowd.isEmpty()) {
			player.sendMessage(Text.literal("Nobody here wants to dance."), true);
			return false;
		}
		for (MobEntity mob : crowd) {
			mob.addCommandTag(DANCE_TAG);
			mob.setTarget(null);
			mob.getNavigation().stop();
			if (mob instanceof CreeperEntity creeper) {
				creeper.setFuseSpeed(-1); // a creeper that was about to blow up calms down
			}
			mob.setAiDisabled(true);
		}
		int steps = DANCE_TICKS / 2;
		Tasks.repeat(steps, 2, i -> {
			boolean last = i == steps - 1;
			for (MobEntity mob : crowd) {
				if (!mob.isAlive() || !mob.getCommandTags().contains(DANCE_TAG)) {
					continue;
				}
				if (last) {
					stopDancing(mob);
					continue;
				}
				float yaw = mob.getYaw() + 36.0F;
				mob.setYaw(yaw);
				mob.setBodyYaw(yaw);
				mob.setHeadYaw(yaw);
				mob.setPitch(i % 10 < 5 ? -25.0F : 20.0F);
				if (i % 10 == 0) {
					Fx.particles(world, ParticleTypes.NOTE, mob.getPos().add(0.0, mob.getHeight() + 0.3, 0.0), 1, 0.2, 0.0);
				}
			}
			if (!last && i % 4 == 0 && player.isAlive() && player.getWorld() == world) {
				float pitch = 0.6F + (i / 4 % 8) * 0.12F;
				Fx.sound(world, player.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_BIT, 1.2F, pitch);
			}
		});
		return true;
	}

	private static void stopDancing(MobEntity mob) {
		mob.setAiDisabled(false);
		mob.removeCommandTag(DANCE_TAG);
	}

	// ------------------------------------------------------------------ Piggy Bank

	/** How many experience points a player with this level and this much of the bar has collected in total. */
	public static int points(int level, float progress) {
		int base;
		if (level <= 16) {
			base = level * level + 6 * level;
		} else if (level <= 31) {
			base = (int) (2.5 * level * level - 40.5 * level + 360.0);
		} else {
			base = (int) (4.5 * level * level - 162.5 * level + 2220.0);
		}
		int next = level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
		return base + Math.round(progress * next);
	}

	/** The experience points stored in a Piggy Bank. */
	public static int stored(ItemStack bank) {
		return bank.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getInt(BANK_KEY);
	}

	private static void store(ItemStack bank, int points) {
		if (points <= 0) {
			NbtComponent.set(DataComponentTypes.CUSTOM_DATA, bank, nbt -> nbt.remove(BANK_KEY));
			bank.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
			bank.remove(DataComponentTypes.LORE);
			return;
		}
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, bank, nbt -> nbt.putInt(BANK_KEY, points));
		bank.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		bank.set(DataComponentTypes.LORE, new LoreComponent(List.of(
				Text.literal("Holds " + points + " experience points").styled(style -> style.withItalic(false).withColor(Formatting.GREEN)))));
	}

	/** Puts all of the player's experience into the bank. */
	static boolean bankIn(ServerWorld world, ServerPlayerEntity player, ItemStack bank) {
		int points = points(player.experienceLevel, player.experienceProgress);
		if (points <= 0) {
			player.sendMessage(Text.literal("You have no experience to put in."), true);
			return false;
		}
		player.setExperienceLevel(0);
		player.setExperiencePoints(0);
		player.totalExperience = 0;
		store(bank, stored(bank) + points);
		player.sendMessage(Text.literal("The Piggy Bank now holds " + stored(bank) + " experience points.").formatted(Formatting.GREEN), true);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 0.6F);
		return true;
	}

	/** Gives everything in the bank back to the player. */
	static boolean bankOut(ServerWorld world, ServerPlayerEntity player, ItemStack bank) {
		int points = stored(bank);
		if (points <= 0) {
			player.sendMessage(Text.literal("The Piggy Bank is empty."), true);
			return false;
		}
		store(bank, 0);
		player.addExperience(points);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6F, 1.2F);
		return true;
	}

	// ------------------------------------------------------------------ Weather Vane

	/** Clear sky, then rain, then thunder, then clear sky again. */
	static boolean weather(ServerWorld world, ServerPlayerEntity player) {
		ServerWorld overworld = world.getServer().getOverworld();
		if (world != overworld) {
			player.sendMessage(Text.literal("The Weather Vane only works in the Overworld."), true);
			return false;
		}
		String now;
		if (overworld.getLevelProperties().isThundering()) {
			overworld.setWeather(12000, 0, false, false);
			now = "Clear sky";
		} else if (overworld.getLevelProperties().isRaining()) {
			overworld.setWeather(0, 12000, true, true);
			now = "Thunderstorm";
		} else {
			overworld.setWeather(0, 12000, true, false);
			now = "Rain";
		}
		player.sendMessage(Text.literal("Weather: " + now).formatted(Formatting.AQUA), true);
		Fx.particles(world, ParticleTypes.CLOUD, player.getEyePos().add(0.0, 1.0, 0.0), 20, 0.5, 0.02);
		Fx.sound(world, player.getPos(), SoundEvents.ITEM_TRIDENT_RETURN, 1.0F, 0.7F);
		return true;
	}

	// ------------------------------------------------------------------ Magnifying Glass

	/** True if the sun shines on the player: daytime, no rain, open sky above. */
	static boolean inSunlight(ServerWorld world, PlayerEntity player) {
		long time = world.getTimeOfDay() % 24000L;
		boolean sunUp = time < 12300L || time > 23850L;
		return world.getDimension().hasSkyLight() && sunUp && !world.isRaining() && world.isSkyVisible(BlockPos.ofFloored(player.getEyePos()));
	}

	/** Sets the mob or the block the player looks at on fire, but only in sunlight. */
	static boolean sunburn(ServerWorld world, ServerPlayerEntity player, int range) {
		if (!inSunlight(world, player)) {
			player.sendMessage(Text.literal("The glass needs direct sunlight."), true);
			return false;
		}
		Vec3d eye = player.getEyePos();
		LivingEntity target = Fx.lookEntity(world, player, range);
		if (target != null) {
			target.setOnFireFor(6);
			Fx.line(world, ParticleTypes.SMALL_FLAME, eye.add(0.0, -0.2, 0.0), target.getPos().add(0.0, target.getHeight() * 0.6, 0.0));
			Fx.sound(world, target.getPos(), SoundEvents.ITEM_FIRECHARGE_USE, 0.6F, 1.4F);
			return true;
		}
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = world.getBlockState(pos);
		BlockPos firePos = pos.offset(hit.getSide());
		if (CampfireBlock.canBeLit(state) || CandleBlock.canBeLit(state)) {
			world.setBlockState(pos, state.with(Properties.LIT, true), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
		} else if (AbstractFireBlock.canPlaceAt(world, firePos, player.getHorizontalFacing())) {
			world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD);
		} else {
			return false;
		}
		Fx.line(world, ParticleTypes.SMALL_FLAME, eye.add(0.0, -0.2, 0.0), hit.getPos());
		Fx.sound(world, hit.getPos(), SoundEvents.ITEM_FIRECHARGE_USE, 0.6F, 1.4F);
		return true;
	}

	// ------------------------------------------------------------------ Skeleton Key

	/** Opens or closes a door or trapdoor, also one made of iron. PASS for every other block. */
	static ActionResult unlock(ItemUsageContext context, PlayerEntity player, ItemStack stack) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		boolean door = state.getBlock() instanceof DoorBlock;
		boolean trapdoor = state.getBlock() instanceof TrapdoorBlock;
		if (!door && !trapdoor) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (door) {
			DoorBlock block = (DoorBlock) state.getBlock();
			block.setOpen(player, world, state, pos, !block.isOpen(state));
		} else {
			BlockState toggled = state.cycle(TrapdoorBlock.OPEN);
			world.setBlockState(pos, toggled, Block.NOTIFY_LISTENERS);
			Fx.sound(world, Vec3d.ofCenter(pos), toggled.get(TrapdoorBlock.OPEN) ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0F, 1.0F);
		}
		stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
		return ActionResult.SUCCESS;
	}

	// ------------------------------------------------------------------ Push Glove

	/**
	 * Moves the clicked block one step: away from the player, or towards the player while sneaking.
	 * It moves what a piston can move, so no obsidian, no chests, no doors.
	 */
	static ActionResult push(ItemUsageContext context, PlayerEntity player, ItemStack stack) {
		World world = context.getWorld();
		BlockPos from = context.getBlockPos();
		BlockState state = world.getBlockState(from);
		Direction direction = player.isSneaking() ? context.getSide() : context.getSide().getOpposite();
		BlockPos to = from.offset(direction);
		if (state.isAir() || !state.getFluidState().isEmpty() || !world.isInBuildLimit(to) || !world.getBlockState(to).isAir()
				|| !PistonBlock.isMovable(state, world, from, direction, false, direction)) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		// Nothing alive may stand where the block goes. This also keeps a pulled block out of the player's own legs.
		if (!world.getOtherEntities(null, new Box(to), entity -> entity instanceof LivingEntity).isEmpty()) {
			return ActionResult.FAIL;
		}
		world.setBlockState(from, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
		world.setBlockState(to, Block.postProcessState(state, world, to), Block.NOTIFY_ALL);
		Fx.sound(world, Vec3d.ofCenter(to), player.isSneaking() ? SoundEvents.BLOCK_PISTON_CONTRACT : SoundEvents.BLOCK_PISTON_EXTEND, 0.6F, 1.1F);
		stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
		player.getItemCooldownManager().set(stack.getItem(), 4);
		return ActionResult.SUCCESS;
	}
}
