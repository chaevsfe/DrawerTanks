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
    img = side_img.copy().convert("RGBA")
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = darken(px[x, y], 0.30)
    return img


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

    lang["itemGroup.drawertanks"] = "Drawer Tanks"
    lang["tooltip.drawertanks.capacity"] = "Capacity: %s B"
    lang["tooltip.drawertanks.contents"] = "%s: %s / %s B"
    lang["tooltip.drawertanks.empty"] = "Empty"
    write_json(os.path.join(a, "lang/en_us.json"), dict(sorted(lang.items())))

    write_json(os.path.join(dt_res, "data/minecraft/tags/block/mineable/axe.json"),
               {"replace": False, "values": [f"drawertanks:{w}_tank" for w in WOODS]})

    print(f"generated assets for {len(WOODS)} woods")


if __name__ == "__main__":
    main()
