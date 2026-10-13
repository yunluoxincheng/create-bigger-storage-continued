# 项目规范

本文件适用于Create: Bigger Storage Continued整个仓库。开发前阅读README、相关实现及本文件；发布操作同时遵守 [RELEASING.md](RELEASING.md)。

## 工作环境与修改边界

- 默认环境为Windows；执行命令前确认PowerShell、Git Bash或CMD，使用匹配的语法和路径。带空格路径须引用。
- 文本按UTF-8读取和写入。出现乱码先核对实际编码，不因为终端显示而直接重编码。
- 修改前阅读现有内容，采用任务需要的最小差异，不顺带改格式、配置或升级无关依赖。
- 不批量或递归删除。单文件删除也须核对精确路径、确认是文件且无通配符，并有用户明确指定的删除范围。
- 未授权不执行提交、推送、历史重写、强制推送或工作区丢弃操作。
- 正常实现不主动新增或扩写测试代码；可运行现有聚焦检查，优先差异检查、编译、构建及必要的运行核验。用户明确要求测试时再进入测试阶段。
- 不操作活动整合包或用户存档，除非用户明确授权。保留注册ID、逐块库存、完整物品NBT和既定升级事务语义。
- 区分源码核验、构建通过、用户接受和游戏内实测；不能以CI成功补记性能或兼容验收。

## Minecraft分支

- 开发分支以Minecraft版本命名，例如`1.20.1`、`1.21.1`；不用`main`承载通用多版本开发。
- 当前开发及GitHub默认分支为`1.20.1`。实际开始其他版本适配时才建立对应分支，不提前声称支持。
- 每条Minecraft版本线独立推进模组版本号；不同分支可以各自发布同一个模组版本号。
- 手动分支构建必须核对分支名等于`gradle.properties`的`minecraft_version`。

## 标签与更新日志

- 新版本使用附注标签`mc<Minecraft>-v<模组版本>`，例如`mc1.20.1-v1.1.3`。
- 标签中的两个版本必须分别等于`minecraft_version`和`mod_version`。
- 对应更新日志保存在`changelog/<Minecraft>/<模组版本>.md`，同步更新`CHANGELOG.md`索引。第一行为`# Create: Bigger Storage Continued <模组版本>`，内容说明支持范围、变化、行为调整和实际验证边界。
- `release.json`中的`release_type`为`release`、`beta`或`alpha`；尚待关键验收的候选版应明确保留预发布状态。
- 已发布的`v1.1.1`、`v1.1.2`及Release保留为历史记录，不重打或移动；从下一次发版开始采用双版本标签。
- 不用其他版本源码冒充没有精确源码快照的历史版本。

## 构建与发布触发

- 日常分支推送和PR不自动构建，不因修改文档或代码触发发版。
- 只有推送`mc*-v*`标签自动构建并发布GitHub Release；构建前严格校验标签格式、两个版本及更新日志。
- 保留`workflow_dispatch`：标签为空只构建；指定符合新格式的已有标签时，检出该标签并构建发布。
- 当前不增加按代码路径触发的日常CI；以后确有需要须先确认规则。届时运行代码、资源、配方、模型、材质、翻译和构建配置都属于影响模组的文件，不能只匹配Java。
- 当前基线使用Java17和带官方SHA-256的Gradle8.12 Wrapper；打包使用已提交资源，不依赖开发者本机Minecraft路径。
- 构建和发布作业分别使用最小权限，Action固定提交SHA；不把凭据写入源码、附件或日志。
- GitHub Release名称包含模组和Minecraft版本，附件包括运行jar、源码jar、SHA-256和核验记录。
- GitHub仓库变量`LATEST_MINECRAFT_BRANCH`指定可更新Latest的Minecraft版本线，当前为`1.20.1`。只有该版本线的正式Release更新Latest；其他版本线及预发布显式使用`--latest=false`。
- 当前只接入GitHub Release；Modrinth和CurseForge另行配置项目ID及Secrets后再接入。

## 公开源码历史

- 原本机私有Git历史含上游ARR美术，保留但不推送到公开仓库。
- 公开仓库从批准美术的1.1.1快照建立独立历史；公开推送使用`build/github-public/`或公开仓库的正常克隆。
- 保留Bigger Storage及Create署名和许可，不复制Create: Connected的AGPL实现或美术。
- 同步公开检出时只同步审查过的源码；不把私有`.git`、构建缓存、存档、历史美术或本机备份带入公开历史。
