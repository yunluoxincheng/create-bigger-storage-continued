# Building and releasing

This project uses Minecraft-named branches, annotated release tags and GitHub Releases. Project rules are in [AGENTS.md](../AGENTS.md).

## Build

Use Java 17 and the included Gradle 8.12 Wrapper. From the repository root:

```powershell
.\gradlew.bat --no-daemon jarJar sourcesJar
```

On Linux or macOS:

```bash
bash ./gradlew --no-daemon jarJar sourcesJar
```

For 1.2.x, `jarJar` produces the ordinary versioned runtime JAR with Flywheel 1.0.5 and executes `reobfJarJar`. The `-unbundled.jar` is an internal artifact. `sourcesJar` produces the source archive. Normal builds use committed resources; resource generation is a separate operation.

On Windows, `build.ps1 -TcpFallback` can work around Java loopback communication issues. Specify `-JdkPath` and `-GradleCache` when local defaults do not apply; `-Offline` requires cached dependencies. The helper is not part of the mod JAR.

## Branches and tags

- Use a branch such as `1.20.1`; create a new Minecraft branch only when adaptation starts.
- Each Minecraft line advances its mod version independently.
- New annotated tags use `mc<Minecraft>-v<mod>`, for example `mc1.20.1-v1.2.1`.
- Both versions must match `gradle.properties`. Published tags and artifacts are immutable.
- Historical `v1.1.1` and `v1.1.2` tags remain valid. Public source history begins with 1.1.1; no exact public 1.1.0 source tag is claimed.

## Prepare a release

1. Set `mod_version` and `release.json` (`release`, `beta` or `alpha`).
2. Write English notes in `changelog/<Minecraft>/<mod>.md`, beginning with `# Create: Bigger Storage Continued <mod>`. Include status, requirements, relevant changes and installation notes. Use `Unreleased` before publication; verify actual publication dates against GitHub `published_at` in UTC.
3. Update [CHANGELOG.md](../CHANGELOG.md), both READMEs and [compatibility information](COMPATIBILITY.md). Move applicable entries from `UNRELEASED.md` into the new version notes.
4. Build and run the release validation/package commands below. Review the diff and stage only the intended files.
5. Commit, create an annotated tag, then push the Minecraft branch and that tag.

Example for a future 1.2.2 release, after its version files have been updated:

```powershell
python -B tools/PrepareRelease.py check --branch 1.20.1 --tag mc1.20.1-v1.2.2
.\gradlew.bat --no-daemon jarJar sourcesJar
python -B tools/PrepareRelease.py package
```

The package check verifies version metadata, resources, notices, duplicate entries, the embedded library and its dependency constraints. Output includes the runtime JAR, source JAR, `SHA256SUMS.txt` and `verification.json`. For local candidate artifacts, pass `--artifact-dir build/candidates/<mod>`; local `build/libs` retains accepted formal versions.

## GitHub workflow

Only a push of `mc*-v*` tags triggers automatic build and release. Ordinary branch pushes and pull requests do not build. Actions use Java 17, the pinned Wrapper and pinned action commits. Build jobs have read permissions; only the publishing job has `contents: write` and uses `GITHUB_TOKEN`.

`workflow_dispatch` without a tag builds the selected Minecraft branch and uploads Actions artifacts. With an existing valid tag, it checks out that tag and builds/releases its source. Manual branch names must match `minecraft_version`.

```powershell
gh workflow run release.yml --repo yunluoxincheng/create-bigger-storage-continued --ref 1.20.1
```

Release titles use `Create: Bigger Storage Continued <mod> — Minecraft <Minecraft>`. Initial notes come from the tag's changelog. After an authorized editorial revision, the maintained branch notes and existing Release body must match; the tag and attachments remain unchanged.

The repository variable `LATEST_MINECRAFT_BRANCH` selects which Minecraft line may update Latest, currently `1.20.1`. Only stable releases on that line update it. Other lines and prereleases use `--latest=false`. Beta acceptance may promote an existing Release when explicitly authorized, preserving its tag and attachments; acceptance does not establish additional tests.

## Public history and other platforms

The original local Git history contains upstream ARR artwork and must not be pushed. Publish from `build/github-public/` or a normal clone of the public repository, copying only reviewed tracked content. Historical records and generated previews remain local and ignored.

Modrinth and CurseForge uploads are not configured. Before adding project IDs and Secrets, review [attribution and platform requirements](ATTRIBUTION.md), retain full notices, and use the same English notes and version metadata on all platforms. AI-use disclosure, platform approval and copyright permission are separate matters.
