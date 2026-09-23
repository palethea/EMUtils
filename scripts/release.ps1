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
$loaders = @('fabric')

. (Join-Path $PSScriptRoot 'mc-versions.ps1')
$mcVersions = Get-SupportedMcVersions

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

    & (Join-Path $PSScriptRoot 'build-release-jars.ps1') -Versions $mcVersions

    $releases = @($mcVersions | ForEach-Object {
        [pscustomobject]@{
            McVersion     = $_
            VersionNumber = "$version+$_"
            Jar           = Join-Path $repo "dist\EMUtils-$_.jar"
        }
    })

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $changelogPath) | Out-Null
    Set-Content -LiteralPath $changelogPath -Value $changelogText -Encoding utf8

    if ($DryRun) {
        Write-Host ""
        Write-Host "Dry run complete. Changelog: $changelogPath"
        foreach ($release in $releases) {
            Write-Host "Would upload $($release.Jar) to Modrinth project $projectId as $($release.VersionNumber) ($VersionType), game version $($release.McVersion)."
        }
        Write-Host "Would tag v$version and create one GitHub release with $($releases.Count) jar(s)."
        return
    }

    $token = $env:MODRINTH_TOKEN
    if (-not $token) {
        $tokenFile = Join-Path $env:USERPROFILE '.modrinth-token'
        if (Test-Path -LiteralPath $tokenFile) {
            $token = (Get-Content -LiteralPath $tokenFile -Raw).Trim()
        }
    }
    if (-not $token) {
        throw "MODRINTH_TOKEN is not set. Create a token at https://modrinth.com/settings/pats and either set the MODRINTH_TOKEN environment variable or save it to $env:USERPROFILE\.modrinth-token."
    }

    # Modrinth applies game versions to a whole version, so each Minecraft version is its own upload.
    # Uploads stop at the first failure; the tag and GitHub release are only created once all succeed.
    # Versions already on Modrinth are skipped, so rerunning after a partial failure finishes the release.
    $published = @(Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$projectId/version" -Headers @{ Authorization = $token } |
        ForEach-Object { $_.version_number })
    foreach ($release in $releases) {
        if ($published -contains $release.VersionNumber) {
            Write-Host "Modrinth version $($release.VersionNumber) is already published; skipping its upload."
            continue
        }

        $payload = [ordered]@{
            name           = "EMUtils $version for Minecraft $($release.McVersion)"
            version_number = $release.VersionNumber
            changelog      = $changelogText
            dependencies   = @(
                [ordered]@{ project_id = $fabricApiProjectId; dependency_type = 'required' }
            )
            game_versions  = @($release.McVersion)
            version_type   = $VersionType
            loaders        = $loaders
            project_id     = $projectId
            featured       = $false
            status         = 'listed'
            file_parts     = @('file')
            primary_file   = 'file'
        } | ConvertTo-Json -Depth 6 -Compress

        $payloadPath = Join-Path $repo "build\release-payload-$($release.VersionNumber).json"
        Set-Content -LiteralPath $payloadPath -Value $payload -Encoding utf8 -NoNewline

        Write-Host "Uploading $($release.VersionNumber) to Modrinth..."
        $responseOutput = & curl.exe --fail-with-body -sS -X POST 'https://api.modrinth.com/v2/version' `
            -H "Authorization: $token" `
            -F "data=<$payloadPath" `
            -F "file=@$($release.Jar);type=application/java-archive"
        $responseText = ($responseOutput -join "`n")

        if ($LASTEXITCODE -ne 0) {
            throw "Modrinth upload of $($release.VersionNumber) failed: $responseText"
        }

        $response = $responseText | ConvertFrom-Json
        if ($response.error) {
            throw "Modrinth rejected $($release.VersionNumber): $($response.description)"
        }

        Write-Host "Published Modrinth version $($response.version_number) (id $($response.id))."
    }

    if (-not $NoTag) {
        $tag = "v$version"
        if (-not (git tag --list $tag)) {
            git tag $tag
            git push origin $tag
        }
        $jars = @($releases | ForEach-Object { $_.Jar })
        gh release create $tag @jars --title "EMUtils $version" --notes-file $changelogPath
        Write-Host "Created GitHub release $tag with $($jars.Count) jar(s)."
    }
}
finally {
    Pop-Location
}
