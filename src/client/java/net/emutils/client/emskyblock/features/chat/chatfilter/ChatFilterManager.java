package net.emutils.client.emskyblock.features.chat.chatfilter;

import java.util.List;
import java.util.regex.Pattern;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emhelpers.text.FormattedText;
import net.minecraft.text.Text;

public final class ChatFilterManager {
	private ChatFilterManager() {
	}

	private static final Pattern FIRE_SALE_PATTERN = Pattern.compile(
		"\u00a76\u00a7k\u00a7lA\u00a7r \u00a7c\u00a7lFIRE SALE \u00a7r\u00a76\u00a7k\u00a7lA(?:\\n|.)*"
	);

	private static final List<Pattern> LOBBY_PATTERNS = List.of(
		Pattern.compile("(?: \u00a7b>\u00a7c>\u00a7a>\u00a7r \u00a7r)?.* \u00a76(?:joined|(?:spooked|slid) into) the lobby!(?:\u00a7r \u00a7a<\u00a7c<\u00a7b<)?"),
		Pattern.compile("\u00a72[\\s]*?\u00a7aYou can now create your own Hypixel SMP server![\\s]*?"),
		Pattern.compile("[\\s]*?.*\u00a7bFor the best experience, click the text below to enable Snow[\\s]\u00a7.*\u00a7bParticles in this lobby![\\s]*?.*\u00a73\u00a7lClick to enable Snow Particles[\\s]*?"),
		Pattern.compile("\u00a7b\u2726 \u00a7r.* \u00a7r\u00a77found a \u00a7r\u00a7e.* \u00a7r\u00a7bMystery Box\u00a7r\u00a77!"),
		Pattern.compile("\u00a7b\u2726 \u00a7r.* \u00a7r\u00a77found (a|an) \u00a7r.* \u00a7r\u00a77in a \u00a7r\u00a7a(Holiday )?Mystery Box\u00a7r\u00a77!"),
		Pattern.compile("\u00a7b\u2726 \u00a7r\u00a77You earned \u00a7r\u00a7b\\d+ \u00a7r\u00a77Mystery Dust!"),
		Pattern.compile("\u00a7b\u00a7b\u2726 \u00a7r\u00a77You earned \u00a7a\\d+ \u00a77Pet Consumables items!")
	);

	private static final List<String> LOBBY_MESSAGES = List.of(
		"  \u00a7r\u00a7f\u00a7l\u27a4 \u00a7r\u00a76You have reached your Hype limit! Add Hype to Prototype Lobby minigames by right-clicking with the Hype Diamond!"
	);

	private static final List<String> LOBBY_CONTAINS = List.of(
		"\u00a7r\u00a76\u00a7lWelcome to the Prototype Lobby\u00a7r",
		"\u00a7r\u00a7e\u00a76\u00a7lHYPIXEL\u00a7e is hosting a \u00a7b\u00a7lBED WARS DOUBLES\u00a7e tournament!",
		"\u00a7r\u00a7e\u00a76\u00a7lHYPIXEL BED WARS DOUBLES\u00a7e tournament is live!",
		"\u00a7r\u00a7e\u00a76\u00a7lHYPIXEL\u00a7e is hosting a \u00a7b\u00a7lTNT RUN\u00a7e tournament!",
		"\u00a7aYou are still radiating with \u00a7bGenerosity\u00a7r\u00a7a!"
	);

	private static final List<Pattern> WARPING_PATTERNS = List.of(
		Pattern.compile("\u00a77Sending to server (.*)\\.\\.\\."),
		Pattern.compile("\u00a77Request join for Hub (.*)\\.\\.\\."),
		Pattern.compile("\u00a77Request join for Dungeon Hub #(.*)\\.\\.\\."),
		Pattern.compile("\u00a7dWarped to (.*)\u00a7r\u00a7d!")
	);

	private static final List<String> WARPING_MESSAGES = List.of(
		"\u00a77Warping...",
		"\u00a77Warping you to your SkyBlock island...",
		"\u00a77Warping using transfer token...",
		"\u00a77Finding player...",
		"\u00a77Sending a visit request..."
	);

