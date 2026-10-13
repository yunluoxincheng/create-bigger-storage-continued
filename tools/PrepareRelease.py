"""Validate versioned release inputs and package built artifacts using stdlib only."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)?\Z")
MINECRAFT_VERSION = re.compile(r"[0-9]+\.[0-9]+(?:\.[0-9]+)?\Z")


def metadata(tag: str, branch: str = "") -> dict:
    properties = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith(("#", "!")) and "=" in line:
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    version = properties["mod_version"]
    minecraft = properties["minecraft_version"]
    if not VERSION.fullmatch(version) or not MINECRAFT_VERSION.fullmatch(minecraft):
        raise ValueError("Invalid mod or Minecraft version")
    release_tag = f"mc{minecraft}-v{version}"
    if tag and tag != release_tag:
        raise ValueError(f"Tag {tag!r} must match gradle.properties: {release_tag}")
    if branch and branch != minecraft:
        raise ValueError(f"Branch {branch!r} must match minecraft_version: {minecraft}")
    config = json.loads((ROOT / "release.json").read_text(encoding="utf-8"))
    release_type = config["release_type"]
    if release_type not in {"release", "beta", "alpha"}:
        raise ValueError("release_type must be release, beta or alpha")
    if "-" in version and release_type == "release":
        raise ValueError("A prerelease version cannot be marked release")
    changelog = ROOT / "changelog" / minecraft / f"{version}.md"
    notes = changelog.read_text(encoding="utf-8").strip()
    if not notes.startswith(f"# Create: Bigger Storage Continued {version}\n"):
        raise ValueError("The changelog heading must match the version")
    if len(notes.splitlines()) < 5:
        raise ValueError("A version-specific changelog is required")
    return {
        "version": version,
        "minecraft": minecraft,
        "release_tag": release_tag,
        "forge": properties["forge_version"],
        "create": properties["create_version"],
        "release_type": release_type,
        "release_name": f"Create: Bigger Storage Continued {version} — Minecraft {minecraft}",
        "changelog": changelog.relative_to(ROOT).as_posix(),
    }


def package(info: dict) -> dict:
    stem = f"create-bigger-storage-continued-{info['minecraft']}-{info['version']}"
    runtime = ROOT / "build" / "libs" / f"{stem}.jar"
    sources = ROOT / "build" / "libs" / f"{stem}-sources.jar"
    with zipfile.ZipFile(runtime) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate runtime jar entries")
        manifest = jar.read("META-INF/MANIFEST.MF").decode("utf-8")
        if f"Implementation-Version: {info['version']}\r\n" not in manifest:
            raise ValueError("Runtime jar version does not match the release")
        for license_name in ("LICENSE_UPSTREAM", "META-INF/LICENSE-Create.txt"):
            if not jar.read(license_name).strip():
                raise ValueError(f"Missing license: {license_name}")
        resources = [n for n in names if n.startswith(("assets/", "data/")) and not n.endswith("/")]
        roots = (ROOT / "src" / "main" / "resources", ROOT / "src" / "generated" / "resources")
        for name in resources:
            candidates = [base / name for base in roots if (base / name).is_file()]
            if not candidates or any(path.read_bytes() != jar.read(name) for path in candidates):
                raise ValueError(f"Runtime resource does not match source: {name}")
        retired = ("assets/create_bs/textures/item/core/", "assets/create_bs/textures/item/source/",
                   "assets/create_bs/textures/item/target/")
        if any(n.startswith(retired) or n.endswith(("/component_frame.png", "/upgrade_frame.png")) for n in names):
            raise ValueError("Retired icon layers must not be packaged")
    with zipfile.ZipFile(sources) as jar:
        if not jar.read("LICENSE_UPSTREAM").strip():
            raise ValueError("Missing source jar license")
    output = ROOT / "build" / "github-release" / info["minecraft"] / info["version"]
    output.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for path in (runtime, sources):
        target = output / path.name
        shutil.copyfile(path, target)
        hashes[path.name] = hashlib.sha256(target.read_bytes()).hexdigest()
    shutil.copyfile(ROOT / info["changelog"], output / "CHANGELOG.md")
    (output / "SHA256SUMS.txt").write_text(
        "".join(f"{digest}  {name}\n" for name, digest in hashes.items()), encoding="utf-8")
    record = {
        **info,
        "sha256": hashes,
        "runtime_resources_verified": len(resources),
        "duplicate_runtime_entries": 0,
        "licenses_verified": True,
        "build_verification": "jar and sourcesJar; jar finalized by Forge reobfJar",
        "in_game_acceptance_verified_by_ci": False,
    }
    (output / "verification.json").write_text(
        json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return record


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("check", "package"))
    parser.add_argument("--tag", default="")
    parser.add_argument("--branch", default="")
    args = parser.parse_args()
    info = metadata(args.tag, args.branch)
    if args.command == "check" and os.environ.get("GITHUB_OUTPUT"):
        with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as stream:
            for key in ("version", "minecraft", "release_tag", "release_type", "release_name"):
                stream.write(f"{key}={info[key]}\n")
    print(json.dumps(package(info) if args.command == "package" else info,
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        sys.exit(1)
