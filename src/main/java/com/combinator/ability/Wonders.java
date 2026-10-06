package com.combinator.ability;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * The second wave: evoker fangs, a dragon staff, a storm of tridents, a horde, a paint bomb, a bedrock breaker,
 * a giant tree, a copy-paste wand, homing shulker bullets, an hourglass that speeds up the world and a monster magnet.
 */
public final class Wonders {
	private static final String COPY_KEY = "combinator_copy";
	/** The biggest area the Copy-Paste Wand copies, in blocks. */
	public static final int COPY_LIMIT = 16384;

	private static final Block[] PAINT = {
			Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.YELLOW_CONCRETE,
			Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE,
			Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE
	};

	/** The random tick speed before the Hourglass of Ages changed it, or -1 while no hourglass runs. */
	private static int savedTickSpeed = -1;
	/** Counts the uses of the hourglass. Only the timer of the newest use puts the tick speed back. */
	private static int hourglassUses = 0;

	private record Copied(BlockPos offset, BlockState state) {
	}

	private Wonders() {
	}

	public static void register() {
		// The tick speed is saved with the world. If the world closes while an hourglass runs, put it back first.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			try {
				restoreTickSpeed(server);
			} catch (Throwable t) {
				ItemCombinator.error("Hourglass reset failed", t);
			}
		});
	}

	private static Vec3d flatLook(ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0.0, look.z);
		return flat.lengthSquared() < 1.0E-4 ? new Vec3d(0.0, 0.0, 1.0) : flat.normalize();
	}

	// ------------------------------------------------------------------ Fang Staff

	/** A line of evoker fangs along the ground, one after another, towards where the player looks. */
	static boolean fangs(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d dir = flatLook(player);
		float yaw = (float) Math.atan2(dir.z, dir.x);
		for (int i = 0; i < range; i++) {
			double distance = 1.25 * (i + 1);
			BlockPos ground = Fx.groundNear(world, player.getX() + dir.x * distance, player.getZ() + dir.z * distance, player.getY(), 4);
			world.spawnEntity(new EvokerFangsEntity(world, player.getX() + dir.x * distance, ground.getY(), player.getZ() + dir.z * distance, yaw, i, player));
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.0F);
		return true;
	}

	/** Sneak: two rings of fangs around the player, like an evoker that is cornered. */
	static boolean fangRing(ServerWorld world, ServerPlayerEntity player) {
		for (int ring = 0; ring < 2; ring++) {
			int count = ring == 0 ? 6 : 10;
			double radius = ring == 0 ? 1.6 : 2.8;
			for (int i = 0; i < count; i++) {
				double angle = i * Math.PI * 2.0 / count + ring * 0.3;
				double x = player.getX() + Math.cos(angle) * radius;
				double z = player.getZ() + Math.sin(angle) * radius;
				BlockPos ground = Fx.groundNear(world, x, z, player.getY(), 3);
				world.spawnEntity(new EvokerFangsEntity(world, x, ground.getY(), z, (float) angle, ring * 3, player));
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, 1.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Dragon Staff

	static boolean dragonFireball(ServerWorld world, ServerPlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		Vec3d eye = player.getEyePos();
		DragonFireballEntity fireball = new DragonFireballEntity(world, player, look);
		fireball.setPosition(eye.x + look.x * 1.5, eye.y - 0.2 + look.y * 1.5, eye.z + look.z * 1.5);
		world.spawnEntity(fireball);
		Fx.sound(world, eye, SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Poseidon's Wrath

	/** Tridents fall from the sky on the spot the player looks at. Every fourth one brings lightning. */
	static boolean tridentStorm(ServerWorld world, ServerPlayerEntity player, int range) {
		Vec3d target = Fx.lookPoint(world, player, range);
		IntConsumer drop = i -> {
			Random random = world.random;
			double x = target.x + (random.nextDouble() - 0.5) * 10.0;
			double z = target.z + (random.nextDouble() - 0.5) * 10.0;
			TridentEntity trident = new TridentEntity(world, player, new ItemStack(Items.TRIDENT));
			trident.setPosition(x, target.y + 22.0, z);
			trident.setVelocity(0.0, -2.5, 0.0);
			trident.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
			world.spawnEntity(trident);
			if (i % 4 == 0) {
				BlockPos ground = Fx.groundNear(world, x, z, target.y, 8);
				Tasks.later(8, () -> UseAbilities.strike(world, player, Vec3d.ofBottomCenter(ground)));
			}
		};
		drop.accept(0);
		Tasks.repeat(15, 2, i -> drop.accept(i + 1));
		Fx.sound(world, target, SoundEvents.ITEM_TRIDENT_THUNDER, 3.0F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Horde Horn

	/** Monsters rise from the ground in a ring around the player and go for the player. */
	static boolean horde(ServerWorld world, ServerPlayerEntity player) {
		Random random = world.random;
		List<EntityType<?>> horde = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			horde.add(EntityType.ZOMBIE);
		}
		for (int i = 0; i < 6; i++) {
			horde.add(EntityType.SKELETON);
		}
		for (int i = 0; i < 4; i++) {
			horde.add(EntityType.CREEPER);
		}
		for (int i = 0; i < horde.size(); i++) {
			double angle = i * Math.PI * 2.0 / horde.size() + random.nextDouble() * 0.2;
			double distance = 8.0 + random.nextDouble() * 6.0;
			BlockPos ground = Fx.groundNear(world, player.getX() + Math.cos(angle) * distance, player.getZ() + Math.sin(angle) * distance, player.getY(), 6);
			Entity entity = Spells.spawn(world, horde.get(i), ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
			if (entity instanceof MobEntity mob) {
				if (horde.get(i) == EntityType.SKELETON) {
					mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
				}
				mob.setTarget(player);
			}
			if (entity != null) {
				Fx.particles(world, ParticleTypes.SOUL, entity.getPos(), 10, 0.4, 0.05);
			}
		}
		Fx.sound(world, player.getPos(), SoundEvents.EVENT_RAID_HORN, 4.0F, 0.7F);
		player.sendMessage(Text.literal("The horde rises.").formatted(Formatting.DARK_RED), true);
		return true;
	}

	// ------------------------------------------------------------------ Rainbow Paint Bomb

	static boolean paintBomb(ServerWorld world, ServerPlayerEntity player) {
		Spells.throwBomb(world, player, 1.2F, 30, at -> paint(world, at, 5));
		return true;
	}

	/** Every solid block in a ball becomes concrete of a random colour, and every sheep gets a random colour. */
	static void paint(ServerWorld world, Vec3d at, int radius) {
		Random random = world.random;
		BlockPos center = BlockPos.ofFloored(at);
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
			if (pos.getSquaredDistance(center) > radius * radius) {
				continue;
			}
			BlockState state = world.getBlockState(pos);
			if (state.isSolidBlock(world, pos) && !state.hasBlockEntity() && state.getHardness(world, pos) >= 0.0F) {
				world.setBlockState(pos.toImmutable(), PAINT[random.nextInt(PAINT.length)].getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
		DyeColor[] colors = DyeColor.values();
		for (SheepEntity sheep : world.getEntitiesByClass(SheepEntity.class, Box.of(at, radius * 2.0, radius * 2.0, radius * 2.0), e -> e.isAlive())) {
			sheep.setColor(colors[random.nextInt(colors.length)]);
		}
		Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, at, 120, radius * 0.5, 0.1);
		Fx.particles(world, ParticleTypes.EXPLOSION, at, 2, 0.5, 0.0);
		Fx.sound(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 1.8F);
		Fx.sound(world, at, SoundEvents.ITEM_DYE_USE, 2.0F, 1.0F);
	}

	// ------------------------------------------------------------------ Bedrock Breaker

	/** Right-click on a block that cannot be mined (bedrock, barriers ...): it breaks and drops itself. */
	static ActionResult breakBedrock(ItemUsageContext context, PlayerEntity player, ItemStack stack) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		if (state.isAir() || state.getHardness(world, pos) >= 0.0F || state.hasBlockEntity()) {
			return ActionResult.PASS;
		}
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.SUCCESS;
		}
		Fx.particles(serverWorld, ParticleTypes.EXPLOSION, Vec3d.ofCenter(pos), 2, 0.3, 0.0);
		world.setBlockState(pos, Fx.AIR, Block.NOTIFY_ALL);
		ItemStack drop = new ItemStack(state.getBlock().asItem());
		if (!drop.isEmpty()) {
			Block.dropStack(world, pos, drop);
		}
		Fx.sound(world, Vec3d.ofCenter(pos), SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0F, 0.6F);
		stack.damage(10, player, LivingEntity.getSlotForHand(context.getHand()));
		player.getItemCooldownManager().set(stack.getItem(), 10);
		return ActionResult.SUCCESS;
	}

	// ------------------------------------------------------------------ World Tree Seed

	/** A giant oak: a 5 x 5 trunk, roots, branches, and round clouds of leaves. It grows from the bottom up. */
	static boolean worldTree(ServerWorld world, ServerPlayerEntity player, int range) {
		BlockPos ground = BlockPos.ofFloored(Fx.lookPoint(world, player, range));
		BlockPos base = Fx.canFill(world.getBlockState(ground)) ? ground : ground.up();
		int height = Math.min(40, world.getTopY() - base.getY() - 12);
		if (height < 16) {
			player.sendMessage(Text.literal("No room for a tree here."), true);
			return false;
		}
		BlockState log = Blocks.OAK_LOG.getDefaultState();
		BlockState leaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
		Random random = world.random;
		long seed = random.nextLong();
		// the trunk, one layer per tick
		IntConsumer trunk = h -> {
			int r = h < 3 ? 3 : 2;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (dx * dx + dz * dz <= r * r + 1) {
						put(world, base.add(dx, h, dz), log);
					}
				}
			}
			if (h % 4 == 0) {
				Fx.sound(world, Vec3d.ofCenter(base.up(h)), SoundEvents.BLOCK_WOOD_PLACE, 2.0F, 0.5F);
			}
		};
		trunk.accept(0);
		Tasks.repeat(height - 1, 1, i -> trunk.accept(i + 1));
		// roots, then branches and leaves when the trunk is done
		Tasks.later(height + 2, () -> {
			Random r = Random.create(seed);
			for (int i = 0; i < 6; i++) {
				double angle = i * Math.PI / 3.0 + r.nextDouble() * 0.4;
				for (int step = 2; step < 7; step++) {
					BlockPos pos = base.add((int) Math.round(Math.cos(angle) * step), -step / 3, (int) Math.round(Math.sin(angle) * step));
					put(world, pos, log);
				}
			}
			for (int i = 0; i < 6; i++) {
				double angle = i * Math.PI / 3.0 + r.nextDouble() * 0.5;
				int start = height / 2 + r.nextInt(height / 3);
				BlockPos end = base.up(start);
				for (int step = 1; step <= 6; step++) {
					end = base.add((int) Math.round(Math.cos(angle) * step), start + step / 2, (int) Math.round(Math.sin(angle) * step));
					put(world, end, log);
				}
				leafBall(world, end, 3 + r.nextInt(2), leaves);
			}
			leafBall(world, base.up(height), 7, leaves);
			Fx.sound(world, Vec3d.ofCenter(base.up(height)), SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 3.0F, 0.6F);
			Fx.particles(world, ParticleTypes.HAPPY_VILLAGER, Vec3d.ofCenter(base.up(height)), 120, 6.0, 0.0);
		});
		return true;
	}

	private static void leafBall(ServerWorld world, BlockPos center, int radius, BlockState leaves) {
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
			if (pos.getSquaredDistance(center) <= radius * radius + 1 && world.getRandom().nextInt(10) != 0) {
				put(world, pos.toImmutable(), leaves);
			}
		}
	}

	private static void put(ServerWorld world, BlockPos pos, BlockState state) {
		if (Fx.canFill(world.getBlockState(pos))) {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
		}
	}

	// ------------------------------------------------------------------ Copy-Paste Wand

	private static NbtCompound copyData(ItemStack wand) {
		return wand.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getCompound(COPY_KEY);
	}

	/** Sneak: remembers the block the player looks at as a corner. The first click sets corner 1, the next corner 2. */
	static boolean copyCorner(ServerWorld world, ServerPlayerEntity player, ItemStack wand, int range) {
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.literal("Look at a block to mark a corner."), true);
			return false;
		}
		BlockPos pos = hit.getBlockPos();
		NbtCompound data = copyData(wand);
		boolean second = data.contains("a") && !data.contains("b");
		data.putIntArray(second ? "b" : "a", new int[] {pos.getX(), pos.getY(), pos.getZ()});
		if (!second) {
			data.remove("b");
		}
		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, wand, nbt -> nbt.put(COPY_KEY, data));
		if (second) {
			int[] a = data.getIntArray("a");
			int volume = (Math.abs(a[0] - pos.getX()) + 1) * (Math.abs(a[1] - pos.getY()) + 1) * (Math.abs(a[2] - pos.getZ()) + 1);
			player.sendMessage(Text.literal("Corner 2 set. The copy has " + volume + " blocks"
					+ (volume > COPY_LIMIT ? ", too many (at most " + COPY_LIMIT + ")." : ".")).formatted(Formatting.AQUA), true);
		} else {
			player.sendMessage(Text.literal("Corner 1 set. Sneak + right-click the opposite corner.").formatted(Formatting.AQUA), true);
		}
		Fx.particles(world, ParticleTypes.END_ROD, Vec3d.ofCenter(pos), 12, 0.4, 0.02);
		Fx.sound(world, Vec3d.ofCenter(pos), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, second ? 1.4F : 1.0F);
		return true;
	}

	/** Right-click: copies what is between the two corners right now and puts it on the block the player looks at. */
	static boolean paste(ServerWorld world, ServerPlayerEntity player, ItemStack wand, int range) {
		NbtCompound data = copyData(wand);
		int[] a = data.getIntArray("a");
		int[] b = data.getIntArray("b");
		if (a.length != 3 || b.length != 3) {
			player.sendMessage(Text.literal("First mark two corners: sneak + right-click two blocks."), true);
			return false;
		}
		BlockHitResult hit = Fx.ray(player, range);
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendMessage(Text.literal("Look at the block where the copy should go."), true);
			return false;
		}
		BlockPos min = new BlockPos(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]));
		BlockPos max = new BlockPos(Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2]));
		long volume = (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
		if (volume > COPY_LIMIT) {
			player.sendMessage(Text.literal("The copy has " + volume + " blocks. At most " + COPY_LIMIT + " work."), true);
			return false;
		}
		// read everything first, so a copy that overlaps its original still comes out right
		List<Copied> copied = new ArrayList<>();
		for (BlockPos pos : BlockPos.iterate(min, max)) {
			BlockState state = world.getBlockState(pos);
			if (!state.isAir() && !state.hasBlockEntity()) {
				copied.add(new Copied(pos.subtract(min), state));
			}
		}
		BlockPos origin = hit.getBlockPos().offset(hit.getSide());
		int placed = 0;
		for (Copied c : copied) {
			BlockPos pos = origin.add(c.offset());
			BlockState there = world.getBlockState(pos);
			if (!world.isInBuildLimit(pos) || there.getHardness(world, pos) < 0.0F || there.hasBlockEntity()) {
				continue;
			}
			world.setBlockState(pos, c.state(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			placed++;
		}
		Fx.particles(world, ParticleTypes.END_ROD, Vec3d.ofCenter(origin), 60, 2.0, 0.05);
		Fx.sound(world, Vec3d.ofCenter(origin), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 2.0F, 1.0F);
		player.sendMessage(Text.literal("Pasted " + placed + " blocks.").formatted(Formatting.AQUA), true);
		return placed > 0;
	}

	// ------------------------------------------------------------------ Shulker Blaster

	/** Homing shulker bullets at the four nearest mobs. A hit makes the mob float up. */
	static boolean shulkerBullets(ServerWorld world, ServerPlayerEntity player, int range) {
		List<LivingEntity> targets = new ArrayList<>(world.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(range),
				e -> e != player && e.isAlive() && !(e instanceof PlayerEntity) && !(e instanceof ArmorStandEntity)));
		if (targets.isEmpty()) {
			player.sendMessage(Text.literal("Nothing to shoot at."), true);
			return false;
		}
		targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)));
		Direction.Axis[] axes = {Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
		for (int i = 0; i < Math.min(4, targets.size()); i++) {
			world.spawnEntity(new ShulkerBulletEntity(world, player, targets.get(i), axes[i % 3]));
		}
		Fx.sound(world, player.getPos(), SoundEvents.ENTITY_SHULKER_SHOOT, 1.5F, 1.0F);
		return true;
	}

	// ------------------------------------------------------------------ Hourglass of Ages

	/** The whole world ticks a hundred times faster for a while: crops grow, grass spreads, fire runs wild. */
	static boolean hourglass(ServerWorld world, ServerPlayerEntity player, int seconds) {
		MinecraftServer server = world.getServer();
		GameRules.IntRule rule = server.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
		if (savedTickSpeed < 0) {
			savedTickSpeed = rule.get();
		}
		rule.set(Math.max(savedTickSpeed, 3) * 100, server);
		int use = ++hourglassUses;
		Tasks.later(seconds * 20, () -> {
			if (use == hourglassUses) {
				restoreTickSpeed(server);
			}
		});
		player.sendMessage(Text.literal("Time runs a hundred times faster.").formatted(Formatting.GOLD), true);
		Fx.particles(world, ParticleTypes.TOTEM_OF_UNDYING, player.getPos().add(0.0, 1.0, 0.0), 80, 2.0, 0.3);
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 2.0F, 2.0F);
		return true;
	}

	private static void restoreTickSpeed(MinecraftServer server) {
		if (savedTickSpeed >= 0) {
			server.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(savedTickSpeed, server);
			savedTickSpeed = -1;
		}
	}

	// ------------------------------------------------------------------ Monster Magnet

	/** Pulls every mob within range to the player, for one second. */
	static boolean monsterMagnet(ServerWorld world, ServerPlayerEntity player, int range) {
		List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(range),
				e -> e.isAlive() && !(e instanceof EnderDragonEntity));
		if (mobs.isEmpty()) {
			player.sendMessage(Text.literal("No mobs to pull."), true);
			return false;
		}
		IntConsumer pull = i -> {
			if (!player.isAlive() || player.getWorld() != world) {
				return;
			}
			for (MobEntity mob : mobs) {
				if (!mob.isAlive()) {
					continue;
				}
				Vec3d to = player.getPos().subtract(mob.getPos());
				double distance = to.length();
				if (distance < 2.0) {
					continue;
				}
				Vec3d v = to.multiply(Math.min(2.0, distance * 0.25) / distance);
				mob.setVelocity(v.x, Math.max(v.y, 0.0) + 0.25, v.z);
				mob.velocityModified = true;
			}
		};
		pull.accept(0);
		Tasks.repeat(19, 1, i -> pull.accept(i + 1));
		Fx.sound(world, player.getPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0F, 0.6F);
		player.sendMessage(Text.literal(mobs.size() + " mobs are coming to you.").formatted(Formatting.RED), true);
		return true;
	}

}
