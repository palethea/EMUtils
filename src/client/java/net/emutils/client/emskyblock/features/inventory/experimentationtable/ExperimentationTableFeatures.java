package net.emutils.client.emskyblock.features.inventory.experimentationtable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.emskyblock.config.EMSkyblockConfig;
import net.emutils.client.emskyblock.features.inventory.common.InventoryFeatureUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

public final class ExperimentationTableFeatures {
	private static final Pattern REMAINING_CLICKS = Pattern.compile("Remaining Clicks:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern XP_REWARD = Pattern.compile("([0-9][0-9,.]*(?:\\.[0-9]+)?\\s*[kmb]?)\\s+Enchanting Exp", Pattern.CASE_INSENSITIVE);
	private static final Pattern SUMMONED_PET = Pattern.compile("You summoned your (?:\\[Lvl \\d+] )?(?<pet>.+?)!", Pattern.CASE_INSENSITIVE);
	private static final Pattern AUTOPET_PET = Pattern.compile("Autopet equipped your (?:\\[Lvl \\d+] )?(?<pet>.+?)!.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern DESPAWNED_PET = Pattern.compile("You despawned your (?:\\[Lvl \\d+] )?.*!", Pattern.CASE_INSENSITIVE);

	private static ExperimentType currentType = ExperimentType.NONE;
	private static Phase currentPhase = Phase.NONE;
	private static final List<String> chronomatronSequence = new ArrayList<>();
	private static final List<Integer> ultrasequencerSequence = new ArrayList<>();
	private static final Map<Integer, ItemStack> superpairsVisibleItems = new HashMap<>();
	private static final Set<Integer> superpairsPendingRead = new HashSet<>();
	private static final Map<String, List<Integer>> superpairsKnownRewards = new HashMap<>();
	private static final Map<Integer, String> superpairsXpRewardBySlot = new HashMap<>();
	private static int addonClickProgress;
	private static boolean maxSequenceAlertSent;
	private static boolean ultraRareAlertSent;
	private static int attemptsSinceUltraRare;
	private static long xpSinceUltraRare;
	private static long tableOpenedAt;
	private static boolean guardianReminderSent;
	private static String lastChronomatronSeen = "";
	private static @Nullable String currentPetName;

	private ExperimentationTableFeatures() {
	}

	public static void onInventoryOpen(String title, ScreenHandler handler) {
		ExperimentType next = type(title);
		if (next != currentType) {
			resetRunState();
		}
		currentType = next;
		if (next == ExperimentType.EXPERIMENTATION_TABLE) {
			tableOpenedAt = System.currentTimeMillis();
			guardianReminderSent = false;
		}
		readState(title, handler);
	}

	public static void onInventoryClose() {
		resetRunState();
		currentType = ExperimentType.NONE;
	}

	public static void onChat(Text message) {
		String stripped = InventoryFeatureUtils.strip(message);
		updateCurrentPet(stripped);
		if (stripped.equalsIgnoreCase("You claimed the Superpairs rewards!")) {
			attemptsSinceUltraRare++;
		}
		if (stripped.toUpperCase(Locale.ROOT).contains("ULTRA-RARE")) {
			attemptsSinceUltraRare = 0;
			xpSinceUltraRare = 0L;
		}
	}

	public static void renderScreenOverlay(DrawContext context, ScreenHandler handler, String title, int screenX, int screenY) {
		readState(title, handler);
		EMSkyblockConfig.ExperimentationTable config = config();
		if (config == null) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (config.guardianReminder
			&& currentType == ExperimentType.EXPERIMENTATION_TABLE
			&& !guardianReminderSent
			&& System.currentTimeMillis() - tableOpenedAt > 200L
			&& !hasKnownGuardianPet()) {
			guardianReminderSent = true;
			InventoryFeatureUtils.chat(Text.literal("Use a Guardian Pet for more XP in the Experimentation Table. ")
				.formatted(Formatting.YELLOW)
				.append(Text.literal("[Open pets]")
					.formatted(Formatting.AQUA)
					.styled(style -> style
						.withClickEvent(new ClickEvent.RunCommand("/pet"))
						.withHoverEvent(new HoverEvent.ShowText(Text.literal("Open the pets menu"))))));
			client.inGameHud.setTitle(Text.literal("Wrong Pet equipped!").formatted(Formatting.RED));
			client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F));
		}

		List<String> lines = new ArrayList<>();
		if (config.dryStreak.enabled && currentType != ExperimentType.NONE) {
			lines.add("Experiment Dry Streak:");
			if (config.dryStreak.attemptsSince) {
				lines.add("Attempts: " + attemptsSinceUltraRare);
			}
			if (config.dryStreak.xpSince) {
				lines.add("XP: " + String.format(Locale.ROOT, "%,d", xpSinceUltraRare));
			}
		}
		if (config.superpairs.display && currentType == ExperimentType.SUPERPAIRS) {
			lines.addAll(superpairsDisplayLines());
		}
		if (!lines.isEmpty()) {
			InventoryFeatureUtils.drawPanel(context, client.textRenderer, screenX - 150, screenY + 16, lines);
		}
	}

