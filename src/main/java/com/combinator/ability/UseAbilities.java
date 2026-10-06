package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import com.combinator.screen.PocketCraftingScreenHandler;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

/**
 * Right-click abilities. This class decides which ability an item has and handles the simple ones.
 * The big ones (bombs, black holes, time stop ...) live in {@link Spells}.
 */
public final class UseAbilities {
	private UseAbilities() {
	}

	// ------------------------------------------------------------------ right-click on a block

	/** Called by every combined item when it is used on a block. PASS means "nothing special, do the normal thing". */
	public static ActionResult useOnBlock(ItemUsageContext context) {
		PlayerEntity player = context.getPlayer();
		if (player == null) {
			return ActionResult.PASS;
		}
		ItemStack stack = context.getStack();
		Traits traits = ComboItems.traits(stack.getItem());
		if (traits == null) {
			return ActionResult.PASS;
		}
		try {
			if (traits.torch) {
				return placeTorch(context, player, stack);
			}
			if (traits.harvestRadius > 0) {
				return harvest(context, player, stack, traits);
			}
			if (traits.bonemealRadius > 0) {
				return bonemeal(context, player, stack, traits);
			}
			if (traits.midas) {
				if (context.getWorld() instanceof ServerWorld world) {
					Spells.goldBlocks(world, context.getBlockPos());
				}
				return ActionResult.success(context.getWorld().isClient);
			}
			if (traits.transmute) {
				if (context.getWorld() instanceof ServerWorld world) {
					if (Spells.transmute(world, context.getBlockPos(), player.isSneaking())) {
						stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
					}
				}
				return ActionResult.success(context.getWorld().isClient);
			}
			if (traits.house) {
				if (context.getWorld() instanceof ServerWorld world) {
					BlockPos ground = context.getSide() == Direction.UP ? context.getBlockPos().up() : context.getBlockPos().offset(context.getSide());
					Spells.buildHouse(world, player, ground);
					stack.decrementUnlessCreative(1, player);
				}
				return ActionResult.success(context.getWorld().isClient);
			}
		} catch (Throwable t) {
			ItemCombinator.LOGGER.error("Block use ability failed", t);
		}
		return ActionResult.PASS;
	}

