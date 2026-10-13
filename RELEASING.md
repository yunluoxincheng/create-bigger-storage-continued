# GitHub构建与版本发布

仓库：https://github.com/yunluoxincheng/create-bigger-storage-continued

## 公开历史与本地历史

公开仓库从已批准材质的1.1.1源码快照建立新的根提交，随后记录1.1.2及后续开发。原本机仓库的早期提交包含上游ARR美术，其历史保留在原目录，不直接推送到公开仓库。

公开推送使用独立检出目录`build/github-public/`，只包含公开根提交及后续提交。后续维护可直接使用该公开检出目录，或在另一个目录正常克隆公开仓库。原历史仓库不配置公开推送远程。

`v1.1.1`指向公开的1.1.1源码及构建发布配置，`v1.1.2`指向库存优化版本。1.1.0仅保留本地安装文件，没有单独提交精确的最终源码快照，不用其他版本源码冒充其可重建标签。

## 分支与版本规则

项目规范见 [AGENTS.md](AGENTS.md)。当前开发及GitHub默认分支为`1.20.1`，后续实际开始适配时建立`1.21.1`等对应分支。各分支独立推进模组版本号，可以分别发布同一个模组版本。

新标签使用`mc<Minecraft>-v<模组版本>`，例如`mc1.20.1-v1.1.3`或`mc1.21.1-v1.1.3`。标签在整个仓库中唯一，两种版本都须与该标签源码中的`gradle.properties`一致。已公开旧标签及Release保留，从下一次发版开始使用新格式，不为流程调整补打同版本新标签。

## 每个版本的准备

1. 更新`gradle.properties`中的`mod_version`。
2. 新建英文`changelog/<Minecraft>/<版本>.md`，第一行为`# Create: Bigger Storage Continued <版本>`；标题下列出发行状态、实际公开发布日期（UTC）及Minecraft、Forge、Create、Java版本。未发布版本不编造发布日期，发布后以GitHub的`published_at`核对。变化按需分为`Added`、`Changed`、`Compatibility`、`Performance`、`Fixes`，每条说明一个变化及其影响，末尾保留简洁的`Installation`。不写开发过程、构建验证、许可介绍、无关的未变化内容或重复中英段落，不宣称未经测量的性能提升；验证边界独立写入兼容／核验文档。更新英文`CHANGELOG.md`索引及中英文README中的版本和兼容信息；旧扁平日志保留为链接入口，原文由历史标签保存。
3. 设置`release.json`的`release_type`为`release`、`beta`或`alpha`。当前1.2.0为`beta`；1.1.2经用户确认已作为正式GitHub Release保留。
4. 编译并核验，再提交该版本源码和更新日志。
5. 创建双版本附注标签，将对应Minecraft分支和该标签推送到公开仓库。普通分支推送不触发构建，标签推送才构建发布。

以公开检出目录中的1.1.3为例，先完成版本及更新日志修改，再执行：

```powershell
python -B tools/PrepareRelease.py check --branch 1.20.1 --tag mc1.20.1-v1.1.3
.\gradlew.bat --no-daemon jarJar sourcesJar
python -B tools/PrepareRelease.py package
git diff --check
git status --short
# 审查并明确暂存本次版本的文件后：
git commit -m "release: prepare 1.1.3"
git tag -a mc1.20.1-v1.1.3 -m "Create: Bigger Storage Continued 1.1.3 - Minecraft 1.20.1"
git push origin 1.20.1
git push origin mc1.20.1-v1.1.3
```

本机若遇Java回环通信问题，仍可使用`build.ps1 -TcpFallback`，并按本机情况指定JDK17及Gradle缓存。GitHub的Linux构建不使用该本机辅助工具。

## 工作流行为

`.github/workflows/release.yml`只在推送`mc*-v*`标签或显式手动触发时运行。日常代码／文档推送以及PR均不构建。未指定标签的手动运行只构建与上传Actions产物；手动分支名必须等于`minecraft_version`。

