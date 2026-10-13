"""Native 16px upgrade-icon studies drawn entirely with Python integer geometry.

No bitmap geometry is copied. Vanilla Minecraft textures supply material colors;
the approved geometry keeps upper-left highlights and layered metal-sheet style.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import zipfile

from PIL import Image, ImageDraw, ImageFont

import MinecraftMaterialColors as materials

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artwork/python-upgrade-samples"
PALETTES, BASE_PALETTE, MATERIAL_REFERENCES = materials.sample()
NAMES = {"wooden": "木质", "standard": "原版", "copper": "铜", "iron": "铁",
         "emerald": "绿宝石", "gold": "金", "crystal": "水晶", "diamond": "钻石",
         "obsidian": "黑曜石", "netherite": "下界合金"}
PAIRS = (("wooden", "standard"), ("standard", "iron"), ("iron", "gold"), ("diamond", "netherite"))
OUTLINE = "#292d2f"
BRASS_DARK, BRASS_LIGHT = "#927446", "#c7b17b"


def mix(first: str, second: str, weight: float) -> tuple[int, int, int, int]:
    a = tuple(int(first[index:index+2], 16) for index in (1, 3, 5))
    b = tuple(int(second[index:index+2], 16) for index in (1, 3, 5))
    return (*(round(x * (1-weight) + y * weight) for x, y in zip(a, b)), 255)


def texture_plate(image: Image.Image, body: str, clusters: tuple) -> None:
    """Shade only the original plate fill; keep outlines and geometry untouched."""
    base = (*(int(body[index:index+2], 16) for index in (1, 3, 5)), 255)
    mask = {(x, y) for y in range(16) for x in range(16) if image.getpixel((x, y)) == base}
    for color, points in clusters:
        for point in points:
            if point in mask:
                image.putpixel(point, color)


def draw_icon(source: str, target: str) -> Image.Image:
    src_dark, src, src_light = PALETTES[source]
    dst_dark, dst, dst_light = PALETTES[target]
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    # A smaller source plate behind a larger target plate: an upgrade assembly.
    draw.polygon(((1, 5), (5, 4), (7, 11), (3, 14), (1, 12)), fill=OUTLINE)
    draw.polygon(((2, 6), (4, 5), (6, 11), (3, 12), (2, 11)), fill=src)
    draw.line((2, 6, 4, 5), fill=src_light)
    draw.line((3, 13, 6, 11), fill=src_dark)
    texture_plate(image, src, (
        (mix(src, src_light, 0.35), ((2, 8), (3, 8), (3, 9))),
        (mix(src, src_dark, 0.4), ((2, 10), (3, 10), (3, 11))),
    ))
    # Sparse flat-color clusters add metal depth without dithering or noise.
    draw.polygon(((5, 3), (12, 2), (14, 5), (14, 11), (8, 13), (5, 11)), fill=OUTLINE)
    draw.polygon(((6, 4), (11, 3), (13, 5), (13, 10), (8, 11), (6, 10)), fill=dst)
    draw.line((6, 4, 11, 3), fill=dst_light)
    draw.line((8, 12, 13, 10), fill=dst_dark)
    texture_plate(image, dst, (
        (mix(dst, dst_light, 0.35), ((7, 5), (8, 5), (7, 6), (8, 6), (9, 6))),
        (mix(dst, dst_dark, 0.4), ((10, 8), (11, 8), (12, 8), (11, 9), (12, 9), (10, 10))),
        (mix(dst, dst_light, 0.7), ((7, 5), (8, 6))),
    ))
    # Two restrained brass connection pins tie the plates together.
    for y in (6, 10):
        draw.rectangle((4, y, 6, y+1), fill=BRASS_DARK)
        draw.line((4, y, 6, y), fill=BRASS_LIGHT)
    # Fixed pixel arrow with a broad triangular head; no tiny shrunk glyph.
    arrow = ((0, 0, 1, 0, 0), (0, 1, 1, 1, 0), (1, 1, 1, 1, 1),
             (0, 0, 1, 0, 0), (0, 0, 1, 0, 0),
             (0, 0, 1, 0, 0), (0, 0, 1, 0, 0))
    occupied = {(9+x, 1+y) for y, row in enumerate(arrow) for x, value in enumerate(row) if value}
    for x, y in occupied:
        for xx, yy in ((x-1, y), (x+1, y), (x, y-1), (x, y+1)):
            if 0 <= xx < 16 and 0 <= yy < 16 and (xx, yy) not in occupied:
                draw.point((xx, yy), fill=OUTLINE)
    # Match the destination material, with the same upper-left lighting as the plates.
    highlights = {(11, 1), (10, 2), (9, 3)}
    shadows = {(12, 2), (12, 3), (13, 3), (11, 7)}
    for point in occupied:
        color = dst_light if point in highlights else dst_dark if point in shadows else dst
        draw.point(point, fill=color)
    return image


def font(size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", size)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    records = []
    board = Image.new("RGBA", (864, 344), "#666b70")
    draw = ImageDraw.Draw(board)
    draw.text((22, 14), "Python原创升级组件样张", font=font(27), fill="white")
    draw.text((22, 53), "Create风格金属薄板 / 箭头随目标材质配色与明暗 / 16×16直接绘制", font=font(15), fill="#e0e4e7")
    for index, (source, target) in enumerate(PAIRS):
        icon = draw_icon(source, target)
        pixels = list(icon.get_flattened_data())
        colors = {pixel for pixel in pixels if pixel[3]}
        alpha = set(icon.getchannel("A").get_flattened_data())
        if len(colors) > 16 or not alpha <= {0, 255}:
            raise ValueError("Native icon contract violated")
        name = source + "_to_" + target + "_upgrade.png"
        path = OUT / name
        icon.save(path, format="PNG", optimize=True)
        records.append({"file": name, "source": source, "target": target, "size": [16, 16],
                        "colors": len(colors), "alpha": sorted(alpha),
                        "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
        x = 28 + index*214
        draw.text((x, 88), NAMES[source] + "→" + NAMES[target] + "升级", font=font(18), fill="white")
        board.alpha_composite(icon.resize((144, 144), Image.Resampling.NEAREST), (x+12, 124))
        board.alpha_composite(icon.resize((32, 32), Image.Resampling.NEAREST), (x+68, 284))
        draw.text((x+38, 321), "32px大小参考", font=font(13), fill="#e0e4e7")
    board.save(OUT / "samples.png", optimize=True)
    manifest = {"method": "Python integer geometry; vanilla Minecraft material colors; destination-palette arrow shading", "image_gen_calls": 0,
                "bitmap_geometry_inputs": 0, "palette_bitmap_inputs": len(MATERIAL_REFERENCES),
                "scope": "samples only; no mod/game replacement",
                "arrow_stem_pixels": 4, "palettes": PALETTES, "files": records}
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    with zipfile.ZipFile(OUT / "upgrade-icon-samples.zip", "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for record in records:
            archive.write(OUT / record["file"], record["file"])
        archive.write(OUT / "manifest.json", "manifest.json")
    print(json.dumps({"icons": len(records), "maximum_colors": max(r["colors"] for r in records),
                      "image_gen_calls": 0, "directory": str(OUT)}))


if __name__ == "__main__":
    main()
