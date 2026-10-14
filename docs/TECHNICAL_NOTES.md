# Vault implementation notes

These notes describe inventory and structure behavior relevant to maintenance and automation. Version requirements and verification boundaries are maintained in [COMPATIBILITY.md](COMPATIBILITY.md).

## Inventory notifications and comparators

Since 1.1.2, real slot changes invalidate the current controller's comparator reading and queue notifications. A controller normally performs at most one structure-wide notification round per server tick; changes occurring during notification remain queued for a later round.

Comparator output uses Create's inventory redstone calculation and is shared across the structure until inventory, structure, NBT loading or capability changes invalidate it. Incomplete structures must not cache a temporary zero result.

Notifications may wait until the next controller tick, and intermediate changes within one tick are combined. Terminal inventory reads, writes and Create inventory-version updates remain immediate; the batching does not defer the inventory operation itself.

## Saving and client synchronization

- A changed member's chunk is marked for saving immediately through `blockEntityChanged`, avoiding the comparator work attached to `setChanged`. Repeated marks in a tick are combined; a mutation after a complete save/snapshot marks it again.
- Opaque vaults do not request client updates for inventory-only changes, and client structure/chunk data omits their inventories.
- Crystal vaults send only the first half of local slots used by the existing internal item renderer. Visible-slot changes are coalesced at the member tick; count, NBT and slot positions are retained. Changes to unused slots do not independently request rendering updates.
- Server persistence retains the complete inventory and storage type. Client render data is not a replacement for save data.
- NBT reads invalidate local caches and queue work without resolving other block entities just to notify them. Regrouping transfers pending notifications to the current controller.

Multiple vaults still tick, and external storage mods may build indexes or scan slots. These changes do not remove all possible sources of terminal or modpack latency; no measured FPS/TPS improvement is claimed here.

## Capability invalidation during chunk teardown

Client chunk replacement removes existing block entities while iterating their collection. Calling `Level.getBlockEntity` from an invalidation callback can delete a removed controller and recreate it, changing the collection during iteration. The resulting exception interrupts chunk/NBT replacement and can leave inconsistent client controller data used by connected textures. This path existed in 1.1.1 and the published 1.2.0.

The development fix invalidates client capabilities locally. On a real server, it reads the controller only from `getChunkNow` and the existing block-entity map, without creating entities or loading chunks; removed or mismatched controllers are skipped. Active server controller inventory, identifier and comparator caches still invalidate. Stored inventory, item NBT, notification batching and texture resources are unaffected. Initial user retesting reports no issue so far; updated logs and broader gameplay checks have not yet been reviewed. Published 1.2.0 artifacts remain unchanged.

## Combined inventory lookup

1.2.0 uses Create's `SameSizeCombinedInvWrapper.create` inside the existing versioned inventory wrapper. Equal-capacity members allow direct slot-to-member lookup; mixed capacities use the standard fallback. This does not change per-block inventory ownership, capacities or the upgrade transaction.

## Threshold-switch compatibility selection

The development optimization wraps only the compatibility-filter operation in Create's `ThresholdSwitchBlockEntity.updateCurrentLevel`. It selects the first matching storage adapter on the first scanned slot and reuses that selection for the rest of the same invocation. The no-match result is cached too. MixinExtras shared locals keep the selection separate for each invocation, including recursive calls; it does not persist across target changes or later scans.

Create still calculates each slot's capacity through the selected adapter, or its original fallback when none matches. Item filtering, empty slots, zero-capacity slots, inventory-version tracking, fluid scanning and redstone/display updates retain their upstream logic. This hook applies to item inventories observed by Create threshold switches, not only this mod's vaults. Full inventory scanning and per-slot Stream processing remain; packager summaries and vault notifications are separate optimization work. No measured FPS/TPS improvement is claimed.

The hook uses the existing MixinExtras 0.4.1 runtime supplied by the supported Create distributions and requires exactly one matching injection point. Both runtime JAR manifests register the Mixin configuration; development launches register it explicitly. The hook targets Create and Java Stream names only and does not require Minecraft name remapping.

