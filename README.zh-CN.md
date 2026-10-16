<p align="center">
  <img src="docs/assets/project-icon.png" width="100" height="100" alt="铁质与铜质保险库项目图标">
</p>

<h1 align="center">Create: Bigger Storage Continued</h1>

<p align="center">
  <a href="README.md">English</a> | <strong>简体中文</strong>
</p>

[Create: Bigger Storage](https://github.com/LopyLuna/Create-Bigger-Storage) 的非官方维护续作，原作者为 LopyLuna。为机械动力提供多档容量的横向与纵向保险库、整组原位升级，以及库存性能优化。

本项目与 LopyLuna 及 Create 团队没有官方关联，也未获得其背书。

<p align="center">
  <a href="https://github.com/yunluoxincheng/create-bigger-storage-continued/releases">下载</a> · <a href="CHANGELOG.md">更新日志</a> · <a href="https://github.com/yunluoxincheng/create-bigger-storage-continued/issues">反馈问题</a>
</p>

## 下载

- **正式版 — 1.1.2：**[适用于 Create 6.0.4](https://github.com/yunluoxincheng/create-bigger-storage-continued/releases/tag/v1.1.2)。
- **测试版 — 1.2.1：**[适用于 Create 6.0.6～6.0.8](https://github.com/yunluoxincheng/create-bigger-storage-continued/releases/tag/mc1.20.1-v1.2.1)，内置 Flywheel 1.0.5。

目前通过 GitHub Releases 提供下载，后续计划发布至 Modrinth 和 CurseForge。

## 主要功能

- **多档库存：**9 种材料档位的横向保险库及对应的纵向保险库。
- **整组升级：**45 种升级组件，覆盖 9 种材料档位与 Create 原版档位之间的全部向上组合。右键已形成结构的任意成员方块，即可升级整座保险库。
- **保留库存：**升级保留物品及其 NBT、方块位置、轴向和结构尺寸；目标容量不足时拒绝升级。
- **方向转换：**18 条横纵向保险库物品转换配方，保留输入物品的 NBT。
- **库存优化：**合并重复库存通知、缓存比较器读数并减少客户端库存同步；1.2.1 结合直接槽位查找、增量库存摘要和独立副本延期物化，改善大型保险库的重复工作。
- **机械动力风格：**按材料配色的面板与边框、半透明蓝色水晶保险库，以及原生 16×16 组件图标。
- **语言：**英语与简体中文。

### 容量档位

下表为**每个方块**的库存槽位数。横向和纵向保险库使用相同档位容量与升级组件。

| 档位 | 木质 | Create 原版 | 铜 | 铁 | 绿宝石 | 金 | 水晶 | 钻石 | 黑曜石 | 下界合金 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 每方块槽位 | 10 | 20* | 30 | 40 | 50 | 60 | 70 | 80 | 90 | 100 |

*原版档位受 Create 服务端配置控制，默认值为 20。原版纵向保险库由可选模组 Create: Connected 提供。*

## 运行要求与兼容性

| 模组版本 | 状态 | Minecraft | Forge | Create | Java |
|---|---|---|---|---|---|
| 1.2.1 | 测试版 | 1.20.1 | 47.4.10 及以上（47.x） | 6.0.6、6.0.7、6.0.8 | 17 |
| 1.2.0 | 测试版 | 1.20.1 | 47.4.10 及以上（47.x） | 6.0.6、6.0.7、6.0.8 | 17 |
| 1.1.2 | 正式版 | 1.20.1 | 47.2.32 及以上（47.x） | 6.0.4 | 17 |
| 1.1.1 | 历史正式版 | 1.20.1 | 47.2.32 及以上（47.x） | 6.0.4 | 17 |

1.2.1 为上述三种 Create 版本提供同一个安装包，内置 **Flywheel 1.0.5**，无需单独下载。当前仍为测试版，已完成的核验和待进行的游戏内验证见[兼容说明](docs/COMPATIBILITY.md)。

**Create: Connected 为可选依赖。**仅原版档位的纵向保险库及涉及该档位的升级需要它。请选择与 Minecraft、Create 匹配的 Connected 版本；其当前上游兼容表将 Create 6.0.6 标为不支持，详见 [Connected 兼容说明](https://github.com/hlysine/create_connected#download)。

本版本线面向 **Minecraft 1.20.1 / Forge**，其他 Minecraft 版本及加载器不在支持范围内。

## 安装

1. 按上表安装所选版本对应的 Minecraft、Forge、Create 和 Java。
2. 从 [GitHub Releases](https://github.com/yunluoxincheng/create-bigger-storage-continued/releases) 下载运行 JAR，放入实例的 `mods` 文件夹。`-sources.jar` 用于开发。
3. 客户端与服务端安装相同版本；替换旧版前关闭两端，更新前备份存档。
4. 仅保留一份使用 `create_bs` ID 的模组。本续作替代原 Bigger Storage，不要与原模组或 `createbiggerstoragetocreate6` 桥接补丁同时加载。

## 使用升级组件

制作对应当前档位和目标档位的升级组件，右键已形成结构的任意成员方块即可。生存模式下**每个方块消耗一个组件**，从玩家物品栏合计扣除；创造模式不消耗组件。

升级由玩家在静态世界结构上进行，不支持部署器或运行中的动态结构。组件也可以在使用前继续合成为更高目标档位的升级件，完整路线见[配方及成本参考](docs/UPGRADES.md)。

## 问题反馈

请通过 [Issues](https://github.com/yunluoxincheng/create-bigger-storage-continued/issues) 反馈问题或建议，提供模组、Minecraft、Forge、Create 版本，复现步骤及相关日志或截图。库存性能问题还应提供保险库尺寸，以及使用的终端或自动化模组。

开发候选包支持使用 `/create_bs diagnostics start` 开始库存诊断，使用 `/create_bs diagnostics stop` 停止并导出报告。命令及计数含义见[诊断说明](docs/TECHNICAL_NOTES.md#manual-vault-diagnostics)。已发布的 1.2.0 安装包尚未包含这些命令。

## 源码构建

需要 **Java 17**，使用项目自带的 **Gradle 8.12 Wrapper**。Windows：

```powershell
.\gradlew.bat --no-daemon jarJar sourcesJar
```

Linux 或 macOS：

```bash
bash ./gradlew --no-daemon jarJar sourcesJar
```

输出位于 `build/libs/`。1.2.0 的普通版本文件名 JAR 包含 Flywheel，`-unbundled.jar` 为内部构建产物。常规构建使用已提交资源；发布和本机构建选项见[发布指南](docs/RELEASING.md)。

## 署名与许可

- **LopyLuna：**原 Bigger Storage 项目及代码。
- **simibubi 与 Create 贡献者：**机械动力及保险库原始美术。
- **Jozufozu 与 Flywheel 贡献者：**内置 Flywheel 库及其上游修复。
- **Continued 贡献者：**维护、纵向保险库、升级及性能优化。

代码采用 [MIT 许可](LICENSE)。Create 美术改作及内置 Flywheel 保留原许可声明，许可、素材来源及平台发布要求见[署名与来源说明](docs/ATTRIBUTION.md)。

项目文档：[升级与配方](docs/UPGRADES.md)、[兼容性](docs/COMPATIBILITY.md)、[技术说明](docs/TECHNICAL_NOTES.md)、[发布指南](docs/RELEASING.md)、[署名与来源](docs/ATTRIBUTION.md)。
