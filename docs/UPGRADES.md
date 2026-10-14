# Vault upgrades and crafting

## Applying upgrades

Craft the component matching the vault's current and target tiers, then right-click any member of a formed, stationary structure. Survival requires one upgrade item **per block**, collected from the player's inventory; creative does not consume them. Insufficient components or target capacity reject the upgrade before it starts.

Upgrades retain positions, orientation, dimensions, per-block inventory and item NBT. They must be performed by a player: deployers and moving contraptions are not supported. Horizontal and vertical vaults share upgrade items. The standard-tier vertical silo and upgrades involving it require a compatible optional Create: Connected release; see [COMPATIBILITY.md](COMPATIBILITY.md).

## Crafting rules

All component and upgrade recipes fit a normal 3×3 crafting table. There are 45 upgrade items, 57 upgrade recipes including alternatives, and 11 component recipes. An upgrade item can be crafted into a higher-target upgrade before use, without upgrading the world structure between crafting steps.

The starting component is paid once. Later steps replace the center with an intermediate upgrade from the same starting tier. Netherite components carry the final expensive materials rather than serving as a starting-tier marker. Wooden → Standard charges only the wooden component, recognizing the existing standard-vault cost. Obsidian → Netherite recognizes the prior diamond-vault cost. No downgrade or material-refund recipes are provided.

Tables below list ingredients for the final crafting step, not cumulative chain costs. A `#` selector is an ingredient tag; a recipe viewer or the linked JSON shows matching items and shaped placement. Modpack datapacks can override these defaults.

## Component recipes

| Output | Ingredients | Recipe |
|---|---|---|
| 8 Base Component | 8 Paper + 1 Iron Nugget | [View](../src/main/resources/data/create_bs/recipes/components/base_component.json) |
| 8 Copper Component | 8 Base Component + 1 Copper Ingot | [View](../src/main/resources/data/create_bs/recipes/components/copper_component.json) |
| 8 Crystal Component | 8 Base Component + 1 Glass | [View](../src/main/resources/data/create_bs/recipes/components/crystal_component.json) |
| 8 Diamond Component | 8 Base Component + 1 Diamond | [View](../src/main/resources/data/create_bs/recipes/components/diamond_component.json) |
| 8 Emerald Component | 8 Base Component + 1 Emerald | [View](../src/main/resources/data/create_bs/recipes/components/emerald_component.json) |
| 8 Gold Component | 8 Base Component + 1 Gold Ingot | [View](../src/main/resources/data/create_bs/recipes/components/gold_component.json) |
| 8 Iron Component | 8 Base Component + 1 Iron Ingot | [View](../src/main/resources/data/create_bs/recipes/components/iron_component.json) |
| 1 Netherite Component | 4 Raw Gold Block + 2 Diamond Block + 2 Netherite Ingot + 1 Base Component | [View](../src/main/resources/data/create_bs/recipes/components/netherite_component.json) |
| 8 Obsidian Component | 8 Base Component + 1 Obsidian | [View](../src/main/resources/data/create_bs/recipes/components/obsidian_component.json) |
| 8 Standard Component | 8 Base Component + 1 `#forge:plates/iron` | [View](../src/main/resources/data/create_bs/recipes/components/standard_component.json) |
| 8 Wooden Component | 8 Base Component + 1 `#minecraft:logs` | [View](../src/main/resources/data/create_bs/recipes/components/wooden_component.json) |

## Upgrade recipes

Alternative rows produce the same upgrade item using a different intermediate route. They can continue into later targets without paying a second starting-component fee.