Development compilation and Forge data-generation initialization passed with Create 6.0.6-150, 6.0.7-281 and 6.0.8-289. Exported transformed classes confirm the hook and invocation-local cache in each version; generated resources were unchanged. These checks do not establish packaged-client gameplay, optional-mod compatibility or measured performance gains.

A user-provided 60-second JFR comparison was reviewed on 2026-10-14 for Create 6.0.8 with `1.2.0-dev-threshold-perf1`. The recordings cover 17:04:06–17:05:06 with `dev-renderfix1` and 18:36:30–18:37:30 with `dev-threshold-perf1` (UTC+08:00), using the same sampling configuration. Threshold-switch samples fell from 442/1738 server-thread samples (25.43%) to 193/1288 (14.98%); compatibility-selection samples fell from 113 to zero. The hook appeared in six samples, and 128 threshold-switch samples still included Stream processing. Zero sampled hits does not mean zero calls. These observations support removal of the repeated-selection hotspot, not an exact timing or guaranteed speedup claim.

The comparison also reports lower rolling tick-time readings: means of 63.37 ms to 56.69 ms over the full recordings, and 62.21 ms to 46.65 ms after excluding the first approximately ten seconds from both. The latter window still had 14/49 readings above 50 ms. Order stages and inventory-operation rates were not automatically held constant; these are rolling averages, not individual tick durations or proof of stable 20 TPS. The report records no ERROR/FATAL messages during the new sample interval or Mixin application failures. Raw recordings were not independently reprocessed for this documentation update. Filter, capacity, fluid, redstone, recursive-call isolation and inventory-conservation acceptance remain separate from this performance observation.

## Shared vault summaries for packagers

The development inventory wrapper extends Create's `VersionedInventoryWrapper`, preserving its inventory identity and version tracking. Each combined vault handler owns separate extractable-item and input-slot snapshots. Successful real mutations clear both snapshots through the existing version increment; controller inventory notifications also clear them for direct member-slot changes. Capability reconstruction creates a new wrapper with empty caches. There is no global cache keyed by world positions, and the hook only handles this mod's combined vault inventories.

The first query after invalidation scans the slots using Create's original slot-limit and simulated-extraction rules, or copies slot contents for input scanning. Item grouping still uses `InventorySummary.add` and its NBT/capability-sensitive comparisons. A version/revision check prevents retaining a scan invalidated by reentrant callbacks. Later queries for the same unchanged handler and mode reuse the grouped snapshot.

Each query receives an independent summary map, lists, count wrappers and copied item stacks. The summary's exact total is copied through a small accessor, without comparing and merging already-grouped entries again; cached snapshots are never exposed to consumers. This matters because Create's arrival processing mutates the previous summary. Packagers retain their own early version-cache return, version recording and `submitNewArrivals` calls. Other inventory types, null targets and bottomless/packager special cases retain their upstream paths.

Cache hits copy grouped entries instead of scanning every slot. The first query after another mutation still scans the full inventory and creates an independent copy, so gains depend on multiple packagers querying the same version; a lone packager does not gain a constant-time rescan. Comparator notification timing and dispatch, package extraction, order data and threshold-switch logic are unchanged by this step. Complete gameplay, arrivals, save/reload and independent performance acceptance for this second candidate remain pending.

Development compilation and Forge data-generation initialization passed with Create 6.0.6-150, 6.0.7-281 and 6.0.8-289. Exported packager bytecode confirms both hooks and retention of the original version-recording and arrival calls; generated resources were unchanged. The summary accessor's field and copy APIs were checked against all three Create class files. Data generation did not exercise summary copying or the live packaging/arrival pipeline.

