"""Recolor the installed Create vault PNGs without redrawing or resizing them.

Output is a local sample set only; no game or mod resource files are replaced.
"""

from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path
import zipfile

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE_JAR = Path(r"E:\MINECRIFT\.minecraft\versions\Skyblock Burgeria\mods\create-1.20.1-6.0.4.jar")
OUT = ROOT / "artwork/create-recolor-samples"
PREFIX = "assets/create/textures/block/vault/"


def recolor(pixel: tuple[int, int, int, int], material: str) -> tuple[int, int, int, int]:
    red, green, blue, alpha = pixel
    if not alpha:
        return pixel  # Keep even the RGB data of unused transparent atlas cells.
    light = red * 0.2126 + green * 0.7152 + blue * 0.0722
    panel = blue > green and blue >= red + 4
    if material == "wooden":
        if not panel:
            return pixel  # Original metal bands and their highlights remain intact.
        channels = (light * 1.28 + 10 + (red-light) * 0.12,
                    light * 0.96 + 7 + (green-light) * 0.12,
                    light * 0.66 + 5 + (blue-light) * 0.12)
    elif material == "iron":
        if panel:
            channels = (red * 0.4 + light * 0.6 + 22,
                        green * 0.4 + light * 0.6 + 23,
                        blue * 0.4 + light * 0.6 + 23)
        else:
            channels = (red * 0.35 + light * 0.65,
                        green * 0.35 + light * 0.65,
                        blue * 0.35 + light * 0.65 + 2)
    else:
        raise ValueError("Unknown material")
    return (*[max(0, min(255, round(channel))) for channel in channels], alpha)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    records = []
    with zipfile.ZipFile(SOURCE_JAR) as archive:
        entries = sorted(name for name in archive.namelist() if name.startswith(PREFIX) and name.endswith(".png"))
        if len(entries) != 12:
            raise ValueError("Expected the 12 original Create vault PNGs")
        for entry in entries:
            raw = archive.read(entry)
            with Image.open(io.BytesIO(raw)) as opened:
                source = opened.convert("RGBA")
            pixels = list(source.get_flattened_data())
            for material in ("wooden", "iron"):
                target_pixels = [recolor(pixel, material) for pixel in pixels]
                # A one-to-one palette map preserves every original color boundary.
                colors = set(pixels)
                if len({recolor(color, material) for color in colors}) != len(colors):
                    raise ValueError("Palette colors merged; shading detail would be lost")
                if any(before[3] != after[3] for before, after in zip(pixels, target_pixels)):
                    raise ValueError("Original alpha channel changed")
                target = Image.new("RGBA", source.size)
                target.putdata(target_pixels)
                destination = OUT / (material + "_vault") / Path(entry).name
                destination.parent.mkdir(parents=True, exist_ok=True)
                target.save(destination, format="PNG", optimize=True)
                records.append({"material": material, "source": entry,
                    "output": destination.relative_to(OUT).as_posix(), "size": list(source.size),
                    "original_color_count": len(colors), "output_color_count": len(set(target_pixels)),
                    "alpha_unchanged": True, "palette_mapping_injective": True,
                    "source_png_sha256": hashlib.sha256(raw).hexdigest(),
                    "output_png_sha256": hashlib.sha256(destination.read_bytes()).hexdigest()})
    manifest = {"date": "2026-10-09", "method": "Create original PNG palette recolor only",
                "source_mod": SOURCE_JAR.name, "image_gen_calls": 0, "resized": False,
                "redrawn": False, "scope": "samples only; no game/mod replacement", "files": records}
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    bundle = OUT / "wooden-and-iron-vault-pngs.zip"
    with zipfile.ZipFile(bundle, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for record in records:
            archive.write(OUT / record["output"], record["output"])
        archive.write(OUT / "manifest.json", "manifest.json")
    print(json.dumps({"materials": 2, "png_files": len(records), "bundle": str(bundle),
                      "alpha_unchanged": True, "color_boundaries_unchanged": True}))


if __name__ == "__main__":
    main()
