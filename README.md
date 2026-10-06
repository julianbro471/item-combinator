# Item Combinator

A Fabric mod for **Minecraft Java Edition 1.21.1**.

It adds one block, the **Combiner Table**. You put two items into it and get one new item.

- **318 combinations** in total.
- 268 of them make one of **264 new items**: tools, weapons, armor, gadgets, food, bombs, cheats.
- 50 of them make a vanilla item that you normally cannot craft (saddle, name tag, elytra, ...).
- Two stackable items that have no combination give a **Chaos Orb**, which does something random.
- There are no tiers and there is no balance. Some items are normal, many are absurd, a few can destroy your world.

## Status: read this first

The mod **compiles and passes its automatic tests**. GitHub runs the tests after every change, also against
the finished .jar, started the same way a player starts it. See "Automatic tests" near the end of this page.

**No person has played it yet.** The tests prove that the game starts, that every combination works, and that
every ability does its main job without an error. They do not prove that it is fun, that it runs well on a
slow PC, or that it works together with other mods. Expect smaller bugs.

**Make a backup of your world before you use the bombs.** Nukes, black holes, meteors, the Blast Pickaxe, the
World Eater, the Armageddon Clock, the Orbital Strike Remote, the Tornado, the Termite Jar, the Pocket Volcano and the Bedrock Breaker
delete or change large parts of the world for good.

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

Put `item-combinator-1.4.0.jar` and the Fabric API .jar into the `mods` folder of your Minecraft installation.
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
| Leather Boots | White Wool | **Cloud Slippers** | No fall damage. | 1 armor, 120 uses |
| Golden Helmet | Emerald | **Merchant's Crown** | Villagers give you better prices. | 2 armor, 150 uses |

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
| Rotten Flesh | Sugar | **Cured Jerky** x2 | Rotten flesh, made safe to eat. Quick to eat. | 5 hunger |
| Cooked Cod | Baked Potato | **Fish and Chips** | Very filling. Gives Luck for 2 minutes. | 12 hunger |
| Cooked Salmon | Baked Potato | **Fish and Chips** | Second way to make it. | 12 hunger |
| Apple | Sugar | **Candy Apple** | Speed II for 30 seconds. Can be eaten when full. | 4 hunger |
| Cobblestone | Bowl | **Stone Soup** | Very filling. You feel like a rock: tough, but slow. | 10 hunger |
| Cactus | Glass Bottle | **Cactus Juice** | Speed II and Jump Boost II for 30 seconds. Makes you dizzy. | 2 hunger |

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

### Everyday tools

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Wooden Pickaxe | Flint | **Flint-Tipped Pickaxe** | Faster than an iron pickaxe. Mines only what stone can mine. | 3 damage, 180 uses |
| Stone Pickaxe | Stone Pickaxe | **Stone Hammer** | Mines a 3x3 area. Sneak to mine one block. | 4 damage, 400 uses |
| Iron Pickaxe | White Wool | **Gentle Pickaxe** | Blocks drop as themselves, like with Silk Touch. | 4 damage, 350 uses |
| Golden Pickaxe | Iron Pickaxe | **Gilded Pickaxe** | As fast as gold, as tough as iron. Stone sometimes drops gold nuggets. | 4 damage, 320 uses |
| Iron Pickaxe | Iron Axe | **Iron Multitool** | Pickaxe, axe, shovel and hoe in one. | 6 damage, 600 uses |
| Iron Pickaxe | Iron Shovel | **Iron Multitool** | Second way to make it. | 6 damage, 600 uses |
| Stone Axe | Stone Axe | **Timber Axe** | Chops down small trees whole. Sneak to chop one log. | 9 damage, 300 uses |
| Stone Shovel | String | **Sifting Shovel** | Dirt, sand and gravel sometimes hide flint, nuggets, bones or even a gem. | 3.5 damage, 260 uses |
| Golden Hoe | Golden Hoe | **Golden Sickle** | Right-click a ripe crop: harvests and replants a 3x3 area. | 1 damage, 150 uses |
| Wooden Hoe | Wheat Seeds | **Planter's Hoe** | Right-click soil: tills a 3x3 patch and plants the seeds you carry. | 1 damage, 120 uses |

