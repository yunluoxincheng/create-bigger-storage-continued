# 原创材质来源与导出

当前正式版1.1.1根据2026-10-10用户澄清，仅参考Minecraft材料颜色及深浅，保留Create自身纹理和同色框架，蓝水晶保持半透明并去掉侧面小点。最终材质、许可、预览和记录见 [create-colors-v5/README.md](create-colors-v5/README.md)，164张运行PNG位于 `src/main/resources/assets/create_bs/textures/`。当前源码提交保留最终资源、生成脚本、必要样张及配色记录。

下文是此前本机制作历史。`material-details-v4/`、`confirmed-textures/`、`a-style/`和`sources/`等历史目录仍保留在本地，不随当前源码提交；其中图片不作为最终材质生成输入。重复导出的PNG及交付ZIP也留在本地，由脚本重新生成。

本目录保存 Create: Bigger Storage Continued 第一版原创素材、生成提示词和技术预览。用户于2026-10-08要求核对上游美术许可，并在不能确认可公开复用时重新制作保险库及组件材质。

## 许可核对

2026-10-08读取上游当前 main，仍为提交 `3429e16d51ca91f2b8f7d46d58e5add18e56537d`：

- [根目录 LICENSE](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/LICENSE) 是 MIT。
- [上游 mods.toml](https://github.com/LopyLuna/Create-Bigger-Storage/blob/3429e16d51ca91f2b8f7d46d58e5add18e56537d/src/main/resources/META-INF/mods.toml) 明确写着 `license="MIT-CODE, ARR-ART"`。
- 完整递归目录含205个文件和109张PNG，仅找到根目录LICENSE，没有另行的美术许可或README说明。

因此没有确认原PNG可用于我们计划公开发布的版本；不能把GitHub显示的MIT标记当作美术授权。本轮不宣称获得了原作者的美术许可，也没有联系原作者。

## 素材制作

- 使用内置 image_gen 工具从文字描述生成原创素材，未把原模组的PNG作为生成或编辑输入。
- 9张保险库四面图集、10张材质标记、组件底板、升级底板和模组图标，共22张源图。完整提示词与来源位于 `provenance.json`。
- 原图保存在 `sources/`，项目引用的运行材质位于 `src/main/resources`，不依赖Codex缓存目录。
- 使用 `tools/ExportArtwork.py` 导出：四面图集裁切、最近邻像素缩放、连接纹理UV切片、标记位置归一化。脚本不读取上游PNG作为创作输入。
- 方块小贴图16×16；中、大型连接贴图64×64；物品图层32×32；模组图标256×256。水晶库保留生成素材的透明通道。
- 45种升级件由底板、起点标记、目标标记三层组成；11种组件由底板及对应材质核心组成，基础组件只有底板。原有物品ID和升级逻辑不变。
- Create 6.0.4 的RECTANGLE索引：列依次为无横向连接、仅右、左右、仅左；行依次为仅下、上下、仅上、无纵向连接。无连接索引为12。
- 正面按整个2×2或3×3门面放大并切片，避免每个方块重复锁扣；其他面分别裁掉连接侧的2、3个逻辑像素边框，再缩放到每格16×16。这是初版的连接外观，需要实际游戏检查不同结构尺寸的接缝。

## 预览与验证边界

`previews/vaults.png` 为9种正面贴图；`components.png` 为11种组件；`upgrades.png` 为45种升级件；`connected.png` 为按RECTANGLE规则拼接的3×3正面预览。

这些图片是技术预览，不是Minecraft游戏截图。横纵向结构、不同轴向和长度、光照下的水晶透明表现，需要实际游戏验收。

2026-10-08本地检查通过：Java编译、Forge/Create初始化与资源生成、重映射构建；56个物品模型的156个图层均引用有效原创PNG。最终jar含139张运行PNG，逐一与当前资源匹配，未匹配上游109张PNG中的任何哈希。57条升级配方、11条组件配方、18条转换配方及99项中英文翻译仍完整。

新材质构建另存为 `build/libs/create-bigger-storage-continued-1.20.1-1.1.0-dev-art-v1.jar`，仅生成项目产物，未替换整合包中的模组。

`export-manifest.json` 记录每个导出文件的尺寸和SHA-256。旧109张PNG的本地备份位于被Git忽略的 `build/artwork-upstream-backup/`，不会进入新jar。

新素材按本项目MIT条款提供，适用于可授予的权利；不据此重授权上游ARR美术。根目录上游LICENSE及原作者署名继续保留。公开源码发布还应处理旧Git历史内的上游美术：使用当前源码树的无历史快照，或先取得原作者许可；本轮没有改写Git历史。
