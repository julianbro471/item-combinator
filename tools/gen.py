"""Generates Java registration code, recipes, models, lang, tags and docs data from spec.py."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import spec

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
JAVA = ROOT + '/src/main/java/com/combinator'
RES = ROOT + '/src/main/resources'
ASSETS = RES + '/assets/combinator'
DATA = RES + '/data'
# English names of the vanilla items used in recipes (for the documentation only)
VAN_NAMES = json.load(open(HERE + '/vanilla_names.json'))


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def wjson(path, obj):
    write(path, json.dumps(obj, indent=2, ensure_ascii=False) + '\n')


def full(i):
    return ('combinator:' if i in spec.CUSTOM_IDS else 'minecraft:') + i


def display(i):
    if i in spec.CUSTOM_IDS:
        return next(x['name'] for x in spec.ITEMS if x['id'] == i)
    return VAN_NAMES.get(i, i.replace('_', ' ').title())


def f(x):
    """Java float literal."""
    s = repr(float(x))
    return s + 'F'


TOOL_KINDS = ('pick', 'axe', 'shovel', 'hoe', 'sword', 'paxel')
ENCH = {1: 14, 2: 18, 3: 22}
DEFAULT_WHERE = {'gadget': 'HOTBAR', 'armor': 'WORN', 'wings': 'WORN'}
HANDHELD_TEX = {'wand', 'staff', 'staff3', 'scythe', 'bat'}

# ------------------------------------------------------------------------------------------ Java

def item_expr(it):
    k, tier = it['kind'], it['rar']
    props = f'props({tier})'
    if k in TOOL_KINDS:
        mat = f'ComboMaterial.of({it["dur"]}, {f(it["speed"]) if "speed" in it else "1.0F"}, "{it["level"]}", {ENCH[tier]})'
        cls = {'pick': 'CPick', 'axe': 'CAxe', 'shovel': 'CShovel', 'hoe': 'CHoe', 'paxel': 'CPaxel', 'sword': 'CSword'}[k]
        dmg = str(int(it['dmg'])) if k == 'sword' else f(it['dmg'])
        return f'new {cls}({mat}, {dmg}, {f(it["aspd"])}, {props})'
    if k == 'gadget':
        if it.get('dur'):
            props += f'.maxDamage({it["dur"]})'
        else:
            props += f'.maxCount({it.get("stack", 1)})'
        return f'new CItem({props})'
    if k == 'food':
        b = f'new FoodComponent.Builder().nutrition({it["nut"]}).saturationModifier({f(it["sat"])})'
        for eff, secs, amp in it['fx']:
            b += f'\n\t\t\t\t\t.statusEffect(new StatusEffectInstance(StatusEffects.{eff}, {secs * 20}, {amp}), 1.0F)'
        if it.get('always'): b += '.alwaysEdible()'
        if it.get('snack'): b += '.snack()'
        if it.get('bowl'): b += '.usingConvertsTo(Items.BOWL)'
        return f'new CItem({props}.maxCount({it.get("stack", 64)}).food({b}.build()))'
    if k == 'armor':
        mat = f'armorMaterial("{it["id"]}", {it["prot"]}, {f(it["tough"])}, {f(it["kb"])}, "{it["layer"]}", false)'
        return f'new CArmor({mat}, ArmorItem.Type.{it["slot"]}, {props}.maxDamage({it["dur"]}))'
    if k == 'wings':
        mat = f'armorMaterial("{it["id"]}", {it["prot"]}, {f(it["tough"])}, {f(it["kb"])}, "{it["layer"]}", true)'
        return f'new CWings({mat}, {props}.maxDamage({it["dur"]}))'
    raise ValueError(k)


def traits_expr(it):
    if not it['traits']:
        return 'null'
    where = it['where'] or DEFAULT_WHERE.get(it['kind'], 'HELD')
    return f'new Traits().at(Traits.Where.{where}){it["traits"]}'


def gen_items_java():
    out = []
    out.append('''package com.combinator.item;

import com.combinator.ItemCombinator;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

/**
 * All combined items. GENERATED from tools/spec.py - change the table there, not this file.
 * Each line: id, rarity (1-3, sets the name colour), number of tooltip lines, the item itself, its abilities.
 */
public final class ComboItems {
	/** Every combined item in creative-tab order. */
	public static final List<Item> ALL = new ArrayList<>();
	private static final Map<Item, Traits> TRAITS = new IdentityHashMap<>();
	private static final Map<Item, Integer> TIP_LINES = new IdentityHashMap<>();
	private static final Map<Item, Integer> RARITIES = new IdentityHashMap<>();
''')
    for it in spec.ITEMS:
        const = it['id'].upper()
        out.append(f'\tpublic static final Item {const} = add("{it["id"]}", {it["rar"]}, {len(it["tip"])},\n'
                   f'\t\t\t{item_expr(it)},\n\t\t\t{traits_expr(it)});\n')
    out.append('''
	private ComboItems() {
	}

	/** Loads this class, which registers all items. */
	public static void init() {
	}

	/** The abilities of an item, or null if it is not a combined item or has no abilities. */
	public static Traits traits(Item item) {
		return TRAITS.get(item);
	}

	/** How many tooltip lines an item has (lang keys item.combinator.ID.tip1, tip2, ...). */
	public static int tipLines(Item item) {
		Integer lines = TIP_LINES.get(item);
		return lines == null ? 0 : lines;
	}

	/** 1, 2 or 3 for combined items (3 = the wildest), 0 for everything else. */
	public static int rarity(Item item) {
		Integer rarity = RARITIES.get(item);
		return rarity == null ? 0 : rarity;
	}

	private static Item add(String id, int rarity, int tipLines, Item item, Traits traits) {
		Registry.register(Registries.ITEM, ItemCombinator.id(id), item);
		ALL.add(item);
		TIP_LINES.put(item, tipLines);
		RARITIES.put(item, rarity);
		if (traits != null) {
			TRAITS.put(item, traits);
		}
		return item;
	}

	/** Rarity 1 = yellow name, 2 = aqua, 3 = purple, glowing and fireproof. */
	private static Item.Settings props(int rarity) {
		Item.Settings settings = new Item.Settings().rarity(rarity <= 1 ? Rarity.UNCOMMON : rarity == 2 ? Rarity.RARE : Rarity.EPIC);
		if (rarity >= 3) {
			settings = settings.fireproof().component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return settings;
	}

	/** layer is the vanilla armor look that is shown when the piece is worn: "iron", "diamond" or "netherite". */
	private static RegistryEntry<ArmorMaterial> armorMaterial(String id, int protection, float toughness, float knockbackResistance, String layer, boolean wings) {
		EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			defense.put(type, protection);
		}
		ArmorMaterial material = new ArmorMaterial(
				defense,
				15,
				wings ? SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA : SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
				() -> Ingredient.ofItems(Items.DIAMOND),
				List.of(new ArmorMaterial.Layer(Identifier.ofVanilla(layer))),
				toughness,
				knockbackResistance);
		return Registry.registerReference(Registries.ARMOR_MATERIAL, ItemCombinator.id(id), material);
	}
}
''')
    write(JAVA + '/item/ComboItems.java', ''.join(out))


def all_recipes():
    """(a, b, result, count, rarity, is_alt) for every combination. rarity 0 = the result is a vanilla item."""
    rows = []
    for it in spec.ITEMS:
        if it['a'] is None:
            continue    # no fixed recipe (Chaos Orb)
        rows.append((it['a'], it['b'], it['id'], it['count'], it['rar'], False))
        for a, b in it['alt']:
            rows.append((a, b, it['id'], it['count'], it['rar'], False))
    for v in spec.VANILLA:
        rows.append((v['a'], v['b'], v['result'], v['count'], 0, False))
    seen = {tuple(sorted((r[0], r[1]))) for r in rows}
    extra = []
    sw = spec.NETHERITE_SWAP
    for a, b, res, cnt, tier, _ in rows:
        cands = set()
        if a in sw: cands.add((sw[a], b))
        if b in sw: cands.add((a, sw[b]))
        if a in sw and b in sw: cands.add((sw[a], sw[b]))
        for ca, cb in sorted(cands):
            key = tuple(sorted((ca, cb)))
            if key in seen: continue
            seen.add(key)
            extra.append((ca, cb, res, cnt, tier, True))
    return rows, extra


def gen_recipes_java():
    rows, extra = all_recipes()
    out = ['''package com.combinator.recipe;

/** The list of all combinations. GENERATED from tools/spec.py - change the table there, not this file. */
final class ComboRecipeList {
	private ComboRecipeList() {
	}

	static void registerAll() {
''']
    def line(r):
        return f'\t\tComboRecipes.add("{full(r[0])}", "{full(r[1])}", "{full(r[2])}", {r[3]}, {r[4]});\n'
    out.append('\t\t// --- new items ---\n')
    out += [line(r) for r in rows if r[4] > 0]
    out.append('\n\t\t// --- vanilla items ---\n')
    out += [line(r) for r in rows if r[4] == 0]
    out.append('\n\t\t// --- netherite gear is accepted in place of diamond gear ---\n')
    out += [line(r) for r in extra]
    out.append('\t}\n}\n')
    write(JAVA + '/recipe/ComboRecipeList.java', ''.join(out))
    return rows, extra

# ------------------------------------------------------------------------------------------ resources

def gen_resources():
    lang = {
        'block.combinator.combiner_table': 'Combiner Table',
        'container.combinator.combiner': 'Combine Items',
        'itemGroup.combinator.main': spec.MOD_NAME,
        'gui.combinator.hints': 'Combines with:',
        'gui.combinator.hint_none': 'No combinations',
    }
    for it in spec.ITEMS:
        lang[f'item.combinator.{it["id"]}'] = it['name']
        for n, line in enumerate(it['tip'], 1):
            lang[f'item.combinator.{it["id"]}.tip{n}'] = line
        handheld = it['kind'] in TOOL_KINDS or it['tex'][0] in HANDHELD_TEX
        wjson(f'{ASSETS}/models/item/{it["id"]}.json', {
            'parent': 'minecraft:item/handheld' if handheld else 'minecraft:item/generated',
            'textures': {'layer0': f'combinator:item/{it["id"]}'}})
    wjson(f'{ASSETS}/lang/en_us.json', lang)

    wjson(f'{ASSETS}/blockstates/combiner_table.json', {'variants': {'': {'model': 'combinator:block/combiner_table'}}})
    wjson(f'{ASSETS}/models/block/combiner_table.json', {
        'parent': 'minecraft:block/cube',
        'textures': {
            'particle': 'combinator:block/combiner_table_side',
            'down': 'combinator:block/combiner_table_bottom',
            'up': 'combinator:block/combiner_table_top',
            'north': 'combinator:block/combiner_table_front',
            'south': 'combinator:block/combiner_table_front',
            'east': 'combinator:block/combiner_table_side',
            'west': 'combinator:block/combiner_table_side'}})
    wjson(f'{ASSETS}/models/item/combiner_table.json', {'parent': 'combinator:block/combiner_table'})

    # ---- data
    wjson(f'{DATA}/combinator/recipe/combiner_table.json', {
        'type': 'minecraft:crafting_shaped', 'category': 'misc',
        'pattern': ['IDI', 'PCP', 'PPP'],
        'key': {'I': {'item': 'minecraft:iron_ingot'}, 'D': {'item': 'minecraft:diamond'},
                'P': {'tag': 'minecraft:planks'}, 'C': {'item': 'minecraft:crafting_table'}},
        'result': {'id': 'combinator:combiner_table', 'count': 1}})
    wjson(f'{DATA}/combinator/advancement/recipes/combiner_table.json', {
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_crafting_table': {'trigger': 'minecraft:inventory_changed',
                                   'conditions': {'items': [{'items': 'minecraft:crafting_table'}]}},
            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked',
                               'conditions': {'recipe': 'combinator:combiner_table'}}},
        'requirements': [['has_the_recipe', 'has_crafting_table']],
        'rewards': {'recipes': ['combinator:combiner_table']}})
    wjson(f'{DATA}/combinator/loot_table/blocks/combiner_table.json', {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': 'combinator:combiner_table'}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

    def tag(path, values):
        wjson(f'{DATA}/{path}.json', {'replace': False, 'values': values})

    tag('minecraft/tags/block/mineable/axe', ['combinator:combiner_table'])
    tag('combinator/tags/block/mineable/paxel', ['#minecraft:mineable/pickaxe', '#minecraft:mineable/axe',
                                                '#minecraft:mineable/shovel', '#minecraft:mineable/hoe'])
    tag('combinator/tags/block/ores', ['#minecraft:coal_ores', '#minecraft:iron_ores', '#minecraft:copper_ores',
                                      '#minecraft:gold_ores', '#minecraft:redstone_ores', '#minecraft:lapis_ores',
                                      '#minecraft:diamond_ores', '#minecraft:emerald_ores',
                                      'minecraft:nether_quartz_ore', 'minecraft:ancient_debris',
                                      {'id': '#c:ores', 'required': False}])

    by = lambda pred: [f'combinator:{i["id"]}' for i in spec.ITEMS if pred(i)]
    tag('minecraft/tags/item/pickaxes', by(lambda i: i['kind'] in ('pick', 'paxel')))
    tag('minecraft/tags/item/axes', by(lambda i: i['kind'] == 'axe'))
    tag('minecraft/tags/item/shovels', by(lambda i: i['kind'] == 'shovel'))
    tag('minecraft/tags/item/hoes', by(lambda i: i['kind'] == 'hoe'))
    tag('minecraft/tags/item/swords', by(lambda i: i['kind'] == 'sword'))
    tag('minecraft/tags/item/head_armor', by(lambda i: i.get('slot') == 'HELMET'))
    tag('minecraft/tags/item/chest_armor', by(lambda i: i.get('slot') == 'CHESTPLATE' or i['kind'] == 'wings'))
    tag('minecraft/tags/item/leg_armor', by(lambda i: i.get('slot') == 'LEGGINGS'))
    tag('minecraft/tags/item/foot_armor', by(lambda i: i.get('slot') == 'BOOTS'))
    # Staffs and other gadgets with durability accept Unbreaking and Mending.
    tag('minecraft/tags/item/enchantable/durability', by(lambda i: i['kind'] == 'gadget' and i.get('dur')))

# ------------------------------------------------------------------------------------------ docs data

def gen_docs_data(rows, extra):
    items = {it['id']: it for it in spec.ITEMS}
    combos = []
    for a, b, res, cnt, tier, _ in rows:
        e = dict(a=a, b=b, a_name=display(a), b_name=display(b), result=res, result_name=display(res),
                 count=cnt, rar=tier, a_custom=a in spec.CUSTOM_IDS, b_custom=b in spec.CUSTOM_IDS)
        if res in items:
            e['kind'] = items[res]['kind']
            e['tip'] = items[res]['tip']
            e['cat'] = items[res]['cat']
            e['stats'] = stat_text(items[res])
        combos.append(e)
    wjson(HERE + '/combos.json', dict(combos=combos, swaps=spec.NETHERITE_SWAP,
                                               items=[dict(id=i['id'], name=i['name'], rar=i['rar'], kind=i['kind'], cat=i['cat'], a=i['a'], b=i['b'],
                                                           tip=i['tip'], stats={k: i[k] for k in ('dur', 'speed', 'dmg', 'aspd', 'prot', 'tough', 'nut') if k in i})
                                                      for i in spec.ITEMS]))
    return combos


# ------------------------------------------------------------------------------------------ README

def stat_text(it):
    k = it['kind']
    if k in TOOL_KINDS:
        return f'{it["dmg"]:g} damage, {it["dur"]} uses'
    if k in ('armor', 'wings'):
        return f'{it["prot"]} armor, {it["dur"]} uses'
    if k == 'food':
        return f'{it["nut"]} hunger'
    if it.get('dur'):
        return f'{it["dur"]} uses'
    return ''


def depth(i, items):
    """How many combining steps deep an item is. Vanilla items are 0."""
    if i not in items or items[i]['a'] is None:
        return 0
    return 1 + max(depth(items[i]['a'], items), depth(items[i]['b'], items))


def gen_readme(rows, extra):
    items = {it['id']: it for it in spec.ITEMS}
    n_new = sum(1 for r in rows if r[4] > 0)
    n_van = sum(1 for r in rows if r[4] == 0)
    L = []
    A = L.append
    A(f'# {spec.MOD_NAME}\n')
    A('A Fabric mod for **Minecraft Java Edition 1.21.1**.\n')
    A('It adds one block, the **Combiner Table**. You put two items into it and get one new item.\n')
    A(f'- **{len(rows)} combinations** in total.')
    A(f'- {n_new} of them make one of **{len(spec.ITEMS) - 1} new items**: tools, weapons, armor, gadgets, food, bombs, cheats.')
    A(f'- {n_van} of them make a vanilla item that you normally cannot craft (saddle, name tag, elytra, ...).')
    A('- Two stackable items that have no combination give a **Chaos Orb**, which does something random.')
    A('- There are no tiers and there is no balance. Some items are normal, many are absurd, a few can destroy your world.\n')
    A('## Status: read this first\n')
    A('The mod **compiles and passes its automatic tests**. GitHub runs the tests after every change, also against')
    A('the finished .jar, started the same way a player starts it. See "Automatic tests" near the end of this page.\n')
    A('**No person has played it yet.** The tests prove that the game starts, that every combination works, and that')
    A('every ability does its main job without an error. They do not prove that it is fun, that it runs well on a')
    A('slow PC, or that it works together with other mods. Expect smaller bugs.\n')
    A('**Make a backup of your world before you use the bombs.** Nukes, black holes, meteors, the Blast Pickaxe and the')
    A('World Eater delete large parts of the world for good.\n')
    A('## What you need\n')
    A('- Minecraft Java Edition **1.21.1**')
    A('- [Fabric Loader](https://fabricmc.net/use/installer/) (the program that loads mods into the game)')
    A('- [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.1 (a helper mod most Fabric mods need)\n')
    A('## Getting the .jar file\n')
    A('The .jar is the file the game loads. It is built from this project.\n')
    A('**From GitHub (no installs on your PC):** GitHub builds the .jar again after every change')
    A('(the file `.github/workflows/build.yml` tells it how). The newest .jar is on the **`builds`** branch of the')
    A('repository, next to `build.log` (the text the build printed). You can also open **Actions**, click the newest run,')
    A('and download **item-combinator-jar** at the bottom of the page.\n')
    A('**On your own PC:** install a Java JDK, version 25 (the version the official Fabric template builds with). Then run `gradlew.bat build` (Windows) or')
    A('`./gradlew build` (Linux, Mac) in this folder. The .jar appears in `build/libs/`. The first build downloads')
    A('about 1 GB and takes several minutes. `build` also runs the server tests. To skip them: `gradlew build -x runGameTest`.\n')
    A('## Installing\n')
    A(f'Put `item-combinator-{spec.VERSION}.jar` and the Fabric API .jar into the `mods` folder of your Minecraft installation.')
    A('Start the game with the Fabric profile.\n')
    A('## How the Combiner Table works\n')
    A('Craft it in a normal crafting table:\n')
    A('| | | |\n|---|---|---|')
    A('| Iron Ingot | Diamond | Iron Ingot |')
    A('| Any Planks | Crafting Table | Any Planks |')
    A('| Any Planks | Any Planks | Any Planks |\n')
    A('Place it and right-click it. It has two input slots and one output slot.\n')
    A('- Put one item in each input slot. If the two items combine, the result shows up on the right.')
    A('- The order does not matter. A + B gives the same as B + A.')
    A('- Taking the result uses up **one item from each slot**. Combining costs nothing else.')
    A('- Shift-click the result to combine as many as you have.')
    A('- **Built-in recipe help:** put an item in only one slot. A panel on the right shows every item it combines with.')
    A('  Move the mouse over one of them to see what you would get.')
    A('- **Chaos Orb:** if both items are stackable and have no combination, the result is a Chaos Orb. Look at the result')
    A('  before you take it. Tools, weapons and armor do not stack, so they are never turned into a Chaos Orb.')
    A('- The table does not store items. They come back to you when you close it.')
    A('- The result is always brand new, even if the inputs were damaged.')
    A('- Enchantments on the inputs are kept if they fit the result. If both inputs have the same enchantment, the higher level wins.')
    A('- Netherite tools and armor work wherever a recipe asks for the diamond version. The result is the same item.')
    A('- A honey bottle gives the empty bottle back.\n')
    A('The name colour of a new item shows how wild it is: yellow = normal, aqua = strong, purple and glowing = absurd.')
    A('Every new item has a description under its name. All new items are also in their own creative tab.\n')
    A('## All combinations that make new items\n')
    A('"Uses" means durability. Names in *italics* are items from this mod, so you have to combine them first.\n')

    def nm(i):
        return f'*{display(i)}*' if i in spec.CUSTOM_IDS else display(i)

    for sec in spec.SECTIONS:
        A(f'### {sec}\n')
        A('| Item A | Item B | Result | What it does | Stats |')
        A('|---|---|---|---|---|')
        for it in spec.ITEMS:
            if it['cat'] != sec: continue
            res = f'**{it["name"]}**' + (f' x{it["count"]}' if it['count'] > 1 else '')
            if it['a'] is None:
                A(f'| Any stackable item | Any other stackable item, if the two have no combination | {res} | {" ".join(it["tip"])} | {stat_text(it)} |')
                continue
            pairs = [(it['a'], it['b'])] + list(it['alt'])
            for n, (a, b) in enumerate(pairs):
                A(f'| {nm(a)} | {nm(b)} | {res} | {" ".join(it["tip"]) if n == 0 else "Second way to make it."} | {stat_text(it)} |')
        A('')
    A('## All combinations that make vanilla items\n')
    A('| Item A | Item B | Result |')
    A('|---|---|---|')
    for v in spec.VANILLA:
        A(f'| {display(v["a"])} | {display(v["b"])} | **{display(v["result"])}**' + (f' x{v["count"]}' if v['count'] > 1 else '') + ' |')
    A('')
    A('## What the Chaos Orb can do\n')
    A('One of these, picked at random: rain of diamonds, rain of gold, full heal, 10 levels, a random item from this mod,')
    A('rain of chickens, a launch into the sky, a thunderstorm, every mob nearby becomes a farm animal, rain of anvils,')
    A('mobs float away, half a day passes, a random teleport, iron golems, an explosion, creepers, falling TNT, sickness,')
    A('a black hole, you grow, golden apples, sudden winter, the ground turns to gold, time stops.')
    A('**Pandora\'s Box** (two Chaos Orbs) does six of them in a row.\n')
    A('## The longest chains\n')
    A('These items need three or more combining steps. Each list shows everything that goes into one.\n')
    def tree(i, d=0):
        if i not in items or items[i]['a'] is None:
            return [('  ' * d) + '- ' + display(i)]
        it = items[i]
        return [('  ' * d) + f'- **{it["name"]}**'] + tree(it['a'], d + 1) + tree(it['b'], d + 1)
    for it in spec.ITEMS:
        if depth(it['id'], items) >= 3:
            L.extend(tree(it['id']))
            A('')
    A('## Things to know\n')
    A('- **Sneak** while mining with an area tool, vein tool, lumber axe or the Blast Pickaxe to break only one block.')
    A('- Area tools skip chests, furnaces and other blocks that hold things. They also skip blocks that are much harder')
    A('  than the block you mined (mining stone does not break obsidian next to it).')
    A('- The Lumber Axe and Deforester only fell natural trees: the logs must touch leaves that grew there. Log houses are safe.')
    A('- Charms and body upgrades work from the hotbar or the off hand. Armor works when worn.')
    A('- The Magnet also pulls back items you just dropped, about 2 seconds after you drop them.')
    A('- Mob heads only drop from zombies, skeletons, creepers, wither skeletons and piglins.')
    A('- The Midas Glove, Polymorph Wand and Size Ray do not work on players. The Midas Glove does not work on the Wither or the Ender Dragon.')
    A('- Mega Dynamite, the Pocket Nuke, the Doomsday Device and the Singularity hurt you too. Throw and run.')
    A('- Nukes and black holes delete blocks without dropping them. Bedrock stays.')
    A('- The Time Stopper uses the game\'s own "tick freeze". Mobs you kill while time stands still fall over when it moves again.')
    A('- The Ghost Cloak puts you in spectator mode for 8 seconds. If you are inside a wall when it ends, you suffocate.')
    A('- The Size Ray change on a mob is permanent.')
    A('- Worn armor from this mod looks like vanilla iron, diamond or netherite armor. Only the inventory picture is new.')
    A('- Wings use the vanilla elytra look.')
    A('- There are no new bows, crossbows, tridents or shields.')
    A('- Texts are in English only.\n')
    A('## Automatic tests\n')
    A('The tests are in `src/gametest`. They are not part of the mod .jar.\n')
    A('**Server tests** (`gradlew runGameTest`). A server starts without a window. A fake player in survival mode then:\n')
    A('- makes every combination, in both orders, and checks the result')
    A('- uses the Combiner Table: one click, shift-click, closing with items inside')
    A('- mines with every tool and counts the broken blocks and the drops (area, vein, tree, smelting, double ores, magnet)')
    A('- hits and kills zombies with every item and checks fire, effects, life steal, area damage, instant kill and loot')
    A('- wears every armor piece and takes a zombie hit and a fall (thorns, fall protection), and takes a deadly hit')
    A('  while holding the totem')
    A('- eats every food and checks hunger, size change and launch')
    A('- holds or wears every item with a passive ability and checks effects, body changes, flying and repair,')
    A('  then takes the item away and checks that everything goes back to normal')
    A('- right-clicks with every item, normal and sneaking, and checks what happened (a fireball exists, the pig in front')
    A('  was hurt, the tester moved, a screen opened, the item wore out, the cooldown started ...)')
    A('- waits for the slow abilities and checks them too (nuke crater, black hole, meteors, anvils, chickens, the')
    A('  portable hole closes again, the time stop and the ghost cloak end by themselves)')
    A('- runs every event of the Chaos Orb\n')
    A('**Client test** (`gradlew runClientTest`). The real game starts, makes a flat world, opens the Combiner Table with a')
    A('right-click, puts two items in, takes the result, and takes screenshots of the table, of every item in the')
    A('inventory, and of armor and wings on the player. It checks that every item has a name, a description, a model')
    A('and a texture.\n')
    A('**The same tests with the finished .jar** (`gradlew prodServerTest` and `gradlew prodClientTest`). Here the game')
    A('runs like on a player\'s PC: with the .jar from `build/libs` and the Fabric API file that players install.\n')
    A('On GitHub the logs and the screenshots of the last run are on the **`builds`** branch.\n')
    A('What the tests do **not** check: flying with the wings, sounds and particles, how strong or fair an item feels,')
    A('speed on a slow PC, multiplayer with real players, the Nether and the End, and other mods.\n')
    A('## Changing the mod\n')
    A('Everything about the items and recipes is in one table: `tools/spec.py`. After changing it, run')
    A('`python tools/gen.py` and `python tools/textures.py` (needs Python 3 and the Pillow package). They rewrite the')
    A('generated Java files, the models, the names and the textures. The ability code is in')
    A('`src/main/java/com/combinator/ability/`.\n')
    A('## License\n')
    A('MIT. All textures were drawn for this mod. No Minecraft assets are included.')
    write(ROOT + '/README.md', '\n'.join(L) + '\n')


if __name__ == '__main__':
    gen_items_java()
    rows, extra = gen_recipes_java()
    gen_resources()
    combos = gen_docs_data(rows, extra)
    gen_readme(rows, extra)
    print(f'{len(spec.ITEMS)} items, {len(rows)} combinations (+{len(extra)} netherite alternates)')
