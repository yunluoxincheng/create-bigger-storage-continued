# Create: Bigger Storage Continued — Unreleased

Changes on the development branch that are not included in published JARs.

## Added

- Add manually controlled vault diagnostics to report inventory queries, cache reuse, summary rebuilds and package flow with `/create_bs diagnostics start` and `stop`.

## Changed

- Replace the earlier AI-derived mod icon with a composition of the current iron and copper vault textures.

## Fixes

- Prevent controller lookup during capability invalidation from modifying the block-entity collection, addressing client chunk-refresh exceptions and broken vault connected textures.

## Performance

- Resolve Create threshold-switch storage compatibility once per inventory scan, reducing repeated type checks for large inventories while preserving item filters and slot capacity calculations.
- Reuse stock summaries across packagers connected to the same unchanged vault inventory, reducing repeated slot scans while preserving independent arrival tracking.