| Output | Ingredients | Recipe |
|---|---|---|
| Copper to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Copper to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_crystal_upgrade.json) |
| Copper to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Copper to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_diamond_upgrade.json) |
| Copper to Diamond Upgrade | 8 Diamond + 1 Copper to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_diamond_upgrade_from_crystal.json) |
| Copper to Emerald Upgrade | 4 Emerald + 4 Emerald Block + 1 Copper to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_emerald_upgrade.json) |
| Copper to Gold Upgrade | 4 Gold Ingot + 4 Gold Block + 1 Copper to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_gold_upgrade.json) |
| Copper to Gold Upgrade | 6 Gold Ingot + 2 Gold Block + 1 Copper to Emerald Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_gold_upgrade_from_emerald.json) |
| Copper to Iron Upgrade | 6 Iron Ingot + 2 Iron Block + 1 Copper Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_iron_upgrade.json) |
| Copper to Netherite Upgrade | 1 Copper to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_netherite_upgrade.json) |
| Copper to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Copper to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/copper_to_obsidian_upgrade.json) |
| Crystal to Diamond Upgrade | 8 Diamond + 1 Crystal Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/crystal_to_diamond_upgrade.json) |
| Crystal to Netherite Upgrade | 1 Crystal to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/crystal_to_netherite_upgrade.json) |
| Crystal to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Crystal to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/crystal_to_obsidian_upgrade.json) |
| Diamond to Netherite Upgrade | 1 Diamond Component + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/diamond_to_netherite_upgrade.json) |
| Diamond to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Diamond Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/diamond_to_obsidian_upgrade.json) |
| Emerald to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Emerald to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_crystal_upgrade.json) |
| Emerald to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Emerald to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_diamond_upgrade.json) |
| Emerald to Diamond Upgrade | 8 Diamond + 1 Emerald to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_diamond_upgrade_from_crystal.json) |
| Emerald to Gold Upgrade | 6 Gold Ingot + 2 Gold Block + 1 Emerald Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_gold_upgrade.json) |
| Emerald to Netherite Upgrade | 1 Emerald to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_netherite_upgrade.json) |
| Emerald to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Emerald to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/emerald_to_obsidian_upgrade.json) |
| Gold to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Gold Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/gold_to_crystal_upgrade.json) |
| Gold to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Gold Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/gold_to_diamond_upgrade.json) |
| Gold to Diamond Upgrade | 8 Diamond + 1 Gold to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/gold_to_diamond_upgrade_from_crystal.json) |
| Gold to Netherite Upgrade | 1 Gold to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/gold_to_netherite_upgrade.json) |
| Gold to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Gold to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/gold_to_obsidian_upgrade.json) |
| Iron to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Iron to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_crystal_upgrade.json) |
| Iron to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Iron to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_diamond_upgrade.json) |
| Iron to Diamond Upgrade | 8 Diamond + 1 Iron to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_diamond_upgrade_from_crystal.json) |
| Iron to Emerald Upgrade | 4 Emerald + 4 Emerald Block + 1 Iron Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_emerald_upgrade.json) |
| Iron to Gold Upgrade | 4 Gold Ingot + 4 Gold Block + 1 Iron Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_gold_upgrade.json) |
| Iron to Gold Upgrade | 6 Gold Ingot + 2 Gold Block + 1 Iron to Emerald Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_gold_upgrade_from_emerald.json) |
| Iron to Netherite Upgrade | 1 Iron to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_netherite_upgrade.json) |
| Iron to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Iron to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/iron_to_obsidian_upgrade.json) |
| Obsidian to Netherite Upgrade | 1 Obsidian Component + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/obsidian_to_netherite_upgrade.json) |
| Standard to Copper Upgrade | 4 Copper Ingot + 4 Copper Block + 1 Standard Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_copper_upgrade.json) |
| Standard to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Standard to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_crystal_upgrade.json) |
| Standard to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Standard to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_diamond_upgrade.json) |
| Standard to Diamond Upgrade | 8 Diamond + 1 Standard to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_diamond_upgrade_from_crystal.json) |
| Standard to Emerald Upgrade | 4 Emerald + 4 Emerald Block + 1 Standard to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_emerald_upgrade.json) |
| Standard to Gold Upgrade | 4 Gold Ingot + 4 Gold Block + 1 Standard to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_gold_upgrade.json) |
| Standard to Gold Upgrade | 6 Gold Ingot + 2 Gold Block + 1 Standard to Emerald Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_gold_upgrade_from_emerald.json) |
| Standard to Iron Upgrade | 4 Iron Ingot + 4 Iron Block + 1 Standard Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_iron_upgrade.json) |
| Standard to Iron Upgrade | 6 Iron Ingot + 2 Iron Block + 1 Standard to Copper Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_iron_upgrade_from_copper.json) |
| Standard to Netherite Upgrade | 1 Standard to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_netherite_upgrade.json) |
| Standard to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Standard to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/standard_to_obsidian_upgrade.json) |
| Wooden to Copper Upgrade | 4 Copper Ingot + 4 Copper Block + 1 Wooden to Standard Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_copper_upgrade.json) |
| Wooden to Crystal Upgrade | 6 Glass + 2 Diamond Block + 1 Wooden to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_crystal_upgrade.json) |
| Wooden to Diamond Upgrade | 4 Diamond + 4 Diamond Block + 1 Wooden to Gold Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_diamond_upgrade.json) |
| Wooden to Diamond Upgrade | 8 Diamond + 1 Wooden to Crystal Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_diamond_upgrade_from_crystal.json) |
| Wooden to Emerald Upgrade | 4 Emerald + 4 Emerald Block + 1 Wooden to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_emerald_upgrade.json) |
| Wooden to Gold Upgrade | 4 Gold Ingot + 4 Gold Block + 1 Wooden to Iron Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_gold_upgrade.json) |
| Wooden to Gold Upgrade | 6 Gold Ingot + 2 Gold Block + 1 Wooden to Emerald Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_gold_upgrade_from_emerald.json) |
| Wooden to Iron Upgrade | 4 Iron Ingot + 4 Iron Block + 1 Wooden to Standard Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_iron_upgrade.json) |
| Wooden to Iron Upgrade | 6 Iron Ingot + 2 Iron Block + 1 Wooden to Copper Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_iron_upgrade_from_copper.json) |
| Wooden to Netherite Upgrade | 1 Wooden to Diamond Upgrade + 1 Netherite Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_netherite_upgrade.json) |
| Wooden to Obsidian Upgrade | 4 Powdered Obsidian + 4 Obsidian + 1 Wooden to Diamond Upgrade | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_obsidian_upgrade.json) |
| Wooden to Standard Upgrade | 1 Wooden Component | [View](../src/main/resources/data/create_bs/recipes/upgrades/wooden_to_standard_upgrade.json) |

## Example: Standard → Netherite

1. Craft standard components from eight base components and one iron plate; the recipe yields eight.
2. Craft Standard → Iron using one standard component, four iron blocks and four iron ingots.
3. Continue to Standard → Gold using four gold blocks and four gold ingots.
4. Continue to Standard → Diamond using four diamond blocks and four diamonds.
5. Craft a netherite component from one base component, four raw gold blocks, two diamond blocks and two netherite ingots.
6. Combine the intermediate upgrade with that netherite component, then use the resulting item on the structure.

The direct route adds 40 iron ingots, 40 gold ingots, 58 diamonds, 36 raw gold and two netherite ingots, plus the component-processing materials. Component recipes are batched: materials left in unused components remain available for other upgrades, and small batches require the whole recipe input.

For fewer diamonds, replace the direct diamond step with the crystal route: two diamond blocks and six glass, followed by eight diamonds. The total becomes 44 diamonds and six glass; the other costs remain the same.

## Orientation conversion

Eighteen one-to-one recipes convert the nine material vault items between horizontal and vertical orientations, preserving the input item's NBT. They do not upgrade an existing world structure or replace the standard vertical silo supplied by Connected.
