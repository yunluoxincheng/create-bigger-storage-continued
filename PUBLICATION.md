# Publication status

This is an unofficial continuation of LopyLuna/Create-Bigger-Storage.

Version 1.1.2 is a local inventory-notification and client-sync optimization
candidate. Compilation and packaging verification do not establish in-game
performance or Tom's Storage / AE / RS compatibility acceptance. Version 1.1.1
remains the previously accepted stable baseline. GitHub tag releases record
their build status separately from in-game acceptance. Modrinth and CurseForge
publication is not connected yet. See [INVENTORY_PERFORMANCE.md](INVENTORY_PERFORMANCE.md)
and [RELEASING.md](RELEASING.md).

Version 1.1.0 is designated as a local stable version following the user's
acceptance of the final artwork. It retains the approved v3 clear crystal
windows and all other v3 resources. See [RELEASE_NOTES.md](RELEASE_NOTES.md).
This designation is not a public upload or a new in-game validation record.

Version 1.1.1 is now accepted as the local stable release after independent
client feedback and acceptance of the connectivity repair. The release keeps
the verified Minecraft 1.20.1 / Forge
47.2.32 / Create 6.0.4 scope; other version adaptations remain planned.
The accepted 1.1.0 artifacts are preserved. Following the user's clarification,
the current batch retains Create texture motifs and uses Minecraft materials
only for color depth, with sparse shared shading, tinted frames and translucent
blue crystal panels. Block-specific motifs from the previous study are removed.

- The original LICENSE remains unchanged and credits LopyLuna (2025).
- Code modifications and new code are maintained under the upstream MIT code terms.
- Upstream main was checked on 2026-10-08 at commit `3429e16d51ca91f2b8f7d46d58e5add18e56537d`. Its metadata declares `MIT-CODE, ARR-ART`; no separate artwork permission was found among its 205 files. The root MIT file is not treated as confirmation that its PNGs can be reused publicly.
- The current 108 vault PNGs preserve Create 6.0.4 texture structure, size and connected-texture atlas layout. Original panel shading is mapped to material colors with sparse common tonal variations, and frames and accents are recolored per material. No corresponding Minecraft block motifs are drawn. Ordinary tiers retain source alpha; crystal panels retain blue facets with partial alpha and translucent rendering. They do not use Bigger Storage's ARR PNGs. The previously replaced mod logo remains separate from this texture batch.
- The local Create jar includes `LICENSE_create-1.20.1`, an MIT license with Copyright (c) 2019 simibubi. That complete notice is preserved in the PNG bundle and in the mod jar as `META-INF/LICENSE-Create.txt`. Vault artwork is attributed to simibubi and the Create contributors; these modifications do not relicense Bigger Storage's ARR artwork.
- The 11 component icons and 45 upgrade icons are drawn directly in Python on native 16x16 grids. Material color palettes are sampled from local vanilla Minecraft 1.20.1 textures; no vanilla sprite geometry or PNG files are redistributed. Reference entries and hashes are recorded in the export manifest. This batch uses no image generation or earlier AI artwork as drawing inputs. New Python-drawn artwork is provided under this project's MIT terms to the extent applicable.
- Current methods, attribution, previews and export hashes are documented in [artwork/create-colors-v5/README.md](artwork/create-colors-v5/README.md). Previous studies, AI sources and the 1.1.0 artwork remain local historical archives described in [artwork/README.md](artwork/README.md), rather than part of the current source submission. Retired construction layers are unreferenced by the 56 item models and excluded from the 1.1.1 jar; files are preserved locally.
- Upstream artwork remains in the original private local Git history and an ignored local backup. The public GitHub repository starts with a new root commit from the approved 1.1.1 source tree, followed by current development; it has no parent links to that private history. Private history and backups are not pushed or packaged. The original local history is preserved rather than rewritten.
- Create: Connected interoperability uses its registry ID and public inventory accessor. Its AGPL implementation and artwork have not been copied.
- All 56 component and upgrade item models reference single Python-drawn PNGs; none references the earlier layered artwork or redistributes a vanilla Minecraft PNG.
- The public source repository is https://github.com/yunluoxincheng/create-bigger-storage-continued. Its current development/default branch is `1.20.1`; later Minecraft versions use independent branches and mod version sequences. New annotated tags use `mc<Minecraft>-v<mod>` and alone trigger automatic builds/releases, with changelogs in `changelog/<Minecraft>/<mod>.md`. Normal pushes and PRs do not build. Manual builds remain available; `LATEST_MINECRAFT_BRANCH` selects which Minecraft line may update the latest stable release. The published `v1.1.1` and `v1.1.2` tags are preserved. Version 1.1.2 remains beta; builds do not establish in-game acceptance. Modrinth/CurseForge uploads are deferred.

Before a public release, record the supported Minecraft/loader/Create matrix from
actual version verification, preserve origin and contributor attribution, and
complete in-world inventory conversion and reload acceptance.