已有新格式标签需要补发或重试尚未创建的Release时，可从对应Minecraft分支手动触发并指定`tag`；工作流检出该标签、验证版本，构建标签中的源码并发布。仅构建当前分支与补发1.1.3的示例分别为：

```powershell
gh workflow run release.yml --repo yunluoxincheng/create-bigger-storage-continued --ref 1.20.1
gh workflow run release.yml --repo yunluoxincheng/create-bigger-storage-continued --ref 1.20.1 -f tag=mc1.20.1-v1.1.3
```

工作流使用Java17和带官方发行包校验值的Gradle8.12 Wrapper；所有第三方Action固定提交SHA。只有发布作业获得`contents: write`，构建作业只有读取权限；不需要个人访问令牌，使用GitHub自带的`GITHUB_TOKEN`。

发布前核对标签中的Minecraft及模组版本与项目声明一致、更新日志存在且标题一致、版本发布类型有效。从1.2.0起构建`jarJar`及`sourcesJar`，`jarJar`自动执行Forge `reobfJarJar`；核对运行jar版本、资源、重复条目、许可和内嵌Flywheel版本，再上传以下附件：

- 可安装的重映射模组jar。
- 带来源许可的源码jar。
- `SHA256SUMS.txt`。
- `verification.json`，明确CI不能代替游戏内验收。

完整安装包仍使用普通版本文件名，内部只嵌入Flywheel 1.0.5及其原许可。`-unbundled.jar`为内部未内嵌产物，不上传、不作为安装文件。发布检查同时验证Jar-in-Jar元数据、客户端依赖约束、库自身版本及后端存在。候选交付移至`build/candidates/<版本>/`，本机libs只保留已接受正式版；可用`PrepareRelease.py package --artifact-dir build/candidates/<版本>`重新核验候选包。

Release标题为`Create: Bigger Storage Continued <模组版本> — Minecraft <Minecraft版本>`，正文来自该标签中的`changelog/<Minecraft>/<版本>.md`。Actions产物名和本地打包目录也同时包含两个版本，避免不同Minecraft版本线互相覆盖。

用户授权统一既有发布说明时，在当前Minecraft分支中修改对应英文日志，再用`gh release edit <已有标签> --notes-file <对应日志>`同步正文；需要时统一标题。此类文案修订保留标签、发行状态、Latest归属、初始发布日期和全部附件，不重新构建或移动标签。发布后文案可以与标签内原始日志不同；当前分版本日志与Release正文须一致，原始文本由历史标签保存。1.1.1、1.1.2及1.2.0采用相同文档标准。

GitHub的Latest标记在整个仓库共用。仓库变量`LATEST_MINECRAFT_BRANCH`当前设置为`1.20.1`，仅该版本线的正式Release可更新Latest；其他版本线和预发布显式使用`--latest=false`。以后要将Latest交给其他Minecraft版本线时，明确修改该仓库变量；默认分支变化不会隐式更改Latest归属。

已发布标签的重复运行不会覆盖已有附件；修正已发布版本时使用新版本和新标签，避免移动已公开标签。首批`v1.1.1`、`v1.1.2`曾使用旧工作流补发，当前工作流只接受新格式标签。

用户明确接受已有预发布作为正式版时，可更新当前分支的发行类型和说明，并编辑既有GitHub Release取消预发布标记；符合Latest归属规则时将其设为Latest。此操作保留标签、原构建附件及初始构建记录，不重新打标签、不补发同版本标签，也不把接受意见当作新增测试。1.1.2已于2026-10-13按此规则由Beta提升为正式版；原始更新日志由历史标签保存，当前分目录日志及Release使用统一的英文格式。

## Modrinth与CurseForge

本阶段仅发布GitHub Release。后续接入时需要各平台项目ID和仓库Secrets，并沿用相同的版本号、英文更新日志、发行类型、游戏版本及加载器信息；对外项目介绍以英文README为基础。当前没有添加占位Token，也不会自动向这两个平台上传。