	private static final List<String> WELCOME_MESSAGES = List.of(
		"\u00a7eWelcome to \u00a7r\u00a7aHypixel SkyBlock\u00a7r\u00a7e!"
	);

	private static final List<Pattern> GUILD_EVENT_EXP_PATTERNS = List.of(
		Pattern.compile("\u00a7aYou earned \u00a7r\u00a7[0-9a-f][\\d,]+ (?:GEXP|Event EXP) (?:\u00a7r\u00a7a\\+ \u00a7r\u00a7[0-9a-f][\\d,]+ Event EXP )?\u00a7r\u00a7afrom playing SkyBlock!")
	);

	private static final List<Pattern> KILL_COMBO_PATTERNS = List.of(
		Pattern.compile("\u00a7.\u00a7l\\+(.*) Kill Combo(.*)"),
		Pattern.compile("\u00a7cYour Kill Combo has expired! You reached a (.*) Kill Combo!")
	);

	private static final List<String> KILL_COMBO_MESSAGES = List.of(
		"\u00a76\u00a7l+50 Kill Combo"
	);

	private static final List<String> PROFILE_JOIN_STARTS_WITH = List.of(
		"\u00a7aYou are playing on profile: \u00a7e",
		"\u00a78Profile ID: "
	);

	private static final List<String> MINI_BAZAAR_AH_MESSAGES = List.of(
		"\u00a77Putting item in escrow...",
		"\u00a77Putting coins in escrow...",
		"\u00a77Setting up the auction...",
		"\u00a77Processing purchase...",
		"\u00a77Processing bid...",
		"\u00a77Claiming BIN auction...",
		"\u00a76[Bazaar] \u00a7r\u00a77Submitting sell offer...",
		"\u00a76[Bazaar] \u00a7r\u00a77Submitting buy order...",
		"\u00a76[Bazaar] \u00a7r\u00a77Executing instant sell...",
		"\u00a76[Bazaar] \u00a7r\u00a77Executing instant buy...",
		"\u00a76[Bazaar] \u00a7r\u00a77Cancelling order...",
		"\u00a76[Bazaar] \u00a7r\u00a77Claiming order...",
		"\u00a76[Bazaar] \u00a7r\u00a77Putting goods in escrow...",
		"\u00a78Depositing coins...",
		"\u00a78Withdrawing coins..."
	);

	private static final List<Pattern> SLAYER_PATTERNS = List.of(
		Pattern.compile(" {2}\u00a7r\u00a75\u00a7lSLAYER QUEST STARTED!"),
		Pattern.compile(" {3}\u00a75\u00a7l\u00bb \u00a77Slay \u00a7c(.*) Combat XP \u00a77worth of (.*)\u00a77."),
		Pattern.compile(" {2}\u00a7r\u00a7a\u00a7lSLAYER QUEST COMPLETE!"),
		Pattern.compile(" {3}\u00a7r\u00a7e(.*)Slayer LVL 9 \u00a7r\u00a75- \u00a7r\u00a7a\u00a7lLVL MAXED OUT!"),
		Pattern.compile(" {3}\u00a7r\u00a75\u00a7l\u00bb \u00a7r\u00a77Talk to Maddox to claim your (.*) Slayer XP!")
	);

	private static final List<String> SLAYER_MESSAGES = List.of(
		"  \u00a7r\u00a76\u00a7lNICE! SLAYER BOSS SLAIN!",
		"\u00a7eYou received kill credit for assisting on a slayer miniboss!"
	);

	private static final List<String> SLAYER_STARTS_WITH = List.of(
		"\u00a7e\u2706 RING... "
	);