Independent reprocessing of the 2026-10-14 20:21:05–20:22:05 (UTC+08:00) Create 6.0.8 JFR retest confirmed runtime participation of `1.2.0-dev-packager-perf2`; the installed JAR hash matched the candidate. Of 1865 server-thread samples, 270 included packager inventory queries. Within that chain, 222 included `InventorySummary.add`: 147 in the vault-summary rebuild path, 56 in arrival processing and 19 in the remaining paths. The summary-copy path appeared in five samples. The comparator-notification chain had 89 samples, 84 of which also included packager queries. These inclusive stack counts are not invocation counts or a cache hit-rate measurement. Threshold compatibility/registry queries remained at zero sampled hits; remaining threshold Stream work appeared in 345 of 425 threshold samples.

The retest's 60 rolling tick-time readings averaged 42.62 ms, with a 40.93 ms median and 9/60 above 50 ms. The user reported greater order pressure and near-congested package transport. The reviewed report records no ERROR/FATAL messages or server-behind warnings in the retest interval. An earlier same-process perf2 recording had large rolling-time spikes that did not recur; their cause remains undetermined. Differences in order phases, throughput, congestion and warmup prevent attributing the better overall readings to shared summaries alone. The wrapper is active and copying is not a prominent sampled hotspot, but its independent benefit remains unproven. Scan/reuse/invalidation counts and equal-workload completion measurements are needed to assess it.

## Manual vault diagnostics

Development candidates provide operator-level commands (permission level 2):

```text
/create_bs diagnostics start
/create_bs diagnostics status
/create_bs diagnostics stop
```

Recording is disabled by default. `start` begins a fresh in-memory window without invalidating inventory caches or imposing a time limit. Another `start` retains the running window. `status` shows elapsed real time and aggregate counters; `stop` freezes the counters and writes a UTF-8 JSON report under `<game directory>/local/performance-diagnostics/create_bs/`. Server shutdown exports an active window automatically. A failed export leaves a frozen report available for another `stop` attempt and blocks a new window until export succeeds. Reports use unique filenames and do not overwrite earlier files or modify world saves.

Only server-thread operations on this mod's vault handlers are counted. The report includes elapsed wall time, server tick delta, versions, controller dimension/position, block type and slot count. Extractable and input-slot modes have separate counters. Per-handler details are bounded to 1024 entries; additional handlers contribute to an overflow aggregate included in the totals. Completed reports retain no world/server references. Details are sorted by rebuild time; they identify runtime handlers, so reconstructed capabilities receive distinct IDs.

| Field | Meaning |
|---|---|
| `query_calls` | Packager summary queries, including Create's own early cache returns |
| `create_local_cache_hits` | Queries satisfied by the packager's original version cache |
| `shared_requests`, `shared_cache_hits` | Queries reaching the shared wrapper and those reusing its snapshot |
| `build_attempts`, `retained_builds`, `discarded_builds`, `build_failures` | Scans completed/failed, and whether their result remained valid for caching |
| `completed_scanned_slots` | Slot visits in completed scans; aborted scans are excluded |
| `avoided_slot_visits` | Shared hits multiplied by the respective handler's slot count; an estimate of avoided scan work |
| `build_total_ms`, `build_max_ms`, `build_mean_ms` | Elapsed rebuild timing, including scheduling/GC rather than pure CPU time |
| `copy_calls`, `copy_failures`, `copy_total_ms`, `copy_max_ms` | Independent-summary copy count and elapsed timing |
| `version_increment_calls` | Wrapper version increments, not package count or all possible direct slot changes |
| `member_or_structure_clear_signals` | Additional invalidation notifications, potentially accompanying the same version increment |
| `packages_prepared` | Successful normal-packager preparation, including queued packages; repackager overrides are not covered in diagnostics1 |
| `packages_taken_via_capability` | Non-simulated package extraction through the packager inventory interface; hand pickups are excluded |
| `successful_unwrap_calls` | Successful normal-packager non-simulated unwrap calls, including accepted empty packages; repackager overrides are not covered in diagnostics1 |

