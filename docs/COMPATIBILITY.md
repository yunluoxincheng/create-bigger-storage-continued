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

The reissued 1.2.0 prerelease packages the completed R2 rebuilding, C3 deferred-copy and N1 scoped-NBT implementations, eligible full-package scan optimization, diagnostics and chunk-invalidation repair described below. It remains Beta. Its Java implementation is unchanged from the final local 1.2.0 build; the recorded source contracts, bytecode checks, measured limitations and gameplay boundaries apply. References below to unchanged published 1.2.0 artifacts describe the earlier development stages before this reissue. Publication does not establish new modpack or third-party Mixin acceptance.

1.2.0 remains Beta. A Create 6.0.8 / Forge 47.4.10 / Flywheel 1.0.5 modpack session loaded the mod, entered a world and completed shutdown saving; the user reported normal gameplay. The same environment exposed intermittent vault texture seams and client chunk-refresh exceptions. Investigation identified controller lookup during capability invalidation as a lifecycle defect. Initial user retesting of the development fix reports no issue so far; updated logs have not yet been reviewed. See [technical notes](TECHNICAL_NOTES.md#capability-invalidation-during-chunk-teardown).

Broader checks are still needed for repaired chunk-refresh behavior, crystal vault display, individual terminal/crafting combinations, comparators, loaded-inventory regrouping/upgrades, save/reload, contraption inventory restoration and matching Connected silo upgrades. The limited R2 manual acceptance below covers inventory preservation through save/reload, regrouping and successful upgrades in the tested scene. Create 6.0.6 and 6.0.7 have no equivalent gameplay acceptance record.

A reviewed, user-provided Create 6.0.8 JFR comparison supports removal of the repeated threshold-switch compatibility-selection hotspot in `1.2.0-dev-threshold-perf1`. This is performance evidence for a development candidate, not full behavior acceptance or a guaranteed TPS gain. Details and sampling limits are recorded in [technical notes](TECHNICAL_NOTES.md#threshold-switch-compatibility-selection); published 1.2.0 artifacts and release status remain unchanged.

The subsequent `1.2.0-dev-packager-perf2` candidate shares unchanged vault summaries between packagers. Development compilation, initialization and packager-hook application passed on the three target Create versions. A Create 6.0.8 JFR retest confirms live participation of summary rebuilding and copying; independent performance benefit and complete arrival/cache correctness remain unverified. See [shared-summary notes](TECHNICAL_NOTES.md#shared-vault-summaries-for-packagers).

The `1.2.0-dev-diagnostics1` candidate adds manually started/stopped counters without changing the shared-cache strategy. Development compilation, initialization and packager counter-hook application passed on the three target Create versions. User-provided Create 6.0.8 output confirms manual recording/export and internally consistent cache/scan counts, demonstrating reduced repeated scans. Net performance gain remains unverified. Diagnostics1 omits repackager preparation/unwrap flow while covering its inherited summary queries and shared-handler package extraction; see [diagnostics](TECHNICAL_NOTES.md#manual-vault-diagnostics).

The `1.2.0-dev-summary-r2-1` implementation rereads dirty vault slots and rebuilds affected Item groups while retaining independent outgoing summaries. Compilation and Forge data-generation initialization passed with Create 6.0.6-150, 6.0.7-281 and 6.0.8-289; generated resources have no Git differences. On 2026-10-15, the user accepted R2 rebuilding for normal use on the development branch after Create 6.0.8 manual checks of inventory updates, differently named stacks, save/reload, split/merge, successful material upgrades and idle release/cold-start recovery. Reviewed reports cover both scan paths and record no rebuild/copy failures or discarded builds; the user confirmed quantity/name preservation in the manual scenarios. Retained-cache memory, net FPS/TPS gain, exceptional rollback, third-party capability behavior and equivalent 6.0.6/6.0.7 gameplay remain outside this acceptance. See [incremental rebuilding](TECHNICAL_NOTES.md#incremental-vault-summary-rebuilding) for evidence and the stable-read boundary. Copy optimization proceeds separately in the C3 development implementation below. Published 1.2.0 artifacts and release status remain unchanged.

The `1.2.0-dev-copy-c3-1` implementation defers independent copies for internal stock checks on native packagers and repackagers attached to this mod's vaults. Public queries, real arrival differences and saved baselines retain independent data. Unknown subclasses and retired-handler reads copy eagerly. Diagnostics use schema 3 with separate query attempts, copy reasons and deferred ownership events; historical schema-2 query/copy equalities do not apply. Compilation, Forge data-generation initialization and transformed C3 packager hooks passed on all three target Create versions. Two existing Create 6.0.8 isolated GameTest selections passed for partial-stack material packaging/receipt and ten-repackager fragment assembly, with internally consistent schema-3 reports. These did not exercise input-mode queries or promise-queue arrival differences. See [deferred internal copies](TECHNICAL_NOTES.md#deferred-internal-summary-copies). This development implementation does not extend the earlier R2 gameplay acceptance to C3, packaged-client/modpack use, third-party Mixins, unusual capability callbacks or modpack-level performance gains.

Additional controlled-source Create 6.0.8 GameTests subsequently passed eleven C3 summary contracts in each of two JVMs, including mode behavior, public NBT/capability isolation, promise differences, historical saves, copy-failure/reentrancy recovery and recording boundaries. Fixed-work source comparisons against pre-C3 R2 confirm reduced copies/allocation for internal checks and limited public demand, while identifying extra cost on unchanged local hits and full-materialization controls. Physical connectivity/transport, general third-party compatibility and packaged-client/modpack performance acceptance remain separate.

The follow-up C3 path implementation passed seventeen controlled-source contracts and transformed-hook checks on Create 6.0.6-150, 6.0.7-281 and 6.0.8-289. Separate first-C3/optimized-C3 comparisons show reduced unchanged-hit time/allocation, with no uniform full-materialization latency improvement. The 6.0.7-281 slim development dependency logged four malformed optional Immersive Engineering recipe files containing merge-conflict markers; startup and the focused contracts completed, but this is not an acceptance of those recipes. The new candidate has not been installed in a client or modpack. See [deferred internal copies](TECHNICAL_NOTES.md#deferred-internal-summary-copies) for the measured boundary.

The subsequent scoped NBT-copy implementation passed twenty-three controlled-source contracts on all three target Create versions, including standard tags, typed empty lists, mutable arrays, custom tag subclasses, stack/capability preservation and nested/exceptional scope cleanup. Controlled comparisons against the preceding C3 sources show reduced allocation and improved complex-materialization/queued-arrival timings, with warmup-sensitive small-scene results and slower unchanged-hit controls. The implementation retains the actual native `ItemStack.copy()` call; ordinary unscoped copying delegates to the original operation. Source tests and production-mapping bytecode checks do not establish compatibility with other mods replacing base NBT copy behavior or the same call site, packaged-client/modpack use or a guaranteed FPS/TPS gain. See the [scoped-copy details and measurement limits](TECHNICAL_NOTES.md#deferred-internal-summary-copies).

On 2026-10-17, the user closed further summary-copy optimization and authorized a local checkpoint commit and an ordinary versioned 1.2.0 build combining R2, C3 and the scoped NBT kernel. This closes the optimization development phase; existing gameplay and performance verification boundaries remain as recorded above. The local build does not change published 1.2.0 artifacts, tags or Beta status.

1.1.2 was accepted as stable and received user feedback that the existing gameplay environment worked normally. That feedback does not establish every optional-mod combination, measured FPS/TPS gains or gameplay acceptance for the three 1.2.0 combinations.

The development `1.2.0-dev-packager-fullscan-1` implementation also passed 19 isolated packager GameTest cases on each of Create 6.0.6, 6.0.7 and 6.0.8: six iron cases, ten mixed-inventory cases, two crafting-context fallbacks and one named-stack fallback, with 318 packages checked and drained in total. A further Create 6.0.8 run without test input-order control passed nineteen cases and drained another 106 packages. These runs use the production scan hooks, with local observation/input-order tools excluded from runtime JARs. They establish this focused server-side behavior, not packaged-client gameplay, full sustained-load acceptance, optional-mod compatibility or a net FPS/TPS gain. Scope and fallback rules are recorded in [full-package scanning](TECHNICAL_NOTES.md#full-package-source-scans). Published artifacts and release status remain unchanged.

On 2026-10-16, the user accepted the current development implementation's usability during a real Create 6.0.8 high-load order session and explicitly closed the rebuilding phase. Reviewed diagnostics show consistent query/build/copy counters and no build/copy failures or discarded builds. This adds real-load usability acceptance to the focused automated checks; it does not establish all optional-mod combinations, exact item conservation from flow counters, or a guaranteed FPS/TPS improvement. That acceptance predates C3 and does not establish C3 gameplay acceptance. Further validation is centered on this mod's diagnostics and actual errors under load; published 1.2.0 remains Beta.

See [technical notes](TECHNICAL_NOTES.md) for timing and inventory semantics, and [release notes](../CHANGELOG.md) for changes by version.
