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

The development fix invalidates client capabilities locally. On a real server, it reads the controller only from `getChunkNow` and the existing block-entity map, without creating entities or loading chunks; removed or mismatched controllers are skipped. Active server controller inventory, identifier and comparator caches still invalidate. Stored inventory, item NBT, notification batching and texture resources are unaffected. Gameplay confirmation of the repair is pending; published 1.2.0 artifacts remain unchanged.

## Combined inventory lookup

1.2.0 uses Create's `SameSizeCombinedInvWrapper.create` inside the existing versioned inventory wrapper. Equal-capacity members allow direct slot-to-member lookup; mixed capacities use the standard fallback. This does not change per-block inventory ownership, capacities or the upgrade transaction.

## Complete 3×3 end-layer regrouping

A complete end layer can fail ordinary connection search when its starting corner still belongs to an older 2×2 controller behind it. The additional connectivity step detects a full, same-type, same-axis 3×3 layer before ordinary formation and permits it to regroup independently.

It skips existing 3×3 structures, mismatched axes/types, source structures extending beyond the target cross-section, missing/unloaded members and active material-upgrade transactions. Existing connections are split through Create's API while keeping every member's inventory object and contents. The nine members form a width-3, length-1 structure; remaining members keep their inventories and queue ordinary regrouping.

Horizontal and vertical material vaults share the same geometry projection. This step does not alter vanilla Create vault connectivity. Independent-client feedback accepted the 1.1.1 repair; large-inventory regrouping and other combinations must not be inferred from that acceptance alone.

## Upgrade transactions

Player upgrades operate on stationary world structures. The transaction snapshots members and inventories, empties them silently, replaces the structure, restores inventory and charges components. Failure attempts rollback; failed recovery writes a recovery record. Preserve item NBT, per-member ownership, capacity checks and payment/rollback order when changing this path.

Crafting and player-facing rules are in [UPGRADES.md](UPGRADES.md).
