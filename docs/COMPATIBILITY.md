# Compatibility

## Version requirements

| Mod | Status | Minecraft | Forge | Create | Java |
|---|---|---|---|---|---|
| 1.2.0 | Beta | 1.20.1 | 47.4.10+ (47.x) | 6.0.6, 6.0.7, 6.0.8 | 17 |
| 1.1.2 | Stable | 1.20.1 | 47.2.32+ (47.x) | 6.0.4 | 17 |
| 1.1.1 | Stable, older release | 1.20.1 | 47.2.32+ (47.x) | 6.0.4 | 17 |

These ranges follow the respective version declarations. Minecraft 1.21.1 and other loaders are outside this version line's support.

1.2.0 compiles against Create 6.0.6-150, Forge 47.4.10, Ponder 1.0.80 and Registrate MC1.20-1.3.3. One runtime JAR covers the listed Create versions. It includes the original Flywheel 1.0.5 library through Forge Jar-in-Jar and requires client Flywheel `[1.0.5,1.0.6)`; Forge and Create ranges are `[47.4.10,48)` and `[6.0.6,6.0.9)`.

Create: Connected is optional, needed only for the standard-tier vertical silo and upgrades involving it. Select a Connected release compatible with Minecraft and Create. Its upstream [compatibility table](https://github.com/hlysine/create_connected#download) lists Create 6.0.6 as unsupported; this is a separate boundary from this mod's Create support. Optional combinations are not assumed tested.

Do not load this continuation with original Bigger Storage or the `createbiggerstoragetocreate6` bridge patch; they share or overlap the `create_bs` installation role.

## Completed 1.2.0 checks

| Create | Forge | Flywheel | Ponder | Compilation, initialization and data generation |
|---|---|---|---|---|
| 6.0.6-150 | 47.4.10 | 1.0.5 | 1.0.80 | Passed; generated resources unchanged |
| 6.0.7-281 | 47.4.10 | 1.0.5 | 1.0.91 | Passed; generated resources unchanged |
| 6.0.8-289 | 47.4.10 | 1.0.5 | 1.0.91 | Passed; generated resources unchanged |

Jar-in-Jar construction and Forge reobfuscation passed. Official Create distribution hashes were checked, and the actual Forge JarSelector 0.3.19 selected exactly one Flywheel 1.0.5 when each distribution was combined with the runtime mod JAR, including Create 6.0.6's older bundled candidate.

Initialization used Forge `runData` and Registrate; it did not exercise game frames or terminal interactions. Dependency selection does not establish full production-client startup or rendering acceptance. Build verification records are supplied separately with release artifacts.

## Gameplay verification boundary

1.2.0 remains Beta. A Create 6.0.8 / Forge 47.4.10 / Flywheel 1.0.5 modpack session loaded the mod, entered a world and completed shutdown saving; the user reported normal gameplay. The same environment exposed intermittent vault texture seams and client chunk-refresh exceptions. Investigation identified controller lookup during capability invalidation as a lifecycle defect. Initial user retesting of the development fix reports no issue so far; updated logs have not yet been reviewed. See [technical notes](TECHNICAL_NOTES.md#capability-invalidation-during-chunk-teardown).

Separate checks are still needed for repaired chunk-refresh behavior, crystal vault display, individual terminal/crafting combinations, comparators, loaded-inventory regrouping/upgrades, save/reload, contraption inventory restoration and matching Connected silo upgrades. Create 6.0.6 and 6.0.7 have no equivalent gameplay acceptance record.

A reviewed, user-provided Create 6.0.8 JFR comparison supports removal of the repeated threshold-switch compatibility-selection hotspot in `1.2.0-dev-threshold-perf1`. This is performance evidence for a development candidate, not full behavior acceptance or a guaranteed TPS gain. Details and sampling limits are recorded in [technical notes](TECHNICAL_NOTES.md#threshold-switch-compatibility-selection); published 1.2.0 artifacts and release status remain unchanged.

The subsequent `1.2.0-dev-packager-perf2` candidate shares unchanged vault summaries between packagers. Development compilation, initialization and packager-hook application passed on the three target Create versions. A Create 6.0.8 JFR retest confirms live participation of summary rebuilding and copying; independent performance benefit and complete arrival/cache correctness remain unverified. See [shared-summary notes](TECHNICAL_NOTES.md#shared-vault-summaries-for-packagers).

The `1.2.0-dev-diagnostics1` candidate adds manually started/stopped counters without changing the shared-cache strategy. Development compilation, initialization and packager counter-hook application passed on the three target Create versions. User-provided Create 6.0.8 output confirms manual recording/export and internally consistent cache/scan counts, demonstrating reduced repeated scans. Net performance gain remains unverified. Diagnostics1 omits repackager preparation/unwrap flow while covering its inherited summary queries and shared-handler package extraction; see [diagnostics](TECHNICAL_NOTES.md#manual-vault-diagnostics).

1.1.2 was accepted as stable and received user feedback that the existing gameplay environment worked normally. That feedback does not establish every optional-mod combination, measured FPS/TPS gains or gameplay acceptance for the three 1.2.0 combinations.

See [technical notes](TECHNICAL_NOTES.md) for timing and inventory semantics, and [release notes](../CHANGELOG.md) for changes by version.
