package net.emutils.client.emskyblock.features.slayer.common;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.context.SkyblockTextUtils;
import net.emutils.client.emskyblock.features.slayer.slayertracker.SlayerTrackerManager;
import net.minecraft.text.Text;

public final class SlayerChatHandler {
	private static final Pattern QUEST_STARTED = Pattern.compile(
		"\\s*\\u00A75\\u00A7lSLAYER QUEST STARTED!\\s*"
	);
	private static final Pattern QUEST_COMPLETE = Pattern.compile(
		"\\s*\\u00A7r\\u00A7a\\u00A7lSLAYER QUEST COMPLETE!\\s*"
	);
	private static final Pattern SLAY_TARGET = Pattern.compile(
		"\\s*\\u00A75\\u00A7l\\u00BB \\u00A77Slay \\u00A7c(.*) Combat XP \\u00A77worth of (.*)\\u00A77\\.\\s*"
	);
	private static final Pattern MADDOX_CLAIM = Pattern.compile(
		"\\s*\\u00A7r\\u00A75\\u00A7l\\u00BB \\u00A7r\\u00A77Talk to Maddox to claim your (.*) Slayer XP!\\s*"
	);
	private static final Pattern BOSS_SLAIN = Pattern.compile(
		"\\s*\\u00A7r\\u00A76\\u00A7lNICE! SLAYER BOSS SLAIN!\\s*"
	);
	private static final Pattern MINIBOSS_CREDIT = Pattern.compile(
		"\\u00A7eYou received kill credit for assisting on a slayer miniboss!"
	);

	private static final List<SlayerDropRule> DROP_RULES = List.of(
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A77(.*)x \\u00A7r\\u00A7f\\u00A7r\\u00A79Revenant Viscera\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"REVENANT_VISCERA",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A79Revenant Viscera\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"REVENANT_VISCERA",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A77(.*)x \\u00A7r\\u00A7f\\u00A7r\\u00A79Foul Flesh\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"FOUL_FLESH",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A79Foul Flesh\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"FOUL_FLESH",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A76\\u00A7lRARE DROP! \\u00A7r\\u00A75Golden Powder.*"
			),
			SlayerBossType.REVENANT,
			"GOLDEN_POWDER",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A72(.*) Pestilence Rune I\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"PESTILENCE_RUNE;1",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75Revenant Catalyst\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"REVENANT_CATALYST",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A79Undead Catalyst\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.REVENANT,
			"UNDEAD_CATALYST",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A76\\u00A7lRARE DROP! \\u00A7r\\u00A79Arachne's Keeper Fragment.*"
			),
			SlayerBossType.TARANTULA,
			"ARACHNE_KEEPER_FRAGMENT",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A76\\u00A7lRARE DROP! \\u00A7r\\u00A75Travel Scroll to Spider's Den Top of Nest.*"
			),
			SlayerBossType.TARANTULA,
			"SPIDERS_DEN_TOP_TRAVEL_SCROLL",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A7a\\u25C6 Bite Rune I\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.TARANTULA,
			"BITE_RUNE;1",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A77(.*)x \\u00A7r\\u00A7f\\u00A7r\\u00A7aToxic Arrow Poison\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.TARANTULA,
			"TOXIC_ARROW_POISON",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A7aToxic Arrow Poison\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.TARANTULA,
			"TOXIC_ARROW_POISON",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A79Bane of Arthropods VI\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.TARANTULA,
			"BANE_OF_ARTHROPODS;6",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A77(.*)x \\u00A7r\\u00A7f\\u00A7r\\u00A7aTwilight Arrow Poison\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"TWILIGHT_ARROW_POISON",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7fMana Steal I\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"MANA_STEAL;1",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75Sinful Dice\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"SINFUL_DICE",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A79Null Atom\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"NULL_ATOM",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75Transmission Tuner\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"TRANSMISSION_TUNER",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75\\u25C6 Endersnake Rune I\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"ENDERSNAKE_RUNE;1",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75\\u25C6 End Rune I\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"ENCHANT_RUNE;1",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A75\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A76Hazmat Enderman\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"HAZMAT_ENDERMAN",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7d\\u00A7lCRAZY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A7fPocket Espresso Machine\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.VOID,
			"POCKET_ESPRESSO_MACHINE",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A79\\u00A7lVERY RARE DROP! {2}\\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A7fWisp's Ice-Flavored Water I Splash Potion\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.INFERNO,
			"WISP_POTION",
			1
		),
		new SlayerDropRule(
			Pattern.compile(
				"\\u00A7b\\u00A7lRARE DROP! \\u00A7r\\u00A77\\(\\u00A7r\\u00A7f\\u00A7r\\u00A75Bundle of Magma Arrows\\u00A7r\\u00A77\\).*"
			),
			SlayerBossType.INFERNO,
			"ARROW_BUNDLE_MAGMA",
			1
		)
	);

	private SlayerChatHandler() {}

	public static void onChat(Text message) {
		if (!EMSkyblockSettings.skyblockEnabled()) {
			return;
		}
		if (!SkyblockContext.inSkyBlock()) {
			return;
		}

		String stripped = SkyblockTextUtils.formattedLegacy(message);
		if (stripped.isEmpty()) {
			return;
		}

		if (QUEST_STARTED.matcher(stripped).matches()) {
			SlayerTrackerManager.onQuestStarted();
			return;
		}

		Matcher targetMatcher = SLAY_TARGET.matcher(stripped);
		if (targetMatcher.matches()) {
			String mobClass = SkyblockTextUtils.strip(targetMatcher.group(2));
			SlayerBossType detected = SlayerBossType.fromMobClass(mobClass);
			if (detected != null) {
				SlayerTrackerManager.onQuestType(detected);
			}
			return;
		}

		if (QUEST_COMPLETE.matcher(stripped).matches() || BOSS_SLAIN.matcher(stripped).matches()) {
			SlayerTrackerManager.onBossKilled();
			return;
		}

		if (MINIBOSS_CREDIT.matcher(stripped).matches()) {
			SlayerTrackerManager.onMiniBossAssist();
			return;
		}

		if (MADDOX_CLAIM.matcher(stripped).matches()) {
			SlayerTrackerManager.onMaddoxClaim();
			return;
		}

		for (SlayerDropRule rule : DROP_RULES) {
			Matcher matcher = rule.pattern.matcher(stripped);
			if (!matcher.matches()) {
				continue;
			}

			long amount = 1L;
			if (rule.amountGroup() > 0) {
				String raw = matcher.group(rule.amountGroup());
				if (raw != null) {
					try {
						amount = Long.parseLong(raw.replace(",", ""));
					} catch (NumberFormatException ignored) {
						amount = 1L;
					}
				}
			}

			SlayerTrackerManager.onDropDetected(rule.boss(), rule.itemId(), amount);
			return;
		}
	}

	private record SlayerDropRule(
		Pattern pattern,
		SlayerBossType boss,
		String itemId,
		int amountGroup
	) {}
}
