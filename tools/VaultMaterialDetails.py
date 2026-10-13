"""Preserve Create texture motifs; vanilla material colors and sparse shading."""

from collections import Counter
import hashlib
import io

from PIL import Image

import MinecraftMaterialColors as materials

REFERENCES = {
    "wooden": "oak_log", "copper": "copper_block", "iron": "iron_block",
    "emerald": "emerald_block", "gold": "gold_block", "crystal": "blue_ice",
    "diamond": "diamond_block", "obsidian": "obsidian", "netherite": "netherite_block",
}


def mix(a: tuple, b: tuple, amount: float) -> tuple:
    return tuple(round(x * (1 - amount) + y * amount) for x, y in zip(a, b))


def ramp(palette: tuple, amount: float) -> tuple:
    amount = max(0.0, min(1.0, amount))
    a, b = palette[:2] if amount <= 0.5 else palette[1:]
    return mix(a, b, amount * 2 if amount <= 0.5 else (amount - 0.5) * 2)


def load_palettes(archive) -> tuple[dict, list]:
    palettes, records = {}, []
    for tier, name in REFERENCES.items():
        entry = "assets/minecraft/textures/block/" + name + ".png"
        raw = archive.read(entry)
        image = Image.open(io.BytesIO(raw)).convert("RGBA")
        counts = Counter(pixel[:3] for pixel in image.get_flattened_data() if pixel[3] == 255)
        ordered = sorted(counts, key=lambda color: (materials.luminance(color), color))
        quantiles = (0.3, 0.9, 0.99) if tier == "obsidian" else (
            (0.03, 0.5, 0.95) if tier == "iron" else (
                (0.05, 0.5, 0.95) if tier in ("gold", "copper") else (0.2, 0.5, 0.9)))
        colors = []
        for quantile in quantiles:
            cumulative = 0
            for color in ordered:
                cumulative += counts[color]
                if cumulative >= sum(counts.values()) * quantile:
                    colors.append(color)
                    break
        if len(set(colors)) != 3:
            raise ValueError("Expected three distinct block material tones: " + tier)
        palettes[tier] = tuple(colors)
        records.append({"material": tier, "source": entry,
                        "source_png_sha256": hashlib.sha256(raw).hexdigest(),
                        "palette": colors, "quantiles": quantiles,
                        "method": "color sampling only; no vanilla texture motif used"})
    return palettes, records


def panel(pixel: tuple) -> bool:
    red, green, blue, alpha = pixel
    return bool(alpha and blue > green and blue >= red + 4)


def normalized(pixel: tuple, limits: tuple) -> float:
    return (materials.luminance(pixel) - limits[0]) / max(1.0, limits[1] - limits[0])


def coordinates(x: int, y: int, filename: str) -> tuple:
    """Align front facets with the physical 2x2/3x3 RECTANGLE face."""
    size = 2 if "medium" in filename else 3 if "large" in filename else 1
    if size == 1:
        return x, y, 16
    column, row = x // 16, y // 16
    u = 0 if column in (0, 1) else size - 1 if column == 3 else 1
    v = 0 if row in (0, 3) else size - 1 if row == 2 else 1
    return u * 16 + x % 16, v * 16 + y % 16, size * 16


def detail(x: int, y: int) -> float:
    """The same sparse color variations for every material; no block motifs."""
    x, y = x % 16, y % 16
    if (x, y) in ((3, 5), (4, 5), (4, 6), (5, 6)):
        return 0.065
    if (x, y) in ((10, 9), (11, 9), (10, 10), (12, 10), (12, 11)):
        return -0.065
    return 0.0


def frame_palette(tier: str, palette: tuple) -> tuple:
    dark, body, light = palette
    if tier == "wooden":
        return mix(dark, (0, 0, 0), 0.32), mix(body, dark, 0.30), light
    if tier == "obsidian":
        return dark, body, mix(light, (132, 112, 155), 0.35)
    # Material-colored casing, with enough contrast to retain Create's frame.
    return mix(dark, (0, 0, 0), 0.66), mix(body, (0, 0, 0), 0.38), light


def side_rivets(source: Image.Image) -> set:
    """Find central two-pixel rivet runs, including dots split across CT tiles."""
    points = set()
    for tile_y in range(0, source.height, 16):
        for tile_x in range(0, source.width, 16):
            for y in range(16):
                metal = [x for x in range(16)
                         if source.getpixel((tile_x + x, tile_y + y))[3]
                         and not panel(source.getpixel((tile_x + x, tile_y + y)))]
                # Full horizontal borders and edge-frame segments are retained.
                if len(metal) == 2 and metal[1] == metal[0] + 1 and 4 <= metal[0] <= 10:
                    points.update((tile_x + x, tile_y + y) for x in metal)
    return points


def render(source: Image.Image, tier: str, filename: str, palette: tuple, limits: tuple) -> Image.Image:
    image = Image.new("RGBA", source.size)
    panel_limits, frame_limits = limits
    trim = frame_palette(tier, palette)
    face = filename.split("_")[1]
    rivets = side_rivets(source) if tier == "crystal" and face == "side" else set()
    for y in range(source.height):
        for x in range(source.width):
            original = source.getpixel((x, y))
            if (x, y) in rivets:
                # Find the closest panel along this column inside its CT tile;
                # evaluate facet RGB and alpha at the repaired pixel position.
                tile_top = (y // 16) * 16
                candidates = [(x, yy) for yy in range(tile_top, tile_top + 16)
                              if panel(source.getpixel((x, yy)))]
                if not candidates:
                    raise ValueError("Side rivet repair needs an adjacent panel pixel")
                donor = min(candidates, key=lambda point: (abs(point[1] - y), point[1]))
                original = source.getpixel(donor)
                if not panel(original):
                    raise ValueError("Side rivet repair needs an adjacent panel pixel")
            if original[3] == 0:
                image.putpixel((x, y), original)
                continue
            if not panel(original):
                rgb = ramp(trim, normalized(original, frame_limits))
                image.putpixel((x, y), (*rgb, original[3]))
                continue
            amount = normalized(original, panel_limits)
            alpha = original[3]
            if tier == "crystal":
                u, v, extent = coordinates(x, y, filename)
                # Three large blue facets; visibility uses real alpha blending.
                facet = 0.16 if u + v < extent * 0.72 else -0.12 if u > v + extent * 0.15 else 0.03
                alpha = 168 if facet > 0.1 else 128 if facet < 0 else 148
                amount = amount * 0.65 + 0.18 + facet
            else:
                if face == "front":
                    u, v, extent = coordinates(x, y, filename)
                    pattern = detail(u * 16 // extent, v * 16 // extent) * 0.55
                else:
                    pattern = detail(x, y)
                amount = amount * 0.84 + 0.08 + pattern
            rgb = ramp(palette, amount)
            image.putpixel((x, y), (*rgb, alpha))
    return image