### Everyday weapons

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Wooden Sword | Stick | **Quarterstaff** | Reaches 2 blocks further. Pushes enemies back. | 5 damage, 150 uses |
| Stone Sword | Flint | **Jagged Blade** | Wounds keep bleeding for 6 seconds. | 6 damage, 220 uses |
| Iron Sword | Iron Sword | **Dual Blades** | Strikes twice as fast as a sword. The second blade adds 1 damage. | 4 damage, 500 uses |
| Iron Sword | Shield | **Bulwark Blade** | While held: +6 armor and you are hard to push around. | 7 damage, 600 uses |
| Golden Sword | Emerald | **Looter's Blade** | Slain mobs drop their loot twice. | 6 damage, 250 uses |
| Wooden Axe | Feather | **Boomerang** | Right-click: hits every mob in a line and brings back dropped items. | 200 uses |

### Bows and rods

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Bow | Bow | **Volley Bow** | Right-click: shoots 5 arrows at once. Needs no arrows. | 300 uses |
| Bow | Torch | **Torch Bow** | Right-click: puts a torch on the block you look at, up to 40 blocks away. | 256 uses |
| Fishing Rod | Iron Ingot | **Grappling Hook** | Right-click a block: pulls you to it. No fall damage while you hold it. | 250 uses |
| Fishing Rod | Lead | **Lasso** | Right-click a mob: pulls it to you. | 200 uses |
| Fishing Rod | Fishing Rod | **Angler's Rod** | Right-click water: a catch at once. No waiting for a bite. | 64 uses |

### Camp and travel

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Chest | Leather | **Backpack** | Right-click: opens 27 extra slots. The things stay inside the backpack. |  |
| *Backpack* | *Backpack* | **Big Backpack** | Right-click: opens 54 extra slots. Keeps what was in the two small ones. |  |
| Furnace | Flint and Steel | **Pocket Furnace** | Hold an item in the other hand. Right-click: smelts up to 8 of it. | 128 uses |
| White Bed | Leather | **Bedroll** | Right-click: you respawn here from now on. Works anywhere, at any time. |  |
| Red Bed | Leather | **Bedroll** | Second way to make it. |  |
| Compass | Cobblestone | **Waystone** | Sneak + right-click: remembers this spot. Right-click: brings you back to it. |  |
| Ladder | String | **Rope Ladder** | Right-click a wall or a ledge: ladders unroll down to the ground. | 32 uses |
| Shears | Shears | **Hedge Shears** | Right-click: shears every sheep nearby and cuts leaves and grass loose. | 300 uses |
| Gunpowder | White Wool | **Smoke Bomb** x2 | Right-click: you vanish in smoke. Mobs nearby lose track of you. |  |
| Water Bucket | Bucket | **Endless Water Bucket** | Right-click a block: places water. It never runs dry. |  |

### Animals and helpers

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Bone | Leather | **Wolf Whistle** | Right-click: a tame wolf appears. It is yours and follows you. | 3 uses |
| Hay Bale | Saddle | **Steed Horn** | Right-click: a tame, saddled, fast horse appears. The horn is used up. |  |
| Emerald | Emerald | **Trader's Token** | Right-click: a wandering trader comes to you. |  |
| Stick | Wheat | **Shepherd's Crook** | Right-click: farm animals nearby follow you for 20 seconds. | 100 uses |
| Stick | String | **Mob Net** | Right-click a mob: it goes into the net. Right-click a block: it comes out again. | 64 uses |

