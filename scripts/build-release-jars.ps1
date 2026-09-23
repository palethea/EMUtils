param(
    [string[]] $Versions
)

# Builds and verifies one release jar per supported Minecraft version and collects them in dist\.
# Each version gets a clean build because every target compiles into the shared build\ directory.

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

. (Join-Path $PSScriptRoot 'mc-versions.ps1')
if (-not $Versions -or $Versions.Count -eq 0) {
    $Versions = Get-SupportedMcVersions
}

$modVersion = (Get-Content -LiteralPath (Join-Path $repo 'gradle.properties') |
    Select-String -Pattern '^mod_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$dist = Join-Path $repo 'dist'
Remove-Item -LiteralPath $dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $dist | Out-Null

Add-Type -AssemblyName System.IO.Compression.FileSystem

Push-Location $repo
try {
    foreach ($mcVersion in $Versions) {
        Write-Host "Building EMUtils $modVersion for Minecraft $mcVersion..."
        java -classpath '.\gradle\wrapper\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain clean build -PmcFamily='26.x' "-PmcVersion=$mcVersion"
        if ($LASTEXITCODE -ne 0) { throw "Build failed for Minecraft $mcVersion" }

        $jar = Join-Path $repo "build\libs\EMUtils-$mcVersion.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Missing release jar: $jar" }
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
        try {
            $entry = $zip.GetEntry('fabric.mod.json')
            if ($null -eq $entry) { throw "Missing fabric.mod.json in $jar" }
            $reader = [System.IO.StreamReader]::new($entry.Open())
            try { $meta = $reader.ReadToEnd() | ConvertFrom-Json }
            finally { $reader.Dispose() }
            if ($null -eq $zip.GetEntry('emutils.versioned.mixins.json')) { throw "Missing emutils.versioned.mixins.json in $jar" }
        }
        finally { $zip.Dispose() }

        if ($meta.id -ne 'emutils') { throw "Unexpected mod id in ${jar}: $($meta.id)" }
        if ($meta.version -ne $modVersion) { throw "Unexpected mod version in ${jar}: $($meta.version), expected $modVersion" }
        if ($meta.depends.minecraft -notmatch [regex]::Escape(">=$mcVersion-")) {
            throw "Unexpected Minecraft dependency in ${jar}: $($meta.depends.minecraft)"
        }

        Copy-Item -LiteralPath $jar -Destination $dist
        Write-Host "EMUtils-$mcVersion.jar -> $($meta.id) $($meta.version), minecraft $($meta.depends.minecraft)"
    }
}
finally {
    Pop-Location
}

Write-Host ""
Write-Host "Release jars in ${dist}:"
Get-ChildItem -LiteralPath $dist -Filter 'EMUtils-*.jar' | ForEach-Object { Write-Host "  $($_.Name)" }