	private static final List<Pattern> SLAYER_DROP_PATTERNS = List.of(
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a77(.*)x \u00a7r\u00a7f\u00a7r\u00a79Revenant Viscera\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a79Revenant Viscera\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a77(.*)x \u00a7r\u00a7f\u00a7r\u00a79Foul Flesh\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a79Foul Flesh\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a75Golden Powder (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a72(.*) Pestilence Rune I\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75Revenant Catalyst\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a79Undead Catalyst\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a72\u25c6 Pestilence Rune I\u00a7r\u00a77\\) \u00a7r\u00a7b(.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a79Arachne's Keeper Fragment (.+)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a75Travel Scroll to Spider's Den Top of Nest (.+)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a7a\u25c6 Bite Rune I\u00a7r\u00a77\\) (.+)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a77(.+)x \u00a7r\u00a7f\u00a7r\u00a7aToxic Arrow Poison\u00a7r\u00a77\\) (.+)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a7aToxic Arrow Poison\u00a7r\u00a77\\) (.+)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a79Bane of Arthropods VI\u00a7r\u00a77\\) (.+)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a77(.*)x \u00a7r\u00a7f\u00a7r\u00a7aTwilight Arrow Poison\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7fMana Steal I\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75Sinful Dice\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a79Null Atom\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75Transmission Tuner\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7fMana Steal I\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75\u25c6 Endersnake Rune I\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a7d\u00a7lCRAZY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a7fPocket Espresso Machine\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75\u25c6 End Rune I\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a75\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a76Hazmat Enderman\u00a7r\u00a77\\) .*"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a7fWisp's Ice-Flavored Water I Splash Potion\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a7b\u00a7lRARE DROP! \u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a75Bundle of Magma Arrows\u00a7r\u00a77\\) (.*)"),
		Pattern.compile("\u00a79\u00a7lVERY RARE DROP! {2}\u00a7r\u00a77\\(\u00a7r\u00a7f\u00a7r\u00a77\\d+x \u00a7r\u00a7f\u00a7r\u00a79(Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate\u00a7r\u00a77\\) (.*)")
	);

	private static final List<Pattern> USELESS_DROP_PATTERNS = List.of(
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a7aEnchanted Ender Pearl (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a7fCarrot (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a7fPotato (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a79Machine Gun Bow (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a75Earth Shard (.*)"),
		Pattern.compile("\u00a76\u00a7lRARE DROP! \u00a7r\u00a75Zombie Lord Chestplate (.*)")
	);

	private static final List<String> USELESS_DROP_MESSAGES = List.of(
		"\u00a76\u00a7lRARE DROP! \u00a7r\u00a7aEnchanted Ender Pearl",
		"\u00a76\u00a7lRARE DROP! \u00a7r\u00a7aEnchanted End Stone",
		"\u00a76\u00a7lRARE DROP! \u00a7r\u00a75Crystal Fragment"
	);

	private static final List<Pattern> LEGACY_ITEMS = List.of(
		Pattern.compile("\u00a7cYou currently have one or more Legacy Items in your inventory or sacks that are no longer used throughout the game! Exchange them in the Legacy Trades menu, accessed through /legacytrades!")
	);

	private static final List<Pattern> USELESS_NOTIFICATION_PATTERNS = List.of(
		Pattern.compile("(?:\u00a7a)?\u00a7aYou tipped \\d+ players? in \\d+(?: different)? games?!")
	);

	private static final List<String> USELESS_NOTIFICATION_MESSAGES = List.of(
		"\u00a7eYour previous \u00a7r\u00a6Plasmaflux Power Orb \u00a7r\u00a7ewas removed!",
		"\u00a7aYou used your \u00a7r\u00a6Mining Speed Boost \u00a7r\u00a7aPickaxe Ability!",
		"\u00a7cYour Mining Speed Boost has expired!",
		"\u00a7a\u00a7r\u00a6Mining Speed Boost \u00a7r\u00a7ais now available!",
		"\u00a7aYou have just received \u00a7r\u00a60 coins \u00a7r\u00a7aas interest in your personal bank account!",
		"\u00a7aSince you've been away you earned \u00a7r\u00a60 coins \u00a7r\u00a7aas interest in your personal bank account!",
		"\u00a7aYou have just received \u00a7r\u00a60 coins \u00a7r\u00a7aas interest in your co-op bank account!"
	);

	private static final List<String> PARTY_MESSAGES = List.of(
		"\u00a79\u00a7m-----------------------------------------------------"
	);

	private static final List<String> AUCTION_HOUSE_MESSAGES = List.of(
		"\u00a7b-----------------------------------------------------",
		"\u00a7eVisit the Auction House to collect your item!"
	);

	private static final List<Pattern> BAZAAR_PATTERNS = List.of(
		Pattern.compile("\u00a7eBuy Order Setup! \u00a7r\u00a7a(.*)\u00a7r\u00a77x (.*) \u00a7r\u00a77for \u00a7r\u00a76(.*) coins\u00a7r\u00a77."),
		Pattern.compile("\u00a7eSell Offer Setup! \u00a7r\u00a7a(.*)\u00a7r\u00a77x (.*) \u00a7r\u00a77for \u00a7r\u00a76(.*) coins\u00a7r\u00a77."),
		Pattern.compile("\u00a7cCancelled! \u00a7r\u00a77Refunded \u00a7r\u00a76(.*) coins \u00a7r\u00a77from cancelling buy order!"),
		Pattern.compile("\u00a7cCancelled! \u00a7r\u00a77Refunded \u00a7r\u00a7a(.*)\u00a7r\u00a77x (.*) \u00a7r\u00a77from cancelling sell offer!")
	);

	private static final List<Pattern> WINTER_ISLAND_PATTERNS = List.of(
		Pattern.compile("\u00a7r\u00a7f\u2603 \u00a7r\u00a77\u00a7r(.*) \u00a7r\u00a77mounted a \u00a7r\u00a7fSnow Cannon\u00a7r\u00a77!")
	);

	private static final List<String> USELESS_WARNING_MESSAGES = List.of(
		"\u00a7cYou are sending commands too fast! Please slow down.",
		"\u00a7cYou can't use this while in combat!",
		"\u00a7cYou can not modify your equipped armor set!",
		"\u00a7cPlease wait a few seconds between refreshing!",
		"\u00a7cThis item is not salvageable!",
		"\u00a7cPlace a Dungeon weapon or armor piece above the anvil to salvage it!",
		"\u00a7cWhoa! Slow down there!",
		"\u00a7cWait a moment before confirming!",
		"\u00a7cYou cannot open the SkyBlock menu while in combat!"
	);

	private static final List<Pattern> ANNOYING_SPAM_PATTERNS = List.of(
		Pattern.compile("\u00a77Your Implosion hit (.*) for \u00a7r\u00a7c(.*) \u00a7r\u00a77damage."),
		Pattern.compile("\u00a77Your Molten Wave hit (.*) for \u00a7r\u00a7c(.*) \u00a7r\u00a77damage."),
		Pattern.compile("\u00a77Your Spirit Sceptre hit (.*) for \u00a7r\u00a7c(.*) \u00a7r\u00a77damage."),
		Pattern.compile("\u00a7cYou need a tool with a \u00a7r\u00a7aBreaking Power \u00a7r\u00a7cof \u00a7r\u00a76(\\d)\u00a7r\u00a7c to mine (.*)\u00a7r\u00a7c! Speak to \u00a7r\u00a7dFragilis \u00a7r\u00a7cby the entrance to the Crystal Hollows to learn more!")
	);

	private static final List<String> ANNOYING_SPAM_MESSAGES = List.of(
		"\u00a7cThere are blocks in the way!",
		"\u00a7aYour Blessing enchant got you double drops!",
		"\u00a7cYou can't use the wardrobe in combat!",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7fFish Bait\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7aGrand Experience Bottle\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7aBlessed Bait\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7fDark Bait\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7fLight Bait\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7aHot Bait\u00a7r\u00a7b.",
		"\u00a76\u00a7lGOOD CATCH! \u00a7r\u00a7bYou found a \u00a7r\u00a7fSpooky Bait\u00a7r\u00a7b.",
		"\u00a7e[NPC] Jacob\u00a7f: \u00a7rMy contest has started!",
		"\u00a7eObtain a \u00a7r\u00a76Booster Cookie \u00a7r\u00a7efrom the community shop in the hub!",
		"Unknown command. Type \"/help\" for help. ('uhfdsolguhkjdjfhgkjhdfdlgkjhldkjhlkjhsldkjfhldshkjf')",
		"\u00a73[SBE] \u00a7a\u00a7cUnable to download bin data. This may result in certain features not working!",
		"\u00a7e[NPC] Feast Chef Ted\u00a7f: Thanks for the donation! I've added a \u00a7eKernel \u00a7fto your purse."
	);

	private static final List<String> SKYMALL_MESSAGES = List.of(
		"\u00a7bNew day! \u00a7r\u00a7eYour \u00a7r\u00a72Sky Mall \u00a7r\u00a7ebuff changed!",
		"\u00a78\u00a7oYou can disable this messaging by toggling Sky Mall in your /hotm!"
	);

	private static final List<String> LOTTERY_MESSAGES = List.of(
		"\u00a7bNew day! \u00a7r\u00a7eYour \u00a7r\u00a72Lottery \u00a7r\u00a7ebuff changed!",
		"\u00a78\u00a7oYou can disable this messaging by toggling Lottery in your /hotf!"
	);

	private static final List<Pattern> PARKOUR_PATTERNS = List.of(
		Pattern.compile("\u00a7aStarted parkour (.*)!"),
		Pattern.compile("\u00a7aFinished parkour (.*) in (.*)!"),
		Pattern.compile("\u00a7aReached checkpoint #(.*) for parkour (.*)!"),
		Pattern.compile("\u00a74Wrong checkpoint for parkour (.*)!"),
		Pattern.compile("\u00a74You haven't reached all checkpoints for parkour (.*)!")
	);

	private static final List<String> PARKOUR_CANCEL_MESSAGES = List.of(
		"\u00a74Cancelled parkour! You cannot fly.",
		"\u00a74Cancelled parkour! You cannot use item abilities.",
		"\u00a74Cancelled parkour!"
	);

	private static final List<Pattern> TELEPORT_PAD_PATTERNS = List.of(
		Pattern.compile("\u00a7aWarped from the (.*) \u00a7r\u00a7ato the (.*)\u00a7r\u00a7a!")
	);

	private static final List<String> TELEPORT_PAD_MESSAGES = List.of(
		"\u00a74This Teleport Pad does not have a destination set!"
	);

	private static final List<Pattern> FEAST_CHEF_PATTERNS = List.of(
		Pattern.compile("\u00a7e\\[NPC] Feast Chef Ted\u00a7f: \u00a7rThanks for the donation! I've added a \u00a7eKernel \u00a7fto your purse.")
	);

	private static final List<String> FEAST_CHEF_MESSAGES = List.of(
		"\u00a7e[NPC] Feast Chef Ted\u00a7f: \u00a7rThanks for the donation! I've added a \u00a7eKernel \u00a7fto your purse."
	);

	private static final List<Pattern> REWARD_BUNDLE_PATTERNS = List.of(
		Pattern.compile("(?:\u00a7.)*You haven't claimed your (?:\u00a7.)*\\w+ Rewards (?:\u00a7.)*yet!"),
		Pattern.compile("(?:\u00a7.)*Talk to the (?:\u00a7.)*.+(?:\u00a7.)*in the (?:\u00a7.)*.+(?:\u00a7.)*!")
	);

	private static final List<Pattern> HOPPITY_APPEAR_PATTERNS = List.of(
		Pattern.compile("\u00a7d\u00a7lHOPPITY'S HUNT \u00a7r\u00a7dA .*\u00a7r\u00a7dhas appeared!")
	);

	private static final Pattern HOPPITY_BEGIN_PATTERN = Pattern.compile(
		"\u00a7dHoppity's Hunt \u00a7r\u00a7ehas begun! Help \u00a7r\u00a7aHoppity \u00a7r\u00a7efind his \u00a7r\u00a76Chocolate Rabbit Eggs \u00a7r\u00a7eacross SkyBlock each day during the \u00a7r\u00a7aSpring\u00a7r\u00a7e!"
	);

	private static final List<Pattern> SACRIFICE_PATTERNS = List.of(
		Pattern.compile("\u00a7c\u00a7lSACRIFICE! (.*) \u00a7r\u00a7eturned (.*) \u00a7r\u00a7einto (.*) Dragon Essence\u00a7r\u00a7e!"),
		Pattern.compile("\u00a7c\u00a7lBONUS LOOT! \u00a7r\u00a7eThey also received (.*) \u00a7r\u00a7efrom their sacrifice!")
	);

	private static final List<Pattern> EVENT_PATTERNS = List.of(
		Pattern.compile("(?:\u00a7f)? +\u00a7r\u00a77You are now \u00a7r\u00a7.Event Level \u00a7r\u00a7.*\u00a7r\u00a77!"),
		Pattern.compile("(?:\u00a7f)? +\u00a7r\u00a77You earned \u00a7r\u00a7.* Event Silver\u00a7r\u00a77!"),
		Pattern.compile("(?:\u00a7f)? +\u00a7r\u00a7.\u00a7k# \u00a7r\u00a7. LEVEL UP! \u00a7r\u00a7.\u00a7k#")
	);

	private static final List<Pattern> FACTORY_UPGRADE_PATTERNS = List.of(
		Pattern.compile(".* \u00a7r\u00a77has been promoted to \u00a7r\u00a77\\[.*\u00a7r\u00a77] \u00a7r\u00a7.*\u00a7r\u00a77!"),
		Pattern.compile("\u00a77Your \u00a7r\u00a7aRabbit Barn \u00a7r\u00a77capacity has been increased to \u00a7r\u00a7a.* Rabbits\u00a7r\u00a77!"),
		Pattern.compile("\u00a77You will now produce \u00a7r\u00a76.* Chocolate \u00a7r\u00a77per click!"),
		Pattern.compile("\u00a77You upgraded to \u00a7r\u00a7d.*?\u00a7r\u00a77!")
	);

	private static final List<Pattern> ACHIEVEMENT_PATTERNS = List.of(
		Pattern.compile("\u00a7e\u00a7k.\u00a7a>> {3}\u00a7aAchievement Unlocked: .* {3}<<\u00a7e\u00a7k.")
	);

	private static final List<String> FIRE_SALE_MESSAGES = List.of(
		"\u00a76\u00a7k\u00a7lA\u00a7r \u00a7c\u00a7lFIRE SALE \u00a7r\u00a76\u00a7k\u00a7lA",
		"\u00a7c\u2668 \u00a7eSelling multiple items for a limited time!"
	);

	private static final List<Pattern> FIRE_SALE_PATTERNS = List.of(
		Pattern.compile("\u00a7c\u2668 \u00a7eFire Sales for .* \u00a7eare starting soon!"),
		Pattern.compile("\u00a7c\\s*\u2668 .* (?:Skin|Rune|Dye) \u00a7e(?:for a limited time )?\\(.* \u00a7eleft\\)(?:\u00a7c|!)"),
		Pattern.compile("\u00a7c\u2668 \u00a7eVisit the Community Shop in the next \u00a7c.* \u00a7eto grab yours! \u00a7a\u00a7l\\[WARP]"),
		Pattern.compile("\u00a7c\u2668 \u00a7eA Fire Sale for .* \u00a7eis starting soon!"),
		Pattern.compile("\u00a7c\u2668 \u00a7r\u00a7eFire Sales? for .* \u00a7r\u00a7eended!"),
		Pattern.compile("\u00a7c {3}\u2668 \u00a7eAnd \\d+ more!")
	);

	public static String onChat(Text message) {
		if (!net.emutils.client.emskyblock.context.SkyblockContext.onHypixel(null) || !EMSkyblockSettings.skyblockEnabled()) {
			return null;
		}

		String formatted = FormattedText.format(message);
		if (formatted.isEmpty()) {
			if (EMSkyblockSettings.chatFilterEmpty()) {
				return "empty";
			}
			return null;
		}

		if (EMSkyblockSettings.chatFilterHypixelHub() && isPresent(formatted, "lobby")) {
			return "lobby";
		}

		if (EMSkyblockSettings.chatFilterWarping() && isPresent(formatted, "warping")) {
			return "warping";
		}

		if (EMSkyblockSettings.chatFilterWelcome() && isPresent(formatted, "welcome")) {
			return "welcome";
		}

		if (EMSkyblockSettings.chatFilterGuildEventExp() && isPresent(formatted, "guild_event_exp")) {
			return "guild_event_exp";
		}

		if (EMSkyblockSettings.chatFilterKillCombo() && isPresent(formatted, "kill_combo")) {
			return "kill_combo";
		}

		if (EMSkyblockSettings.chatFilterProfileJoin() && isPresent(formatted, "profile_join")) {
			return "profile_join";
		}

		if (EMSkyblockSettings.chatFilterParkour() && isPresent(formatted, "parkour")) {
			return "parkour";
		}

		if (EMSkyblockSettings.chatFilterTeleportPads() && isPresent(formatted, "teleport_pads")) {
			return "teleport_pads";
		}

		if (EMSkyblockSettings.chatFilterFeastChef() && isPresent(formatted, "feast_chef")) {
			return "feast_chef";
		}

		if (EMSkyblockSettings.chatFilterOthers() && isOthers(formatted)) {
			return "others";
		}

		if (EMSkyblockSettings.chatFilterFireSale() && (FIRE_SALE_PATTERN.matcher(formatted).matches() || isPresent(formatted, "fire_sale"))) {
			return "fire_sale";
		}

		if (EMSkyblockSettings.chatFilterRewardBundles() && isPresent(formatted, "reward_bundles")) {
			return "reward_bundles";
		}

		if (EMSkyblockSettings.chatFilterFactoryUpgrade() && isPresent(formatted, "factory_upgrade")) {
			return "factory_upgrade";
		}

		if (EMSkyblockSettings.chatFilterHoppityEggs() && isPresent(formatted, "hoppity_appear")) {
			return "hoppity_appear";
		}

		if (EMSkyblockSettings.chatFilterHoppityBegun() && isPresent(formatted, "hoppity_begin")) {
			return "hoppity_begin";
		}

		if (EMSkyblockSettings.chatFilterSacrifice() && isPresent(formatted, "sacrifice")) {
			return "sacrifice";
		}

		if (EMSkyblockSettings.chatFilterEventLevelUp() && isPresent(formatted, "event")) {
			return "event";
		}

		if (EMSkyblockSettings.chatFilterLegacyItems() && isPresent(formatted, "legacy_items")) {
			return "legacy_items";
		}

		if (EMSkyblockSettings.chatFilterAlphaAchievements() && isPresent(formatted, "achievement_get")) {
			return "achievement_get";
		}

		return null;
	}

	private static boolean isOthers(String message) {
		return isPresent(message, "bz_ah_minis") ||
			isPresent(message, "slayer") ||
			isPresent(message, "slayer_drop") ||
			isPresent(message, "useless_drop") ||
			isPresent(message, "useless_notification") ||
			isPresent(message, "party") ||
			isPresent(message, "money") ||
			isPresent(message, "winter_island") ||
			isPresent(message, "useless_warning") ||
			isPresent(message, "annoying_spam") ||
			isPresent(message, "skymall") ||
			isPresent(message, "lottery");
	}

	private static boolean isPresent(String message, String key) {
		return switch (key) {
			case "lobby" -> matchesAny(message, LOBBY_PATTERNS, LOBBY_MESSAGES, LOBBY_CONTAINS, null);
			case "warping" -> matchesAny(message, WARPING_PATTERNS, WARPING_MESSAGES, null, null);
			case "welcome" -> matchesAny(message, null, WELCOME_MESSAGES, null, null);
			case "guild_event_exp" -> matchesAny(message, GUILD_EVENT_EXP_PATTERNS, null, null, null);
			case "kill_combo" -> matchesAny(message, KILL_COMBO_PATTERNS, KILL_COMBO_MESSAGES, null, null);
			case "profile_join" -> matchesAny(message, null, null, null, PROFILE_JOIN_STARTS_WITH);
			case "parkour" -> matchesAny(message, PARKOUR_PATTERNS, PARKOUR_CANCEL_MESSAGES, null, null);
			case "teleport_pads" -> matchesAny(message, TELEPORT_PAD_PATTERNS, TELEPORT_PAD_MESSAGES, null, null);
			case "feast_chef" -> matchesAny(message, FEAST_CHEF_PATTERNS, FEAST_CHEF_MESSAGES, null, null);
			case "bz_ah_minis" -> matchesAny(message, null, MINI_BAZAAR_AH_MESSAGES, null, null);
			case "slayer" -> matchesAny(message, SLAYER_PATTERNS, SLAYER_MESSAGES, null, SLAYER_STARTS_WITH);
			case "slayer_drop" -> matchesAny(message, SLAYER_DROP_PATTERNS, null, null, null);
			case "useless_drop" -> matchesAny(message, USELESS_DROP_PATTERNS, USELESS_DROP_MESSAGES, null, null);
			case "useless_notification" -> matchesAny(message, USELESS_NOTIFICATION_PATTERNS, USELESS_NOTIFICATION_MESSAGES, null, null);
			case "party" -> matchesAny(message, null, PARTY_MESSAGES, null, null);
			case "money" -> matchesAny(message, BAZAAR_PATTERNS, AUCTION_HOUSE_MESSAGES, null, null);
			case "winter_island" -> matchesAny(message, WINTER_ISLAND_PATTERNS, null, null, null);
			case "useless_warning" -> matchesAny(message, null, USELESS_WARNING_MESSAGES, null, null);
			case "annoying_spam" -> matchesAny(message, ANNOYING_SPAM_PATTERNS, ANNOYING_SPAM_MESSAGES, null, null);
			case "skymall" -> matchesAny(message, null, SKYMALL_MESSAGES, null, null);
			case "lottery" -> matchesAny(message, null, LOTTERY_MESSAGES, null, null);
			case "fire_sale" -> matchesAny(message, FIRE_SALE_PATTERNS, FIRE_SALE_MESSAGES, null, null);
			case "reward_bundles" -> matchesAny(message, REWARD_BUNDLE_PATTERNS, null, null, null);
			case "factory_upgrade" -> matchesAny(message, FACTORY_UPGRADE_PATTERNS, null, null, null);
			case "hoppity_appear" -> matchesAny(message, HOPPITY_APPEAR_PATTERNS, null, null, null);
			case "hoppity_begin" -> matchesAny(message, List.of(HOPPITY_BEGIN_PATTERN), null, null, null);
			case "sacrifice" -> matchesAny(message, SACRIFICE_PATTERNS, null, null, null);
			case "event" -> matchesAny(message, EVENT_PATTERNS, null, null, null);
			case "legacy_items" -> matchesAny(message, LEGACY_ITEMS, null, null, null);
			case "achievement_get" -> matchesAny(message, ACHIEVEMENT_PATTERNS, null, null, null);
			default -> false;
		};
	}

	private static boolean matchesAny(
		String message,
		List<Pattern> patterns,
		List<String> exactMessages,
		List<String> containsMessages,
		List<String> startsWithMessages
	) {
		if (patterns != null) {
			for (Pattern pattern : patterns) {
				if (pattern.matcher(message).matches()) {
					return true;
				}
			}
		}

		if (exactMessages != null) {
			for (String exact : exactMessages) {
				if (message.equals(exact)) {
					return true;
				}
			}
		}

		if (containsMessages != null) {
			for (String contains : containsMessages) {
				if (message.contains(contains)) {
					return true;
				}
			}
		}

		if (startsWithMessages != null) {
			for (String startsWith : startsWithMessages) {
				if (message.startsWith(startsWith)) {
					return true;
				}
			}
		}

		return false;
	}
}
