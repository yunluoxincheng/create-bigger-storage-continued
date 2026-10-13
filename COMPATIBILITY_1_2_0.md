# 1.2.0 共用构建与兼容边界

目标：Minecraft1.20.1、Forge47.4.10、Java17，一个模组jar覆盖Create6.0.6、6.0.7、6.0.8，并在客户端实际使用Flywheel1.0.5。未宣称支持未验证的未来Create版本或Minecraft1.21.1。

## 实现范围

- 以Create6.0.6-150为最低编译基线，Ponder1.0.80；Flywheel固定1.0.5，Registrate保持MC1.20-1.3.3。
- `mods.toml`声明Forge`[47.4.10,48)`、Create`[6.0.6,6.0.9)`及客户端Flywheel`[1.0.5,1.0.6)`。
- Forge Jar-in-Jar仅嵌入原Flywheel1.0.5 jar，使用原Maven坐标及范围协商，不复制或重定位其后端代码。完整安装包由`jarJar`和`reobfJarJar`生成。
- 内部Flywheel完整MIT许可保留在嵌入jar的`META-INF/LICENSE.md`；打包核对版本、后端存在、范围及SHA-256。
- 核心保险库运行代码只把`CombinedInvWrapper`替换为Create公共`SameSizeCombinedInvWrapper.create`。相同容量直接定位成员；不同容量自动回退。通知合并、库存NBT、升级、支付和回退保持原实现。

## 已执行检查

| Create | Forge | Flywheel开发依赖 | Ponder开发依赖 | Java编译／Forge初始化／Registrate数据生成 | 生成资源 |
|---|---|---|---|---|---|
| 6.0.6-150 | 47.4.10 | 1.0.5 | 1.0.80 | 通过 | 无变更 |
| 6.0.7-281 | 47.4.10 | 1.0.5 | 1.0.91 | 通过 | 无变更 |
| 6.0.8-289 | 47.4.10 | 1.0.5 | 1.0.91 | 通过 | 无变更 |

初始化使用Forge `runData`开发入口，包含模组注册及资源生成，不是实际游戏帧渲染，也不是存档或终端操作测试。

另外下载三版正式Create发行包并验证官方SHA-512，将每版发行包与最终重映射模组jar交给Forge使用的真实JarSelector0.3.19解析器：三个组合均只选择一份Flywheel1.0.5。此检查使用实际嵌入依赖元数据及库内容，确认6.0.6的1.0.4候选被较高约束排除；它不替代完整生产客户端启动。

资源、模型、配方、翻译、原许可及历史安装文件的核对记录见`build/compatibility/1.2.0/verification.json`。未新增测试代码，未操作活动整合包或用户存档。

## 待游戏验收

- 完整生产客户端加载、正常及水晶保险库显示、所用Flywheel后端的实际绘制。
- 终端存取和合成、比较器、含物品结构重组及升级、保存重进、动态结构库存恢复。
- 匹配版本的Create: Connected纵向silo升级。

1.1.2已收到用户对游戏实测无明显问题的反馈；不能自动推广为本版本的三组合验收。当前1.2.0保持Beta，候选安装文件位于`build/candidates/1.2.0/`。本机libs仅保留已接受正式版。