### Finds and luck

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Stick | Gold Ingot | **Divining Rod** | Right-click: ores within 8 blocks glow through the walls for 10 seconds. | 32 uses |
| Book | Lapis Lazuli | **Tome of Chance** | Right-click: costs 3 levels and turns into a random enchanted book. |  |
| Stick | Stick | **Hiking Staff** | While held: you step up full blocks without jumping. Short falls do not hurt. |  |
| Feather | String | **Dreamcatcher** | Phantoms never come for you. Keep it in your hotbar or off hand. |  |
| Iron Ingot | Iron Ingot | **Lucky Horseshoe** | Gives Luck: better fishing and loot. Keep it in your hotbar or off hand. |  |
| Egg | Feather | **Emergency Chicken** | Hold it and you float down slowly. No fall damage. |  |

### Movement tricks

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Sand | Glass | **Bottled Dust Devil** | Jump a second time in the air. Keep it in your hotbar or off hand. |  |
| Stick | Slimeball | **Pogo Stick** | Hold it: every landing bounces you higher. Sneak to stop. No fall damage. |  |
| Honeycomb | Leather Boots | **Sticky Boots** | Walk against a wall to climb it. Sneak to hold on. | 1 armor, 120 uses |
| Lead | Pufferfish | **Puffer Balloon** | Hold it: you float up. Sneak to sink slowly. |  |

### Odd charms

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Glass Bottle | Glowstone Dust | **Firefly Jar** | Lights up the place around you. Keep it in your hotbar or off hand. |  |
| Glass Pane | Iron Ingot | **Pocket Mirror** | A shot that hits you hits the shooter instead. Works once every 3 seconds. |  |
| Barrel | Bread | **Lunchbox** | Eats plain food from your inventory for you when you are hungry. Keep in hotbar. |  |
| Compass | Clock | **Explorer's Almanac** | Hold it: shows your position, the biome, the time, the light and slime chunks. |  |
| Leather Boots | Bone Meal | **Meadow Boots** | Flowers grow where you walk on grass. Crops near you grow faster. | 1 armor, 120 uses |
| Magma Cream | Leather Boots | **Strider Boots** | Lava hardens under your feet. It melts again behind you. | 2 armor, 160 uses |

### Odd gadgets

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Clock | Ender Pearl | **Rewind Watch** | Right-click: back to where you were 5 seconds ago, with the health of then. It has to be in your hotbar before. | 32 uses |
| Tripwire Hook | Bone | **Skeleton Key** | Right-click an iron door or an iron trapdoor: it opens or closes. | 64 uses |
| Glowstone | Note Block | **Disco Ball** | Right-click: every mob within 10 blocks stops and dances for 8 seconds. | 16 uses |
| Clay Ball | Pink Dye | **Piggy Bank** | Right-click: puts your experience in. Sneak + right-click: takes it out. |  |
| Piston | Leather | **Push Glove** | Right-click a block: pushes it away. Sneak + right-click: pulls it to you. | 128 uses |
| Lightning Rod | Feather | **Weather Vane** | Right-click: changes the weather. Clear, then rain, then thunder. | 16 uses |
| Glass Pane | Stick | **Magnifying Glass** | Right-click in sunlight: sets the mob or block you look at on fire. |  |
| Saddle | Lead | **Rodeo Saddle** | Right-click any mob to ride it. You do not steer. Sneak to get off. | 32 uses |

