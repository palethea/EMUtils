param(
    [string] $Base = 'origin/master',
    [switch] $Full
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot

Push-Location $repo
try {
    $changed = @(git diff --name-only "$Base...HEAD" 2>$null)
    if ($changed.Count -eq 0) {
        $changed = @(git diff --name-only 2>$null)
    }
}
finally {
    Pop-Location
}

$sensitive = @($changed | Where-Object {
    $_ -match '^src/26_x/.*/mixin/' -or
    $_ -match '^src/26_\d+/' -or
    $_ -match '\.accesswidener$' -or
    $_ -match 'mixins\.json$' -or
    $_ -match 'EMUtilsMixinPlugin\.java$' -or
    $_ -match '^gradle/mappings/' -or
    $_ -eq 'build.gradle'
})

. (Join-Path $PSScriptRoot 'mc-versions.ps1')
$supportedVersions = Get-SupportedMcVersions

$runner = Join-Path $PSScriptRoot 'test-26x-launches.ps1'
if ($Full -or $sensitive.Count -gt 0) {
    if ($sensitive.Count -gt 0) {
        Write-Host "Version-sensitive files changed:"
        $sensitive | ForEach-Object { Write-Host "  $_" }
    }
    Write-Host "Running the full supported 26.x matrix ($($supportedVersions -join ', '))."
    & $runner
}
else {
    Write-Host "No version-sensitive files changed; running the latest version only ($($supportedVersions[-1]))."
    & $runner -Latest
}
