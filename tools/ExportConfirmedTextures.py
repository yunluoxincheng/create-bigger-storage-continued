"""Export Create vault recolors and the approved native Python component style.

No generated artwork is read. The --install option backs up each named runtime
file before replacing it; the installed Minecraft modpack is never modified.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import shutil
import zipfile

from PIL import Image, ImageDraw

import DrawUpgradeIconSamples as icons
import RecolorCreateVaultSamples as vaults
import VaultMaterialDetails as details

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "artwork/create-colors-v5"
TIERS = tuple(icons.PALETTES)
VAULT_TIERS = tuple(tier for tier in TIERS if tier != "standard")
BASE_PALETTE = icons.BASE_PALETTE
PREVIEW_SUFFIX = "-create-colors-v5"
VAULT_PALETTES = {}
VAULT_REFERENCES = []


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def component(tier: str | None) -> Image.Image:
    """One centered material plate, using the approved plate geometry and shading."""
    dark, body, light = BASE_PALETTE if tier is None else icons.PALETTES[tier]
    plate = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(plate)
    draw.polygon(((5, 3), (12, 2), (14, 5), (14, 11), (8, 13), (5, 11)), fill=icons.OUTLINE)
    draw.polygon(((6, 4), (11, 3), (13, 5), (13, 10), (8, 11), (6, 10)), fill=body)
    draw.line((6, 4, 11, 3), fill=light)
    draw.line((8, 12, 13, 10), fill=dark)
    icons.texture_plate(plate, body, (
        (icons.mix(body, light, 0.35), ((7, 5), (8, 5), (7, 6), (8, 6), (9, 6))),
        (icons.mix(body, dark, 0.4), ((10, 8), (11, 8), (12, 8), (11, 9), (12, 9), (10, 10))),
        (icons.mix(body, light, 0.7), ((7, 5), (8, 6))),
    ))
    for y in (6, 10):
        draw.rectangle((4, y, 6, y + 1), fill=icons.BRASS_DARK)
        draw.line((4, y, 6, y), fill=icons.BRASS_LIGHT)
    result = Image.new("RGBA", (16, 16))
    result.alpha_composite(plate, (-2, 1))
    return result


def save(image: Image.Image, relative: str, records: list, **metadata) -> None:
    path = OUT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, format="PNG", optimize=True)
    records.append({"path": relative, "size": list(image.size),
                    "sha256": digest(path.read_bytes()), **metadata})


def export_vaults(records: list) -> bytes:
    global VAULT_PALETTES, VAULT_REFERENCES
    with zipfile.ZipFile(icons.materials.SOURCE_JAR) as vanilla:
        VAULT_PALETTES, VAULT_REFERENCES = details.load_palettes(vanilla)
    with zipfile.ZipFile(vaults.SOURCE_JAR) as archive:
        entries = sorted(name for name in archive.namelist()
                         if name.startswith(vaults.PREFIX) and name.endswith(".png"))
        if len(entries) != 12:
            raise ValueError("Expected 12 original Create vault textures")
        license_bytes = archive.read("LICENSE_create-1.20.1")
        sources = {entry: Image.open(io.BytesIO(archive.read(entry))).convert("RGBA") for entry in entries}
        colors = {pixel for source in sources.values() for pixel in source.get_flattened_data() if pixel[3]}
        panel_luma = [icons.materials.luminance(pixel) for pixel in colors if details.panel(pixel)]
        frame_luma = [icons.materials.luminance(pixel) for pixel in colors if not details.panel(pixel)]
        limits = ((min(panel_luma), max(panel_luma)), (min(frame_luma), max(frame_luma)))
        for entry in entries:
            raw = archive.read(entry)
            source = sources[entry]
            pixels = list(source.get_flattened_data())
            for tier in VAULT_TIERS:
                target = details.render(source, tier, Path(entry).name, VAULT_PALETTES[tier], limits)
                alpha_unchanged = target.getchannel("A").tobytes() == source.getchannel("A").tobytes()
                if tier != "crystal" and not alpha_unchanged:
                    raise ValueError("Original Create alpha changed")
                if any(a != b for a, b in zip(pixels, target.get_flattened_data()) if not a[3]):
                    raise ValueError("Transparent atlas RGB changed")
                save(target, f"assets/create_bs/textures/block/{tier}_vault/{Path(entry).name}",
                     records, kind="vault", material=tier, source=entry,
                     source_sha256=digest(raw), alpha_unchanged=alpha_unchanged,
                     palette_mapping_injective=False, detail_method="original Create shading with sparse shared color variation; no block motifs",
                     material_frame_recolored=True, translucent_panels=tier == "crystal",
                     alpha_values=sorted(set(target.getchannel("A").get_flattened_data())),
                     colors=len(set(target.get_flattened_data())))
    return license_bytes


def export_items(records: list) -> dict:
    images = {"base_component": component(None)}
    images.update({tier + "_component": component(tier) for tier in TIERS})
    for index, source in enumerate(TIERS):
        for target in TIERS[index + 1:]:
            item_id = source + "_to_" + target + "_upgrade"
            images[item_id] = icons.draw_icon(source, target)
    if len(images) != 56:
        raise ValueError("Expected 11 components and 45 upgrades")
    for item_id, image in images.items():
        colors = {pixel for pixel in image.get_flattened_data() if pixel[3]}
        alpha = set(image.getchannel("A").get_flattened_data())
        if image.size != (16, 16) or len(colors) > 16 or not alpha <= {0, 255}:
            raise ValueError(f"Invalid native icon: {item_id}")
        if item_id.endswith("_upgrade"):
            source, target = item_id.removesuffix("_upgrade").split("_to_")
            if (source, target) in icons.PAIRS:
                approved = Image.open(icons.OUT / (item_id + ".png")).convert("RGBA")
                if image.getchannel("A").tobytes() != approved.getchannel("A").tobytes():
                    raise ValueError("Approved upgrade silhouette changed")
        model = ROOT / "src/generated/resources/assets/create_bs/models/item" / (item_id + ".json")
        data = json.loads(model.read_text(encoding="utf-8"))
        if data != {"parent": "minecraft:item/generated",
                    "textures": {"layer0": "create_bs:item/" + item_id}}:
            raise ValueError(f"Unexpected item model: {item_id}")
        save(image, f"assets/create_bs/textures/item/{item_id}.png", records,
             kind="upgrade" if item_id.endswith("_upgrade") else "component",
             opaque_colors=len(colors), alpha=sorted(alpha))
    return images


def previews(images: dict) -> None:
    board = Image.new("RGBA", (864, 690), "#666b70")
    draw = ImageDraw.Draw(board)
    for index, tier in enumerate(VAULT_TIERS):
        x, y = index % 3 * 288, index // 3 * 230
        draw.text((x + 16, y + 8), icons.NAMES[tier] + "保险库", font=icons.font(20), fill="white")
        for offset, face in enumerate(("front", "side", "top", "bottom")):
            texture = Image.open(OUT / f"assets/create_bs/textures/block/{tier}_vault/vault_{face}_small.png").convert("RGBA")
            position = (x + 8 + offset * 68, y + 50)
            if tier == "crystal":
                checkerboard(board, position, (64, 64))
            board.alpha_composite(texture.resize((64, 64), Image.Resampling.NEAREST), position)
            draw.text((x + 25 + offset * 68, y + 120), ("正面", "侧面", "顶面", "底面")[offset], font=icons.font(13), fill="white")
        texture = Image.open(OUT / f"assets/create_bs/textures/block/{tier}_vault/vault_front_small.png").convert("RGBA")
        if tier == "crystal":
            checkerboard(board, (x + 16, y + 154), (64, 64))
        board.alpha_composite(texture.resize((64, 64), Image.Resampling.NEAREST), (x + 16, y + 154))
        draw.text((x + 94, y + 165), "MC材料颜色层次\n" + ("半透明蓝水晶面板" if tier == "crystal" else "Create纹理与同色框架"), font=icons.font(14), fill="#e0e4e7")
    board.save(OUT / ("vaults" + PREVIEW_SUFFIX + ".png"), optimize=True)
    board = Image.new("RGBA", (864, 344), "#666b70")
    draw = ImageDraw.Draw(board)
    for index, tier in enumerate((None, *TIERS)):
        x, y = index % 6 * 144, index // 6 * 172
        image = images["base_component" if tier is None else tier + "_component"]
        draw.text((x + 12, y + 8), "基础组件" if tier is None else icons.NAMES[tier] + "组件", font=icons.font(16), fill="white")
        board.alpha_composite(image.resize((96, 96), Image.Resampling.NEAREST), (x + 8, y + 35))
        board.alpha_composite(image.resize((32, 32), Image.Resampling.NEAREST), (x + 104, y + 93))
        draw.text((x + 100, y + 132), "32px", font=icons.font(12), fill="#e0e4e7")
    board.save(OUT / ("components" + PREVIEW_SUFFIX + ".png"), optimize=True)
    board = Image.new("RGBA", (1040, 1016), "#666b70")
    draw = ImageDraw.Draw(board)
    draw.text((16, 12), "45种升级组件 · 目标材质箭头 · 4像素箭杆", font=icons.font(22), fill="white")
    for index, tier in enumerate(TIERS):
        draw.text((92 + index * 94, 54), icons.NAMES[tier], font=icons.font(14), fill="white")
        draw.text((6, 116 + index * 92), icons.NAMES[tier], font=icons.font(14), fill="white")
    for start, source in enumerate(TIERS):
        for end in range(start + 1, len(TIERS)):
            image = images[source + "_to_" + TIERS[end] + "_upgrade"]
            board.alpha_composite(image.resize((80, 80), Image.Resampling.NEAREST), (92 + end * 94, 88 + start * 92))
    board.save(OUT / ("upgrades" + PREVIEW_SUFFIX + ".png"), optimize=True)
    crystal_preview()
    material_preview()


def checkerboard(board: Image.Image, position: tuple, size: tuple) -> None:
    draw = ImageDraw.Draw(board)
    x0, y0 = position
    for y in range(0, size[1], 8):
        for x in range(0, size[0], 8):
            color = "#a4a9ad" if (x // 8 + y // 8) % 2 else "#72797f"
            draw.rectangle((x0 + x, y0 + y, x0 + min(x + 7, size[0] - 1),
                            y0 + min(y + 7, size[1] - 1)), fill=color)


def material_preview() -> None:
    sides = Image.new("RGBA", (864, 690), "#666b70")
    connected = Image.new("RGBA", (960, 696), "#666b70")
    for index, tier in enumerate(VAULT_TIERS):
        x, y = index % 3 * 288, index // 3 * 230
        ImageDraw.Draw(sides).text((x + 16, y + 12), icons.NAMES[tier] + "侧面细节", font=icons.font(20), fill="white")
        path = OUT / f"assets/create_bs/textures/block/{tier}_vault/vault_side_small.png"
        image = Image.open(path).convert("RGBA")
        if tier == "crystal":
            checkerboard(sides, (x + 16, y + 58), (144, 144))
        sides.alpha_composite(image.resize((144, 144), Image.Resampling.NEAREST), (x + 16, y + 58))
        sides.alpha_composite(image.resize((32, 32), Image.Resampling.NEAREST), (x + 206, y + 116))
        ImageDraw.Draw(sides).text((x + 194, y + 157), "32px参考", font=icons.font(13), fill="white")
        x, y = index % 3 * 320, index // 3 * 232
        ImageDraw.Draw(connected).text((x + 12, y + 10), icons.NAMES[tier] + " 3×3正面 / 侧面", font=icons.font(17), fill="white")
        for offset, face in enumerate(("front", "side")):
            path = OUT / f"assets/create_bs/textures/block/{tier}_vault/vault_{face}_large.png"
            atlas = Image.open(path).convert("RGBA")
            pane = Image.new("RGBA", (48, 48))
            for row in range(3):
                for column in range(3):
                    col_index, row_index = (1, 2, 3)[column], (0, 1, 2)[row]
                    tile = atlas.crop((col_index * 16, row_index * 16, (col_index + 1) * 16, (row_index + 1) * 16))
                    pane.alpha_composite(tile, (column * 16, row * 16))
            position = (x + 10 + offset * 154, y + 56)
            if tier == "crystal":
                checkerboard(connected, position, (144, 144))
            connected.alpha_composite(pane.resize((144, 144), Image.Resampling.NEAREST), position)
    sides.save(OUT / "side-colors-v5.png", optimize=True)
    connected.save(OUT / "connected-colors-v5.png", optimize=True)


def crystal_preview() -> None:
    folder = OUT / "assets/create_bs/textures/block/crystal_vault"
    board = Image.new("RGBA", (864, 584), "#666b70")
    draw = ImageDraw.Draw(board)
    draw.text((20, 12), "蓝水晶保险库 · 半透明面板 · 大块晶面", font=icons.font(25), fill="white")
    draw.text((20, 52), "棋盘格仅用来展示透明区域，不在游戏贴图中", font=icons.font(16), fill="white")
    for index, face in enumerate(("front", "side", "top", "bottom")):
        image = Image.open(folder / ("vault_" + face + "_small.png")).convert("RGBA")
        position = (24 + index * 212, 112)
        checkerboard(board, position, (128, 128))
        board.alpha_composite(image.resize((128, 128), Image.Resampling.NEAREST), position)
        draw.text((position[0], 82), ("正面", "侧面", "顶面", "底面")[index], font=icons.font(18), fill="white")
    for face, size, x in (("front", 2, 24), ("front", 3, 264), ("side", 3, 540)):
        atlas = Image.open(folder / f"vault_{face}_{'medium' if size == 2 else 'large'}.png").convert("RGBA")
        pane = Image.new("RGBA", (size * 16, size * 16))
        for y in range(size):
            for column in range(size):
                atlas_x = 1 if column == 0 else 3 if column == size - 1 else 2
                atlas_y = 0 if y == 0 else 2 if y == size - 1 else 1
                tile = atlas.crop((atlas_x * 16, atlas_y * 16, (atlas_x + 1) * 16, (atlas_y + 1) * 16))
                pane.alpha_composite(tile, (column * 16, y * 16))
        enlarged = pane.resize((size * 64, size * 64), Image.Resampling.NEAREST)
        position = (x, 328)
        checkerboard(board, position, enlarged.size)
        board.alpha_composite(enlarged, position)
        draw.text((x, 290), f"{size}×{size}" + ("正面连接" if face == "front" else "侧面连接"), font=icons.font(18), fill="white")
    draw.text((20, 548), "Create RECTANGLE图集技术预览；实际横纵向外观待游戏验收", font=icons.font(16), fill="white")
    board.save(OUT / "blue-crystal-v5.png", optimize=True)


def install(records: list, license_bytes: bytes) -> None:
    resources = ROOT / "src/main/resources"
    backup = ROOT / "build/before-create-colors-v5"
    # Preserve an existing backup on repeat runs; never overwrite it.
    for record in records:
        relative = record["path"]
        destination = resources / relative
        prior = backup / relative
        if destination.is_file() and destination.read_bytes() == (OUT / relative).read_bytes():
            continue
        if destination.is_file() and not prior.exists():
            prior.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(destination, prior)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(OUT / relative, destination)
    license_path = resources / "META-INF/LICENSE-Create.txt"
    if license_path.is_file() and license_path.read_bytes() != license_bytes:
        raise ValueError("Existing Create license differs")
    license_path.write_bytes(license_bytes)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--install", action="store_true")
    args = parser.parse_args()
    enum_text = (ROOT / "src/main/java/uwu/lopyluna/create_bs/content/vault/UpgradeTier.java").read_text(encoding="utf-8")
    if tuple(re.findall(r'^\s+[A-Z]+\("([a-z]+)",', enum_text, re.MULTILINE)) != TIERS:
        raise ValueError("Python tier order differs from Java registry")
    OUT.mkdir(parents=True, exist_ok=True)
    records = []
    license_bytes = export_vaults(records)
    images = export_items(records)
    previews(images)
    if len(records) != 164:
        raise ValueError("Expected 108 vault and 56 item PNG files")
    manifest = {"method": "original Create texture motifs; vanilla material color depth only; sparse shared shading; translucent blue crystal; Python native16 items",
                "source_mod": vaults.SOURCE_JAR.name, "source_jar_sha256": digest(vaults.SOURCE_JAR.read_bytes()),
                "image_gen_calls": 0, "arrow_stem_pixels": 4, "tier_order": TIERS,
                "item_palettes": icons.PALETTES, "base_palette": BASE_PALETTE,
                "minecraft_source_jar": str(icons.materials.SOURCE_JAR),
                "minecraft_source_jar_sha256": digest(icons.materials.SOURCE_JAR.read_bytes()),
                "material_references": icons.MATERIAL_REFERENCES,
                "vault_material_references": VAULT_REFERENCES, "vault_palettes": VAULT_PALETTES,
                "approved_icon_geometry_preserved": True, "colors_revised_by_user_request": True,
                "crystal_window_style": "blue facets; panel alpha 128/148/168; opaque blue frame; translucent render layer; interior side rivets removed",
                "files": records}
    (OUT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (OUT / "LICENSE-Create.txt").write_bytes(license_bytes)
    with zipfile.ZipFile(OUT / "create-colors-v5-pngs.zip", "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for record in records:
            archive.write(OUT / record["path"], record["path"])
        for filename in ("manifest.json", "LICENSE-Create.txt", "README.md"):
            if (OUT / filename).is_file():
                archive.write(OUT / filename, filename)
    if args.install:
        install(records, license_bytes)
    print(json.dumps({"vault_pngs": 108, "upgrade_pngs": 45, "component_pngs": 11,
                      "approved_icon_geometry_preserved": True, "installed_in_project": args.install,
                      "output": str(OUT)}))


if __name__ == "__main__":
    main()
