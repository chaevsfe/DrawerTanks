#!/usr/bin/env python3
# Generates Drawer Tanks assets derived from Storage Drawers textures.
# Usage: gen_assets.py <path-to-storagedrawers-resources> <path-to-drawertanks-common-resources>

import json
import os
import sys
from PIL import Image

WOODS = ["acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak",
         "jungle", "mangrove", "oak", "pale_oak", "spruce", "warped"]

# window opening in the 16x16 front face, inclusive pixel bounds
WIN_MIN = 3
WIN_MAX = 12


def darken(px, f):
    return (int(px[0] * f), int(px[1] * f), int(px[2] * f), px[3])


def make_front(side_img):
    img = side_img.copy().convert("RGBA")
    px = img.load()
    for y in range(WIN_MIN - 1, WIN_MAX + 2):
        for x in range(WIN_MIN - 1, WIN_MAX + 2):
            inner = WIN_MIN <= x <= WIN_MAX and WIN_MIN <= y <= WIN_MAX
            corner = (x in (WIN_MIN, WIN_MAX)) and (y in (WIN_MIN, WIN_MAX))
            if inner and not corner:
                px[x, y] = (0, 0, 0, 0)
            elif corner:
                px[x, y] = darken(px[x, y], 0.55)
            else:
                px[x, y] = darken(px[x, y], 0.45)
    return img


def make_interior(side_img):
    # neutral gray vat interior so dark fluids like oil stay visible
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 112 + ((x * 31 + y * 17) % 5) * 5
            px[x, y] = (v, v, v + 3, 255)
    return img


def tint(px, f, add):
    return (min(255, int(px[0] * f) + add[0]), min(255, int(px[1] * f) + add[1]),
            min(255, int(px[2] * f) + add[2]), px[3])


FRAME_OUTER = (47, 79, 71, 255)
FRAME_INNER = (28, 52, 47, 255)
FRAME_KNOB = (87, 130, 122, 255)


def ender_body(x, y):
    # wavy vertical streaks over a near-black green-gray, like the ender chest body
    n = (x * 13 + y * 29 + x * x * (y + 3)) % 11
    if n < 2:
        return (25, 42, 40, 255)
    if n < 4:
        return (17, 28, 28, 255)
    return (12, 19, 20, 255)


def make_linked_side(_side_img):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = ender_body(x, y)
    for i in range(16):
        px[i, 0] = FRAME_OUTER
        px[i, 15] = FRAME_OUTER
        px[0, i] = FRAME_OUTER
        px[15, i] = FRAME_OUTER
    for i in range(1, 15):
        px[i, 1] = FRAME_INNER
        px[i, 14] = FRAME_INNER
        px[1, i] = FRAME_INNER
        px[14, i] = FRAME_INNER
    for x, y in [(0, 0), (15, 0), (0, 15), (15, 15)]:
        px[x, y] = FRAME_KNOB
    return img


def make_linked_front(side_img):
    img = make_linked_side(side_img)
    px = img.load()
    for y in range(WIN_MIN - 1, WIN_MAX + 2):
        for x in range(WIN_MIN - 1, WIN_MAX + 2):
            inner = WIN_MIN <= x <= WIN_MAX and WIN_MIN <= y <= WIN_MAX
            corner = (x in (WIN_MIN, WIN_MAX)) and (y in (WIN_MIN, WIN_MAX))
            if inner and not corner:
                px[x, y] = (0, 0, 0, 0)
            elif corner:
                px[x, y] = FRAME_INNER
            else:
                px[x, y] = FRAME_OUTER
    # eye-of-ender latch above the window
    px[7, 0] = (47, 138, 94, 255)
    px[8, 0] = (47, 138, 94, 255)
    px[7, 1] = (126, 230, 170, 255)
    px[8, 1] = (30, 66, 48, 255)
    return img


def make_coupler():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # blaze rod shaft, 2px wide with highlight and shadow, gold bands
    for i in range(9):
        x, y = 2 + i, 14 - i
        px[x, y] = (232, 190, 92, 255)
        px[x + 1, y] = (176, 118, 40, 255)
        if x + 2 <= 15 and i < 8:
            px[x + 2, y] = (110, 70, 24, 255)
    for i in (1, 4):
        x, y = 2 + i, 14 - i
        px[x, y] = (255, 232, 160, 255)
        px[x + 1, y] = (216, 162, 60, 255)
    # iron collar between rod and eye
    for x, y in [(10, 5), (11, 5), (10, 6), (11, 4)]:
        px[x, y] = (168, 168, 176, 255)
    px[11, 5] = (120, 120, 128, 255)
    # ender eye head: green orb with dark pupil and white glint
    eye = {(12, 1): (46, 120, 86), (13, 1): (58, 150, 104),
           (11, 2): (58, 150, 104), (12, 2): (120, 232, 170), (13, 2): (90, 200, 140), (14, 2): (46, 120, 86),
           (11, 3): (90, 200, 140), (12, 3): (22, 46, 34), (13, 3): (120, 232, 170), (14, 3): (58, 150, 104),
           (12, 4): (58, 150, 104), (13, 4): (46, 120, 86)}
    for (x, y), c in eye.items():
        px[x, y] = (*c, 255)
    px[13, 2] = (235, 255, 240, 255)
    # ender sparkles
    for x, y in [(9, 1), (15, 5), (8, 8)]:
        px[x, y] = (186, 108, 234, 255)
    return img


