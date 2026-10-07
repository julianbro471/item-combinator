package com.combinator.ability;

import com.combinator.ItemCombinator;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Pure chaos: the Staff of Wild Magic, the Dice of Fate, Musical Chairs, the Party Cannon and the Gremlin in a Jar.
 */
public final class Wild {
	/**
	 * Right-click abilities the Staff of Wild Magic never casts: they need a special item in the hand, or they are this staff.
	 * The cataclysms are left out too: the staff keeps the spells it had when it was made, newer items do not add to it.
	 */
	private static final Set<Use> NOT_WILD = EnumSet.of(Use.NONE, Use.BACKPACK, Use.SMELT_HAND, Use.FISH, Use.WAYPOINT_GO, Use.WAYPOINT_SET,
			Use.BANK_IN, Use.BANK_OUT, Use.ENCHANT_BOOK, Use.DUPE, Use.ENDER_POUCH, Use.WORKBENCH, Use.CHAOS, Use.PANDORA, Use.WILD_MAGIC,
			Use.DICE, Use.DOOM_CANCEL, Use.COPY_PASTE, Use.COPY_CORNER,
			Use.CHUNK_ERASE, Use.REGION_ERASE, Use.CHUNK_INVERT, Use.CHUNK_LAUNCH, Use.CARPET_BOMB, Use.ANNIHILATE, Use.TSAR_BOMBA,
			Use.EVENT_HORIZON, Use.FAULT_LINE,
			// The fifth wave is not part of the staff's spell book.
			Use.POCKET_DIMENSION, Use.POCKET_GROUP, Use.BACKROOMS, Use.SKY_REALM, Use.MOON, Use.PARALLEL, Use.DIMENSION_HOP, Use.BANISH,
			Use.WORMHOLE_BLUE, Use.WORMHOLE_ORANGE, Use.GRAVE_WARP, Use.ESCAPE, Use.ELEVATOR_UP, Use.ELEVATOR_DOWN, Use.BRACELET_GO,
			Use.BRACELET_PULL, Use.WANDER, Use.PET_RECALL, Use.BLUEPRINT, Use.BLUEPRINT_PICK, Use.BIOME_PAINT, Use.BIOME_PICK, Use.RAIL_LINE,
			Use.QUARRY, Use.SORT_SELF, Use.ENDER_MAIL, Use.LIGHT_UP);
	/** Every spell the Staff of Wild Magic can cast. */
	public static final List<Use> SPELLS = new ArrayList<>();

	static {
		for (Use use : Use.values()) {
			if (!NOT_WILD.contains(use)) {
				SPELLS.add(use);
			}
		}
	}

	/** How many different pranks the gremlin knows. */
	public static final int PRANKS = 12;

	private static Use forcedSpell = null;
	private static int forcedRoll = 0;