### Mass destruction

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| *Time Stopper* | *Doomsday Device* | **Armageddon Clock** | Right-click: the end of the world starts where you stand. 10 second countdown, then earthquake, fire rain, lightning, lava geysers and a black hole. Sneak + right-click: call it off. | 8 uses |
| Spyglass | Beacon | **Orbital Strike Remote** | Right-click: a red light marks the spot you look at. 2 seconds later a beam from space burns a hole 40 blocks deep. | 24 uses |
| *Dynamite* | *Dynamite* | **Cluster Bomb** x2 | Right-click to throw. It splits into 8 lit TNT blocks that fly everywhere. |  |
| *Dynamite* | Shulker Shell | **Gravity Grenade** x2 | Right-click to throw. The ground where it lands flies into the sky and rains down somewhere else. Mobs fly too. |  |
| *Dynamite* | Ender Pearl | **Scatter Bomb** x2 | Right-click to throw. Every mob and item near the blast is teleported somewhere random. Players too. |  |
| *Dynamite* | Block of Gold | **Gold Bomb** | Right-click to throw. Everything near the blast turns into gold blocks. Mobs too. |  |
| Glass Bottle | Spider Eye | **Termite Jar** | Right-click something wooden: termites eat it and every wooden block it touches. Trees, houses, ships. Up to 1500 blocks. |  |
| *Magnifying Glass* | Beacon | **Solar Death Ray** | Right-click: a beam of sunlight for 3 seconds. Stone melts to lava, sand to glass, wood burns, water boils, mobs cook. | 64 uses |
| *Ocean Orb* | Goat Horn | **Tsunami Horn** | Right-click: a wall of water rolls away from you for 64 blocks. It sweeps mobs, flowers and torches away. | 32 uses |
| *Bottled Dust Devil* | Breeze Rod | **Tornado in a Bottle** | Right-click: a tornado touches down where you look and wanders for 15 seconds. It rips up the ground and throws mobs (and you) around. | 16 uses |
| *Fist of Doom* | Block of Amethyst | **Snap Gauntlet** | Right-click: half of all living things within 64 blocks turn to dust. Not players. Six snaps. | 6 uses |
| Dispenser | Creeper Head | **Creeper Cannon** | Right-click: fires a lit creeper. Sneak + right-click: a charged one. | 64 uses |
| Beehive | Gunpowder | **Hive Grenade** x2 | Right-click to throw. 10 angry bees burst out and sting the nearest living thing. That can be you. |  |
| Baked Potato | TNT | **Hot Potato** | Explodes 15 seconds after you pick it up. Right-click: throw it away. It goes off 3 seconds later, unless someone catches it. |  |
| Magma Block | Note Block | **The Floor Is Lava** | Right-click: plays a song. For 8 seconds the ground around you is lava, except the block you stand on. Do not move. | 16 uses |
| Magma Block | Fire Charge | **Pocket Volcano** | Right-click: a volcano grows out of the ground where you look and erupts for 10 seconds: lava bombs, explosions and fire. |  |
| *Earthshaker* | *Tempest Blade* | **Mjolnir** | Calls lightning on the enemy you hit. The lightning jumps to nearby enemies. Right-click: a ring of 12 lightning bolts and a shock wave throws everything away. | 16 damage, 6000 uses |
| Diamond Sword | Glass | **Glass Cannon** | 60 damage. It shatters after one hit. | 60 damage, 1 uses |
| Crossbow | End Rod | **Railgun** | Right-click: a shot that goes straight through walls for 128 blocks. It drills a hole and hits every mob on the line for 30 damage. | 128 uses |
| Flint and Steel | Blaze Rod | **Flamethrower** | Right-click: 2 seconds of fire. Sets everything in front of you ablaze. | 128 uses |
| Bow | TNT | **Boom Bow** | Right-click: shoots lit TNT instead of arrows. Sneak + right-click: a cluster bomb. | 200 uses |
| Saddle | Firework Rocket | **Pig Missile** | Right-click: you ride a rocket-powered pig. It flies where you look and explodes when it hits something. You get thrown clear. | 16 uses |
| Egg | Bone Block | **Kaiju Egg** | Right-click: a random monster hatches 10 blocks away. It is six times as big, has 200 extra health and hunts you. |  |
| Flint and Steel | Netherrack | **Ring of Fire** | Right-click: a ring of fire that never goes out closes around you. | 32 uses |
| Golden Helmet | Lightning Rod | **Storm Crown** | While worn: every 3 seconds lightning strikes a monster near you. | 3 armor, 400 uses |
| Leather Cap | Poisonous Potato | **Plague Mask** | While worn: everything alive within 6 blocks gets poisoned and withers. Tame pets are spared. | 1 armor, 200 uses |
| Lava Bucket | Bucket | **Endless Lava Bucket** | Right-click a block: places lava. It never runs dry. |  |
| *Blast Pickaxe* | Obsidian | **Bedrock Breaker** | Right-click bedrock, barriers or anything else that cannot be mined: it breaks and drops. Mind the void. | 6 damage, 2000 uses |
| Stick | Block of Emerald | **Fang Staff** | Right-click: a line of evoker fangs snaps towards where you look. Sneak + right-click: two rings of fangs around you. | 200 uses |
| Blaze Rod | Dragon's Breath | **Dragon Staff** | Right-click: shoots a dragon fireball. It leaves a cloud of dragon breath. | 200 uses |
| Trident | Lightning Rod | **Poseidon's Wrath** | Right-click: 16 tridents and a few lightning bolts rain on the spot you look at. | 64 uses |
| Goat Horn | Rotten Flesh | **Horde Horn** | Right-click: 12 zombies, 6 skeletons and 4 creepers rise from the ground around you. They are not your friends. | 8 uses |
| *Dynamite* | Pink Dye | **Rainbow Paint Bomb** x2 | Right-click to throw. Every block near the blast gets a random colour. Sheep too. |  |

