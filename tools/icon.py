"""The launcher icon: the young road drake on night blue, and docs/logo.png.

    python tools/icon.py
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
from cut import frames, SHEETS  # noqa: E402

BG = (27, 33, 64, 255)  # icon_bg

cart = list(frames(SHEETS / "mount_car.png", 3, 2))[1]
fg = Image.new("RGBA", (432, 432), (0, 0, 0, 0))
s = 290 / max(cart.size)
a = cart.resize((round(cart.width * s), round(cart.height * s)), Image.LANCZOS)
fg.paste(a, ((432 - a.width) // 2, (432 - a.height) // 2), a)
out = ROOT / "app/src/main/res/drawable-nodpi"
fg.save(out / "ic_launcher_fg.webp", "WEBP", lossless=True)
logo = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
m = Image.new("L", (512, 512), 0); ImageDraw.Draw(m).rounded_rectangle((0, 0, 511, 511), 112, fill=255)
logo.paste(Image.new("RGBA", (512, 512), BG), (0, 0), m)
big = fg.resize((512 * 432 // 330, 512 * 432 // 330), Image.LANCZOS)
logo.alpha_composite(big, ((512 - big.width) // 2, (512 - big.height) // 2))
(ROOT / "docs").mkdir(exist_ok=True)
logo.save(ROOT / "docs/logo.png")
print("icon ok")
