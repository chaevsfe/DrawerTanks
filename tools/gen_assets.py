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
    # dark wood vat interior
    img = side_img.copy().convert("RGBA")
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = darken(px[x, y], 0.22)
    return img


def tint(px, f, add):
    return (min(255, int(px[0] * f) + add[0]), min(255, int(px[1] * f) + add[1]),
            min(255, int(px[2] * f) + add[2]), px[3])


FRAME_OUTER = (47, 79, 71, 255)
FRAME_INNER = (28, 52, 47, 255)
FRAME_KNOB = (87, 130, 122, 255)

ENDER_TEX = None


def load_ender_tex(mc_assets):
    global ENDER_TEX
    path = os.path.join(mc_assets, "assets/minecraft/textures/entity/chest/ender.png")
    if os.path.exists(path):
        ENDER_TEX = Image.open(path).convert("RGBA")


def pad_to_16(img):
    w, h = img.size
    out = Image.new("RGBA", (16, 16))
    ox = (16 - w) // 2
    oy = (16 - h) // 2
    out.paste(img, (ox, oy))
    px = out.load()
    for y in range(16):
        for x in range(16):
            sx = min(max(x, ox), ox + w - 1)
            sy = min(max(y, oy), oy + h - 1)
            if px[x, y][3] == 0:
                px[x, y] = px[sx, sy]
    return out


def chest_side_16():
    # the chest side as seen in world: lid strip (with the opening seam) over the base
    lid = ENDER_TEX.crop((0, 14, 14, 19))
    base = ENDER_TEX.crop((0, 33, 14, 43))
    face = Image.new("RGBA", (14, 15))
    face.paste(lid, (0, 0))
    face.paste(base, (0, 5))
    return pad_to_16(face)


def chest_top_16():
    # lid top inset in a clean dark border, with subtle mottling so it does not read flat
    lid = ENDER_TEX.crop((14, 0, 28, 14))
    out = Image.new("RGBA", (16, 16), (12, 19, 20, 255))
    out.paste(lid, (1, 1))
    px = out.load()
    for y in range(16):
        for x in range(16):
            n = (x * 7 + y * 13 + x * y) % 9
            p = px[x, y]
            if n == 0:
                px[x, y] = (max(0, p[0] - 6), max(0, p[1] - 9), max(0, p[2] - 9), 255)
            elif n == 4:
                px[x, y] = (min(255, p[0] + 5), min(255, p[1] + 8), min(255, p[2] + 8), 255)
    return out


def ender_body(x, y):
    # wavy vertical streaks over a near-black green-gray, like the ender chest body
    n = (x * 13 + y * 29 + x * x * (y + 3)) % 11
    if n < 2:
        return (25, 42, 40, 255)
    if n < 4:
        return (17, 28, 28, 255)
    return (12, 19, 20, 255)


def make_linked_side(_side_img):
    if ENDER_TEX is not None:
        return chest_side_16()

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

    ring = FRAME_OUTER
    ring_dark = FRAME_INNER
    eye_bright = (126, 230, 170, 255)
    eye_dark = (47, 138, 94, 255)
    if ENDER_TEX is not None:
        # brightest dot of the lid border, so the ring reads like the chest's studded frame
        ring = max((ENDER_TEX.getpixel((x, 0)) for x in range(14, 28)), key=lambda p: p[0] + p[1] + p[2])
        ring_dark = ENDER_TEX.getpixel((2, 36))
        eye_bright = ENDER_TEX.getpixel((49, 36))
        eye_dark = ENDER_TEX.getpixel((47, 38))

    for y in range(WIN_MIN - 1, WIN_MAX + 2):
        for x in range(WIN_MIN - 1, WIN_MAX + 2):
            inner = WIN_MIN <= x <= WIN_MAX and WIN_MIN <= y <= WIN_MAX
            corner = (x in (WIN_MIN, WIN_MAX)) and (y in (WIN_MIN, WIN_MAX))
            if inner and not corner:
                px[x, y] = (0, 0, 0, 0)
            elif corner:
                px[x, y] = ring_dark
            else:
                px[x, y] = ring
    # eye-of-ender latch above the window
    px[7, 0] = eye_dark
    px[8, 0] = eye_dark
    px[7, 1] = eye_bright
    px[8, 1] = eye_dark
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


def framed_block_model():
    model = block_model("oak")
    model["render_type"] = "minecraft:cutout_mipped"
    model["textures"] = {
        "particle": "drawertanks:block/tank_raw_front",
        "front": "drawertanks:block/tank_raw_front",
        "interior": "drawertanks:block/tank_interior"
    }
    # only the window opening itself; the material ring from the meta model owns the border
    model["elements"][0] = {
        "from": [3, 3, 0], "to": [13, 13, 0],
        "faces": {"north": {"uv": [3, 3, 13, 13], "texture": "#front", "cullface": "north"}}
    }
    return model


