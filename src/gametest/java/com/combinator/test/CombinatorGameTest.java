package com.combinator.test;

import com.combinator.ItemCombinator;
import com.combinator.ability.Capture;
import com.combinator.ability.Charms;
import com.combinator.ability.Chaos;
import com.combinator.ability.CombatAbilities;
import com.combinator.ability.Doom;
import com.combinator.ability.Mayhem;
import com.combinator.ability.Oddities;
import com.combinator.ability.Wild;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import com.combinator.recipe.ComboRecipes;
import com.combinator.screen.BackpackScreenHandler;
import com.combinator.screen.CombinerScreenHandler;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatic tests. They run on a server without a real player (GitHub starts them after every change).
 * A fake player crafts every combination, mines, fights, eats, wears and right-clicks with every item.
 * A test fails if the game throws an error or if an item does not do what its abilities say.
 *
 * Every test builds its own small stage high up in the air, so the tests cannot disturb each other.
 */
public class CombinatorGameTest implements FabricGameTest {
	private static final Logger LOG = LoggerFactory.getLogger("combinator-test");
	/** Height of the stage floor. */
	private static final int STAGE_Y = 100;
	/**
	 * The tests wait this long before they start, so the area around the fake player is fully loaded.
	 * It also has to be longer than 60 ticks: a player who just joined cannot be hurt for 3 seconds.
	 */
	private static final int WARMUP = 70;
	private static int playerNumber = 0;

	// ------------------------------------------------------------------ small helpers

	/** Collects everything that went wrong in one test. */
	private static final class Report {
		private final String test;
		private final List<String> problems = new ArrayList<>();
		private int errorsSeen = ItemCombinator.ERRORS.size();

		Report(String test) {
			this.test = test;
		}

		void problem(String text) {
			this.problems.add(text);
			LOG.error("[{}] PROBLEM: {}", this.test, text);
		}

		void info(String text) {
			LOG.info("[{}] {}", this.test, text);
		}

		void check(boolean ok, String textIfNot) {
			if (!ok) {
				this.problem(textIfNot);
			}
		}

		/** Picks up errors that the mod caught and logged since the last call. */
		void modErrors(String during) {
			List<String> all = ItemCombinator.ERRORS;
			while (this.errorsSeen < all.size()) {
				this.problem(during + " -> " + all.get(this.errorsSeen));
				this.errorsSeen++;
			}
		}

		void finish(TestContext context) {
			this.modErrors("end of test");
			if (this.problems.isEmpty()) {
				LOG.info("[{}] PASSED", this.test);
				context.complete();
				return;
			}
			LOG.error("[{}] FAILED with {} problem(s)", this.test, this.problems.size());
			throw new GameTestException(this.test + ": " + this.problems.size() + " problem(s). First: " + this.problems.get(0));
		}
	}

	private static String name(Item item) {
		return Registries.ITEM.getId(item).getPath();
	}

	private static BlockPos stageCenter(TestContext context) {
		BlockPos corner = context.getAbsolutePos(new BlockPos(4, 1, 4));
		return new BlockPos(corner.getX(), STAGE_Y, corner.getZ());
	}