def make_gui(sd_res):
    img = Image.open(os.path.join(sd_res, "assets/storagedrawers/textures/gui/drawers_1.png")).convert("RGBA")
    px = img.load()
    panel = (198, 198, 198, 255)
    for y in range(16, 74):
        for x in range(8, 168):
            px[x, y] = panel
    # gauge frame: 1px border around a light gray 16x56 interior at (80,18), BC-style
    for y in range(17, 75):
        for x in range(79, 97):
            inner = 80 <= x <= 95 and 18 <= y <= 73
            px[x, y] = (148, 148, 150, 255) if inner else (85, 85, 85, 255)
    return img


def linked_block_model():
    model = block_model("dark_oak")
    model["textures"] = {
        "particle": "drawertanks:block/linked_tank_side",
        "side": "drawertanks:block/linked_tank_side",
        "front": "drawertanks:block/linked_tank_front",
        "interior": "drawertanks:block/tank_interior"
    }
    return model


def block_model(wood):
    side = f"storagedrawers:block/drawers_{wood}_side"
    return {
        "parent": "block/block",
        "textures": {
            "particle": side,
            "side": side,
            "front": f"drawertanks:block/tank_{wood}_front",
            "interior": "drawertanks:block/tank_interior"
        },
        "elements": [
            {
                "from": [0, 0, 0], "to": [16, 16, 16],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#front", "cullface": "north"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "south"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "east"},
                    "west": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "west"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "up"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#side", "cullface": "down"}
                }
            },
            {
                "from": [3, 3, 3], "to": [13, 13, 3],
                "faces": {
                    "north": {"uv": [3, 3, 13, 13], "texture": "#interior"}
                }
            },
            {
                "from": [3, 3, 0], "to": [3, 13, 3],
                "faces": {
                    "east": {"uv": [13, 3, 16, 13], "texture": "#interior"}
                }
            },
            {
                "from": [13, 3, 0], "to": [13, 13, 3],
                "faces": {
                    "west": {"uv": [0, 3, 3, 13], "texture": "#interior"}
                }
            },
            {
                "from": [3, 3, 0], "to": [13, 3, 3],
                "faces": {
                    "up": {"uv": [3, 0, 13, 3], "texture": "#interior"}
                }
            },
            {
                "from": [3, 13, 0], "to": [13, 13, 3],
                "faces": {
                    "down": {"uv": [3, 13, 13, 16], "texture": "#interior"}
                }
            }
        ]
    }


def blockstate(wood):
    m = f"drawertanks:block/tank_{wood}"
    return {"variants": {
        "facing=north": {"model": m},
        "facing=east": {"model": m, "y": 90},
        "facing=south": {"model": m, "y": 180},
        "facing=west": {"model": m, "y": 270}
    }}


def item_def(wood):
    return {"model": {"type": "minecraft:model", "model": f"drawertanks:block/tank_{wood}"}}


def recipe(wood):
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": ["///", "GBG", "///"],
        "key": {
            "/": f"minecraft:{wood}_planks",
            "G": "minecraft:glass",
            "B": "minecraft:bucket"
        },
        "result": {"id": f"drawertanks:{wood}_tank", "count": 1}
    }


def loot_table(wood):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [
                    {
                        "type": "minecraft:item",
                        "name": f"drawertanks:{wood}_tank",
                        "functions": [
                            {
                                "function": "minecraft:copy_components",
                                "source": "block_entity",
                                "include": ["drawertanks:tank_contents", "drawertanks:tank_upgrades"]
                            }
                        ]
                    }
                ],
                "rolls": 1.0
            }
        ],
        "random_sequence": f"minecraft:blocks/{wood}_tank"
    }


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=4)
        f.write("\n")