Each query is counted once across the original cache check and shared-wrapper entry. Timing is only performed around whole rebuild/copy operations while recording; there is no per-slot timer or logging. Instrumentation hooks still have small branch/call overhead while disabled. Reentrant timing can overlap, and hit rate/avoided slots do not alone measure a guaranteed speedup. Package counters measure flow within the window, not queue length; previously queued packages may leave during recording. They do not measure individual material quantities or prove inventory conservation. Use the report with a JFR from the same interval and comparable completed orders. Published 1.2.0 JARs do not include these commands.

Development compilation, initialization and the packager query/preparation/unwrap hooks passed with Create 6.0.6-150, 6.0.7-281 and 6.0.8-289. The capability-extraction hook's two return sites and owner field were checked in all three versions' bytecode; that target was not loaded by data generation. User-provided Create 6.0.8 output now establishes in-game manual recording/export. Complete counter coverage and instrumentation overhead remain to be checked.

The 2026-10-14 14:28:22–14:33:50 UTC report covers 328.25 seconds and 60 handlers without detail truncation. Per-handler and aggregate checks reproduce `queries = local hits + shared requests`, `shared requests = shared hits + rebuilds`, and the recorded slot/copy totals. Across both modes, 18,726 shared requests produced 13,145 hits and 5,581 rebuilds. Completed scans visited 22,463,860 slots; shared hits avoided an estimated 146,698,420 additional visits, approximately 86.72% of the slot work expected if all those shared requests scanned the handler. This confirms reduced repeat scanning within the observed workload, not a measured FPS/TPS improvement or guaranteed timing gain.

Elapsed rebuild timing totalled 13.918 seconds and copying 9.718 seconds. The user corrected the equipment mapping: the 4000-slot handler belongs to a tenfold crafting matrix with ten repackagers; the 13,500-slot main warehouse has ten ordinary packagers, five receiving and five sending through stock links; the 18,900-slot tree-farm vault has one sending packager. Their extractable shared-hit rates were 90%, 87.88% and 0%, respectively. Main-warehouse rebuilds took 9.403 seconds and copies 0.887 seconds. Matrix copies took 8.823 seconds (90.78% of total copy timing), averaging 3.244 ms with a 190.711 ms maximum. Package/ordering NBT copied by this path is a candidate explanation, not a measured attribution of its cost.

The matrix's zero preparation/unwrap counts alongside 2603 capability-extracted packages reflect a diagnostics1 coverage gap: repackagers override preparation and unwrap methods, so the ordinary-packager hooks do not execute, while the shared inventory-handler extraction hook still counts exported packages. They inherit the summary-query path, so its cache/scan/copy counts remain covered. Package flow must not be treated as a conservation test; repackager preparation/receipt instrumentation needs to be added before comparing its flow counts. Main-warehouse rebuild cost and matrix copy cost are separate follow-up targets.

## Complete 3×3 end-layer regrouping

A complete end layer can fail ordinary connection search when its starting corner still belongs to an older 2×2 controller behind it. The additional connectivity step detects a full, same-type, same-axis 3×3 layer before ordinary formation and permits it to regroup independently.

It skips existing 3×3 structures, mismatched axes/types, source structures extending beyond the target cross-section, missing/unloaded members and active material-upgrade transactions. Existing connections are split through Create's API while keeping every member's inventory object and contents. The nine members form a width-3, length-1 structure; remaining members keep their inventories and queue ordinary regrouping.

Horizontal and vertical material vaults share the same geometry projection. This step does not alter vanilla Create vault connectivity. Independent-client feedback accepted the 1.1.1 repair; large-inventory regrouping and other combinations must not be inferred from that acceptance alone.

## Upgrade transactions

Player upgrades operate on stationary world structures. The transaction snapshots members and inventories, empties them silently, replaces the structure, restores inventory and charges components. Failure attempts rollback; failed recovery writes a recovery record. Preserve item NBT, per-member ownership, capacity checks and payment/rollback order when changing this path.

Crafting and player-facing rules are in [UPGRADES.md](UPGRADES.md).
