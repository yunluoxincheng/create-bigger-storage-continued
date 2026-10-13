param(
    [string]$JdkPath = 'C:\Program Files\Java\jdk-17',
    [string]$GradleCache = 'D:\DevCache\.gradle',
    [switch]$TcpFallback,
    [switch]$Offline,
    [string[]]$Tasks = @('build')
)

$ErrorActionPreference = 'Stop'
$savedJavaHome = $env:JAVA_HOME
$savedPath = $env:Path
$savedJavaOptions = $env:JAVA_TOOL_OPTIONS
$savedGradleCache = $env:GRADLE_USER_HOME
$savedLocation = Get-Location

try {
    if (!(Test-Path -LiteralPath (Join-Path $JdkPath 'bin\javac.exe') -PathType Leaf)) {
        throw 'A Java 17 JDK is required. Specify its directory with -JdkPath.'
    }
    Set-Location -LiteralPath $PSScriptRoot
    $env:JAVA_HOME = $JdkPath
    $env:Path = "$(Join-Path $JdkPath 'bin');$savedPath"
    if ($GradleCache -and (Test-Path -LiteralPath $GradleCache -PathType Container)) {
        $env:GRADLE_USER_HOME = $GradleCache
    }

    if ($TcpFallback) {
        if ($savedJavaOptions) {
            throw 'TCP fallback requires an environment without existing JAVA_TOOL_OPTIONS.'
        }
        $classes = Join-Path $PSScriptRoot 'build\tcp-agent-classes'
        New-Item -ItemType Directory -Path $classes -Force | Out-Null
        & (Join-Path $JdkPath 'bin\javac.exe') --release 17 -d $classes 'tools\TcpOnlyAgent.java'
        if ($LASTEXITCODE -ne 0) { throw 'TCP build helper compilation failed.' }
        $agentJar = Join-Path $PSScriptRoot 'build\tcp-only-agent.jar'
        & (Join-Path $JdkPath 'bin\jar.exe') --create --file $agentJar --manifest 'tools\tcp-agent.mf' -C $classes .
        if ($LASTEXITCODE -ne 0) { throw 'TCP build helper packaging failed.' }
        $env:JAVA_TOOL_OPTIONS = '-javaagent:"' + $agentJar + '"'
    }

    if ($Tasks -contains 'runData' -and ($Tasks -contains 'build' -or $Tasks -contains 'jar')) {
        # Forge runData needs processResources before loading, then changes generated inputs.
        # A second invocation ensures processResources stages those fresh files for the jar.
        $generationArguments = @('--no-daemon', 'runData')
        if ($Offline) { $generationArguments += '--offline' }
        & gradle @generationArguments
        if ($LASTEXITCODE -ne 0) { throw "Resource generation failed with exit code $LASTEXITCODE." }
        $Tasks = @($Tasks | Where-Object { $_ -ne 'runData' })
    }
    $gradleArguments = @('--no-daemon') + $Tasks
    if ($Offline) { $gradleArguments += '--offline' }
    & gradle @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }
} finally {
    $env:JAVA_HOME = $savedJavaHome
    $env:Path = $savedPath
    $env:JAVA_TOOL_OPTIONS = $savedJavaOptions
    $env:GRADLE_USER_HOME = $savedGradleCache
    Set-Location -LiteralPath $savedLocation.Path
}