def main():
    sd_res, dt_res = sys.argv[1], sys.argv[2]
    sd_tex = os.path.join(sd_res, "assets/storagedrawers/textures/block")
    a = os.path.join(dt_res, "assets/drawertanks")
    d = os.path.join(dt_res, "data/drawertanks")

    os.makedirs(os.path.join(a, "textures/block"), exist_ok=True)

    oak_side = Image.open(os.path.join(sd_tex, "drawers_oak_side.png"))
    make_interior(oak_side).save(os.path.join(a, "textures/block/tank_interior.png"))

    lang = {}
    for wood in WOODS:
        side = Image.open(os.path.join(sd_tex, f"drawers_{wood}_side.png"))
        make_front(side).save(os.path.join(a, f"textures/block/tank_{wood}_front.png"))
        write_json(os.path.join(a, f"models/block/tank_{wood}.json"), block_model(wood))
        write_json(os.path.join(a, f"blockstates/{wood}_tank.json"), blockstate(wood))
        write_json(os.path.join(a, f"items/{wood}_tank.json"), item_def(wood))
        write_json(os.path.join(d, f"recipe/{wood}_tank.json"), recipe(wood))
        write_json(os.path.join(d, f"loot_table/blocks/{wood}_tank.json"), loot_table(wood))
        pretty = " ".join(w.capitalize() for w in wood.split("_"))
        lang[f"block.drawertanks.{wood}_tank"] = f"{pretty} Tank"

    os.makedirs(os.path.join(a, "textures/gui"), exist_ok=True)
    make_gui(sd_res).save(os.path.join(a, "textures/gui/tank.png"))

    dark_side = Image.open(os.path.join(sd_tex, "drawers_dark_oak_side.png"))
    make_linked_side(dark_side).save(os.path.join(a, "textures/block/linked_tank_side.png"))
    make_linked_front(dark_side).save(os.path.join(a, "textures/block/linked_tank_front.png"))
    os.makedirs(os.path.join(a, "textures/item"), exist_ok=True)
    make_coupler().save(os.path.join(a, "textures/item/tank_coupler.png"))

    m = linked_block_model()
    write_json(os.path.join(a, "models/block/tank_linked.json"), m)
    write_json(os.path.join(a, "blockstates/linked_tank.json"), {"variants": {
        "facing=north": {"model": "drawertanks:block/tank_linked"},
        "facing=east": {"model": "drawertanks:block/tank_linked", "y": 90},
        "facing=south": {"model": "drawertanks:block/tank_linked", "y": 180},
        "facing=west": {"model": "drawertanks:block/tank_linked", "y": 270}
    }})
    write_json(os.path.join(a, "items/linked_tank.json"),
               {"model": {"type": "minecraft:model", "model": "drawertanks:block/tank_linked"}})
    write_json(os.path.join(a, "models/item/tank_coupler.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "drawertanks:item/tank_coupler"}})
    write_json(os.path.join(a, "items/tank_coupler.json"),
               {"model": {"type": "minecraft:model", "model": "drawertanks:item/tank_coupler"}})

    write_json(os.path.join(d, "tags/item/tanks.json"),
               {"replace": False, "values": [f"drawertanks:{w}_tank" for w in WOODS]})
    write_json(os.path.join(d, "recipe/linked_tank.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["OEO", "ETE", "OEO"],
        "key": {"O": "minecraft:obsidian", "E": "minecraft:ender_eye", "T": "#drawertanks:tanks"},
        "result": {"id": "drawertanks:linked_tank", "count": 1}
    })
    write_json(os.path.join(d, "recipe/tank_coupler.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["E", "R", "I"],
        "key": {"E": "minecraft:ender_eye", "R": "minecraft:blaze_rod", "I": "minecraft:iron_ingot"},
        "result": {"id": "drawertanks:tank_coupler", "count": 1}
    })
    write_json(os.path.join(d, "loot_table/blocks/linked_tank.json"), loot_table("linked"))

    lang["block.drawertanks.linked_tank"] = "Linked Tank"
    lang["item.drawertanks.tank_coupler"] = "Tank Coupler"
    lang["message.drawertanks.coupler.source_selected"] = "Source selected, now use the coupler on the tank that should receive the fluid"
    lang["message.drawertanks.coupler.linked"] = "Linked, fluid will now flow from the source into this tank"
    lang["message.drawertanks.coupler.unlinked"] = "Link cleared"
    lang["message.drawertanks.coupler.same_tank"] = "This is the selected source, use the coupler on the receiving tank"

    lang["itemGroup.drawertanks"] = "Drawer Tanks"
    lang["tooltip.drawertanks.capacity"] = "Capacity: %s B"
    lang["tooltip.drawertanks.contents"] = "%s: %s / %s B"
    lang["tooltip.drawertanks.empty"] = "Empty"
    write_json(os.path.join(a, "lang/en_us.json"), dict(sorted(lang.items())))

    write_json(os.path.join(dt_res, "data/minecraft/tags/block/mineable/axe.json"),
               {"replace": False, "values": [f"drawertanks:{w}_tank" for w in WOODS] + ["drawertanks:linked_tank"]})

    print(f"generated assets for {len(WOODS)} woods + linked tank")


if __name__ == "__main__":
    main()