### Instant creation

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Grass Block | Nether Star | **Genesis Seed** | Right-click: a floating island grows in the sky above the spot you look at. Trees, ores, a pond, a waterfall, flowers and sheep. |  |
| Cocoa Beans | Emerald | **Magic Beans** | Right-click the ground: a beanstalk grows 64 blocks into the sky. Climb the vines. On the cloud at the top: a chest, and a sleeping giant. |  |
| *House in a Box* | Stone Bricks | **Castle in a Box** | Right-click: a stone castle builds itself where you look. Walls, four towers, a gate, a keep with a golden throne, two golem guards. |  |
| Sandstone | Block of Gold | **Pharaoh's Scarab** | Right-click: a pyramid rises where you look. Inside: a treasure chamber. Do not step on the pressure plate. |  |
| Diamond Shovel | Grass Block | **Titan's Spade** | Right-click: a mountain with a snowy peak rises where you look. Sneak + right-click: digs a crater and fills it with water. | 64 uses |
| *Bridge Staff* | Prismarine Crystals | **Bifrost Staff** | Right-click: a rainbow bridge arcs to the spot you look at, up to 64 blocks away. | 64 uses |
| Crying Obsidian | Eye of Ender | **Portal Gun** | Right-click a block: a lit Nether portal appears there. Sneak + right-click the floor: a hole straight into the End. | 16 uses |
| *Size Ray* | Slimeball | **Mitosis Ray** | Right-click a mob: it splits into two. Sneak + right-click: every mob within 8 blocks splits. Also the Wither. | 128 uses |
| Dispenser | Hay Bale | **Menagerie Cannon** | Right-click: fires a random animal or monster. Anything from a bee to a ravager. Very rarely a warden. | 256 uses |
| *Ocean Orb* | Stick | **Staff of the Red Sea** | Right-click: the water in front of you parts, a path 5 wide and 48 long down to the sea floor. The sea comes back after 20 seconds. | 32 uses |
| Goat Horn | Snow Block | **Snowman Horn** | Right-click: 8 snow golems appear around you and throw snowballs at monsters. | 16 uses |
| Stick | Stone Bricks | **Fortress Staff** | Right-click: a stone wall rises in front of you. Sneak + right-click: a stone dome closes over you. | 128 uses |
| Composter | Wheat Seeds | **Farm in a Box** | Right-click: a ripe 9x9 farm appears where you look: water, farmland, wheat, carrots, potatoes, beetroot and a scarecrow. |  |
| Oak Sapling | Bone Block | **World Tree Seed** | Right-click the ground: a giant tree grows there. 40 blocks high, with roots, branches and a huge crown. |  |
| *Mirror of Duplication* | Blaze Rod | **Copy-Paste Wand** | Sneak + right-click two blocks: they are the corners of what to copy. Right-click a block: the copy appears there. Up to 16384 blocks. | 200 uses |

### Pure chaos

