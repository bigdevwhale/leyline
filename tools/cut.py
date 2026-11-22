"""Cuts the Codex sheets in art/sheets into app drawables (the Caravan/Knackling cutter).

Magenta is keyed by a flood fill from the cell edges, so lavender and pink inside the art survive.

    python tools/cut.py [sheet names]
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Same order as the prompts in gen_art.py and the ids in domain/Book.kt.
INGR_A = ["wind", "stone", "mist", "resin", "moon", "dawn", "storm", "frost", "sun"]
INGR_B = ["ore", "wax", "dust", "ink", "spark", "feather", "lily", "copper", "shard"]
POTIONS = ["tailwind", "harvest", "seeker", "longroad", "awaken1", "awaken2", "awaken3", "aurora", "mirror", "great", "unknown", "power"]
KINDS = ["garden", "water", "old", "scholar", "forge", "bazaar", "gate", "homes"]
WARDS = ["grove", "well", "lantern", "menhir", "bell", "crystal"]
STAGES = ["baby", "young", "grown", "elder", "sleep", "joy"]
ICONS = ["moon", "rain", "snow", "sun", "omen", "sigil", "compass", "hourglass", "scroll", "star", "lantern", "shrine"]

SPRITES = [  # sheet, names, prefix, cols, rows, max px
    ("ingr_a", INGR_A, "ing", 3, 3, 220), ("ingr_b", INGR_B, "ing", 3, 3, 220),
    ("potions", POTIONS, "pot", 4, 3, 220), ("gifts", KINDS, "gift", 4, 2, 220),
    ("wards", WARDS, "ward", 3, 2, 300), ("cauldron", ["idle", "brew"], "cauldron", 2, 1, 520),
    ("tabs", ["home", "map", "book", "stable"], "tab", 4, 1, 160), ("icons", ICONS, "ic", 4, 3, 140),
    ("spirits", KINDS, "spirit", 4, 2, 420),
    ("decor", ["garland", "mobile", "cage", "ball", "telescope", "teatable", "clock", "chest", "mushrooms", "basket", "books", "lamp"], "decor", 4, 3, 360),
    *[(f"mount_{b}", STAGES, f"mount_{b}", 3, 2, 520) for b in ["car", "bus", "tram", "metro", "bike"]],
]
SCENES = {"den": 1000, "den_night": 1000, "prologue_city": 900, "prologue_ride": 900, "prologue_brew": 900,
          "banner_grimoire": 960, "banner_stable": 960, "banner_map": 960, "banner_spirits": 960}


def is_key(arr):
    r, g, b = arr[..., 0].astype(int), arr[..., 1].astype(int), arr[..., 2].astype(int)
    return (np.minimum(r, b) - g > 70) & (r > 150) & (b > 150)


def key_cell(cell):
    """Transparent where magenta touches the edges (flood fill); soft fringe by colour distance."""
    arr = np.array(cell.convert("RGBA")).astype(np.int32)
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    d = np.sqrt((r - 255) ** 2 + g ** 2 + (b - 255) ** 2)
    near = d < 120
    lab, _ = ndimage.label(near)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    bg = np.isin(lab, list(edge))
    # small enclosed magenta holes (between legs, inside handles) go too
    hole = ((d < 60) | ((np.minimum(r, b) - g > 110) & (r > 180) & (b > 180))) & ~bg
    bg |= hole
    out = arr.copy()
    alpha = np.where(bg, 0, 255)
    # the fringe: pixels next to the background lose their pink and some alpha
    grown = ndimage.binary_dilation(bg, iterations=4) & ~bg
    touch = ndimage.binary_dilation(bg, iterations=1) & ~bg
    k = np.clip((160 - d) / 160, 0, 1)
    fr = grown & (d < 200)
    alpha = np.where(fr, (255 * (1 - k * 0.85)).astype(np.int32), alpha)
    # the outermost pixel is half see-through, so no hard pink line is left
    alpha = np.where(touch, alpha // 2, alpha)
    # spill: near the edge, magenta tint (red and blue both above green) is pulled back to grey-ish
    m = np.minimum(r, b) - g
    pinky = grown & (m > 12)
    keep = np.maximum(r - b, 0)  # warm reds (orange fur) keep their red
    out[..., 0] = np.where(pinky, np.minimum(r, g + keep + (r - g - keep).clip(0) * 0.15), r)
    out[..., 2] = np.where(pinky, g + (b - g) * 0.15, b)
    out[..., 3] = alpha
    return Image.fromarray(out.clip(0, 255).astype(np.uint8))


def biggest_only(cell):
    arr = np.array(cell)
    lab, n = ndimage.label(arr[:, :, 3] > 30)
    if n == 0:
        return cell
    sizes = ndimage.sum(np.ones_like(lab), lab, range(n + 1))
    big = int(np.argmax(sizes[1:]) + 1)
    edge = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0, big}
    for k in edge:
        arr[lab == k, 3] = 0
    for k in range(1, n + 1):
        if sizes[k] < 30:
            arr[lab == k, 3] = 0
    return Image.fromarray(arr)


def frames(path, cols, rows):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    cw, ch = w / cols, h / rows
    for j in range(rows):
        for i in range(cols):
            cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            cell = biggest_only(key_cell(cell))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            yield cell.crop(box) if box else cell


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = set(sys.argv[1:])
    for sheet, names, prefix, cols, rows, size in SPRITES:
        f = SHEETS / f"{sheet}.png"
        if not f.exists() or (want and sheet not in want):
            continue
        for name, fr in zip(names, frames(f, cols, rows)):
            fr.thumbnail((size, size), Image.LANCZOS)
            fr.save(OUT / f"{prefix}_{name}.webp", "WEBP", quality=90, method=6)
        print(sheet, flush=True)
    for name, width in SCENES.items():
        f = SHEETS / f"{name}.png"
        if not f.exists() or (want and name not in want):
            continue
        g = Image.open(f).convert("RGB")
        g = g.resize((width, round(width * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / f"{name}.webp", "WEBP", quality=86, method=6)
        print(name, flush=True)


if __name__ == "__main__":
    main()
