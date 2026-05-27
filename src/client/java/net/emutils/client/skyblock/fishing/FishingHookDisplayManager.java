package net.emutils.client.skyblock.fishing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import net.emutils.client.skyblock.SkyblockFeatures;
import net.emutils.client.skyblock.SkyblockTextUtils;
import net.emutils.client.skyblock.config.EMSkyblockSettings;
import net.emutils.client.skyblock.fishing.tracker.profit.FishingProfitTrackerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.jspecify.annotations.Nullable;

public final class FishingHookDisplayManager {
	/** SkyHanni / legacy Hypixel yellow-bold timer (e.g. §e§l0.7). */
	private static final Pattern COUNTDOWN_YELLOW_PATTERN = Pattern.compile("^§e§l(\\d+(?:\\.\\d+)?)$");
	/** Current Hypixel gray timer above bobber (e.g. §73). */
	private static final Pattern COUNTDOWN_GRAY_PATTERN = Pattern.compile("^§7(\\d+(?:\\.\\d+)?)$");
	private static final Pattern STRIPPED_COUNTDOWN = Pattern.compile("^(\\d+(?:\\.\\d+)?)$");
	private static final double SEARCH_RADIUS = 3.0D;
	private static final double MAX_BOBBER_DISTANCE_SQ = 2.5D * 2.5D;

	private static @Nullable ArmorStandEntity hookStand;
	private static @Nullable Text displayText;
	private static int trackedBobberId = -1;
	private static int pullDetectionBobberId = -1;
	private static boolean pullReadyLatch;

	private FishingHookDisplayManager() {
	}

	public static void clear() {
		hookStand = null;
		displayText = null;
		trackedBobberId = -1;
		pullDetectionBobberId = -1;
		pullReadyLatch = false;
	}