| Item A | Item B | Result | What it does | Stats |
|---|---|---|---|---|
| Blaze Rod | Chorus Fruit | **Staff of Wild Magic** | Right-click: casts a random spell of this mod. From a fireball to a tornado to a nuke. Even the end of the world. | 100 uses |
| Bone Block | Emerald | **Dice of Fate** | Right-click: roll a 20-sided die. 1: a Wither. 20: netherite, diamonds and 30 levels. Everything in between. | 20 uses |
| Note Block | Chorus Fruit | **Musical Chairs** | Right-click: every living thing within 32 blocks swaps places with another one. You too. | 32 uses |
| Dispenser | Firework Rocket | **Party Cannon** | Right-click: a volley of exploding fireworks. Sneak + right-click: it rains cake. | 128 uses |
| *Firefly Jar* | Fermented Spider Eye | **Gremlin in a Jar** | Keep it in your hotbar. Now and then the gremlin plays a prank: it swaps your items, opens doors, steals torches, makes you hiccup. |  |
| Shield | Block of Redstone | **Force Field** | Right-click: for 10 seconds nothing gets closer than 5 blocks. Mobs are pushed away, arrows and fireballs vanish. | 32 uses |
| Cake | Phantom Membrane | **Upside-Down Cake** | You fall upward for 6 seconds. Then you float back down. Can be eaten when full. | 6 hunger |
| Blue Ice | Blaze Rod | **Glacier Staff** | Right-click: a line of ice spikes bursts out of the ground towards where you look. Mobs in the way are thrown up and frozen. | 128 uses |
| *Dynamite* | White Wool | **Sheep Bomb** x2 | Right-click to throw. 16 sheep in every colour burst out. |  |
| *Dynamite* | Cobweb | **Cobweb Grenade** x2 | Right-click to throw. Cobwebs everywhere near the blast. Mobs get stuck. |  |
| Shulker Shell | Crossbow | **Shulker Blaster** | Right-click: homing shulker bullets fly at the four nearest mobs. They float away. | 200 uses |
| *Chrono Clock* | Bone Block | **Hourglass of Ages** | Right-click: for 10 seconds the whole world ticks 100 times faster. Crops grow, grass spreads, fire runs wild, leaves fall. | 16 uses |
| *Magnet* | Rotten Flesh | **Monster Magnet** | Right-click: every mob within 32 blocks is pulled to you. Every single one. | 64 uses |

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
| Lava Bucket | Water Bucket | **Obsidian** |
| Compass | Bone | **Recovery Compass** |
| Paper | Compass | **Empty Map** |
| Book | Feather | **Book and Quill** |
| Cobblestone | Cobblestone | **Gravel** x2 |
| Gravel | Water Bucket | **Clay** |
| Cobblestone | Oak Leaves | **Mossy Cobblestone** |
| Cobblestone | Birch Leaves | **Mossy Cobblestone** |
| Cobblestone | Spruce Leaves | **Mossy Cobblestone** |
| String | String | **Lead** |
| Snow Block | Water Bucket | **Ice** x2 |
| Sugar | Egg | **Cake** |

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

- **Armageddon Clock**
  - **Time Stopper**
    - **Chrono Clock**
      - Clock
      - Diamond
    - Eye of Ender
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

- **Mjolnir**
  - **Earthshaker**
    - **Excavator**
      - Diamond Pickaxe
      - Diamond Pickaxe
    - TNT
  - **Tempest Blade**
    - **Thunder Blade**
      - Diamond Sword
      - Lightning Rod
    - **Gale Saber**
      - Diamond Sword
      - Breeze Rod

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
- The Volley Bow and the Torch Bow are not drawn like a normal bow: one right-click is one shot.
  There are no new crossbows, tridents or shields.
- **Backpacks:** only the backpack in your hand opens. A backpack cannot go into a backpack or a shulker box, and
  a shulker box cannot go into a backpack. If a backpack burns on the ground, the things inside fall out.
- The Mob Net takes every mob except the Ender Dragon and the Wither. The mob keeps its health, name, trades and
  what it carries. The net wears out when the mob comes out, so it never breaks with a mob inside.
