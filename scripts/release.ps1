param(
    [switch] $DryRun,
    [string] $Since,
    [string] $Changelog,
    [ValidateSet('release', 'beta', 'alpha')]
    [string] $VersionType = 'release',
    [switch] $NoTag
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$projectId = 'pf8Myc7A'
$fabricApiProjectId = 'P7dR8mSH'
$gameVersions = @('26.1.2', '26.2')
$loaders = @('fabric')

function Get-Section {
    param([string[]] $Subjects, [string] $Pattern, [string] $Heading)

    $items = @($Subjects | Where-Object { $_ -match $Pattern })
    if ($items.Count -eq 0) { return $null }

    $lines = @("## $Heading", "")
    foreach ($item in $items) {
        $lines += "- $item"
    }
    return ($lines -join "`n")
}

function New-Changelog {
    param([string] $Base)

    $subjects = @(git log --no-merges --pretty=format:'%s' "$Base..HEAD")
    $subjects = @($subjects |
        ForEach-Object {
            ($_ -replace '^(feat|fix|chore|docs|refactor|test)(\([^)]*\))?:\s*', '') -replace '\s+\(#\d+\)$', ''
        } |
        Where-Object { $_ -and $_ -notmatch '^(bump|release|update mod_version)' } |
        ForEach-Object { $_.Substring(0, 1).ToUpper() + $_.Substring(1) })

    $sections = @(
        (Get-Section $subjects '^(?i)add' 'Added')
        (Get-Section $subjects '^(?i)fix' 'Fixed')
        (Get-Section $subjects '^(?i)(?!add|fix)' 'Improved')
    ) | Where-Object { $_ }

    return (($sections -join "`n`n").Trim())
}

Push-Location $repo
try {
    $properties = Get-Content -LiteralPath (Join-Path $repo 'gradle.properties')
    $match = $properties | Select-String -Pattern '^mod_version=(.+)$'
    if (-not $match) { throw 'Could not read mod_version from gradle.properties' }
    $version = $match.Matches[0].Groups[1].Value.Trim()
    Write-Host "Releasing EMUtils $version"

    if ($Changelog) {
        if (-not (Test-Path -LiteralPath $Changelog)) { throw "Changelog file not found: $Changelog" }
        $changelogText = (Get-Content -LiteralPath $Changelog -Raw).Trim()
        Write-Host "Using changelog from $Changelog"
    }
    else {
        $base = $Since
        if (-not $base) {
            $base = (git describe --tags --abbrev=0 --match 'v*' 2>$null)
        }
        if (-not $base) {
            throw "No v* tag found. Pass -Since <ref> (for example -Since 1bb78ed) or -Changelog <path>."
        }
        Write-Host "Generating changelog from $base..HEAD"
        $changelogText = New-Changelog $base
    }

    $changelogPath = Join-Path $repo "build\release-changelog-$version.md"
    Write-Host ""
    Write-Host "---- changelog ----"
    Write-Host $changelogText
    Write-Host "-------------------"

    Write-Host "Building EMUtils-26.x.jar..."
    java -classpath '.\gradle\wrapper\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain clean build -PmcFamily='26.x'

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $jar = Join-Path $repo 'build\libs\EMUtils-26.x.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing release jar: $jar" }
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $entry = $zip.GetEntry('fabric.mod.json')
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try { $meta = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    }
    finally { $zip.Dispose() }
    if ($meta.id -ne 'emutils') { throw "Unexpected mod id in jar: $($meta.id)" }
    Write-Host "Verified jar: $($meta.id) $($meta.version)"

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $changelogPath) | Out-Null
    Set-Content -LiteralPath $changelogPath -Value $changelogText -Encoding utf8

    if ($DryRun) {
        Write-Host ""
        Write-Host "Dry run complete. Changelog: $changelogPath"
        Write-Host "Would upload $jar to Modrinth project $projectId as $version ($VersionType), game versions $($gameVersions -join ', ')."
        return
    }

    $token = $env:MODRINTH_TOKEN
    if (-not $token) {
        throw "MODRINTH_TOKEN is not set. Create a token at https://modrinth.com/settings/pats and set it as an environment variable."
    }

    $payload = [ordered]@{
        name           = "EMUtils $version"
        version_number = $version
        changelog      = $changelogText
        dependencies   = @(
            [ordered]@{ project_id = $fabricApiProjectId; dependency_type = 'required' }
        )
        game_versions  = $gameVersions
        version_type   = $VersionType
        loaders        = $loaders
        project_id     = $projectId
        featured       = $false
        status         = 'listed'
    } | ConvertTo-Json -Depth 6 -Compress

    Write-Host "Uploading to Modrinth..."
    $response = Invoke-RestMethod `
        -Method Post `
        -Uri 'https://api.modrinth.com/v2/version' `
        -Headers @{ Authorization = $token } `
        -Form @{ data = $payload; file = Get-Item -LiteralPath $jar }

    Write-Host "Published Modrinth version $($response.version_number) (id $($response.id))."

    if (-not $NoTag) {
        $tag = "v$version"
        if (-not (git tag --list $tag)) {
            git tag $tag
            git push origin $tag
        }
        gh release create $tag $jar --title "EMUtils $version" --notes-file $changelogPath
        Write-Host "Created GitHub release $tag."
    }
}
finally {
    Pop-Location
}
