param(
    [Parameter(Mandatory = $true)]
    [string] $Version
)

# Writes gradle/mappings/<version>-official-identity.tiny: an identity (official -> named) class list
# for every net.minecraft and com.mojang class in the client jar and the server bundler jar.
# 26.x ships unobfuscated, so Loom only needs class names to map the Mojang namespace onto itself.

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$output = Join-Path $repo "gradle\mappings\$Version-official-identity.tiny"

$manifest = Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
$entry = $manifest.versions | Where-Object { $_.id -eq $Version } | Select-Object -First 1
if (-not $entry) { throw "Minecraft $Version is not in Mojang's version manifest" }
$versionMeta = Invoke-RestMethod $entry.url

$temp = Join-Path ([System.IO.Path]::GetTempPath()) "emutils-mappings-$Version"
New-Item -ItemType Directory -Force -Path $temp | Out-Null

Add-Type -AssemblyName System.IO.Compression.FileSystem
$classes = [System.Collections.Generic.SortedSet[string]]::new([System.StringComparer]::Ordinal)
try {
    foreach ($side in @('client', 'server')) {
        $download = $versionMeta.downloads.$side
        $jar = Join-Path $temp "$side.jar"
        Write-Host "Downloading Minecraft $Version $side jar..."
        Invoke-WebRequest -Uri $download.url -OutFile $jar
        $hash = (Get-FileHash -LiteralPath $jar -Algorithm SHA1).Hash.ToLowerInvariant()
        if ($hash -ne $download.sha1) { throw "SHA-1 mismatch for the $side jar: $hash, expected $($download.sha1)" }

        $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
        try {
            foreach ($zipEntry in $zip.Entries) {
                $name = $zipEntry.FullName
                if ($name.EndsWith('.class') -and ($name.StartsWith('net/minecraft/') -or $name.StartsWith('com/mojang/'))) {
                    [void] $classes.Add($name.Substring(0, $name.Length - '.class'.Length))
                }
            }
        }
        finally { $zip.Dispose() }
    }
}
finally {
    Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("tiny`t2`t0`tofficial`tnamed")
foreach ($class in $classes) {
    $lines.Add("c`t$class`t$class")
}
[System.IO.File]::WriteAllText($output, ($lines -join "`n") + "`n", [System.Text.UTF8Encoding]::new($false))
Write-Host "Wrote $($classes.Count) classes to $output"
