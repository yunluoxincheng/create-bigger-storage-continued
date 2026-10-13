"""Validate versioned release inputs and package built artifacts using stdlib only."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import shutil
import sys
import tomllib
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
        "flywheel": properties["flywheel_version"],
        "ponder": properties["ponder_version"],
        "release_type": release_type,
        "release_name": f"Create: Bigger Storage Continued {version} — Minecraft {minecraft}",
        "changelog": changelog.relative_to(ROOT).as_posix(),
    }


def package(info: dict, artifact_dir: str = "build/libs") -> dict:
    stem = f"create-bigger-storage-continued-{info['minecraft']}-{info['version']}"
    artifacts = ROOT / artifact_dir
    runtime = artifacts / f"{stem}.jar"
    sources = artifacts / f"{stem}-sources.jar"
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
        bundled = json.loads(jar.read("META-INF/jarjar/metadata.json"))["jars"]
        if len(bundled) != 1:
            raise ValueError("Only the fixed Flywheel dependency should be bundled")
        dependency = bundled[0]
        if dependency["identifier"] != {
            "group": "dev.engine-room.flywheel",
            "artifact": f"flywheel-forge-{info['minecraft']}",
        } or dependency["version"]["artifactVersion"] != info["flywheel"]:
            raise ValueError("Bundled Flywheel does not match the development baseline")
        if dependency["version"]["range"] != "[1.0.5,1.0.6)":
            raise ValueError("Unexpected Flywheel selection range")
        embedded = jar.read(dependency["path"])
        with zipfile.ZipFile(io.BytesIO(embedded)) as library:
            if not library.read("META-INF/LICENSE.md").strip():
                raise ValueError("Missing embedded Flywheel license")
            if "dev/engine_room/flywheel/backend/engine/indirect/IndirectInstancer.class" not in library.namelist():
                raise ValueError("Flywheel backend is missing")
            library_mods = tomllib.loads(library.read("META-INF/mods.toml").decode("utf-8"))
            if not any(mod["modId"] == "flywheel" and mod["version"] == info["flywheel"]
                       for mod in library_mods["mods"]):
                raise ValueError("Embedded Flywheel mod version does not match metadata")
        mods = tomllib.loads(jar.read("META-INF/mods.toml").decode("utf-8"))
        flywheel_requirement = next(
            d for d in mods["dependencies"]["create_bs"] if d["modId"] == "flywheel")
        if not flywheel_requirement["mandatory"] or flywheel_requirement["side"] != "CLIENT" \
                or flywheel_requirement["versionRange"] != "[1.0.5,1.0.6)":
            raise ValueError("Client Flywheel requirement must enforce the fixed backend")
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
        "bundled_flywheel": {
            "version": info["flywheel"],
            "range": dependency["version"]["range"],
            "sha256": hashlib.sha256(embedded).hexdigest(),
            "license_preserved": True,
        },
        "build_verification": "jarJar and sourcesJar; jarJar finalized by Forge reobfJarJar",
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
    parser.add_argument("--artifact-dir", default="build/libs")
    args = parser.parse_args()
    info = metadata(args.tag, args.branch)
    if args.command == "check" and os.environ.get("GITHUB_OUTPUT"):
        with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as stream:
            for key in ("version", "minecraft", "release_tag", "release_type", "release_name"):
                stream.write(f"{key}={info[key]}\n")
    print(json.dumps(package(info, args.artifact_dir) if args.command == "package" else info,
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, zipfile.BadZipFile) as error:
        print(f"Release preparation failed: {error}", file=sys.stderr)
        sys.exit(1)