- A Waystone remembers one spot. It also brings you back from the Nether or the End.
- The Bedroll sets your respawn point without a bed. If the spot is blocked when you die, you wake up at the world spawn.
- The Planter's Hoe plants wheat seeds, carrots, potatoes or beetroot seeds from your inventory.
- The Angler's Rod catches fish and junk, but no treasure. For treasure you need a normal fishing rod.
- The Tome of Chance can give any enchantment, also Mending, also a curse. The books are weaker than the best
  books of an enchanting table.
- Bleeding from the Jagged Blade does not add up. A new hit only starts the 6 seconds again.
- The Divining Rod shows at most 96 ore blocks at a time.
- The Endless Water Bucket does not work in the Nether, like a normal water bucket.
- **Double jump, Pogo Stick and Sticky Boots** are worked out by your own game, not by the server (like all player
  movement). They are made for single player. On a server without this mod they do nothing.
- Sticky Boots: you only climb while you walk against the wall. Let go of the forward key and you fall.
- The Firefly Jar uses the game's invisible light block. It moves with you and is removed when you put the jar away,
  leave the world or die. If the game crashes, one invisible light can stay behind.
- The Strider Boots only harden still lava (not flowing lava), and only while you stand on something. Jumping into
  a lava lake from above still ends badly.
- The Rewind Watch gives back health, not items. It does not undo anything you did to the world.
- The Disco Ball does not work on the Wither and the Ender Dragon. A creeper that was about to explode calms down.
- The Push Glove moves what a piston can move: no obsidian, no chests, no doors. A block with a window of its own
  (crafting table, furnace) can only be pulled, because a normal right-click opens it.
- The Rodeo Saddle lets you sit on a mob. The mob goes where it wants. Hostile mobs still attack you.
- The Lunchbox never eats food with side effects: no golden apples, no rotten flesh, none of the special foods.
- The Pocket Mirror stops arrows, tridents, fireballs and other shots, one every 3 seconds. A ghast fireball still
  explodes next to you.
- The Magnifying Glass needs the sun: daytime, no rain, open sky above you.
- **Armageddon Clock:** it takes about 50 seconds from the right-click to the end. It happens around the spot where
  you stood, not around you, so you can run. Only the player who started it can call it off (sneak + right-click
  with the clock). If the world is closed in the middle, it stops. Everybody within 160 blocks sees the countdown.
- The Orbital Strike, the Termite Jar, the Solar Death Ray, the Tornado and the Railgun do not drop the blocks they
  destroy. Bedrock and other unbreakable blocks stay. Obsidian also stops the Railgun and the Death Ray.
- The Termite Jar eats everything an axe mines fastest (logs, planks, doors, fences, crafting tables, pumpkins ...),
  but no chests or other blocks that hold things. It stops after 1500 blocks.
- The Tsunami Horn and the Staff of the Red Sea leave no water behind. The Red Sea comes back after 20 seconds,
  also over anything you put in its place. In the Nether the tsunami boils away into steam.
- The Tornado also throws you around if you get too close. You take fall damage when it lets you go.
- The Floor Is Lava puts the ground back after 8 seconds. If the world is closed in the middle, the lava stays.
- The Hot Potato counts down while it is anywhere in your inventory. Put it in a chest to stop the countdown.
  A thrown potato that somebody picks up starts counting for them.
- The Snap Gauntlet never takes players or the Ender Dragon. It does take the Wither, villagers and pets.
- The Mitosis Ray copies everything about a mob: health, name, trades, what it holds. It also copies the Wither.
- The Menagerie Cannon and the Kaiju Egg spawn real mobs. Some of them (a warden, a ghast, a ravager) can kill you.
- The Portal Gun makes real portals. The End portal leads to the End from anywhere, and back home from the End.
- Magic Beans: the beanstalk is 64 blocks high. Climb the vines on its sides.
  The giant on the cloud does not move. Do not ask why.
