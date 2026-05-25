package net.emutils.client.spotify;

final class SpotifyClientFactory {
	private static final SpotifyClient CLIENT = create();

	private SpotifyClientFactory() {
	}

	static SpotifyClient get() {
		return CLIENT;
	}

	private static SpotifyClient create() {
		if (LinuxSpotifyClient.isSupported()) {
			return new LinuxSpotifyClient();
		}
		if (MacOsSpotifyClient.isSupported()) {
			return new MacOsSpotifyClient();
		}
		if (WindowsSmtcSpotifyClient.isSupported()) {
			return new WindowsSmtcSpotifyClient();
		}
		return new NoopSpotifyClient();
	}
}
