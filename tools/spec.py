"""Single source of truth for the Item Combinator mod.

Everything else (Java registration, recipes, models, lang, tags, textures, docs)
is generated from the tables in this file by gen.py.

There are no tiers. Any item may be an ingredient of any other item.
The number after the name is only the rarity, which sets the name colour in the game:
1 = yellow, 2 = aqua, 3 = purple and glowing (the wildest items).
"""

MOD_ID = "combinator"
MOD_NAME = "Item Combinator"
VERSION = "1.5.0"

ITEMS = []      # custom items, in creative-tab / documentation order
VANILLA = []    # recipes whose result is a vanilla item
SECTIONS = []   # documentation headings, in order
_section = [None]


def section(name):
    if name not in SECTIONS:
        SECTIONS.append(name)
    _section[0] = name


def it(id, name, rar, a, b, kind, tip, traits="", tex=None, count=1, alt=(), where=None, **kw):
    """a and b are the two ingredients. a = None means the item has no fixed recipe (Chaos Orb)."""
    ITEMS.append(dict(id=id, name=name, rar=rar, a=a, b=b, kind=kind, tip=list(tip), traits=traits, cat=_section[0],
                      tex=tex, count=count, alt=list(alt), where=where, **kw))


def van(a, b, result, count=1, note=""):
    VANILLA.append(dict(a=a, b=b, result=result, count=count, note=note))


# ---------------------------------------------------------------------------------------------
# PICKAXES            dmg = attack damage shown in game, aspd = attack speed modifier
# ---------------------------------------------------------------------------------------------
section("Mining tools")
it("iron_hammer", "Iron Hammer", 1, "iron_pickaxe", "iron_pickaxe", "pick",
   ["Mines a 3x3 area.", "Sneak to mine one block."],
   ".area(1, false)", tex=("hammer", "iron", "iron", None),
   dur=750, speed=4.5, dmg=5, aspd=-3.0, level="iron")
it("excavator", "Excavator", 1, "diamond_pickaxe", "diamond_pickaxe", "pick",
   ["Mines a 3x3x3 cube.", "Sneak to mine one block."],
   ".area(1, true)", tex=("hammer", "diamond", "diamond", None),
   dur=4000, speed=5.5, dmg=6, aspd=-3.0, level="diamond")
it("molten_pickaxe", "Molten Pickaxe", 1, "diamond_pickaxe", "blast_furnace", "pick",
   ["Smelts everything it mines."],
   ".smelt()", tex=("pickaxe", "magma", "fire", "flame"),
   dur=2000, speed=8, dmg=5, aspd=-2.8, level="diamond")
it("vein_pickaxe", "Vein Pickaxe", 1, "diamond_pickaxe", "chain", "pick",
   ["Mines a whole ore vein at once.", "Sneak to mine one block."],
   ".vein(48)", tex=("pickaxe", "diamond", "iron", "chain"),
   dur=2000, speed=8, dmg=5, aspd=-2.8, level="diamond")
it("prospector_pickaxe", "Prospector's Pickaxe", 1, "diamond_pickaxe", "rabbit_foot", "pick",
   ["Ores drop twice as much."],
   ".doubleOres()", tex=("pickaxe", "gold", "emerald", "gem"),
   dur=1800, speed=8, dmg=5, aspd=-2.8, level="diamond")
it("torch_pickaxe", "Torchlight Pickaxe", 1, "iron_pickaxe", "lantern", "pick",
   ["Right-click a block to place a torch.", "Needs no torches. Costs 2 durability."],
   ".torch()", tex=("pickaxe", "iron", "fire", "flame"),
   dur=600, speed=6, dmg=4, aspd=-2.8, level="iron")
it("redstone_drill", "Redstone Drill", 1, "diamond_pickaxe", "redstone_block", "pick",
   ["Mines very fast.", "Gives Haste II while held."],
   ".fx(StatusEffects.HASTE, 1)", tex=("drill", "iron", "redstone", None),
   dur=3000, speed=16, dmg=4, aspd=-2.8, level="diamond")
it("paxel", "Diamond Paxel", 1, "diamond_pickaxe", "diamond_axe", "paxel",
   ["Pickaxe, axe, shovel and hoe in one."],
   "", tex=("paxel", "diamond", "diamond", None), alt=[("diamond_pickaxe", "diamond_shovel")],
   dur=3000, speed=8, dmg=7, aspd=-3.0, level="diamond")

it("magnetic_pickaxe", "Magnetic Pickaxe", 2, "diamond_pickaxe", "magnet", "pick",
   ["Drops go straight into your inventory."],
   ".magnetDrops()", tex=("pickaxe", "diamond", "redstone", "magnet"),
   dur=2500, speed=9, dmg=5, aspd=-2.8, level="diamond")
it("magma_excavator", "Magma Excavator", 2, "excavator", "molten_pickaxe", "pick",
   ["Mines a 3x3x3 cube and smelts it.", "Sneak to mine one block."],
   ".area(1, true).smelt()", tex=("hammer", "magma", "fire", "flame"),
   dur=6000, speed=7, dmg=7, aspd=-3.0, level="netherite")
it("motherlode_pickaxe", "Motherlode Pickaxe", 2, "vein_pickaxe", "prospector_pickaxe", "pick",
   ["Mines a whole ore vein at once.", "Ores drop twice as much."],
   ".vein(96).doubleOres()", tex=("pickaxe", "gold", "diamond", "gem"),
   dur=4000, speed=9, dmg=6, aspd=-2.8, level="netherite")
it("tunnel_bore", "Tunnel Bore", 2, "redstone_drill", "iron_hammer", "pick",
   ["Mines a 3x3 tunnel very fast.", "Gives Haste II while held."],
   ".area(1, false).fx(StatusEffects.HASTE, 1)", tex=("drill", "netherite", "redstone", "gem"),
   dur=6000, speed=14, dmg=6, aspd=-3.0, level="netherite")

it("world_eater", "World Eater", 3, "magma_excavator", "motherlode_pickaxe", "paxel",
   ["Mines a 9x9x9 cube of any block.", "Smelts drops and doubles ores.",
    "Drops go into your inventory.", "Sneak to mine one block."],
   ".area(4, true).smelt().magnetDrops().doubleOres()", tex=("hammer", "void", "fire", "star"),
   dur=12000, speed=12, dmg=9, aspd=-3.0, level="netherite")

# ---------------------------------------------------------------------------------------------
# AXES
# ---------------------------------------------------------------------------------------------
section("Axes")
it("lumber_axe", "Lumber Axe", 1, "diamond_axe", "diamond_axe", "axe",
   ["Chops down the whole tree.", "Sneak to chop one log."],
   ".tree(160)", tex=("axe", "diamond", "wood", None),
   dur=3000, speed=8, dmg=9, aspd=-3.0, level="diamond")
it("ember_axe", "Ember Axe", 1, "diamond_axe", "blaze_powder", "axe",
   ["Logs drop as charcoal.", "Sets enemies on fire."],
   ".smelt().ignite(5)", tex=("axe", "magma", "fire", "flame"),
   dur=1800, speed=8, dmg=9, aspd=-3.0, level="diamond")
it("battle_axe", "Battle Axe", 1, "diamond_axe", "diamond_sword", "axe",
   ["Hits also damage nearby enemies."],
   ".sweep(2.5f, 0.5f)", tex=("battleaxe", "diamond", "iron", None),
   dur=2400, speed=8, dmg=12, aspd=-3.1, level="diamond")
it("executioner_axe", "Executioner's Axe", 1, "diamond_axe", "skeleton_skull", "axe",
   ["Slain mobs may drop their head.", "Kills give extra XP."],
   ".heads(0.15f).bonusXp(3)", tex=("axe", "netherite", "bone", "skull"),
   dur=2000, speed=8, dmg=10, aspd=-3.0, level="diamond")

it("deforester", "Deforester", 2, "lumber_axe", "magnet", "axe",
   ["Chops down the whole tree.", "Logs go straight into your inventory."],
   ".tree(512).magnetDrops()", tex=("axe", "emerald", "redstone", "magnet"),
   dur=6000, speed=10, dmg=10, aspd=-3.0, level="netherite")
it("warlord_axe", "Warlord's Axe", 2, "battle_axe", "executioner_axe", "axe",
   ["Hits also damage nearby enemies.", "Slain mobs often drop their head."],
   ".sweep(3.0f, 0.6f).heads(0.35f).bonusXp(6)", tex=("battleaxe", "netherite", "blood", "skull"),
   dur=5000, speed=9, dmg=15, aspd=-3.0, level="netherite")

# ---------------------------------------------------------------------------------------------
# SHOVELS
# ---------------------------------------------------------------------------------------------
section("Shovels")
it("trench_shovel", "Trench Shovel", 1, "diamond_shovel", "diamond_shovel", "shovel",
   ["Digs a 3x3x3 cube.", "Sneak to dig one block."],
   ".area(1, true)", tex=("shovel", "diamond", "diamond", None),
   dur=4000, speed=6, dmg=6, aspd=-3.0, level="diamond")
it("kiln_shovel", "Kiln Shovel", 1, "diamond_shovel", "furnace", "shovel",
   ["Smelts what it digs.", "Sand becomes glass, clay becomes bricks."],
   ".smelt()", tex=("shovel", "magma", "fire", "flame"),
   dur=2000, speed=8, dmg=5.5, aspd=-3.0, level="diamond")

it("terraformer", "Terraformer", 2, "excavator", "trench_shovel", "paxel",
   ["Digs a 3x3x3 cube of any block.", "Sneak to dig one block."],
   ".area(1, true)", tex=("paxel", "emerald", "diamond", "star"),
   dur=8000, speed=8, dmg=7, aspd=-3.0, level="netherite")

# ---------------------------------------------------------------------------------------------
# HOES / FARMING
# ---------------------------------------------------------------------------------------------
section("Farming")
it("harvester_hoe", "Harvester Hoe", 1, "diamond_hoe", "diamond_hoe", "hoe",
   ["Right-click a crop to harvest and", "replant ripe crops in a 5x5 area."],
   ".harvest(2)", tex=("hoe", "diamond", "wheat", None),
   dur=2500, speed=8, dmg=1, aspd=0.0, level="diamond")
it("verdant_staff", "Verdant Staff", 1, "diamond_hoe", "bone_block", "gadget",
   ["Right-click plants to grow them.", "Works on a 3x3 area."],
   ".bonemeal(1)", tex=("staff", "wood", "emerald", "leaf"),
   dur=256)

it("farmers_scythe", "Farmer's Scythe", 2, "harvester_hoe", "verdant_staff", "hoe",
   ["Right-click a crop to harvest and", "replant ripe crops in a 9x9 area.",
    "Crops go into your inventory."],
   ".harvest(4).magnetDrops()", tex=("scythe", "emerald", "wheat", "leaf"),
   dur=5000, speed=10, dmg=6, aspd=-2.0, level="netherite")

# ---------------------------------------------------------------------------------------------
# SWORDS
# ---------------------------------------------------------------------------------------------
section("Swords")
it("blazing_sword", "Blazing Sword", 1, "diamond_sword", "blaze_rod", "sword",
   ["Sets enemies on fire."],
   ".ignite(6)", tex=("sword", "fire", "gold", None),
   dur=1800, dmg=8, aspd=-2.4, level="diamond")
it("frost_blade", "Frost Blade", 1, "diamond_sword", "blue_ice", "sword",
   ["Freezes and slows enemies."],
   ".hit(StatusEffects.SLOWNESS, 100, 2).freeze()", tex=("sword", "ice", "iron", None),
   dur=1800, dmg=8, aspd=-2.4, level="diamond")
it("venom_blade", "Venom Blade", 1, "diamond_sword", "spider_eye", "sword",
   ["Poisons enemies."],
   ".hit(StatusEffects.POISON, 120, 1)", tex=("sword", "venom", "netherite", None),
   dur=1800, dmg=7, aspd=-2.4, level="diamond")
it("withering_blade", "Withering Blade", 1, "diamond_sword", "wither_skeleton_skull", "sword",
   ["Withers enemies."],
   ".hit(StatusEffects.WITHER, 120, 1)", tex=("sword", "wither", "bone", None),
   dur=1800, dmg=8, aspd=-2.4, level="diamond")
it("thunder_blade", "Thunder Blade", 1, "diamond_sword", "lightning_rod", "sword",
   ["Calls lightning on the enemy you hit."],
   ".lightning(5.0f).strikeCooldown(40)", tex=("sword", "thunder", "copper", None),
   dur=1800, dmg=8, aspd=-2.4, level="diamond")
it("vampire_blade", "Vampire Blade", 1, "diamond_sword", "ghast_tear", "sword",
   ["Heals you when you hit enemies."],
   ".lifesteal(0.25f)", tex=("sword", "blood", "netherite", None),
   dur=1800, dmg=7, aspd=-2.4, level="diamond")
it("ender_blade", "Ender Blade", 1, "diamond_sword", "ender_pearl", "sword",
   ["Right-click to teleport 12 blocks ahead."],
   ".use(Use.BLINK).range(12).cooldown(40).cost(3)", tex=("sword", "ender", "void", None),
   dur=1800, dmg=8, aspd=-2.4, level="diamond")
it("greatsword", "Greatsword", 1, "diamond_sword", "diamond_sword", "sword",
   ["Slow but very strong.", "Hits also damage nearby enemies."],
   ".sweep(3.0f, 0.5f)", tex=("greatsword", "diamond", "iron", None),
   dur=3000, dmg=12, aspd=-3.0, level="diamond")
it("gale_saber", "Gale Saber", 1, "diamond_sword", "breeze_rod", "sword",
   ["Very fast.", "Launches enemies into the air."],
   ".knockUp(0.9f)", tex=("saber", "wind", "iron", None),
   dur=1800, dmg=7, aspd=-2.0, level="diamond")
it("midas_sword", "Midas Sword", 1, "golden_sword", "gold_block", "sword",
   ["Slain mobs drop gold nuggets.", "Kills give extra XP."],
   ".nuggets(4).bonusXp(4)", tex=("sword", "gold", "emerald", None),
   dur=1200, dmg=7, aspd=-2.4, level="diamond")
