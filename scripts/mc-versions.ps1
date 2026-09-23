# Dot-source this file to read the supported Minecraft versions from gradle.properties.

function Get-SupportedMcVersions {
    $properties = Join-Path (Split-Path -Parent $PSScriptRoot) 'gradle.properties'
    $match = Get-Content -LiteralPath $properties | Select-String -Pattern '^supported_mc_versions=(.+)$'
    if (-not $match) { throw "Could not read supported_mc_versions from $properties" }
    $versions = @($match.Matches[0].Groups[1].Value.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($versions.Count -eq 0) { throw "supported_mc_versions in $properties is empty" }
    # The leading comma stops PowerShell from unwrapping a one-version list into a plain string.
    return , $versions
}
