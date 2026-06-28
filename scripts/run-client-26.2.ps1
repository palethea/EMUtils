param(
    [switch] $WaitForDebugger,
    [switch] $NoLaunchTestWorld
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$args = @(
    '-classpath', '.\gradle\wrapper\gradle-wrapper.jar',
    'org.gradle.wrapper.GradleWrapperMain',
    'runClient',
    "-PmcFamily=26.x",
    "-PmcVersion=26.2",
    '--stacktrace'
)

if (-not $NoLaunchTestWorld) {
    $args += "-PemutilsLaunchTestWorld=true"
}

if ($WaitForDebugger) {
    $args += '--debug-jvm'
}

Push-Location $repo
try {
    java @args
}
finally {
    Pop-Location
}
