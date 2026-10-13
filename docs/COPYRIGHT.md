# Copyright and publication review

Reviewed on **2026-10-14**. This record distinguishes upstream copyright permission, asset provenance, platform policy, and completed checks. It is not a record of author endorsement or platform approval.

## Upstream code and unofficial continuation

The imported Bigger Storage source is from [LopyLuna/Create-Bigger-Storage](https://github.com/LopyLuna/Create-Bigger-Storage) at commit `3429e16d51ca91f2b8f7d46d58e5add18e56537d`.

- Its [LICENSE](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/LICENSE) grants MIT permission to modify, publish and distribute code, provided the copyright and permission notices are retained.
- Its [mods.toml](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/src/main/resources/META-INF/mods.toml) declares `MIT-CODE, ARR-ART`. The code grant must not be extended to the original artwork.
- Under these code terms, a compliant unofficial continuation does not require a separate case-by-case permission for code modification or redistribution. This project does not claim personal permission or endorsement from LopyLuna.
- Original ARR artwork may not be reused or adapted without a separate applicable permission. Attribution alone does not supply that permission.
- Attribution identifies the original source and must not imply that LopyLuna participates in or approves this continuation. Brand names and endorsement are separate from the MIT code grant.

The original LopyLuna copyright and MIT text remain unchanged in [LICENSE](../LICENSE) and are included in binaries and source archives as `LICENSE_UPSTREAM`.

## Source inventory

The machine-readable [asset source inventory](assets/source-inventory.json) links each asset category to its source, license notice and evidence. It includes the separate [icon provenance](assets/icon-provenance.json).

| Category | Source and evidence | Treatment |
|---|---|---|
| Bigger Storage code | Fixed upstream commit and MIT notice | Preserve attribution and full notice |
| 108 vault PNGs | Create 6.0.4 artwork; input hashes and recoloring records in the texture manifest | Preserve Create MIT notice; do not use Bigger Storage ARR PNGs |
| 56 component and upgrade PNGs | Python integer drawing; vanilla images used for color sampling only | Record procedural origin and AI assistance without claiming human-only creation |
| Vault base model | Geometry and UV match Create after excluding functional `cullface` fields | Record Create source and notice |
| Tier model wrappers | Parent/texture mappings to the base models and current textures | Treat as functional resource mappings; do not infer permission for arbitrary upstream art |
| Vertical base model | One axis-aligned cube with explicit face mappings | Maintain source in this repository |
| Current project icon | Composition of existing iron/copper vault faces | Preserve input hashes, Create notice and transform recipe |
| Flywheel 1.0.5 | Original nested library, MIT notice crediting Jozufozu | Preserve its original package identity and full notice |
| Create: Connected interoperability | Registry lookup and inventory access; no nested Connected library | Do not copy or redistribute its AGPL implementation or artwork |

The vault/component texture manifest covers **164 textures** and excludes the separately tracked project icon. Vanilla color sampling does not distribute a vanilla JAR or original vanilla sprites.

## Completed publication checks

The review downloaded the actual GitHub runtime and source JARs for **1.1.1, 1.1.2 and 1.2.0**, six archives in total, and checked their hashes against GitHub's recorded asset digests.

- All six contain the complete LopyLuna MIT notice and Create MIT notice.
- The 1.2.0 runtime JAR contains the original Flywheel 1.0.5 library and its MIT notice.
- No exact copy of the fixed upstream's 109 PNGs was found in the six archives or the public Git objects reviewed at commit `85522a7`.
- All 164 current vault/component PNGs match their recorded export hashes; all 108 Create texture inputs and the Create input JAR match their provenance hashes.
- The horizontal base model's geometry and UV match Create when functional face-culling fields are excluded.

Different hashes alone do not prove independent creation: cropping or recoloring can still make a derivative work. The source and generation records were reviewed alongside the hash checks. These checks do not establish rights to unidentified external assets or certify approval by a distribution platform.

Published archives contain the previous AI-derived icon. The current replacement is a development-tree resource change and does not overwrite those archives or move their tags.

## Modrinth

Source: [Content Rules](https://modrinth.com/legal/rules), checked on 2026-10-14.

Section 4 permits license-abiding forks that have diverged substantially and requires meaningful source attribution. The vertical vaults, upgrades, performance changes and compatibility work must be described accurately; a renamed or unchanged reupload must not be presented as a distinct project.

Section 6 requires relevant AI-use disclosures, prohibits project-page images created or derived from generative AI output, and prohibits projects primarily or entirely comprised of AI output. The earlier AI-derived icon must not be used for the project page. A Python drawing or composition must not automatically be labeled human-made or treated as a way to bypass this rule.

**Status: not submitted; approval not established.** The replacement has licensed texture inputs and no generated-image inputs, but its composition instructions, code maintenance and page writing were AI-assisted. Review the actual contribution history, make required disclosures and assess the current policy before submission. There is no measured or certified human/AI authorship percentage.

## CurseForge

Sources: [Moderation Policies](https://support.curseforge.com/en/support/solutions/articles/9000197279-moderation-policies) and [Copyright Claim Policy](https://support.curseforge.com/en/support/solutions/articles/9000222335-submitting-a-copyright-claim-on-curseforge), checked on 2026-10-14.

The moderation policy allows a fork permitted by its source license, requires credit and links to the original creator, and requires an accurate description of the differences. Its copyright policy states that CurseForge uses the on-platform license when it conflicts with an external license for the same work.

**Status: user-confirmed MIT; independent retrieval unavailable.** On 2026-10-14 the user checked the original project's current [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/create-bigger-storage) and reported its License field as MIT. The agent's HTTP retrieval returned 403 and its browser connections could not read the page. This is a user observation, not an independently captured page or approval by the platform. Together with the verified upstream code license, it supplies no evidence of a restrictive code-license conflict requiring separate author permission. Continue respecting the explicit upstream ARR artwork boundary and recheck the entry if publication rules or licenses change.

## Follow-up requirements

- Retain full notices in runtime/source packages and attribution in the public description.
- Keep original ARR artwork and the private upstream-art history out of public source and packages.
- Keep the current source inventory, icon recipe and hashes synchronized with resource changes.
- Preserve the user's CurseForge MIT confirmation and recheck the page at submission if direct access becomes available.
- Review Modrinth AI rules and disclosures against the real project history before submitting.
- Contact the original author only when the user authorizes it. Community courtesy is distinct from an additional legal requirement under the MIT code terms; do not invent an approval record.
