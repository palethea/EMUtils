param(
    [string[]] $Versions = @('26.1.2', '26.2'),
    [int] $TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$logDir = Join-Path $repo 'build\launch-smoke-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$emHelpers = Resolve-Path (Join-Path $repo '..\EMHelpers')

function Show-LogTail {
    param(
        [string] $Path,
        [int] $Lines = 120
    )

    if (Test-Path -LiteralPath $Path) {
        Write-Host ""
        Write-Host "---- $Path tail ----"
        Get-Content -LiteralPath $Path -Tail $Lines
    }
}

Push-Location $repo
try {
    Write-Host "Compiling EMHelpers for 26.x dev runtime..."
    Push-Location $emHelpers
    try {
        java -classpath '.\gradle\wrapper\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain compileClientJava -PmcFamily='26.x' --stacktrace
    }
    finally {
        Pop-Location
    }

    foreach ($version in $Versions) {
        Write-Host "Launching Minecraft $version into a singleplayer test world with EMUtils smoke verifier..."

        $stdout = Join-Path $logDir "26x-$version.out.log"
        $stderr = Join-Path $logDir "26x-$version.err.log"
        Remove-Item -LiteralPath $stdout, $stderr -Force -ErrorAction SilentlyContinue

        $arguments = @(
            '-classpath', '.\gradle\wrapper\gradle-wrapper.jar',
            'org.gradle.wrapper.GradleWrapperMain',
            '--no-daemon',
            'runClient',
            "-PmcFamily=26.x",
            "-PmcVersion=$version",
            "-PemutilsSmokeLaunch=true",
            '--stacktrace'
        )

        $process = Start-Process `
            -FilePath 'java' `
            -ArgumentList $arguments `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdout `
            -RedirectStandardError $stderr

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            Stop-Process -Id $process.Id -Force
            Show-LogTail $stdout
            Show-LogTail $stderr
            throw "Minecraft $version did not finish within $TimeoutSeconds seconds."
        }

        if ($process.ExitCode -ne 0) {
            Show-LogTail $stdout
            Show-LogTail $stderr
            throw "Minecraft $version failed with exit code $($process.ExitCode)."
        }

        $latestLog = Join-Path $repo "run\26x-smoke\$version\logs\latest.log"
        $marker = 'EMUtils smoke launch verifier reached singleplayer world; stopping Minecraft.'
        $stdoutHasMarker = (Test-Path -LiteralPath $stdout) -and (Select-String -LiteralPath $stdout -SimpleMatch $marker -Quiet)
        $latestHasMarker = (Test-Path -LiteralPath $latestLog) -and (Select-String -LiteralPath $latestLog -SimpleMatch $marker -Quiet)
        if (-not ($stdoutHasMarker -or $latestHasMarker)) {
            Show-LogTail $stdout
            Show-LogTail $stderr
            Show-LogTail $latestLog
            throw "Minecraft $version exited without the EMUtils smoke verifier success marker."
        }

        Write-Host "Minecraft $version reached a singleplayer world and shut down cleanly."
    }
}
finally {
    Pop-Location
}

Write-Host "All supported 26.x singleplayer launch smoke tests passed."
