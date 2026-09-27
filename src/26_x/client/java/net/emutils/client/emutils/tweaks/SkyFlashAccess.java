package net.emutils.client.emutils.tweaks;

/** Implemented by {@code ClientLevel} through a mixin, so UI snapshots can check Hide Thunder Flash. */
public interface SkyFlashAccess {
	/** How many more ticks the sky flashes from lightning, after vanilla's and EMUtils' flash options. */
	int emutils$visibleSkyFlashTime();
}
