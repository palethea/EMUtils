param(
	[Parameter(Mandatory = $true)]
	[ValidateSet('poll', 'previous', 'playpause', 'next')]
	[string]$Action
)

$ErrorActionPreference = 'Stop'

function Get-AwaitHelper {
	$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
		$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
	})[0]

	return {
		param($WinRtTask, $ResultType)
		$asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
		$netTask = $asTask.Invoke($null, @($WinRtTask))
		$netTask.Wait(-1) | Out-Null
		$netTask.Result
	}.GetNewClosure()
}

function Get-SpotifySession {
	param($Manager)
	foreach ($session in $Manager.GetSessions()) {
		if ($session.SourceAppUserModelId -match '(?i)spotify') {
			return $session
		}
	}

	$current = $Manager.GetCurrentSession()
	if ($null -ne $current -and $current.SourceAppUserModelId -match '(?i)spotify') {
		return $current
	}

	return $null
}

function Convert-StreamToBase64 {
	param($Await, $StreamRef)
	if ($null -eq $StreamRef) {
		return ''
	}

	try {
		$stream = & $Await $StreamRef.OpenReadAsync() ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
		if ($null -eq $stream) {
			return ''
		}

		$size = [int]$stream.Size
		if ($size -le 0 -or $size -gt 2097152) {
			return ''
		}

		$reader = [Windows.Storage.Streams.DataReader]::Create($stream)
		[void]$reader.LoadAsync($size).AsTask().GetAwaiter().GetResult()
		$bytes = New-Object byte[] $size
		[void]$reader.ReadBytes($bytes)
		$reader.Dispose()
		$stream.Dispose()
		return [Convert]::ToBase64String($bytes)
	} catch {
		return ''
	}
}

$Await = Get-AwaitHelper
$manager = & $Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$session = Get-SpotifySession -Manager $manager

if ($null -eq $session) {
	if ($Action -eq 'poll') {
		Write-Output '{"kind":"unavailable"}'
	}
	exit 0
}

switch ($Action) {
	'poll' {
		$playback = $session.GetPlaybackInfo()
		$timeline = $session.GetTimelineProperties()
		$properties = & $Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])

		$title = if ($properties) { [string]$properties.Title } else { '' }
		$artist = if ($properties) { [string]$properties.Artist } else { '' }
		$playing = ($playback.PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing)
		$positionMs = [int64]($timeline.Position.TotalMilliseconds)
		$durationMs = [int64]($timeline.EndTime.TotalMilliseconds)
		$thumbBase64 = Convert-StreamToBase64 -Await $Await -StreamRef $(if ($properties) { $properties.Thumbnail } else { $null })

		if ([string]::IsNullOrWhiteSpace($title)) {
			Write-Output '{"kind":"no_track"}'
			exit 0
		}

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
		[void](& $Await ($session.TrySkipPreviousAsync()) ([bool]))
	}
	'playpause' {
		[void](& $Await ($session.TryTogglePlayPauseAsync()) ([bool]))
	}
	'next' {
		[void](& $Await ($session.TrySkipNextAsync()) ([bool]))
	}
}