	/** Keeps the area around the stage loaded, with or without a player. */
	private static void forceChunks(ServerWorld world, BlockPos center, boolean forced) {
		ChunkPos middle = new ChunkPos(center);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				world.setChunkForced(middle.x + dx, middle.z + dz, forced);
			}
		}
	}

	/** True when the stage is loaded so far that mobs and dropped items in it take part in the game. */
	private static boolean stageReady(ServerWorld world, BlockPos center) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (!world.shouldTickEntity(center.add(dx * 16, 0, dz * 16))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Calls the body once per tick, starting when the warm-up time is over and the stage is fully loaded.
	 * The body gets 1 on its first call, 2 on the second, and so on.
	 */
	private static void everyTickWhenReady(TestContext context, ServerWorld world, BlockPos center, IntConsumer body) {
		int[] waited = {0};
		int[] running = {0};
		context.runAtEveryTick(() -> {
			if (running[0] == 0) {
				waited[0]++;
				if (waited[0] < WARMUP || !stageReady(world, center)) {
					if (waited[0] > 10000) {
						throw new GameTestException("test setup: the test area did not finish loading");
					}
					return;
				}
			}
			running[0]++;
			body.accept(running[0]);
		});
	}

	/** Runs the action once, when the warm-up time is over and the stage is fully loaded. */
	private static void whenReady(TestContext context, ServerWorld world, BlockPos center, Runnable action) {
		everyTickWhenReady(context, world, center, tick -> {
			if (tick == 1) {
				action.run();
			}
		});
	}

	/** Removes blocks, mobs and items around the stage and builds a fresh grass floor. */
	private static void resetStage(ServerWorld world, BlockPos center) {
		BlockState air = Blocks.AIR.getDefaultState();
		for (BlockPos pos : BlockPos.iterate(center.add(-16, -5, -16), center.add(16, 24, 16))) {
			if (!world.getBlockState(pos).isAir()) {
				world.setBlockState(pos, air, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}
		BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
		for (BlockPos pos : BlockPos.iterate(center.add(-12, -1, -12), center.add(12, -1, 12))) {
			world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}
		// Mobs and dropped items go away. Flying things (bombs, fireballs, lightning) stay, so they can finish their job.
		Box box = new Box(center).expand(40.0);
		for (Entity entity : world.getOtherEntities(null, box, e -> !(e instanceof PlayerEntity)
				&& (e instanceof LivingEntity || e instanceof ItemEntity || e instanceof ExperienceOrbEntity))) {
			entity.discard();
		}
	}

	private static void fill(ServerWorld world, BlockPos from, BlockPos to, BlockState state) {
		for (BlockPos pos : BlockPos.iterate(from, to)) {
			world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
		}
	}

	private static int count(ServerWorld world, BlockPos from, BlockPos to, Block block) {
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(from, to)) {
			if (world.getBlockState(pos).isOf(block)) {
				count++;
			}
		}
		return count;
	}

	/** A fake player in survival mode, connected like a real one, but with nobody at the keyboard. */
	private static ServerPlayerEntity newPlayer(ServerWorld world, BlockPos center) {
		MinecraftServer server = world.getServer();
		GameProfile profile = new GameProfile(UUID.randomUUID(), "tester" + (++playerNumber));
		ServerPlayerEntity player = new ServerPlayerEntity(server, world, profile, SyncedClientOptions.createDefault());
		ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerManager().onPlayerConnect(connection, player, ConnectedClientData.createDefault(profile, false));
		player.changeGameMode(GameMode.SURVIVAL);
		place(player, world, center);
		return player;
	}

	/** Puts the player on the middle of the stage, looking forward (towards +Z) and a little down. */
	private static void place(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
		player.teleport(world, center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.0F, 20.0F);
		player.setYaw(0.0F);
		player.setPitch(20.0F);
		player.setHeadYaw(0.0F);
		player.setVelocity(Vec3d.ZERO);
		player.fallDistance = 0.0F;
	}

	/** Brings the player back to a clean state: survival mode, full health, empty hands, on the stage. */
	private static void resetPlayer(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
		if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
			player.changeGameMode(GameMode.SURVIVAL);
		}
		if (player.currentScreenHandler != player.playerScreenHandler) {
			player.closeHandledScreen();
		}
		player.clearActiveItem();
		player.setSneaking(false);
		player.stopRiding();
		player.getInventory().clear();
		player.clearStatusEffects();
		player.setHealth(player.getMaxHealth());
		player.setFireTicks(0);
		// Forget the last hit. For half a second after a hit the game ignores weaker hits.
		player.timeUntilRegen = 0;
		player.hurtTime = 0;
		place(player, world, center);
	}

	private static void removePlayer(ServerPlayerEntity player) {
		try {
			MinecraftServer server = player.getServer();
			if (server != null) {
				server.getPlayerManager().remove(player);
			}
		} catch (Throwable t) {
			LOG.warn("Could not remove the fake player", t);
		}
	}

	private static void hold(ServerPlayerEntity player, ItemStack stack) {
		player.getInventory().selectedSlot = 0;
		player.getInventory().setStack(0, stack);
	}

	/** Puts an item where its passive abilities work: armor is worn, everything else goes into the main hand. */
	private static void equip(ServerPlayerEntity player, ItemStack stack) {
		Traits traits = ComboItems.traits(stack.getItem());
		if (stack.getItem() instanceof ArmorItem armor && (traits == null || traits.where == Traits.Where.WORN)) {
			player.equipStack(armor.getType().getEquipmentSlot(), stack);
		} else {
			hold(player, stack);
		}
	}

	private static MobEntity spawnMob(ServerWorld world, EntityType<? extends MobEntity> type, double x, double y, double z) {
		MobEntity mob = type.create(world);
		if (mob == null) {
			throw new GameTestException("could not create " + type);
		}
		mob.refreshPositionAndAngles(x, y, z, 180.0F, 0.0F);
		mob.setAiDisabled(true);
		mob.setPersistent();
		world.spawnEntity(mob);
		return mob;
	}

	private static BlockHitResult hitTop(BlockPos pos) {
		return new BlockHitResult(Vec3d.ofCenter(pos).add(0.0, 0.5, 0.0), Direction.UP, pos, false);
	}

	/** How many of an item lie on the ground near the stage plus how many the player carries. */
	private static int have(ServerWorld world, ServerPlayerEntity player, BlockPos center, Item item) {
		int count = player.getInventory().count(item);
		for (ItemEntity entity : world.getEntitiesByClass(ItemEntity.class, new Box(center).expand(24.0), e -> e.isAlive())) {
			if (entity.getStack().isOf(item)) {
				count += entity.getStack().getCount();
			}
		}
		return count;
	}

	private static int onGround(ServerWorld world, BlockPos center, Item item) {
		int count = 0;
		for (ItemEntity entity : world.getEntitiesByClass(ItemEntity.class, new Box(center).expand(24.0), e -> e.isAlive())) {
			if (entity.getStack().isOf(item)) {
				count += entity.getStack().getCount();
			}
		}
		return count;
	}

	private static void clearDrops(ServerWorld world, BlockPos center) {
		for (Entity entity : world.getOtherEntities(null, new Box(center).expand(30.0),
				e -> e instanceof ItemEntity || e instanceof ExperienceOrbEntity)) {
			entity.discard();
		}
	}

	private static TagKey<Item> itemTag(String path) {
		return TagKey.of(RegistryKeys.ITEM, Identifier.ofVanilla(path));
	}

	/** The first combined item whose abilities match. The test fails if there is none. */
	private static Item find(String what, Predicate<Traits> wanted) {
		for (Item item : ComboItems.ALL) {
			Traits traits = ComboItems.traits(item);
			if (traits != null && wanted.test(traits)) {
				return item;
			}
		}
		throw new GameTestException("no item has the ability: " + what);
	}

	/** How many stacks the player carries, both hands included. */
	private static int carried(ServerPlayerEntity player) {
		int stacks = 0;
		for (int i = 0; i < player.getInventory().size(); i++) {
			if (!player.getInventory().getStack(i).isEmpty()) {
				stacks++;
			}
		}
		return stacks;
	}

	/** How many of an item are stored inside a backpack. */
	private static int holds(ItemStack bag, Item item) {
		int count = 0;
		for (ItemStack stack : bag.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).iterateNonEmpty()) {
			if (stack.isOf(item)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	/** The glowing ore outlines of the Divining Rod near the stage: the name of the block each one shows. */
	private static List<String> oreOutlines(ServerWorld world, BlockPos center) {
		List<String> shown = new ArrayList<>();
		for (Entity entity : world.getOtherEntities(null, new Box(center).expand(40.0), e -> e.getCommandTags().contains("combinator_ore_sight"))) {
			String block = entity.writeNbt(new NbtCompound()).getCompound("block_state").getString("Name");
			shown.add(entity.isGlowing() ? block : block + " (not glowing)");
		}
		return shown;
	}

	/** Runs one part of a test. A crash in it is written down and the other parts still run. */
	private static void step(Report report, String what, Runnable body) {
		try {
			body.run();
		} catch (Throwable t) {
			report.problem(what + " crashed: " + t);
			LOG.error("test step: " + what, t);
		}
		report.modErrors(what);
	}

	// ------------------------------------------------------------------ 1. combinations

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_recipes", tickLimit = 20000)
	public void recipes(TestContext context) {
		Report report = new Report("recipes");
		report.info(ComboItems.ALL.size() + " items, " + ComboRecipes.ALL.size() + " combinations (with alternates)");
		report.check(ComboItems.ALL.size() >= 100, "only " + ComboItems.ALL.size() + " items are registered");
		report.check(ComboRecipes.ALL.size() >= 100, "only " + ComboRecipes.ALL.size() + " combinations are registered");

		for (Item item : ComboItems.ALL) {
			Identifier id = Registries.ITEM.getId(item);
			report.check(id.getNamespace().equals(ItemCombinator.MOD_ID), "item is not registered: " + item);
			try {
				ItemStack stack = new ItemStack(item);
				stack.getName().getString();
				stack.getMaxCount();
				stack.getRarity();
			} catch (Throwable t) {
				report.problem("cannot make a stack of " + id + ": " + t);
			}
		}

		Set<Item> craftable = new HashSet<>();
		for (ComboRecipes.Combo combo : ComboRecipes.ALL) {
			String label = name(combo.a()) + " + " + name(combo.b());
			try {
				ItemStack forward = ComboRecipes.craft(new ItemStack(combo.a()), new ItemStack(combo.b()));
				ItemStack backward = ComboRecipes.craft(new ItemStack(combo.b()), new ItemStack(combo.a()));
				report.check(forward.isOf(combo.result()) && forward.getCount() == combo.count(),
						label + " gives " + forward + " instead of " + combo.count() + "x " + name(combo.result()));
				report.check(ItemStack.areEqual(forward, backward), label + " gives something else when the two items are swapped");
				craftable.add(combo.result());
			} catch (Throwable t) {
				report.problem(label + " crashed: " + t);
			}
		}
		for (Item item : ComboItems.ALL) {
			if (item != ComboItems.CHAOS_ORB && !craftable.contains(item)) {
				report.problem("no combination makes " + name(item));
			}
		}

		// Two stackable items without a combination give a Chaos Orb. Tools never do.
		if (ComboRecipes.find(Items.DIRT, Items.FEATHER) == null) {
			ItemStack orb = ComboRecipes.craft(new ItemStack(Items.DIRT), new ItemStack(Items.FEATHER));
			report.check(orb.isOf(ComboItems.CHAOS_ORB), "dirt + feather should give a Chaos Orb but gives " + orb);
		}
		if (ComboRecipes.find(Items.WOODEN_HOE, Items.DIRT) == null) {
			ItemStack nothing = ComboRecipes.craft(new ItemStack(Items.WOODEN_HOE), new ItemStack(Items.DIRT));
			report.check(nothing.isEmpty(), "wooden hoe + dirt should give nothing but gives " + nothing);
		}
		report.check(ComboRecipes.craft(ItemStack.EMPTY, new ItemStack(Items.DIRT)).isEmpty(), "one empty slot should give nothing");

		// Enchantments move from the inputs to the result.
		try {
			RegistryEntry<Enchantment> efficiency = context.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT)
					.getEntry(RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.ofVanilla("efficiency"))).orElseThrow();
			boolean tested = false;
			for (ComboRecipes.Combo combo : ComboRecipes.ALL) {
				if (combo.a() == Items.DIAMOND_PICKAXE && combo.result() instanceof PickaxeItem) {
					ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
					pick.addEnchantment(efficiency, 5);
					ItemStack out = ComboRecipes.craft(pick, new ItemStack(combo.b()));
					report.check(out.getEnchantments().getLevel(efficiency) == 5,
							"Efficiency V was lost when making " + name(combo.result()));
					tested = true;
					break;
				}
			}
			report.check(tested, "found no pickaxe combination to test enchantments with");
		} catch (Throwable t) {
			report.problem("enchantment carry-over crashed: " + t);
		}
		report.finish(context);
	}

	// ------------------------------------------------------------------ 2. data files

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_data", tickLimit = 20000)
	public void dataFiles(TestContext context) {
		Report report = new Report("data");
		ServerWorld world = context.getWorld();
		MinecraftServer server = world.getServer();

		Optional<RecipeEntry<?>> recipe = server.getRecipeManager().get(ItemCombinator.id("combiner_table"));
		report.check(recipe.isPresent(), "the crafting recipe of the Combiner Table did not load");
		if (recipe.isPresent()) {
			ItemStack result = recipe.get().value().getResult(world.getRegistryManager());
			report.check(result.isOf(ItemCombinator.COMBINER_TABLE_ITEM), "the Combiner Table recipe gives " + result);
		}
		report.check(server.getAdvancementLoader().get(ItemCombinator.id("recipes/combiner_table")) != null,
				"the recipe unlock advancement did not load");

		BlockPos pos = stageCenter(context);
		List<ItemStack> drops = Block.getDroppedStacks(ItemCombinator.COMBINER_TABLE.getDefaultState(), world, pos, null);
		report.check(drops.size() == 1 && drops.get(0).isOf(ItemCombinator.COMBINER_TABLE_ITEM),
				"a broken Combiner Table drops " + drops + " instead of itself");

		TagKey<Block> ores = TagKey.of(RegistryKeys.BLOCK, ItemCombinator.id("ores"));
		TagKey<Block> paxel = TagKey.of(RegistryKeys.BLOCK, ItemCombinator.id("mineable/paxel"));
		TagKey<Block> axe = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("mineable/axe"));
		report.check(Blocks.IRON_ORE.getDefaultState().isIn(ores) && Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState().isIn(ores)
				&& Blocks.ANCIENT_DEBRIS.getDefaultState().isIn(ores), "the ore list (tag combinator:ores) is missing ores");
		report.check(!Blocks.STONE.getDefaultState().isIn(ores), "stone is in the ore list");
		report.check(Blocks.STONE.getDefaultState().isIn(paxel) && Blocks.DIRT.getDefaultState().isIn(paxel)
				&& Blocks.OAK_LOG.getDefaultState().isIn(paxel), "the paxel block list (tag combinator:mineable/paxel) is incomplete");
		report.check(ItemCombinator.COMBINER_TABLE.getDefaultState().isIn(axe), "the Combiner Table is not mined faster with an axe");

		TagKey<Item> durability = itemTag("enchantable/durability");
		for (Item item : ComboItems.ALL) {
			ItemStack stack = new ItemStack(item);
			String tag = null;
			if (item instanceof SwordItem) {
				tag = "swords";
			} else if (item instanceof PickaxeItem) {
				tag = "pickaxes";
			} else if (item instanceof AxeItem) {
				tag = "axes";
			} else if (item instanceof ShovelItem) {
				tag = "shovels";
			} else if (item instanceof HoeItem) {
				tag = "hoes";
			} else if (item instanceof ArmorItem armor) {
				EquipmentSlot slot = armor.getType().getEquipmentSlot();
				tag = slot == EquipmentSlot.HEAD ? "head_armor" : slot == EquipmentSlot.CHEST ? "chest_armor"
						: slot == EquipmentSlot.LEGS ? "leg_armor" : "foot_armor";
			}
			if (tag != null) {
				report.check(stack.isIn(itemTag(tag)), name(item) + " is not in the item list minecraft:" + tag + " (it could not be enchanted)");
			}
			if (stack.isDamageable()) {
				report.check(stack.isIn(durability), name(item) + " wears out but cannot get Unbreaking or Mending");
			}
		}
		report.finish(context);
	}

	// ------------------------------------------------------------------ 3. the Combiner Table

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_table", tickLimit = 20000)
	public void combinerTable(TestContext context) {
		Report report = new Report("table");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			try {
				BlockPos tablePos = center.add(0, 0, 2);
				world.setBlockState(tablePos, ItemCombinator.COMBINER_TABLE.getDefaultState());
				ActionResult opened = player.interactionManager.interactBlock(player, world, ItemStack.EMPTY, Hand.MAIN_HAND, hitTop(tablePos));
				report.info("right-click on the table: " + opened);
				if (!(player.currentScreenHandler instanceof CombinerScreenHandler handler)) {
					report.problem("right-clicking the Combiner Table did not open its screen");
				} else {
					report.check(handler.canUse(player), "the table screen closes at once (canUse is false)");
					tableChecks(report, player, handler);
					player.closeHandledScreen();
				}
			} catch (Throwable t) {
				report.problem("the Combiner Table crashed: " + t);
				LOG.error("table test", t);
			}
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	private static void tableChecks(Report report, ServerPlayerEntity player, CombinerScreenHandler handler) {
		// One click on the result: one item of each input is used up.
		ComboRecipes.Combo first = ComboRecipes.ALL.get(0);
		handler.getSlot(CombinerScreenHandler.SLOT_A).setStack(new ItemStack(first.a()));
		report.check(handler.getSlot(CombinerScreenHandler.SLOT_OUT).getStack().isEmpty(), "one item alone already gives a result");
		report.check(!ComboRecipes.partners(first.a()).isEmpty(), "the recipe help has no partner for " + name(first.a()));
		handler.getSlot(CombinerScreenHandler.SLOT_B).setStack(new ItemStack(first.b()));
		ItemStack shown = handler.getSlot(CombinerScreenHandler.SLOT_OUT).getStack();
		report.check(shown.isOf(first.result()), "the result slot shows " + shown + " instead of " + name(first.result()));
		handler.onSlotClick(CombinerScreenHandler.SLOT_OUT, 0, SlotActionType.PICKUP, player);
		report.check(handler.getCursorStack().isOf(first.result()), "taking the result gives " + handler.getCursorStack());
		report.check(handler.getSlot(CombinerScreenHandler.SLOT_A).getStack().isEmpty() && handler.getSlot(CombinerScreenHandler.SLOT_B).getStack().isEmpty(),
				"the inputs were not used up");
		report.check(handler.getSlot(CombinerScreenHandler.SLOT_OUT).getStack().isEmpty(), "the result slot is not empty after the inputs are gone");
		handler.setCursorStack(ItemStack.EMPTY);
		player.getInventory().clear();

		// Shift-click: combines as often as there are inputs.
		ComboRecipes.Combo stackable = null;
		for (ComboRecipes.Combo combo : ComboRecipes.ALL) {
			if (combo.a() != combo.b() && new ItemStack(combo.a()).getMaxCount() >= 3 && new ItemStack(combo.b()).getMaxCount() >= 3
					&& combo.a().getRecipeRemainder() == null && combo.b().getRecipeRemainder() == null) {
				stackable = combo;
				break;
			}
		}
		if (stackable == null) {
			report.problem("found no combination of two stackable items for the shift-click test");
		} else {
			String label = name(stackable.a()) + " + " + name(stackable.b());
			handler.getSlot(CombinerScreenHandler.SLOT_A).setStack(new ItemStack(stackable.a(), 3));
			handler.getSlot(CombinerScreenHandler.SLOT_B).setStack(new ItemStack(stackable.b(), 5));
			handler.onSlotClick(CombinerScreenHandler.SLOT_OUT, 0, SlotActionType.QUICK_MOVE, player);
			int made = player.getInventory().count(stackable.result());
			report.check(made == 3 * stackable.count(), "shift-click with 3x " + label + " made " + made + " instead of " + 3 * stackable.count());
			report.check(handler.getSlot(CombinerScreenHandler.SLOT_A).getStack().isEmpty(), "shift-click left items in the first slot");
			report.check(handler.getSlot(CombinerScreenHandler.SLOT_B).getStack().getCount() == 2, "shift-click should leave 2 items in the second slot");
			player.getInventory().clear();
			handler.getSlot(CombinerScreenHandler.SLOT_B).setStack(ItemStack.EMPTY);

			// Shift-click from the inventory into the table.
			player.getInventory().setStack(9, new ItemStack(stackable.a(), 2));
			handler.onSlotClick(3, 0, SlotActionType.QUICK_MOVE, player);
			report.check(handler.getSlot(CombinerScreenHandler.SLOT_A).getStack().isOf(stackable.a()), "shift-click from the inventory does not fill the first slot");

			// Closing the screen gives the inputs back.
			player.closeHandledScreen();
			report.check(player.getInventory().count(stackable.a()) == 2, "closing the table did not give the inputs back");
			player.getInventory().clear();
		}
	}

	// ------------------------------------------------------------------ 4. mining

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_mining", tickLimit = 20000)
	public void mining(TestContext context) {
		Report report = new Report("mining");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			int tested = 0;
			for (Item item : ComboItems.ALL) {
				Traits traits = ComboItems.traits(item);
				ItemStack probe = new ItemStack(item);
				boolean tool = probe.isSuitableFor(Blocks.STONE.getDefaultState()) || probe.isSuitableFor(Blocks.OAK_PLANKS.getDefaultState())
						|| probe.isSuitableFor(Blocks.CLAY.getDefaultState());
				if (!tool && (traits == null || !traits.hasMining())) {
					continue;
				}
				try {
					mineWith(report, world, player, center, item, traits);
					tested++;
				} catch (Throwable t) {
					report.problem(name(item) + ": mining crashed: " + t);
					LOG.error("mining test " + name(item), t);
				}
				report.modErrors(name(item) + " (mining)");
			}
			report.info(tested + " tools tested");
			report.check(tested >= 15, "only " + tested + " tools were found");
			resetStage(world, center);
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	private static void mineWith(Report report, ServerWorld world, ServerPlayerEntity player, BlockPos center, Item item, Traits traits) {
		String id = name(item);
		resetStage(world, center);
		resetPlayer(player, world, center);
		ItemStack tool = new ItemStack(item);
		hold(player, tool);

		// The kind of block this tool is made for.
		Block material;
		if (tool.isSuitableFor(Blocks.STONE.getDefaultState())) {
			material = Blocks.STONE;
		} else if (tool.isSuitableFor(Blocks.OAK_PLANKS.getDefaultState())) {
			material = Blocks.OAK_PLANKS;
		} else {
			material = Blocks.CLAY;
		}
		boolean boom = traits != null && traits.mineBoom > 0;

		// A 5x5x5 cube in front of the player. The middle block is mined.
		BlockPos cubeFrom = center.add(-2, 0, 3);
		BlockPos cubeTo = center.add(2, 4, 7);
		fill(world, cubeFrom, cubeTo, material.getDefaultState());
		BlockPos middle = center.add(0, 2, 5);
		player.interactionManager.tryBreakBlock(middle);
		report.check(world.getBlockState(middle).isAir(), id + ": the mined block is still there");
		int gone = 125 - count(world, cubeFrom, cubeTo, material);
		if (traits != null && traits.areaRadius > 0 && !boom) {
			int side = Math.min(5, 2 * traits.areaRadius + 1);
			int expected = traits.areaCube ? side * side * side : side * side;
			report.check(gone == expected, id + ": area mining broke " + gone + " blocks, expected " + expected);
		} else if (!boom) {
			report.check(gone == 1, id + ": broke " + gone + " blocks instead of 1");
		}
		report.check(player.getMainHandStack().isOf(item), id + ": the tool is gone after mining");

		// An ore vein of 6 blocks. Pickaxes only.
		if (material == Blocks.STONE) {
			fill(world, cubeFrom, cubeTo, Blocks.AIR.getDefaultState());
			clearDrops(world, center);
			player.getInventory().clear();
			hold(player, new ItemStack(item));
			BlockPos oreFrom = center.add(-1, 0, 3);
			BlockPos oreTo = center.add(1, 1, 3);
			fill(world, oreFrom, oreTo, Blocks.IRON_ORE.getDefaultState());
			player.interactionManager.tryBreakBlock(center.add(0, 0, 3));
			int oresLeft = count(world, oreFrom, oreTo, Blocks.IRON_ORE);
			boolean vein = traits != null && traits.veinLimit > 0;
			boolean area = traits != null && traits.areaRadius > 0;
			if (vein) {
				report.check(oresLeft == 0, id + ": vein mining left " + oresLeft + " of 6 ore blocks");
			} else if (!area && !boom) {
				report.check(oresLeft == 5, id + ": mined " + (6 - oresLeft) + " ore blocks instead of 1");
			}
			int mined = 6 - oresLeft;
			boolean smelt = traits != null && traits.smelt;
			boolean twice = traits != null && traits.doubleOres;
			boolean magnet = traits != null && traits.magnetDrops;
			boolean silk = traits != null && traits.silk;
			Item wanted = silk ? Items.IRON_ORE : smelt ? Items.IRON_INGOT : Items.RAW_IRON;
			Item unwanted = silk || smelt ? Items.RAW_IRON : Items.IRON_INGOT;
			if (!boom) {
				int got = have(world, player, center, wanted);
				int expected = mined * (twice ? 2 : 1);
				report.check(got == expected, id + ": " + mined + " iron ore gave " + got + "x " + name(wanted) + ", expected " + expected);
				report.check(have(world, player, center, unwanted) == 0, id + ": iron ore dropped " + name(unwanted));
				if (magnet) {
					report.check(onGround(world, center, wanted) == 0 && player.getInventory().count(wanted) == expected,
							id + ": the drops did not go into the inventory");
				} else {
					report.check(player.getInventory().count(wanted) == 0, id + ": drops went into the inventory without a magnet ability");
				}
			}
			if (silk) {
				// The tool is enchanted only for the moment of the break.
				report.check(player.getMainHandStack().getEnchantments().isEmpty(), id + ": the tool kept an enchantment after mining");
			}
			fill(world, oreFrom, oreTo, Blocks.AIR.getDefaultState());
		}

		// Tools that sometimes find extra things: 125 blocks of the right kind must give at least one find.
		// (The chance that an honest tool finds nothing in 125 blocks is less than one in a million.)
		if (traits != null && (traits.sift || traits.stoneNuggets > 0)) {
			fill(world, cubeFrom, cubeTo, Blocks.AIR.getDefaultState());
			clearDrops(world, center);
			player.getInventory().clear();
			ItemStack sturdy = new ItemStack(item);
			sturdy.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(false));
			hold(player, sturdy);
			Block ground = traits.sift ? Blocks.DIRT : Blocks.STONE;
			Item normalDrop = traits.sift ? Items.DIRT : Items.COBBLESTONE;
			fill(world, cubeFrom, cubeTo, ground.getDefaultState());
			for (BlockPos pos : BlockPos.iterate(cubeFrom, cubeTo)) {
				player.interactionManager.tryBreakBlock(pos.toImmutable());
			}
			report.check(count(world, cubeFrom, cubeTo, ground) == 0, id + ": could not mine all 125 test blocks");
			int normal = have(world, player, center, normalDrop);
			int finds = 0;
			for (ItemEntity entity : world.getEntitiesByClass(ItemEntity.class, new Box(center).expand(24.0), e -> e.isAlive())) {
				if (!entity.getStack().isOf(normalDrop)) {
					finds += entity.getStack().getCount();
				}
			}
			report.info(id + ": 125 blocks gave " + normal + " normal drops and " + finds + " extra finds");
			report.check(normal == 125, id + ": 125 blocks gave " + normal + " normal drops");
			report.check(finds > 0, id + ": 125 blocks gave no extra find");
			if (traits.stoneNuggets > 0) {
				report.check(have(world, player, center, Items.GOLD_NUGGET) > 0, id + ": 125 stone blocks gave no gold nugget");
			}
			fill(world, cubeFrom, cubeTo, Blocks.AIR.getDefaultState());
		}

		// A small tree: 4 logs with leaves on top. Axes only.
		if (tool.isSuitableFor(Blocks.OAK_PLANKS.getDefaultState()) || (traits != null && traits.treeLimit > 0)) {
			fill(world, cubeFrom, cubeTo, Blocks.AIR.getDefaultState());
			clearDrops(world, center);
			player.getInventory().clear();
			hold(player, new ItemStack(item));
			BlockPos trunkFrom = center.add(0, 0, 4);
			BlockPos trunkTo = center.add(0, 3, 4);
			fill(world, trunkFrom, trunkTo, Blocks.OAK_LOG.getDefaultState());
			fill(world, center.add(-1, 4, 3), center.add(1, 4, 5), Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.DISTANCE, 1));
			player.interactionManager.tryBreakBlock(trunkFrom);
			int logsLeft = count(world, trunkFrom, trunkTo, Blocks.OAK_LOG);
			if (traits != null && traits.treeLimit > 0) {
				report.check(logsLeft == 0, id + ": tree felling left " + logsLeft + " of 4 logs");
				if (traits.smelt) {
					report.check(have(world, player, center, Items.CHARCOAL) == 4, id + ": 4 logs should give 4 charcoal");
				} else {
					report.check(have(world, player, center, Items.OAK_LOG) == 4, id + ": 4 logs gave " + have(world, player, center, Items.OAK_LOG) + " logs");
				}
			} else if ((traits == null || traits.areaRadius == 0) && !boom) {
				report.check(logsLeft == 3, id + ": mined " + (4 - logsLeft) + " logs instead of 1");
				if (traits != null && traits.smelt) {
					report.check(have(world, player, center, Items.CHARCOAL) == 1, id + ": a log should give charcoal");
				}
			}
		}
	}

	// ------------------------------------------------------------------ 5. fighting

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_combat", tickLimit = 20000)
	public void combat(TestContext context) {
		Report report = new Report("combat");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			int tested = 0;
			for (Item item : ComboItems.ALL) {
				try {
					fightWith(report, world, player, center, item, ComboItems.traits(item));
					tested++;
				} catch (Throwable t) {
					report.problem(name(item) + ": fighting crashed: " + t);
					LOG.error("combat test " + name(item), t);
				}
				report.modErrors(name(item) + " (fighting)");
			}
			report.info(tested + " items tested as weapons");
			resetStage(world, center);
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	private static MobEntity[] zombies(ServerWorld world, BlockPos center) {
		double x = center.getX() + 0.5;
		double y = center.getY();
		double z = center.getZ() + 0.5;
		return new MobEntity[] {
				spawnMob(world, EntityType.ZOMBIE, x, y, z + 2.0),
				spawnMob(world, EntityType.ZOMBIE, x + 1.5, y, z + 2.5),
				spawnMob(world, EntityType.ZOMBIE, x - 1.5, y, z + 2.5)
		};
	}

	private static void fightWith(Report report, ServerWorld world, ServerPlayerEntity player, BlockPos center, Item item, Traits traits) {
		String id = name(item);
		resetStage(world, center);
		resetPlayer(player, world, center);
		player.setInvulnerable(true); // explosions and lightning from the weapon must not kill the tester
		hold(player, new ItemStack(item));

		// a) a normal swing, the way the game does it
		MobEntity[] group = zombies(world, center);
		player.attack(group[0]);

		// b) a full-strength hit
		resetStage(world, center);
		CombatAbilities.clearStrikeCooldowns();
		group = zombies(world, center);
		hold(player, new ItemStack(item));
		player.setHealth(10.0F);
		MobEntity victim = group[0];
		float before = victim.getHealth();
		boolean hurt = victim.damage(world.getDamageSources().playerAttack(player), 6.0F);
		report.check(hurt, id + ": a hit did no damage");
		if (traits != null) {
			boolean stillThere = victim.isAlive();
			if (traits.instakill) {
				report.check(!stillThere, id + ": instant kill left the zombie alive");
			}
			if (traits.midas) {
				report.check(!stillThere && (world.getBlockState(victim.getBlockPos()).isOf(Blocks.GOLD_BLOCK)
								|| have(world, player, center, Items.GOLD_BLOCK) > 0),
						id + ": the zombie did not turn into gold");
			}
			if (stillThere) {
				if (traits.igniteSeconds > 0) {
					report.check(victim.isOnFire(), id + ": the zombie does not burn");
				}
				for (Traits.Fx fx : traits.hitEffects) {
					if (fx.effect() != StatusEffects.POISON && fx.effect() != StatusEffects.REGENERATION) { // zombies are immune to these two
						report.check(victim.hasStatusEffect(fx.effect()), id + ": the zombie did not get " + fx.effect().getIdAsString());
					}
				}
				if (traits.freeze) {
					report.check(victim.getFrozenTicks() > 0, id + ": the zombie is not frozen");
				}
				if (traits.knockUp > 0) {
					report.check(victim.getVelocity().y > 0.0, id + ": the zombie was not thrown up");
				}
				if (traits.magicDamage > 0) {
					report.check(before - victim.getHealth() > 6.0F, id + ": no extra magic damage (" + (before - victim.getHealth()) + ")");
				}
			}
			if (traits.lifesteal > 0) {
				report.check(player.getHealth() > 10.0F, id + ": life steal did not heal");
			}
			if (traits.sweepRadius > 0 || traits.blastRadius > 0 || traits.chainRadius > 0) {
				report.check(group[1].getHealth() < group[1].getMaxHealth() || !group[1].isAlive(),
						id + ": the zombie next to the target took no damage");
			}
		}

		// c) a killing blow
		resetStage(world, center);
		CombatAbilities.clearStrikeCooldowns();
		group = zombies(world, center);
		hold(player, new ItemStack(item));
		player.setHealth(10.0F);
		group[0].damage(world.getDamageSources().playerAttack(player), 1000.0F);
		report.check(!group[0].isAlive(), id + ": 1000 damage did not kill a zombie");
		if (traits != null) {
			if (traits.nuggets > 0) {
				report.check(onGround(world, center, Items.GOLD_NUGGET) > 0, id + ": the kill dropped no gold nuggets");
			}
			if (traits.bonusXp > 0) {
				int orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, new Box(center).expand(10.0), e -> e.isAlive()).size();
				report.check(orbs > 0, id + ": the kill gave no bonus XP");
			}
			if (traits.lifesteal > 0) {
				report.check(player.getHealth() > 10.0F, id + ": a kill with life steal did not heal");
			}
		}
		player.setInvulnerable(false);
	}

	// ------------------------------------------------------------------ 6. armor, totem and fall protection

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_defense", tickLimit = 20000)
	public void defense(TestContext context) {
		Report report = new Report("defense");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			// First without any item: the tester must be hurt by a zombie and by a fall.
			// If this fails, the test itself is broken and the checks below would mean nothing.
			resetPlayer(player, world, center);
			player.setInvulnerable(false);
			MobEntity control = spawnMob(world, EntityType.ZOMBIE, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
			player.damage(world.getDamageSources().mobAttack(control), 2.0F);
			report.check(player.getHealth() < player.getMaxHealth(), "test setup: a zombie hit did not hurt the tester");
			resetPlayer(player, world, center);
			player.damage(world.getDamageSources().fall(), 6.0F);
			report.check(player.getHealth() < player.getMaxHealth(), "test setup: a fall did not hurt the tester");

			int tested = 0;
			for (Item item : ComboItems.ALL) {
				Traits traits = ComboItems.traits(item);
				if (traits == null || (!traits.noFall && !traits.hasThorns && traits.totemCooldown == 0 && !(item instanceof ArmorItem))) {
					continue;
				}
				String id = name(item);
				try {
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(false);
					equip(player, new ItemStack(item));

					// hit by a zombie
					MobEntity zombie = spawnMob(world, EntityType.ZOMBIE, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
					float zombieHealth = zombie.getHealth();
					boolean hit = player.damage(world.getDamageSources().mobAttack(zombie), 4.0F);
					report.check(hit, id + ": the zombie hit did not count");
					if (traits.hasThorns) {
						if (traits.thornsDamage > 0 || traits.thornsBoom > 0) {
							report.check(zombie.getHealth() < zombieHealth || !zombie.isAlive(), id + ": the attacker took no damage back");
						}
						if (traits.thornsFire > 0) {
							report.check(zombie.isOnFire() || !zombie.isAlive(), id + ": the attacker does not burn");
						}
					}
					report.check(player.isAlive(), id + ": the tester died from a 4 damage hit");

					// a long fall
					player.setHealth(player.getMaxHealth());
					player.hurtTime = 0;
					player.timeUntilRegen = 0;
					float health = player.getHealth();
					player.damage(world.getDamageSources().fall(), 6.0F);
					if (traits.noFall) {
						report.check(player.getHealth() == health, id + ": fall damage was not blocked");
					}

					// a deadly hit
					if (traits.totemCooldown > 0) {
						player.timeUntilRegen = 0;
						player.damage(world.getDamageSources().generic(), 100000.0F);
						report.check(player.isAlive() && player.getHealth() > 0.0F, id + ": the totem did not save the player");
						report.check(player.getItemCooldownManager().isCoolingDown(item), id + ": the totem has no cooldown after saving the player");
						report.check(player.getMainHandStack().isOf(item), id + ": the totem was used up");
						player.getItemCooldownManager().remove(item);
					}
					tested++;
				} catch (Throwable t) {
					report.problem(id + ": defense crashed: " + t);
					LOG.error("defense test " + id, t);
				}
				report.modErrors(id + " (defense)");
			}
			report.info(tested + " protective items tested");
			report.check(tested >= 5, "only " + tested + " protective items were found");
			report.check(player.isAlive(), "the tester is dead at the end of the test");
			resetStage(world, center);
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	// ------------------------------------------------------------------ 7. right-click on a block

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_blockuse", tickLimit = 20000)
	public void blockUse(TestContext context) {
		Report report = new Report("block use");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			int special = 0;
			// Items that work with crops go first. Crops need daylight, and the game works out light in the background:
			// a wall, a house or a column of gold that stood on a spot a moment ago can still throw its shadow there.
			List<Item> order = new ArrayList<>();
			List<Item> afterCrops = new ArrayList<>();
			for (Item item : ComboItems.ALL) {
				Traits traits = ComboItems.traits(item);
				boolean crops = traits != null && (traits.harvestRadius > 0 || traits.bonemealRadius > 0 || traits.sow);
				(crops ? order : afterCrops).add(item);
			}
			order.addAll(afterCrops);
			for (Item item : order) {
				Traits traits = ComboItems.traits(item);
				String id = name(item);
				try {
					resetStage(world, center);
					resetPlayer(player, world, center);
					ItemStack stack = new ItemStack(item);
					hold(player, stack);
					BlockPos target = center.add(0, -1, 3); // a floor block in front of the player

					if (traits != null && traits.harvestRadius > 0) {
						int r = Math.min(traits.harvestRadius, 4);
						fill(world, target.add(-r, 0, -r), target.add(r, 0, r), Blocks.FARMLAND.getDefaultState());
						CropBlock wheat = (CropBlock) Blocks.WHEAT;
						fill(world, target.add(-r, 1, -r), target.add(r, 1, r), wheat.withAge(wheat.getMaxAge()));
						ActionResult result = player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target.up()));
						int ripe = 0;
						for (BlockPos pos : BlockPos.iterate(target.add(-r, 1, -r), target.add(r, 1, r))) {
							BlockState state = world.getBlockState(pos);
							report.check(state.isOf(Blocks.WHEAT), id + ": harvesting removed the plant instead of replanting it");
							if (state.isOf(Blocks.WHEAT) && wheat.isMature(state)) {
								ripe++;
							}
						}
						report.check(ripe == 0, id + ": harvesting (" + result + ") left " + ripe + " ripe plants");
						report.check(have(world, player, center, Items.WHEAT) >= (2 * r + 1) * (2 * r + 1), id + ": harvesting gave too little wheat");
						if (traits.magnetDrops) {
							report.check(onGround(world, center, Items.WHEAT) == 0, id + ": the harvest did not go into the inventory");
						}
						special++;
					} else if (traits != null && traits.bonemealRadius > 0) {
						fill(world, target.add(-1, 0, -1), target.add(1, 0, 1), Blocks.FARMLAND.getDefaultState());
						CropBlock wheat = (CropBlock) Blocks.WHEAT;
						fill(world, target.add(-1, 1, -1), target.add(1, 1, 1), wheat.withAge(0));
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target.up()));
						BlockState state = world.getBlockState(target.up());
						report.check(state.isOf(Blocks.WHEAT) && state.get(CropBlock.AGE) > 0, id + ": the plant did not grow");
						special++;
					} else if (traits != null && traits.sow) {
						player.getInventory().setStack(5, new ItemStack(Items.WHEAT_SEEDS, 16));
						ActionResult result = player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target));
						int farmland = count(world, target.add(-1, 0, -1), target.add(1, 0, 1), Blocks.FARMLAND);
						int planted = count(world, target.add(-1, 1, -1), target.add(1, 1, 1), Blocks.WHEAT);
						int seedsLeft = player.getInventory().count(Items.WHEAT_SEEDS);
						report.check(farmland == 9, id + ": right-click (" + result + ") made " + farmland + " of 9 farmland blocks");
						report.check(planted == 9, id + ": " + planted + " of 9 seeds were planted");
						report.check(seedsLeft == 7, id + ": 9 of 16 seeds should be used up, " + seedsLeft + " are left");
						report.check(stack.getDamage() == 1, id + ": the hoe should wear out by 1, it wore out by " + stack.getDamage());
						special++;
					} else if (traits != null && traits.ropeLadder) {
						// a wall, 6 blocks high
						BlockPos foot = center.add(6, 0, 4);
						BlockPos top = foot.up(5);
						fill(world, foot, top, Blocks.STONE.getDefaultState());
						// a click on the side of the top block: the ladder runs down this side
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND,
								new BlockHitResult(Vec3d.ofCenter(top), Direction.NORTH, top, false));
						int front = count(world, foot.north(), top.north(), Blocks.LADDER);
						report.check(front == 6, id + ": " + front + " of 6 ladders hang on the clicked side of the wall");
						// a click on top of the wall: the ladder hangs down the far side
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(top));
						int back = count(world, foot.south(), top.south(), Blocks.LADDER);
						report.check(back == 6, id + ": " + back + " of 6 ladders hang on the far side of the wall");
						report.check(stack.getDamage() == 2, id + ": two uses should cost 2 durability, they cost " + stack.getDamage());
						special++;
					} else if (traits != null && traits.endlessWater) {
						BlockPos spot = center.add(6, -1, 0);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(spot));
						report.check(world.getBlockState(spot.up()).isOf(Blocks.WATER), id + ": no water was placed");
						report.check(player.getMainHandStack().isOf(item), id + ": the bucket was used up");
						special++;
					} else if (traits != null && traits.bedrockBreak) {
						world.setBlockState(target, Blocks.BEDROCK.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target));
						report.check(world.getBlockState(target).isAir(), id + ": the bedrock was not broken");
						report.check(have(world, player, center, Items.BEDROCK) == 1, id + ": the bedrock did not drop");
						special++;
					} else if (traits != null && traits.endlessLava) {
						BlockPos spot = center.add(6, -1, 0);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(spot));
						report.check(world.getBlockState(spot.up()).isOf(Blocks.LAVA), id + ": no lava was placed");
						report.check(player.getMainHandStack().isOf(item), id + ": the bucket was used up");
						special++;
					} else if (traits != null && traits.unlock) {
						// an iron door (two blocks high) and an iron trapdoor
						BlockPos doorPos = center.add(6, 0, 3);
						world.setBlockState(doorPos, Blocks.IRON_DOOR.getDefaultState().with(DoorBlock.HALF, DoubleBlockHalf.LOWER),
								Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						world.setBlockState(doorPos.up(), Blocks.IRON_DOOR.getDefaultState().with(DoorBlock.HALF, DoubleBlockHalf.UPPER),
								Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						BlockHitResult onDoor = new BlockHitResult(Vec3d.ofCenter(doorPos), Direction.NORTH, doorPos, false);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, onDoor);
						report.check(world.getBlockState(doorPos).isOf(Blocks.IRON_DOOR) && world.getBlockState(doorPos).get(DoorBlock.OPEN),
								id + ": the iron door did not open");
						report.check(world.getBlockState(doorPos.up()).isOf(Blocks.IRON_DOOR) && world.getBlockState(doorPos.up()).get(DoorBlock.OPEN),
								id + ": the upper half of the iron door did not open");
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, onDoor);
						report.check(world.getBlockState(doorPos).isOf(Blocks.IRON_DOOR) && !world.getBlockState(doorPos).get(DoorBlock.OPEN),
								id + ": the iron door did not close again");
						BlockPos hatch = center.add(6, 0, 0);
						world.setBlockState(hatch, Blocks.IRON_TRAPDOOR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(hatch));
						report.check(world.getBlockState(hatch).isOf(Blocks.IRON_TRAPDOOR) && world.getBlockState(hatch).get(TrapdoorBlock.OPEN),
								id + ": the iron trapdoor did not open");
						report.check(stack.getDamage() == 3, id + ": three uses should cost 3 durability, they cost " + stack.getDamage());
						special++;
					} else if (traits != null && traits.push) {
						BlockPos block = center.add(0, 0, 3);
						world.setBlockState(block, Blocks.STONE.getDefaultState());
						// a click on the side that faces the tester pushes the block away
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND,
								new BlockHitResult(Vec3d.ofCenter(block), Direction.NORTH, block, false));
						report.check(world.getBlockState(block).isAir() && world.getBlockState(block.south()).isOf(Blocks.STONE),
								id + ": the stone block was not pushed one step away");
						// sneaking pulls it back
						player.getItemCooldownManager().remove(item);
						player.setSneaking(true);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND,
								new BlockHitResult(Vec3d.ofCenter(block.south()), Direction.NORTH, block.south(), false));
						player.setSneaking(false);
						report.check(world.getBlockState(block).isOf(Blocks.STONE) && world.getBlockState(block.south()).isAir(),
								id + ": the stone block was not pulled back");
						report.check(stack.getDamage() == 2, id + ": two moves should cost 2 durability, they cost " + stack.getDamage());
						// what a piston cannot move stays: obsidian, and a chest (it holds things)
						player.getItemCooldownManager().remove(item);
						BlockPos heavy = center.add(3, 0, 3);
						world.setBlockState(heavy, Blocks.OBSIDIAN.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND,
								new BlockHitResult(Vec3d.ofCenter(heavy), Direction.NORTH, heavy, false));
						report.check(world.getBlockState(heavy).isOf(Blocks.OBSIDIAN) && world.getBlockState(heavy.south()).isAir(), id + ": obsidian was moved");
						world.setBlockState(heavy, Blocks.CHEST.getDefaultState());
						player.setSneaking(true); // without sneaking the click would open the chest
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND,
								new BlockHitResult(Vec3d.ofCenter(heavy), Direction.NORTH, heavy, false));
						player.setSneaking(false);
						report.check(world.getBlockState(heavy).isOf(Blocks.CHEST) && world.getBlockState(heavy.north()).isAir(), id + ": a chest was moved");
						world.setBlockState(heavy, Blocks.AIR.getDefaultState());
						special++;
					} else if (traits != null && traits.torch) {
						world.setBlockState(target, Blocks.STONE.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target));
						report.check(world.getBlockState(target.up()).isOf(Blocks.TORCH), id + ": no torch was placed");
						special++;
					} else if (traits != null && traits.midas) {
						fill(world, target.add(0, 1, 0), target.add(0, 3, 0), Blocks.COBBLESTONE.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target.up(3)));
						report.check(count(world, target.add(0, 1, 0), target.add(0, 3, 0), Blocks.GOLD_BLOCK) == 3, id + ": the blocks did not turn into gold");
						special++;
					} else if (traits != null && traits.transmute) {
						world.setBlockState(target.up(), Blocks.COBBLESTONE.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target.up()));
						report.check(world.getBlockState(target.up()).isOf(Blocks.COAL_BLOCK), id + ": cobblestone did not turn into a coal block");
						player.setSneaking(true);
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target.up()));
						report.check(world.getBlockState(target.up()).isOf(Blocks.COBBLESTONE), id + ": sneaking did not turn the coal block back");
						special++;
					} else if (traits != null && traits.house) {
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target));
						int planks = count(world, center.add(-6, 0, 0), center.add(6, 6, 12), Blocks.OAK_PLANKS);
						report.check(planks > 80, id + ": the house has only " + planks + " plank blocks");
						report.check(count(world, center.add(-6, 0, 0), center.add(6, 6, 12), Blocks.RED_BED) == 2, id + ": the house has no bed");
						report.check(player.getMainHandStack().isEmpty(), id + ": the house box was not used up");
						special++;
					} else {
						// Nothing special: it only must not crash.
						world.setBlockState(target, Blocks.GRASS_BLOCK.getDefaultState());
						player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hitTop(target));
						// The log stands away from the spot where the crops of the other items grow. A solid block there would
						// leave a shadow for a moment (light is worked out in the background), and crops break in the dark.
						BlockPos log = target.add(6, 1, 0);
						world.setBlockState(log, Blocks.OAK_LOG.getDefaultState());
						player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hitTop(log));
					}
				} catch (Throwable t) {
					report.problem(id + ": right-click on a block crashed: " + t);
					LOG.error("block use test " + id, t);
				}
				report.modErrors(id + " (right-click on a block)");
			}
			report.info(special + " items with a special right-click on blocks tested");
			report.check(special >= 6, "only " + special + " items with a special block right-click were found");
			resetStage(world, center);
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	// ------------------------------------------------------------------ 8. food

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_food", tickLimit = 20000)
	public void food(TestContext context) {
		Report report = new Report("food");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		whenReady(context, world, center, () -> {
			int eaten = 0;
			for (Item item : ComboItems.ALL) {
				ItemStack stack = new ItemStack(item);
				if (stack.get(DataComponentTypes.FOOD) == null) {
					continue;
				}
				Traits traits = ComboItems.traits(item);
				String id = name(item);
				try {
					resetPlayer(player, world, center);
					hold(player, stack);
					player.getHungerManager().setFoodLevel(5);
					ItemStack left = stack.finishUsing(world, player);
					report.check(player.getHungerManager().getFoodLevel() > 5, id + ": eating did not feed the player");
					report.check(left.isEmpty() || left.getCount() == 0 || !left.isOf(item), id + ": the food was not used up");
					if (traits != null && traits.eatBuffSeconds > 0) {
						for (int i = 0; i < traits.attrs.size(); i++) {
							report.check(hasModifier(player, item, traits, i), id + ": eating did not change " + traits.attrs.get(i).id());
						}
					}
					if (traits != null && traits.eatLaunch > 0) {
						report.check(player.getVelocity().y > 1.0, id + ": eating did not launch the player");
					}
					eaten++;
				} catch (Throwable t) {
					report.problem(id + ": eating crashed: " + t);
					LOG.error("food test " + id, t);
				}
				report.modErrors(id + " (eating)");
			}
			report.info(eaten + " foods eaten");
			report.check(eaten >= 3, "only " + eaten + " foods were found");
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	private static boolean hasModifier(ServerPlayerEntity player, Item item, Traits traits, int index) {
		Traits.Attr attr = traits.attrs.get(index);
		Optional<RegistryEntry.Reference<EntityAttribute>> attribute = Registries.ATTRIBUTE.getEntry(Identifier.ofVanilla(attr.id()));
		if (attribute.isEmpty()) {
			throw new GameTestException(name(item) + " uses the unknown attribute " + attr.id());
		}
		EntityAttributeInstance instance = player.getAttributeInstance(attribute.get());
		return instance != null && instance.hasModifier(ItemCombinator.id(name(item) + "_" + index));
	}

	// ------------------------------------------------------------------ 9. abilities that work without clicking

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_passive", tickLimit = 20000)
	public void passive(TestContext context) {
		Report report = new Report("passive");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);

		// One fake player per item, all at the same time.
		List<Item> items = new ArrayList<>();
		List<ServerPlayerEntity> players = new ArrayList<>();
		for (Item item : ComboItems.ALL) {
			Traits traits = ComboItems.traits(item);
			if (traits == null) {
				continue;
			}
			boolean attributes = !traits.attrs.isEmpty() && traits.eatBuffSeconds == 0;
			if (traits.passive.isEmpty() && !attributes && !traits.flight && !traits.mend && traits.pullRadius == 0) {
				continue;
			}
			items.add(item);
		}
		report.info(items.size() + " items with passive abilities");
		report.check(items.size() >= 10, "only " + items.size() + " items with passive abilities were found");

		Runnable giveItems = () -> {
			for (Item item : items) {
				ServerPlayerEntity player = newPlayer(world, center);
				players.add(player);
				Traits traits = ComboItems.traits(item);
				equip(player, new ItemStack(item));
				if (traits.mend) {
					ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
					worn.setDamage(100);
					player.getInventory().setStack(20, worn);
				}
			}
			world.spawnEntity(new ItemEntity(world, center.getX() + 3.5, center.getY() + 0.2, center.getZ() + 0.5, new ItemStack(Items.DIAMOND)));
		};

		Runnable checkWithItems = () -> {
			report.modErrors("passive abilities while the items are active");
			for (int n = 0; n < items.size(); n++) {
				Item item = items.get(n);
				ServerPlayerEntity player = players.get(n);
				Traits traits = ComboItems.traits(item);
				String id = name(item);
				try {
					for (Traits.Fx fx : traits.passive) {
						report.check(player.hasStatusEffect(fx.effect()), id + ": the effect " + fx.effect().getIdAsString() + " is missing");
					}
					if (traits.eatBuffSeconds == 0) {
						for (int i = 0; i < traits.attrs.size(); i++) {
							report.check(hasModifier(player, item, traits, i), id + ": the change to " + traits.attrs.get(i).id() + " is missing");
						}
					}
					if (traits.flight) {
						report.check(player.getAbilities().allowFlying, id + ": flying is not allowed");
					}
					if (traits.mend) {
						ItemStack worn = player.getInventory().getStack(20);
						report.check(worn.getDamage() < 100, id + ": the damaged pickaxe was not repaired");
					}
					// now take the item away
					player.getInventory().clear();
				} catch (Throwable t) {
					report.problem(id + ": passive check crashed: " + t);
					LOG.error("passive test " + id, t);
				}
			}
		};

		Runnable checkWithoutItems = () -> {
			report.modErrors("passive abilities after the items were removed");
			for (int n = 0; n < items.size(); n++) {
				Item item = items.get(n);
				ServerPlayerEntity player = players.get(n);
				Traits traits = ComboItems.traits(item);
				String id = name(item);
				try {
					if (traits.eatBuffSeconds == 0) {
						for (int i = 0; i < traits.attrs.size(); i++) {
							report.check(!hasModifier(player, item, traits, i), id + ": the change to " + traits.attrs.get(i).id() + " stays after the item is gone");
						}
					}
					if (traits.flight) {
						report.check(!player.getAbilities().allowFlying, id + ": flying stays allowed after the item is gone");
					}
					report.check(player.getHealth() <= player.getMaxHealth(), id + ": more health than the maximum after the item is gone");
				} catch (Throwable t) {
					report.problem(id + ": passive check crashed: " + t);
				}
				removePlayer(player);
			}
			resetStage(world, center);
			forceChunks(world, center, false);
			report.finish(context);
		};

		// Effects are given once a second, body changes twice a second, repair every two seconds: 50 ticks cover all of them.
		everyTickWhenReady(context, world, center, tick -> {
			if (tick == 1) {
				giveItems.run();
			} else if (tick == 51) {
				checkWithItems.run();
			} else if (tick == 76) {
				checkWithoutItems.run();
			}
		});
	}

	// ------------------------------------------------------------------ 10. right-click abilities

	/** One right-click that the test performs. */
	private record Click(Item item, boolean sneak) {
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_use", tickLimit = 20000)
	public void rightClick(TestContext context) {
		Report report = new Report("right-click");
		ServerWorld world = context.getWorld();
		MinecraftServer server = world.getServer();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity[] player = {newPlayer(world, center)};

		// The time stop and the ghost cloak come last. The test then waits and checks that both end by themselves.
		List<Click> clicks = new ArrayList<>();
		List<Click> timeStops = new ArrayList<>();
		List<Click> ghosts = new ArrayList<>();
		Set<Use> covered = new HashSet<>();
		covered.add(Use.NONE);
		for (Item item : ComboItems.ALL) {
			Traits traits = ComboItems.traits(item);
			Use use = traits == null ? Use.NONE : traits.use;
			Use sneakUse = traits == null ? Use.NONE : traits.sneakUse;
			covered.add(use);
			covered.add(sneakUse);
			(use == Use.TIME_STOP ? timeStops : use == Use.GHOST ? ghosts : clicks).add(new Click(item, false));
			if (sneakUse != Use.NONE) {
				(sneakUse == Use.TIME_STOP ? timeStops : sneakUse == Use.GHOST ? ghosts : clicks).add(new Click(item, true));
			}
		}
		clicks.addAll(timeStops);
		clicks.addAll(ghosts);
		for (Use use : Use.values()) {
			report.check(covered.contains(use), "no item uses the ability " + use);
		}
		report.info(clicks.size() + " right-clicks to do");

		int[] next = {0};
		int[] doneAt = {-1};
		int[] tick = {0};
		List<String> noEffect = new ArrayList<>();
		String[] last = {"start"};

		everyTickWhenReady(context, world, center, now -> {
			tick[0] = now;
			report.modErrors("after " + last[0]);
			if (next[0] < clicks.size()) {
				Click click = clicks.get(next[0]++);
				Traits traits = ComboItems.traits(click.item());
				String id = name(click.item()) + (click.sneak() ? " (sneaking)" : "");
				last[0] = id;
				try {
					if (!player[0].isAlive() || player[0].isRemoved()) {
						report.problem("the tester died before " + id);
						removePlayer(player[0]);
						player[0] = newPlayer(world, center);
					}
					ServerPlayerEntity p = player[0];
					resetStage(world, center);
					resetPlayer(p, world, center);
					p.setInvulnerable(true);
					// a wall behind the target, a pig to aim at, and something to copy in the other hand
					fill(world, center.add(-3, 0, 9), center.add(3, 4, 11), Blocks.STONE.getDefaultState());
					MobEntity pig = spawnMob(world, EntityType.PIG, center.getX() + 0.5, center.getY(), center.getZ() + 3.5);
					Use use = traits == null ? Use.NONE : click.sneak() ? traits.sneakUse : traits.use;
					if (use == Use.DRAIN) {
						world.setBlockState(center.add(3, 0, 0), Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
					if (use == Use.FISH) {
						// a pond where the tester looks
						fill(world, center.add(-1, -1, 4), center.add(1, -1, 7), Blocks.WATER.getDefaultState());
					}
					if (use == Use.SHEAR_AREA) {
						spawnMob(world, EntityType.SHEEP, center.getX() + 2.5, center.getY(), center.getZ() + 0.5);
						world.setBlockState(center.add(-2, 0, 1), Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true),
								Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
					if (use == Use.DISCO) {
						// a cow that can think, so it has something to stop doing
						spawnMob(world, EntityType.COW, center.getX() - 2.5, center.getY(), center.getZ() + 2.5).setAiDisabled(false);
					}
					if (use == Use.WEATHER) {
						world.getServer().getOverworld().setWeather(6000, 0, false, false);
					}
					if (use == Use.SUNBURN) {
						world.getServer().getOverworld().setWeather(6000, 0, false, false);
						world.setTimeOfDay(6000L); // noon
					}
					if (use == Use.TERMITES) {
						termiteHouse(world, center);
					}
					if (use == Use.PART_SEA) {
						fill(world, center.add(-2, -1, 1), center.add(2, -1, 9), Blocks.WATER.getDefaultState());
					}
					if (use == Use.WILD_MAGIC) {
						Wild.forceNextSpell(Use.SMALL_FIREBALL);
					}
					if (use == Use.DICE) {
						Wild.forceNextRoll(12);
					}
					if (use == Use.ORE_SIGHT) {
						world.setBlockState(center.add(4, 0, -3), Blocks.IRON_ORE.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						world.setBlockState(center.add(-4, 2, -3), Blocks.DIAMOND_ORE.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					}
					ItemStack stack = new ItemStack(click.item());
					hold(p, stack);
					p.setStackInHand(Hand.OFF_HAND, use == Use.SMELT_HAND ? new ItemStack(Items.RAW_IRON, 12) : new ItemStack(Items.DIAMOND));
					p.getItemCooldownManager().remove(click.item());
					p.setSneaking(click.sneak());
					p.setHealth(10.0F);
					p.setExperienceLevel(use == Use.ENCHANT_BOOK ? 5 : use == Use.BANK_IN ? 10 : 0);
					Vec3d standing = p.getPos();
					int countBefore = stack.getCount();
					int damageBefore = stack.getDamage();
					ActionResult result = p.interactionManager.interactItem(p, world, stack, Hand.MAIN_HAND);
					if (use != Use.NONE) {
						LOG.info("[right-click] {} -> {} : {}", id, use, result);
						if (result == ActionResult.FAIL) {
							noEffect.add(id + " (" + use + ")");
						} else {
							report.check(result.isAccepted(), id + ": right-click was ignored (" + result + ")");
							int cooldown = click.sneak() ? traits.sneakCooldown : traits.cooldown;
							if (cooldown > 0) {
								report.check(p.getItemCooldownManager().isCoolingDown(click.item()), id + ": no cooldown after use");
							}
							if (traits.consume) {
								report.check(stack.getCount() == countBefore - 1, id + ": the item was not used up");
							} else if (traits.cost > 0 && stack.isDamageable()) {
								report.check(stack.getDamage() == damageBefore + traits.cost, id + ": the item did not wear out by " + traits.cost);
							}
							String missing = missingEffect(use, world, p, center, pig, standing);
							report.check(missing == null, id + " (" + use + "): " + missing);
						}
					}
					p.clearActiveItem();
				} catch (Throwable t) {
					report.problem(id + ": right-click crashed: " + t);
					LOG.error("right-click test " + id, t);
				}
				report.modErrors(id);
				if (next[0] == clicks.size()) {
					doneAt[0] = tick[0];
					last[0] = "all right-clicks (effects that take time)";
				}
				return;
			}
			// Let bombs, black holes, time stops and so on run to their end.
			if (tick[0] - doneAt[0] < 500) {
				return;
			}
			report.check(!server.getTickManager().isFrozen(), "time is still frozen long after the time stop");
			report.check(player[0].isAlive(), "the tester is dead at the end");
			report.check(player[0].interactionManager.getGameMode() == GameMode.SURVIVAL,
					"the tester is still in " + player[0].interactionManager.getGameMode() + " mode long after the ghost cloak");
			report.info("right-clicks without effect in this test scene: " + (noEffect.isEmpty() ? "none" : String.join(", ", noEffect)));
			if (server.getTickManager().isFrozen()) {
				server.getTickManager().setFrozen(false);
			}
			resetPlayer(player[0], world, center);
			removePlayer(player[0]);
			resetStage(world, center);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	private static <T extends Entity> boolean any(ServerWorld world, BlockPos center, Class<T> type) {
		return !world.getEntitiesByClass(type, new Box(center).expand(90.0), e -> e.isAlive()).isEmpty();
	}

	private static int countAny(ServerWorld world, BlockPos from, BlockPos to, Block... blocks) {
		int total = 0;
		for (Block block : blocks) {
			total += count(world, from, to, block);
		}
		return total;
	}

	/**
	 * How many blocks of the given kind lie in the chunk the tester looks at, inside the stage, at the given height
	 * above the floor (0 = the floor itself).
	 */
	private static int chunkFloor(ServerWorld world, BlockPos center, int dy, Block block) {
		ChunkPos chunk = new ChunkPos(center.add(0, 0, 4));
		int y = center.getY() - 1 + dy;
		BlockPos from = new BlockPos(Math.max(chunk.getStartX(), center.getX() - 12), y, Math.max(chunk.getStartZ(), center.getZ() - 12));
		BlockPos to = new BlockPos(Math.min(chunk.getEndX(), center.getX() + 12), y, Math.min(chunk.getEndZ(), center.getZ() + 12));
		return count(world, from, to, block);
	}

	/**
	 * Empties the chunk the tester looks at, from the bedrock to the sky, except the stage floor. Earlier tests leave
	 * things above and below the stage (a beanstalk, a cloud, a flipped chunk), and the chunk tests move whole columns.
	 */
	private static void clearLookChunk(ServerWorld world, BlockPos center) {
		ChunkPos chunk = new ChunkPos(center.add(0, 0, 4));
		BlockState air = Blocks.AIR.getDefaultState();
		for (BlockPos pos : BlockPos.iterate(chunk.getStartX(), world.getBottomY() + 1, chunk.getStartZ(), chunk.getEndX(), world.getTopY() - 1, chunk.getEndZ())) {
			if (pos.getY() != center.getY() - 1 && !world.getBlockState(pos).isAir()) {
				world.setBlockState(pos, air, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
			}
		}
	}

	/** A 3 x 3 x 3 cube of oak planks in front of the tester, for the termites. */
	private static void termiteHouse(ServerWorld world, BlockPos center) {
		fill(world, center.add(-1, 0, 3), center.add(1, 2, 5), Blocks.OAK_PLANKS.getDefaultState());
	}

	/** True while a pig stands within 2.5 blocks of the spot. */
	private static boolean pigNear(ServerWorld world, Vec3d spot) {
		return !world.getEntitiesByClass(PigEntity.class, Box.of(spot, 5.0, 5.0, 5.0), e -> e.isAlive() && e.getPos().distanceTo(spot) < 2.5).isEmpty();
	}

	private static int countLogs(ServerWorld world, BlockPos center) {
		TagKey<Block> logs = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("logs"));
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(center.add(-16, 0, -8), center.add(16, 14, 24))) {
			if (world.getBlockState(pos).isIn(logs)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Looks whether a right-click ability left its mark right away. Returns null if it did, otherwise what is missing.
	 * The scene: the tester stands on grass and looks at the floor 4 blocks ahead. A pig stands in the line of sight,
	 * 3 blocks away. A stone wall is 9 blocks away. Abilities that take time are checked in the test "slowEffects".
	 */
	private static String missingEffect(Use use, ServerWorld world, ServerPlayerEntity p, BlockPos center, MobEntity pig, Vec3d standing) {
		boolean pigHurt = !pig.isAlive() || pig.getHealth() < pig.getMaxHealth();
		switch (use) {
			case SMALL_FIREBALL:
				return any(world, center, SmallFireballEntity.class) ? null : "no fireball appeared";
			case BIG_FIREBALL:
			case METEOR:
				return any(world, center, FireballEntity.class) ? null : "no fireball appeared";
			case WITHER_SKULL:
				return any(world, center, WitherSkullEntity.class) ? null : "no wither skull appeared";
			case ARROW_BURST:
				return any(world, center, ArrowEntity.class) ? null : "no arrows appeared";
			case DYNAMITE:
			case MEGA_BOMB:
			case NUKE:
				return any(world, center, TntEntity.class) ? null : "no bomb was thrown";
			case LIGHTNING:
			case LIGHTNING_STORM:
				return any(world, center, LightningEntity.class) ? null : "no lightning appeared";
			case BEAM:
				return pigHurt ? null : "the pig in the line of fire was not hurt";
			case FROST_CONE:
				return pigHurt || pig.hasStatusEffect(StatusEffects.SLOWNESS) ? null : "the pig in front was not hurt or slowed";
			case QUAKE:
				return any(world, center, FallingBlockEntity.class) ? null : "no blocks were thrown into the air";
			case FLOOD:
				return count(world, center.add(-8, -1, -4), center.add(8, 8, 14), Blocks.WATER) > 0 ? null : "no water appeared";
			case DRAIN:
				return world.getBlockState(center.add(3, 0, 0)).isOf(Blocks.WATER) ? "the water next to the tester is still there" : null;
			case FREEZE_AREA:
				return count(world, center.add(-6, 0, -6), center.add(6, 0, 6), Blocks.SNOW) > 0 ? null : "no snow appeared";
			case BLINK:
				return p.getPos().distanceTo(standing) > 2.0 ? null : "the tester did not move";
			case LEAP:
				return p.getVelocity().length() > 0.3 ? null : "the tester was not pushed";
			case RECALL:
				return p.getPos().distanceTo(standing) > 30.0 || p.getWorld() != world ? null : "the tester was not sent to the spawn point";
			case SWAP:
				return p.getPos().distanceTo(standing) > 2.0 && pig.getPos().distanceTo(standing) < 1.0 ? null : "tester and pig did not swap places";
			case GHOST:
				return p.interactionManager.getGameMode() == GameMode.SPECTATOR ? null : "the tester is not a ghost";
			case POLYMORPH:
				return pig.isRemoved() && !world.getEntitiesByClass(MobEntity.class, new Box(center).expand(8.0), e -> e != pig && e.isAlive()).isEmpty()
						? null : "the pig was not turned into another animal";
			case SHRINK:
			case GROW: {
				EntityAttributeInstance scale = pig.getAttributeInstance(Registries.ATTRIBUTE.getEntry(Identifier.ofVanilla("generic.scale")).orElseThrow());
				return scale != null && scale.hasModifier(ItemCombinator.id("size_ray")) ? null : "the size of the pig did not change";
			}
			case SUMMON_GOLEMS:
				return any(world, center, IronGolemEntity.class) ? null : "no iron golem appeared";
			case LIFT:
				return pig.hasStatusEffect(StatusEffects.LEVITATION) ? null : "the pig does not float";
			case SLAM:
				return pigHurt ? null : "the pig was not hurt";
			case GLOW_SCAN:
				return pig.hasStatusEffect(StatusEffects.GLOWING) ? null : "the pig does not glow";
			case HEAL:
				return p.getHealth() > 10.0F ? null : "the tester was not healed";
			case ENDER_POUCH:
			case WORKBENCH:
				return p.currentScreenHandler != p.playerScreenHandler ? null : "no screen was opened";
			case TIME_DAY:
				return world.getServer().getOverworld().getTimeOfDay() % 24000L == 1000L ? null : "it is not morning";
			case TIME_NIGHT:
				return world.getServer().getOverworld().getTimeOfDay() % 24000L == 13000L ? null : "it is not night";
			case TIME_STOP:
				return world.getServer().getTickManager().isFrozen() ? null : "time is not frozen";
			case XP:
				return p.experienceLevel > 0 ? null : "no experience levels were given";
			case DUPE:
				return p.getInventory().count(Items.DIAMOND) >= 2 ? null : "the diamond in the other hand was not copied";
			case BRIDGE:
				return count(world, center.add(-1, -1, 13), center.add(1, -1, 16), Blocks.OAK_PLANKS) > 0 ? null : "no bridge was built past the edge of the floor";
			case HOLE:
				return count(world, center.add(-1, -1, 4), center.add(1, -1, 6), Blocks.GRASS_BLOCK) < 9 ? null : "no hole was opened in the floor";
			case FOREST:
				return countLogs(world, center) > 0 ? null : "no trees grew";
			case LASER_DRILL:
				return count(world, center.add(-3, 0, 9), center.add(3, 4, 11), Blocks.STONE) < 105 && p.getInventory().count(Items.COBBLESTONE) > 0
						? null : "the wall was not drilled or the stone did not go into the inventory";
			case BOOMERANG:
				return pigHurt ? null : "the pig in the line of flight was not hurt";
			case TORCH_SHOT:
				return count(world, center.add(-1, 0, 3), center.add(1, 0, 6), Blocks.TORCH) == 1 ? null : "no torch stands where the tester looks";
			case GRAPPLE:
				return p.getVelocity().length() > 0.3 ? null : "the tester was not pulled";
			case YANK:
				return pig.getVelocity().length() > 0.3 ? null : "the pig was not pulled";
			case FISH:
				return carried(p) >= 3 && any(world, center, ExperienceOrbEntity.class)
						? null : "no catch from the pond (the tester carries " + carried(p) + " stacks, 3 are expected)";
			case BACKPACK:
				return p.currentScreenHandler instanceof BackpackScreenHandler ? null : "the backpack did not open";
			case SMELT_HAND:
				return p.getInventory().count(Items.IRON_INGOT) == 8 && p.getOffHandStack().isOf(Items.RAW_IRON) && p.getOffHandStack().getCount() == 4
						? null : "12 raw iron should give 8 iron ingots and leave 4, but there are " + p.getInventory().count(Items.IRON_INGOT)
								+ " ingots and the other hand holds " + p.getOffHandStack();
			case SET_SPAWN: {
				BlockPos spawn = p.getSpawnPointPosition();
				boolean here = spawn != null && spawn.equals(p.getBlockPos()) && p.getSpawnPointDimension() == world.getRegistryKey();
				p.setSpawnPoint(World.OVERWORLD, null, 0.0F, false, false);
				return here ? null : "the respawn point is " + spawn + " and not where the tester stands";
			}
			case WAYPOINT_SET:
				return p.getMainHandStack().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).contains("combinator_waypoint")
						? null : "the stone did not remember the spot";
			case SHEAR_AREA:
				return onGround(world, center, Items.WHITE_WOOL) > 0 && onGround(world, center, Items.OAK_LEAVES) > 0
						? null : "the sheep gave " + onGround(world, center, Items.WHITE_WOOL) + " wool and the leaves gave "
								+ onGround(world, center, Items.OAK_LEAVES) + " leaf blocks";
			case SMOKE:
				return p.hasStatusEffect(StatusEffects.INVISIBILITY) ? null : "the tester is not invisible";
			case SUMMON_WOLVES: {
				int wolves = world.getEntitiesByClass(WolfEntity.class, new Box(center).expand(8.0), wolf -> wolf.isAlive() && wolf.isOwner(p)).size();
				return wolves >= 1 ? null : "no wolf that belongs to the tester appeared";
			}
			case SUMMON_HORSE:
				return !world.getEntitiesByClass(HorseEntity.class, new Box(center).expand(8.0), horse -> horse.isAlive() && horse.isTame() && horse.isSaddled()).isEmpty()
						? null : "no tame horse with a saddle appeared";
			case SUMMON_TRADER:
				return any(world, center, WanderingTraderEntity.class) ? null : "no wandering trader appeared";
			case ORE_SIGHT: {
				List<String> shown = oreOutlines(world, center);
				return shown.size() == 2 && shown.contains("minecraft:iron_ore") && shown.contains("minecraft:diamond_ore")
						? null : "the glowing outlines show " + shown + " instead of the iron ore and the diamond ore";
			}
			case DISCO:
				return !world.getEntitiesByClass(MobEntity.class, new Box(center).expand(12.0),
						mob -> mob.isAlive() && mob.isAiDisabled() && mob.getCommandTags().contains(Oddities.DANCE_TAG)).isEmpty()
						? null : "the cow next to the tester does not dance";
			case BANK_IN:
				return p.experienceLevel == 0 && Oddities.stored(p.getMainHandStack()) >= 160 && Oddities.stored(p.getMainHandStack()) < 177
						? null : "10 levels are 160 points, but the tester has level " + p.experienceLevel + " and the bank holds "
								+ Oddities.stored(p.getMainHandStack());
			case WEATHER: {
				boolean rain = world.getLevelProperties().isRaining();
				world.getServer().getOverworld().setWeather(6000, 0, false, false);
				return rain ? null : "it did not start to rain";
			}
			case SUNBURN:
				return pig.isOnFire() ? null : "the pig in the sunlight does not burn";
			case ARMAGEDDON:
				return Doom.active(p) ? null : "the countdown did not start";
			case DOOM_CANCEL:
				return Doom.active(p) ? "the Armageddon is still running" : null;
			case CLUSTER_BOMB:
			case GRAVITY_GRENADE:
			case SCATTER_BOMB:
			case GOLD_BOMB:
			case SHEEP_BOMB:
			case COBWEB_BOMB:
			case HIVE_GRENADE:
			case PAINT_BOMB:
				return any(world, center, TntEntity.class) ? null : "no bomb was thrown";
			case FANGS:
			case FANG_RING:
				return any(world, center, EvokerFangsEntity.class) ? null : "no evoker fangs";
			case DRAGON_FIREBALL:
				return any(world, center, DragonFireballEntity.class) ? null : "no dragon fireball";
			case TRIDENT_STORM:
				return any(world, center, TridentEntity.class) ? null : "no tridents";
			case HORDE:
				return any(world, center, ZombieEntity.class) ? null : "no horde";
			case WORLD_TREE:
				return count(world, center.add(-4, 0, 0), center.add(4, 2, 9), Blocks.OAK_LOG) > 0 ? null : "no giant tree started to grow";
			case COPY_CORNER:
				return p.getMainHandStack().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).contains("combinator_copy")
						? null : "the wand did not remember the corner";
			case SHULKER_BULLETS:
				return any(world, center, ShulkerBulletEntity.class) ? null : "no shulker bullets";
			case HOURGLASS:
				return world.getServer().getGameRules().getInt(GameRules.RANDOM_TICK_SPEED) > 3 ? null : "the world does not tick faster";
			case MONSTER_MAGNET:
				return pig.getVelocity().length() > 0.3 ? null : "the pig was not pulled";
			case CARPET_BOMB:
			case TSAR_BOMBA:
				return any(world, center, TntEntity.class) ? null : "no bomb or missile is on its way";
			case FAULT_LINE:
				return count(world, center.add(-1, -1, 3), center.add(1, -1, 4), Blocks.GRASS_BLOCK) == 0 ? null : "no canyon opened";
			case TERMITES:
				return count(world, center.add(-1, 0, 3), center.add(1, 2, 5), Blocks.OAK_PLANKS) < 27 ? null : "the termites ate nothing";
			case DEATH_RAY:
			case RAILGUN:
				return pigHurt ? null : "the pig in the line of fire was not hurt";
			case TSUNAMI:
				return count(world, center.add(-6, 0, 1), center.add(6, 2, 3), Blocks.WATER) > 0 ? null : "no wave appeared";
			case TORNADO:
				return any(world, center, FallingBlockEntity.class) ? null : "the tornado tore up no ground";
			case SNAP:
				return pig.isRemoved() ? null : "the pig did not turn to dust";
			case CREEPER_CANNON:
			case CHARGED_CREEPER:
				return any(world, center, CreeperEntity.class) ? null : "no creeper was fired";
			case HOT_POTATO:
				return onGround(world, center, ComboItems.HOT_POTATO) > 0 ? null : "no potato was thrown";
			case FLOOR_IS_LAVA:
				return count(world, center.add(-9, -1, -9), center.add(9, -1, 9), Blocks.LAVA) > 30 && world.getBlockState(center.down()).isOf(Blocks.GRASS_BLOCK)
						? null : "the floor did not turn to lava, or the block under the tester did";
			case VOLCANO:
				return countAny(world, center.add(-9, 0, 3), center.add(9, 1, 21), Blocks.BASALT, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK) > 0
						? null : "no volcano started to grow";
			case LIGHTNING_RING:
				return any(world, center, LightningEntity.class) && pigHurt ? null : "no lightning, or the pig was not hurt";
			case FLAMETHROWER:
				return any(world, center, SmallFireballEntity.class) ? null : "no fire came out";
			case PIG_MISSILE:
				return p.getVehicle() instanceof PigEntity ? null : "the tester does not ride the pig missile";
			case KAIJU:
				return !world.getEntitiesByClass(LivingEntity.class, new Box(center).expand(20.0), Mayhem::isKaiju).isEmpty()
						? null : "no giant monster hatched";
			case RING_OF_FIRE:
				return count(world, center.add(-6, 0, -6), center.add(6, 2, 6), Blocks.FIRE) > 10 ? null : "no ring of fire";
			case ICE_SPIKES:
				return count(world, center.add(-2, 0, 2), center.add(2, 7, 4), Blocks.PACKED_ICE) > 0 && pigHurt ? null : "no ice spike, or the pig was not hurt";
			case SKY_ISLAND:
				return count(world, center.add(-10, 10, -6), center.add(10, 24, 16), Blocks.GRASS_BLOCK) > 0 ? null : "no island appeared in the sky";
			case BEANSTALK:
				return count(world, center.add(-4, 0, 0), center.add(5, 3, 11), Blocks.MOSS_BLOCK) > 0 ? null : "no beanstalk started to grow";
			case CASTLE:
				return countAny(world, center.add(-11, 0, 3), center.add(11, 1, 27), Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS) > 0
						? null : "no castle started to grow";
			case PYRAMID:
				return count(world, center.add(-11, 0, 3), center.add(11, 0, 27), Blocks.SMOOTH_SANDSTONE) > 0 ? null : "no pyramid started to grow";
			case MOUNTAIN:
				return count(world, center.add(-11, 0, -6), center.add(11, 0, 16), Blocks.STONE) > 0 ? null : "no mountain started to rise";
			case CRATER:
				return count(world, center.add(-3, -1, 2), center.add(3, -1, 8), Blocks.GRASS_BLOCK) < 20 ? null : "no crater was dug";
			case RAINBOW_BRIDGE:
				return countAny(world, center.add(-6, -1, -2), center.add(6, 10, 30), Blocks.RED_WOOL, Blocks.PURPLE_WOOL) > 0 ? null : "no rainbow bridge";
			case NETHER_PORTAL:
				return count(world, center.add(-4, -1, 0), center.add(4, 6, 10), Blocks.NETHER_PORTAL) == 6 ? null : "no lit Nether portal";
			case END_PORTAL:
				return count(world, center.add(-4, -1, 0), center.add(4, -1, 10), Blocks.END_PORTAL) == 9 ? null : "no End portal in the floor";
			case MITOSIS:
			case MITOSIS_BURST:
				return world.getEntitiesByClass(PigEntity.class, new Box(center).expand(12.0), e -> e.isAlive()).size() >= 2 ? null : "the pig did not split";
			case MENAGERIE:
				return !world.getEntitiesByClass(MobEntity.class, new Box(center).expand(20.0), e -> e != pig && e.isAlive()).isEmpty()
						? null : "no animal was fired";
			case PART_SEA:
				return count(world, center.add(-2, -1, 1), center.add(2, -1, 9), Blocks.WATER) < 45 ? null : "the water did not part";
			case SNOW_ARMY:
				return world.getEntitiesByClass(SnowGolemEntity.class, new Box(center).expand(12.0), e -> e.isAlive()).size() >= 4 ? null : "no snow golems";
			case STONE_WALL:
				return countAny(world, center.add(-4, 0, 3), center.add(4, 4, 3), Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE) > 0 ? null : "no wall rose";
			case STONE_DOME:
				return countAny(world, center.add(-6, -1, -6), center.add(6, 6, 6), Blocks.STONE_BRICKS, Blocks.GLASS) > 50 ? null : "no dome closed";
			case FARM:
				return countAny(world, center.add(-6, 0, -2), center.add(6, 0, 12), Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS) > 20
						? null : "no farm appeared";
			case WILD_MAGIC:
				return any(world, center, SmallFireballEntity.class) ? null : "the wild spell (a fireball) was not cast";
			case DICE:
				return p.experienceLevel >= 5 ? null : "a roll of 12 should give 5 levels, the tester has " + p.experienceLevel;
			case MUSICAL_CHAIRS:
				return p.getPos().distanceTo(standing) > 2.0 && pig.getPos().distanceTo(standing) < 1.0 ? null : "tester and pig did not swap places";
			case PARTY:
				return any(world, center, FireworkRocketEntity.class) ? null : "no fireworks";
			case FORCE_FIELD:
				return pig.getVelocity().length() > 0.3 ? null : "the pig was not pushed away";
			case ENCHANT_BOOK:
				return p.getInventory().count(Items.ENCHANTED_BOOK) == 1 && p.experienceLevel == 2
						? null : "5 levels should become an enchanted book and 2 levels, but there are "
								+ p.getInventory().count(Items.ENCHANTED_BOOK) + " books and " + p.experienceLevel + " levels";
			default:
				return null;
		}
	}

	// ------------------------------------------------------------------ 11. abilities that take time

	/** An ability that needs time, how many ticks to wait, and how to see that it worked. */
	private interface SlowCheck {
		String missing(ServerWorld world, BlockPos center);
	}

	/** Things that have to stand on the stage before the right-click. */
	private interface SlowSetup {
		void prepare(ServerWorld world, BlockPos center);
	}

	private record Slow(Use use, int ticks, SlowCheck check, SlowSetup setup) {
		Slow(Use use, int ticks, SlowCheck check) {
			this(use, ticks, check, null);
		}
	}

	private static double nearestCow(ServerWorld world, BlockPos center) {
		double nearest = 999.0;
		for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class, new Box(center).expand(30.0), e -> e.isAlive())) {
			nearest = Math.min(nearest, Math.sqrt(animal.squaredDistanceTo(Vec3d.ofBottomCenter(center))));
		}
		return nearest;
	}

	private static int floorLeft(ServerWorld world, BlockPos center) {
		return count(world, center.add(-12, -1, -12), center.add(12, -1, 12), Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_slow", tickLimit = 20000)
	public void slowEffects(TestContext context) {
		Report report = new Report("slow effects");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);
		TagKey<Block> anvils = TagKey.of(RegistryKeys.BLOCK, Identifier.ofVanilla("anvil"));

		List<Slow> list = List.of(
				new Slow(Use.MEGA_BOMB, 100, (w, c) -> floorLeft(w, c) < 600 ? null : "the floor is not damaged (" + floorLeft(w, c) + " of 625 blocks left)"),
				new Slow(Use.NUKE, 170, (w, c) -> floorLeft(w, c) < 300 ? null : "the floor is still there (" + floorLeft(w, c) + " of 625 blocks left)"),
				new Slow(Use.BLACK_HOLE, 220, (w, c) -> count(w, c.add(-2, -1, 1), c.add(2, -1, 6), Blocks.GRASS_BLOCK) < 30
						? null : "the black hole ate no blocks"),
				new Slow(Use.METEOR, 120, (w, c) -> floorLeft(w, c) < 625 ? null : "the meteor left no crater"),
				new Slow(Use.METEOR_SHOWER, 200, (w, c) -> floorLeft(w, c) < 615 ? null : "the meteor shower left no craters (" + floorLeft(w, c) + " of 625 blocks left)"),
				new Slow(Use.ANVIL_STORM, 120, (w, c) -> {
					for (BlockPos pos : BlockPos.iterate(c.add(-12, 0, -12), c.add(12, 3, 16))) {
						if (w.getBlockState(pos).isIn(anvils)) {
							return null;
						}
					}
					return "no anvil landed";
				}),
				new Slow(Use.CHICKEN_STORM, 40, (w, c) -> {
					int chickens = w.getEntitiesByClass(ChickenEntity.class, new Box(c).expand(40.0), e -> e.isAlive()).size();
					return chickens >= 10 ? null : "only " + chickens + " chickens appeared";
				}),
				new Slow(Use.HOLE, 190, (w, c) -> count(w, c.add(-1, -1, 4), c.add(1, -1, 6), Blocks.GRASS_BLOCK) == 9
						? null : "the portable hole did not close again"),
				// A cow that can walk stands 9 blocks away. After 5 seconds it must have come at least 2.5 blocks closer.
				new Slow(Use.LURE, 100, (w, c) -> nearestCow(w, c) < 6.5 ? null : "the cow did not follow (it is " + nearestCow(w, c) + " blocks away, it started 9 away)",
						(w, c) -> {
							MobEntity cow = spawnMob(w, EntityType.COW, c.getX() + 0.5, c.getY(), c.getZ() + 9.5);
							cow.setAiDisabled(false);
						}),
				// The dance lasts 8 seconds. After it the cow must be able to think again.
				new Slow(Use.DISCO, 180, (w, c) -> {
					List<MobEntity> cows = w.getEntitiesByClass(MobEntity.class, new Box(c).expand(30.0), e -> e.isAlive());
					if (cows.isEmpty()) {
						return "the cow is gone";
					}
					for (MobEntity cow : cows) {
						if (cow.isAiDisabled() || cow.getCommandTags().contains(Oddities.DANCE_TAG)) {
							return "the cow is still frozen after the dance";
						}
					}
					return null;
				}, (w, c) -> spawnMob(w, EntityType.COW, c.getX() + 2.5, c.getY(), c.getZ() + 2.5).setAiDisabled(false)),
				// The glowing outlines have to go away by themselves after 10 seconds.
				new Slow(Use.ORE_SIGHT, 230, (w, c) -> oreOutlines(w, c).isEmpty() ? null : "the ore outlines are still there: " + oreOutlines(w, c),
						(w, c) -> w.setBlockState(c.add(4, 0, -3), Blocks.IRON_ORE.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE)),
				// ---- version 1.4: mass destruction, instant creation and pure chaos
				new Slow(Use.ORBITAL_STRIKE, 100, (w, c) -> count(w, c.add(-2, -1, 2), c.add(2, -1, 7), Blocks.GRASS_BLOCK) < 10
						? null : "the beam from space burned no hole"),
				new Slow(Use.CLUSTER_BOMB, 140, (w, c) -> floorLeft(w, c) < 600 ? null : "the cluster bomb left " + floorLeft(w, c) + " of 625 floor blocks"),
				new Slow(Use.GRAVITY_GRENADE, 120, (w, c) -> floorLeft(w, c) < 620 ? null : "the ground did not fly away (" + floorLeft(w, c) + " of 625 left)"),
				new Slow(Use.SCATTER_BOMB, 60, (w, c) -> pigNear(w, Vec3d.ofBottomCenter(c.add(0, 0, 3))) ? "the pig was not teleported away" : null,
						(w, c) -> spawnMob(w, EntityType.PIG, c.getX() + 0.5, c.getY(), c.getZ() + 3.5)),
				new Slow(Use.GOLD_BOMB, 80, (w, c) -> count(w, c.add(-12, -2, -12), c.add(12, 8, 12), Blocks.GOLD_BLOCK) > 20 ? null : "too few gold blocks"),
				new Slow(Use.SHEEP_BOMB, 60, (w, c) -> w.getEntitiesByClass(SheepEntity.class, new Box(c).expand(40.0), e -> e.isAlive()).size() >= 10
						? null : "fewer than 10 sheep came out"),
				new Slow(Use.COBWEB_BOMB, 60, (w, c) -> count(w, c.add(-12, 0, -12), c.add(12, 8, 12), Blocks.COBWEB) > 5 ? null : "too few cobwebs"),
				new Slow(Use.HIVE_GRENADE, 60, (w, c) -> any(w, c, BeeEntity.class) ? null : "no bees came out"),
				new Slow(Use.TERMITES, 100, (w, c) -> count(w, c.add(-1, 0, 3), c.add(1, 2, 5), Blocks.OAK_PLANKS) == 0
						? null : count(w, c.add(-1, 0, 3), c.add(1, 2, 5), Blocks.OAK_PLANKS) + " of 27 planks are left", (w, c) -> termiteHouse(w, c)),
				new Slow(Use.DEATH_RAY, 80, (w, c) -> count(w, c.add(-1, -1, 3), c.add(1, -1, 6), Blocks.GRASS_BLOCK) < 12 ? null : "the death ray melted nothing"),
				// the wave carries a pig away (a pig that can think, so it can be moved)
				new Slow(Use.TSUNAMI, 40, (w, c) -> pigNear(w, Vec3d.ofBottomCenter(c.add(0, 0, 6))) ? "the wave did not carry the pig away" : null,
						(w, c) -> spawnMob(w, EntityType.PIG, c.getX() + 0.5, c.getY(), c.getZ() + 6.5).setAiDisabled(false)),
				new Slow(Use.TORNADO, 100, (w, c) -> floorLeft(w, c) < 615 ? null : "the tornado tore up too little ground (" + floorLeft(w, c) + " of 625 left)"),
				new Slow(Use.CREEPER_CANNON, 80, (w, c) -> floorLeft(w, c) < 625 ? null : "the creeper did not explode"),
				new Slow(Use.FLOOR_IS_LAVA, 200, (w, c) -> count(w, c.add(-12, -1, -12), c.add(12, -1, 12), Blocks.LAVA) == 0 && floorLeft(w, c) == 625
						? null : "the floor did not come back (" + floorLeft(w, c) + " of 625 grass blocks)"),
				new Slow(Use.VOLCANO, 120, (w, c) -> countAny(w, c.add(-9, 0, 3), c.add(9, 12, 21), Blocks.BASALT, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK) > 40
						? null : "no volcano"),
				new Slow(Use.SKY_ISLAND, 60, (w, c) -> count(w, c.add(-10, 10, -6), c.add(10, 24, 16), Blocks.GRASS_BLOCK) > 50 ? null : "no island in the sky"),
				new Slow(Use.BEANSTALK, 50, (w, c) -> count(w, c.add(-4, 0, 0), c.add(5, 64, 11), Blocks.MOSS_BLOCK) > 40 ? null : "the beanstalk did not grow"),
				new Slow(Use.CASTLE, 40, (w, c) -> countAny(w, c.add(-11, 0, 3), c.add(11, 11, 27), Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS,
						Blocks.CRACKED_STONE_BRICKS) > 300 ? null : "the castle is not finished"),
				new Slow(Use.PYRAMID, 40, (w, c) -> countAny(w, c.add(-11, 0, 3), c.add(11, 11, 27), Blocks.SANDSTONE, Blocks.CUT_SANDSTONE,
						Blocks.SMOOTH_SANDSTONE) > 300 ? null : "the pyramid is not finished"),
				new Slow(Use.MOUNTAIN, 50, (w, c) -> count(w, c.add(-11, 0, -6), c.add(11, 17, 16), Blocks.STONE) > 200 ? null : "the mountain did not rise"),
				new Slow(Use.PART_SEA, 440, (w, c) -> count(w, c.add(-2, -1, 1), c.add(2, -1, 9), Blocks.WATER) == 45 ? null : "the sea did not come back",
						(w, c) -> fill(w, c.add(-2, -1, 1), c.add(2, -1, 9), Blocks.WATER.getDefaultState())),
				new Slow(Use.CAKE_RAIN, 80, (w, c) -> count(w, c.add(-10, -1, -10), c.add(10, 3, 10), Blocks.CAKE) + onGround(w, c, Items.CAKE) > 0
						? null : "no cake fell"),
				new Slow(Use.PAINT_BOMB, 60, (w, c) -> floorLeft(w, c) < 615 ? null : "the paint bomb painted nothing"),
				new Slow(Use.WORLD_TREE, 70, (w, c) -> count(w, c.add(-8, 0, -4), c.add(8, 45, 14), Blocks.OAK_LOG) > 150 ? null : "the giant tree did not grow"),
				new Slow(Use.HOURGLASS, 230, (w, c) -> w.getServer().getGameRules().getInt(GameRules.RANDOM_TICK_SPEED) < 100
						? null : "the world still ticks fast after the hourglass ran out"),
				// ---- the cataclysms: they work on whole chunks
				// the canyon bends towards -x as it goes (the tester looks towards +z), so only x = -1 and 0 lie in it for 5 blocks
				new Slow(Use.FAULT_LINE, 60, (w, c) -> count(w, c.add(-1, -1, 3), c.add(0, -1, 7), Blocks.GRASS_BLOCK) == 0
						&& w.getBlockState(new BlockPos(c.getX(), w.getBottomY() + 1, c.getZ() + 3)).isOf(Blocks.LAVA) ? null : "no canyon down to the lava"),
				new Slow(Use.CARPET_BOMB, 160, (w, c) -> floorLeft(w, c) < 500 ? null : "the carpet bombing left " + floorLeft(w, c) + " of 625 floor blocks"),
				new Slow(Use.CHUNK_INVERT, 40, (w, c) -> chunkFloor(w, c, 0, Blocks.GRASS_BLOCK) == 0 ? null : "the chunk was not turned upside down",
						(w, c) -> clearLookChunk(w, c)),
				new Slow(Use.CHUNK_LAUNCH, 40, (w, c) -> chunkFloor(w, c, 0, Blocks.GRASS_BLOCK) == 0 && chunkFloor(w, c, 96, Blocks.GRASS_BLOCK) > 0
						? null : "the chunk did not fly 96 blocks up: " + chunkFloor(w, c, 0, Blocks.GRASS_BLOCK) + " grass blocks still on the floor, "
								+ chunkFloor(w, c, 96, Blocks.GRASS_BLOCK) + " up in the sky; in front of the tester the floor is "
								+ w.getBlockState(c.add(0, -1, 4)) + ", 96 blocks higher " + w.getBlockState(c.add(0, 95, 4))
								+ ", the chunk is " + new ChunkPos(c.add(0, 0, 4)) + " and the tester stands in " + new ChunkPos(c),
						(w, c) -> clearLookChunk(w, c)),
				new Slow(Use.ANNIHILATE, 120, (w, c) -> w.getBlockState(c.add(0, -1, 4)).isAir() && w.getBlockState(new BlockPos(c.getX(), w.getBottomY(), c.getZ() + 4)).isAir()
						? null : "the beam did not burn through to the void"),
				new Slow(Use.EVENT_HORIZON, 440, (w, c) -> floorLeft(w, c) < 500 ? null : "the event horizon ate too little (" + floorLeft(w, c) + " of 625 left)"),
				new Slow(Use.CHUNK_ERASE, 100, (w, c) -> chunkFloor(w, c, 0, Blocks.GRASS_BLOCK) == 0 ? null : "the chunk was not erased"),
				new Slow(Use.REGION_ERASE, 140, (w, c) -> floorLeft(w, c) == 0 ? null : "the 3 x 3 chunks were not erased (" + floorLeft(w, c) + " floor blocks left)"),
				new Slow(Use.TSAR_BOMBA, 160, (w, c) -> floorLeft(w, c) == 0 ? null : "the Tsar Bomba left " + floorLeft(w, c) + " floor blocks"),
				// last, because it destroys everything: the whole Armageddon, from the countdown to the final blast
				new Slow(Use.ARMAGEDDON, Doom.TOTAL + 80, (w, c) -> floorLeft(w, c) < 300 ? null : "the world did not end (" + floorLeft(w, c) + " of 625 floor blocks left)"));

		int[] index = {0};
		int[] waitUntil = {-1};
		int[] tick = {0};
		everyTickWhenReady(context, world, center, now -> {
			tick[0] = now;
			if (waitUntil[0] >= 0) {
				if (tick[0] < waitUntil[0]) {
					return;
				}
				Slow slow = list.get(index[0]);
				try {
					String missing = slow.check().missing(world, center);
					report.check(missing == null, slow.use() + ": " + missing);
					LOG.info("[slow effects] {} after {} ticks: {}", slow.use(), slow.ticks(), missing == null ? "ok" : missing);
				} catch (Throwable t) {
					report.problem(slow.use() + ": check crashed: " + t);
				}
				report.modErrors(slow.use().toString());
				waitUntil[0] = -1;
				index[0]++;
				return;
			}
			if (index[0] >= list.size()) {
				removePlayer(player);
				resetStage(world, center);
				forceChunks(world, center, false);
				report.finish(context);
				return;
			}
			Slow slow = list.get(index[0]);
			try {
				// find an item with this ability
				Item item = null;
				boolean sneak = false;
				for (Item candidate : ComboItems.ALL) {
					Traits traits = ComboItems.traits(candidate);
					if (traits != null && (traits.use == slow.use() || traits.sneakUse == slow.use())) {
						item = candidate;
						sneak = traits.use != slow.use();
						break;
					}
				}
				if (item == null) {
					report.problem("no item has the ability " + slow.use());
					index[0]++;
					return;
				}
				resetStage(world, center);
				resetPlayer(player, world, center);
				player.setInvulnerable(true);
				if (slow.setup() != null) {
					slow.setup().prepare(world, center);
				}
				ItemStack stack = new ItemStack(item);
				hold(player, stack);
				player.getItemCooldownManager().remove(item);
				player.setSneaking(sneak);
				ActionResult result = player.interactionManager.interactItem(player, world, stack, Hand.MAIN_HAND);
				report.check(result.isAccepted(), slow.use() + ": right-click with " + name(item) + " gave " + result);
				waitUntil[0] = tick[0] + slow.ticks();
			} catch (Throwable t) {
				report.problem(slow.use() + ": crashed: " + t);
				LOG.error("slow effects test " + slow.use(), t);
				index[0]++;
			}
		});
	}

	// ------------------------------------------------------------------ 11b. dice, gremlin, hot potato, storm crown, plague mask

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_mayhem", tickLimit = 20000)
	public void mayhem(TestContext context) {
		Report report = new Report("mayhem");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);
		int potatoDone = 2 + Mayhem.POTATO_FUSE + 20;
		int crownDone = potatoDone + 70;
		int maskDone = crownDone + 30;
		MobEntity[] zombie = {null};

		everyTickWhenReady(context, world, center, tick -> {
			if (tick == 1) {
				for (int roll = 1; roll <= 20; roll++) {
					int number = roll;
					step(report, "dice roll " + number, () -> {
						resetStage(world, center);
						resetPlayer(player, world, center);
						player.setInvulnerable(true);
						player.setExperienceLevel(0);
						hold(player, new ItemStack(ComboItems.DICE_OF_FATE));
						player.getInventory().setStack(3, new ItemStack(Items.STICK));
						String text = Wild.fate(world, player, number);
						LOG.info("[mayhem] dice {}: {}", number, text);
						String missing = switch (number) {
							case 1 -> any(world, center, WitherEntity.class) ? null : "no Wither appeared";
							case 5 -> player.getInventory().getStack(3).isEmpty() && onGround(world, center, Items.STICK) == 1 ? null : "the hotbar did not fall out";
							case 8 -> any(world, center, ZombieEntity.class) ? null : "no zombies appeared";
							case 12 -> player.experienceLevel == 5 ? null : "5 levels were expected, the tester has " + player.experienceLevel;
							case 17 -> onGround(world, center, Items.DIAMOND) > 0 ? null : "no diamonds fell";
							case 20 -> player.experienceLevel >= 30 && onGround(world, center, Items.NETHERITE_INGOT) > 0 ? null : "no jackpot";
							default -> null;
						};
						report.check(missing == null, "dice roll " + number + ": " + missing);
					});
				}
				for (int prank = 0; prank < Wild.PRANKS; prank++) {
					int number = prank;
					step(report, "gremlin prank " + number, () -> {
						resetStage(world, center);
						resetPlayer(player, world, center);
						player.setInvulnerable(true);
						hold(player, new ItemStack(ComboItems.GREMLIN_JAR));
						player.getInventory().setStack(4, new ItemStack(Items.STICK));
						world.setBlockState(center.add(2, 0, 2), Blocks.TORCH.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						world.setBlockState(center.add(-2, 0, 2), Blocks.OAK_TRAPDOOR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						String text = Wild.prank(world, player, number);
						LOG.info("[mayhem] prank {}: {}", number, text);
						report.check(player.getInventory().count(ComboItems.GREMLIN_JAR) == 1 && player.getInventory().count(Items.STICK) == 1,
								"gremlin prank " + number + ": an item was lost");
						String missing = switch (number) {
							case 2 -> any(world, center, ChickenEntity.class) ? null : "no chicken appeared";
							case 3 -> player.getVelocity().y > 0.3 ? null : "no hiccup";
							case 9 -> world.getBlockState(center.add(-2, 0, 2)).get(TrapdoorBlock.OPEN) ? null : "the trapdoor did not open";
							case 10 -> world.getBlockState(center.add(2, 0, 2)).isAir() ? null : "the torch was not stolen";
							default -> null;
						};
						report.check(missing == null, "gremlin prank " + number + ": " + missing);
					});
				}
				step(report, "copy-paste wand", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(true);
					ItemStack wand = new ItemStack(ComboItems.COPY_WAND);
					hold(player, wand);
					// the tester looks at the floor block 4 ahead: it becomes gold, is copied, and pasted on top of itself
					world.setBlockState(center.add(0, -1, 4), Blocks.GOLD_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
					player.setSneaking(true);
					player.interactionManager.interactItem(player, world, wand, Hand.MAIN_HAND);
					player.getItemCooldownManager().remove(wand.getItem());
					player.interactionManager.interactItem(player, world, wand, Hand.MAIN_HAND);
					player.getItemCooldownManager().remove(wand.getItem());
					player.setSneaking(false);
					player.interactionManager.interactItem(player, world, wand, Hand.MAIN_HAND);
					report.check(world.getBlockState(center.add(0, 0, 4)).isOf(Blocks.GOLD_BLOCK), "the Copy-Paste Wand did not paste the gold block");
				});
				step(report, "hot potato", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(true);
					player.getInventory().setStack(5, new ItemStack(ComboItems.HOT_POTATO));
				});
				return;
			}
			if (tick == potatoDone) {
				step(report, "hot potato", () -> {
					report.check(player.getInventory().count(ComboItems.HOT_POTATO) == 0, "the hot potato did not go off after "
							+ Mayhem.POTATO_FUSE / 20 + " seconds");
					report.check(floorLeft(world, center) < 625, "the hot potato left no crater");
				});
				step(report, "storm crown", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(true);
					equip(player, new ItemStack(ComboItems.STORM_CROWN));
					zombie[0] = spawnMob(world, EntityType.ZOMBIE, center.getX() + 0.5, center.getY(), center.getZ() + 6.5);
				});
				return;
			}
			if (tick == crownDone) {
				step(report, "storm crown", () -> report.check(zombie[0].getHealth() < zombie[0].getMaxHealth() || !zombie[0].isAlive(),
						"the Storm Crown did not strike the zombie"));
				step(report, "plague mask", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(true);
					equip(player, new ItemStack(ComboItems.PLAGUE_MASK));
					zombie[0] = spawnMob(world, EntityType.ZOMBIE, center.getX() + 0.5, center.getY(), center.getZ() + 3.5);
				});
				return;
			}
			if (tick == maskDone) {
				step(report, "plague mask", () -> report.check(zombie[0].hasStatusEffect(StatusEffects.WITHER), "the Plague Mask did not make the zombie wither"));
				resetPlayer(player, world, center);
				removePlayer(player);
				resetStage(world, center);
				forceChunks(world, center, false);
				report.finish(context);
			}
		});
	}

	// ------------------------------------------------------------------ 12. every event of the Chaos Orb

	private static String missingChaos(int number, ServerWorld world, ServerPlayerEntity p, BlockPos center, MobEntity pig, Vec3d standing, long timeBefore) {
		switch (number) {
			case 0:
				return onGround(world, center, Items.DIAMOND) > 0 ? null : "no diamonds fell";
			case 1:
				return onGround(world, center, Items.GOLD_INGOT) > 0 ? null : "no gold fell";
			case 2:
				return p.hasStatusEffect(StatusEffects.ABSORPTION) ? null : "no absorption effect";
			case 3:
				return p.experienceLevel >= 10 ? null : "no experience levels";
			case 4:
				return !p.getInventory().isEmpty() || !world.getEntitiesByClass(ItemEntity.class, new Box(center).expand(8.0), e -> e.isAlive()).isEmpty()
						? null : "no prize item";
			case 6:
				return p.getVelocity().y > 3.0 ? null : "the tester was not launched";
			case 7:
				return any(world, center, LightningEntity.class) ? null : "no lightning";
			case 8:
				return pig.isRemoved() ? null : "the pig was not changed";
			case 10:
				return pig.hasStatusEffect(StatusEffects.LEVITATION) ? null : "the pig does not float";
			case 11:
				return world.getServer().getOverworld().getTimeOfDay() == timeBefore + 12000L ? null : "the time did not jump half a day";
			case 12:
				return p.getPos().distanceTo(standing) > 1.0 ? null : "the tester was not moved";
			case 13:
				return any(world, center, IronGolemEntity.class) ? null : "no iron golems";
			case 14:
				return floorLeft(world, center) < 625 ? null : "the explosion left no crater";
			case 15:
				return any(world, center, CreeperEntity.class) ? null : "no creepers";
			case 16:
				return any(world, center, TntEntity.class) ? null : "no TNT";
			case 17:
				return p.hasStatusEffect(StatusEffects.NAUSEA) ? null : "no nausea";
			case 19: {
				EntityAttributeInstance scale = p.getAttributeInstance(Registries.ATTRIBUTE.getEntry(Identifier.ofVanilla("generic.scale")).orElseThrow());
				return scale != null && scale.getValue() > 1.5 ? null : "the tester did not grow";
			}
			case 20:
				return onGround(world, center, Items.GOLDEN_APPLE) > 0 ? null : "no golden apples fell";
			case 21:
				return count(world, center.add(-6, 0, -6), center.add(6, 0, 6), Blocks.SNOW) > 0 ? null : "no snow";
			case 22:
				return world.getBlockState(center.down()).isOf(Blocks.GOLD_BLOCK) ? null : "the ground did not turn to gold";
			case 23:
				return world.getServer().getTickManager().isFrozen() ? null : "time is not frozen";
			default:
				return null; // 5 chickens, 9 anvils and 18 black hole take time and are covered by the test "slow effects"
		}
	}

	/** The random teleport of the Chaos Orb in the Nether: it must find a spot, and never one on top of the bedrock roof. */
	private static void netherTeleport(Report report, MinecraftServer server, ServerPlayerEntity player) {
		try {
			ServerWorld nether = server.getWorld(World.NETHER);
			if (nether == null) {
				report.problem("the test server has no Nether");
				return;
			}
			// a small room in the middle of the Nether to start from
			BlockPos base = new BlockPos(8, 64, 8);
			fill(nether, base.add(-3, -1, -3), base.add(3, -1, 3), Blocks.NETHERRACK.getDefaultState());
			fill(nether, base.add(-3, 0, -3), base.add(3, 3, 3), Blocks.AIR.getDefaultState());
			resetPlayer(player, player.getServerWorld(), player.getBlockPos());
			player.setInvulnerable(true);
			Vec3d start = new Vec3d(8.5, 64.0, 8.5);
			int moved = 0;
			for (int i = 0; i < 6; i++) {
				player.teleport(nether, start.x, start.y, start.z, 0.0F, 0.0F);
				Chaos.event(nether, player, 12);
				BlockPos feet = player.getBlockPos();
				report.check(player.getWorld() == nether, "the random teleport left the Nether");
				report.check(feet.getY() < 123, "the random teleport in the Nether ended at height " + feet.getY() + ", in or above the roof");
				if (player.getPos().distanceTo(start) > 1.0) {
					moved++;
					report.check(nether.getBlockState(feet.down()).isSolidBlock(nether, feet.down())
									&& nether.getBlockState(feet).getCollisionShape(nether, feet).isEmpty()
									&& nether.getBlockState(feet.up()).getCollisionShape(nether, feet.up()).isEmpty()
									&& nether.getFluidState(feet).isEmpty(),
							"the random teleport in the Nether ended in a bad spot: " + feet + " on " + nether.getBlockState(feet.down()));
				}
			}
			report.info("Nether: the random teleport moved the tester " + moved + " of 6 times");
			report.check(moved > 0, "the random teleport never found a spot in the Nether");
		} catch (Throwable t) {
			report.problem("the random teleport in the Nether crashed: " + t);
			LOG.error("nether teleport test", t);
		}
		report.modErrors("chaos teleport in the Nether");
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_chaos", tickLimit = 20000)
	public void chaos(TestContext context) {
		Report report = new Report("chaos");
		ServerWorld world = context.getWorld();
		MinecraftServer server = world.getServer();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		int[] next = {0};
		int[] tick = {0};
		int[] doneAt = {-1};
		everyTickWhenReady(context, world, center, now -> {
			tick[0] = now;
			if (next[0] < Chaos.EVENTS) {
				int number = next[0]++;
				try {
					if (server.getTickManager().isFrozen()) {
						server.getTickManager().setFrozen(false);
					}
					resetStage(world, center);
					resetPlayer(player, world, center);
					player.setInvulnerable(true);
					player.setExperienceLevel(0);
					MobEntity pig = spawnMob(world, EntityType.PIG, center.getX() + 0.5, center.getY(), center.getZ() + 3.5);
					Vec3d standing = player.getPos();
					long timeBefore = server.getOverworld().getTimeOfDay();
					boolean done = Chaos.event(world, player, number);
					report.check(done, "chaos event " + number + " did nothing");
					String missing = missingChaos(number, world, player, center, pig, standing, timeBefore);
					report.check(missing == null, "chaos event " + number + ": " + missing);
					LOG.info("[chaos] event {}: {}", number, missing == null ? "ok" : missing);
				} catch (Throwable t) {
					report.problem("chaos event " + number + " crashed: " + t);
					LOG.error("chaos test " + number, t);
				}
				report.modErrors("chaos event " + number);
				if (next[0] == Chaos.EVENTS) {
					doneAt[0] = tick[0];
				}
				return;
			}
			// The last event is the time stop (5 seconds). Wait until everything is over.
			if (tick[0] - doneAt[0] < 300) {
				return;
			}
			report.check(!server.getTickManager().isFrozen(), "time is still frozen long after the chaos time stop");
			if (server.getTickManager().isFrozen()) {
				server.getTickManager().setFrozen(false);
			}
			netherTeleport(report, server, player);
			place(player, world, center);
			removePlayer(player);
			resetStage(world, center);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	// ------------------------------------------------------------------ 13. items that remember things, and effects that take time

	private static void backpackChecks(Report report, ServerWorld world, ServerPlayerEntity p, BlockPos center) {
		Item small = find("small backpack", t -> t.use == Use.BACKPACK && t.power < 6.0F);
		Item big = find("big backpack", t -> t.use == Use.BACKPACK && t.power >= 6.0F);
		resetStage(world, center);
		resetPlayer(p, world, center);
		ItemStack bag = new ItemStack(small);
		hold(p, bag);
		p.interactionManager.interactItem(p, world, bag, Hand.MAIN_HAND);
		if (!(p.currentScreenHandler instanceof BackpackScreenHandler handler)) {
			report.problem("right-click did not open the backpack");
			return;
		}
		report.check(handler.slots.size() == 27 + 36, "the small backpack has " + (handler.slots.size() - 36) + " slots instead of 27");
		report.check(handler.canUse(p), "the backpack window closes at once");

		// What is put in is stored inside the item.
		handler.getSlot(0).setStack(new ItemStack(Items.COBBLESTONE, 32));
		report.check(holds(bag, Items.COBBLESTONE) == 32, "cobblestone put into the backpack is not stored in the item");
		p.getInventory().setStack(9, new ItemStack(Items.DIRT, 5));
		handler.onSlotClick(27, 0, SlotActionType.QUICK_MOVE, p);
		report.check(holds(bag, Items.DIRT) == 5 && p.getInventory().count(Items.DIRT) == 0, "shift-click did not move dirt into the backpack");

		// A backpack or a shulker box cannot go into a backpack.
		p.getInventory().setStack(10, new ItemStack(small));
		p.getInventory().setStack(11, new ItemStack(Items.SHULKER_BOX));
		handler.onSlotClick(28, 0, SlotActionType.QUICK_MOVE, p);
		handler.onSlotClick(29, 0, SlotActionType.QUICK_MOVE, p);
		handler.onSlotClick(28, 0, SlotActionType.PICKUP, p);
		report.check(handler.getCursorStack().isEmpty(), "a backpack could be picked up while a backpack is open");
		report.check(holds(bag, small) == 0, "a backpack went into a backpack");
		report.check(holds(bag, Items.SHULKER_BOX) == 0, "a shulker box went into a backpack");
		report.check(p.getInventory().getStack(10).isOf(small) && p.getInventory().getStack(11).isOf(Items.SHULKER_BOX),
				"the second backpack or the shulker box vanished from the inventory");
		// Number key 1 would swap the open backpack itself into the window.
		handler.onSlotClick(5, 0, SlotActionType.SWAP, p);
		report.check(p.getMainHandStack() == bag && holds(bag, small) == 0, "the open backpack could be moved into itself with a number key");
		p.closeHandledScreen();

		// Close and open again: everything is still there, and it can be taken out.
		p.interactionManager.interactItem(p, world, bag, Hand.MAIN_HAND);
		if (p.currentScreenHandler instanceof BackpackScreenHandler again) {
			ItemStack first = again.getSlot(0).getStack();
			report.check(first.isOf(Items.COBBLESTONE) && first.getCount() == 32, "after opening again the first slot holds " + first);
			again.onSlotClick(0, 0, SlotActionType.QUICK_MOVE, p);
			report.check(p.getInventory().count(Items.COBBLESTONE) == 32 && holds(bag, Items.COBBLESTONE) == 0,
					"shift-click did not take the cobblestone out of the backpack");
		} else {
			report.problem("the backpack did not open a second time");
		}
		p.closeHandledScreen();

		// Two backpacks make a big one that keeps what was in both.
		ItemStack other = new ItemStack(small);
		other.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(List.of(new ItemStack(Items.DIAMOND, 3))));
		ItemStack merged = ComboRecipes.craft(bag, other);
		report.check(merged.isOf(big), "two backpacks give " + merged + " instead of a big backpack");
		report.check(holds(merged, Items.DIRT) == 5 && holds(merged, Items.DIAMOND) == 3, "the big backpack lost what was in the two small ones");
		p.getInventory().clear();
		hold(p, merged);
		p.interactionManager.interactItem(p, world, merged, Hand.MAIN_HAND);
		if (p.currentScreenHandler instanceof BackpackScreenHandler large) {
			report.check(large.slots.size() == 54 + 36, "the big backpack has " + (large.slots.size() - 36) + " slots instead of 54");
			int dirt = 0;
			int diamonds = 0;
			for (int i = 0; i < 54 && i < large.slots.size(); i++) {
				ItemStack inside = large.getSlot(i).getStack();
				dirt += inside.isOf(Items.DIRT) ? inside.getCount() : 0;
				diamonds += inside.isOf(Items.DIAMOND) ? inside.getCount() : 0;
			}
			report.check(dirt == 5 && diamonds == 3, "the window of the big backpack shows " + dirt + " dirt and " + diamonds + " diamonds instead of 5 and 3");
		} else {
			report.problem("the big backpack did not open");
		}
		p.closeHandledScreen();
	}

	private static void waystoneChecks(Report report, ServerWorld world, ServerPlayerEntity p, BlockPos center) {
		Item stone = find("waystone", t -> t.use == Use.WAYPOINT_GO && t.sneakUse == Use.WAYPOINT_SET);
		resetStage(world, center);
		resetPlayer(p, world, center);
		ItemStack stack = new ItemStack(stone);
		hold(p, stack);
		ActionResult fresh = p.interactionManager.interactItem(p, world, stack, Hand.MAIN_HAND);
		report.check(fresh == ActionResult.FAIL, "a new waystone gave " + fresh + " although it remembers no spot yet");
		p.setSneaking(true);
		ActionResult set = p.interactionManager.interactItem(p, world, stack, Hand.MAIN_HAND);
		report.check(set.isAccepted(), "sneak + right-click with the waystone gave " + set);
		p.setSneaking(false);
		p.getItemCooldownManager().remove(stone);
		Vec3d home = p.getPos();
		p.teleport(world, home.x + 9.0, home.y, home.z - 7.0, 90.0F, 0.0F);
		report.check(p.getPos().distanceTo(home) > 5.0, "test setup: the tester could not be moved away");
		ActionResult go = p.interactionManager.interactItem(p, world, stack, Hand.MAIN_HAND);
		double off = p.getPos().distanceTo(home);
		report.check(go.isAccepted() && off < 0.1, "the waystone (" + go + ") left the tester " + off + " blocks away from the remembered spot");
		report.check(p.getItemCooldownManager().isCoolingDown(stone), "the waystone has no cooldown after the trip");
		report.check(p.getMainHandStack() == stack, "the waystone was used up");
		p.getItemCooldownManager().remove(stone);
	}

	private static void mobNetChecks(Report report, ServerWorld world, ServerPlayerEntity p, BlockPos center) {
		Item net = find("mob net", t -> t.capture);
		resetStage(world, center);
		resetPlayer(p, world, center);
		ItemStack stack = new ItemStack(net);
		hold(p, stack);
		MobEntity pig = spawnMob(world, EntityType.PIG, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
		pig.setCustomName(Text.literal("Test Pig"));
		pig.setHealth(7.0F);

		// Catch it the way the real game does: the client sends "the player right-clicked this mob".
		p.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interactAt(pig, false, Hand.MAIN_HAND, new Vec3d(0.0, 0.4, 0.0)));
		report.check(pig.isRemoved(), "the pig is still there after the right-click with the net");
		report.check(Capture.isFull(stack), "the net is empty after the right-click on the pig");
		report.check(stack.getDamage() == 0, "the net wore out when the mob went in (it could break with a mob inside)");

		// A full net does not take a second mob.
		MobEntity cow = spawnMob(world, EntityType.COW, center.getX() - 1.0, center.getY(), center.getZ() + 2.5);
		p.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interactAt(cow, false, Hand.MAIN_HAND, new Vec3d(0.0, 0.4, 0.0)));
		report.check(cow.isAlive() && !cow.isRemoved(), "a full net swallowed a second mob");
		cow.discard();

		// Right-click on the floor: the same pig comes out again.
		BlockPos floor = center.add(2, -1, 2);
		ActionResult out = p.interactionManager.interactBlock(p, world, stack, Hand.MAIN_HAND, hitTop(floor));
		List<PigEntity> pigs = world.getEntitiesByClass(PigEntity.class, new Box(floor.up()).expand(1.0), e -> e.isAlive());
		report.check(out.isAccepted() && pigs.size() == 1, "letting the mob out (" + out + ") gave " + pigs.size() + " pigs instead of 1");
		if (pigs.size() == 1) {
			PigEntity back = pigs.get(0);
			report.check(Math.abs(back.getHealth() - 7.0F) < 0.01F, "the pig came out with " + back.getHealth() + " health instead of 7");
			report.check(back.hasCustomName() && back.getCustomName().getString().equals("Test Pig"), "the pig lost its name in the net");
		}
		report.check(!Capture.isFull(stack), "the net is still full after the mob came out");
		report.check(stack.getDamage() == 1, "one catch should cost 1 durability, it cost " + stack.getDamage());
		ActionResult empty = p.interactionManager.interactBlock(p, world, stack, Hand.MAIN_HAND, hitTop(floor));
		report.check(world.getEntitiesByClass(PigEntity.class, new Box(floor.up()).expand(3.0), e -> e.isAlive()).size() == 1,
				"an empty net (" + empty + ") let out a second pig");
	}

	private static void lootChecks(Report report, ServerWorld world, ServerPlayerEntity p, BlockPos center) {
		Item blade = find("double loot", t -> t.doubleLoot);
		// First with a normal sword, to know what one sheep drops.
		resetStage(world, center);
		resetPlayer(p, world, center);
		hold(p, new ItemStack(Items.IRON_SWORD));
		MobEntity sheep = spawnMob(world, EntityType.SHEEP, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
		sheep.damage(world.getDamageSources().playerAttack(p), 1000.0F);
		int normal = onGround(world, center, Items.WHITE_WOOL);
		report.check(!sheep.isAlive() && normal == 1, "test setup: a sheep killed with an iron sword dropped " + normal + " wool instead of 1");

		resetStage(world, center);
		hold(p, new ItemStack(blade));
		sheep = spawnMob(world, EntityType.SHEEP, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
		sheep.damage(world.getDamageSources().playerAttack(p), 1000.0F);
		int doubled = onGround(world, center, Items.WHITE_WOOL);
		report.check(doubled == 2 * normal, name(blade) + ": a sheep dropped " + doubled + " wool, expected " + 2 * normal);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_keepsakes", tickLimit = 20000)
	public void keepsakes(TestContext context) {
		Report report = new Report("keepsakes");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);
		MobEntity[] golem = {null};
		Stat<?> sinceRest = Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST);

		everyTickWhenReady(context, world, center, tick -> {
			if (tick == 1) {
				step(report, "backpack", () -> backpackChecks(report, world, player, center));
				step(report, "waystone", () -> waystoneChecks(report, world, player, center));
				step(report, "mob net", () -> mobNetChecks(report, world, player, center));
				step(report, "double loot", () -> lootChecks(report, world, player, center));
				step(report, "start of bleeding and of the dreamcatcher", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					// Two hits with the bleeding sword on an iron golem (100 health, no armor).
					hold(player, new ItemStack(find("bleeding", t -> t.bleedSeconds > 0)));
					golem[0] = spawnMob(world, EntityType.IRON_GOLEM, center.getX() + 0.5, center.getY(), center.getZ() + 3.5);
					golem[0].damage(world.getDamageSources().playerAttack(player), 6.0F);
					golem[0].timeUntilRegen = 0;
					golem[0].damage(world.getDamageSources().playerAttack(player), 6.0F);
					report.check(Math.abs(golem[0].getHealth() - 88.0F) < 0.01F,
							"test setup: two hits of 6 should leave the golem at 88 health, it has " + golem[0].getHealth());
					// The game counts how long the player has not slept. Phantoms come when the number is high.
					hold(player, new ItemStack(find("no phantoms", t -> t.noPhantoms)));
					player.getStatHandler().setStat(player, sinceRest, 100000);
				});
				return;
			}
			if (tick < 81) {
				return;
			}
			step(report, "bleeding and the dreamcatcher after 4 seconds", () -> {
				if (golem[0] != null) {
					float health = golem[0].getHealth();
					report.info("bleeding: the golem went from 88 to " + health + " health in 4 seconds");
					report.check(health <= 85.5F, "the golem did not bleed (health " + health + ", expected about 84)");
					report.check(health >= 82.5F, "the bleeding from two hits added up (health " + health + ", expected about 84)");
				}
				int rest = player.getStatHandler().getStat(sinceRest);
				report.check(rest < 200, "the Dreamcatcher did not reset the time without sleep (it is " + rest + " ticks)");
			});
			resetStage(world, center);
			removePlayer(player);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}

	// ------------------------------------------------------------------ 14. odd charms: things that work while carried, held or worn

	private static void quickCharmChecks(Report report, ServerWorld world, ServerPlayerEntity p, BlockPos center) {
		resetStage(world, center);
		resetPlayer(p, world, center);

		// Double jump: the game client reports the jump, the server forgets the fall height. Without the item it must not.
		p.fallDistance = 5.0F;
		Charms.trick(p, Charms.TRICK_DOUBLE_JUMP);
		report.check(p.fallDistance == 5.0F, "a double jump was accepted from a player without the item");
		hold(p, new ItemStack(find("double jump", t -> t.airJumps > 0)));
		int seen = Charms.tricksSeen;
		Charms.trick(p, Charms.TRICK_DOUBLE_JUMP);
		report.check(p.fallDistance == 0.0F && Charms.tricksSeen == seen + 1, "the double jump did not reset the fall height");
		p.fallDistance = 5.0F;
		Charms.trick(p, Charms.TRICK_CLIMB);
		report.check(p.fallDistance == 5.0F, "wall climbing was accepted from a player without the boots");
		p.getInventory().clear();
		p.equipStack(EquipmentSlot.FEET, new ItemStack(find("wall climbing", t -> t.wallClimb)));
		Charms.trick(p, Charms.TRICK_CLIMB);
		report.check(p.fallDistance == 0.0F, "wall climbing did not reset the fall height");
		p.getInventory().clear();

		// Pocket Mirror: an arrow shot by a skeleton hurts the skeleton, not the tester. Then the mirror needs a pause.
		Item mirror = find("mirror", t -> t.reflectCooldown > 0);
		hold(p, new ItemStack(mirror));
		p.getItemCooldownManager().remove(mirror);
		MobEntity skeleton = spawnMob(world, EntityType.SKELETON, center.getX() + 0.5, center.getY(), center.getZ() + 5.5);
		ArrowEntity arrow = new ArrowEntity(world, skeleton, new ItemStack(Items.ARROW), null);
		DamageSource shot = world.getDamageSources().arrow(arrow, skeleton);
		float before = skeleton.getHealth();
		boolean hurt = p.damage(shot, 4.0F);
		report.check(!hurt && p.getHealth() == p.getMaxHealth(), "the arrow hurt the tester although the mirror was ready");
		report.check(skeleton.getHealth() < before, "the arrow was not sent back to the skeleton");
		report.check(p.getItemCooldownManager().isCoolingDown(mirror), "the mirror needs no pause after a shot");
		p.timeUntilRegen = 0;
		p.damage(shot, 4.0F);
		report.check(p.getHealth() < p.getMaxHealth(), "the mirror also stopped a second arrow right after the first");
		p.getItemCooldownManager().remove(mirror);
		skeleton.discard();

		// Piggy Bank: 10 levels in, 10 levels out.
		resetPlayer(p, world, center);
		Item bankItem = find("piggy bank", t -> t.use == Use.BANK_IN);
		ItemStack bank = new ItemStack(bankItem);
		hold(p, bank);
		p.setExperienceLevel(10);
		p.interactionManager.interactItem(p, world, bank, Hand.MAIN_HAND);
		report.check(p.experienceLevel == 0 && Oddities.stored(bank) == 160, "10 levels did not go into the piggy bank as 160 points");
		p.getItemCooldownManager().remove(bankItem);
		p.setSneaking(true);
		p.interactionManager.interactItem(p, world, bank, Hand.MAIN_HAND);
		p.setSneaking(false);
		int back = Oddities.points(p.experienceLevel, p.experienceProgress);
		report.check(Oddities.stored(bank) == 0 && Math.abs(back - 160) <= 2, "the piggy bank gave back " + back + " of 160 points");
		p.getItemCooldownManager().remove(bankItem);
		p.setExperienceLevel(0);

		// Rodeo Saddle: a right-click on a cow, sent the way the real game sends it.
		resetPlayer(p, world, center);
		ItemStack saddle = new ItemStack(find("riding any mob", t -> t.mount));
		hold(p, saddle);
		MobEntity cow = spawnMob(world, EntityType.COW, center.getX() + 0.5, center.getY(), center.getZ() + 2.5);
		p.networkHandler.onPlayerInteractEntity(PlayerInteractEntityC2SPacket.interactAt(cow, false, Hand.MAIN_HAND, new Vec3d(0.0, 0.4, 0.0)));
		report.check(p.getVehicle() == cow, "the tester does not sit on the cow after the right-click with the saddle");
		report.check(saddle.getDamage() == 1, "mounting should cost 1 durability, it cost " + saddle.getDamage());
		p.stopRiding();
		cow.discard();

		// Explorer's Almanac: the line it shows must name the position and the day.
		String line = Charms.almanacText(world, p).getString();
		BlockPos at = p.getBlockPos();
		report.check(line.contains("X " + at.getX()) && line.contains("Y " + at.getY()) && line.contains("Z " + at.getZ()) && line.contains("Day "),
				"the almanac shows a wrong line: " + line);
		report.info("almanac: " + line);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_charms", tickLimit = 20000)
	public void oddCharms(TestContext context) {
		Report report = new Report("odd charms");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);
		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		Item watch = find("rewind", t -> t.use == Use.REWIND);
		// The walk for the flower boots: 23 steps along a row of grass, away from everything else.
		int walkStart = 220;
		int walkSteps = 23;
		int walkEnd = walkStart + walkSteps * 4;
		// The lava pool for the lava boots: 3x3 blocks in the floor, in front of the tester.
		BlockPos poolFrom = center.add(-1, -1, 1);
		BlockPos poolTo = center.add(1, -1, 3);

		everyTickWhenReady(context, world, center, tick -> {
			if (tick == 1) {
				step(report, "quick checks", () -> quickCharmChecks(report, world, player, center));
				step(report, "handing out the charms", () -> {
					resetStage(world, center);
					resetPlayer(player, world, center);
					// young wheat on all 24 blocks around the tester
					for (BlockPos pos : BlockPos.iterate(center.add(-2, -1, -2), center.add(2, -1, 2))) {
						if (pos.getX() != center.getX() || pos.getZ() != center.getZ()) {
							world.setBlockState(pos.toImmutable(), Blocks.FARMLAND.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
							world.setBlockState(pos.up(), wheat.withAge(0), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
						}
					}
					hold(player, new ItemStack(find("balloon", t -> t.balloon)));
					player.getInventory().setStack(1, new ItemStack(watch));
					player.getInventory().setStack(2, new ItemStack(find("light", t -> t.lantern)));
					player.getInventory().setStack(3, new ItemStack(find("lunchbox", t -> t.autoEat)));
					player.getInventory().setStack(10, new ItemStack(Items.BREAD, 3));
					player.getInventory().setStack(11, new ItemStack(Items.GOLDEN_APPLE));
					player.equipStack(EquipmentSlot.FEET, new ItemStack(find("flower boots", t -> t.meadow)));
					player.getHungerManager().setFoodLevel(5);
				});
			} else if (tick == 14) {
				step(report, "balloon and light", () -> {
					report.check(player.hasStatusEffect(StatusEffects.LEVITATION), "holding the balloon does not lift the tester");
					report.check(world.getBlockState(center.up()).isOf(Blocks.LIGHT), "there is no light at the tester's head");
					player.setSneaking(true);
				});
			} else if (tick == 24) {
				step(report, "balloon while sneaking", () -> {
					report.check(!player.hasStatusEffect(StatusEffects.LEVITATION), "sneaking with the balloon still lifts the tester");
					report.check(player.hasStatusEffect(StatusEffects.SLOW_FALLING), "sneaking with the balloon gives no soft fall");
					player.setSneaking(false);
				});
			} else if (tick == 50) {
				step(report, "lunchbox", () -> {
					int food = player.getHungerManager().getFoodLevel();
					report.check(food > 5 && player.getInventory().count(Items.BREAD) < 3,
							"the lunchbox did not feed the tester (hunger " + food + " of 20, " + player.getInventory().count(Items.BREAD) + " of 3 bread left)");
					report.check(player.getInventory().count(Items.GOLDEN_APPLE) == 1, "the lunchbox ate the golden apple");
				});
			} else if (tick == 118) {
				step(report, "rewind watch", () -> {
					// The tester stood on the same spot with full health for more than 5 seconds. Now move away, get hurt, and rewind.
					Vec3d home = player.getPos();
					player.teleport(world, home.x + 7.0, home.y, home.z - 6.0, 90.0F, 0.0F);
					player.setHealth(6.0F);
					player.getInventory().selectedSlot = 1;
					ItemStack stack = player.getMainHandStack();
					ActionResult result = player.interactionManager.interactItem(player, world, stack, Hand.MAIN_HAND);
					double off = player.getPos().distanceTo(home);
					report.check(result.isAccepted() && off < 0.1, "the watch (" + result + ") left the tester " + off + " blocks away from the old spot");
					report.check(player.getHealth() == player.getMaxHealth(), "the watch did not bring back the old health, it is " + player.getHealth());
					report.check(player.getItemCooldownManager().isCoolingDown(watch), "the watch has no cooldown");
					report.check(stack.getDamage() == 1, "the watch did not wear out by 1");
					player.getInventory().selectedSlot = 0;
				});
			} else if (tick == walkStart - 4) {
				step(report, "crops near the flower boots", () -> {
					int grown = 0;
					for (BlockPos pos : BlockPos.iterate(center.add(-2, 0, -2), center.add(2, 0, 2))) {
						BlockState state = world.getBlockState(pos);
						if (state.isOf(Blocks.WHEAT) && wheat.getAge(state) > 0) {
							grown++;
						}
					}
					report.info("flower boots: " + grown + " of 24 wheat plants grew in 10 seconds");
					report.check(grown > 0, "no wheat plant near the flower boots grew in 10 seconds");
				});
			} else if (tick >= walkStart && tick < walkEnd && (tick - walkStart) % 4 == 0) {
				int stepNumber = (tick - walkStart) / 4;
				BlockPos spot = center.add(-11 + stepNumber, 0, -7);
				player.teleport(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 0.0F);
			} else if (tick == walkEnd + 4) {
				step(report, "flowers behind the flower boots, and the light that follows", () -> {
					int flowers = 0;
					for (BlockPos pos : BlockPos.iterate(center.add(-11, 0, -7), center.add(11, 0, -7))) {
						if (!world.getBlockState(pos).isAir()) {
							flowers++;
						}
					}
					report.info("flower boots: " + flowers + " flowers on a walk of " + walkSteps + " blocks");
					report.check(flowers > 0, "no flower grew on a walk of " + walkSteps + " blocks over grass");
					int lights = count(world, center.add(-14, 0, -14), center.add(14, 4, 14), Blocks.LIGHT);
					report.check(lights == 1 && world.getBlockState(player.getBlockPos().up()).isOf(Blocks.LIGHT),
							"the light should be one block at the tester's head, but there are " + lights + " light blocks");
					// Now the lava boots: everything else goes away.
					player.getInventory().clear();
					place(player, world, center);
					fill(world, center.add(-2, 0, -2), center.add(2, 0, 2), Blocks.AIR.getDefaultState()); // the wheat goes away
					fill(world, center.add(-1, -2, 1), center.add(1, -2, 3), Blocks.STONE.getDefaultState());
					fill(world, poolFrom, poolTo, Blocks.LAVA.getDefaultState());
					player.equipStack(EquipmentSlot.FEET, new ItemStack(find("lava boots", t -> t.lavaWalk)));
				});
			} else if (tick == walkEnd + 10) {
				step(report, "lava boots", () -> {
					report.check(count(world, center.add(-14, 0, -14), center.add(14, 4, 14), Blocks.LIGHT) == 0,
							"the light is still there although the jar is gone");
					int hard = count(world, poolFrom, poolTo, Blocks.SMOOTH_BASALT);
					int lava = count(world, poolFrom, poolTo, Blocks.LAVA);
					report.check(hard == 6 && lava == 3, "next to the lava boots " + hard + " lava blocks hardened and " + lava
							+ " stayed lava, expected 6 and 3");
					player.getInventory().clear();
				});
			} else if (tick == walkEnd + 100) {
				step(report, "the lava melts again", () -> {
					int lava = count(world, poolFrom, poolTo, Blocks.LAVA);
					report.check(lava == 9, "after the lava boots are gone only " + lava + " of 9 blocks are lava again");
				});
				resetStage(world, center);
				removePlayer(player);
				forceChunks(world, center, false);
				report.finish(context);
			}
		});
	}
}