	public static void drawSlotOverlay(DrawContext context, ScreenHandler handler, Slot slot, String title) {
		if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
			return;
		}

		EMSkyblockConfig.ExperimentationTable config = config();
		if (config == null) {
			return;
		}

		if (config.addons.enabled && config.addons.highlightNextClick && currentPhase == Phase.REPLICATE) {
			if (currentType == ExperimentType.ULTRASEQUENCER) {
				int index = ultrasequencerSequence.indexOf(slot.id);
				if (index >= addonClickProgress) {
					int color = index == addonClickProgress
						? InventoryFeatureUtils.color(config.addons.nextColor, 0xAA55FF55)
						: InventoryFeatureUtils.color(config.addons.secondColor, 0x77FFFF55);
					InventoryFeatureUtils.highlightSlot(context, slot, color);
				}
			} else if (currentType == ExperimentType.CHRONOMATRON) {
				String expected = chronomatronSequence.size() > addonClickProgress
					? chronomatronSequence.get(addonClickProgress)
					: null;
				if (expected != null && expected.equalsIgnoreCase(colorName(slot.getStack()))) {
					InventoryFeatureUtils.highlightSlot(context, slot, InventoryFeatureUtils.color(config.addons.nextColor, 0xAA55FF55));
				}
			}
		}