	private static ActionResult placeTorch(ItemUsageContext context, PlayerEntity player, ItemStack stack) {
		World world = context.getWorld();
		Direction side = context.getSide();
		if (side == Direction.DOWN) {
			return ActionResult.PASS;
		}
		BlockPos place = context.getBlockPos().offset(side);
		BlockState torch = side == Direction.UP
				? Blocks.TORCH.getDefaultState()
				: Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, side);
		if (!world.getBlockState(place).isAir() || !torch.canPlaceAt(world, place)) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			world.setBlockState(place, torch);
			Fx.sound(world, Vec3d.ofCenter(place), SoundEvents.BLOCK_WOOD_PLACE, 1.0F, 1.0F);
			stack.damage(2, player, LivingEntity.getSlotForHand(context.getHand()));
		}
		return ActionResult.success(world.isClient);
	}

	private static boolean isRipe(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.isMature(state);
		}
		if (block instanceof NetherWartBlock) {
			return state.get(NetherWartBlock.AGE) >= 3;
		}
		return false;
	}

	private static BlockState replanted(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof CropBlock crop) {
			return crop.withAge(0);
		}
		return state.with(NetherWartBlock.AGE, 0);
	}

	private static ActionResult harvest(ItemUsageContext context, PlayerEntity player, ItemStack stack, Traits traits) {
		World world = context.getWorld();
		BlockPos center = context.getBlockPos();
		if (!isRipe(world.getBlockState(center))) {
			return ActionResult.PASS;
		}
		if (world instanceof ServerWorld serverWorld) {
			int r = traits.harvestRadius;
			int harvested = 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					for (int dy = -1; dy <= 1; dy++) {
						BlockPos pos = center.add(dx, dy, dz);
						BlockState state = serverWorld.getBlockState(pos);
						if (!isRipe(state)) {
							continue;
						}
						List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, null, player, stack);
						serverWorld.setBlockState(pos, replanted(state));
						for (ItemStack drop : drops) {
							if (traits.magnetDrops) {
								player.getInventory().insertStack(drop);
							}
							if (!drop.isEmpty()) {
								Block.dropStack(serverWorld, pos, drop);
							}
						}
						harvested++;
					}
				}
			}
			if (harvested > 0) {
				Fx.sound(world, Vec3d.ofCenter(center), SoundEvents.BLOCK_CROP_BREAK, 1.0F, 1.0F);
				stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
			}
		}
		return ActionResult.success(world.isClient);
	}

	private static ActionResult bonemeal(ItemUsageContext context, PlayerEntity player, ItemStack stack, Traits traits) {
		World world = context.getWorld();
		BlockPos center = context.getBlockPos();
		if (!(world.getBlockState(center).getBlock() instanceof Fertilizable)) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			int r = traits.bonemealRadius;
			int grown = 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockPos pos = center.add(dx, 0, dz);
					if (BoneMealItem.useOnFertilizable(new ItemStack(Items.BONE_MEAL), world, pos)) {
						world.syncWorldEvent(WorldEvents.BONE_MEAL_USED, pos, 15);
						grown++;
					}
				}
			}
			if (grown > 0) {
				stack.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
			}
		}
		return ActionResult.success(world.isClient);
	}

	// ------------------------------------------------------------------ right-click in the air

	/** Called by every combined item when it is used. PASS means "nothing special, do the normal thing". */
	public static TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		Traits traits = ComboItems.traits(stack.getItem());
		if (traits == null) {
			return TypedActionResult.pass(stack);
		}
		boolean sneak = user.isSneaking() && traits.sneakUse != Use.NONE;
		Use action = sneak ? traits.sneakUse : traits.use;
		if (action == Use.NONE) {
			return TypedActionResult.pass(stack);
		}
		if (user.getItemCooldownManager().isCoolingDown(stack.getItem())) {
			return TypedActionResult.fail(stack);
		}
		if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity player)) {
			return TypedActionResult.success(stack);
		}
		boolean done;
		try {
			done = perform(action, serverWorld, player, traits, hand);
		} catch (Throwable t) {
			ItemCombinator.LOGGER.error("Use ability {} failed", action, t);
			done = false;
		}
		if (!done) {
			return TypedActionResult.fail(stack);
		}
		int cooldown = sneak ? traits.sneakCooldown : traits.cooldown;
		if (cooldown > 0) {
			user.getItemCooldownManager().set(stack.getItem(), cooldown);
		}
		if (traits.consume) {
			stack.decrementUnlessCreative(1, user);
		} else if (traits.cost > 0) {
			stack.damage(traits.cost, user, LivingEntity.getSlotForHand(hand));
		}
		return TypedActionResult.success(stack);
	}

	/** Called after a combined food item was eaten. */
	public static void onEaten(Item item, World world, LivingEntity eater) {
		Traits traits = ComboItems.traits(item);
		if (traits == null || !(world instanceof ServerWorld serverWorld) || !(eater instanceof ServerPlayerEntity player)) {
			return;
		}
		try {
			if (traits.eatBuffSeconds > 0) {
				PassiveAbilities.grantBuff(player, item, traits.eatBuffSeconds);
				Fx.particles(serverWorld, ParticleTypes.POOF, player.getPos().add(0.0, 1.0, 0.0), 30, 0.5, 0.05);
			}
			if (traits.eatLaunch > 0) {
				player.addVelocity(0.0, traits.eatLaunch, 0.0);
				player.velocityModified = true;
				player.fallDistance = 0.0F;
				Fx.particles(serverWorld, ParticleTypes.FLAME, player.getPos(), 60, 0.3, 0.1);
				Fx.sound(serverWorld, player.getPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 0.8F);
			}
		} catch (Throwable t) {
			ItemCombinator.LOGGER.error("Food ability failed", t);
		}
	}

	private static boolean perform(Use action, ServerWorld world, ServerPlayerEntity player, Traits traits, Hand hand) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		switch (action) {
			// ---------------------------------------------------------- projectiles
			case SMALL_FIREBALL: {
				SmallFireballEntity fireball = new SmallFireballEntity(world, player, look);
				fireball.setPosition(eye.x + look.x, eye.y - 0.2, eye.z + look.z);
				world.spawnEntity(fireball);
				Fx.sound(world, eye, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 1.0F);
				return true;
			}
			case BIG_FIREBALL: {
				FireballEntity fireball = new FireballEntity(world, player, look, Math.round(traits.power));
				fireball.setPosition(eye.x + look.x * 1.5, eye.y - 0.2, eye.z + look.z * 1.5);
				world.spawnEntity(fireball);
				Fx.sound(world, eye, SoundEvents.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
				return true;
			}
			case WITHER_SKULL:
				return Spells.witherSkull(world, player);
			case ARROW_BURST:
				return Spells.arrowBurst(world, player, Math.round(traits.power));
			case BEAM:
				return Spells.beam(world, player, traits.range, traits.power);
			case DYNAMITE: {
				TntEntity tnt = new TntEntity(world, eye.x + look.x, eye.y - 0.3, eye.z + look.z, player);
				tnt.setFuse(40);
				tnt.setVelocity(look.multiply(traits.power));
				world.spawnEntity(tnt);
				Fx.sound(world, eye, SoundEvents.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
				return true;
			}
			case MEGA_BOMB:
				return Spells.megaBomb(world, player, traits.power);
			case NUKE:
				return Spells.nuke(world, player, traits.power);

			// ---------------------------------------------------------- weather and disasters
			case FROST_CONE: {
				double half = traits.range / 2.0;
				Vec3d center = eye.add(look.multiply(half));
				Box box = new Box(center.x - half, center.y - half, center.z - half, center.x + half, center.y + half, center.z + half);
				for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
					Vec3d to = target.getPos().subtract(player.getPos());
					if (to.lengthSquared() > 2.0 && to.normalize().dotProduct(look) < 0.3) {
						continue; // behind or beside the player
					}
					target.damage(world.getDamageSources().indirectMagic(player, player), traits.power);
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 3), player);
					target.setFrozenTicks(target.getMinFreezeDamageTicks() + 100);
				}
				Fx.particles(world, ParticleTypes.SNOWFLAKE, center, 120, half * 0.5, 0.02);
				Fx.sound(world, eye, SoundEvents.BLOCK_GLASS_BREAK, 1.0F, 1.4F);
				return true;
			}
			case LIGHTNING:
				return strike(world, player, Fx.lookPoint(world, player, traits.range));
			case LIGHTNING_STORM: {
				Vec3d target = Fx.lookPoint(world, player, traits.range);
				strike(world, player, target);
				for (int i = 0; i < 5; i++) {
					double dx = (world.random.nextDouble() - 0.5) * 9.0;
					double dz = (world.random.nextDouble() - 0.5) * 9.0;
					BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(target.x + dx, target.y, target.z + dz));
					// Stay close to the target height so the storm also works in caves.
					double y = Math.abs(top.getY() - target.y) <= 6.0 ? top.getY() : target.y;
					strike(world, player, new Vec3d(target.x + dx, y, target.z + dz));
				}
				return true;
			}
			case METEOR:
				return Spells.meteor(world, player, Fx.lookPoint(world, player, traits.range), traits.power);
			case METEOR_SHOWER:
				return Spells.meteorShower(world, player, Fx.lookPoint(world, player, traits.range));
			case BLACK_HOLE: {
				BlockHitResult hit = Fx.ray(player, traits.range);
				// Open it a little in front of the wall, or in mid-air at full range.
				Vec3d center = hit.getType() == HitResult.Type.BLOCK ? hit.getPos().subtract(look.multiply(1.5)) : hit.getPos();
				return Spells.blackHole(world, center);
			}
			case QUAKE:
				return Spells.quake(world, player);
			case FLOOD:
				return Spells.flood(world, Fx.lookPoint(world, player, traits.range));
			case DRAIN:
				return Spells.drain(world, player);
			case FREEZE_AREA:
				return Spells.freezeArea(world, player, traits.range);
			case ANVIL_STORM:
				return Spells.anvilStorm(world, Fx.lookPoint(world, player, traits.range));
			case CHICKEN_STORM:
				return Spells.chickenStorm(world, Fx.lookPoint(world, player, traits.range));

			// ---------------------------------------------------------- movement
			case BLINK:
				return blink(world, player, traits.range);
			case LEAP: {
				player.addVelocity(look.x * traits.power, look.y * traits.power * 0.8 + 0.35, look.z * traits.power);
				player.velocityModified = true;
				player.fallDistance = 0.0F;
				Fx.particles(world, ParticleTypes.CLOUD, player.getPos(), 20, 0.3, 0.05);
				Fx.sound(world, eye, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.8F, 1.3F);
				return true;
			}
			case RECALL:
				return recall(world, player);
			case SWAP:
				return Spells.swap(world, player, traits.range);
			case GHOST:
				return Spells.ghost(world, player, Math.round(traits.power * 20.0F));

			// ---------------------------------------------------------- mobs
			case POLYMORPH:
				return Spells.polymorph(world, player, traits.range);
			case SHRINK:
				return Spells.resize(world, player, traits.range, false);
			case GROW:
				return Spells.resize(world, player, traits.range, true);
			case MADNESS:
				return Spells.madness(world, player, traits.range);
			case SUMMON_GOLEMS:
				return Spells.summonGolems(world, player, Math.round(traits.power));
			case LIFT:
				return Spells.gravity(world, player, traits.range, true);
			case SLAM:
				return Spells.gravity(world, player, traits.range, false);
			case GLOW_SCAN: {
				Box box = player.getBoundingBox().expand(traits.range);
				for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
					target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 300, 0, false, false));
				}
				Fx.sound(world, eye, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
				return true;
			}

			// ---------------------------------------------------------- world and utility
			case HEAL: {
				player.heal(traits.power);
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
				Fx.particles(world, ParticleTypes.HEART, eye, 8, 0.5, 0.0);
				Fx.sound(world, eye, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7F, 1.6F);
				return true;
			}
			case ENDER_POUCH: {
				player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
						(syncId, inventory, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, p.getEnderChestInventory()),
						Text.translatable("container.enderchest")));
				Fx.sound(world, eye, SoundEvents.BLOCK_ENDER_CHEST_OPEN, 0.6F, 1.0F);
				return true;
			}
			case WORKBENCH: {
				BlockPos here = player.getBlockPos();
				player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
						(syncId, inventory, p) -> new PocketCraftingScreenHandler(syncId, inventory, ScreenHandlerContext.create(world, here)),
						Text.translatable("container.crafting")));
				return true;
			}
			case TIME_DAY:
			case TIME_NIGHT: {
				ServerWorld overworld = world.getServer().getOverworld();
				long now = overworld.getTimeOfDay();
				long target = (now / 24000L) * 24000L + (action == Use.TIME_NIGHT ? 13000L : 1000L);
				if (target <= now) {
					target += 24000L;
				}
				overworld.setTimeOfDay(target);
				Fx.sound(world, eye, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8F, 1.5F);
				return true;
			}
			case TIME_STOP:
				return Spells.timeStop(world, player, Math.round(traits.power * 20.0F));
			case XP:
				return Spells.xp(world, player, Math.round(traits.power));
			case DUPE:
				return Spells.duplicate(world, player, hand);
			case BRIDGE:
				return Spells.bridge(world, player, traits.range);
			case HOLE:
				return Spells.hole(world, player, traits.range, 160);
			case DOWSE:
				return Spells.dowse(world, player, traits.range);
			case FOREST:
				return Spells.forest(world, Fx.lookPoint(world, player, traits.range));
			case LASER_DRILL:
				return Spells.laserDrill(world, player, traits.range);
			case CHAOS:
				return Chaos.one(world, player);
			case PANDORA:
				return Chaos.pandora(world, player);
			default:
				return false;
		}
	}

	/** A real lightning bolt at the target. The player gets the credit for what it kills. */
	static boolean strike(ServerWorld world, ServerPlayerEntity player, Vec3d target) {
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
		if (bolt == null) {
			return false;
		}
		bolt.refreshPositionAfterTeleport(target);
		bolt.setChanneler(player);
		world.spawnEntity(bolt);
		return true;
	}

	private static boolean blink(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		BlockPos base;
		if (hit.getType() == HitResult.Type.BLOCK) {
			base = hit.getBlockPos().offset(hit.getSide());
		} else {
			base = BlockPos.ofFloored(hit.getPos());
		}
		// Try the spot itself, then one block up and one block down, and take the first with room for the player.
		for (int dy : new int[] {0, 1, -1}) {
			BlockPos spot = base.up(dy);
			Vec3d dest = Vec3d.ofBottomCenter(spot);
			Box box = player.getBoundingBox().offset(dest.subtract(player.getPos()));
			if (world.isSpaceEmpty(player, box)) {
				Vec3d from = player.getPos();
				Fx.particles(world, ParticleTypes.PORTAL, from.add(0.0, 1.0, 0.0), 30, 0.4, 0.2);
				Fx.sound(world, from, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				player.requestTeleport(dest.x, dest.y, dest.z);
				player.fallDistance = 0.0F;
				Fx.particles(world, ParticleTypes.PORTAL, dest.add(0.0, 1.0, 0.0), 30, 0.4, 0.2);
				Fx.sound(world, dest, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
				return true;
			}
		}
		return false;
	}

	private static boolean recall(ServerWorld world, ServerPlayerEntity player) {
		ServerWorld destWorld = null;
		BlockPos spawn = player.getSpawnPointPosition();
		if (spawn != null) {
			destWorld = world.getServer().getWorld(player.getSpawnPointDimension());
		}
		if (spawn == null || destWorld == null) {
			destWorld = world.getServer().getOverworld();
			spawn = destWorld.getSpawnPos();
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
		player.teleport(destWorld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
		player.fallDistance = 0.0F;
		Fx.sound(destWorld, player.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
		return true;
	}
}
