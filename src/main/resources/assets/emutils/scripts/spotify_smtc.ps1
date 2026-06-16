<#
EMUtils Spotify SMTC bridge for Windows.

Why this file exists:
- Windows exposes Spotify's currently playing song through the Windows Runtime
  GlobalSystemMediaTransportControls API, usually called SMTC.
- Java SE does not include a built-in WinRT projection, so the mod launches this
  tiny PowerShell bridge only on Windows to read/control that local SMTC session.

What it does:
- For "poll", it reads the active Spotify media session and prints one compact
  JSON object to stdout for the Java mod to parse.
- For "previous", "playpause", and "next", it calls the matching local SMTC
  media-control method.

What it does not do:
- It does not download or install anything.
- It does not write files, except PowerShell's own normal runtime behavior.
- It does not send data anywhere. The only output is the JSON printed to stdout.
- It does not read arbitrary user files; the only image data it touches is the
  album thumbnail stream handed to it by Windows SMTC.
#>

param(
	# Java passes one of these four actions. Validation prevents arbitrary script modes.
	[Parameter(Mandatory = $true)]
	[ValidateSet('poll', 'previous', 'playpause', 'next')]
	[string]$Action
)

# Treat unexpected WinRT/PowerShell failures as hard failures so Java can fall
# back to an unavailable Spotify state instead of parsing partial output.
$ErrorActionPreference = 'Stop'

# Load the .NET WinRT projection and the specific Windows Runtime types used by
# this bridge. These are local Windows APIs, not external modules.
Add-Type -AssemblyName System.Runtime.WindowsRuntime
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
[void][Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType = WindowsRuntime]

function Get-AwaitHelper {
	# WinRT methods return IAsyncOperation<T>. PowerShell can call them, but it
	# needs this reflection helper to convert each operation into a .NET Task<T>.
	$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
		$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
	})[0]

	return {
		param($WinRtTask, $ResultType)
		# Make AsTask<T> for the requested WinRT result type, wait locally, and
		# return the typed result to the caller.
		$asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
		$netTask = $asTask.Invoke($null, @($WinRtTask))
		$netTask.Wait(-1) | Out-Null
		$netTask.Result
	}.GetNewClosure()
}

function Get-SpotifySession {
	param($Manager)
	# Prefer an explicit Spotify session if multiple media apps are registered
	# with SMTC at the same time.
	foreach ($session in $Manager.GetSessions()) {
		if ($session.SourceAppUserModelId -match '(?i)spotify') {
			return $session
		}
	}

	# Some Windows builds only surface the active app through GetCurrentSession.
	$current = $Manager.GetCurrentSession()
	if ($null -ne $current -and $current.SourceAppUserModelId -match '(?i)spotify') {
		return $current
	}

	return $null
}

function Convert-StreamToBase64 {
	param($Await, $StreamRef)
	# Missing thumbnail data is normal on Windows for some Spotify builds.
	if ($null -eq $StreamRef) {
		return ''
	}

	try {
		# The thumbnail stream also comes from WinRT, so use the same await helper.
		$stream = & $Await $StreamRef.OpenReadAsync() ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
		if ($null -eq $stream) {
			return ''
		}

		# Keep the payload bounded. Java has an iTunes artwork fallback when
		# Windows reports no cover or a suspiciously large thumbnail.
		$size = [int]$stream.Size
		if ($size -le 0 -or $size -gt 2097152) {
			return ''
		}

		# DataReader copies the WinRT stream into a byte array that JSON can carry
		# back to Java as base64.
		$reader = [Windows.Storage.Streams.DataReader]::Create($stream)
		[void]$reader.LoadAsync($size).AsTask().GetAwaiter().GetResult()
		$bytes = New-Object byte[] $size
		[void]$reader.ReadBytes($bytes)
		$reader.Dispose()
		$stream.Dispose()
		return [Convert]::ToBase64String($bytes)
	} catch {
		# Thumbnail failures should not break track polling.
		return ''
	}
}

function Get-EffectivePositionMs {
	param($Timeline, $Playing)
	# SMTC reports the last known position plus the timestamp when it changed.
	# While playing, advance the position locally so the HUD timer is smooth.
	$positionMs = [int64]($Timeline.Position.TotalMilliseconds)
	if (-not $Playing) {
		return $positionMs
	}

	$elapsedMs = [int64](([DateTimeOffset]::Now - $Timeline.LastUpdatedTime).TotalMilliseconds)
	$effectiveMs = $positionMs + [Math]::Max(0, $elapsedMs)
	$endMs = [int64]($Timeline.EndTime.TotalMilliseconds)
	# Do not let the displayed position run beyond the track duration.
	if ($endMs -gt 0) {
		return [Math]::Min($effectiveMs, $endMs)
	}

	return $effectiveMs
}

$Await = Get-AwaitHelper

# Request the process-local SMTC manager from Windows and find Spotify's session.
$manager = & $Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$session = Get-SpotifySession -Manager $manager

# No Spotify session is not an error. For controls, there is simply nothing to do.
if ($null -eq $session) {
	if ($Action -eq 'poll') {
		Write-Output '{"kind":"unavailable"}'
	}
	exit 0
}

switch ($Action) {
	'poll' {
		# Poll is read-only: collect playback status, timeline, metadata, and
		# optional thumbnail data, then emit one JSON line.
		$playback = $session.GetPlaybackInfo()
		$timeline = $session.GetTimelineProperties()
		$properties = & $Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])

		# Empty strings are easier for Java to handle than null JSON fields here.
		$title = if ($properties) { [string]$properties.Title } else { '' }
		$artist = if ($properties) { [string]$properties.Artist } else { '' }
		$playing = ($playback.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing)
		$positionMs = Get-EffectivePositionMs -Timeline $timeline -Playing $playing
		$durationMs = [int64]($timeline.EndTime.TotalMilliseconds)
		$thumbBase64 = Convert-StreamToBase64 -Await $Await -StreamRef $(if ($properties) { $properties.Thumbnail } else { $null })

		# A session with no title usually means Spotify is open but not exposing a
		# current track yet.
		if ([string]::IsNullOrWhiteSpace($title)) {
			Write-Output '{"kind":"no_track"}'
			exit 0
		}

		# Keep property names stable; WindowsSmtcSpotifyClient parses this shape.
		$payload = [ordered]@{
			kind = 'track'
			title = $title
			artist = $artist
			playing = $playing
			positionMs = $positionMs
			durationMs = $durationMs
			artBase64 = $thumbBase64
		}
		Write-Output ($payload | ConvertTo-Json -Compress)
	}
	'previous' {
		# Local media key equivalent: ask Spotify's SMTC session to skip backward.
		[void](& $Await ($session.TrySkipPreviousAsync()) ([bool]))
	}
	'playpause' {
		# Local media key equivalent: ask Spotify's SMTC session to toggle playback.
		[void](& $Await ($session.TryTogglePlayPauseAsync()) ([bool]))
	}
	'next' {
		# Local media key equivalent: ask Spotify's SMTC session to skip forward.
		[void](& $Await ($session.TrySkipNextAsync()) ([bool]))
	}
}