	public static boolean shouldHideHookStand(Entity entity) {
		if (!EMSkyblockSettings.fishingHookHideArmorStand()) {
			return false;
		}

		if (!(entity instanceof ArmorStandEntity stand)) {
			return false;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (!shouldRun(client)) {
			return false;
		}

		PlayerEntity player = client.player;
		if (player == null || player.fishHook == null) {
			return false;
		}

		return isValidHookStand(stand, player.fishHook);
	}

	public static void tick(MinecraftClient client) {
		tickPullReadyCatch(client);
		if (!shouldRun(client)) {
			hookStand = null;
			displayText = null;
			trackedBobberId = -1;
			return;
		}

		PlayerEntity player = client.player;
		if (player == null || client.world == null) {
			clear();
			return;
		}

		FishingBobberEntity bobber = player.fishHook;
		if (bobber == null) {
			clear();
			return;
		}

		if (bobber.getId() != trackedBobberId) {
			trackedBobberId = bobber.getId();
			hookStand = null;
			displayText = null;
			FishingActivity.onBobberCast();
		}

		ArmorStandEntity stand = pickHookStand(findHookStands(client, bobber), bobber);
		if (stand == null) {
			hookStand = null;
			displayText = null;
			return;
		}

		Text text = resolveDisplayText(stand);
		if (text == null) {
			hookStand = null;
			displayText = null;
			return;
		}

		hookStand = stand;
		displayText = text;
	}

	public static boolean isActive() {
		return displayText != null;
	}

	@Nullable
	public static Text displayText() {
		return displayText;
	}

	/** Width anchor so countdown and pull alert share the same centered layout slot. */
	public static Text layoutAnchorText() {
		return FishingHookTextFormat.fromLegacyCodes(EMSkyblockSettings.fishingHookCustomAlertText());
	}

	public static int layoutAnchorWidth(TextRenderer textRenderer) {
		return textRenderer.getWidth(layoutAnchorText());
	}

	private static List<ArmorStandEntity> findHookStands(MinecraftClient client, FishingBobberEntity bobber) {
		Box searchBox = bobber.getBoundingBox().expand(SEARCH_RADIUS);
		List<ArmorStandEntity> matches = new ArrayList<>();
		for (ArmorStandEntity candidate : client.world.getEntitiesByClass(ArmorStandEntity.class, searchBox, Entity::hasCustomName)) {
			if (!isValidHookStand(candidate, bobber)) {
				continue;
			}

			matches.add(candidate);
		}

		return matches;
	}

	@Nullable
	private static ArmorStandEntity pickHookStand(List<ArmorStandEntity> matches, FishingBobberEntity bobber) {
		if (matches.isEmpty()) {
			return null;
		}

		for (ArmorStandEntity stand : matches) {
			if (isPullReady(stand)) {
				return stand;
			}
		}

		if (!EMSkyblockSettings.fishingHookShowCountdown()) {
			return null;
		}

		return matches.stream()
			.filter(FishingHookDisplayManager::isCountdown)
			.min(Comparator.comparingDouble(stand -> stand.squaredDistanceTo(bobber)))
			.orElse(null);
	}

	private static boolean isValidHookStand(ArmorStandEntity stand, FishingBobberEntity bobber) {
		if (!stand.isAlive() || stand.isRemoved()) {
			return false;
		}
		if (!stand.isCustomNameVisible()) {
			return false;
		}
		if (stand.squaredDistanceTo(bobber) > MAX_BOBBER_DISTANCE_SQ) {
			return false;
		}

		return matchesHookStand(stand);
	}

	private static boolean matchesHookStand(ArmorStandEntity stand) {
		if (isPullReady(stand)) {
			return true;
		}

		return EMSkyblockSettings.fishingHookShowCountdown() && isCountdown(stand);
	}

	private static boolean isPullReady(ArmorStandEntity stand) {
		return "!!!".equals(SkyblockTextUtils.strip(stand.getCustomName()));
	}

	private static boolean isCountdown(ArmorStandEntity stand) {
		String formatted = SkyblockTextUtils.formattedLegacyLessResets(stand.getCustomName());
		if (COUNTDOWN_YELLOW_PATTERN.matcher(formatted).matches()) {
			return true;
		}
		if (COUNTDOWN_GRAY_PATTERN.matcher(formatted).matches()) {
			return true;
		}

		String stripped = SkyblockTextUtils.strip(stand.getCustomName());
		if (!STRIPPED_COUNTDOWN.matcher(stripped).matches()) {
			return false;
		}

		// Plain number only when the legacy name is essentially just color codes + digits (no extra words).
		return formatted.length() <= stripped.length() + 6;
	}

	@Nullable
	private static Text resolveDisplayText(ArmorStandEntity stand) {
		if (isPullReady(stand)) {
			return FishingHookTextFormat.fromLegacyCodes(EMSkyblockSettings.fishingHookCustomAlertText());
		}

		if (!EMSkyblockSettings.fishingHookShowCountdown() || !isCountdown(stand)) {
			return null;
		}

		if (EMSkyblockSettings.fishingHookUseCustomCountdownColor()) {
			String value = SkyblockTextUtils.strip(stand.getCustomName());
			return FishingHookTextFormat.fromLegacyCodes(
				EMSkyblockSettings.fishingHookCountdownColorPrefix() + value
			);
		}

		return stand.getCustomName().copy();
	}

	private static void tickPullReadyCatch(MinecraftClient client) {
		if (!shouldDetectPullReady(client)) {
			pullDetectionBobberId = -1;
			pullReadyLatch = false;
			return;
		}

		PlayerEntity player = client.player;
		if (player == null || client.world == null) {
			pullDetectionBobberId = -1;
			pullReadyLatch = false;
			return;
		}

		FishingBobberEntity bobber = player.fishHook;
		if (bobber == null) {
			pullDetectionBobberId = -1;
			pullReadyLatch = false;
			return;
		}

		if (bobber.getId() != pullDetectionBobberId) {
			pullDetectionBobberId = bobber.getId();
			pullReadyLatch = false;
			FishingActivity.onBobberCast();
		}

		boolean pullReady = isBobberPullReady(client, bobber);
		if (pullReady && !pullReadyLatch) {
			pullReadyLatch = true;
			FishingProfitTrackerManager.onPullReady();
		} else if (!pullReady) {
			pullReadyLatch = false;
		}
	}

	private static boolean shouldDetectPullReady(MinecraftClient client) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.fishingProfitTrackerEnabled()) {
			return false;
		}

		if (!SkyblockFeatures.inSkyBlock(client)) {
			return false;
		}

		PlayerEntity player = client.player;
		return player != null && FishingActivity.isHoldingRod(player);
	}

	private static boolean isBobberPullReady(MinecraftClient client, FishingBobberEntity bobber) {
		for (ArmorStandEntity stand : findHookStands(client, bobber)) {
			if (isPullReady(stand)) {
				return true;
			}
		}

		return false;
	}

	private static boolean shouldRun(MinecraftClient client) {
		if (!EMSkyblockSettings.skyblockEnabled() || !EMSkyblockSettings.fishingHookDisplayEnabled()) {
			return false;
		}

		if (!SkyblockFeatures.inSkyBlock(client)) {
			return false;
		}

		PlayerEntity player = client.player;
		return player != null && isHoldingFishingRod(player);
	}

	private static boolean isHoldingFishingRod(PlayerEntity player) {
		return player.getMainHandStack().isOf(Items.FISHING_ROD) || player.getOffHandStack().isOf(Items.FISHING_ROD);
	}
}