- The Pharaoh's Scarab trap is real: the pressure plate in the treasure chamber sets off TNT under the floor.
- The Staff of Wild Magic picks from every right-click ability of this mod, the Armageddon Clock and the Pocket
  Nuke included. It never picks an ability that needs a special item in your hand (backpack, waystone ...).
- The Dice of Fate is fair: every number from 1 to 20 is equally likely. 1 spawns a real Wither.
- The Gremlin in a Jar plays a prank about every 20 seconds while it is in your hotbar or off hand.
- The Upside-Down Cake turns your gravity around for 6 seconds. Under a roof you stand on the ceiling.
  Slow Falling lasts 45 seconds, so the way back down is safe.
- The Storm Crown's lightning only looks real: it never hits you, and it starts no fires.
- The Plague Mask also makes villagers and other friendly animals sick. Only tame pets are spared.
- The Bedrock Breaker breaks bedrock, barriers and other unbreakable blocks, but nothing that holds things
  (no End portals, no command blocks). Breaking the bottom of the world opens the void. Each block costs 10 durability.
- The Copy-Paste Wand copies blocks, but not chests or other blocks that hold things, and no mobs.
  It copies what is there at the moment you paste, so you can copy a building that changed since you marked it.
- The Hourglass of Ages changes the game rule randomTickSpeed for 10 seconds, for the whole world.
  If the world is closed in the middle, the old value is put back first.
- The Horde Horn and the Monster Magnet bring real monsters. Use them at night only if you are brave.
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
- runs every event of the Chaos Orb, and its random teleport also in the Nether
- fills, closes, opens and merges backpacks, and tries to put a backpack and a shulker box into a backpack
- remembers a spot with the Waystone and travels back to it
- catches a pig with the Mob Net (sent the way a real game client sends a right-click) and lets it out again
- kills a sheep with a normal sword and with the Looter's Blade and compares the wool
- hits an iron golem twice with the Jagged Blade and measures the bleeding for 4 seconds
- wears, holds and carries the odd charms for 20 seconds: balloon, light, lunchbox, flower boots, lava boots,
  and goes 5 seconds back in time with the Rewind Watch
- lets a skeleton arrow bounce off the Pocket Mirror, fills and empties the Piggy Bank, rides a cow
- runs the whole Armageddon from countdown to the final blast, and checks that the stage is gone
- waits for every bomb, the tornado, the volcano, the tsunami, the termites, the castle, the pyramid, the island,
  the beanstalk, the mountain and the parted sea, and checks what each one left behind
- copies a block with the Copy-Paste Wand and pastes it, breaks bedrock with the Bedrock Breaker
- rolls every number of the Dice of Fate, plays every prank of the gremlin, carries the Hot Potato until it
  explodes, and wears the Storm Crown and the Plague Mask next to a zombie

**Client test** (`gradlew runClientTest`). The real game starts, makes a flat world, opens the Combiner Table with a
right-click, puts two items in, takes the result, and takes screenshots of the table, of every item in the
inventory, and of armor and wings on the player. It checks that every item has a name, a description, a model
and a texture. It also presses the jump and forward keys (pretend key presses) and measures how high the
player gets with the double jump, the Pogo Stick and the Sticky Boots.

**The same tests with the finished .jar** (`gradlew prodServerTest` and `gradlew prodClientTest`). Here the game
runs like on a player's PC: with the .jar from `build/libs` and the Fabric API file that players install.

On GitHub the logs and the screenshots of the last run are on the **`builds`** branch.

What the tests do **not** check: flying with the wings, sounds and particles, how strong or fair an item feels,
speed on a slow PC, multiplayer with real players, most abilities in the Nether and the End, and other mods.

## Changing the mod

Everything about the items and recipes is in one table: `tools/spec.py`. After changing it, run
`python tools/gen.py` and `python tools/textures.py` (needs Python 3 and the Pillow package). They rewrite the
generated Java files, the models, the names and the textures. The ability code is in
`src/main/java/com/combinator/ability/`.

## License

MIT. All textures were drawn for this mod. No Minecraft assets are included.