	private Wild() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(Wild::tick);
	}

	/** Makes the next cast of the Staff of Wild Magic this spell. Only for the automatic tests. */
	public static void forceNextSpell(Use spell) {
		forcedSpell = spell;
	}

	/** Makes the next throw of the Dice of Fate this number. Only for the automatic tests. */
	public static void forceNextRoll(int roll) {
		forcedRoll = roll;
	}

	// ------------------------------------------------------------------ Staff of Wild Magic

	static boolean wildMagic(ServerWorld world, ServerPlayerEntity player, Hand hand) {
		Use spell = forcedSpell != null ? forcedSpell : SPELLS.get(world.random.nextInt(SPELLS.size()));
		forcedSpell = null;
		player.sendMessage(Text.literal("Wild magic: " + niceName(spell)).formatted(Formatting.LIGHT_PURPLE), true);
		Fx.particles(world, ParticleTypes.WITCH, player.getPos().add(0.0, 1.0, 0.0), 30, 0.5, 0.1);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.0F);
		if (!UseAbilities.perform(spell, world, player, spellTraits(spell), hand)) {
			player.sendMessage(Text.literal("Wild magic: " + niceName(spell) + " fizzles.").formatted(Formatting.DARK_PURPLE), true);
		}
		return true; // the staff is used even when the spell fizzles
	}

	/** "LIGHTNING_STORM" becomes "lightning storm". */
	static String niceName(Use use) {
		return use.name().toLowerCase().replace('_', ' ');
	}

	/** Range and power for a wild spell, close to what the item with that spell has. */
	private static Traits spellTraits(Use spell) {
		Traits traits = new Traits().range(48).power(3.0F);
		switch (spell) {
			case ARROW_BURST -> traits.power(8.0F);
			case BEAM -> traits.power(14.0F);
			case MEGA_BOMB -> traits.power(8.0F);
			case NUKE -> traits.power(14.0F);
			case HEAL -> traits.power(8.0F);
			case DYNAMITE -> traits.power(1.2F);
			case LEAP -> traits.power(1.6F);
			case FROST_CONE -> traits.range(8).power(4.0F);
			case BOOMERANG -> traits.range(18).power(5.0F);
			case SUMMON_WOLVES -> traits.power(2.0F);
			case SUMMON_GOLEMS -> traits.power(2.0F);
			case GHOST -> traits.power(5.0F);
			case TIME_STOP -> traits.power(5.0F);
			case XP -> traits.power(3.0F);
			case LIGHTNING_RING -> traits.range(10).power(8.0F);
			case RAILGUN -> traits.range(96).power(20.0F);
			case SNOW_ARMY -> traits.power(6.0F);
			case FLOOR_IS_LAVA, RING_OF_FIRE, FORCE_FIELD -> traits.range(6);
			case TSUNAMI -> traits.range(24);
			case SNAP -> traits.range(32);
			case MITOSIS_BURST -> traits.range(8);
			case DISCO -> traits.range(10);
			case FANGS -> traits.range(16);
			case HOURGLASS -> traits.power(10.0F);
			default -> {
			}
		}
		return traits;
	}

	// ------------------------------------------------------------------ Dice of Fate

	static boolean dice(ServerWorld world, ServerPlayerEntity player) {
		int roll = forcedRoll > 0 ? forcedRoll : 1 + world.random.nextInt(20);
		forcedRoll = 0;
		fate(world, player, roll);
		return true;
	}

	/** What a roll of the Dice of Fate does: 1 is the worst, 20 the best. Returns the text shown. The tests call every number. */
	public static String fate(ServerWorld world, ServerPlayerEntity player, int roll) {
		Vec3d here = player.getPos();
		Random random = world.random;
		String text;
		switch (roll) {
			case 1 -> {
				text = "Doom. A Wither rises.";
				Entity wither = Spells.spawn(world, EntityType.WITHER, here.x + 6.0, here.y + 1.0, here.z);
				if (wither instanceof WitherEntity boss) {
					boss.onSummoned();
				}
			}
			case 2 -> {
				text = "Lightning, and creepers.";
				UseAbilities.strike(world, player, here);
				for (int i = 0; i < 3; i++) {
					BlockPos spot = Fx.groundNear(world, here.x + (random.nextDouble() - 0.5) * 12.0, here.z + (random.nextDouble() - 0.5) * 12.0, here.y, 6);
					Spells.spawn(world, EntityType.CREEPER, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				}
			}
			case 3 -> {
				text = "You catch fire.";
				player.setOnFireFor(6.0F);
			}
			case 4 -> {
				text = "Angry bees.";
				Mayhem.bees(world, here.add(0.0, 2.0, 0.0), 6);
			}
			case 5 -> {
				text = "Butterfingers. Your hotbar falls out.";
				PlayerInventory inventory = player.getInventory();
				for (int i = 0; i < 9; i++) {
					ItemStack stack = inventory.getStack(i);
					if (i != inventory.selectedSlot && !stack.isEmpty()) {
						player.dropItem(stack.copy(), true, false);
						inventory.setStack(i, ItemStack.EMPTY);
					}
				}
			}
			case 6 -> {
				text = "Darkness and dizziness.";
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
			}
			case 7 -> {
				text = "A short flight.";
				player.addVelocity(0.0, 2.0, 0.0);
				player.velocityModified = true;
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 300, 0));
			}
			case 8 -> {
				text = "Zombies!";
				for (int i = 0; i < 4; i++) {
					double angle = i * Math.PI / 2.0;
					BlockPos spot = Fx.groundNear(world, here.x + Math.cos(angle) * 5.0, here.z + Math.sin(angle) * 5.0, here.y, 4);
					Spells.spawn(world, EntityType.ZOMBIE, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				}
			}
			case 9 -> {
				text = "Somewhere else.";
				BlockPos spot = Fx.standingSpot(world, here, 60.0);
				if (spot != null) {
					player.requestTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
					player.fallDistance = 0.0F;
				}
			}
			case 10 -> text = "Nothing happens. The dice is bored.";
			case 11 -> {
				text = "A snack.";
				player.getInventory().offerOrDrop(new ItemStack(Items.GOLDEN_CARROT, 8));
			}
			case 12 -> {
				text = "Five levels.";
				player.addExperienceLevels(5);
			}
			case 13 -> {
				text = "Speed and jumps for a minute.";
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200, 1));
			}
			case 14 -> {
				text = "It rains iron.";
				rain(world, here, Items.IRON_INGOT, 16);
			}
			case 15 -> {
				text = "Two loyal wolves.";
				Gadgets.summonWolves(world, player, 2);
			}
			case 16 -> {
				text = "It rains gold.";
				rain(world, here, Items.GOLD_INGOT, 16);
			}
			case 17 -> {
				text = "It rains diamonds.";
				rain(world, here, Items.DIAMOND, 6);
			}
			case 18 -> {
				text = "A random item of this mod.";
				player.getInventory().offerOrDrop(new ItemStack(ComboItems.ALL.get(random.nextInt(ComboItems.ALL.size()))));
			}
			case 19 -> {
				text = "A totem and an enchanted golden apple.";
				player.getInventory().offerOrDrop(new ItemStack(Items.TOTEM_OF_UNDYING));
				player.getInventory().offerOrDrop(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
			}
			default -> {
				text = "CRITICAL SUCCESS!";
				rain(world, here, Items.NETHERITE_INGOT, 2);
				rain(world, here, Items.DIAMOND, 12);
				player.addExperienceLevels(30);
				List<Item> wildest = new ArrayList<>();
				for (Item item : ComboItems.ALL) {
					if (ComboItems.rarity(item) == 3) {
						wildest.add(item);
					}
				}
				if (!wildest.isEmpty()) {
					player.getInventory().offerOrDrop(new ItemStack(wildest.get(random.nextInt(wildest.size()))));
				}
				fireworks(world, player, 5, true);
			}
		}
		Formatting color = roll <= 5 ? Formatting.RED : roll <= 10 ? Formatting.GRAY : roll < 20 ? Formatting.GREEN : Formatting.GOLD;
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(3, 40, 10));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(text).formatted(color)));
		player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(String.valueOf(roll)).formatted(color, Formatting.BOLD)));
		if (roll == 1) {
			Fx.sound(world, here, SoundEvents.ENTITY_WITHER_SPAWN, 1.0F, 1.0F);
		} else if (roll >= 20) {
			Fx.sound(world, here, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
		} else {
			Fx.sound(world, here, SoundEvents.BLOCK_BONE_BLOCK_PLACE, 1.0F, 1.0F);
		}
		return text;
	}

	private static void rain(ServerWorld world, Vec3d center, Item item, int count) {
		Random random = world.random;
		for (int i = 0; i < count; i++) {
			world.spawnEntity(new ItemEntity(world, center.x + (random.nextDouble() - 0.5) * 6.0, center.y + 8.0 + random.nextDouble() * 4.0,
					center.z + (random.nextDouble() - 0.5) * 6.0, new ItemStack(item)));
		}
	}

	// ------------------------------------------------------------------ Musical Chairs

	/** Everything alive nearby, the player too, moves to the place of another one. Nobody stays where they were. */
	static boolean musicalChairs(ServerWorld world, ServerPlayerEntity player, int range) {
		List<LivingEntity> seated = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range),
				e -> e.isAlive() && !e.isSpectator() && !e.hasVehicle() && !(e instanceof EnderDragonEntity) && !(e instanceof ArmorStandEntity)));
		if (seated.size() < 2) {
			player.sendMessage(Text.literal("Musical chairs needs at least two. Bring a friend, or a cow."), true);
			return false;
		}
		Random random = world.random;
		for (int i = seated.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			LivingEntity swap = seated.get(i);
			seated.set(i, seated.get(j));
			seated.set(j, swap);
		}
		List<Vec3d> seats = new ArrayList<>();
		for (LivingEntity living : seated) {
			seats.add(living.getPos());
		}
		// everybody takes the seat of the next one in the shuffled list, so nobody keeps their own
		for (int i = 0; i < seated.size(); i++) {
			LivingEntity living = seated.get(i);
			Vec3d seat = seats.get((i + 1) % seats.size());
			living.requestTeleport(seat.x, seat.y, seat.z);
			living.fallDistance = 0.0F;
			Fx.particles(world, ParticleTypes.NOTE, seat.add(0.0, living.getHeight() + 0.3, 0.0), 3, 0.3, 0.5);
		}
		int[] tune = {0, 4, 7, 12};
		Tasks.repeat(tune.length, 3, i -> Fx.sound(world, player.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP, 1.5F,
				(float) Math.pow(2.0, (tune[i] - 6) / 12.0)));
		return true;
	}

	// ------------------------------------------------------------------ Party Cannon

	static boolean party(ServerWorld world, ServerPlayerEntity player) {
		fireworks(world, player, 7, false);
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, player.getEyePos().add(player.getRotationVec(1.0F).multiply(2.0)), 40, 1.0, 0.1);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0F, 0.8F);
		player.sendMessage(Text.literal("Party!").formatted(Formatting.LIGHT_PURPLE), true);
		return true;
	}

	/** Exploding fireworks in a fan where the player looks, or straight up. */
	static void fireworks(ServerWorld world, ServerPlayerEntity player, int count, boolean up) {
		Random random = world.random;
		for (int i = 0; i < count; i++) {
			float yaw = player.getYaw() + (i - (count - 1) / 2.0F) * 8.0F;
			float pitch = up ? -80.0F + random.nextFloat() * 20.0F : player.getPitch() - 5.0F - random.nextFloat() * 10.0F;
			FireworkRocketEntity rocket = new FireworkRocketEntity(world, rocket(random), player, player.getX(), player.getEyeY() - 0.15, player.getZ(), true);
			rocket.setVelocity(player, pitch, yaw, 0.0F, 1.6F, 1.0F);
			world.spawnEntity(rocket);
		}
	}

	private static ItemStack rocket(Random random) {
		DyeColor[] colors = DyeColor.values();
		FireworkExplosionComponent.Type[] shapes = FireworkExplosionComponent.Type.values();
		FireworkExplosionComponent explosion = new FireworkExplosionComponent(shapes[random.nextInt(shapes.length)],
				IntList.of(colors[random.nextInt(colors.length)].getFireworkColor(), colors[random.nextInt(colors.length)].getFireworkColor()),
				IntList.of(colors[random.nextInt(colors.length)].getFireworkColor()), true, random.nextBoolean());
		ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
		stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(explosion)));
		return stack;
	}

	/** Sneak: cakes fall from the sky around the player. */
	static boolean cakeRain(ServerWorld world, ServerPlayerEntity player, int count) {
		Tasks.repeat(count, 2, i -> {
			if (!player.isAlive() || player.getWorld() != world) {
				return;
			}
			Random random = world.random;
			BlockPos pos = BlockPos.ofFloored(player.getX() + (random.nextDouble() - 0.5) * 12.0, player.getY() + 12.0 + random.nextInt(6),
					player.getZ() + (random.nextDouble() - 0.5) * 12.0);
			if (world.getBlockState(pos).isAir()) {
				FallingBlockEntity.spawnFromBlock(world, pos, Blocks.CAKE.getDefaultState());
			}
		});
		player.sendMessage(Text.literal("Happy birthday!").formatted(Formatting.LIGHT_PURPLE), true);
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
		return true;
	}

	// ------------------------------------------------------------------ Gremlin in a Jar

	private static void tick(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			try {
				if (!player.isAlive() || player.isSpectator() || player.getRandom().nextInt(20) != 0) {
					continue;
				}
				if (PassiveAbilities.has(player, traits -> traits.gremlin)) {
					prank(player.getServerWorld(), player, player.getRandom().nextInt(PRANKS));
				}
			} catch (Throwable t) {
				ItemCombinator.error("Gremlin prank failed", t);
			}
		}
	}

	private static final List<RegistryEntry<StatusEffect>> SILLY_EFFECTS = List.of(StatusEffects.SPEED, StatusEffects.SLOWNESS,
			StatusEffects.JUMP_BOOST, StatusEffects.NAUSEA, StatusEffects.GLOWING, StatusEffects.LEVITATION, StatusEffects.INVISIBILITY,
			StatusEffects.NIGHT_VISION, StatusEffects.BLINDNESS);

	/** One prank of the gremlin (0 to PRANKS - 1). Returns what happened. The tests call every number. */
	public static String prank(ServerWorld world, ServerPlayerEntity player, int number) {
		Random random = world.random;
		Vec3d here = player.getPos();
		BlockPos feet = player.getBlockPos();
		String text;
		switch (number) {
			case 0 -> {
				text = "The gremlin swaps your items around.";
				PlayerInventory inventory = player.getInventory();
				int a = random.nextInt(9);
				int b = (a + 1 + random.nextInt(8)) % 9;
				ItemStack first = inventory.getStack(a);
				inventory.setStack(a, inventory.getStack(b));
				inventory.setStack(b, first);
			}
			case 1 -> {
				text = "The gremlin lights a firecracker.";
				Fx.particles(world, ParticleTypes.EXPLOSION, here.add(0.0, 0.5, 0.0), 1, 0.0, 0.0);
				Fx.sound(world, here, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);
			}
			case 2 -> {
				text = "The gremlin pulls a chicken out of nowhere.";
				Spells.spawn(world, EntityType.CHICKEN, here.x + 1.0, here.y + 1.0, here.z);
			}
			case 3 -> {
				text = "Hic!";
				player.addVelocity(0.0, 0.45, 0.0);
				player.velocityModified = true;
				Fx.sound(world, here, SoundEvents.ENTITY_PLAYER_BURP, 1.0F, 1.6F);
			}
			case 4 -> {
				RegistryEntry<StatusEffect> effect = SILLY_EFFECTS.get(random.nextInt(SILLY_EFFECTS.size()));
				boolean short_ = effect == StatusEffects.LEVITATION || effect == StatusEffects.BLINDNESS;
				player.addStatusEffect(new StatusEffectInstance(effect, short_ ? 30 : 120, 0));
				text = "The gremlin casts a silly spell on you.";
			}
			case 5 -> {
				text = "The gremlin plays with matches.";
				LivingEntity nearest = null;
				double best = Double.MAX_VALUE;
				for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(10.0), e -> e != player && e.isAlive())) {
					double distance = living.squaredDistanceTo(player);
					if (distance < best) {
						best = distance;
						nearest = living;
					}
				}
				if (nearest != null) {
					nearest.setOnFireFor(4.0F);
				} else {
					BlockPos spot = feet.offset(player.getHorizontalFacing(), 2);
					if (world.getBlockState(spot).isAir() && AbstractFireBlock.canPlaceAt(world, spot, Direction.UP)) {
						world.setBlockState(spot, AbstractFireBlock.getState(world, spot), Block.NOTIFY_ALL);
					}
				}
			}
			case 6 -> {
				text = "The gremlin leaves you a cake.";
				BlockPos spot = feet.offset(player.getHorizontalFacing());
				BlockState cake = Blocks.CAKE.getDefaultState();
				if (world.getBlockState(spot).isAir() && cake.canPlaceAt(world, spot)) {
					world.setBlockState(spot, cake, Block.NOTIFY_ALL);
				} else {
					player.getInventory().offerOrDrop(new ItemStack(Items.CAKE));
				}
			}
			case 7 -> {
				text = "The gremlin eats some of your food. You feel hungry.";
				player.getHungerManager().addExhaustion(12.0F);
			}
			case 8 -> {
				text = "The gremlin screams.";
				Fx.sound(world, here, SoundEvents.ENTITY_GOAT_SCREAMING_AMBIENT, 2.0F, 1.0F);
			}
			case 9 -> {
				text = "The gremlin plays with the doors.";
				int flipped = 0;
				for (BlockPos pos : BlockPos.iterateOutwards(feet, 6, 3, 6)) {
					if (flipped >= 4) {
						break;
					}
					BlockState state = world.getBlockState(pos);
					if (!state.contains(Properties.OPEN)) {
						continue;
					}
					if (state.getBlock() instanceof DoorBlock door) {
						if (state.contains(DoorBlock.HALF) && state.get(DoorBlock.HALF) == net.minecraft.block.enums.DoubleBlockHalf.LOWER) {
							door.setOpen(player, world, state, pos.toImmutable(), !door.isOpen(state));
							flipped++;
						}
					} else if (state.getBlock() instanceof TrapdoorBlock || state.getBlock() instanceof FenceGateBlock) {
						world.setBlockState(pos.toImmutable(), state.cycle(Properties.OPEN), Block.NOTIFY_ALL);
						flipped++;
					}
				}
			}
			case 10 -> {
				text = "The gremlin steals your light.";
				int taken = 0;
				for (BlockPos pos : BlockPos.iterateOutwards(feet, 6, 3, 6)) {
					if (taken >= 4) {
						break;
					}
					BlockState state = world.getBlockState(pos);
					if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH) || state.isOf(Blocks.LANTERN) || state.isOf(Blocks.SOUL_TORCH)
							|| state.isOf(Blocks.SOUL_WALL_TORCH) || state.isOf(Blocks.SOUL_LANTERN)) {
						world.breakBlock(pos.toImmutable(), true);
						taken++;
					}
				}
			}
			default -> {
				text = "The gremlin spins you around.";
				player.teleport(world, player.getX(), player.getY(), player.getZ(), player.getYaw() + 180.0F, player.getPitch());
			}
		}
		player.sendMessage(Text.literal(text).formatted(Formatting.DARK_GREEN), true);
		Fx.sound(world, here, SoundEvents.ENTITY_WITCH_CELEBRATE, 0.6F, 1.8F);
		return text;
	}
}
