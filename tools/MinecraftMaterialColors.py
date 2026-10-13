"""Sample material colors, not sprite geometry, from the local vanilla client."""

from collections import Counter
import hashlib
import io
from pathlib import Path
import zipfile

from PIL import Image

SOURCE_JAR = Path(r"D:\DevCache\.gradle\caches\forge_gradle\minecraft_repo\versions\1.20.1\client.jar")
REFERENCES = {
    "wooden": "block/oak_planks", "copper": "item/copper_ingot",
    "iron": "item/iron_ingot", "emerald": "item/emerald", "gold": "item/gold_ingot",
    "crystal": "block/blue_ice", "diamond": "item/diamond", "obsidian": "block/obsidian",
    "netherite": "item/netherite_ingot", "base": "item/paper",
}
TIERS = ("wooden", "standard", "copper", "iron", "emerald", "gold", "crystal",
         "diamond", "obsidian", "netherite")


def luminance(color: tuple) -> float:
    return color[0] * 0.2126 + color[1] * 0.7152 + color[2] * 0.0722


def sample() -> tuple[dict, tuple, list]:
    palettes, records = {}, []
    with zipfile.ZipFile(SOURCE_JAR) as archive:
        for tier, reference in REFERENCES.items():
            entry = "assets/minecraft/textures/" + reference + ".png"
            raw = archive.read(entry)
            image = Image.open(io.BytesIO(raw)).convert("RGBA")
            counts = Counter(pixel[:3] for pixel in image.get_flattened_data() if pixel[3] == 255)
            ordered = sorted(counts, key=lambda color: (luminance(color), color))
            total = sum(counts.values())
            # Prefer brighter native purple-black texels for tiny obsidian icons.
            quantiles = (0.3, 0.9, 0.99) if tier == "obsidian" else (
                (0.2, 0.45, 0.9) if tier == "crystal" else (
                    (0.2, 0.5, 0.85) if tier == "base" else (0.2, 0.6, 0.85)))
            colors = []
            for quantile in quantiles:
                cumulative = 0
                for color in ordered:
                    cumulative += counts[color]
                    if cumulative >= total * quantile:
                        colors.append("#" + "".join(f"{channel:02x}" for channel in color))
                        break
            if len(set(colors)) != 3:
                raise ValueError(f"Material needs three distinct source shades: {tier}")
            palettes[tier] = tuple(colors)
            records.append({"material": tier, "source": entry,
                            "source_png_sha256": hashlib.sha256(raw).hexdigest(),
                            "quantiles": quantiles, "colors": colors})
    base = palettes.pop("base")
    # Standard represents Create's vault, with no vanilla material analogue.
    palettes["standard"] = ("#344354", "#586a7c", "#8b9da6")
    return {tier: palettes[tier] for tier in TIERS}, base, records
