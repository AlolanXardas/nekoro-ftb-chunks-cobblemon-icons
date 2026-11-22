"""Caught badges: one picture layer of the user's coin/bag art, recolored for every badge color.

Exports the layers with Aseprite, then writes 32x32 textures into both loader projects:
  assets/<mod id>/textures/badge/<badge>/<color>.png   colored badge, one per color option
  assets/<mod id>/textures/badge/<badge>/faded.png     gray badge
and a preview to art/badges.png.

Recoloring keeps the shading of the original: the most common fill shade becomes the chosen color,
darker shades (outline, shadows) darken it, lighter shades blend it toward white.
The ball is special: only its top half takes the color, the bottom half stays white
and the band stays dark; in the faded version both halves are gray.
Every badge gets a black frame, whatever its color.
"""
import subprocess
import sys
import tempfile
from collections import Counter
from pathlib import Path

from PIL import Image

MOD_ID = "nekoroftbchunkscobblemonicons"
ROOT = Path(__file__).resolve().parent.parent
ASEPRITE = Path(r"E:\SteamLibrary\steamapps\common\Aseprite\Aseprite.exe")
BAGS = Path(r"E:\Minecraft\Mods\Nekoro Collector Edition\art\source\xItemBags.ase")
COINS = Path(r"E:\Minecraft\Mods\Nekoro Coins\art\coins\xItemCoins.ase")

# badge id -> (file, layer, frame); frames count from 1 like the coin numbers
BADGES = {
    "ball": (BAGS, "Picture", 31),
    "cat": (COINS, "Cat", 1),
    "star": (BAGS, "Picture", 30),
    "cross": (BAGS, "Picture", 21),
    "eye": (BAGS, "Picture", 32),
    "exclamation": (BAGS, "Picture", 2),
    "question": (BAGS, "Picture", 33),
}

# Same ids and values as OutlineColor (without "off").
COLORS = {
    "black": 0x303030, "white": 0xFFFFFF, "gray": 0x8A8A8A, "red": 0xE02020, "orange": 0xFF8A1E,
    "gold": 0xFFC31E, "yellow": 0xFFF23D, "green": 0x2FC23A, "light_blue": 0x5CD2FF,
    "dark_blue": 0x1F3FBF, "purple": 0x6A1BC2, "pink": 0xFF5CC8,
}
FADED = (88, 88, 88)
BALL_BOTTOM = (255, 255, 255)
BALL_DARK = (40, 40, 46)
FRAME = (0, 0, 0, 255)
SIZE = 32


def rgb(value):
    return (value >> 16) & 255, (value >> 8) & 255, value & 255


def luma(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def export_layer(file, layer, frame, out_dir):
    pattern = out_dir / f"{file.stem}-{layer}{{frame1}}.png"
    target = out_dir / f"{file.stem}-{layer}{frame}.png"
    if not target.exists():
        subprocess.run([str(ASEPRITE), "-b", "--layer", layer, str(file), "--save-as", str(pattern)],
                       check=True, capture_output=True)
    return Image.open(target).convert("RGBA")


def centered(image):
    """Crops to the drawn pixels and centers them on a 32x32 canvas."""
    box = image.getbbox()
    part = image.crop(box)
    canvas = Image.new("RGBA", (SIZE, SIZE))
    canvas.paste(part, ((SIZE - part.width) // 2, (SIZE - part.height) // 2))
    return canvas


def shade(color, ratio):
    """ratio 1 = color, below 1 darker (not quite black, so outlines keep the hue), above 1 toward white."""
    if ratio <= 1:
        k = 0.22 + 0.78 * ratio
        return tuple(round(v * k) for v in color)
    t = min(ratio - 1, 1)
    return tuple(round(v + (255 - v) * t) for v in color)


def recolor(image, color):
    pixels = [p for p in image.getdata() if p[3]]
    counts = Counter(p[:3] for p in pixels)
    darkest = min(counts, key=luma)
    fill = max((c for c in counts if c != darkest), key=counts.get)
    base = luma(fill)
    out = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            p = image.getpixel((x, y))
            if not p[3]:
                continue
            l = luma(p[:3])
            ratio = l / base if l <= base else 1 + (l - base) / (1 - base)
            out.putpixel((x, y), shade(color, ratio) + (p[3],))
    return out


def framed(image):
    """Black one-pixel frame around the picture (also around its holes), the same for every color."""
    out = image.copy()
    for y in range(image.height):
        for x in range(image.width):
            if image.getpixel((x, y))[3]:
                continue
            for dx, dy in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < image.width and 0 <= ny < image.height and image.getpixel((nx, ny))[3]:
                    out.putpixel((x, y), FRAME)
                    break
    return out


def ball(image, top, bottom):
    """The source ball has a light fill and dark band/outline; the band row splits the halves."""
    counts = Counter(p[:3] for p in image.getdata() if p[3])
    dark = min(counts, key=luma)
    band = max(range(image.height),
               key=lambda y: sum(1 for x in range(image.width) if image.getpixel((x, y))[:3] == dark
                                 and image.getpixel((x, y))[3]))
    out = Image.new("RGBA", image.size)
    for y in range(image.height):
        for x in range(image.width):
            p = image.getpixel((x, y))
            if not p[3]:
                continue
            c = BALL_DARK if p[:3] == dark else (top if y < band else bottom)
            out.putpixel((x, y), c + (p[3],))
    return out


def main():
    projects = [ROOT / "minimap", ROOT / "minimap-fabric"]
    images = {}
    with tempfile.TemporaryDirectory() as tmp:
        for badge, (file, layer, frame) in BADGES.items():
            images[badge] = centered(export_layer(file, layer, frame, Path(tmp)))

    preview = Image.new("RGBA", ((len(COLORS) + 1) * (SIZE + 2), len(BADGES) * (SIZE + 2)), (48, 48, 56, 255))
    for row, (badge, image) in enumerate(images.items()):
        variants = {}
        for name, value in COLORS.items():
            color = rgb(value)
            variants[name] = ball(image, color, BALL_BOTTOM) if badge == "ball" else recolor(image, color)
        variants["faded"] = ball(image, FADED, FADED) if badge == "ball" else recolor(image, FADED)
        for col, (name, result) in enumerate(variants.items()):
            result = framed(result)
            for project in projects:
                target = project / "src/main/resources/assets" / MOD_ID / "textures/badge" / badge / f"{name}.png"
                target.parent.mkdir(parents=True, exist_ok=True)
                result.save(target)
            preview.alpha_composite(result, (col * (SIZE + 2), row * (SIZE + 2)))
    preview.resize((preview.width * 4, preview.height * 4), Image.NEAREST).save(ROOT / "art/badges.png")
    print(f"{len(images)} badges x {len(COLORS) + 1} variants")


if __name__ == "__main__":
    sys.exit(main())