it("creeper_cleaver", "Creeper Cleaver", 1, "diamond_sword", "tnt", "sword",
   ["Every hit is a real explosion.", "It breaks blocks. It does not hurt you."],
   ".boomHit(3.0f).strikeCooldown(15)", tex=("cleaver", "slime", "redstone", None),
   dur=1800, dmg=8, aspd=-2.6, level="diamond")
it("slime_bat", "Slime Bat", 1, "wooden_sword", "slime_block", "sword",
   ["Sends enemies flying."],
   ".knockback(7.0f)", tex=("bat", "slime", "wood", None),
   dur=400, dmg=3, aspd=-2.4, level="wooden")

it("frostfire_blade", "Frostfire Blade", 2, "blazing_sword", "frost_blade", "sword",
   ["Burns, freezes and slows enemies.", "Deals extra magic damage."],
   ".ignite(8).hit(StatusEffects.SLOWNESS, 120, 3).freeze().magic(3.0f)",
   tex=("sword_split", "fire", "ice", None),
   dur=4000, dmg=10, aspd=-2.4, level="netherite")
it("plague_blade", "Plague Blade", 2, "venom_blade", "withering_blade", "sword",
   ["Poisons, withers and weakens enemies."],
   ".hit(StatusEffects.POISON, 160, 2).hit(StatusEffects.WITHER, 160, 2).hit(StatusEffects.WEAKNESS, 160, 1)",
   tex=("sword_split", "venom", "wither", None),
   dur=4000, dmg=9, aspd=-2.4, level="netherite")
it("tempest_blade", "Tempest Blade", 2, "thunder_blade", "gale_saber", "sword",
   ["Calls lightning on the enemy you hit.", "The lightning jumps to nearby enemies.",
    "Launches enemies into the air."],
   ".lightning(8.0f).chain(10.0f, 6.0f).knockUp(0.8f).strikeCooldown(30)", tex=("saber", "thunder", "wind", "bolt"),
   dur=4000, dmg=10, aspd=-2.2, level="netherite")
it("bloodthirst_greatsword", "Bloodthirst Greatsword", 2, "vampire_blade", "greatsword", "sword",
   ["Heals you when you hit enemies.", "Hits also damage nearby enemies."],
   ".lifesteal(0.35f).sweep(3.0f, 0.6f)", tex=("greatsword", "blood", "netherite", None),
   dur=5000, dmg=14, aspd=-3.0, level="netherite")
it("void_blade", "Void Blade", 2, "ender_blade", "echo_shard", "sword",
   ["Right-click to teleport 28 blocks ahead.", "Deals extra magic damage."],
   ".use(Use.BLINK).range(28).cooldown(20).cost(2).magic(4.0f)", tex=("sword", "void", "ender", "star"),
   dur=4000, dmg=10, aspd=-2.4, level="netherite")

it("elemental_blade", "Blade of the Elements", 3, "frostfire_blade", "tempest_blade", "sword",
   ["Burns, freezes and slows enemies.", "Chain lightning. Launches enemies.",
    "Right-click: fires a burning beam."],
   ".ignite(8).hit(StatusEffects.SLOWNESS, 120, 3).freeze().lightning(8.0f).chain(12.0f, 8.0f).knockUp(0.6f).magic(4.0f)"
   ".strikeCooldown(30).use(Use.BEAM).range(40).power(18.0f).cooldown(30)",
   tex=("sword_rainbow", "fire", "ice", "star"),
   dur=9000, dmg=14, aspd=-2.2, level="netherite")
it("soul_reaver", "Soul Reaver", 3, "plague_blade", "bloodthirst_greatsword", "sword",
   ["Poisons, withers and weakens enemies.", "Heals you for half the damage dealt.",
    "Hits also damage nearby enemies."],
   ".hit(StatusEffects.POISON, 200, 2).hit(StatusEffects.WITHER, 200, 2).hit(StatusEffects.WEAKNESS, 200, 1)"
   ".lifesteal(0.5f).sweep(3.5f, 0.7f)",
   tex=("greatsword", "void", "blood", "skull"),
   dur=9000, dmg=17, aspd=-2.8, level="netherite")

# ---------------------------------------------------------------------------------------------
# GADGETS: wands, staffs, charms, utilities
# ---------------------------------------------------------------------------------------------
section("Wands, staffs and charms")
it("magnet", "Magnet", 1, "iron_ingot", "redstone", "gadget",
   ["Pulls nearby items and XP to you.", "Keep it in your hotbar or off hand."],
   ".pull(7)", tex=("magnet", "redstone", "iron", None))
it("fire_wand", "Wand of Fire", 1, "blaze_rod", "fire_charge", "gadget",
   ["Right-click to shoot a fireball."],
   ".use(Use.SMALL_FIREBALL).cooldown(12).cost(1)", tex=("wand", "gold", "fire", None), dur=200)
it("frost_wand", "Wand of Frost", 1, "stick", "blue_ice", "gadget",
   ["Right-click to freeze and slow", "all enemies in front of you."],
   ".use(Use.FROST_CONE).range(7).power(4.0f).cooldown(30).cost(1)", tex=("wand", "wood", "ice", None), dur=200)
it("storm_staff", "Staff of Storms", 1, "lightning_rod", "blaze_rod", "gadget",
   ["Right-click to call lightning", "where you look."],
   ".use(Use.LIGHTNING).range(48).cooldown(40).cost(1)", tex=("staff", "copper", "thunder", None), dur=150)
it("warp_staff", "Warp Staff", 1, "end_rod", "ender_pearl", "gadget",
   ["Right-click to teleport where you look.", "Range: 32 blocks."],
   ".use(Use.BLINK).range(32).cooldown(20).cost(1)", tex=("staff", "bone", "ender", None), dur=250)
it("gust_staff", "Gust Staff", 1, "breeze_rod", "feather", "gadget",
   ["Right-click to leap where you look.", "No fall damage while in your hotbar."],
   ".use(Use.LEAP).power(1.6f).cooldown(25).cost(1).noFall()", tex=("staff", "iron", "wind", None), dur=300)
it("healing_staff", "Staff of Healing", 1, "blaze_rod", "glistering_melon_slice", "gadget",
   ["Right-click to heal yourself.", "15 second cooldown."],
   ".use(Use.HEAL).power(8.0f).cooldown(300).cost(1)", tex=("staff", "gold", "blood", "heart"), dur=64)
it("recall_compass", "Recall Compass", 1, "compass", "ender_pearl", "gadget",
   ["Right-click to teleport to your", "spawn point. 30 second cooldown."],
   ".use(Use.RECALL).cooldown(600).cost(1)", tex=("compass", "iron", "ender", None), dur=32)
it("dynamite", "Dynamite", 1, "gunpowder", "paper", "gadget",
   ["Right-click to throw.", "Explodes like TNT after 2 seconds."],
   ".use(Use.DYNAMITE).power(1.2f).cooldown(15).consume()", tex=("dynamite", "redstone", "bone", None),
   count=2, stack=16)
it("ender_pouch", "Ender Pouch", 1, "ender_chest", "leather", "gadget",
   ["Right-click to open your", "Ender Chest anywhere."],
   ".use(Use.ENDER_POUCH)", tex=("pouch", "void", "ender", None))
it("pocket_workbench", "Pocket Workbench", 1, "crafting_table", "leather", "gadget",
   ["Right-click to open a", "crafting grid anywhere."],
   ".use(Use.WORKBENCH)", tex=("workbench", "wood", "iron", None))
it("mending_charm", "Mending Charm", 1, "anvil", "diamond", "gadget",
   ["Slowly repairs all your items.", "Keep it in your hotbar or off hand."],
   ".mend()", tex=("charm", "iron", "diamond", None))
it("feather_charm", "Feather Charm", 1, "feather", "phantom_membrane", "gadget",
   ["No fall damage.", "Keep it in your hotbar or off hand."],
   ".noFall()", tex=("charm", "bone", "wind", None))