def meta_tank_sides_model():
    mat = "storagedrawers:block/base/base_oak"

    def cull(face):
        return {"uv": [0, 0, 16, 16], "texture": "#mat", "cullface": face}

    return {
        "render_type": "minecraft:cutout_mipped",
        "textures": {"particle": mat, "mat": mat},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                "south": cull("south"), "east": cull("east"), "west": cull("west"),
                "up": cull("up"), "down": cull("down")}},
            {"from": [0, 13, 0], "to": [16, 16, 0],
             "faces": {"north": {"uv": [0, 0, 16, 3], "texture": "#mat", "cullface": "north"}}},
            {"from": [0, 0, 0], "to": [16, 3, 0],
             "faces": {"north": {"uv": [0, 13, 16, 16], "texture": "#mat", "cullface": "north"}}},
            {"from": [0, 3, 0], "to": [3, 13, 0],
             "faces": {"north": {"uv": [13, 3, 16, 13], "texture": "#mat", "cullface": "north"}}},
            {"from": [13, 3, 0], "to": [16, 13, 0],
             "faces": {"north": {"uv": [0, 3, 3, 13], "texture": "#mat", "cullface": "north"}}}
        ]
    }


def facing_blockstate(model):
    return {"variants": {
        "facing=north": {"model": model},
        "facing=east": {"model": model, "y": 90},
        "facing=south": {"model": model, "y": 180},
        "facing=west": {"model": model, "y": 270}
    }}


def linked_block_model():
    model = block_model("dark_oak")
    model["textures"] = {
        "particle": "drawertanks:block/linked_tank_side",
        "side": "drawertanks:block/linked_tank_side",
        "top": "drawertanks:block/linked_tank_top",
        "front": "drawertanks:block/linked_tank_front",
        "interior": "drawertanks:block/tank_interior"
    }
    for element in model["elements"]:
        for face in ("up", "down"):
            if face in element["faces"] and element["faces"][face]["texture"] == "#side":
                element["faces"][face]["texture"] = "#top"
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

    if len(sys.argv) > 3:
        load_ender_tex(sys.argv[3])

    dark_side = Image.open(os.path.join(sd_tex, "drawers_dark_oak_side.png"))
    make_linked_side(dark_side).save(os.path.join(a, "textures/block/linked_tank_side.png"))
    make_linked_front(dark_side).save(os.path.join(a, "textures/block/linked_tank_front.png"))
    if ENDER_TEX is not None:
        chest_top_16().save(os.path.join(a, "textures/block/linked_tank_top.png"))
    else:
        make_linked_side(dark_side).save(os.path.join(a, "textures/block/linked_tank_top.png"))

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

    write_json(os.path.join(d, "tags/item/tanks.json"),
               {"replace": False, "values": [f"drawertanks:{w}_tank" for w in WOODS]})
    write_json(os.path.join(d, "recipe/linked_tank.json"), {
        "type": "minecraft:crafting_shaped",
        "pattern": ["OEO", "ETE", "OEO"],
        "key": {"O": "minecraft:obsidian", "E": "minecraft:ender_eye", "T": "#drawertanks:tanks"},
        "result": {"id": "drawertanks:linked_tank", "count": 1}
    })
    write_json(os.path.join(d, "loot_table/blocks/linked_tank.json"), loot_table("linked"))

    raw_side = Image.open(os.path.join(sd_tex, "drawers_raw_side.png"))
    make_front(raw_side).save(os.path.join(a, "textures/block/tank_raw_front.png"))
    write_json(os.path.join(a, "models/block/framed_tank.json"), framed_block_model())
    write_json(os.path.join(a, "blockstates/framed_tank.json"), facing_blockstate("drawertanks:block/framed_tank"))
    write_json(os.path.join(a, "items/framed_tank.json"),
               {"model": {"type": "storagedrawers:framed_block", "model": "drawertanks:framed_tank", "variant": "facing=north"}})
    write_json(os.path.join(a, "models/block/framed/tank_sides.json"), meta_tank_sides_model())
    write_json(os.path.join(a, "blockstates/meta_tank_side.json"), facing_blockstate("drawertanks:block/framed/tank_sides"))

    lang["block.drawertanks.framed_tank"] = "Framed Tank"
    lang["block.drawertanks.meta_tank_side"] = "Tank Side"
    lang["block.drawertanks.linked_tank"] = "Linked Tank"
    lang["message.drawertanks.linked.cleared"] = "Channel colors washed off"

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
