# Item Combinator

A Fabric mod for **Minecraft Java Edition 1.21.1**.

It adds one block, the **Combiner Table**. You put two items into it and get one new item.

- **177 combinations** in total.
- 139 of them make one of **138 new items**: tools, weapons, armor, gadgets, food, bombs, cheats.
- 38 of them make a vanilla item that you normally cannot craft (saddle, name tag, elytra, ...).
- Two stackable items that have no combination give a **Chaos Orb**, which does something random.
- There are no tiers and there is no balance. Some items are normal, many are absurd, a few can destroy your world.

## Status: read this first

The mod **compiles and passes its automatic tests**. GitHub runs the tests after every change, also against
the finished .jar, started the same way a player starts it. See "Automatic tests" near the end of this page.

**No person has played it yet.** The tests prove that the game starts, that every combination works, and that
every ability does its main job without an error. They do not prove that it is fun, that it runs well on a
slow PC, or that it works together with other mods. Expect smaller bugs.

**Make a backup of your world before you use the bombs.** Nukes, black holes, meteors, the Blast Pickaxe and the
World Eater delete large parts of the world for good.

## What you need

- Minecraft Java Edition **1.21.1**
- [Fabric Loader](https://fabricmc.net/use/installer/) (the program that loads mods into the game)
- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1 (a helper mod most Fabric mods need)

## Getting the .jar file

The .jar is the file the game loads. It is built from this project.

**From GitHub (no installs on your PC):** GitHub builds the .jar again after every change
(the file `.github/workflows/build.yml` tells it how). The newest .jar is on the **`builds`** branch of the
repository, next to `build.log` (the text the build printed). You can also open **Actions**, click the newest run,
and download **item-combinator-jar** at the bottom of the page.

**On your own PC:** install a Java JDK, version 25 (the version the official Fabric template builds with). Then run `gradlew.bat build` (Windows) or
`./gradlew build` (Linux, Mac) in this folder. The .jar appears in `build/libs/`. The first build downloads
about 1 GB and takes several minutes. `build` also runs the server tests. To skip them: `gradlew build -x runGameTest`.

## Installing

Put `item-combinator-1.1.0.jar` and the Fabric API .jar into the `mods` folder of your Minecraft installation.
Start the game with the Fabric profile.

## How the Combiner Table works

Craft it in a normal crafting table:

| | | |
|---|---|---|
| Iron Ingot | Diamond | Iron Ingot |
| Any Planks | Crafting Table | Any Planks |
| Any Planks | Any Planks | Any Planks |

Place it and right-click it. It has two input slots and one output slot.

- Put one item in each input slot. If the two items combine, the result shows up on the right.
- The order does not matter. A + B gives the same as B + A.
- Taking the result uses up **one item from each slot**. Combining costs nothing else.
- Shift-click the result to combine as many as you have.
- **Built-in recipe help:** put an item in only one slot. A panel on the right shows every item it combines with.
  Move the mouse over one of them to see what you would get.
- **Chaos Orb:** if both items are stackable and have no combination, the result is a Chaos Orb. Look at the result
  before you take it. Tools, weapons and armor do not stack, so they are never turned into a Chaos Orb.
- The table does not store items. They come back to you when you close it.
- The result is always brand new, even if the inputs were damaged.
- Enchantments on the inputs are kept if they fit the result. If both inputs have the same enchantment, the higher level wins.
- Netherite tools and armor work wherever a recipe asks for the diamond version. The result is the same item.
- A honey bottle gives the empty bottle back.

The name colour of a new item shows how wild it is: yellow = normal, aqua = strong, purple and glowing = absurd.
Every new item has a description under its name. All new items are also in their own creative tab.

## All combinations that make new items

"Uses" means durability. Names in *italics* are items from this mod, so you have to combine them first.

### Mining tools

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Iron Pickaxe | Iron Pickaxe | **Iron Hammer** | Mines a 3x3 area. Sneak to mine one block. | 5 damage, 750 uses |
| Diamond Pickaxe | Diamond Pickaxe | **Excavator** | Mines a 3x3x3 cube. Sneak to mine one block. | 6 damage, 4000 uses |
| Diamond Pickaxe | Blast Furnace | **Molten Pickaxe** | Smelts everything it mines. | 5 damage, 2000 uses |
| Diamond Pickaxe | Chain | **Vein Pickaxe** | Mines a whole ore vein at once. Sneak to mine one block. | 5 damage, 2000 uses |
| Diamond Pickaxe | Rabbit's Foot | **Prospector's Pickaxe** | Ores drop twice as much. | 5 damage, 1800 uses |
| Iron Pickaxe | Lantern | **Torchlight Pickaxe** | Right-click a block to place a torch. Needs no torches. Costs 2 durability. | 4 damage, 600 uses |
| Diamond Pickaxe | Block of Redstone | **Redstone Drill** | Mines very fast. Gives Haste II while held. | 4 damage, 3000 uses |
| Diamond Pickaxe | Diamond Axe | **Diamond Paxel** | Pickaxe, axe, shovel and hoe in one. | 7 damage, 3000 uses |
| Diamond Pickaxe | Diamond Shovel | **Diamond Paxel** | Second way to make it. | 7 damage, 3000 uses |
| Diamond Pickaxe | *Magnet* | **Magnetic Pickaxe** | Drops go straight into your inventory. | 5 damage, 2500 uses |
| *Excavator* | *Molten Pickaxe* | **Magma Excavator** | Mines a 3x3x3 cube and smelts it. Sneak to mine one block. | 7 damage, 6000 uses |
| *Vein Pickaxe* | *Prospector's Pickaxe* | **Motherlode Pickaxe** | Mines a whole ore vein at once. Ores drop twice as much. | 6 damage, 4000 uses |
| *Redstone Drill* | *Iron Hammer* | **Tunnel Bore** | Mines a 3x3 tunnel very fast. Gives Haste II while held. | 6 damage, 6000 uses |
| *Magma Excavator* | *Motherlode Pickaxe* | **World Eater** | Mines a 9x9x9 cube of any block. Smelts drops and doubles ores. Drops go into your inventory. Sneak to mine one block. | 9 damage, 12000 uses |
| Diamond Pickaxe | TNT | **Blast Pickaxe** | Every block you mine explodes. The blast does not hurt you. Sneak to mine one block. | 5 damage, 3000 uses |
| *Redstone Drill* | End Rod | **Mining Laser** | Right-click: burns a 3x3 tunnel 48 blocks long. Drops go to you. | 500 uses |
| *Excavator* | TNT | **Earthshaker** | Mines a 3x3x3 cube. Right-click: earthquake. The ground and all nearby mobs fly into the air. | 8 damage, 5000 uses |

### Axes

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Diamond Axe | Diamond Axe | **Lumber Axe** | Chops down the whole tree. Sneak to chop one log. | 9 damage, 3000 uses |
| Diamond Axe | Blaze Powder | **Ember Axe** | Logs drop as charcoal. Sets enemies on fire. | 9 damage, 1800 uses |
| Diamond Axe | Diamond Sword | **Battle Axe** | Hits also damage nearby enemies. | 12 damage, 2400 uses |
| Diamond Axe | Skeleton Skull | **Executioner's Axe** | Slain mobs may drop their head. Kills give extra XP. | 10 damage, 2000 uses |
| *Lumber Axe* | *Magnet* | **Deforester** | Chops down the whole tree. Logs go straight into your inventory. | 10 damage, 6000 uses |
| *Battle Axe* | *Executioner's Axe* | **Warlord's Axe** | Hits also damage nearby enemies. Slain mobs often drop their head. | 15 damage, 5000 uses |

### Shovels

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Diamond Shovel | Diamond Shovel | **Trench Shovel** | Digs a 3x3x3 cube. Sneak to dig one block. | 6 damage, 4000 uses |
| Diamond Shovel | Furnace | **Kiln Shovel** | Smelts what it digs. Sand becomes glass, clay becomes bricks. | 5.5 damage, 2000 uses |
| *Excavator* | *Trench Shovel* | **Terraformer** | Digs a 3x3x3 cube of any block. Sneak to dig one block. | 7 damage, 8000 uses |

### Farming

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Diamond Hoe | Diamond Hoe | **Harvester Hoe** | Right-click a crop to harvest and replant ripe crops in a 5x5 area. | 1 damage, 2500 uses |
| Diamond Hoe | Bone Block | **Verdant Staff** | Right-click plants to grow them. Works on a 3x3 area. | 256 uses |
| *Harvester Hoe* | *Verdant Staff* | **Farmer's Scythe** | Right-click a crop to harvest and replant ripe crops in a 9x9 area. Crops go into your inventory. | 6 damage, 5000 uses |

### Swords

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Diamond Sword | Blaze Rod | **Blazing Sword** | Sets enemies on fire. | 8 damage, 1800 uses |
| Diamond Sword | Blue Ice | **Frost Blade** | Freezes and slows enemies. | 8 damage, 1800 uses |
| Diamond Sword | Spider Eye | **Venom Blade** | Poisons enemies. | 7 damage, 1800 uses |
| Diamond Sword | Wither Skeleton Skull | **Withering Blade** | Withers enemies. | 8 damage, 1800 uses |
| Diamond Sword | Lightning Rod | **Thunder Blade** | Calls lightning on the enemy you hit. | 8 damage, 1800 uses |
| Diamond Sword | Ghast Tear | **Vampire Blade** | Heals you when you hit enemies. | 7 damage, 1800 uses |
| Diamond Sword | Ender Pearl | **Ender Blade** | Right-click to teleport 12 blocks ahead. | 8 damage, 1800 uses |
| Diamond Sword | Diamond Sword | **Greatsword** | Slow but very strong. Hits also damage nearby enemies. | 12 damage, 3000 uses |
| Diamond Sword | Breeze Rod | **Gale Saber** | Very fast. Launches enemies into the air. | 7 damage, 1800 uses |
| Golden Sword | Block of Gold | **Midas Sword** | Slain mobs drop gold nuggets. Kills give extra XP. | 7 damage, 1200 uses |
| Diamond Sword | TNT | **Creeper Cleaver** | Every hit is a real explosion. It breaks blocks. It does not hurt you. | 8 damage, 1800 uses |
| Wooden Sword | Slime Block | **Slime Bat** | Sends enemies flying. | 3 damage, 400 uses |
| *Blazing Sword* | *Frost Blade* | **Frostfire Blade** | Burns, freezes and slows enemies. Deals extra magic damage. | 10 damage, 4000 uses |
| *Venom Blade* | *Withering Blade* | **Plague Blade** | Poisons, withers and weakens enemies. | 9 damage, 4000 uses |
| *Thunder Blade* | *Gale Saber* | **Tempest Blade** | Calls lightning on the enemy you hit. The lightning jumps to nearby enemies. Launches enemies into the air. | 10 damage, 4000 uses |
| *Vampire Blade* | *Greatsword* | **Bloodthirst Greatsword** | Heals you when you hit enemies. Hits also damage nearby enemies. | 14 damage, 5000 uses |
| *Ender Blade* | Echo Shard | **Void Blade** | Right-click to teleport 28 blocks ahead. Deals extra magic damage. | 10 damage, 4000 uses |
| *Frostfire Blade* | *Tempest Blade* | **Blade of the Elements** | Burns, freezes and slows enemies. Chain lightning. Launches enemies. Right-click: fires a burning beam. | 14 damage, 9000 uses |
| *Plague Blade* | *Bloodthirst Greatsword* | **Soul Reaver** | Poisons, withers and weakens enemies. Heals you for half the damage dealt. Hits also damage nearby enemies. | 17 damage, 9000 uses |
| *Slime Bat* | Piston | **Home Run Bat** | Sends enemies flying over the horizon. | 4 damage, 800 uses |
| Diamond Sword | End Crystal | **Beam Saber** | Sets enemies on fire. Right-click: fires a beam that burns everything in a line. | 9 damage, 2500 uses |
| *Blade of the Elements* | *Soul Reaver* | **Blade of Everything** | Every sword effect of this mod at once. Right-click: fires a beam. | 25 damage, 20000 uses |

### Wands, staffs and charms

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Iron Ingot | Redstone Dust | **Magnet** | Pulls nearby items and XP to you. Keep it in your hotbar or off hand. |  |
| Blaze Rod | Fire Charge | **Wand of Fire** | Right-click to shoot a fireball. | 200 uses |
| Stick | Blue Ice | **Wand of Frost** | Right-click to freeze and slow all enemies in front of you. | 200 uses |
| Lightning Rod | Blaze Rod | **Staff of Storms** | Right-click to call lightning where you look. | 150 uses |
| End Rod | Ender Pearl | **Warp Staff** | Right-click to teleport where you look. Range: 32 blocks. | 250 uses |
| Breeze Rod | Feather | **Gust Staff** | Right-click to leap where you look. No fall damage while in your hotbar. | 300 uses |
| Blaze Rod | Glistering Melon Slice | **Staff of Healing** | Right-click to heal yourself. 15 second cooldown. | 64 uses |
| Compass | Ender Pearl | **Recall Compass** | Right-click to teleport to your spawn point. 30 second cooldown. | 32 uses |
| Gunpowder | Paper | **Dynamite** x2 | Right-click to throw. Explodes like TNT after 2 seconds. |  |
| Ender Chest | Leather | **Ender Pouch** | Right-click to open your Ender Chest anywhere. |  |
| Crafting Table | Leather | **Pocket Workbench** | Right-click to open a crafting grid anywhere. |  |
| Anvil | Diamond | **Mending Charm** | Slowly repairs all your items. Keep it in your hotbar or off hand. |  |
| Feather | Phantom Membrane | **Feather Charm** | No fall damage. Keep it in your hotbar or off hand. |  |
| Obsidian | Magma Cream | **Obsidian Charm** | Fire and lava do not hurt you. Keep it in your hotbar or off hand. |  |
| Nautilus Shell | Prismarine Crystals | **Tide Charm** | Breathe and swim fast underwater. Keep it in your hotbar or off hand. |  |
| Golden Pickaxe | Block of Redstone | **Haste Charm** | Gives Haste II. Keep it in your hotbar or off hand. |  |
| Sugar | Rabbit's Foot | **Swift Charm** | Gives Speed II. Keep it in your hotbar or off hand. |  |
| Spyglass | Eye of Ender | **Seer's Spyglass** | Right-click to make nearby mobs glow through walls for 15 seconds. | 100 uses |
| Totem of Undying | Nether Star | **Eternal Totem** | Saves you from death. Is not used up. 5 minute cooldown. Keep it in your hotbar or off hand. |  |
| Clock | Diamond | **Chrono Clock** | Right-click: skip to morning. Sneak + right-click: skip to night. | 64 uses |
| *Feather Charm* | *Swift Charm* | **Explorer's Charm** | Speed II, Jump Boost II, no fall damage. Keep it in your hotbar or off hand. |  |
| *Obsidian Charm* | *Tide Charm* | **Elemental Charm** | Immune to fire. Breathe underwater. Swim fast. Resistance I. Keep it in your hotbar or off hand. |  |
| *Wand of Fire* | TNT | **Inferno Staff** | Right-click to shoot an exploding fireball. | 300 uses |
| *Staff of Storms* | *Gust Staff* | **Tempest Staff** | Right-click: lightning storm where you look. Sneak + right-click: leap. No fall damage while in your hotbar. | 400 uses |
| *Warp Staff* | *Recall Compass* | **Rift Staff** | Right-click: teleport up to 64 blocks. Sneak + right-click: go to your spawn point. | 500 uses |
| *Explorer's Charm* | *Elemental Charm* | **Charm of the Ancients** | Speed II, Jump Boost II, Haste II. Night Vision. Immune to fire. Breathe underwater. No fall damage. +10 hearts. Hotbar or off hand. |  |
| *Inferno Staff* | *Tempest Staff* | **Staff of the Archmage** | Right-click: a huge exploding fireball. Sneak + right-click: meteor shower where you look. No fall damage. | 1500 uses |

### Wings

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Elytra | Diamond Chestplate | **Armored Elytra** | An elytra with the protection of a diamond chestplate. | 8 armor, 900 uses |
| *Armored Elytra* | Firework Rocket | **Rocket Elytra** | Armored elytra with a booster. Hold sneak while gliding to speed up. | 8 armor, 1500 uses |
| *Rocket Elytra* | Nether Star | **Seraph Wings** | Lets you fly like in creative mode. Hold sneak while gliding to speed up. | 8 armor, 4000 uses |

### Armor

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Iron Helmet | Lantern | **Miner's Helmet** | Night Vision while worn. | 2 armor, 240 uses |
| Iron Helmet | Glass | **Diving Helmet** | Breathe underwater while worn. | 2 armor, 240 uses |
| Diamond Chestplate | Magma Block | **Magma Chestplate** | Immune to fire while worn. Sets attackers on fire. | 8 armor, 600 uses |
| Iron Chestplate | Cactus | **Cactus Chestplate** | Hurts anything that attacks you. | 6 armor, 300 uses |
| Diamond Leggings | Sugar | **Swift Leggings** | Speed II while worn. | 6 armor, 560 uses |
| Iron Boots | Slime Block | **Spring Boots** | Jump Boost III while worn. No fall damage. | 2 armor, 260 uses |
| *Miner's Helmet* | *Diving Helmet* | **Spelunker's Helmet** | Night Vision, Haste I and underwater breathing while worn. | 3 armor, 500 uses |
| *Magma Chestplate* | *Cactus Chestplate* | **Inferno Thornmail** | Immune to fire. Resistance I. Attackers burn and explode. The explosion breaks no blocks. | 9 armor, 900 uses |
| *Spring Boots* | *Swift Charm* | **Hermes Boots** | Speed III and Jump Boost III. No fall damage. | 3 armor, 600 uses |
| *Spring Boots* | End Stone | **Moon Boots** | Low gravity and huge jumps. No fall damage. | 3 armor, 600 uses |

### Food

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Steak | Gold Ingot | **Golden Steak** | Absorption and Regeneration. | 10 hunger |
| Bread | Honey Bottle | **Honey Toast** | Speed I for 30 seconds. | 8 hunger |
| Pumpkin Pie | Glow Berries | **Miner's Pie** | Night Vision and Haste I for 3 minutes. | 9 hunger |
| Mushroom Stew | Blaze Powder | **Blazing Stew** | Fire Resistance for 3 minutes. Strength I for 45 seconds. | 8 hunger |
| Golden Carrot | Glowstone Dust | **Glowing Carrot** | Night Vision for 5 minutes. Can be eaten when full. | 6 hunger |
| Cookie | Sugar | **Sugar Rush Cookie** | Eaten very fast. Speed II for 20 seconds. Can be eaten when full. | 2 hunger |
| Bread | Cooked Porkchop | **Hero Sandwich** | Very filling. | 14 hunger |
| Cooked Cod | Dried Kelp | **Sailor's Kelp Roll** | Water Breathing for 2 minutes. Dolphin's Grace for 1 minute. | 9 hunger |
| *Golden Steak* | *Hero Sandwich* | **Royal Feast** | Fills your whole hunger bar. Absorption II, Regeneration II, Resistance I. | 20 hunger |
| *Royal Feast* | Enchanted Golden Apple | **Ambrosia** | Absorption IV, Regeneration II, Strength II. Resistance I and Fire Resistance. Can be eaten when full. | 20 hunger |
| Firework Rocket | Beetroot | **Rocket Chili** | Launches you high into the sky. You float back down. Can be eaten when full. | 4 hunger |

### Bombs and disasters

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| *Dynamite* | TNT | **Mega Dynamite** x2 | Right-click to throw. A huge explosion after 3 seconds. It hurts you too. |  |
| *Mega Dynamite* | Nether Star | **Pocket Nuke** x2 | Right-click to throw. Then run. Erases everything within 18 blocks. It hurts you too. |  |
| *Pocket Nuke* | *Singularity* | **Doomsday Device** | Right-click to throw. Then run far. Erases everything within 32 blocks. |  |
| *Inferno Staff* | Magma Block | **Meteor Staff** | Right-click: a meteor hits where you look. Sneak + right-click: meteor shower. | 300 uses |
| Echo Shard | Nether Star | **Singularity** x2 | Right-click: opens a black hole where you look. For 10 seconds it eats blocks and mobs. It pulls you in too. |  |
| Heart of the Sea | Sponge | **Ocean Orb** | Right-click: floods the spot you look at. Sneak + right-click: removes all water and lava around you. | 64 uses |
| *Wand of Frost* | Packed Ice | **Winter Globe** | Right-click: freezes everything around you. Water turns to ice, lava to obsidian. Mobs freeze solid. | 64 uses |
| *Staff of Storms* | Anvil | **Anvil Storm Staff** | Right-click: rains anvils where you look. | 100 uses |
| *Staff of Storms* | Egg | **Fowl Weather Staff** | Right-click: rains chickens where you look. They are real chickens. | 100 uses |
| Wither Skeleton Skull | Blaze Rod | **Wither Wand** | Right-click: shoots exploding wither skulls. | 200 uses |
| Dispenser | Crossbow | **Arrow Gatling** | Right-click: fires 10 arrows at once. Needs no arrows. | 1000 uses |

### Transformation

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Leather | Block of Gold | **Midas Glove** | Right-click a block: it and its neighbours turn into gold blocks. Hit a mob: it turns into a gold block. |  |
| Block of Emerald | Block of Redstone | **Philosopher's Stone** | Right-click a block to upgrade it: stone, coal, iron, gold, diamond, emerald, netherite. Sneak to go back. | 256 uses |
| Blaze Rod | Egg | **Polymorph Wand** | Right-click a mob: it turns into a random farm animal. | 64 uses |
| Spyglass | Amethyst Shard | **Size Ray** | Right-click a mob: it shrinks and gets weak. Sneak + right-click: it grows huge. | 128 uses |
| Cake | Bone Meal | **Eat-Me Cake** | You grow 4 times as big for 2 minutes. More health, damage and reach. Can be eaten when full. | 6 hunger |
| Red Mushroom | Brown Mushroom | **Shrinking Mushroom** | You shrink to a fifth of your size for 2 minutes. Fits through tiny gaps. Can be eaten when full. | 2 hunger |

### Mobs, time and space

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| *Chrono Clock* | Eye of Ender | **Time Stopper** | Right-click: stops time for 10 seconds. Everything freezes except you. | 32 uses |
| Bell | Fermented Spider Eye | **Bell of Madness** | Right-click: nearby monsters attack each other. | 64 uses |
| Goat Horn | Block of Iron | **Golem Horn** | Right-click: calls 3 iron golems that fight for you. | 16 uses |
| *Gust Staff* | Shulker Shell | **Gravity Staff** | Right-click: nearby mobs fly into the sky. Sneak + right-click: slams them down. | 200 uses |
| *Warp Staff* | Chorus Fruit | **Swap Staff** | Right-click a mob: you swap places. | 200 uses |
| Phantom Membrane | Ender Pearl | **Ghost Cloak** | Right-click: you are a ghost for 8 seconds. Fly through walls. Do not end inside one. | 32 uses |
| Black Wool | Ender Pearl | **Portable Hole** | Right-click a wall: opens a tunnel for 8 seconds. Then the wall comes back. | 64 uses |
| Stick | Scaffolding | **Bridge Staff** | Right-click: builds a wooden bridge 32 blocks ahead of you. | 64 uses |
| Crafting Table | Oak Door | **House in a Box** | Right-click the ground: builds a small furnished house in front of you. |  |
| *Verdant Staff* | Oak Sapling | **Staff of the Wild** | Right-click: a forest grows at once where you look. | 64 uses |
| Compass | Amethyst Shard | **Diamond Dowser** | Right-click: tells you where the nearest diamond ore or ancient debris is. | 64 uses |

### Body upgrades

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Leather | Block of Iron | **Titan's Belt** | You are 2.5 times as big. +20 hearts, +8 damage, longer reach. Keep it in your hotbar or off hand. |  |
| Sticky Piston | Leather | **Long Arm Glove** | Reach blocks and mobs 32 blocks away. Keep it in your hotbar or off hand. |  |
| Enchanted Golden Apple | Totem of Undying | **Heart of the Colossus** | +40 hearts and Regeneration II. Keep it in your hotbar or off hand. |  |
| *Haste Charm* | Block of Diamond | **Miner's Gauntlet** | Mine 10 times faster with anything, even your fist. Keep it in your hotbar or off hand. |  |
| Netherite Ingot | Nether Star | **Fist of Doom** | Whatever you hit dies. Hold it in your main hand. |  |
| *Charm of the Ancients* | *Heart of the Colossus* | **Charm of Godhood** | All of the Charm of the Ancients. Creative flight. +40 hearts. Long reach. Mine 5 times faster. Hotbar or off hand. |  |

### Cheats

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Book | Bottle o' Enchanting | **Tome of Infinite Wisdom** | Right-click: gives you 5 levels. It never runs out. |  |
| Glass Pane | Eye of Ender | **Mirror of Duplication** | Hold it in one hand, any item in the other. Right-click: copies the whole stack. |  |

### Chaos

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Any stackable item | Any other stackable item, if the two have no combination | **Chaos Orb** | Comes out when you combine two stackable items that have no recipe. Right-click: something random happens. |  |
| *Chaos Orb* | *Chaos Orb* | **Pandora's Box** | Right-click: six random things happen, one after another. |  |

## All combinations that make vanilla items

| Item A | Item B | Result |
|---|---|---|
| Rotten Flesh | Rotten Flesh | **Leather** |
| Leather | Iron Ingot | **Saddle** |
| Paper | String | **Name Tag** |
| Chain | Leather Cap | **Chainmail Helmet** |
| Chain | Leather Tunic | **Chainmail Chestplate** |
| Chain | Leather Pants | **Chainmail Leggings** |
| Chain | Leather Boots | **Chainmail Boots** |
| Saddle | Iron Ingot | **Iron Horse Armor** |
| Saddle | Gold Ingot | **Golden Horse Armor** |
| Saddle | Diamond | **Diamond Horse Armor** |
| Diamond Sword | Prismarine Shard | **Trident** |
| Phantom Membrane | Shulker Shell | **Elytra** |
| Golden Apple | Block of Emerald | **Totem of Undying** |
| Golden Apple | Block of Gold | **Enchanted Golden Apple** |
| Nautilus Shell | Diamond | **Heart of the Sea** |
| String | Slimeball | **Cobweb** |
| Block of Gold | Stick | **Bell** |
| Glass Bottle | Lapis Lazuli | **Bottle o' Enchanting** |
| Coal | Flint | **Gunpowder** x2 |
| Charcoal | Flint | **Gunpowder** x2 |
| Clay Ball | Lime Dye | **Slimeball** |
| White Wool | Flint | **String** x4 |
| Gravel | Gravel | **Flint** |
| Block of Amethyst | Amethyst Shard | **Budding Amethyst** |
| Deepslate | Block of Iron | **Reinforced Deepslate** |
| Glass Bottle | Chorus Fruit | **Dragon's Breath** |
| Skeleton Skull | Block of Coal | **Wither Skeleton Skull** |
| Obsidian | Ghast Tear | **Crying Obsidian** |
| Blackstone | Gold Ingot | **Gilded Blackstone** |
| Amethyst Shard | Sculk | **Echo Shard** |
| Dirt | Wheat Seeds | **Grass Block** |
| Dirt | Brown Mushroom | **Mycelium** |
| Dirt | Spruce Sapling | **Podzol** |
| Netherrack | Crimson Fungus | **Crimson Nylium** |
| Netherrack | Warped Fungus | **Warped Nylium** |
| Redstone Dust | Blaze Powder | **Glowstone Dust** x2 |
| Glowstone Dust | Ink Sac | **Glow Ink Sac** |
| Yellow Wool | Slimeball | **Sponge** |

## What the Chaos Orb can do

One of these, picked at random: rain of diamonds, rain of gold, full heal, 10 levels, a random item from this mod,
rain of chickens, a launch into the sky, a thunderstorm, every mob nearby becomes a farm animal, rain of anvils,
mobs float away, half a day passes, a random teleport, iron golems, an explosion, creepers, falling TNT, sickness,
a black hole, you grow, golden apples, sudden winter, the ground turns to gold, time stops.
**Pandora's Box** (two Chaos Orbs) does six of them in a row.

## The longest chains

These items need three or more combining steps. Each list shows everything that goes into one.

- **World Eater**
  - **Magma Excavator**
    - **Excavator**
      - Diamond Pickaxe
      - Diamond Pickaxe
    - **Molten Pickaxe**
      - Diamond Pickaxe
      - Blast Furnace
  - **Motherlode Pickaxe**
    - **Vein Pickaxe**
      - Diamond Pickaxe
      - Chain
    - **Prospector's Pickaxe**
      - Diamond Pickaxe
      - Rabbit's Foot

- **Blade of the Elements**
  - **Frostfire Blade**
    - **Blazing Sword**
      - Diamond Sword
      - Blaze Rod
    - **Frost Blade**
      - Diamond Sword
      - Blue Ice
  - **Tempest Blade**
    - **Thunder Blade**
      - Diamond Sword
      - Lightning Rod
    - **Gale Saber**
      - Diamond Sword
      - Breeze Rod

- **Soul Reaver**
  - **Plague Blade**
    - **Venom Blade**
      - Diamond Sword
      - Spider Eye
    - **Withering Blade**
      - Diamond Sword
      - Wither Skeleton Skull
  - **Bloodthirst Greatsword**
    - **Vampire Blade**
      - Diamond Sword
      - Ghast Tear
    - **Greatsword**
      - Diamond Sword
      - Diamond Sword

- **Charm of the Ancients**
  - **Explorer's Charm**
    - **Feather Charm**
      - Feather
      - Phantom Membrane
    - **Swift Charm**
      - Sugar
      - Rabbit's Foot
  - **Elemental Charm**
    - **Obsidian Charm**
      - Obsidian
      - Magma Cream
    - **Tide Charm**
      - Nautilus Shell
      - Prismarine Crystals

- **Staff of the Archmage**
  - **Inferno Staff**
    - **Wand of Fire**
      - Blaze Rod
      - Fire Charge
    - TNT
  - **Tempest Staff**
    - **Staff of Storms**
      - Lightning Rod
      - Blaze Rod
    - **Gust Staff**
      - Breeze Rod
      - Feather

- **Seraph Wings**
  - **Rocket Elytra**
    - **Armored Elytra**
      - Elytra
      - Diamond Chestplate
    - Firework Rocket
  - Nether Star

- **Ambrosia**
  - **Royal Feast**
    - **Golden Steak**
      - Steak
      - Gold Ingot
    - **Hero Sandwich**
      - Bread
      - Cooked Porkchop
  - Enchanted Golden Apple

- **Blade of Everything**
  - **Blade of the Elements**
    - **Frostfire Blade**
      - **Blazing Sword**
        - Diamond Sword
        - Blaze Rod
      - **Frost Blade**
        - Diamond Sword
        - Blue Ice
    - **Tempest Blade**
      - **Thunder Blade**
        - Diamond Sword
        - Lightning Rod
      - **Gale Saber**
        - Diamond Sword
        - Breeze Rod
  - **Soul Reaver**
    - **Plague Blade**
      - **Venom Blade**
        - Diamond Sword
        - Spider Eye
      - **Withering Blade**
        - Diamond Sword
        - Wither Skeleton Skull
    - **Bloodthirst Greatsword**
      - **Vampire Blade**
        - Diamond Sword
        - Ghast Tear
      - **Greatsword**
        - Diamond Sword
        - Diamond Sword

- **Pocket Nuke**
  - **Mega Dynamite**
    - **Dynamite**
      - Gunpowder
      - Paper
    - TNT
  - Nether Star

- **Doomsday Device**
  - **Pocket Nuke**
    - **Mega Dynamite**
      - **Dynamite**
        - Gunpowder
        - Paper
      - TNT
    - Nether Star
  - **Singularity**
    - Echo Shard
    - Nether Star

- **Meteor Staff**
  - **Inferno Staff**
    - **Wand of Fire**
      - Blaze Rod
      - Fire Charge
    - TNT
  - Magma Block

- **Charm of Godhood**
  - **Charm of the Ancients**
    - **Explorer's Charm**
      - **Feather Charm**
        - Feather
        - Phantom Membrane
      - **Swift Charm**
        - Sugar
        - Rabbit's Foot
    - **Elemental Charm**
      - **Obsidian Charm**
        - Obsidian
        - Magma Cream
      - **Tide Charm**
        - Nautilus Shell
        - Prismarine Crystals
  - **Heart of the Colossus**
    - Enchanted Golden Apple
    - Totem of Undying

## Things to know

- **Sneak** while mining with an area tool, vein tool, lumber axe or the Blast Pickaxe to break only one block.
- Area tools skip chests, furnaces and other blocks that hold things. They also skip blocks that are much harder
  than the block you mined (mining stone does not break obsidian next to it).
- The Lumber Axe and Deforester only fell natural trees: the logs must touch leaves that grew there. Log houses are safe.
- Charms and body upgrades work from the hotbar or the off hand. Armor works when worn.
- The Magnet also pulls back items you just dropped, about 2 seconds after you drop them.
- Mob heads only drop from zombies, skeletons, creepers, wither skeletons and piglins.
- The Midas Glove, Polymorph Wand and Size Ray do not work on players. The Midas Glove does not work on the Wither or the Ender Dragon.
- Mega Dynamite, the Pocket Nuke, the Doomsday Device and the Singularity hurt you too. Throw and run.
- Nukes and black holes delete blocks without dropping them. Bedrock stays.
- The Time Stopper uses the game's own "tick freeze". Mobs you kill while time stands still fall over when it moves again.
- The Ghost Cloak puts you in spectator mode for 8 seconds. If you are inside a wall when it ends, you suffocate.
- The Size Ray change on a mob is permanent.
- Worn armor from this mod looks like vanilla iron, diamond or netherite armor. Only the inventory picture is new.
- Wings use the vanilla elytra look.
- There are no new bows, crossbows, tridents or shields.
- Texts are in English only.

## Automatic tests

The tests are in `src/gametest`. They are not part of the mod .jar.

**Server tests** (`gradlew runGameTest`). A server starts without a window. A fake player in survival mode then:

- makes every combination, in both orders, and checks the result
- uses the Combiner Table: one click, shift-click, closing with items inside
- mines with every tool and counts the broken blocks and the drops (area, vein, tree, smelting, double ores, magnet)
- hits and kills zombies with every item and checks fire, effects, life steal, area damage, instant kill and loot
- wears every armor piece and takes a zombie hit and a fall (thorns, fall protection), and takes a deadly hit
  while holding the totem
- eats every food and checks hunger, size change and launch
- holds or wears every item with a passive ability and checks effects, body changes, flying and repair,
  then takes the item away and checks that everything goes back to normal
- right-clicks with every item, normal and sneaking, and checks what happened (a fireball exists, the pig in front
  was hurt, the tester moved, a screen opened, the item wore out, the cooldown started ...)
- waits for the slow abilities and checks them too (nuke crater, black hole, meteors, anvils, chickens, the
  portable hole closes again, the time stop and the ghost cloak end by themselves)
- runs every event of the Chaos Orb

**Client test** (`gradlew runClientTest`). The real game starts, makes a flat world, opens the Combiner Table with a
right-click, puts two items in, takes the result, and takes screenshots of the table, of every item in the
inventory, and of armor and wings on the player. It checks that every item has a name, a description, a model
and a texture.

**The same tests with the finished .jar** (`gradlew prodServerTest` and `gradlew prodClientTest`). Here the game
runs like on a player's PC: with the .jar from `build/libs` and the Fabric API file that players install.

On GitHub the logs and the screenshots of the last run are on the **`builds`** branch.

What the tests do **not** check: flying with the wings, sounds and particles, how strong or fair an item feels,
speed on a slow PC, multiplayer with real players, the Nether and the End, and other mods.

## Changing the mod

Everything about the items and recipes is in one table: `tools/spec.py`. After changing it, run
`python tools/gen.py` and `python tools/textures.py` (needs Python 3 and the Pillow package). They rewrite the
generated Java files, the models, the names and the textures. The ability code is in
`src/main/java/com/combinator/ability/`.

## License

MIT. All textures were drawn for this mod. No Minecraft assets are included.
