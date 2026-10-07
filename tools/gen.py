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
HANDHELD_TEX = {'wand', 'staff', 'staff3', 'scythe', 'bat', 'bow', 'hook', 'lasso', 'rod', 'crook', 'net', 'fork', 'cane', 'bigshears', 'boomerang', 'key', 'shovel', 'mjolnir'}

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
        if it.get('bottle'): b += '.usingConvertsTo(Items.GLASS_BOTTLE)'
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
        'death.attack.combinator.vacuum': '%1$s ran out of air on the Moon',
        'death.attack.combinator.vacuum.player': '%1$s ran out of air on the Moon while fighting %2$s',
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
    A('**Make a backup of your world before you use the bombs.** Nukes, black holes, meteors, the Blast Pickaxe, the')
    A('World Eater, the Armageddon Clock, the Orbital Strike Remote, the Tornado, the Termite Jar, the Pocket Volcano, the Bedrock Breaker')
    A('and all the cataclysms (Chunk Eraser, Tsar Bomba ...)')
    A('delete or change large parts of the world for good.\n')
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
    A('- The Volley Bow and the Torch Bow are not drawn like a normal bow: one right-click is one shot.')
    A('  There are no new crossbows, tridents or shields.')
    A('- **Backpacks:** only the backpack in your hand opens. A backpack cannot go into a backpack or a shulker box, and')
    A('  a shulker box cannot go into a backpack. If a backpack burns on the ground, the things inside fall out.')
    A('- The Mob Net takes every mob except the Ender Dragon and the Wither. The mob keeps its health, name, trades and')
    A('  what it carries. The net wears out when the mob comes out, so it never breaks with a mob inside.')
    A('- A Waystone remembers one spot. It also brings you back from the Nether or the End.')
    A('- The Bedroll sets your respawn point without a bed. If the spot is blocked when you die, you wake up at the world spawn.')
    A('- The Planter\'s Hoe plants wheat seeds, carrots, potatoes or beetroot seeds from your inventory.')
    A('- The Angler\'s Rod catches fish and junk, but no treasure. For treasure you need a normal fishing rod.')
    A('- The Tome of Chance can give any enchantment, also Mending, also a curse. The books are weaker than the best')
    A('  books of an enchanting table.')
    A('- Bleeding from the Jagged Blade does not add up. A new hit only starts the 6 seconds again.')
    A('- The Divining Rod shows at most 96 ore blocks at a time.')
    A('- The Endless Water Bucket does not work in the Nether, like a normal water bucket.')
    A('- **Double jump, Pogo Stick and Sticky Boots** are worked out by your own game, not by the server (like all player')
    A('  movement). They are made for single player. On a server without this mod they do nothing.')
    A('- Sticky Boots: you only climb while you walk against the wall. Let go of the forward key and you fall.')
    A('- The Firefly Jar uses the game\'s invisible light block. It moves with you and is removed when you put the jar away,')
    A('  leave the world or die. If the game crashes, one invisible light can stay behind.')
    A('- The Strider Boots only harden still lava (not flowing lava), and only while you stand on something. Jumping into')
    A('  a lava lake from above still ends badly.')
    A('- The Rewind Watch gives back health, not items. It does not undo anything you did to the world.')
    A('- The Disco Ball does not work on the Wither and the Ender Dragon. A creeper that was about to explode calms down.')
    A('- The Push Glove moves what a piston can move: no obsidian, no chests, no doors. A block with a window of its own')
    A('  (crafting table, furnace) can only be pulled, because a normal right-click opens it.')
    A('- The Rodeo Saddle lets you sit on a mob. The mob goes where it wants. Hostile mobs still attack you.')
    A('- The Lunchbox never eats food with side effects: no golden apples, no rotten flesh, none of the special foods.')
    A('- The Pocket Mirror stops arrows, tridents, fireballs and other shots, one every 3 seconds. A ghast fireball still')
    A('  explodes next to you.')
    A('- The Magnifying Glass needs the sun: daytime, no rain, open sky above you.')
    A('- **Armageddon Clock:** it takes about 50 seconds from the right-click to the end. It happens around the spot where')
    A('  you stood, not around you, so you can run. Only the player who started it can call it off (sneak + right-click')
    A('  with the clock). If the world is closed in the middle, it stops. Everybody within 160 blocks sees the countdown.')
    A('- The Orbital Strike, the Termite Jar, the Solar Death Ray, the Tornado and the Railgun do not drop the blocks they')
    A('  destroy. Bedrock and other unbreakable blocks stay. Obsidian also stops the Railgun and the Death Ray.')
    A('- The Termite Jar eats everything an axe mines fastest (logs, planks, doors, fences, crafting tables, pumpkins ...),')
    A('  but no chests or other blocks that hold things. It stops after 1500 blocks.')
    A('- The Tsunami Horn and the Staff of the Red Sea leave no water behind. The Red Sea comes back after 20 seconds,')
    A('  also over anything you put in its place. In the Nether the tsunami boils away into steam.')
    A('- The Tornado also throws you around if you get too close. You take fall damage when it lets you go.')
    A('- The Floor Is Lava puts the ground back after 8 seconds. If the world is closed in the middle, the lava stays.')
    A('- The Hot Potato counts down while it is anywhere in your inventory. Put it in a chest to stop the countdown.')
    A('  A thrown potato that somebody picks up starts counting for them.')
    A('- The Snap Gauntlet never takes players or the Ender Dragon. It does take the Wither, villagers and pets.')
    A('- The Mitosis Ray copies everything about a mob: health, name, trades, what it holds. It also copies the Wither.')
    A('- The Menagerie Cannon and the Kaiju Egg spawn real mobs. Some of them (a warden, a ghast, a ravager) can kill you.')
    A('- The Portal Gun makes real portals. The End portal leads to the End from anywhere, and back home from the End.')
    A('- Magic Beans: the beanstalk is 64 blocks high. Climb the vines on its sides.')
    A('  The giant on the cloud does not move. Do not ask why.')
    A('- The Pharaoh\'s Scarab trap is real: the pressure plate in the treasure chamber sets off TNT under the floor.')
    A('- The Staff of Wild Magic picks from every right-click ability of this mod, the Armageddon Clock and the Pocket')
    A('  Nuke included. It never picks an ability that needs a special item in your hand (backpack, waystone ...).')
    A('- The Dice of Fate is fair: every number from 1 to 20 is equally likely. 1 spawns a real Wither.')
    A('- The Gremlin in a Jar plays a prank about every 20 seconds while it is in your hotbar or off hand.')
    A('- The Upside-Down Cake turns your gravity around for 6 seconds. Under a roof you stand on the ceiling.')
    A('  Slow Falling lasts 45 seconds, so the way back down is safe.')
    A('- The Storm Crown\'s lightning only looks real: it never hits you, and it starts no fires.')
    A('- The Plague Mask also makes villagers and other friendly animals sick. Only tame pets are spared.')
    A('- The Bedrock Breaker breaks bedrock, barriers and other unbreakable blocks, but nothing that holds things')
    A('  (no End portals, no command blocks). Breaking the bottom of the world opens the void. Each block costs 10 durability.')
    A('- The Copy-Paste Wand copies blocks, but not chests or other blocks that hold things, and no mobs.')
    A('  It copies what is there at the moment you paste, so you can copy a building that changed since you marked it.')
    A('- The Hourglass of Ages changes the game rule randomTickSpeed for 10 seconds, for the whole world.')
    A('  If the world is closed in the middle, the old value is put back first.')
    A('- The Horde Horn and the Monster Magnet bring real monsters. Use them at night only if you are brave.')
    A('- **Cataclysms** (Chunk Eraser, Region Eraser, Chunk Inverter, Chunk Launcher, Carpet Bomber, Orbital Annihilator,')
    A('  Tsar Bomba, Event Horizon, Fault Line Spike) change hundreds of thousands of blocks. Expect the game to slow down')
    A('  for a few seconds, more on a slow PC. They only touch loaded chunks. Nothing they erase drops.')
    A('- The Chunk Eraser and the Region Eraser leave the bedrock. The Orbital Annihilator does not: it opens the void.')
    A('  It spares command blocks, structure blocks and End gateways.')
    A('- The Tsar Bomba erases everything within 64 blocks of where the missile lands (the Doomsday Device: 32).')
    A('  Living things up to 96 blocks away get fallout: Wither, Weakness and Nausea. Then it rains, and the edge burns.')
    A('- The Chunk Inverter and the Chunk Launcher leave unbreakable blocks and blocks that hold things (chests ...)')
    A('  where they are. The Chunk Launcher gives everything standing on the chunk Slow Falling, so it lands softly.')
    A('- The Staff of Wild Magic never casts the cataclysms, and none of the items of version 1.5 either.')
    A('- **The five worlds of this mod** are new dimensions: the Pocket Dimension (Pocket Dimension Cube), the Backrooms')
    A('  (Noclip Pearl), the Sky Realm (Cloud Key), the Moon (Moon Rocket) and the Other Side (Looking Glass). They exist')
    A('  in worlds made with version 1.5 or later. In an older world the items say that the world is missing.')
    A('- Each of these items takes you to its world. Used in its own world, it brings you back to where you left from.')
    A('  That spot is remembered, also when you close the game or die. Without it you go to your spawn point.')
    A('- Every player has their own room in the Pocket Dimension. It is built the first time, then it stays as you left it.')
    A('  Sneaking takes along everything alive and every dropped item within 4 blocks, other players too.')
    A('  The lodestone in the corner of the room is a way out. Who falls out of the room lands back in it.')
    A('- A thrown Noclip Pearl takes you where it lands. A mob or player it hits comes along, without asking. Used in the')
    A('  Backrooms, the thrown pearl brings you both home. The pearl is never used up.')
    A('- The Backrooms have no end. The rooms are made as you walk. A lodestone under a green light, about one in 40 rooms,')
    A('  is a way out. Smilers are invisible endermen: only their eyes can be seen. Looking at them makes them angry.')
    A('  Smilers never leave the Backrooms: a thrown Noclip Pearl, the Pocket Dimension Cube and wormholes do not take them')
    A('  along, and one that shows up in another world (out of a Mob Net, say) fades away.')
    A('- Who falls off the Sky Realm falls into their own world from high up, with Slow Falling.')
    A('- On the Moon everything alive weighs a sixth, so you jump about 5 blocks high, and falls do not hurt.')
    A('  There is no air: without an Oxygen Helmet (the old Diving Helmet) or a Spelunker\'s Helmet on your head, the air')
    A('  bubbles go down as under water, and then it hurts until you die or fly home. Creative mode needs no helmet.')
    A('  Some craters have a meteorite: iron, gold, diamonds, emeralds or ancient debris inside.')
    A('- The Other Side is a second copy of the land of the world: the same seed, so the same mountains, caves, villages')
    A('  and treasure. Nothing you change in your world changes there. The sun stays at dusk, so monsters come out.')
    A('- The Dimension Hopper goes through every world of the game in turn, those of other mods too. In the Nether every')
    A('  block counts as 8, as with Nether portals. Over the void of the End it builds a small obsidian platform.')
    A('- The Wormhole Gun\'s two ends stay open until you shoot new ones, or until the world is closed.')
    A('  Mobs, players, dropped items, arrows, minecarts, falling blocks and lit TNT go through.')
    A('- The Architect\'s Blueprint and the Biome Brush use the game\'s own /place and /fillbiome commands, so they work')
    A('  without cheats switched on. Some structures do not fit everywhere: a mansion needs high ground, for example.')
    A('  Then nothing is built and the blueprint does not wear out. The biome is painted from the bottom to the top of')
    A('  the world. Some changes (grass colour) only show after the chunk is loaded again.')
    A('- The Quarry in a Box keeps everything except common stone, dirt, sand, gravel and the like. Water and lava in')
    A('  the pit are removed. When its 6 chests are full, the rest drops next to them. The box is used up.')
    A('- The Builder\'s Wand and the Sorting Wand have the usual tool durability and can be repaired and enchanted.')
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
    A('- runs every event of the Chaos Orb, and its random teleport also in the Nether')
    A('- fills, closes, opens and merges backpacks, and tries to put a backpack and a shulker box into a backpack')
    A('- remembers a spot with the Waystone and travels back to it')
    A('- catches a pig with the Mob Net (sent the way a real game client sends a right-click) and lets it out again')
    A('- kills a sheep with a normal sword and with the Looter\'s Blade and compares the wool')
    A('- hits an iron golem twice with the Jagged Blade and measures the bleeding for 4 seconds')
    A('- wears, holds and carries the odd charms for 20 seconds: balloon, light, lunchbox, flower boots, lava boots,')
    A('  and goes 5 seconds back in time with the Rewind Watch')
    A('- lets a skeleton arrow bounce off the Pocket Mirror, fills and empties the Piggy Bank, rides a cow')
    A('- runs the whole Armageddon from countdown to the final blast, and checks that the stage is gone')
    A('- erases, flips and launches chunks, carpet bombs, drills to the void, and drops a Tsar Bomba on the stage')
    A('- waits for every bomb, the tornado, the volcano, the tsunami, the termites, the castle, the pyramid, the island,')
    A('  the beanstalk, the mountain and the parted sea, and checks what each one left behind')
    A('- copies a block with the Copy-Paste Wand and pastes it, breaks bedrock with the Bedrock Breaker')
    A('- rolls every number of the Dice of Fate, plays every prank of the gremlin, carries the Hot Potato until it')
    A('  explodes, and wears the Storm Crown and the Plague Mask next to a zombie')
    A('- sends a pig through a wormhole, sends mail and visits a second fake player with the Friendship Bracelet,')
    A('  calls a tame wolf with the Pet Whistle, sorts a chest, digs a quarry and counts the ores in its chest\n')
    A('The game\'s test server only makes the Overworld, the Nether and the End. The five worlds of the mod are tested')
    A('by the client test below.\n')
    A('**Client test** (`gradlew runClientTest`). The real game starts, makes a flat world, opens the Combiner Table with a')
    A('right-click, puts two items in, takes the result, and takes screenshots of the table, of every item in the')
    A('inventory, and of armor and wings on the player. It checks that every item has a name, a description, a model')
    A('and a texture. It also presses the jump and forward keys (pretend key presses) and measures how high the')
    A('player gets with the double jump, the Pogo Stick and the Sticky Boots. Then it travels through the five worlds of')
    A('the mod and takes a screenshot in each: it leaves the pocket room over the lodestone, checks the walls, lamps')
    A('and Smilers of the Backrooms, falls off the Sky Realm, jumps on the Moon, visits the Other Side, hops through all')
    A('eight worlds with the Dimension Hopper and banishes a pig to the Backrooms.\n')
    A('**The same tests with the finished .jar** (`gradlew prodServerTest` and `gradlew prodClientTest`). Here the game')
    A('runs like on a player\'s PC: with the .jar from `build/libs` and the Fabric API file that players install.\n')
    A('On GitHub the logs and the screenshots of the last run are on the **`builds`** branch.\n')
    A('What the tests do **not** check: flying with the wings, sounds and particles, how strong or fair an item feels,')
    A('speed on a slow PC, multiplayer with real players, most abilities in the Nether and the End, and other mods.\n')
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