it("obsidian_charm", "Obsidian Charm", 1, "obsidian", "magma_cream", "gadget",
   ["Fire and lava do not hurt you.", "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.FIRE_RESISTANCE, 0)", tex=("charm", "void", "fire", None))
it("tide_charm", "Tide Charm", 1, "nautilus_shell", "prismarine_crystals", "gadget",
   ["Breathe and swim fast underwater.", "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0)", tex=("charm", "prismarine", "ice", None))
it("haste_charm", "Haste Charm", 1, "golden_pickaxe", "redstone_block", "gadget",
   ["Gives Haste II.", "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.HASTE, 1)", tex=("charm", "gold", "redstone", None))
it("swift_charm", "Swift Charm", 1, "sugar", "rabbit_foot", "gadget",
   ["Gives Speed II.", "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.SPEED, 1)", tex=("charm", "bone", "thunder", None))
it("seer_spyglass", "Seer's Spyglass", 1, "spyglass", "ender_eye", "gadget",
   ["Right-click to make nearby mobs", "glow through walls for 15 seconds."],
   ".use(Use.GLOW_SCAN).range(32).cooldown(100).cost(1)", tex=("spyglass", "copper", "ender", None), dur=100)
it("eternal_totem", "Eternal Totem", 1, "totem_of_undying", "nether_star", "gadget",
   ["Saves you from death. Is not used up.", "5 minute cooldown.",
    "Keep it in your hotbar or off hand."],
   ".totem(6000)", tex=("totem", "gold", "emerald", "star"))
it("chrono_clock", "Chrono Clock", 1, "clock", "diamond", "gadget",
   ["Right-click: skip to morning.", "Sneak + right-click: skip to night."],
   ".use(Use.TIME_DAY).sneakUse(Use.TIME_NIGHT, 200).cooldown(200).cost(1)", tex=("clock", "gold", "diamond", None), dur=64)

it("explorer_charm", "Explorer's Charm", 2, "feather_charm", "swift_charm", "gadget",
   ["Speed II, Jump Boost II, no fall damage.", "Keep it in your hotbar or off hand."],
   ".noFall().fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1)", tex=("charm2", "wind", "thunder", None))
it("elemental_charm", "Elemental Charm", 2, "obsidian_charm", "tide_charm", "gadget",
   ["Immune to fire. Breathe underwater.", "Swim fast. Resistance I.",
    "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.WATER_BREATHING, 0)"
   ".fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 0)", tex=("charm2", "fire", "prismarine", None))
it("inferno_staff", "Inferno Staff", 2, "fire_wand", "tnt", "gadget",
   ["Right-click to shoot an", "exploding fireball."],
   ".use(Use.BIG_FIREBALL).power(4.0f).cooldown(30).cost(1)", tex=("staff", "magma", "fire", "flame"), dur=300)
it("tempest_staff", "Tempest Staff", 2, "storm_staff", "gust_staff", "gadget",
   ["Right-click: lightning storm where you look.", "Sneak + right-click: leap.",
    "No fall damage while in your hotbar."],
   ".use(Use.LIGHTNING_STORM).range(64).cooldown(60).cost(1).sneakUse(Use.LEAP, 25).power(1.8f).noFall()",
   tex=("staff", "wind", "thunder", "bolt"), dur=400)
it("rift_staff", "Rift Staff", 2, "warp_staff", "recall_compass", "gadget",
   ["Right-click: teleport up to 64 blocks.", "Sneak + right-click: go to your spawn point."],
   ".use(Use.BLINK).range(64).cooldown(15).cost(1).sneakUse(Use.RECALL, 300)", tex=("staff", "void", "ender", "star"), dur=500)

it("ancient_charm", "Charm of the Ancients", 3, "explorer_charm", "elemental_charm", "gadget",
   ["Speed II, Jump Boost II, Haste II.", "Night Vision. Immune to fire.",
    "Breathe underwater. No fall damage.", "+10 hearts. Hotbar or off hand."],
   ".attr(\"generic.max_health\", 20.0, 0).noFall().fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1).fx(StatusEffects.HASTE, 1)"
   ".fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.FIRE_RESISTANCE, 0)"
   ".fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 0)",
   tex=("charm3", "gold", "diamond", "star"))
it("archmage_staff", "Staff of the Archmage", 3, "inferno_staff", "tempest_staff", "gadget",
   ["Right-click: a huge exploding fireball.", "Sneak + right-click: meteor shower",
    "where you look. No fall damage."],
   ".use(Use.BIG_FIREBALL).power(6.0f).cooldown(20).cost(1).sneakUse(Use.METEOR_SHOWER, 80).range(96).noFall()",
   tex=("staff3", "void", "gold", "star"), dur=1500)

# ---------------------------------------------------------------------------------------------
# WINGS (chest armor that also works as an elytra)
# ---------------------------------------------------------------------------------------------
section("Wings")
it("armored_elytra", "Armored Elytra", 1, "elytra", "diamond_chestplate", "wings",
   ["An elytra with the protection", "of a diamond chestplate."],
   "", tex=("wings", "diamond", "iron", None),
   prot=8, tough=2.0, kb=0.0, dur=900, layer="diamond")
it("rocket_elytra", "Rocket Elytra", 2, "armored_elytra", "firework_rocket", "wings",
   ["Armored elytra with a booster.", "Hold sneak while gliding to speed up."],
   ".boost()", tex=("wings", "redstone", "fire", "flame"),
   prot=8, tough=2.0, kb=0.0, dur=1500, layer="diamond")
it("seraph_wings", "Seraph Wings", 3, "rocket_elytra", "nether_star", "wings",
   ["Lets you fly like in creative mode.", "Hold sneak while gliding to speed up."],
   ".boost().flight()", tex=("wings", "bone", "gold", "star"),
   prot=8, tough=3.0, kb=0.1, dur=4000, layer="netherite")

# ---------------------------------------------------------------------------------------------
# ARMOR
# ---------------------------------------------------------------------------------------------
section("Armor")
it("miner_helmet", "Miner's Helmet", 1, "iron_helmet", "lantern", "armor",
   ["Night Vision while worn."],
   ".fx(StatusEffects.NIGHT_VISION, 0)", tex=("helmet", "iron", "thunder", "lamp"),
   slot="HELMET", prot=2, tough=0.0, kb=0.0, dur=240, layer="iron")
it("diving_helmet", "Diving Helmet", 1, "iron_helmet", "glass", "armor",
   ["Breathe underwater while worn."],
   ".fx(StatusEffects.WATER_BREATHING, 0)", tex=("helmet", "copper", "ice", "visor"),
   slot="HELMET", prot=2, tough=0.0, kb=0.0, dur=240, layer="iron")
it("magma_chestplate", "Magma Chestplate", 1, "diamond_chestplate", "magma_block", "armor",
   ["Immune to fire while worn.", "Sets attackers on fire."],
   ".fx(StatusEffects.FIRE_RESISTANCE, 0).thorns(0.0f, 5)", tex=("chestplate", "magma", "fire", None),
   slot="CHESTPLATE", prot=8, tough=2.0, kb=0.0, dur=600, layer="diamond")
it("cactus_chestplate", "Cactus Chestplate", 1, "iron_chestplate", "cactus", "armor",
   ["Hurts anything that attacks you."],
   ".thorns(3.0f, 0)", tex=("chestplate", "cactus", "bone", None),
   slot="CHESTPLATE", prot=6, tough=0.0, kb=0.0, dur=300, layer="iron")
it("swift_leggings", "Swift Leggings", 1, "diamond_leggings", "sugar", "armor",
   ["Speed II while worn."],
   ".fx(StatusEffects.SPEED, 1)", tex=("leggings", "diamond", "thunder", None),
   slot="LEGGINGS", prot=6, tough=2.0, kb=0.0, dur=560, layer="diamond")
it("spring_boots", "Spring Boots", 1, "iron_boots", "slime_block", "armor",
   ["Jump Boost III while worn.", "No fall damage."],
   ".fx(StatusEffects.JUMP_BOOST, 2).noFall()", tex=("boots", "iron", "slime", None),
   slot="BOOTS", prot=2, tough=0.0, kb=0.0, dur=260, layer="iron")

it("spelunker_helmet", "Spelunker's Helmet", 2, "miner_helmet", "diving_helmet", "armor",
   ["Night Vision, Haste I and", "underwater breathing while worn."],
   ".fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.WATER_BREATHING, 0).fx(StatusEffects.HASTE, 0)",
   tex=("helmet", "diamond", "thunder", "lamp"),
   slot="HELMET", prot=3, tough=2.0, kb=0.0, dur=500, layer="diamond")
it("inferno_thornmail", "Inferno Thornmail", 2, "magma_chestplate", "cactus_chestplate", "armor",
   ["Immune to fire. Resistance I.", "Attackers burn and explode.", "The explosion breaks no blocks."],
   ".fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.RESISTANCE, 0).thorns(6.0f, 8).thornsBoom(2.5f)",
   tex=("chestplate", "netherite", "fire", "flame"),
   slot="CHESTPLATE", prot=9, tough=3.0, kb=0.1, dur=900, layer="netherite")
it("hermes_boots", "Hermes Boots", 2, "spring_boots", "swift_charm", "armor",
   ["Speed III and Jump Boost III.", "No fall damage."],
   ".fx(StatusEffects.SPEED, 2).fx(StatusEffects.JUMP_BOOST, 2).noFall()", tex=("boots", "gold", "wind", "wing"),
   slot="BOOTS", prot=3, tough=2.0, kb=0.0, dur=600, layer="diamond")

# ---------------------------------------------------------------------------------------------
# FOOD        fx = (effect, seconds, amplifier)
# ---------------------------------------------------------------------------------------------
section("Food")
it("golden_steak", "Golden Steak", 1, "cooked_beef", "gold_ingot", "food",
   ["Absorption and Regeneration."],
   tex=("steak", "gold", "magma", None), stack=64,
   nut=10, sat=1.2, fx=[("ABSORPTION", 60, 0), ("REGENERATION", 5, 0)])
it("honey_toast", "Honey Toast", 1, "bread", "honey_bottle", "food",
   ["Speed I for 30 seconds."],
   tex=("toast", "wheat", "gold", None), stack=64,
   nut=8, sat=0.8, fx=[("SPEED", 30, 0)])
it("miners_pie", "Miner's Pie", 1, "pumpkin_pie", "glow_berries", "food",
   ["Night Vision and Haste I for 3 minutes."],
   tex=("pie", "wheat", "thunder", None), stack=64,
   nut=9, sat=0.6, fx=[("NIGHT_VISION", 180, 0), ("HASTE", 180, 0)])
it("blazing_stew", "Blazing Stew", 1, "mushroom_stew", "blaze_powder", "food",
   ["Fire Resistance for 3 minutes.", "Strength I for 45 seconds."],
   tex=("stew", "wood", "fire", None), stack=1, bowl=True,
   nut=8, sat=0.8, fx=[("FIRE_RESISTANCE", 180, 0), ("STRENGTH", 45, 0)])
it("glowing_carrot", "Glowing Carrot", 1, "golden_carrot", "glowstone_dust", "food",
   ["Night Vision for 5 minutes.", "Can be eaten when full."],
   tex=("carrot", "thunder", "venom", None), stack=64, always=True,
   nut=6, sat=1.2, fx=[("NIGHT_VISION", 300, 0)])
it("sugar_cookie", "Sugar Rush Cookie", 1, "cookie", "sugar", "food",
   ["Eaten very fast. Speed II for 20 seconds.", "Can be eaten when full."],
   tex=("cookie", "wheat", "bone", None), stack=64, always=True, snack=True,
   nut=2, sat=0.2, fx=[("SPEED", 20, 1)])
it("hero_sandwich", "Hero Sandwich", 1, "bread", "cooked_porkchop", "food",
   ["Very filling."],
   tex=("sandwich", "wheat", "blood", None), stack=64,
   nut=14, sat=0.9, fx=[])
it("kelp_roll", "Sailor's Kelp Roll", 1, "cooked_cod", "dried_kelp", "food",
   ["Water Breathing for 2 minutes.", "Dolphin's Grace for 1 minute."],
   tex=("roll", "cactus", "bone", None), stack=64,
   nut=9, sat=0.8, fx=[("WATER_BREATHING", 120, 0), ("DOLPHINS_GRACE", 60, 0)])

it("royal_feast", "Royal Feast", 2, "golden_steak", "hero_sandwich", "food",
   ["Fills your whole hunger bar.", "Absorption II, Regeneration II, Resistance I."],
   tex=("feast", "gold", "blood", None), stack=16,
   nut=20, sat=1.0, fx=[("ABSORPTION", 120, 1), ("REGENERATION", 10, 1), ("RESISTANCE", 60, 0)])

it("ambrosia", "Ambrosia", 3, "royal_feast", "enchanted_golden_apple", "food",
   ["Absorption IV, Regeneration II, Strength II.", "Resistance I and Fire Resistance.",
    "Can be eaten when full."],
   tex=("goblet", "gold", "ender", "star"), stack=16, always=True,
   nut=20, sat=1.5, fx=[("ABSORPTION", 120, 3), ("REGENERATION", 30, 1), ("STRENGTH", 120, 1),
                        ("RESISTANCE", 300, 0), ("FIRE_RESISTANCE", 300, 0)])


# =============================================================================================
# THE WILD ONES. Balance is not a goal here.
# attr(id, value, op): op 0 = add, 1 = add value x base, 2 = multiply the total by (1 + value)
# =============================================================================================
section("Mining tools")
it("tnt_pickaxe", "Blast Pickaxe", 2, "diamond_pickaxe", "tnt", "pick",
   ["Every block you mine explodes.", "The blast does not hurt you.", "Sneak to mine one block."],
   ".mineBoom(3.5f)", tex=("pickaxe", "redstone", "bone", "flame"),
   dur=3000, speed=8, dmg=5, aspd=-2.8, level="diamond")
it("mining_laser", "Mining Laser", 2, "redstone_drill", "end_rod", "gadget",
   ["Right-click: burns a 3x3 tunnel", "48 blocks long. Drops go to you."],
   ".use(Use.LASER_DRILL).range(48).cooldown(30).cost(1)", tex=("raygun", "iron", "redstone", None), dur=500)
it("earthshaker", "Earthshaker", 2, "excavator", "tnt", "pick",
   ["Mines a 3x3x3 cube.", "Right-click: earthquake. The ground", "and all nearby mobs fly into the air."],
   ".area(1, true).use(Use.QUAKE).cooldown(60).cost(5)", tex=("hammer", "copper", "magma", "bolt"),
   dur=5000, speed=6, dmg=8, aspd=-3.0, level="netherite")

section("Swords")
it("home_run_bat", "Home Run Bat", 2, "slime_bat", "piston", "sword",
   ["Sends enemies flying over the horizon."],
   ".knockback(30.0f).knockUp(1.6f)", tex=("bat", "wood", "redstone", "star"),
   dur=800, dmg=4, aspd=-2.4, level="wooden")
it("beam_saber", "Beam Saber", 2, "diamond_sword", "end_crystal", "sword",
   ["Sets enemies on fire.", "Right-click: fires a beam that burns", "everything in a line."],
   ".ignite(4).use(Use.BEAM).range(40).power(18.0f).cooldown(25).cost(2)", tex=("saber", "blood", "netherite", None),
   dur=2500, dmg=9, aspd=-2.2, level="diamond")
it("omni_blade", "Blade of Everything", 3, "elemental_blade", "soul_reaver", "sword",
   ["Every sword effect of this mod at once.", "Right-click: fires a beam."],
   ".ignite(10).hit(StatusEffects.SLOWNESS, 200, 3).hit(StatusEffects.POISON, 200, 2).hit(StatusEffects.WITHER, 200, 2)"
   ".hit(StatusEffects.WEAKNESS, 200, 1).freeze().lifesteal(0.5f).sweep(4.0f, 0.8f).lightning(10.0f).chain(12.0f, 10.0f)"
   ".knockUp(0.6f).magic(6.0f).strikeCooldown(20).use(Use.BEAM).range(48).power(30.0f).cooldown(20)",
   tex=("greatsword", "thunder", "void", "star"),
   dur=20000, dmg=25, aspd=-2.0, level="netherite")

section("Bombs and disasters")
it("mega_dynamite", "Mega Dynamite", 2, "dynamite", "tnt", "gadget",
   ["Right-click to throw.", "A huge explosion after 3 seconds.", "It hurts you too."],
   ".use(Use.MEGA_BOMB).power(9.0f).cooldown(20).consume()", tex=("dynamite", "redstone", "thunder", "flame"),
   count=2, stack=16)
it("nuke", "Pocket Nuke", 3, "mega_dynamite", "nether_star", "gadget",
   ["Right-click to throw. Then run.", "Erases everything within 18 blocks.", "It hurts you too."],
   ".use(Use.NUKE).power(18.0f).cooldown(40).consume()", tex=("bomb", "netherite", "venom", "skull"),
   count=2, stack=16)
it("doomsday_device", "Doomsday Device", 3, "nuke", "singularity", "gadget",
   ["Right-click to throw. Then run far.", "Erases everything within 32 blocks."],
   ".use(Use.NUKE).power(32.0f).cooldown(200).consume()", tex=("bomb", "void", "redstone", "skull"),
   stack=16)
it("meteor_staff", "Meteor Staff", 3, "inferno_staff", "magma_block", "gadget",
   ["Right-click: a meteor hits where you look.", "Sneak + right-click: meteor shower."],
   ".use(Use.METEOR).range(96).power(7.0f).cooldown(40).cost(1).sneakUse(Use.METEOR_SHOWER, 100)",
   tex=("staff", "void", "fire", "star"), dur=300)
it("singularity", "Singularity", 3, "echo_shard", "nether_star", "gadget",
   ["Right-click: opens a black hole where you", "look. For 10 seconds it eats blocks and", "mobs. It pulls you in too."],
   ".use(Use.BLACK_HOLE).range(48).cooldown(60).consume()", tex=("orb", "void", "wither", None),
   count=2, stack=16)
it("ocean_orb", "Ocean Orb", 2, "heart_of_the_sea", "sponge", "gadget",
   ["Right-click: floods the spot you look at.", "Sneak + right-click: removes all water", "and lava around you."],
   ".use(Use.FLOOD).range(32).cooldown(20).cost(1).sneakUse(Use.DRAIN, 20)", tex=("orb", "ice", "prismarine", None), dur=64)
it("winter_globe", "Winter Globe", 2, "frost_wand", "packed_ice", "gadget",
   ["Right-click: freezes everything around you.", "Water turns to ice, lava to obsidian.", "Mobs freeze solid."],
   ".use(Use.FREEZE_AREA).range(14).cooldown(60).cost(1)", tex=("orb", "bone", "ice", None), dur=64)
it("anvil_staff", "Anvil Storm Staff", 2, "storm_staff", "anvil", "gadget",
   ["Right-click: rains anvils where you look."],
   ".use(Use.ANVIL_STORM).range(64).cooldown(60).cost(1)", tex=("staff", "iron", "netherite", "anvil"), dur=100)
it("poultry_staff", "Fowl Weather Staff", 2, "storm_staff", "egg", "gadget",
   ["Right-click: rains chickens where you look.", "They are real chickens."],
   ".use(Use.CHICKEN_STORM).range(64).cooldown(40).cost(1)", tex=("staff", "wood", "bone", "egg"), dur=100)
it("wither_wand", "Wither Wand", 2, "wither_skeleton_skull", "blaze_rod", "gadget",
   ["Right-click: shoots exploding wither skulls."],
   ".use(Use.WITHER_SKULL).cooldown(8).cost(1)", tex=("wand", "netherite", "wither", "skull"), dur=200)
it("arrow_gatling", "Arrow Gatling", 2, "dispenser", "crossbow", "gadget",
   ["Right-click: fires 10 arrows at once.", "Needs no arrows."],
   ".use(Use.ARROW_BURST).power(10.0f).cooldown(4).cost(1)", tex=("raygun", "wood", "iron", None), dur=1000)

section("Transformation")
it("midas_glove", "Midas Glove", 3, "leather", "gold_block", "gadget",
   ["Right-click a block: it and its neighbours", "turn into gold blocks.", "Hit a mob: it turns into a gold block."],
   ".midas()", tex=("glove", "gold", "thunder", None), where="HELD")
it("philosopher_stone", "Philosopher's Stone", 3, "emerald_block", "redstone_block", "gadget",
   ["Right-click a block to upgrade it:", "stone, coal, iron, gold, diamond,", "emerald, netherite. Sneak to go back."],
   ".transmute()", tex=("orb", "blood", "gold", None), dur=256, where="HELD")
it("polymorph_wand", "Polymorph Wand", 2, "blaze_rod", "egg", "gadget",
   ["Right-click a mob: it turns into", "a random farm animal."],
   ".use(Use.POLYMORPH).range(24).cooldown(20).cost(1)", tex=("wand", "gold", "venom", None), dur=64)
it("size_ray", "Size Ray", 2, "spyglass", "amethyst_shard", "gadget",
   ["Right-click a mob: it shrinks and gets weak.", "Sneak + right-click: it grows huge."],
   ".use(Use.SHRINK).range(32).cooldown(10).cost(1).sneakUse(Use.GROW, 10)", tex=("raygun", "prismarine", "thunder", None), dur=128)
it("eat_me_cake", "Eat-Me Cake", 2, "cake", "bone_meal", "food",
   ["You grow 4 times as big for 2 minutes.", "More health, damage and reach.", "Can be eaten when full."],
   ".eatBuff(120).attr(\"generic.scale\", 3.0, 2).attr(\"generic.max_health\", 40.0, 0)"
   ".attr(\"generic.attack_damage\", 10.0, 0).attr(\"generic.step_height\", 2.0, 0)"
   ".attr(\"player.block_interaction_range\", 8.0, 0).attr(\"player.entity_interaction_range\", 8.0, 0)",
   tex=("cake", "bone", "blood", None), stack=16, always=True,
   nut=6, sat=0.6, fx=[])
it("shrinking_mushroom", "Shrinking Mushroom", 2, "red_mushroom", "brown_mushroom", "food",
   ["You shrink to a fifth of your size", "for 2 minutes. Fits through tiny gaps.", "Can be eaten when full."],
   ".eatBuff(120).attr(\"generic.scale\", -0.8, 2).attr(\"generic.safe_fall_distance\", 20.0, 0)"
   ".attr(\"generic.movement_speed\", 0.5, 2)",
   tex=("mushroom", "redstone", "bone", None), stack=64, always=True, snack=True,
   nut=2, sat=0.2, fx=[])

section("Mobs, time and space")
it("time_stopper", "Time Stopper", 3, "chrono_clock", "ender_eye", "gadget",
   ["Right-click: stops time for 10 seconds.", "Everything freezes except you."],
   ".use(Use.TIME_STOP).power(10.0f).cooldown(400).cost(1)", tex=("clock", "void", "ender", None), dur=32)
it("madness_bell", "Bell of Madness", 2, "bell", "fermented_spider_eye", "gadget",
   ["Right-click: nearby monsters", "attack each other."],
   ".use(Use.MADNESS).range(24).cooldown(100).cost(1)", tex=("bell", "gold", "blood", None), dur=64)
it("golem_horn", "Golem Horn", 2, "goat_horn", "iron_block", "gadget",
   ["Right-click: calls 3 iron golems", "that fight for you."],
   ".use(Use.SUMMON_GOLEMS).power(3.0f).cooldown(600).cost(1)", tex=("horn", "bone", "iron", None), dur=16)
it("gravity_staff", "Gravity Staff", 2, "gust_staff", "shulker_shell", "gadget",
   ["Right-click: nearby mobs fly into the sky.", "Sneak + right-click: slams them down."],
   ".use(Use.LIFT).range(16).cooldown(60).cost(1).sneakUse(Use.SLAM, 40)", tex=("staff", "iron", "void", None), dur=200)
it("swap_staff", "Swap Staff", 2, "warp_staff", "chorus_fruit", "gadget",
   ["Right-click a mob: you swap places."],
   ".use(Use.SWAP).range(48).cooldown(10).cost(1)", tex=("staff", "ender", "blood", None), dur=200)
it("ghost_cloak", "Ghost Cloak", 3, "phantom_membrane", "ender_pearl", "gadget",
   ["Right-click: you are a ghost for 8 seconds.", "Fly through walls. Do not end inside one."],
   ".use(Use.GHOST).power(8.0f).cooldown(400).cost(1)", tex=("cloak", "wind", "bone", None), dur=32)
it("portable_hole", "Portable Hole", 2, "black_wool", "ender_pearl", "gadget",
   ["Right-click a wall: opens a tunnel", "for 8 seconds. Then the wall comes back."],
   ".use(Use.HOLE).range(12).cooldown(40).cost(1)", tex=("hole", "wither", "void", None), dur=64)
it("bridge_staff", "Bridge Staff", 1, "stick", "scaffolding", "gadget",
   ["Right-click: builds a wooden bridge", "32 blocks ahead of you."],
   ".use(Use.BRIDGE).range(32).cooldown(20).cost(1)", tex=("staff", "wood", "wheat", None), dur=64)
it("house_box", "House in a Box", 2, "crafting_table", "oak_door", "gadget",
   ["Right-click the ground: builds a small", "furnished house in front of you."],
   ".house()", tex=("box", "wood", "redstone", None), stack=16, where="HELD")
it("forest_staff", "Staff of the Wild", 2, "verdant_staff", "oak_sapling", "gadget",
   ["Right-click: a forest grows at once", "where you look."],
   ".use(Use.FOREST).range(32).cooldown(40).cost(1)", tex=("staff", "cactus", "wheat", "leaf"), dur=64)
it("ore_dowser", "Diamond Dowser", 2, "compass", "amethyst_shard", "gadget",
   ["Right-click: tells you where the nearest", "diamond ore or ancient debris is."],
   ".use(Use.DOWSE).range(32).cooldown(40).cost(1)", tex=("compass", "gold", "diamond", None), dur=64)

section("Body upgrades")
it("titan_belt", "Titan's Belt", 3, "leather", "iron_block", "gadget",
   ["You are 2.5 times as big.", "+20 hearts, +8 damage, longer reach.", "Keep it in your hotbar or off hand."],
   ".attr(\"generic.scale\", 1.5, 2).attr(\"generic.max_health\", 40.0, 0).attr(\"generic.attack_damage\", 8.0, 0)"
   ".attr(\"generic.step_height\", 1.5, 0).attr(\"player.block_interaction_range\", 4.0, 0)"
   ".attr(\"player.entity_interaction_range\", 4.0, 0)",
   tex=("belt", "leather", "gold", None))
it("long_arm_glove", "Long Arm Glove", 2, "sticky_piston", "leather", "gadget",
   ["Reach blocks and mobs 32 blocks away.", "Keep it in your hotbar or off hand."],
   ".attr(\"player.block_interaction_range\", 28.0, 0).attr(\"player.entity_interaction_range\", 29.0, 0)",
   tex=("glove", "leather", "iron", None))
it("colossus_heart", "Heart of the Colossus", 3, "enchanted_golden_apple", "totem_of_undying", "gadget",
   ["+40 hearts and Regeneration II.", "Keep it in your hotbar or off hand."],
   ".attr(\"generic.max_health\", 80.0, 0).fx(StatusEffects.REGENERATION, 1)", tex=("heart", "blood", "gold", None))
it("miner_gauntlet", "Miner's Gauntlet", 3, "haste_charm", "diamond_block", "gadget",
   ["Mine 10 times faster with anything,", "even your fist.", "Keep it in your hotbar or off hand."],
   ".attr(\"player.block_break_speed\", 9.0, 2).fx(StatusEffects.HASTE, 1)", tex=("glove", "iron", "redstone", None))
it("fist_of_doom", "Fist of Doom", 3, "netherite_ingot", "nether_star", "gadget",
   ["Whatever you hit dies.", "Hold it in your main hand."],
   ".instakill().knockback(8.0f)", tex=("glove", "netherite", "void", "skull"), where="HELD")
it("godhood_charm", "Charm of Godhood", 3, "ancient_charm", "colossus_heart", "gadget",
   ["All of the Charm of the Ancients.", "Creative flight. +40 hearts. Long reach.",
    "Mine 5 times faster. Hotbar or off hand."],
   ".flight().noFall().attr(\"generic.max_health\", 80.0, 0).attr(\"player.block_interaction_range\", 6.0, 0)"
   ".attr(\"player.entity_interaction_range\", 6.0, 0).attr(\"player.block_break_speed\", 4.0, 2)"
   ".fx(StatusEffects.SPEED, 1).fx(StatusEffects.JUMP_BOOST, 1).fx(StatusEffects.HASTE, 1)"
   ".fx(StatusEffects.NIGHT_VISION, 0).fx(StatusEffects.FIRE_RESISTANCE, 0).fx(StatusEffects.WATER_BREATHING, 0)"
   ".fx(StatusEffects.DOLPHINS_GRACE, 0).fx(StatusEffects.RESISTANCE, 1).fx(StatusEffects.REGENERATION, 1)",
   tex=("charm3", "gold", "diamond", "heart"))

section("Armor")
it("moon_boots", "Moon Boots", 2, "spring_boots", "end_stone", "armor",
   ["Low gravity and huge jumps.", "No fall damage."],
   ".fx(StatusEffects.JUMP_BOOST, 3).noFall().attr(\"generic.gravity\", -0.85, 2)", tex=("boots", "bone", "void", None),
   slot="BOOTS", prot=3, tough=2.0, kb=0.0, dur=600, layer="diamond")

section("Food")
it("rocket_chili", "Rocket Chili", 2, "firework_rocket", "beetroot", "food",
   ["Launches you high into the sky.", "You float back down.", "Can be eaten when full."],
   ".eatLaunch(4.0f)", tex=("chili", "redstone", "cactus", "flame"), stack=64, always=True,
   nut=4, sat=0.4, fx=[("SLOW_FALLING", 30, 0), ("FIRE_RESISTANCE", 30, 0)])

section("Cheats")
it("xp_tome", "Tome of Infinite Wisdom", 3, "book", "experience_bottle", "gadget",
   ["Right-click: gives you 5 levels.", "It never runs out."],
   ".use(Use.XP).power(5.0f).cooldown(10)", tex=("book", "emerald", "gold", "star"))
it("dupe_mirror", "Mirror of Duplication", 3, "glass_pane", "ender_eye", "gadget",
   ["Hold it in one hand, any item in the other.", "Right-click: copies the whole stack."],
   ".use(Use.DUPE).cooldown(40)", tex=("mirror", "iron", "ice", None))

section("Chaos")
it("chaos_orb", "Chaos Orb", 2, None, None, "gadget",
   ["Comes out when you combine two stackable", "items that have no recipe.", "Right-click: something random happens."],
   ".use(Use.CHAOS).cooldown(10).consume()", tex=("orb", "ender", "blood", "question"), stack=64)
it("pandora_box", "Pandora's Box", 3, "chaos_orb", "chaos_orb", "gadget",
   ["Right-click: six random things happen,", "one after another."],
   ".use(Use.PANDORA).cooldown(140).consume()", tex=("box", "void", "gold", "skull"), stack=16)

# ---------------------------------------------------------------------------------------------
# EVERYDAY ITEMS: made from wood, stone, gold, iron and other things of a normal playthrough
# ---------------------------------------------------------------------------------------------
section("Everyday tools")
it("flint_pickaxe", "Flint-Tipped Pickaxe", 1, "wooden_pickaxe", "flint", "pick",
   ["Faster than an iron pickaxe.", "Mines only what stone can mine."],
   "", tex=("pickaxe", "flint", "wood", None),
   dur=180, speed=7, dmg=3, aspd=-2.8, level="stone")
it("stone_hammer", "Stone Hammer", 1, "stone_pickaxe", "stone_pickaxe", "pick",
   ["Mines a 3x3 area.", "Sneak to mine one block."],
   ".area(1, false)", tex=("hammer", "stone", "stone", None),
   dur=400, speed=3.5, dmg=4, aspd=-3.0, level="stone")
it("gentle_pickaxe", "Gentle Pickaxe", 1, "iron_pickaxe", "white_wool", "pick",
   ["Blocks drop as themselves,", "like with Silk Touch."],
   ".silk()", tex=("pickaxe", "iron", "bone", "wool"),
   dur=350, speed=6, dmg=4, aspd=-2.8, level="iron")
it("gilded_pickaxe", "Gilded Pickaxe", 1, "golden_pickaxe", "iron_pickaxe", "pick",
   ["As fast as gold, as tough as iron.", "Stone sometimes drops gold nuggets."],
   ".stoneNuggets(0.12f)", tex=("pickaxe", "gold", "iron", "gem"),
   dur=320, speed=12, dmg=4, aspd=-2.8, level="iron")
it("iron_multitool", "Iron Multitool", 1, "iron_pickaxe", "iron_axe", "paxel",
   ["Pickaxe, axe, shovel and hoe in one."],
   "", tex=("paxel", "iron", "iron", None), alt=[("iron_pickaxe", "iron_shovel")],
   dur=600, speed=6, dmg=6, aspd=-3.0, level="iron")
it("timber_axe", "Timber Axe", 1, "stone_axe", "stone_axe", "axe",
   ["Chops down small trees whole.", "Sneak to chop one log."],
   ".tree(40)", tex=("axe", "stone", "wood", None),
   dur=300, speed=4, dmg=9, aspd=-3.2, level="stone")
it("sifting_shovel", "Sifting Shovel", 1, "stone_shovel", "string", "shovel",
   ["Dirt, sand and gravel sometimes hide", "flint, nuggets, bones or even a gem."],
   ".sift()", tex=("shovel", "stone", "wheat", "gem"),
   dur=260, speed=4, dmg=3.5, aspd=-3.0, level="stone")
it("golden_sickle", "Golden Sickle", 1, "golden_hoe", "golden_hoe", "hoe",
   ["Right-click a ripe crop: harvests", "and replants a 3x3 area."],
   ".harvest(1)", tex=("sickle", "gold", "wood", None),
   dur=150, speed=12, dmg=1, aspd=0.0, level="gold")
it("planter_hoe", "Planter's Hoe", 1, "wooden_hoe", "wheat_seeds", "hoe",
   ["Right-click soil: tills a 3x3 patch", "and plants the seeds you carry."],
   ".sow()", tex=("hoe", "wood", "wheat", "seed"),
   dur=120, speed=2, dmg=1, aspd=-1.0, level="wooden")

section("Everyday weapons")
it("quarterstaff", "Quarterstaff", 1, "wooden_sword", "stick", "sword",
   ["Reaches 2 blocks further.", "Pushes enemies back."],
   ".attr(\"player.entity_interaction_range\", 2.0, 0).knockback(1.5f)", tex=("pole", "wood", "iron", None),
   dur=150, dmg=5, aspd=-2.2, level="wooden")
it("jagged_blade", "Jagged Blade", 1, "stone_sword", "flint", "sword",
   ["Wounds keep bleeding for 6 seconds."],
   ".bleed(6)", tex=("jagged", "flint", "stone", None),
   dur=220, dmg=6, aspd=-2.4, level="stone")
it("dual_blades", "Dual Blades", 1, "iron_sword", "iron_sword", "sword",
   ["Strikes twice as fast as a sword.", "The second blade adds 1 damage."],
   ".magic(1.0f)", tex=("twin", "iron", "leather", None),
   dur=500, dmg=4, aspd=-0.8, level="iron")
it("bulwark_blade", "Bulwark Blade", 1, "iron_sword", "shield", "sword",
   ["While held: +6 armor and you are", "hard to push around."],
   ".attr(\"generic.armor\", 6.0, 0).attr(\"generic.knockback_resistance\", 0.6, 0)", tex=("sword", "iron", "wood", "shield"),
   dur=600, dmg=7, aspd=-2.6, level="iron")
it("looter_blade", "Looter's Blade", 1, "golden_sword", "emerald", "sword",
   ["Slain mobs drop their loot twice."],
   ".doubleLoot()", tex=("sword", "gold", "emerald", "gem"),
   dur=250, dmg=6, aspd=-2.4, level="gold")
it("boomerang", "Boomerang", 1, "wooden_axe", "feather", "gadget",
   ["Right-click: hits every mob in a line", "and brings back dropped items."],
   ".use(Use.BOOMERANG).range(18).power(5.0f).cooldown(16).cost(1)", tex=("boomerang", "wood", "bone", None),
   dur=200, where="HELD")

section("Bows and rods")
it("volley_bow", "Volley Bow", 1, "bow", "bow", "gadget",
   ["Right-click: shoots 5 arrows at once.", "Needs no arrows."],
   ".use(Use.ARROW_BURST).power(5.0f).cooldown(24).cost(1)", tex=("bow", "wood", "iron", "arrows"),
   dur=300, where="HELD")
it("torch_bow", "Torch Bow", 1, "bow", "torch", "gadget",
   ["Right-click: puts a torch on the block", "you look at, up to 40 blocks away."],
   ".use(Use.TORCH_SHOT).range(40).cooldown(6).cost(1)", tex=("bow", "wood", "fire", "flame"),
   dur=256, where="HELD")
it("grappling_hook", "Grappling Hook", 1, "fishing_rod", "iron_ingot", "gadget",
   ["Right-click a block: pulls you to it.", "No fall damage while you hold it."],
   ".use(Use.GRAPPLE).range(40).cooldown(20).cost(1).noFall()", tex=("hook", "iron", "wood", None),
   dur=250, where="HELD")
it("lasso", "Lasso", 1, "fishing_rod", "lead", "gadget",
   ["Right-click a mob: pulls it to you."],
   ".use(Use.YANK).range(24).cooldown(15).cost(1)", tex=("lasso", "leather", "wheat", None),
   dur=200, where="HELD")
it("angler_rod", "Angler's Rod", 1, "fishing_rod", "fishing_rod", "gadget",
   ["Right-click water: a catch at once.", "No waiting for a bite."],
   ".use(Use.FISH).range(12).cooldown(100).cost(1)", tex=("rod", "wood", "gold", None),
   dur=64, where="HELD")

section("Camp and travel")
it("backpack", "Backpack", 1, "chest", "leather", "gadget",
   ["Right-click: opens 27 extra slots.", "The things stay inside the backpack."],
   ".use(Use.BACKPACK).power(3.0f)", tex=("backpack", "leather", "wheat", None), where="HELD")
it("big_backpack", "Big Backpack", 2, "backpack", "backpack", "gadget",
   ["Right-click: opens 54 extra slots.", "Keeps what was in the two small ones."],
   ".use(Use.BACKPACK).power(6.0f)", tex=("backpack", "blood", "gold", "star"), where="HELD")
it("pocket_furnace", "Pocket Furnace", 1, "furnace", "flint_and_steel", "gadget",
   ["Hold an item in the other hand.", "Right-click: smelts up to 8 of it."],
   ".use(Use.SMELT_HAND).cooldown(20).cost(1)", tex=("furnace", "stone", "fire", None), dur=128)
it("bedroll", "Bedroll", 1, "white_bed", "leather", "gadget",
   ["Right-click: you respawn here from", "now on. Works anywhere, at any time."],
   ".use(Use.SET_SPAWN).cooldown(40)", tex=("bedroll", "blood", "leather", None), alt=[("red_bed", "leather")])
it("waystone", "Waystone", 1, "compass", "cobblestone", "gadget",
   ["Sneak + right-click: remembers this spot.", "Right-click: brings you back to it."],
   ".use(Use.WAYPOINT_GO).cooldown(200).sneakUse(Use.WAYPOINT_SET, 20)", tex=("waystone", "stone", "ender", None))
it("rope_ladder", "Rope Ladder", 1, "ladder", "string", "gadget",
   ["Right-click a wall or a ledge: ladders", "unroll down to the ground."],
   ".ropeLadder()", tex=("ladder", "wood", "wheat", None), dur=32, where="HELD")
it("hedge_shears", "Hedge Shears", 1, "shears", "shears", "gadget",
   ["Right-click: shears every sheep nearby", "and cuts leaves and grass loose."],
   ".use(Use.SHEAR_AREA).range(8).cooldown(20).cost(1)", tex=("bigshears", "iron", "redstone", None),
   dur=300, where="HELD")
it("smoke_bomb", "Smoke Bomb", 1, "gunpowder", "white_wool", "gadget",
   ["Right-click: you vanish in smoke.", "Mobs nearby lose track of you."],
   ".use(Use.SMOKE).cooldown(40).consume()", tex=("bomb", "wither", "bone", None), stack=16, count=2)
it("endless_bucket", "Endless Water Bucket", 1, "water_bucket", "bucket", "gadget",
   ["Right-click a block: places water.", "It never runs dry."],
   ".endlessWater()", tex=("bucket", "iron", "ice", None), where="HELD")

section("Animals and helpers")
it("wolf_whistle", "Wolf Whistle", 1, "bone", "leather", "gadget",
   ["Right-click: a tame wolf appears.", "It is yours and follows you."],
   ".use(Use.SUMMON_WOLVES).power(1.0f).cooldown(1200).cost(1)", tex=("whistle", "bone", "leather", None), dur=3)
it("steed_horn", "Steed Horn", 1, "hay_block", "saddle", "gadget",
   ["Right-click: a tame, saddled, fast horse", "appears. The horn is used up."],
   ".use(Use.SUMMON_HORSE).cooldown(100).consume()", tex=("horn", "leather", "gold", None))
it("trader_token", "Trader's Token", 1, "emerald", "emerald", "gadget",
   ["Right-click: a wandering trader", "comes to you."],
   ".use(Use.SUMMON_TRADER).cooldown(100).consume()", tex=("coin", "emerald", "gold", None), stack=16)
it("shepherd_crook", "Shepherd's Crook", 1, "stick", "wheat", "gadget",
   ["Right-click: farm animals nearby", "follow you for 20 seconds."],
   ".use(Use.LURE).range(20).cooldown(100).cost(1)", tex=("crook", "wood", "wheat", None),
   dur=100, where="HELD")
it("mob_net", "Mob Net", 2, "stick", "string", "gadget",
   ["Right-click a mob: it goes into the net.", "Right-click a block: it comes out again."],
   ".capture()", tex=("net", "wood", "bone", None), dur=64, where="HELD")

section("Finds and luck")
it("divining_rod", "Divining Rod", 1, "stick", "gold_ingot", "gadget",
   ["Right-click: ores within 8 blocks glow", "through the walls for 10 seconds."],
   ".use(Use.ORE_SIGHT).range(8).cooldown(240).cost(1)", tex=("fork", "wood", "gold", None),
   dur=32, where="HELD")
it("tome_of_chance", "Tome of Chance", 2, "book", "lapis_lazuli", "gadget",
   ["Right-click: costs 3 levels and turns", "into a random enchanted book."],
   ".use(Use.ENCHANT_BOOK).cooldown(10).consume()", tex=("book", "lapis", "gold", "question"), stack=16)
it("hiking_staff", "Hiking Staff", 1, "stick", "stick", "gadget",
   ["While held: you step up full blocks", "without jumping. Short falls do not hurt."],
   ".attr(\"generic.step_height\", 0.5, 0).attr(\"generic.safe_fall_distance\", 3.0, 0)", tex=("cane", "wood", "gold", None), where="HELD")
it("dreamcatcher", "Dreamcatcher", 1, "feather", "string", "gadget",
   ["Phantoms never come for you.", "Keep it in your hotbar or off hand."],
   ".noPhantoms()", tex=("dreamcatcher", "wood", "bone", None))
it("lucky_horseshoe", "Lucky Horseshoe", 1, "iron_ingot", "iron_ingot", "gadget",
   ["Gives Luck: better fishing and loot.", "Keep it in your hotbar or off hand."],
   ".fx(StatusEffects.LUCK, 0)", tex=("horseshoe", "iron", "gold", None))
it("emergency_chicken", "Emergency Chicken", 1, "egg", "feather", "gadget",
   ["Hold it and you float down slowly.", "No fall damage."],
   ".fx(StatusEffects.SLOW_FALLING, 0).noFall()", tex=("chicken", "bone", "wind", None), where="HELD")

section("Armor")
it("cloud_slippers", "Cloud Slippers", 1, "leather_boots", "white_wool", "armor",
   ["No fall damage."],
   ".noFall()", tex=("boots", "wind", "bone", None),
   slot="BOOTS", prot=1, tough=0.0, kb=0.0, dur=120, layer="leather")
it("merchant_crown", "Merchant's Crown", 1, "golden_helmet", "emerald", "armor",
   ["Villagers give you better prices."],
   ".fx(StatusEffects.HERO_OF_THE_VILLAGE, 0)", tex=("crown", "gold", "emerald", None),
   slot="HELMET", prot=2, tough=0.0, kb=0.0, dur=150, layer="gold")

section("Food")
it("cured_jerky", "Cured Jerky", 1, "rotten_flesh", "sugar", "food",
   ["Rotten flesh, made safe to eat.", "Quick to eat."],
   "", tex=("steak", "leather", "meat", None), stack=64, snack=True, count=2,
   nut=5, sat=0.6, fx=[])
it("fish_and_chips", "Fish and Chips", 1, "cooked_cod", "baked_potato", "food",
   ["Very filling.", "Gives Luck for 2 minutes."],
   "", tex=("fishchips", "copper", "bone", None), alt=[("cooked_salmon", "baked_potato")],
   nut=12, sat=0.8, fx=[("LUCK", 120, 0)])
it("candy_apple", "Candy Apple", 1, "apple", "sugar", "food",
   ["Speed II for 30 seconds.", "Can be eaten when full."],
   "", tex=("candyapple", "redstone", "bone", None), always=True,
   nut=4, sat=0.4, fx=[("SPEED", 30, 1)])

# ---------------------------------------------------------------------------------------------
# ODD ITEMS: surprising pairs that give an ability nobody expects from them
# ---------------------------------------------------------------------------------------------
section("Movement tricks")
it("dust_devil", "Bottled Dust Devil", 1, "sand", "glass", "gadget",
   ["Jump a second time in the air.", "Keep it in your hotbar or off hand."],
   ".airJumps(1)", tex=("jar", "glass", "sand", None))
it("pogo_stick", "Pogo Stick", 1, "stick", "slime_ball", "gadget",
   ["Hold it: every landing bounces you", "higher. Sneak to stop. No fall damage."],
   ".bounce().noFall()", tex=("pogo", "redstone", "slime", None), where="HELD")
it("sticky_boots", "Sticky Boots", 1, "honeycomb", "leather_boots", "armor",
   ["Walk against a wall to climb it.", "Sneak to hold on."],
   ".wallClimb()", tex=("boots", "leather", "honey", None),
   slot="BOOTS", prot=1, tough=0.0, kb=0.0, dur=120, layer="leather")
it("puffer_balloon", "Puffer Balloon", 1, "lead", "pufferfish", "gadget",
   ["Hold it: you float up.", "Sneak to sink slowly."],
   ".balloon()", tex=("balloon", "thunder", "bone", None), where="HELD")

section("Odd charms")
it("firefly_jar", "Firefly Jar", 1, "glass_bottle", "glowstone_dust", "gadget",
   ["Lights up the place around you.", "Keep it in your hotbar or off hand."],
   ".lantern()", tex=("jar", "glass", "thunder", None))
it("pocket_mirror", "Pocket Mirror", 1, "glass_pane", "iron_ingot", "gadget",
   ["A shot that hits you hits the shooter", "instead. Works once every 3 seconds."],
   ".reflect(60)", tex=("mirror", "gold", "wind", "arrows"))
it("lunchbox", "Lunchbox", 1, "barrel", "bread", "gadget",
   ["Eats plain food from your inventory for", "you when you are hungry. Keep in hotbar."],
   ".autoEat()", tex=("lunchbox", "redstone", "bone", None))
it("almanac", "Explorer's Almanac", 1, "compass", "clock", "gadget",
   ["Hold it: shows your position, the biome,", "the time, the light and slime chunks."],
   ".almanac()", tex=("book", "leather", "gold", "leaf"), where="HELD")
it("meadow_boots", "Meadow Boots", 1, "leather_boots", "bone_meal", "armor",
   ["Flowers grow where you walk on grass.", "Crops near you grow faster."],
   ".meadow()", tex=("boots", "emerald", "wheat", "leaf"),
   slot="BOOTS", prot=1, tough=0.0, kb=0.0, dur=120, layer="leather")
it("strider_boots", "Strider Boots", 1, "magma_cream", "leather_boots", "armor",
   ["Lava hardens under your feet.", "It melts again behind you."],
   ".lavaWalk()", tex=("boots", "magma", "fire", "flame"),
   slot="BOOTS", prot=2, tough=0.0, kb=0.0, dur=160, layer="leather")

section("Odd gadgets")
it("rewind_watch", "Rewind Watch", 2, "clock", "ender_pearl", "gadget",
   ["Right-click: back to where you were", "5 seconds ago, with the health of then.", "It has to be in your hotbar before."],
   ".use(Use.REWIND).cooldown(600).cost(1)", tex=("clock", "copper", "ender", None), dur=32)
it("skeleton_key", "Skeleton Key", 1, "tripwire_hook", "bone", "gadget",
   ["Right-click an iron door or an iron", "trapdoor: it opens or closes."],
   ".unlock()", tex=("key", "bone", "iron", None), dur=64, where="HELD")
it("disco_ball", "Disco Ball", 2, "glowstone", "note_block", "gadget",
   ["Right-click: every mob within 10 blocks", "stops and dances for 8 seconds."],
   ".use(Use.DISCO).range(10).cooldown(600).cost(1)", tex=("disco", "iron", "ice", None), dur=16)
it("piggy_bank", "Piggy Bank", 1, "clay_ball", "pink_dye", "gadget",
   ["Right-click: puts your experience in.", "Sneak + right-click: takes it out."],
   ".use(Use.BANK_IN).cooldown(10).sneakUse(Use.BANK_OUT, 10)", tex=("piggy", "pink", "blood", None))
it("push_glove", "Push Glove", 1, "piston", "leather", "gadget",
   ["Right-click a block: pushes it away.", "Sneak + right-click: pulls it to you."],
   ".push()", tex=("glove", "stone", "wood", None), dur=128, where="HELD")
it("weather_vane", "Weather Vane", 2, "lightning_rod", "feather", "gadget",
   ["Right-click: changes the weather.", "Clear, then rain, then thunder."],
   ".use(Use.WEATHER).cooldown(200).cost(1)", tex=("vane", "copper", "bone", None), dur=16)
it("magnifying_glass", "Magnifying Glass", 1, "glass_pane", "stick", "gadget",
   ["Right-click in sunlight: sets the mob", "or block you look at on fire."],
   ".use(Use.SUNBURN).range(6).cooldown(30)", tex=("magnifier", "gold", "glass", None))
it("rodeo_saddle", "Rodeo Saddle", 1, "saddle", "lead", "gadget",
   ["Right-click any mob to ride it.", "You do not steer. Sneak to get off."],
   ".mount()", tex=("saddle", "leather", "wheat", "star"), dur=32, where="HELD")

section("Food")
it("stone_soup", "Stone Soup", 1, "cobblestone", "bowl", "food",
   ["Very filling. You feel like a rock:", "tough, but slow."],
   "", tex=("soup", "wood", "wheat", None), stack=16, bowl=True,
   nut=10, sat=0.6, fx=[("RESISTANCE", 60, 0), ("SLOWNESS", 30, 0)])
it("cactus_juice", "Cactus Juice", 1, "cactus", "glass_bottle", "food",
   ["Speed II and Jump Boost II for", "30 seconds. Makes you dizzy."],
   "", tex=("bottle", "glass", "cactus", None), stack=16, always=True, bottle=True,
   nut=2, sat=0.3, fx=[("SPEED", 30, 1), ("JUMP_BOOST", 30, 1), ("NAUSEA", 8, 0)])

# ---------------------------------------------------------------------------------------------
# MAYHEM (version 1.4): creation, destruction and chaos. No balance at all.
# ---------------------------------------------------------------------------------------------
section("Mass destruction")
it("armageddon_clock", "Armageddon Clock", 3, "time_stopper", "doomsday_device", "gadget",
   ["Right-click: the end of the world starts", "where you stand. 10 second countdown, then", "earthquake, fire rain, lightning, lava",
    "geysers and a black hole. Sneak + right-click:", "call it off."],
   ".use(Use.ARMAGEDDON).cooldown(200).cost(1).sneakUse(Use.DOOM_CANCEL, 20)",
   tex=("clock", "blood", "void", "skull"), dur=8)
it("orbital_remote", "Orbital Strike Remote", 3, "spyglass", "beacon", "gadget",
   ["Right-click: a red light marks the spot you", "look at. 2 seconds later a beam from space", "burns a hole 40 blocks deep."],
   ".use(Use.ORBITAL_STRIKE).range(128).cooldown(200).cost(1)", tex=("remote", "netherite", "redstone", "star"), dur=24)
it("cluster_bomb", "Cluster Bomb", 2, "dynamite", "dynamite", "gadget",
   ["Right-click to throw. It splits into", "8 lit TNT blocks that fly everywhere."],
   ".use(Use.CLUSTER_BOMB).cooldown(30).consume()", tex=("bomb", "redstone", "thunder", "flame"), count=2, stack=16)
it("gravity_grenade", "Gravity Grenade", 2, "dynamite", "shulker_shell", "gadget",
   ["Right-click to throw. The ground where it", "lands flies into the sky and rains down", "somewhere else. Mobs fly too."],
   ".use(Use.GRAVITY_GRENADE).cooldown(30).consume()", tex=("bomb", "void", "wind", "wing"), count=2, stack=16)
it("scatter_bomb", "Scatter Bomb", 2, "dynamite", "ender_pearl", "gadget",
   ["Right-click to throw. Every mob and item", "near the blast is teleported somewhere", "random. Players too."],
   ".use(Use.SCATTER_BOMB).cooldown(30).consume()", tex=("bomb", "ender", "void", None), count=2, stack=16)
it("gold_bomb", "Gold Bomb", 3, "dynamite", "gold_block", "gadget",
   ["Right-click to throw. Everything near", "the blast turns into gold blocks.", "Mobs too."],
   ".use(Use.GOLD_BOMB).cooldown(40).consume()", tex=("bomb", "gold", "thunder", "gem"), stack=16)
it("termite_jar", "Termite Jar", 2, "glass_bottle", "spider_eye", "gadget",
   ["Right-click something wooden: termites eat", "it and every wooden block it touches.", "Trees, houses, ships. Up to 1500 blocks."],
   ".use(Use.TERMITES).range(8).cooldown(40).consume()", tex=("jar", "glass", "meat", None), stack=16)
it("death_ray", "Solar Death Ray", 3, "magnifying_glass", "beacon", "gadget",
   ["Right-click: a beam of sunlight for 3 seconds.", "Stone melts to lava, sand to glass, wood", "burns, water boils, mobs cook."],
   ".use(Use.DEATH_RAY).range(64).cooldown(80).cost(1)", tex=("magnifier", "gold", "fire", "star"), dur=64)
it("tsunami_horn", "Tsunami Horn", 3, "ocean_orb", "goat_horn", "gadget",
   ["Right-click: a wall of water rolls away", "from you for 64 blocks. It sweeps mobs,", "flowers and torches away."],
   ".use(Use.TSUNAMI).range(32).cooldown(100).cost(1)", tex=("horn", "prismarine", "ice", None), dur=32)
it("tornado_bottle", "Tornado in a Bottle", 3, "dust_devil", "breeze_rod", "gadget",
   ["Right-click: a tornado touches down where you", "look and wanders for 15 seconds. It rips up", "the ground and throws mobs (and you) around."],
   ".use(Use.TORNADO).range(32).cooldown(300).cost(1)", tex=("tornado", "glass", "flint", None), dur=16)
it("snap_gauntlet", "Snap Gauntlet", 3, "fist_of_doom", "amethyst_block", "gadget",
   ["Right-click: half of all living things", "within 64 blocks turn to dust.", "Not players. Six snaps."],
   ".use(Use.SNAP).range(64).cooldown(600).cost(1)", tex=("glove", "gold", "void", "gem"), dur=6, where="HELD")
it("creeper_cannon", "Creeper Cannon", 2, "dispenser", "creeper_head", "gadget",
   ["Right-click: fires a lit creeper.", "Sneak + right-click: a charged one."],
   ".use(Use.CREEPER_CANNON).cooldown(20).cost(1).sneakUse(Use.CHARGED_CREEPER, 60)", tex=("cannon", "netherite", "slime", None), dur=64)
it("hive_grenade", "Hive Grenade", 2, "beehive", "gunpowder", "gadget",
   ["Right-click to throw. 10 angry bees burst out", "and sting the nearest living thing.", "That can be you."],
   ".use(Use.HIVE_GRENADE).cooldown(30).consume()", tex=("bomb", "honey", "wood", None), count=2, stack=16)
it("hot_potato", "Hot Potato", 2, "baked_potato", "tnt", "gadget",
   ["Explodes 15 seconds after you pick it up.", "Right-click: throw it away. It goes off", "3 seconds later, unless someone catches it."],
   ".use(Use.HOT_POTATO).cooldown(10).consume().hotPotato()", tex=("potato", "wheat", "fire", "flame"), stack=16)
it("floor_is_lava", "The Floor Is Lava", 2, "magma_block", "note_block", "gadget",
   ["Right-click: plays a song. For 8 seconds the", "ground around you is lava, except the block", "you stand on. Do not move."],
   ".use(Use.FLOOR_IS_LAVA).range(9).cooldown(300).cost(1)", tex=("disc", "wither", "magma", "flame"), dur=16)
it("pocket_volcano", "Pocket Volcano", 3, "magma_block", "fire_charge", "gadget",
   ["Right-click: a volcano grows out of the ground", "where you look and erupts for 10 seconds:", "lava bombs, explosions and fire."],
   ".use(Use.VOLCANO).range(48).cooldown(400).consume()", tex=("volcano", "netherite", "fire", None), stack=16)
it("mjolnir", "Mjolnir", 3, "earthshaker", "tempest_blade", "sword",
   ["Calls lightning on the enemy you hit.", "The lightning jumps to nearby enemies.",
    "Right-click: a ring of 12 lightning bolts", "and a shock wave throws everything away."],
   ".lightning(10.0f).chain(10.0f, 8.0f).knockUp(0.5f).strikeCooldown(20)"
   ".use(Use.LIGHTNING_RING).range(10).power(10.0f).cooldown(80).cost(2)",
   tex=("mjolnir", "iron", "leather", "bolt"), dur=6000, dmg=16, aspd=-3.0, level="netherite")
it("glass_cannon", "Glass Cannon", 2, "diamond_sword", "glass", "sword",
   ["60 damage.", "It shatters after one hit."],
   "", tex=("sword", "glass", "diamond", "star"), dur=1, dmg=60, aspd=-2.4, level="diamond")
it("railgun", "Railgun", 3, "crossbow", "end_rod", "gadget",
   ["Right-click: a shot that goes straight through", "walls for 128 blocks. It drills a hole and", "hits every mob on the line for 30 damage."],
   ".use(Use.RAILGUN).range(128).power(30.0f).cooldown(60).cost(1)", tex=("raygun", "iron", "thunder", "bolt"), dur=128, where="HELD")
it("flamethrower", "Flamethrower", 2, "flint_and_steel", "blaze_rod", "gadget",
   ["Right-click: 2 seconds of fire.", "Sets everything in front of you ablaze."],
   ".use(Use.FLAMETHROWER).cooldown(50).cost(1)", tex=("cannon", "copper", "fire", "flame"), dur=128, where="HELD")
it("boom_bow", "Boom Bow", 2, "bow", "tnt", "gadget",
   ["Right-click: shoots lit TNT instead of arrows.", "Sneak + right-click: a cluster bomb."],
   ".use(Use.DYNAMITE).power(2.4f).cooldown(16).cost(1).sneakUse(Use.CLUSTER_BOMB, 60)", tex=("bow", "wood", "redstone", "flame"),
   dur=200, where="HELD")
it("pig_missile", "Pig Missile", 2, "saddle", "firework_rocket", "gadget",
   ["Right-click: you ride a rocket-powered pig.", "It flies where you look and explodes when it", "hits something. You get thrown clear."],
   ".use(Use.PIG_MISSILE).cooldown(100).cost(1)", tex=("saddle", "pink", "fire", "flame"), dur=16)
it("kaiju_egg", "Kaiju Egg", 3, "egg", "bone_block", "gadget",
   ["Right-click: a random monster hatches 10", "blocks away. It is six times as big, has", "200 extra health and hunts you."],
   ".use(Use.KAIJU).cooldown(100).consume()", tex=("seed", "bone", "venom", "skull"), stack=16)
it("ring_of_fire", "Ring of Fire", 2, "flint_and_steel", "netherrack", "gadget",
   ["Right-click: a ring of fire that never goes", "out closes around you."],
   ".use(Use.RING_OF_FIRE).range(5).cooldown(100).cost(1)", tex=("charm", "magma", "fire", "flame"), dur=32)
it("storm_crown", "Storm Crown", 3, "golden_helmet", "lightning_rod", "armor",
   ["While worn: every 3 seconds lightning", "strikes a monster near you."],
   ".stormCrown()", tex=("crown", "thunder", "copper", "bolt"),
   slot="HELMET", prot=3, tough=1.0, kb=0.0, dur=400, layer="gold")
it("plague_mask", "Plague Mask", 2, "leather_helmet", "poisonous_potato", "armor",
   ["While worn: everything alive within", "6 blocks gets poisoned and withers.", "Tame pets are spared."],
   ".plagueAura()", tex=("helmet", "leather", "venom", "skull"),
   slot="HELMET", prot=1, tough=0.0, kb=0.0, dur=200, layer="leather")
it("endless_lava", "Endless Lava Bucket", 2, "lava_bucket", "bucket", "gadget",
   ["Right-click a block: places lava.", "It never runs dry."],
   ".endlessLava()", tex=("bucket", "iron", "fire", "flame"), where="HELD")

section("Instant creation")
it("genesis_seed", "Genesis Seed", 3, "grass_block", "nether_star", "gadget",
   ["Right-click: a floating island grows in the sky", "above the spot you look at. Trees, ores,", "a pond, a waterfall, flowers and sheep."],
   ".use(Use.SKY_ISLAND).range(64).cooldown(60).consume()", tex=("seed", "emerald", "gold", "star"), stack=16)
it("magic_beans", "Magic Beans", 2, "cocoa_beans", "emerald", "gadget",
   ["Right-click the ground: a beanstalk grows 64", "blocks into the sky. Climb the vines. On the", "cloud at the top: a chest, and a sleeping giant."],
   ".use(Use.BEANSTALK).range(24).cooldown(60).consume()", tex=("beans", "venom", "emerald", None), stack=16)
it("castle_box", "Castle in a Box", 3, "house_box", "stone_bricks", "gadget",
   ["Right-click: a stone castle builds itself", "where you look. Walls, four towers, a gate,", "a keep with a golden throne, two golem guards."],
   ".use(Use.CASTLE).range(32).cooldown(60).consume()", tex=("box", "stone", "gold", "shield"), stack=16)
it("pharaoh_scarab", "Pharaoh's Scarab", 2, "sandstone", "gold_block", "gadget",
   ["Right-click: a pyramid rises where you look.", "Inside: a treasure chamber.", "Do not step on the pressure plate."],
   ".use(Use.PYRAMID).range(32).cooldown(60).consume()", tex=("scarab", "gold", "lapis", None), stack=16)
it("titan_spade", "Titan's Spade", 3, "diamond_shovel", "grass_block", "gadget",
   ["Right-click: a mountain with a snowy peak", "rises where you look. Sneak + right-click:", "digs a crater and fills it with water."],
   ".use(Use.MOUNTAIN).range(48).cooldown(100).cost(1).sneakUse(Use.CRATER, 60)", tex=("shovel", "emerald", "gold", "star"), dur=64)
it("bifrost_staff", "Bifrost Staff", 2, "bridge_staff", "prismarine_crystals", "gadget",
   ["Right-click: a rainbow bridge arcs to the", "spot you look at, up to 64 blocks away."],
   ".use(Use.RAINBOW_BRIDGE).range(64).cooldown(40).cost(1)", tex=("staff3", "diamond", "pink", "star"), dur=64)
it("portal_gun", "Portal Gun", 3, "crying_obsidian", "ender_eye", "gadget",
   ["Right-click a block: a lit Nether portal", "appears there. Sneak + right-click the floor:", "a hole straight into the End."],
   ".use(Use.NETHER_PORTAL).range(32).cooldown(100).cost(1).sneakUse(Use.END_PORTAL, 200)", tex=("raygun", "void", "ender", None), dur=16)
it("mitosis_ray", "Mitosis Ray", 3, "size_ray", "slime_ball", "gadget",
   ["Right-click a mob: it splits into two.", "Sneak + right-click: every mob within", "8 blocks splits. Also the Wither."],
   ".use(Use.MITOSIS).range(32).cooldown(10).cost(1).sneakUse(Use.MITOSIS_BURST, 100)", tex=("raygun", "slime", "venom", None), dur=128)
it("menagerie_cannon", "Menagerie Cannon", 2, "dispenser", "hay_block", "gadget",
   ["Right-click: fires a random animal or", "monster. Anything from a bee to a ravager.", "Very rarely a warden."],
   ".use(Use.MENAGERIE).cooldown(8).cost(1)", tex=("cannon", "wood", "wheat", "paw"), dur=256)
it("moses_staff", "Staff of the Red Sea", 3, "ocean_orb", "stick", "gadget",
   ["Right-click: the water in front of you parts,", "a path 5 wide and 48 long down to the sea floor.", "The sea comes back after 20 seconds."],
   ".use(Use.PART_SEA).range(48).cooldown(200).cost(1)", tex=("staff", "wood", "prismarine", None), dur=32)
it("snowman_horn", "Snowman Horn", 1, "goat_horn", "snow_block", "gadget",
   ["Right-click: 8 snow golems appear around", "you and throw snowballs at monsters."],
   ".use(Use.SNOW_ARMY).power(8.0f).cooldown(400).cost(1)", tex=("horn", "ice", "bone", None), dur=16)
it("fortress_staff", "Fortress Staff", 1, "stick", "stone_bricks", "gadget",
   ["Right-click: a stone wall rises in front", "of you. Sneak + right-click: a stone dome", "closes over you."],
   ".use(Use.STONE_WALL).cooldown(20).cost(1).sneakUse(Use.STONE_DOME, 100)", tex=("staff", "stone", "iron", "shield"), dur=128)
it("farm_box", "Farm in a Box", 1, "composter", "wheat_seeds", "gadget",
   ["Right-click: a ripe 9x9 farm appears where", "you look: water, farmland, wheat, carrots,", "potatoes, beetroot and a scarecrow."],
   ".use(Use.FARM).range(24).cooldown(20).consume()", tex=("box", "wood", "wheat", "seed"), stack=16)

section("Pure chaos")
it("wild_staff", "Staff of Wild Magic", 3, "blaze_rod", "chorus_fruit", "gadget",
   ["Right-click: casts a random spell of this mod.", "From a fireball to a tornado to a nuke.", "Even the end of the world."],
   ".use(Use.WILD_MAGIC).range(48).cooldown(40).cost(1)", tex=("staff3", "ender", "blood", "question"), dur=100)
it("dice_of_fate", "Dice of Fate", 2, "bone_block", "emerald", "gadget",
   ["Right-click: roll a 20-sided die.", "1: a Wither. 20: netherite, diamonds", "and 30 levels. Everything in between."],
   ".use(Use.DICE).cooldown(100).cost(1)", tex=("dice", "bone", "blood", None), dur=20)
it("musical_chairs", "Musical Chairs", 2, "note_block", "chorus_fruit", "gadget",
   ["Right-click: every living thing within", "32 blocks swaps places with another one.", "You too."],
   ".use(Use.MUSICAL_CHAIRS).range(32).cooldown(100).cost(1)", tex=("chair", "wood", "pink", None), dur=32)
it("party_cannon", "Party Cannon", 2, "dispenser", "firework_rocket", "gadget",
   ["Right-click: a volley of exploding fireworks.", "Sneak + right-click: it rains cake."],
   ".use(Use.PARTY).cooldown(30).cost(1).sneakUse(Use.CAKE_RAIN, 200)", tex=("cannon", "pink", "thunder", "star"), dur=128)
it("gremlin_jar", "Gremlin in a Jar", 2, "firefly_jar", "fermented_spider_eye", "gadget",
   ["Keep it in your hotbar. Now and then the", "gremlin plays a prank: it swaps your items,", "opens doors, steals torches, makes you hiccup."],
   ".gremlin()", tex=("jar", "glass", "venom", None))
it("force_field", "Force Field", 2, "shield", "redstone_block", "gadget",
   ["Right-click: for 10 seconds nothing gets", "closer than 5 blocks. Mobs are pushed", "away, arrows and fireballs vanish."],
   ".use(Use.FORCE_FIELD).range(5).cooldown(400).cost(1)", tex=("orb", "diamond", "thunder", "bolt"), dur=32)
it("upside_down_cake", "Upside-Down Cake", 2, "cake", "phantom_membrane", "food",
   ["You fall upward for 6 seconds.", "Then you float back down.", "Can be eaten when full."],
   ".eatBuff(6).attr(\"generic.gravity\", -1.5, 2)", tex=("cake", "void", "pink", None), stack=16, always=True,
   nut=6, sat=0.6, fx=[("SLOW_FALLING", 45, 0)])
it("glacier_staff", "Glacier Staff", 2, "blue_ice", "blaze_rod", "gadget",
   ["Right-click: a line of ice spikes bursts out", "of the ground towards where you look.", "Mobs in the way are thrown up and frozen."],
   ".use(Use.ICE_SPIKES).range(24).cooldown(40).cost(1)", tex=("staff", "ice", "diamond", None), dur=128)
it("sheep_bomb", "Sheep Bomb", 1, "dynamite", "white_wool", "gadget",
   ["Right-click to throw.", "16 sheep in every colour burst out."],
   ".use(Use.SHEEP_BOMB).cooldown(30).consume()", tex=("bomb", "pink", "bone", "wool"), count=2, stack=16)
it("cobweb_grenade", "Cobweb Grenade", 1, "dynamite", "cobweb", "gadget",
   ["Right-click to throw. Cobwebs everywhere", "near the blast. Mobs get stuck."],
   ".use(Use.COBWEB_BOMB).cooldown(30).consume()", tex=("bomb", "bone", "wither", None), count=2, stack=16)

section("Mass destruction")
it("bedrock_breaker", "Bedrock Breaker", 3, "tnt_pickaxe", "obsidian", "pick",
   ["Right-click bedrock, barriers or anything else", "that cannot be mined: it breaks and drops.", "Mind the void."],
   ".bedrockBreak()", tex=("pickaxe", "wither", "netherite", "skull"),
   dur=2000, speed=10, dmg=6, aspd=-2.8, level="netherite")
it("fang_staff", "Fang Staff", 2, "stick", "emerald_block", "gadget",
   ["Right-click: a line of evoker fangs snaps", "towards where you look. Sneak + right-click:", "two rings of fangs around you."],
   ".use(Use.FANGS).range(16).cooldown(30).cost(1).sneakUse(Use.FANG_RING, 60)", tex=("staff", "emerald", "bone", "skull"), dur=200)
it("dragon_staff", "Dragon Staff", 3, "blaze_rod", "dragon_breath", "gadget",
   ["Right-click: shoots a dragon fireball.", "It leaves a cloud of dragon breath."],
   ".use(Use.DRAGON_FIREBALL).cooldown(30).cost(1)", tex=("staff3", "void", "pink", "flame"), dur=200)
it("poseidon_wrath", "Poseidon's Wrath", 3, "trident", "lightning_rod", "gadget",
   ["Right-click: 16 tridents and a few lightning", "bolts rain on the spot you look at."],
   ".use(Use.TRIDENT_STORM).range(64).cooldown(100).cost(1)", tex=("fork", "prismarine", "thunder", "bolt"), dur=64, where="HELD")
it("horde_horn", "Horde Horn", 2, "goat_horn", "rotten_flesh", "gadget",
   ["Right-click: 12 zombies, 6 skeletons and", "4 creepers rise from the ground around you.", "They are not your friends."],
   ".use(Use.HORDE).cooldown(200).cost(1)", tex=("horn", "venom", "bone", "skull"), dur=8)
it("paint_bomb", "Rainbow Paint Bomb", 1, "dynamite", "pink_dye", "gadget",
   ["Right-click to throw. Every block near the", "blast gets a random colour. Sheep too."],
   ".use(Use.PAINT_BOMB).cooldown(30).consume()", tex=("bomb", "pink", "diamond", "star"), count=2, stack=16)

section("Instant creation")
it("world_tree_seed", "World Tree Seed", 2, "oak_sapling", "bone_block", "gadget",
   ["Right-click the ground: a giant tree grows", "there. 40 blocks high, with roots, branches", "and a huge crown."],
   ".use(Use.WORLD_TREE).range(32).cooldown(60).consume()", tex=("seed", "wood", "emerald", "leaf"), stack=16)
it("copy_wand", "Copy-Paste Wand", 3, "dupe_mirror", "blaze_rod", "gadget",
   ["Sneak + right-click two blocks: they are the", "corners of what to copy. Right-click a block:",
    "the copy appears there. Up to 16384 blocks."],
   ".use(Use.COPY_PASTE).range(48).cooldown(20).cost(1).sneakUse(Use.COPY_CORNER, 5)", tex=("wand", "diamond", "ice", "star"), dur=200)

section("Pure chaos")
it("shulker_blaster", "Shulker Blaster", 2, "shulker_shell", "crossbow", "gadget",
   ["Right-click: homing shulker bullets fly at", "the four nearest mobs. They float away."],
   ".use(Use.SHULKER_BULLETS).range(24).cooldown(30).cost(1)", tex=("raygun", "void", "pink", None), dur=200, where="HELD")
it("hourglass", "Hourglass of Ages", 3, "chrono_clock", "bone_block", "gadget",
   ["Right-click: for 10 seconds the whole world", "ticks 100 times faster. Crops grow, grass", "spreads, fire runs wild, leaves fall."],
   ".use(Use.HOURGLASS).power(10.0f).cooldown(600).cost(1)", tex=("jar", "gold", "sand", None), dur=16)
it("monster_magnet", "Monster Magnet", 2, "magnet", "rotten_flesh", "gadget",
   ["Right-click: every mob within 32 blocks", "is pulled to you. Every single one."],
   ".use(Use.MONSTER_MAGNET).range(32).cooldown(100).cost(1)", tex=("magnet", "venom", "iron", "skull"), dur=64)

# ---------------------------------------------------------------------------------------------
# CATACLYSMS (version 1.4, third wave): destruction on the scale of chunks
# ---------------------------------------------------------------------------------------------
section("Mass destruction")
it("chunk_eraser", "Chunk Eraser", 3, "singularity", "crying_obsidian", "gadget",
   ["Right-click: the chunk you look at (16 x 16", "blocks) is erased from the sky down to", "the bedrock. Nothing drops."],
   ".use(Use.CHUNK_ERASE).range(128).cooldown(100).consume()", tex=("cube", "grass", "dirt", "flame"), stack=16)
it("region_eraser", "Region Eraser", 3, "chunk_eraser", "chunk_eraser", "gadget",
   ["Right-click: the chunk you look at and the", "8 chunks around it (48 x 48 blocks) are", "erased down to the bedrock."],
   ".use(Use.REGION_ERASE).range(128).cooldown(200).consume()", tex=("cube", "void", "wither", "skull"), stack=16)
it("chunk_inverter", "Chunk Inverter", 3, "upside_down_cake", "nether_star", "gadget",
   ["Right-click: the chunk you look at is turned", "upside down. Deep ores come to the top,", "the grass ends up at the bottom."],
   ".use(Use.CHUNK_INVERT).range(128).cooldown(200).cost(1)", tex=("cube", "stone", "grass", "question"), dur=8)
it("chunk_launcher", "Chunk Launcher", 3, "gravity_grenade", "nether_star", "gadget",
   ["Right-click: the chunk you look at is thrown", "96 blocks into the sky, with everything", "standing on it. A hole stays behind."],
   ".use(Use.CHUNK_LAUNCH).range(128).cooldown(200).cost(1)", tex=("cube", "grass", "dirt", "wing"), dur=8)
it("carpet_bomber", "Carpet Bomber", 3, "cluster_bomb", "phantom_membrane", "gadget",
   ["Right-click: 81 TNT blocks fall in rows", "over 48 x 48 blocks around the spot", "you look at."],
   ".use(Use.CARPET_BOMB).range(128).cooldown(200).cost(1)", tex=("plane", "netherite", "redstone", "flame"), dur=16)
it("orbital_annihilator", "Orbital Annihilator", 3, "orbital_remote", "end_crystal", "gadget",
   ["Right-click: 3 seconds of warning, then a", "beam 16 blocks wide burns a hole through", "everything, the bedrock too, into the void."],
   ".use(Use.ANNIHILATE).range(160).cooldown(400).cost(1)", tex=("remote", "void", "blood", "skull"), dur=8)
it("tsar_bomba", "Tsar Bomba", 3, "doomsday_device", "respawn_anchor", "gadget",
   ["Right-click: a missile flies to the spot you", "look at, up to 160 blocks away. It erases", "everything within 64 blocks, then fallout."],
   ".use(Use.TSAR_BOMBA).range(160).cooldown(400).consume()", tex=("missile", "netherite", "redstone", "skull"), stack=4)
it("event_horizon", "Event Horizon", 3, "singularity", "singularity", "gadget",
   ["Right-click: a black hole that grows for 20", "seconds. It eats everything within 16 blocks", "and pulls in everything within 40."],
   ".use(Use.EVENT_HORIZON).range(48).cooldown(400).consume()", tex=("orb", "void", "ender", "star"), stack=4)
it("fault_line", "Fault Line Spike", 3, "earthshaker", "iron_block", "gadget",
   ["Right-click: a canyon 5 blocks wide and 160", "long tears open in front of you, down to", "the bedrock. Lava runs at the bottom."],
   ".use(Use.FAULT_LINE).power(160.0f).cooldown(200).cost(1)", tex=("spike", "netherite", "magma", "bolt"), dur=16)

# ---------------------------------------------------------------------------------------------
# OTHER WORLDS (version 1.5, fourth wave): five new dimensions, and travel between all worlds
# ---------------------------------------------------------------------------------------------
section("Other worlds")
it("pocket_cube", "Pocket Dimension Cube", 3, "backpack", "ender_eye", "gadget",
   ["Right-click: you step into your own room in", "the Pocket Dimension, furnished, under the",
    "stars. Right-click there: back again.", "Sneak: take everything within 4 blocks along."],
   ".use(Use.POCKET_DIMENSION).cooldown(40).sneakUse(Use.POCKET_GROUP, 40)", tex=("cube", "pink", "void", "star"))
it("noclip_pearl", "Noclip Pearl", 3, "ender_pearl", "yellow_wool", "gadget",
   ["Right-click: you clip through the floor of", "reality into the Backrooms. Endless yellow",
    "rooms, humming lamps. Do not look at the", "eyes. Right-click there, or step on the",
    "lodestone under a green light: back."],
   ".use(Use.BACKROOMS).cooldown(40)", tex=("orb", "honey", "sand", "question"))
it("cloud_key", "Cloud Key", 3, "skeleton_key", "phantom_membrane", "gadget",
   ["Right-click: up to the Sky Realm, a world", "of floating islands with nothing below.",
    "Fall off and you drop back into your world", "from the clouds. Right-click there: back."],
   ".use(Use.SKY_REALM).cooldown(40)", tex=("key", "wind", "ice", "wing"))
it("moon_rocket", "Moon Rocket", 3, "firework_rocket", "end_stone", "gadget",
   ["Right-click: lift-off. You land softly on", "the Moon: grey dust, craters, meteorites,",
    "a black sky. You weigh a sixth, so you jump", "very high. Right-click there: fly home."],
   ".use(Use.MOON).cooldown(60)", tex=("rocket", "iron", "redstone", None))
it("looking_glass", "Looking Glass", 3, "pocket_mirror", "ender_eye", "gadget",
   ["Right-click: you step through to the Other", "Side: the same land as your world, the same",
    "villages and caves, but nobody has ever", "been there. Eternal dusk.", "Right-click there: back."],
   ".use(Use.PARALLEL).cooldown(40)", tex=("mirror", "void", "glass", "gem"))
it("dimension_hopper", "Dimension Hopper", 3, "portal_gun", "chorus_fruit", "gadget",
   ["Right-click: you jump to the next world:", "Overworld, Nether, End, the worlds of this",
    "mod, the worlds of other mods, and round", "again. Same spot on the map, safe ground."],
   ".use(Use.DIMENSION_HOP).cooldown(40).cost(1)", tex=("compass", "void", "pink", "star"), dur=64)
it("banishing_wand", "Banishing Wand", 2, "noclip_pearl", "stick", "gadget",
   ["Right-click a mob: it is banished to the", "Backrooms, thousands of blocks away.", "It does not come back."],
   ".use(Use.BANISH).range(24).cooldown(20).cost(1)", tex=("wand", "sand", "honey", "skull"), dur=64)

# ---------------------------------------------------------------------------------------------
# TELEPORTATION (version 1.5)
# ---------------------------------------------------------------------------------------------
section("Teleportation")
it("wormhole_gun", "Wormhole Gun", 3, "portal_gun", "ender_pearl", "gadget",
   ["Right-click: a blue wormhole end where you", "look. Sneak + right-click: an orange one.",
    "What goes into one comes out of the other,", "as fast as it went in. Even across worlds."],
   ".use(Use.WORMHOLE_BLUE).range(64).cooldown(5).sneakUse(Use.WORMHOLE_ORANGE, 5)", tex=("raygun", "lapis", "fire", None))
it("grave_compass", "Grave Compass", 2, "recovery_compass", "ender_pearl", "gadget",
   ["Right-click: you are taken to the spot", "where you last died, in any world."],
   ".use(Use.GRAVE_WARP).cooldown(200).cost(1)", tex=("compass", "bone", "wither", "skull"), dur=32)
it("escape_rope", "Escape Rope", 1, "lead", "feather", "gadget",
   ["Right-click: straight up to the open sky,", "from any cave. In the Nether: onto the roof."],
   ".use(Use.ESCAPE).cooldown(40).cost(1)", tex=("rope", "wheat", "wood", None), dur=32)
it("elevator_pearl", "Elevator Pearl", 2, "ender_pearl", "piston", "gadget",
   ["Right-click: up through the ceiling to the", "next floor above you. Sneak + right-click:",
    "down through the floor to the next room."],
   ".use(Use.ELEVATOR_UP).range(64).cooldown(10).cost(1).sneakUse(Use.ELEVATOR_DOWN, 10)", tex=("orb", "ender", "iron", "arrows"), dur=256)
it("friendship_bracelet", "Friendship Bracelet", 1, "string", "pink_dye", "gadget",
   ["Right-click: you go to the nearest player,", "in any world. Sneak + right-click: the", "nearest player comes to you."],
   ".use(Use.BRACELET_GO).cooldown(100).sneakUse(Use.BRACELET_PULL, 100)", tex=("bracelet", "pink", "lapis", "heart"))
it("wanderlust_atlas", "Wanderlust Atlas", 2, "map", "ender_pearl", "gadget",
   ["Right-click: you are thrown 1000 to 4000", "blocks away in a random direction, onto", "dry land if there is any. Adventure!"],
   ".use(Use.WANDER).cooldown(200).cost(1)", tex=("book", "leather", "gold", "star"), dur=32)
it("pet_whistle", "Pet Whistle", 1, "wolf_whistle", "ender_pearl", "gadget",
   ["Right-click: every tame animal of yours,", "wherever it is in a loaded place of any", "world, comes running to you."],
   ".use(Use.PET_RECALL).cooldown(100)", tex=("whistle", "gold", "ender", "paw"))

# ---------------------------------------------------------------------------------------------
# CREATION TOOLS (version 1.5)
# ---------------------------------------------------------------------------------------------
section("Instant creation")
it("blueprint", "Architect's Blueprint", 3, "house_box", "map", "gadget",
   ["Right-click the ground: a real structure of", "the game is built there at once: a village,",
    "a mansion, an end city, an ancient city ...", "Sneak + right-click: choose which one."],
   ".use(Use.BLUEPRINT).range(48).cooldown(100).cost(1).sneakUse(Use.BLUEPRINT_PICK, 5)", tex=("scroll", "lapis", "wood", None), dur=16)
it("biome_brush", "Biome Brush", 2, "verdant_staff", "painting", "gadget",
   ["Right-click: the land within 12 blocks of", "the spot you look at becomes another biome:",
    "colours, sky, weather and mobs. Sneak +", "right-click: choose the biome."],
   ".use(Use.BIOME_PAINT).range(48).cooldown(20).cost(1).sneakUse(Use.BIOME_PICK, 5)", tex=("brush", "pink", "emerald", None), dur=128)
it("builder_wand", "Builder's Wand", 1, "blaze_rod", "bricks", "gadget",
   ["Right-click a block: every connected block", "of that kind on that side grows one block",
    "outwards. Up to 64 at once. Uses the blocks", "from your inventory."],
   ".builderWand()", tex=("wand", "gold", "stone", None), dur=1024)
it("rail_wand", "Express Rail Wand", 2, "rail", "redstone_block", "gadget",
   ["Right-click: a powered railway 128 blocks", "long in the direction you face. Tunnels",
    "through hills, a track bed over valleys.", "You get into a minecart on it."],
   ".use(Use.RAIL_LINE).power(128.0f).cooldown(100).cost(1)", tex=("staff", "iron", "redstone", "bolt"), dur=32)

# ---------------------------------------------------------------------------------------------
# UTILITY (version 1.5)
# ---------------------------------------------------------------------------------------------
section("Utility")
it("quarry_box", "Quarry in a Box", 2, "excavator", "chest", "gadget",
   ["Right-click the ground: a pit of 11 x 11", "blocks is dug down to the bedrock. Ores and",
    "everything else worth keeping go into", "chests at the edge. Stone and dirt do not."],
   ".use(Use.QUARRY).range(16).power(5.0f).cooldown(200).consume()", tex=("box", "iron", "gold", "gem"), stack=16)
it("sorting_wand", "Sorting Wand", 1, "stick", "hopper", "gadget",
   ["Sneak + right-click a chest: everything you", "carry that the chest already holds goes",
    "into it, then the chest is sorted.", "Right-click: sorts your own inventory."],
   ".use(Use.SORT_SELF).sortWand().cooldown(5)", tex=("wand", "emerald", "wood", "chain"))
it("ender_mail", "Ender Mail", 1, "paper", "ender_pearl", "gadget",
   ["Hold it, and something in the other hand.", "Right-click: that stack flies to the",
    "nearest player, in any world. Rename the", "mail on an anvil to send it to that player."],
   ".use(Use.ENDER_MAIL).cooldown(20)", tex=("envelope", "bone", "ender", "gem"))
it("lumen_orb", "Lumen Orb", 2, "firefly_jar", "glowstone", "gadget",
   ["Right-click: an invisible light hangs over", "every dark floor within 24 blocks. Caves",
    "light up, and monsters cannot spawn there."],
   ".use(Use.LIGHT_UP).range(24).cooldown(60).cost(1)", tex=("orb", "thunder", "fire", "lamp"), dur=64)
it("almond_water", "Almond Water", 1, "glass_bottle", "sugar", "food",
   ["Every bad effect goes away. Regeneration.", "Smilers nearby forget you. Lies around in",
    "the Backrooms. Can be drunk when full."],
   ".cleanse()", tex=("bottle", "glass", "sand", None), stack=16, always=True, bottle=True,
   nut=2, sat=0.5, fx=[("REGENERATION", 10, 1)])

# ---------------------------------------------------------------------------------------------
# VANILLA RESULTS
# ---------------------------------------------------------------------------------------------
van("rotten_flesh", "rotten_flesh", "leather")
van("leather", "iron_ingot", "saddle")
van("paper", "string", "name_tag")
van("chain", "leather_helmet", "chainmail_helmet")
van("chain", "leather_chestplate", "chainmail_chestplate")
van("chain", "leather_leggings", "chainmail_leggings")
van("chain", "leather_boots", "chainmail_boots")
van("saddle", "iron_ingot", "iron_horse_armor")
van("saddle", "gold_ingot", "golden_horse_armor")
van("saddle", "diamond", "diamond_horse_armor")
van("diamond_sword", "prismarine_shard", "trident")
van("phantom_membrane", "shulker_shell", "elytra")
van("golden_apple", "emerald_block", "totem_of_undying")
van("golden_apple", "gold_block", "enchanted_golden_apple")
van("nautilus_shell", "diamond", "heart_of_the_sea")
van("string", "slime_ball", "cobweb")
van("gold_block", "stick", "bell")
van("glass_bottle", "lapis_lazuli", "experience_bottle")
van("coal", "flint", "gunpowder", 2)
van("charcoal", "flint", "gunpowder", 2)
van("clay_ball", "lime_dye", "slime_ball")
van("white_wool", "flint", "string", 4)
van("gravel", "gravel", "flint")
van("amethyst_block", "amethyst_shard", "budding_amethyst")
van("deepslate", "iron_block", "reinforced_deepslate")
van("glass_bottle", "chorus_fruit", "dragon_breath")
van("skeleton_skull", "coal_block", "wither_skeleton_skull")
van("obsidian", "ghast_tear", "crying_obsidian")
van("blackstone", "gold_ingot", "gilded_blackstone")
van("amethyst_shard", "sculk", "echo_shard")
van("dirt", "wheat_seeds", "grass_block")
van("dirt", "brown_mushroom", "mycelium")
van("dirt", "spruce_sapling", "podzol")
van("netherrack", "crimson_fungus", "crimson_nylium")
van("netherrack", "warped_fungus", "warped_nylium")
van("redstone", "blaze_powder", "glowstone_dust", 2)
van("glowstone_dust", "ink_sac", "glow_ink_sac")
van("yellow_wool", "slime_ball", "sponge")
van("lava_bucket", "water_bucket", "obsidian")
van("compass", "bone", "recovery_compass")
van("paper", "compass", "map")
van("book", "feather", "writable_book")
van("cobblestone", "cobblestone", "gravel", 2)
van("gravel", "water_bucket", "clay")
van("cobblestone", "oak_leaves", "mossy_cobblestone")
van("cobblestone", "birch_leaves", "mossy_cobblestone")
van("cobblestone", "spruce_leaves", "mossy_cobblestone")
van("string", "string", "lead")
van("snow_block", "water_bucket", "ice", 2)
van("sugar", "egg", "cake")

CUSTOM_IDS = {i["id"] for i in ITEMS}

# Netherite gear is accepted wherever a recipe asks for the diamond version.
NETHERITE_SWAP = {
    "diamond_pickaxe": "netherite_pickaxe", "diamond_axe": "netherite_axe",
    "diamond_shovel": "netherite_shovel", "diamond_hoe": "netherite_hoe",
    "diamond_sword": "netherite_sword", "diamond_chestplate": "netherite_chestplate",
    "diamond_leggings": "netherite_leggings",
}
