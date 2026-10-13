param(
    [string]$CreateJar = 'E:\MINECRIFT\.minecraft\versions\Skyblock Burgeria\mods\create-1.20.1-6.0.4.jar'
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$encoding = [System.Text.UTF8Encoding]::new($false)
$tiers = @(
    @{id='wooden'; zh='木质'; short='木质'}, @{id='standard'; zh='原版'; short='原版'},
    @{id='copper'; zh='铜质'; short='铜'}, @{id='iron'; zh='铁质'; short='铁'},
    @{id='emerald'; zh='绿宝石'; short='绿宝石'}, @{id='gold'; zh='金质'; short='金'},
    @{id='crystal'; zh='水晶'; short='水晶'}, @{id='diamond'; zh='钻石'; short='钻石'},
    @{id='obsidian'; zh='黑曜石'; short='黑曜石'}, @{id='netherite'; zh='下界合金'; short='下界合金'}
)
$language = [ordered]@{}
$upgradeRecipeCount = 0
function Get-CanonicalJsonValue($value) {
    if ($value -is [System.Collections.IDictionary]) {
        $canonical = [ordered]@{}
        foreach ($key in @($value.Keys | Sort-Object)) {
            $canonical[$key] = Get-CanonicalJsonValue $value[$key]
        }
        return $canonical
    }
    if ($value -is [array]) {
        return ,@($value | ForEach-Object { Get-CanonicalJsonValue $_ })
    }
    return $value
}
function Write-GeneratedJson([string]$relative, $value) {
    $path = [System.IO.Path]::GetFullPath((Join-Path $project $relative))
    if (!$path.StartsWith($project + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Generated path escapes project' }
    New-Item -ItemType Directory -Path (Split-Path -Parent $path) -Force | Out-Null
    $json = $value | ConvertTo-Json -Depth 20
    # Only this generator's explicitly named files are updated.
    $changed = $true
    if (Test-Path -LiteralPath $path -PathType Leaf) {
        $existing = Get-Content -Encoding UTF8 -LiteralPath $path -Raw | ConvertFrom-Json -AsHashtable
        $before = Get-CanonicalJsonValue $existing | ConvertTo-Json -Depth 20 -Compress
        $after = Get-CanonicalJsonValue $value | ConvertTo-Json -Depth 20 -Compress
        $changed = $before -cne $after
    }
    if ($changed) {
        [System.IO.File]::WriteAllText($path, $json + [Environment]::NewLine, $encoding)
    }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($CreateJar)
try {
    $reader = [System.IO.StreamReader]::new(
        $archive.GetEntry('data/create/recipes/crafting/kinetics/item_vault.json').Open(),
        [System.Text.Encoding]::UTF8)
    try { $standardRecipe = $reader.ReadToEnd() | ConvertFrom-Json -AsHashtable }
    finally { $reader.Dispose() }
} finally { $archive.Dispose() }

function Get-UpgradeCenter([int]$from, [int]$previous) {
    if ($previous -eq $from) { return "create_bs:$($tiers[$from].id)_component" }
    return "create_bs:$($tiers[$from].id)_to_$($tiers[$previous].id)_upgrade"
}
function Write-MaterialUpgrade([int]$from, [int]$to, [int]$previous,
    [string]$template, [string]$suffix = '') {
    if ($previous -lt $from -or $previous -ge $to) { throw 'Invalid upgrade material path' }
    $recipe = Get-Content -Encoding UTF8 -LiteralPath (
        Join-Path $project "src/main/resources/data/create_bs/recipes/crafting/$template.json"
    ) -Raw | ConvertFrom-Json -AsHashtable
    $id = "$($tiers[$from].id)_to_$($tiers[$to].id)_upgrade"
    $recipe.key.V = @{item=(Get-UpgradeCenter $from $previous)}
    $recipe.result = @{item="create_bs:$id"}
    Write-GeneratedJson "src/main/resources/data/create_bs/recipes/upgrades/$id$suffix.json" $recipe
    $script:upgradeRecipeCount++
}

$language['item.create_bs.base_component'] = '基础组件'
Write-GeneratedJson 'src/main/resources/data/create_bs/recipes/components/base_component.json' @{
    type='minecraft:crafting_shaped'; category='misc'; pattern=@('PPP', 'PNP', 'PPP')
    key=@{P=@{item='minecraft:paper'}; N=@{item='minecraft:iron_nugget'}}
    result=@{item='create_bs:base_component'; count=8}
}
$markers = @(
    @{tag='minecraft:logs'}, $standardRecipe.key.B,
    @{item='minecraft:copper_ingot'}, @{item='minecraft:iron_ingot'},
    @{item='minecraft:emerald'}, @{item='minecraft:gold_ingot'},
    @{item='minecraft:glass'}, @{item='minecraft:diamond'}, @{item='minecraft:obsidian'}
)
for ($tier = 0; $tier -lt $tiers.Count; $tier++) {
    $componentId = "$($tiers[$tier].id)_component"
    $language["item.create_bs.$componentId"] = "$($tiers[$tier].short)组件"
    if ($tier -lt 9) {
        $ingredients = @()
        for ($slot = 0; $slot -lt 8; $slot++) {
            $ingredients += @{item='create_bs:base_component'}
        }
        $ingredients += $markers[$tier]
        $recipe = @{type='minecraft:crafting_shapeless'; category='misc';
            ingredients=$ingredients; result=@{item="create_bs:$componentId"; count=8}}
    } else {
        $recipe = Get-Content -Encoding UTF8 -LiteralPath (
            Join-Path $project 'src/main/resources/data/create_bs/recipes/crafting/netherite_item_vault.json'
        ) -Raw | ConvertFrom-Json -AsHashtable
        $recipe.key.V = @{item='create_bs:base_component'}
        $recipe.result = @{item="create_bs:$componentId"}
    }
    Write-GeneratedJson "src/main/resources/data/create_bs/recipes/components/$componentId.json" $recipe
}

for ($from = 0; $from -lt $tiers.Count; $from++) {
    $source = $tiers[$from]
    $language["create_bs.tier.$($source.id)"] = $source.zh
    if ($source.id -ne 'standard') {
        $normal = "$($source.id)_item_vault"
        $vertical = "vertical_$normal"
        $language["block.create_bs.$normal"] = "$($source.zh)物品保险库"
        $language["block.create_bs.$vertical"] = "纵向$($source.zh)物品保险库"
        Write-GeneratedJson "src/main/resources/data/create_bs/recipes/conversion/$vertical.json" @{
            type='create_bs:vault_orientation'; input="create_bs:$normal"; output="create_bs:$vertical"
        }
        Write-GeneratedJson "src/main/resources/data/create_bs/recipes/conversion/$normal.json" @{
            type='create_bs:vault_orientation'; input="create_bs:$vertical"; output="create_bs:$normal"
        }
    }
    for ($to = $from + 1; $to -lt $tiers.Count; $to++) {
        $destination = $tiers[$to]
        $id = "$($source.id)_to_$($destination.id)_upgrade"
        $language["item.create_bs.$id"] = "$($source.short)→$($destination.short)升级"
        if ($to -eq 1) {
            # Wooden vaults already contain the cost of a standard vault.
            Write-GeneratedJson "src/main/resources/data/create_bs/recipes/upgrades/$id.json" @{
                type='minecraft:crafting_shapeless'; category='misc'
                ingredients=@(@{item='create_bs:wooden_component'}); result=@{item="create_bs:$id"}
            }
            $upgradeRecipeCount++
        } elseif ($to -eq 9) {
            # Obsidian retains its previously paid diamond-vault cost.
            $previous = if ($from -eq 8) { 8 } else { 7 }
            Write-GeneratedJson "src/main/resources/data/create_bs/recipes/upgrades/$id.json" @{
                type='minecraft:crafting_shapeless'; category='misc'
                ingredients=@(@{item=(Get-UpgradeCenter $from $previous)},
                    @{item='create_bs:netherite_component'})
                result=@{item="create_bs:$id"}
            }
            $upgradeRecipeCount++
        } else {
            switch ($to) {
                2 { $previous=1; $template='copper_item_vault' }
                3 {
                    if ($from -eq 2) { $previous=2; $template='alt_iron_item_vault' }
                    else { $previous=1; $template='iron_item_vault' }
                }
                4 { $previous=3; $template='emerald_item_vault' }
                5 {
                    if ($from -eq 4) { $previous=4; $template='alt_gold_item_vault' }
                    else { $previous=3; $template='gold_item_vault' }
                }
                6 { $previous=5; $template='crystal_item_vault' }
                7 {
                    if ($from -eq 6) { $previous=6; $template='alt_diamond_item_vault' }
                    else { $previous=5; $template='diamond_item_vault' }
                }
                8 { $previous=7; $template='obsidian_item_vault' }
                default { throw 'Unknown upgrade destination' }
            }
            Write-MaterialUpgrade $from $to $previous $template
        }
    }
}
# Preserve every upstream material branch without adding duplicate upgrade items.
for ($from = 0; $from -le 1; $from++) {
    Write-MaterialUpgrade $from 3 2 'alt_iron_item_vault' '_from_copper'
}
for ($from = 0; $from -le 3; $from++) {
    Write-MaterialUpgrade $from 5 4 'alt_gold_item_vault' '_from_emerald'
}
for ($from = 0; $from -le 5; $from++) {
    Write-MaterialUpgrade $from 7 6 'alt_diamond_item_vault' '_from_crystal'
}
if ($upgradeRecipeCount -ne 57) { throw "Expected 57 upgrade recipes, generated $upgradeRecipeCount" }
$language['create_bs.upgrade.tooltip'] = '%s → %s'
$language['create_bs.upgrade.whole_structure'] = '右键任意成员方块，升级整个保险库结构。'
$language['create_bs.upgrade.one_per_block'] = '每个方块消耗 1 个对应组件，从玩家背包合计扣除。'
$language['create_bs.upgrade.success'] = '已升级 %s 个方块，消耗 %s 个组件。'
$language['create_bs.upgrade.error.unavailable'] = '当前无法执行此升级。'
$language['create_bs.upgrade.error.structure'] = '来源档位不匹配，或保险库结构尚未完整形成。'
$language['create_bs.upgrade.error.connected'] = '原版纵向目标需要创意传动提供的纵向物品保险库。'
$language['create_bs.upgrade.error.geometry'] = '目标档位不能保留当前结构尺寸，已取消升级。'
$language['create_bs.upgrade.error.access'] = '所有成员方块都必须已加载且允许操作。'
$language['create_bs.upgrade.error.capacity'] = '目标实际容量不足，库存未更改。'
$language['create_bs.upgrade.error.components'] = '升级整个结构需要 %s 个对应组件。'
$language['create_bs.upgrade.error.prepare'] = '无法准备升级，库存未更改，请检查日志。'
$language['create_bs.upgrade.error.rollback'] = '升级失败，原保险库已恢复。'
$language['create_bs.upgrade.error.recovery'] = '回退未完成，原数据已保存至升级恢复记录，请停止操作并检查日志。'
$language['create_bs.upgrade.error.recovery_unsaved'] = '回退及恢复记录保存失败，请停止操作并检查服务端日志。'
Write-GeneratedJson 'src/main/resources/assets/create_bs/lang/zh_cn.json' $language
Write-Output "Generated 11 component recipes, $upgradeRecipeCount upgrade recipes, 18 orientation recipes, and $($language.Count) Chinese entries."