		if (currentType == ExperimentType.SUPERPAIRS) {
			drawSuperpairsOverlay(context, slot, config);
		}
	}

	public static boolean guardSlotClick(String title, Slot slot) {
		if (slot == null || !InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
			return false;
		}

		EMSkyblockConfig.ExperimentationTable config = config();
		if (config == null) {
			return false;
		}

		if (currentType == ExperimentType.SUPERPAIRS) {
			readSuperpairsClick(slot, config);
			return false;
		}

		if (!config.addons.enabled || currentPhase != Phase.REPLICATE) {
			return false;
		}

		if (currentType == ExperimentType.ULTRASEQUENCER && !ultrasequencerSequence.isEmpty()) {
			int expected = addonClickProgress < ultrasequencerSequence.size() ? ultrasequencerSequence.get(addonClickProgress) : -1;
			if (slot.id == expected) {
				addonClickProgress++;
				maybeSendMaxSequenceAlert(config);
				return false;
			}
			return config.addons.preventMisclicks;
		}

		if (currentType == ExperimentType.CHRONOMATRON && !chronomatronSequence.isEmpty()) {
			String expected = addonClickProgress < chronomatronSequence.size() ? chronomatronSequence.get(addonClickProgress) : null;
			if (expected != null && expected.equalsIgnoreCase(colorName(slot.getStack()))) {
				addonClickProgress++;
				maybeSendMaxSequenceAlert(config);
				return false;
			}
			return config.addons.preventMisclicks;
		}

		return false;
	}

	private static void readState(String title, ScreenHandler handler) {
		currentType = type(title);
		if (currentType == ExperimentType.NONE) {
			return;
		}

		currentPhase = phase(handler);
		if (currentType == ExperimentType.ULTRASEQUENCER && currentPhase == Phase.READ) {
			readUltrasequencer(handler);
		} else if (currentType == ExperimentType.CHRONOMATRON && currentPhase == Phase.READ) {
			readChronomatron(handler);
		} else if (currentType == ExperimentType.SUPERPAIRS) {
			readSuperpairs(handler);
		}
	}

	private static void readUltrasequencer(ScreenHandler handler) {
		List<SlotNumber> numbered = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}
			String name = InventoryFeatureUtils.itemName(slot.getStack());
			try {
				numbered.add(new SlotNumber(slot.id, Integer.parseInt(name)));
			} catch (NumberFormatException ignored) {
			}
		}
		if (numbered.isEmpty()) {
			return;
		}
		numbered.sort(Comparator.comparingInt(SlotNumber::number));
		ultrasequencerSequence.clear();
		for (SlotNumber slotNumber : numbered) {
			ultrasequencerSequence.add(slotNumber.slotId());
		}
		addonClickProgress = 0;
		maxSequenceAlertSent = false;
	}

	private static void readChronomatron(ScreenHandler handler) {
		List<String> activeColors = new ArrayList<>();
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}
			String color = colorName(slot.getStack());
			if (color != null) {
				activeColors.add(color);
			}
		}
		if (activeColors.isEmpty()) {
			lastChronomatronSeen = "";
			return;
		}
		String seen = String.join(",", activeColors);
		if (seen.equals(lastChronomatronSeen)) {
			return;
		}
		lastChronomatronSeen = seen;
		String next = activeColors.getFirst();
		if (chronomatronSequence.isEmpty() || !chronomatronSequence.getLast().equalsIgnoreCase(next)) {
			chronomatronSequence.add(next);
			addonClickProgress = 0;
			maxSequenceAlertSent = false;
		}
	}

	private static void readSuperpairs(ScreenHandler handler) {
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}
			boolean unknown = isUnknownSuperpairs(slot.getStack());
			if (superpairsPendingRead.contains(slot.id) && !unknown) {
				superpairsVisibleItems.put(slot.id, slot.getStack().copy());
				superpairsPendingRead.remove(slot.id);
				trackKnownReward(slot.id, slot.getStack());
			}
			if (!unknown) {
				trackKnownReward(slot.id, slot.getStack());
				readXpReward(slot.id, slot.getStack());
				maybeUltraRareAlert(slot.getStack());
			}
		}
	}

	private static void readSuperpairsClick(Slot slot, EMSkyblockConfig.ExperimentationTable config) {
		if (isUnknownSuperpairs(slot.getStack())) {
			superpairsPendingRead.add(slot.id);
			return;
		}
		if (config.superpairs.clickedItemsVisible.enabled) {
			superpairsVisibleItems.put(slot.id, slot.getStack().copy());
		}
		trackKnownReward(slot.id, slot.getStack());
	}

	private static void drawSuperpairsOverlay(DrawContext context, Slot slot, EMSkyblockConfig.ExperimentationTable config) {
		if (config.superpairs.clickedItemsVisible.enabled && isUnknownSuperpairs(slot.getStack())) {
			ItemStack remembered = superpairsVisibleItems.get(slot.id);
			if (remembered != null && !remembered.isEmpty()) {
				context.drawItem(remembered, slot.x, slot.y, slot.id);
				InventoryFeatureUtils.outlineSlot(context, slot, 0xAA55FFFF);
			}
		}

		if (config.superpairs.xpOverlay) {
			Matcher matcher = XP_REWARD.matcher(InventoryFeatureUtils.itemName(slot.getStack()));
			if (matcher.find()) {
				String text = matcher.group(1);
				context.drawText(MinecraftClient.getInstance().textRenderer, text, slot.x + 2, slot.y + 9, 0xFF55FFFF, true);
			}
		}
	}

	private static void trackKnownReward(int slotId, ItemStack stack) {
		String name = InventoryFeatureUtils.itemName(stack);
		if (name.isBlank() || isUnknownSuperpairs(stack)) {
			return;
		}
		superpairsKnownRewards.computeIfAbsent(name, ignored -> new ArrayList<>());
		List<Integer> slots = superpairsKnownRewards.get(name);
		if (!slots.contains(slotId)) {
			slots.add(slotId);
		}
	}

	private static void readXpReward(int slotId, ItemStack stack) {
		Matcher matcher = XP_REWARD.matcher(InventoryFeatureUtils.itemName(stack));
		if (matcher.find()) {
			String amount = matcher.group(1).replace(" ", "");
			if (amount.equals(superpairsXpRewardBySlot.get(slotId))) {
				return;
			}

			superpairsXpRewardBySlot.put(slotId, amount);
			xpSinceUltraRare += Math.round(parseCompactNumber(amount));
		}
	}

	private static void maybeUltraRareAlert(ItemStack stack) {
		EMSkyblockConfig.ExperimentationTable config = config();
		if (config == null || !config.superpairs.ultraRareBookAlert || ultraRareAlertSent) {
			return;
		}

		if (!InventoryFeatureUtils.itemName(stack).equalsIgnoreCase("Enchanted Book")) {
			return;
		}

		boolean ultraRare = false;
		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			if (line.toUpperCase(Locale.ROOT).contains("ULTRA-RARE")) {
				ultraRare = true;
				break;
			}
		}
		if (!ultraRare) {
			return;
		}

		ultraRareAlertSent = true;
		attemptsSinceUltraRare = 0;
		xpSinceUltraRare = 0L;
		InventoryFeatureUtils.chat("You uncovered an ULTRA-RARE book: " + InventoryFeatureUtils.itemName(stack));
		MinecraftClient client = MinecraftClient.getInstance();
		client.inGameHud.setTitle(Text.literal("ULTRA-RARE BOOK!").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
		client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F));
	}

	private static List<String> superpairsDisplayLines() {
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, List<Integer>> entry : superpairsKnownRewards.entrySet()) {
			if (entry.getValue().size() >= 2) {
				lines.add(entry.getKey() + ": " + entry.getValue());
			}
		}
		if (!lines.isEmpty()) {
			lines.add(0, "Superpairs Data:");
		}
		return lines;
	}

	private static void maybeSendMaxSequenceAlert(EMSkyblockConfig.ExperimentationTable config) {
		if (!config.addons.maxSequenceAlert || maxSequenceAlertSent) {
			return;
		}

		int target = currentType == ExperimentType.ULTRASEQUENCER ? ultrasequencerSequence.size() : chronomatronSequence.size();
		if (target > 0 && addonClickProgress >= target) {
			maxSequenceAlertSent = true;
			InventoryFeatureUtils.chat("You reached the end of the current Experimentation add-on sequence.");
			MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F));
		}
	}

	private static Phase phase(ScreenHandler handler) {
		Slot slot = slot(handler, 49);
		if (slot == null || !slot.hasStack()) {
			return Phase.NONE;
		}
		String name = InventoryFeatureUtils.itemName(slot.getStack());
		if (name.contains("Remember the pattern")) {
			return Phase.READ;
		}
		if (name.startsWith("Timer:")) {
			return Phase.REPLICATE;
		}
		return Phase.NONE;
	}

	private static boolean isUnknownSuperpairs(ItemStack stack) {
		String name = InventoryFeatureUtils.itemName(stack);
		return name.equals("?")
			|| name.equalsIgnoreCase("Click any button!")
			|| name.equalsIgnoreCase("Click a second button!")
			|| name.equalsIgnoreCase("Next button is instantly rewarded!");
	}

	@Nullable
	private static String colorName(ItemStack stack) {
		String name = InventoryFeatureUtils.itemName(stack);
		return switch (name) {
			case "Green", "Lime", "Pink", "Cyan", "Orange", "Purple", "Red", "Blue", "Light Blue", "Yellow", "Magenta" -> name;
			default -> null;
		};
	}

	private static ExperimentType type(String title) {
		String clean = InventoryFeatureUtils.strip(title);
		if (clean.startsWith("Superpairs")) {
			return ExperimentType.SUPERPAIRS;
		}
		if (clean.startsWith("Chronomatron")) {
			return ExperimentType.CHRONOMATRON;
		}
		if (clean.startsWith("Ultrasequencer")) {
			return ExperimentType.ULTRASEQUENCER;
		}
		if (clean.equals("Experimentation Table")) {
			return ExperimentType.EXPERIMENTATION_TABLE;
		}
		return ExperimentType.NONE;
	}

	private static void resetRunState() {
		chronomatronSequence.clear();
		ultrasequencerSequence.clear();
		addonClickProgress = 0;
		superpairsVisibleItems.clear();
		superpairsPendingRead.clear();
		superpairsKnownRewards.clear();
		superpairsXpRewardBySlot.clear();
		maxSequenceAlertSent = false;
		ultraRareAlertSent = false;
		currentPhase = Phase.NONE;
		lastChronomatronSeen = "";
	}

	private static void updateCurrentPet(String message) {
		Matcher summoned = SUMMONED_PET.matcher(message);
		if (summoned.matches()) {
			currentPetName = normalizePetName(summoned.group("pet"));
			return;
		}

		Matcher autopet = AUTOPET_PET.matcher(message);
		if (autopet.matches()) {
			currentPetName = normalizePetName(autopet.group("pet"));
			return;
		}

		if (DESPAWNED_PET.matcher(message).matches()) {
			currentPetName = null;
		}
	}

	private static boolean hasKnownGuardianPet() {
		return currentPetName != null && currentPetName.equalsIgnoreCase("Guardian");
	}

	private static String normalizePetName(String name) {
		return name.replaceAll("\\s+\\u2726$", "").trim();
	}

	private static double parseCompactNumber(String value) {
		String normalized = value.replace(",", "").trim().toLowerCase(Locale.ROOT);
		double multiplier = 1.0D;
		if (normalized.endsWith("k")) {
			multiplier = 1_000.0D;
			normalized = normalized.substring(0, normalized.length() - 1);
		} else if (normalized.endsWith("m")) {
			multiplier = 1_000_000.0D;
			normalized = normalized.substring(0, normalized.length() - 1);
		} else if (normalized.endsWith("b")) {
			multiplier = 1_000_000_000.0D;
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return InventoryFeatureUtils.parseDouble(normalized) * multiplier;
	}

	@Nullable
	private static Slot slot(ScreenHandler handler, int slotId) {
		for (Slot slot : handler.slots) {
			if (slot.id == slotId) {
				return slot;
			}
		}
		return null;
	}

	private static EMSkyblockConfig.ExperimentationTable config() {
		EMSkyblockConfig config = InventoryFeatureUtils.config();
		return config == null ? null : config.inventory.experimentationTable;
	}

	private enum ExperimentType {
		NONE,
		EXPERIMENTATION_TABLE,
		SUPERPAIRS,
		CHRONOMATRON,
		ULTRASEQUENCER
	}

	private enum Phase {
		NONE,
		READ,
		REPLICATE
	}

	private record SlotNumber(int slotId, int number) {
	}
}
