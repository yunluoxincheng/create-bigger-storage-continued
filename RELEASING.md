# GitHub构建与版本发布

仓库：https://github.com/yunluoxincheng/create-bigger-storage-continued

## 公开历史与本地历史

公开仓库从已批准材质的1.1.1源码快照建立新的根提交，随后记录1.1.2及后续开发。原本机仓库的早期提交包含上游ARR美术，其历史保留在原目录，不直接推送到公开仓库。

首次公开推送使用独立检出目录`build/github-public/`，只包含公开根提交及后续提交。后续维护可直接使用该公开检出目录，或在另一个目录正常克隆公开仓库。不要给原历史仓库配置公开远程后直接推送它的`main`或全部引用。

`v1.1.1`指向公开的1.1.1源码及构建发布配置，`v1.1.2`指向库存优化版本。1.1.0仅保留本地安装文件，没有单独提交精确的最终源码快照，不用其他版本源码冒充其可重建标签。

## 每个版本的准备

1. 更新`gradle.properties`中的`mod_version`。
2. 新建`changelog/<版本>.md`，第一行为`# Create: Bigger Storage Continued <版本>`，写明变化、支持范围和实际验证边界；更新`CHANGELOG.md`索引。
3. 设置`release.json`的`release_type`为`release`、`beta`或`alpha`。当前1.1.2为`beta`，GitHub Release标记预发布。
4. 编译并核验，再提交该版本源码和更新日志。
5. 创建附注标签`v<版本>`，将分支和该标签推送到公开仓库。

以公开检出目录中的1.1.3为例，先完成版本及更新日志修改，再执行：

```powershell
python -B tools/PrepareRelease.py check --tag v1.1.3
.\gradlew.bat --no-daemon jar sourcesJar
python -B tools/PrepareRelease.py package
git diff --check
git status --short
# 审查并明确暂存本次版本的文件后：
git commit -m "release: prepare 1.1.3"
git tag -a v1.1.3 -m "Create: Bigger Storage Continued 1.1.3"
git push origin main
git push origin v1.1.3
```

本机若遇Java回环通信问题，仍可使用`build.ps1 -TcpFallback`，并按本机情况指定JDK17及Gradle缓存。GitHub的Linux构建不使用该本机辅助工具。

## 工作流行为

`.github/workflows/release.yml`在`main`推送、面向`main`的PR和手动触发时只构建与上传Actions产物。推送`v*`标签时，构建成功后自动创建GitHub Release。

工作流使用Java17和带官方发行包校验值的Gradle8.12 Wrapper；所有第三方Action固定提交SHA。只有发布作业获得`contents: write`，构建作业只有读取权限；不需要个人访问令牌，使用GitHub自带的`GITHUB_TOKEN`。

发布前核对标签与`mod_version`一致、更新日志标题一致、版本发布类型有效。构建`jar`及`sourcesJar`，`jar`自动执行Forge `reobfJar`；核对运行jar版本、资源、重复条目及许可，再上传以下附件：

- 可安装的重映射模组jar。
- 带来源许可的源码jar。
- `SHA256SUMS.txt`。
- `verification.json`，明确CI不能代替游戏内验收。

Release正文来自该标签中的`changelog/<版本>.md`。预发布不会替换GitHub的最新正式版标记。已发布标签的重复运行不会覆盖已有附件；修正已发布版本时使用新版本和新标签，避免移动已公开标签。

## Modrinth与CurseForge

本阶段仅发布GitHub Release。后续接入时需要各平台项目ID和仓库Secrets，并沿用相同的版本号、更新日志、发行类型、游戏版本及加载器信息。当前没有添加占位Token，也不会自动向这两个平台上传。
