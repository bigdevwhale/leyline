"""Generates Leyline's art with the Codex CLI image tool (the Caravan/Roamling pipeline).

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in
the run's output is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py              # everything missing
    python tools/gen_art.py ingr_a den   # only these
    python tools/gen_art.py --ref mount_car mount_bus   # pass art/sheets/mount_car.png as style reference
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying).\n\n"

STYLE = (
    "Style: a cozy fantasy storybook illustration in the manner of an old illuminated grimoire: fine dark-ink outlines, soft watercolour "
    "and gouache shading, a palette of deep night blue, violet, warm gold, parchment cream and moss green, with luminous teal-cyan magic glow "
    "as the accent colour. Hand-painted, warm and friendly, no photo realism, no 3D render, no pixel art, no anime."
)
OBJ_STYLE = STYLE + (" Plain objects drawn clearly and readably like inventory icons of a cozy game, slightly glowing. "
                     "IMPORTANT: NO faces, NO eyes, NO mouths on objects, NO people, NO animals, NO text.")
GRID = ("on a flat solid pure magenta (#FF00FF) background. One item per cell, centered, filling most of the cell, "
        "with clear margin; nothing touches the cell borders; no text, no letters, no numbers, no grid lines, no shadows on the background.")
NOBODY = "IMPORTANT: NO people, NO text, NO letters, NO numbers anywhere."


def sheet(what, cols, rows, items, style=OBJ_STYLE):
    lines = "\n".join(f"{i + 1}) {t}" for i, t in enumerate(items))
    return HEAD + f"Image: a sprite sheet of {len(items)} {what}.\n{style}\nLayout: {cols} columns x {rows} rows of equal square cells {GRID}\nItems, left to right, top to bottom:\n{lines}"


# Ingredients: the order matters (tools/cut.py, domain/Book.kt).
INGR_A = [
    "a corked glass bottle with a swirling silver-white breeze and tiny leaves whirling inside (wind)",
    "a smooth round grey river stone with a softly glowing teal spiral rune carved on it (patience stone)",
    "a round glass flask filled with pale blue drifting mist and tiny droplets (river mist)",
    "a big glowing golden drop of tree resin on a short pine twig with needles (resin)",
    "a small dark-blue velvet pouch spilling glittering silver dust with tiny crescent sparkles (moon dust)",
    "a single big dewdrop on a rose-pink leaf glowing with sunrise colours (dawn dew)",
    "a small jar of dark stormy water with a tiny bright lightning spark inside (storm water)",
    "a sharp crystal shard of blue-white frost shaped like a snowflake (hoarfrost)",
    "a warm glowing piece of sun-yellow amber with a tiny sun shape inside (sun amber)",
]
INGR_B = [
    "a chunk of dark rock ore with glowing violet crystal veins (deep ore)",
    "a melted beeswax candle stub with a small golden flame (candle wax)",
    "a crumbling fragment of an ancient carved stone ornament with golden dust (dust of ages)",
    "a small crystal bottle of dark ink full of tiny stars with a quill beside it (sage's ink)",
    "a glowing orange ember held in small iron tongs with sparks (forge spark)",
    "a long elegant traveller's feather with a brass ring around its quill (wanderer's feather)",
    "a single pale white lily flower with a faint silver glow (pale lily)",
    "a small heap of old copper coins with square holes and a bit of green patina (market copper)",
    "a glowing five-pointed crystal star shard, teal and white (star shard)",
]
# Potions, elixirs and wonders: same order as the recipes in domain/Book.kt (cells 11 and 12 are the unknown vial and the power orb).
POTIONS = [
    "a slim tall bottle of pale silver potion with a feather tied to its neck and wind swirls (tailwind elixir)",
    "a round fat bottle of green-gold potion with tiny leaves and seeds floating (generous road potion)",
    "a small dropper vial of bright teal liquid with a tiny compass on the cork (seeker's drops)",
    "a long flask of amber potion wrapped in a little leather road strap (long road brew)",
    "a small glass phial with a single bright spark of light inside, a gold cap (spark of awakening)",
    "a heart-shaped crimson glass bottle with a warm glowing ember inside (beast's heart)",
    "an ancient dark-green bottle with gold runes and a glowing dragon-scale wax seal (ancient blood)",
    "a bottle containing swirling green and violet northern lights (bottled aurora)",
    "a small round silver hand mirror whose glass shows a glowing winding road (mirror of the road)",
    "a magnificent crystal flask of rainbow potion with a golden stopper and stars (the great elixir)",
    "an empty corked clear glass vial with a faint question-like swirl of smoke (unknown recipe)",
    "a small sphere of glowing teal-cyan light with soft rays, like a captured spark of magic (power orb)",
]
GIFTS = [
    "a woven wreath of pine twigs, golden resin drops and small pink dawn flowers",
    "a glowing misty-blue pearl in an open shell",
    "a tall old candle dripping golden wax in an ancient bronze holder",
    "a small inkwell of glowing star ink with a silver quill",
    "a tiny forged iron charm in the shape of a flame, glowing orange",
    "a round copper amulet on a cord with a teal stone in the middle",
    "a brass traveller's compass with a feather engraved on the lid, open",
    "a little hearth charm: a woven straw star with a tiny glowing ember in the middle",
]
WARDS = [
    "a small sacred tree shrine: a young oak with ribbons and a little stone altar",
    "a round stone well with soft blue mist rising from it",
    "a tall wrought-iron lantern post with a silver moon-coloured light",
    "a tall rough standing stone with a glowing teal spiral",
    "a small open bell tower of white stone with a bronze bell",
    "a tall faceted crystal obelisk glowing teal-white on a stone base",
]

CREATURE = (STYLE + " Each picture is the same friendly fantasy creature, full body, side view facing right, cute and expressive with big warm eyes, "
            "standing alone in its cell. IMPORTANT anatomy: only the legs described, NEVER human arms or hands; no saddle, no rider, no people, no text.")

MOUNTS = {
    "car": "a road drake: a sturdy four-legged little dragon with short folded wings on its back, warm amber-orange scales, a cream belly, "
           "small curved horns and softly glowing teal rune marks along its back",
    "bus": "a big friendly carrier beetle with six short legs, a wide round mossy-green shell with little glowing golden lantern windows along its sides "
           "like a cozy omnibus, two curled antennae",
    "tram": "a spark serpent: a long sky-blue eel-like dragon floating in the air along a faint glowing line, small fin wings, crackling electric whiskers "
            "and tiny lightning sparks on its tail, no legs",
    "metro": "a burrow wyrm: a plump four-legged mole-dragon with velvety dark violet fur, big digging claws on its four paws, small closed sleepy eyes "
             "and a row of glowing violet crystal spines along its back",
    "bike": "a wind fox: a swift slender fox with two tails, rust-red fur with silver wind-swirl markings and glowing teal paws",
}
STAGES = [
    "a tiny round baby just hatched, sitting, with a bit of eggshell beside it",
    "a young one, half-grown, playful, standing",
    "a fully grown adult, proud and strong, standing",
    "an ancient elder: bigger, majestic, with golden rune markings glowing and a crown of small teal crystals, standing",
    "the grown adult curled up asleep, eyes closed",
    "the grown adult happy, leaping or prancing with joy, little sparkles around",
]


def mount(desc):
    lines = "\n".join(f"{i + 1}) {s}" for i, s in enumerate(STAGES))
    return (HEAD + f"Image: a sprite sheet of 6 pictures of one creature: {desc}.\n{CREATURE}\n"
            f"Layout: 3 columns x 2 rows of equal square cells {GRID}\nPictures, left to right, top to bottom:\n{lines}")


SPIRIT_STYLE = (STYLE + " Each is a friendly magical guardian spirit of a city quarter, a creature (not a person), full body, three-quarter view, "
                "gentle wise face with warm eyes, softly glowing. IMPORTANT: animals only with their natural legs or wings, NEVER human arms or hands, no clothes except as described, no text.")
SPIRITS = [
    "a gentle stag spirit made of moss and ferns with small white flowers on its antlers (spirit of gardens)",
    "a river otter spirit with water-droplet sparkles and a lily pad on its head (spirit of waters)",
    "an old wise owl holding a small glowing candle-lantern in its beak (spirit of the old town)",
    "a dark blue cat with star-speckled fur and small round spectacles on its nose (spirit of scholars)",
    "a fire salamander with glowing ember skin and sparks rising (spirit of forges)",
    "a magpie with a little copper coin in its beak and teal sheen on its feathers (spirit of markets)",
    "a tall grey crane with a long striped traveller's scarf around its neck (spirit of gates and stations)",
    "a round hedgehog with a tiny glowing lantern on its back like a little house (spirit of homes)",
]

BANNER = HEAD + ("Image: a wide banner illustration for a cozy magical app about city rides. Landscape about 3:1, full-bleed edge to edge "
                 "(no border, no frame, no text, no letters, no numbers, no logo).\n" + STYLE +
                 "\nKeep the left third calmer and simpler (a title will be written over it).\n" + NOBODY + " No creatures.\nScene: ")
SCENE = HEAD + ("Image: an illustration for a cozy magical app about city rides. PORTRAIT 2:3, full-bleed (no border, no text, no letters, no logo).\n"
                + STYLE + "\n" + NOBODY + " No creatures, no animals.\nScene: ")

JOBS = {
    "ingr_a": sheet("magical ingredients", 3, 3, INGR_A),
    "ingr_b": sheet("magical ingredients", 3, 3, INGR_B),
    "potions": sheet("magic potions and wonders", 4, 3, POTIONS),
    "gifts": sheet("small magical gifts and charms", 4, 2, GIFTS),
    "wards": sheet("small magical guardian structures, each standing alone on a small round patch of grass and stones, all drawn at the same scale", 3, 2, WARDS),
    "cauldron": sheet("cauldrons", 2, 1, [
        "a round black iron witch's cauldron on three short legs over a small calm fire, quiet and empty-looking, a little steam",
        "the same round black iron cauldron bubbling with glowing teal-cyan potion, swirls of sparkling steam rising, golden sparks",
    ]),
    "tabs": sheet("navigation icons for the bottom menu of a cozy magic app, bold simple silhouettes that read well at small size", 4, 1, [
        "a round black cauldron with a teal glow",
        "a folded city map with glowing teal lines across it",
        "a thick old spell book with a brass clasp and a star on the cover",
        "a paw print of a dragon with a small glowing rune",
    ]),
    "icons": sheet("small interface icons", 4, 3, [
        "a full silver moon with soft glow", "a grey cloud with blue rain drops", "a white snowflake", "a golden sun with short rays",
        "a fortune card with a golden star on a night-blue back", "a glowing teal rune circle sigil", "a brass compass", "a brass hourglass",
        "a rolled parchment scroll with a red ribbon", "a golden five-pointed star", "a small glowing lantern", "a small shrine marker: a stone pillar with a teal flame on top",
    ]),
    "decor": sheet("cozy things for a wizard's tower room, each a separate piece seen from the front, drawn to be placed into a room picture", 4, 3, [
        "a long garland of small glowing paper star lanterns on a string, hanging in a gentle curve (wide and thin)",
        "a hanging mobile of little golden moons and stars on threads under a wooden ring",
        "a hanging brass birdcage with a small glowing teal wisp of light inside instead of a bird",
        "a crystal ball on a small carved wooden stand, softly glowing violet",
        "a brass telescope on a wooden tripod pointing up to the right",
        "a round low tea table with a steaming teapot, two cups and a plate of cookies",
        "a tall narrow grandfather clock of dark wood with a moon-phase dial (no numbers)",
        "an open wooden treasure chest with a patterned blanket and scrolls inside",
        "a big clay pot with softly glowing blue mushrooms and ferns",
        "a round woven basket bed with a soft plaid blanket and a pillow",
        "a stack of old spell books with a burning candle on top",
        "a small round rug-side floor lamp: a paper lantern on a short wooden stand glowing warm",
    ]),
    "spirits": HEAD + "Image: a sprite sheet of 8 creature portraits.\n" + SPIRIT_STYLE + "\nLayout: 4 columns x 2 rows of equal cells " + GRID
               + "\nCreatures, left to right, top to bottom:\n" + "\n".join(f"{i + 1}) {t}" for i, t in enumerate(SPIRITS)),
    **{f"mount_{k}": mount(v) for k, v in MOUNTS.items()},
    "den": SCENE + ("the cozy round study at the top of a wizard's tower at golden evening. Seen from inside, flat front view. Upper half: tall arched windows "
                    "show a city skyline at sunset with faint glowing teal lines running along its streets, shelves with jars and books on both sides, "
                    "bundles of dried herbs hanging from the beams, a hanging brass lamp. The lower 40% is an EMPTY wooden floor with a big round "
                    "patterned rug in the middle – nothing stands on the rug (a cauldron and a creature will be placed there later)."),
    "den_night": SCENE + ("the same cozy round study at the top of a wizard's tower at night: tall arched windows show a dark-blue city skyline with a big moon and "
                          "glowing teal lines along the streets, candles burning on the shelves with jars and books, dried herbs hanging. The lower 40% is an "
                          "EMPTY wooden floor with a big round patterned rug in the middle – nothing stands on the rug."),
    "prologue_city": SCENE + ("a view from a high tower over a big modern city at dusk, painted as a storybook: streets, bridges over a river, tram lines, parks, "
                              "old domes and new houses; along the streets run softly glowing teal-cyan magic lines like veins of light that meet in bright knots."),
    "prologue_ride": SCENE + ("an ordinary city street at night: a city bus and a small car drive along it, and behind them trails of glowing teal light and tiny "
                              "golden sparkles drift into the air; lamp posts, a tram wire, a calm night sky. The vehicles have nobody visible inside."),
    "prologue_brew": SCENE + ("an old wooden alchemist's table seen from the front: a round black cauldron glowing teal in the middle, and around it in the air "
                              "float magical ingredients: a bottle of wind, a flask of mist, a resin drop, a pouch of moon dust, a frost crystal, an ember."),
    "banner_grimoire": BANNER + "an open old spell book on a desk with glowing diagrams of potions (no letters), jars of ingredients, a quill and a candle.",
    "banner_stable": BANNER + "a cozy magical stable at night: wooden stalls with straw, hanging lanterns, saddle-less racks with scarves, a round window with stars.",
    "banner_map": BANNER + "an old illuminated map of a city on a table with glowing teal lines connecting places, a compass and small standing stones as markers.",
    "banner_spirits": BANNER + "an evening city square with an old fountain, lanterns, ivy and small glowing wisps of light floating between the houses.",
}


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


REF = None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    cmd = [shutil.which("codex.cmd") or shutil.which("codex") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only"]
    if REF is not None:
        cmd += ["-i", str(REF)]
        prompt = prompt.replace(HEAD, HEAD + "The attached image is the style reference: match its drawing style, line and palette exactly (not its content).\n\n")
    r = None
    for _ in range(2):
        r = subprocess.run(cmd + ["-"], input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
        p = image_of((r.stdout or "") + (r.stderr or ""))
        if p is not None:
            shutil.copy(p, dst)
            print("ok", name, flush=True)
            return
    print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)


def main():
    global REF
    OUT.mkdir(parents=True, exist_ok=True)
    args = sys.argv[1:]
    if args[:1] == ["--ref"]:
        REF = OUT / f"{args[1]}.png"
        args = args[2:]
    jobs = [j for j in JOBS.items() if not args or j[0] in args]
    with ThreadPoolExecutor(4) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
