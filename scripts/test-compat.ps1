param(
    [string[]] $Versions,
    [string[]] $Combos = @('none', 'sodium', 'sodium,iris,modmenu'),
    [int] $TimeoutSeconds = 600,
    # How far a feature's changed pixels may be from the baseline's, as a factor either way.
    [double] $Tolerance = 2.0
)

# Visual compatibility tests (#246): runs the compatibility scenes (UI snapshot steps 1000 and up) without
# other mods, for the baseline, and with Sodium, Iris (a second round with its test shader pack on) and Mod
# Menu. Each run checks that every feature visibly changes the picture; this script then checks that it
# changes about as much as in the baseline, so a feature drawn only partly, or far too much, fails too.
# The frames, with pictures of what changed, are in run\26x-compat\<version>\<mods>\screenshots\compat.

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

. (Join-Path $PSScriptRoot 'mc-versions.ps1')
if (-not $Versions -or $Versions.Count -eq 0) {
    $Versions = Get-SupportedMcVersions
}
if ($Combos -notcontains 'none') {
    $Combos = @('none') + $Combos
}

$logDir = Join-Path $repo 'build\compat-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Get-RunName([string] $combo) {
    $mods = @($combo -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -ne 'none' } | Sort-Object -Unique)
    if ($mods.Count -eq 0) { 'baseline' } else { $mods -join '-' }
}

function Read-Results([string] $path) {
    $results = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        $fields = $line -split "`t"
        if ($fields.Count -ge 5) {
            $results[$fields[0]] = [pscustomobject]@{ Signal = [int] $fields[1]; Noise = [int] $fields[2]; Passed = $fields[4] -eq 'pass' }
        }
    }
    $results
}

$failures = New-Object System.Collections.Generic.List[string]
Push-Location $repo
try {
    foreach ($version in $Versions) {
        $baseline = $null
        foreach ($combo in $Combos) {
            $name = Get-RunName $combo
            Write-Host "Minecraft $version, $name`: running the compatibility scenes..."
            $stdout = Join-Path $logDir "$version-$name.out.log"
            $stderr = Join-Path $logDir "$version-$name.err.log"
            Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
            $arguments = @(
                '-classpath', '.\gradle\wrapper\gradle-wrapper.jar',
                'org.gradle.wrapper.GradleWrapperMain',
                'runClient',
                '-PmcFamily=26.x',
                "-PmcVersion=$version",
                '-PemutilsUiSnapshot=true',
                '-PemutilsUiSnapshotFrom=1000',
                "-PemutilsCompat=$combo"
            )
            $process = Start-Process -FilePath 'java' -ArgumentList $arguments -NoNewWindow -PassThru `
                -RedirectStandardOutput $stdout -RedirectStandardError $stderr
            if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
                Stop-Process -Id $process.Id -Force
                $failures.Add("$version $name`: did not finish within $TimeoutSeconds seconds")
                continue
            }

            $runDir = Join-Path $repo "run\26x-compat\$version\$name"
            $log = Join-Path $runDir 'logs\latest.log'
            $resultsFile = Join-Path $runDir 'compat-results.tsv'
            if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $log) -or
                -not (Select-String -LiteralPath $log -SimpleMatch 'EMUtils compatibility scenes done' -Quiet)) {
                $failures.Add("$version $name`: the run didn't get through the scenes (exit code $($process.ExitCode), see $stdout)")
                continue
            }
            foreach ($failed in Select-String -LiteralPath $log -SimpleMatch 'EMUtils UI snapshot check FAILED') {
                $failures.Add("$version $name`: " + ($failed.Line -replace '^.*check FAILED: ', ''))
            }

            $results = Read-Results $resultsFile
            if ($name -eq 'baseline') {
                $baseline = $results
                continue
            }
            if ($null -eq $baseline) {
                $failures.Add("$version $name`: no baseline to compare with")
                continue
            }
            foreach ($scene in $results.Keys | Sort-Object) {
                $reference = $baseline[$scene -replace '-shaders$', '']
                if ($null -eq $reference -or $reference.Signal -le 0) {
                    continue
                }
                $ratio = $results[$scene].Signal / $reference.Signal
                if ($ratio -lt 1 / $Tolerance -or $ratio -gt $Tolerance) {
                    $failures.Add(("{0} {1}: {2} changes {3} pixels, {4:0.00} times the baseline's {5}" -f
                        $version, $name, $scene, $results[$scene].Signal, $ratio, $reference.Signal))
                }
            }
        }
    }
}
finally {
    Pop-Location
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Compatibility problems:"
    $failures | ForEach-Object { Write-Host "  $_" }
    throw "$($failures.Count) compatibility check(s) failed. The frames are in run\26x-compat\<version>\<mods>\screenshots\compat."
}
Write-Host "All compatibility scenes look like the baseline's."
