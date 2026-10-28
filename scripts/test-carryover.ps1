param(
    [string[]] $Versions,
    # The old releases to start from, as folders in src\test\carry-over; all of them by default.
    [string[]] $Releases,
    [int] $TimeoutSeconds = 240
)

# Carry-over tests (#267): starts a smoke launch with an old release's settings, keybinds and data files, in
# which every setting differs from its default, and checks in the game that this build kept all of them
# (see CarryOverCheck). To add a release, make its folder in src\test\carry-over from that release's own files.

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

. (Join-Path $PSScriptRoot 'mc-versions.ps1')
if (-not $Versions -or $Versions.Count -eq 0) {
    $Versions = Get-SupportedMcVersions
}
if (-not $Releases -or $Releases.Count -eq 0) {
    $Releases = @(Get-ChildItem -LiteralPath (Join-Path $repo 'src\test\carry-over') -Directory | ForEach-Object Name)
}

$logDir = Join-Path $repo 'build\carryover-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$failures = New-Object System.Collections.Generic.List[string]
Push-Location $repo
try {
    foreach ($version in $Versions) {
        foreach ($release in $Releases) {
            Write-Host "Minecraft $version`: starting from EMUtils $release's files..."
            $stdout = Join-Path $logDir "$version-$release.out.log"
            $stderr = Join-Path $logDir "$version-$release.err.log"
            Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue
            $arguments = @(
                '-classpath', '.\gradle\wrapper\gradle-wrapper.jar',
                'org.gradle.wrapper.GradleWrapperMain',
                'runClient',
                '-PmcFamily=26.x',
                "-PmcVersion=$version",
                '-PemutilsSmokeLaunch=true',
                "-PemutilsCarryOver=$release"
            )
            $process = Start-Process -FilePath 'java' -ArgumentList $arguments -NoNewWindow -PassThru `
                -RedirectStandardOutput $stdout -RedirectStandardError $stderr
            if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
                Stop-Process -Id $process.Id -Force
                $failures.Add("$version from $release`: did not finish within $TimeoutSeconds seconds")
                continue
            }

            $log = Join-Path $repo "run\26x-carryover\$version\logs\latest.log"
            if ($process.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $log)) {
                $failures.Add("$version from $release`: the run failed (exit code $($process.ExitCode), see $stdout)")
                continue
            }
            foreach ($failed in Select-String -LiteralPath $log -SimpleMatch 'EMUtils carry-over check FAILED' | Sort-Object Line -Unique) {
                $failures.Add("$version from $release`: " + ($failed.Line -replace '^.*check FAILED: ', ''))
            }
            if (-not (Select-String -LiteralPath $log -SimpleMatch 'EMUtils carry-over check passed' -Quiet) -and
                -not (Select-String -LiteralPath $log -SimpleMatch 'EMUtils carry-over check FAILED' -Quiet)) {
                $failures.Add("$version from $release`: the check didn't run (see $log)")
            }
        }
    }
}
finally {
    Pop-Location
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "Carry-over problems:"
    $failures | ForEach-Object { Write-Host "  $_" }
    throw "$($failures.Count) carry-over check(s) failed."
}
Write-Host "Settings, keybinds and data files from $($Releases -join ', ') carry over on $($Versions -join ', ')."
