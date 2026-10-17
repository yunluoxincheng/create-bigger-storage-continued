# Attribution, asset sources and publication

This is an unofficial continuation of [LopyLuna's Bigger Storage](https://github.com/LopyLuna/Create-Bigger-Storage), without claimed affiliation or endorsement from LopyLuna or the Create team. Copyright permission and platform approval are separate.

## Project identity and maintainership

- **Original project and author:** [Create: Bigger Storage](https://github.com/LopyLuna/Create-Bigger-Storage) by [LopyLuna](https://github.com/LopyLuna). Original authorship and the upstream MIT copyright notice are retained.
- **Current continuation maintainer:** [yunluoxincheng](https://github.com/yunluoxincheng), with Continued contributors. This credit identifies maintenance of this continuation and does not attribute the original project to its current maintainer.
- **Continuation homepage and source:** [Create: Bigger Storage Continued](https://github.com/yunluoxincheng/create-bigger-storage-continued).
- **Continuation issue tracker:** [GitHub Issues](https://github.com/yunluoxincheng/create-bigger-storage-continued/issues).

Project pages should credit LopyLuna as the original author and yunluoxincheng as the current continuation maintainer. The original author and Create team are not represented as participants in or endorsers of this continuation. Maintenance code and documentation include AI-assisted contributions; the asset-specific AI assistance and source records are described below.

## Code and license scope

The imported source is commit `3429e16d51ca91f2b8f7d46d58e5add18e56537d`. Its [MIT license](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/LICENSE) permits compliant code modification and redistribution without a separate case-by-case author approval. Its [metadata](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/src/main/resources/META-INF/mods.toml) explicitly separates `MIT-CODE, ARR-ART`; the code grant does not authorize original ARR artwork or brand endorsement.

[LICENSE](../LICENSE) retains the LopyLuna 2025 copyright and complete MIT text. Runtime and source JARs include it as `LICENSE_UPSTREAM`. New maintenance code uses the MIT terms to the extent the contributors can grant the relevant rights. Attribution does not imply participation in this continuation. Original ARR artwork and the private history containing it are not published.

## Asset and dependency sources

The [source inventory](assets/source-inventory.json), [texture manifest](../artwork/create-colors-v5/manifest.json) and [icon provenance](assets/icon-provenance.json) record source identities, methods and hashes.

| Content | Source and treatment |
|---|---|
| 108 vault PNGs | Create 6.0.4 vault artwork, recolored with material palettes while retaining panel motifs and connected-texture layout. Preserve the [Create MIT notice](../src/main/resources/META-INF/LICENSE-Create.txt), copyright 2019 simibubi. |
| 56 component/upgrade PNGs | Python integer drawing; vanilla images supply color samples only. No vanilla sprites or client JAR are distributed. |
| Horizontal base model | Geometry and UV correspond to Create's vault model when functional `cullface` fields are excluded; retain Create source attribution. |
| Tier/vertical models | Functional parent/texture mappings and the repository's one-cube vertical face mapping. This does not imply permission for arbitrary upstream artwork. |
| Project icon | Composition of current iron/copper vault faces; method and inputs recorded below. |
| Flywheel 1.0.5 | Original MIT library credited to Jozufozu (2021–2024), nested under its original Maven identity with `META-INF/LICENSE.md` intact. |
| Connected interoperability | Registry lookup and inventory access; its AGPL implementation/artwork are not copied or bundled. |

The 164-entry texture manifest excludes the project icon. The four PNGs in `artwork/python-upgrade-samples/` are generation inputs for approved silhouette checks and remain tracked. Historical experiments and review boards are local artifacts.

## Project icon

![Texture-based project icon](assets/project-icon.png)

The current 400×400 transparent icon composes iron and copper vault front, side and top textures. Python/Pillow performs nearest-neighbor projection and arrangement; no generated-image bitmap or previous AI logo is an input. The instructions and composition were AI-assisted, so this is not a claim of human-only authorship or platform approval.

`src/main/resources/pack.png` and `docs/assets/project-icon.png` are byte-identical. The [provenance record](assets/icon-provenance.json) stores six input hashes, original Create texture paths, placements, outline settings and output hashes.

To reproduce the icon: start with an 80×80 transparent RGBA canvas; process the recorded faces in order; map each destination pixel center into the parallelogram defined by the first, second and fourth vertices; sample the 16×16 source with integer nearest-neighbor coordinates. Draw each cube's one-pixel outline after its faces, with iron before copper. Resize to 400×400 by nearest neighbor and save lossless RGBA PNGs to the recorded paths.

Stable 1.1.1 and 1.1.2 artifacts and the original 1.2.0 prerelease used the previous AI-derived icon. The replacement is included in the user-authorized [1.2.0 prerelease reissue](../changelog/1.20.1/1.2.0.md). Stable historical artifacts remain unchanged.

## Completed source review

On 2026-10-14, the actual GitHub runtime and source JARs for 1.1.1, 1.1.2 and 1.2.0 were checked against GitHub asset digests. All six retained full LopyLuna and Create MIT notices; the 1.2.0 nested Flywheel retained its notice. No exact copy of the original 109 upstream PNGs was found in those archives or the public Git objects reviewed at `85522a7`.

All 164 current texture output hashes and all 108 Create input hashes matched their provenance; the input Create JAR hash also matched. The base model geometry/UV comparison was checked separately. A different hash alone does not establish independent creation; source and transformation records must be considered. These checks do not certify rights to unidentified assets or platform approval.

## Platform publication

The [Modrinth rules](https://modrinth.com/legal/rules), reviewed on 2026-10-14, permit license-abiding substantially diverged forks with meaningful attribution. They also require relevant AI disclosures, restrict AI-derived images on project pages and prohibit projects primarily or entirely comprised of AI output. A Python drawing, pixelation or composition must not automatically be labeled human-made or treated as a policy workaround. Review the real contribution history and current rules before submission; no human/AI authorship percentage or approval has been certified.

[CurseForge moderation](https://support.curseforge.com/en/support/solutions/articles/9000197279-moderation-policies) allows license-permitted forks with attribution and an accurate description of their differences. Its [copyright policy](https://support.curseforge.com/en/support/solutions/articles/9000222335-submitting-a-copyright-claim-on-curseforge) uses the on-platform license if it conflicts with an external license for the same work. On 2026-10-14 the user checked the original project's page and reported MIT; independent retrieval returned HTTP 403. Record that evidence distinction and recheck at submission if access is available. Continue respecting the explicit ARR artwork boundary.

Neither platform is configured or approved for this continuation. Public descriptions must state the actual changes and unofficial identity, retain attribution and make applicable disclosures. Contact the original author only when authorized; courtesy communication and the MIT code grant are distinct.
