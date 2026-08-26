param(
    [string]$GradleUserHome = "D:\GradleUserHome\rider-symbol-graph",
    [string]$BuildDir = "D:\GradleBuilds\rider-symbol-graph",
    [string]$JavaHome = "D:\software\JDK21",
    [string]$GradleHome = "D:\software\gradle-9.0.0"
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

New-Item -ItemType Directory -Force -Path $GradleUserHome, $BuildDir | Out-Null
$env:GRADLE_USER_HOME = $GradleUserHome
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;" + $env:Path
$projectCacheDir = Join-Path $BuildDir "gradle-project-cache"
New-Item -ItemType Directory -Force -Path $projectCacheDir | Out-Null
$gradleArgs = @("--project-cache-dir", $projectCacheDir, "-PsymbolGraphBuildDir=$BuildDir", "buildPlugin")

Push-Location $projectDir
try {
    if (Test-Path -LiteralPath ".\gradlew.bat") {
        & ".\gradlew.bat" @gradleArgs
    } elseif (Test-Path -LiteralPath "$GradleHome\bin\gradle.bat") {
        & "$GradleHome\bin\gradle.bat" @gradleArgs
    } else {
        & gradle @gradleArgs
    }
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
