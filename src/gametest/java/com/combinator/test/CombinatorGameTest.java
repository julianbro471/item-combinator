package com.combinator.test;

import com.combinator.ItemCombinator;
import com.combinator.ability.Chaos;
import com.combinator.ability.CombatAbilities;
import com.combinator.item.ComboItems;
import com.combinator.item.Traits;
import com.combinator.item.Use;
import com.combinator.recipe.ComboRecipes;
import com.combinator.screen.CombinerScreenHandler;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.component.DataComponentTypes;
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
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
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
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
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
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
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

	// ------------------------------------------------------------------ 1. combinations

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_recipes", tickLimit = 200)
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_data", tickLimit = 200)
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_table", tickLimit = 400)
	public void combinerTable(TestContext context) {
		Report report = new Report("table");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_mining", tickLimit = 600)
	public void mining(TestContext context) {
		Report report = new Report("mining");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
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
			Item wanted = smelt ? Items.IRON_INGOT : Items.RAW_IRON;
			Item unwanted = smelt ? Items.RAW_IRON : Items.IRON_INGOT;
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
			fill(world, oreFrom, oreTo, Blocks.AIR.getDefaultState());
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_combat", tickLimit = 600)
	public void combat(TestContext context) {
		Report report = new Report("combat");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_defense", tickLimit = 600)
	public void defense(TestContext context) {
		Report report = new Report("defense");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_blockuse", tickLimit = 600)
	public void blockUse(TestContext context) {
		Report report = new Report("block use");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
			int special = 0;
			for (Item item : ComboItems.ALL) {
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
						world.setBlockState(target.up(), Blocks.OAK_LOG.getDefaultState());
						player.interactionManager.interactBlock(player, world, player.getMainHandStack(), Hand.MAIN_HAND, hitTop(target.up()));
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_food", tickLimit = 600)
	public void food(TestContext context) {
		Report report = new Report("food");
		ServerWorld world = context.getWorld();
		BlockPos center = stageCenter(context);
		forceChunks(world, center, true);
		resetStage(world, center);
		ServerPlayerEntity player = newPlayer(world, center);

		context.runAtTick(WARMUP, () -> {
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_passive", tickLimit = 800)
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

		context.runAtTick(WARMUP, () -> {
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
		});

		context.runAtTick(WARMUP + 50, () -> {
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
		});

		context.runAtTick(WARMUP + 75, () -> {
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
		});
	}

	// ------------------------------------------------------------------ 10. right-click abilities

	/** One right-click that the test performs. */
	private record Click(Item item, boolean sneak) {
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_use", tickLimit = 3000)
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

		context.runAtEveryTick(() -> {
			tick[0]++;
			if (tick[0] <= WARMUP) {
				return;
			}
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
					ItemStack stack = new ItemStack(click.item());
					hold(p, stack);
					p.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.DIAMOND));
					p.getItemCooldownManager().remove(click.item());
					p.setSneaking(click.sneak());
					p.setHealth(10.0F);
					p.setExperienceLevel(0);
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
			default:
				return null;
		}
	}

	// ------------------------------------------------------------------ 11. abilities that take time

	/** An ability that needs time, how many ticks to wait, and how to see that it worked. */
	private interface SlowCheck {
		String missing(ServerWorld world, BlockPos center);
	}

	private record Slow(Use use, int ticks, SlowCheck check) {
	}

	private static int floorLeft(ServerWorld world, BlockPos center) {
		return count(world, center.add(-12, -1, -12), center.add(12, -1, 12), Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_slow", tickLimit = 3000)
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
						? null : "the portable hole did not close again"));

		int[] index = {0};
		int[] waitUntil = {-1};
		int[] tick = {0};
		context.runAtEveryTick(() -> {
			tick[0]++;
			if (tick[0] <= WARMUP) {
				return;
			}
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

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "combinator_chaos", tickLimit = 2000)
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
		context.runAtEveryTick(() -> {
			tick[0]++;
			if (tick[0] <= WARMUP) {
				return;
			}
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
			removePlayer(player);
			resetStage(world, center);
			forceChunks(world, center, false);
			report.finish(context);
		});
	}
}
