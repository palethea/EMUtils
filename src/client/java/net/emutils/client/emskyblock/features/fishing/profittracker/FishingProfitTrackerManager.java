package net.emutils.client.emskyblock.features.fishing.profittracker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.config.EMSkyblockSettings;
import net.emutils.client.emskyblock.context.SkyblockFeatures;
import net.emutils.client.emskyblock.features.fishing.common.FishingActivity;
import net.emutils.client.emskyblock.features.fishing.trackercommon.FishingTrackerStorage;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerDisplayMode;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerPanelLine;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.EivCoinFormat;
import net.emutils.client.emskyblock.features.inventory.estimateditemvalue.SkyblockItemRarity;
import net.emutils.client.emskyblock.pricing.SkyblockPrices;
import net.emutils.client.emskyblock.sacks.SkyblockSackChange;
import net.emutils.client.emskyblock.sacks.SkyblockSackChangeBatch;
import net.emutils.client.emskyblock.sacks.SkyblockSackTracker;
import net.minecraft.client.MinecraftClient;
import org.jspecify.annotations.Nullable;

public final class FishingProfitTrackerManager {

    private static final Pattern COINS_CATCH = Pattern.compile(
        ".+ CATCH! .+You caught .+(?<coins>[\\d,]+) Coins.+",
        Pattern.CASE_INSENSITIVE
    );
    private static final long CATCH_DEDUPE_MS = 1_500L;
    private static final long SACK_ITEM_SUPPRESS_MS = 2_000L;

    private static TrackerDisplayMode displayMode = TrackerDisplayMode.SESSION;
    private static long lastCatchRecordMs;
    private static final Map<String, Long> sackRecordedItems = new HashMap<>();
    private static final Map<String, Long> inventoryRecordedItems =
        new HashMap<>();

    private FishingProfitTrackerManager() {}

    public static TrackerDisplayMode displayMode() {
        return displayMode;
    }

    public static void cycleDisplayMode() {
        displayMode = displayMode.next();
    }

    public static void resetCurrentMode() {
        if (displayMode == TrackerDisplayMode.SESSION) {
            FishingTrackerStorage.resetSessionProfit();
        } else {
            FishingTrackerStorage.resetAllTimeProfit();
        }
    }

    public static void onPullReady() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (
            !EMSkyblockSettings.skyblockEnabled() ||
            !EMSkyblockSettings.fishingProfitTrackerEnabled() ||
            !SkyblockFeatures.inSkyBlock(client)
        ) {
            return;
        }

        recordCatch();
    }

    public static void onCoinsChat(String message) {
        if (!FishingActivity.isFishing(MinecraftClient.getInstance())) {
            return;
        }

        Matcher matcher = COINS_CATCH.matcher(message);
        if (!matcher.matches()) {
            return;
        }

        long coins = parseInt(matcher.group("coins"));
        if (coins <= 0L) {
            return;
        }

        recordItem(FishingProfitItemRegistry.SKYBLOCK_COIN, coins);
        recordCatch();
    }

    public static void onItemPickup(String itemId, int amount) {
        if (amount <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        String normalizedId = itemId.toLowerCase(Locale.ROOT);
        Long lastSackMs = sackRecordedItems.get(normalizedId);
        if (lastSackMs != null && now - lastSackMs < SACK_ITEM_SUPPRESS_MS) {
            return;
        }

        Map<String, Long> items = new HashMap<>();
        items.put(itemId, (long) amount);
        onItemPickups(items);
    }

    public static void onItemPickups(Map<String, Long> items) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!canTrackFishingLoot(client)) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean anyTracked = false;
        for (Map.Entry<String, Long> entry : items.entrySet()) {
            long amount = entry.getValue();
            String itemId = entry.getKey();
            if (amount <= 0L || !FishingProfitItemRegistry.isAllowed(itemId)) {
                continue;
            }

            Long lastSackMs = sackRecordedItems.get(
                itemId.toLowerCase(Locale.ROOT)
            );
            if (
                lastSackMs != null && now - lastSackMs < SACK_ITEM_SUPPRESS_MS
            ) {
                continue;
            }

            recordItem(itemId, amount);
            inventoryRecordedItems.put(itemId.toLowerCase(Locale.ROOT), now);
            anyTracked = true;
        }

        if (anyTracked) {
            recordCatch();
        }

        pruneSourceDedupe(now);
    }

    public static void onSackChange(SkyblockSackChangeBatch batch) {
        if (SkyblockSackTracker.isManualSackInteractionRecent()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (!canTrackFishingLoot(client)) {
            return;
        }

        long now = System.currentTimeMillis();
        Set<String> recordedInBatch = new HashSet<>();
        boolean anyTracked = false;
        for (SkyblockSackChange change : batch.changes()) {
            if (
                change.delta() <= 0 ||
                !FishingProfitItemRegistry.isAllowed(change.itemId())
            ) {
                continue;
            }

            String normalizedId = change.itemId().toLowerCase(Locale.ROOT);
            String batchKey = normalizedId + "\u0000" + change.delta();
            if (!recordedInBatch.add(batchKey)) {
                sackRecordedItems.put(normalizedId, now);
                continue;
            }

            Long lastInventoryMs = inventoryRecordedItems.get(normalizedId);
            if (
                lastInventoryMs != null &&
                now - lastInventoryMs < SACK_ITEM_SUPPRESS_MS
            ) {
                sackRecordedItems.put(normalizedId, now);
                continue;
            }

            recordItem(change.itemId(), change.delta());
            sackRecordedItems.put(normalizedId, now);
            anyTracked = true;
        }

        if (anyTracked) {
            recordCatch();
        }

        pruneSourceDedupe(now);
    }

    public static void recordCatch() {
        long now = System.currentTimeMillis();
        if (
            lastCatchRecordMs > 0L && now - lastCatchRecordMs < CATCH_DEDUPE_MS
        ) {
            FishingActivity.onCatch();
            return;
        }

        lastCatchRecordMs = now;
        FishingTrackerStorage.fishingProfit(
            TrackerDisplayMode.SESSION
        ).addCatch();
        FishingTrackerStorage.fishingProfit(
            TrackerDisplayMode.ALL_TIME
        ).addCatch();
        FishingTrackerStorage.saveIfDirty();
        FishingActivity.onCatch();
    }

    private static void pruneSourceDedupe(long now) {
        sackRecordedItems
            .entrySet()
            .removeIf(e -> now - e.getValue() > SACK_ITEM_SUPPRESS_MS);
        inventoryRecordedItems
            .entrySet()
            .removeIf(e -> now - e.getValue() > SACK_ITEM_SUPPRESS_MS);
    }

    private static void recordItem(String itemId, long amount) {
        FishingTrackerStorage.fishingProfit(TrackerDisplayMode.SESSION).addItem(
            itemId,
            amount
        );
        FishingTrackerStorage.fishingProfit(
            TrackerDisplayMode.ALL_TIME
        ).addItem(itemId, amount);
        FishingTrackerStorage.saveIfDirty();
    }

    private static boolean canTrackFishingLoot(MinecraftClient client) {
        return (
            EMSkyblockSettings.skyblockEnabled() &&
            EMSkyblockSettings.fishingProfitTrackerEnabled() &&
            SkyblockFeatures.inSkyBlock(client) &&
            FishingActivity.isFishing(client)
        );
    }

    public static boolean shouldShow(MinecraftClient client) {
        if (
            !EMSkyblockSettings.skyblockEnabled() ||
            !EMSkyblockSettings.fishingProfitTrackerEnabled()
        ) {
            return false;
        }

        if (!SkyblockFeatures.inSkyBlock(client)) {
            return false;
        }

        if (
            EMSkyblockSettings.fishingProfitTrackerShowWithFishingArmor() &&
            FishingActivity.isWearingFishingArmor(client)
        ) {
            return true;
        }

        if (
            EMSkyblockSettings.fishingProfitTrackerShowOnFishingIslands() &&
            FishingActivity.isOnFishingIsland()
        ) {
            return true;
        }

        if (EMSkyblockSettings.fishingProfitTrackerShowWhenPickup()) {
            return FishingActivity.isFishing(client);
        }

        return FishingActivity.isFishingStrict(client);
    }

    public static List<TrackerPanelLine> lines(
        @Nullable MinecraftClient client,
        boolean preview
    ) {
        FishingProfitTrackerData data = preview
            ? previewData()
            : FishingTrackerStorage.fishingProfit(displayMode);
        List<TrackerPanelLine> lines = new ArrayList<>();
        lines.add(
            TrackerPanelLine.header(
                TrackerPanelLine.TrackerHeaderParts.fishingProfit(displayMode)
            )
        );

        SkyblockPrices prices = EMUtilsClient.skyblockPrices();
        List<ItemLine> itemLines = buildItemLines(data, prices, preview);
        double totalProfit = 0.0D;
        for (ItemLine itemLine : itemLines) {
            totalProfit += itemLine.profit();
            lines.add(TrackerPanelLine.of(itemLine.text()));
        }

        lines.add(
            TrackerPanelLine.of(
                "§7Times fished: §a" + formatCount(data.totalCatchAmount)
            )
        );
        lines.add(
            TrackerPanelLine.of(
                "§7" +
                    (displayMode == TrackerDisplayMode.SESSION
                        ? "Session"
                        : "Total") +
                    " Profit: §6" +
                    EivCoinFormat.compact(totalProfit) +
                    " coins"
            )
        );

        if (
            EMSkyblockSettings.fishingProfitTrackerShowUptime() &&
            data.sessionStartMs > 0L
        ) {
            long elapsed = Math.max(
                0L,
                System.currentTimeMillis() - data.sessionStartMs
            );
            if (elapsed > 0L && totalProfit > 0.0D) {
                double perHour = (totalProfit * 3_600_000.0D) / elapsed;
                lines.add(
                    TrackerPanelLine.of(
                        "§7Profit Per Hour: §6" +
                            EivCoinFormat.compact(perHour) +
                            " coins"
                    )
                );
            }
            lines.add(
                TrackerPanelLine.of("§7Uptime §a" + formatDuration(elapsed))
            );
        }

        return lines;
    }

    private static List<ItemLine> buildItemLines(
        FishingProfitTrackerData data,
        @Nullable SkyblockPrices prices,
        boolean preview
    ) {
        List<ItemLine> lines = new ArrayList<>();
        for (Map.Entry<
            String,
            FishingProfitTrackerData.TrackedItem
        > entry : data.items.entrySet()) {
            long amount = entry.getValue().amount;
            if (amount <= 0L) {
                continue;
            }

            double unitPrice = unitPrice(prices, entry.getKey(), preview);
            double profit = unitPrice * amount;
            String name = displayName(entry.getKey());
            String priceText =
                profit > 0.0D
                    ? " §7(§6" + EivCoinFormat.compact(profit) + "§7)"
                    : "";
            lines.add(
                new ItemLine(
                    " §7› §7" + formatCount(amount) + "x " + name + priceText,
                    profit
                )
            );
        }

        lines.sort(Comparator.comparingDouble(ItemLine::profit).reversed());
        if (lines.size() > EMSkyblockSettings.fishingProfitTrackerMaxLines()) {
            return lines.subList(
                0,
                EMSkyblockSettings.fishingProfitTrackerMaxLines()
            );
        }

        return lines;
    }

    private static double unitPrice(
        @Nullable SkyblockPrices prices,
        String itemId,
        boolean preview
    ) {
        if (FishingProfitItemRegistry.SKYBLOCK_COIN.equals(itemId)) {
            return 1.0D;
        }

        if (prices == null) {
            return preview ? 10_000.0D : 0.0D;
        }

        SkyblockPrices.PriceResult result = prices.price(itemId);
        return result.known() ? result.amount() : 0.0D;
    }

    private static final Map<String, String> DISPLAY_NAME_OVERRIDES =
        Map.ofEntries(
            Map.entry("RAW_FISH", "Cod"),
            Map.entry("RAW_FISH-1", "Salmon"),
            Map.entry("RAW_FISH-2", "Tropical Fish"),
            Map.entry("RAW_FISH-3", "Pufferfish"),
            Map.entry("SAND-1", "Red Sand"),
            Map.entry("GOLDEN_APPLE-1", "Enchanted Golden Apple"),
            Map.entry("GUARDIAN;0", "Guardian Pet"),
            Map.entry("GUARDIAN;1", "Guardian Pet"),
            Map.entry("GUARDIAN;2", "Guardian Pet"),
            Map.entry("GUARDIAN;3", "Guardian Pet"),
            Map.entry("GUARDIAN;4", "Guardian Pet"),
            Map.entry("SQUID;0", "Squid Pet"),
            Map.entry("SQUID;1", "Squid Pet"),
            Map.entry("SQUID;2", "Squid Pet"),
            Map.entry("SQUID;3", "Squid Pet"),
            Map.entry("SQUID;4", "Squid Pet"),
            Map.entry("FLYING_FISH;2", "Flying Fish Pet"),
            Map.entry("FLYING_FISH;3", "Flying Fish Pet"),
            Map.entry("FLYING_FISH;4", "Flying Fish Pet"),
            Map.entry("MEGALODON;3", "Megalodon Pet"),
            Map.entry("MEGALODON;4", "Megalodon Pet"),
            Map.entry("BABY_YETI;0", "Baby Yeti Pet"),
            Map.entry("BABY_YETI;3", "Baby Yeti Pet"),
            Map.entry("BABY_YETI;4", "Baby Yeti Pet"),
            Map.entry("LURE;6", "Lure VI"),
            Map.entry("ANGLER;6", "Angler VI"),
            Map.entry("LUCK_OF_THE_SEA;6", "Luck of the Sea VI"),
            Map.entry("MAGNET;6", "Magnet VI"),
            Map.entry("FRAIL;6", "Frail VI"),
            Map.entry("CASTER;6", "Caster VI"),
            Map.entry("SPIKED_HOOK;6", "Spiked Hook VI"),
            Map.entry("BLESSING;6", "Blessing VI"),
            Map.entry("FIRE_PROTECTION;6", "Fire Protection VI"),
            Map.entry("PISCARY;6", "Piscary VI"),
            Map.entry("ULTIMATE_FLASH;1", "Ultimate Flash I"),
            Map.entry("MUSIC_RUNE;1", "Music Rune I"),
            Map.entry("FAIRY_HELMET", "Fairy Helmet"),
            Map.entry("FAIRY_CHESTPLATE", "Fairy Chestplate"),
            Map.entry("FAIRY_LEGGINGS", "Fairy Leggings"),
            Map.entry("FAIRY_BOOTS", "Fairy Boots"),
            Map.entry("CLAY_BALL", "Clay Ball"),
            Map.entry("PRISMARINE_CRYSTALS", "Prismarine Crystals"),
            Map.entry("PRISMARINE_SHARD", "Prismarine Shard"),
            Map.entry("WATER_LILY", "Lily Pad"),
            Map.entry("INK_SACK", "Ink Sac"),
            Map.entry("SEA_LANTERN", "Sea Lantern"),
            Map.entry("WATER_ORB", "Water Orb"),
            Map.entry("MAGMA_FISH", "Magma Fish"),
            Map.entry("MAGMA_FISH_SILVER", "Silver Magma Fish"),
            Map.entry("MYCEL", "Mycelium"),
            Map.entry("SULPHUR_ORE", "Sulphur"),
            Map.entry("CHUM", "Chum"),
            Map.entry("SHREDDED_LINE", "Shredded Line"),
            Map.entry("THE_SHREDDER", "Shredder"),
            Map.entry("DIVER_FRAGMENT", "Diver Fragment"),
            Map.entry("WATER_HYDRA_HEAD", "Water Hydra Head"),
            Map.entry("FISH_AFFINITY_TALISMAN", "Fish Affinity Talisman"),
            Map.entry("SQUID_BOOTS", "Squid Boots"),
            Map.entry("SEA_LUMIES", "Sea Lumies"),
            Map.entry("BOBBIN_SCRIPTURES", "Bobbin' Scriptures"),
            Map.entry("DYE_AQUAMARINE", "Aquamarine Dye"),
            Map.entry("DYE_BONE", "Bone Dye"),
            Map.entry("DYE_ICEBERG", "Iceberg Dye"),
            Map.entry("DYE_TREASURE", "Treasure Dye"),
            Map.entry("DYE_CARMINE", "Carmine Dye"),
            Map.entry("CORRUPTED_FRAGMENT", "Corrupted Fragment"),
            Map.entry("GRAND_EXP_BOTTLE", "Grand Experience Bottle"),
            Map.entry("TITANIC_EXP_BOTTLE", "Titanic Experience Bottle"),
            Map.entry("LAVA_SHELL", "Lava Shell"),
            Map.entry("LAVA_WATER_ORB", "Lava Water Orb"),
            Map.entry("LUMP_OF_MAGMA", "Lump of Magma"),
            Map.entry("SLUG_BOOTS", "Slug Boots"),
            Map.entry("MOOGMA_PELT", "Moogma Pelt"),
            Map.entry("MOOGMA_LEGGINGS", "Moogma Leggings"),
            Map.entry("CUP_OF_BLOOD", "Cup of Blood"),
            Map.entry("BLADE_OF_THE_VOLCANO", "Blade of the Volcano"),
            Map.entry("PITCHIN_KOI", "Pitchin' Koi"),
            Map.entry("PYROCLASTIC_SCALE", "Pyroclastic Scale"),
            Map.entry("FLAMING_HEART", "Flaming Heart"),
            Map.entry("FLAMING_CHESTPLATE", "Flaming Chestplate"),
            Map.entry("ORB_OF_ENERGY", "Orb of Energy"),
            Map.entry("STAFF_OF_THE_VOLCANO", "Staff of the Volcano"),
            Map.entry("HORN_OF_TAURUS", "Horn of Taurus"),
            Map.entry("TAURUS_HELMET", "Taurus Helmet"),
            Map.entry("THUNDER_SHARDS", "Thunder Shards"),
            Map.entry("MAGMA_LORD_FRAGMENT", "Magma Lord Fragment"),
            Map.entry("RADIOACTIVE_VIAL", "Radioactive Vial"),
            Map.entry("CHARM;1", "Charm"),
            Map.entry("PROSPERITY;1", "Prosperity"),
            Map.entry("RESPITE;1", "Respite")
        );

    private static final Map<String, SkyblockItemRarity> ITEM_RARITIES =
        Map.ofEntries(
            Map.entry("RAW_FISH", SkyblockItemRarity.COMMON),
            Map.entry("RAW_FISH-1", SkyblockItemRarity.COMMON),
            Map.entry("RAW_FISH-2", SkyblockItemRarity.COMMON),
            Map.entry("RAW_FISH-3", SkyblockItemRarity.RARE),
            Map.entry("ENCHANTED_PUFFERFISH", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RAW_FISH", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RAW_SALMON", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_CLOWNFISH", SkyblockItemRarity.UNCOMMON),
            Map.entry("GUARDIAN;0", SkyblockItemRarity.COMMON),
            Map.entry("GUARDIAN;1", SkyblockItemRarity.UNCOMMON),
            Map.entry("GUARDIAN;2", SkyblockItemRarity.RARE),
            Map.entry("GUARDIAN;3", SkyblockItemRarity.EPIC),
            Map.entry("GUARDIAN;4", SkyblockItemRarity.LEGENDARY),
            Map.entry("SQUID;0", SkyblockItemRarity.COMMON),
            Map.entry("SQUID;1", SkyblockItemRarity.UNCOMMON),
            Map.entry("SQUID;2", SkyblockItemRarity.RARE),
            Map.entry("SQUID;3", SkyblockItemRarity.EPIC),
            Map.entry("SQUID;4", SkyblockItemRarity.LEGENDARY),
            Map.entry("FLYING_FISH;2", SkyblockItemRarity.RARE),
            Map.entry("FLYING_FISH;3", SkyblockItemRarity.EPIC),
            Map.entry("FLYING_FISH;4", SkyblockItemRarity.LEGENDARY),
            Map.entry("MEGALODON;3", SkyblockItemRarity.EPIC),
            Map.entry("MEGALODON;4", SkyblockItemRarity.LEGENDARY),
            Map.entry("BABY_YETI;0", SkyblockItemRarity.COMMON),
            Map.entry("BABY_YETI;3", SkyblockItemRarity.EPIC),
            Map.entry("BABY_YETI;4", SkyblockItemRarity.LEGENDARY),
            Map.entry("LURE;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("ANGLER;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("LUCK_OF_THE_SEA;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("MAGNET;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("FRAIL;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("CASTER;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("SPIKED_HOOK;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("BLESSING;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("FIRE_PROTECTION;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("PISCARY;6", SkyblockItemRarity.LEGENDARY),
            Map.entry("ULTIMATE_FLASH;1", SkyblockItemRarity.LEGENDARY),
            Map.entry("MUSIC_RUNE;1", SkyblockItemRarity.RARE),
            Map.entry("FAIRY_HELMET", SkyblockItemRarity.RARE),
            Map.entry("FAIRY_CHESTPLATE", SkyblockItemRarity.RARE),
            Map.entry("FAIRY_LEGGINGS", SkyblockItemRarity.RARE),
            Map.entry("FAIRY_BOOTS", SkyblockItemRarity.RARE),
            Map.entry("CLAY_BALL", SkyblockItemRarity.COMMON),
            Map.entry("PRISMARINE_CRYSTALS", SkyblockItemRarity.COMMON),
            Map.entry("PRISMARINE_SHARD", SkyblockItemRarity.COMMON),
            Map.entry("WATER_LILY", SkyblockItemRarity.COMMON),
            Map.entry("INK_SACK", SkyblockItemRarity.COMMON),
            Map.entry("WATER_ORB", SkyblockItemRarity.COMMON),
            Map.entry("SPONGE", SkyblockItemRarity.COMMON),
            Map.entry("ROTTEN_FLESH", SkyblockItemRarity.COMMON),
            Map.entry("BONE", SkyblockItemRarity.COMMON),
            Map.entry("MUTTON", SkyblockItemRarity.COMMON),
            Map.entry("WOOL", SkyblockItemRarity.COMMON),
            Map.entry("RABBIT", SkyblockItemRarity.COMMON),
            Map.entry("RABBIT_HIDE", SkyblockItemRarity.COMMON),
            Map.entry("RABBIT_FOOT", SkyblockItemRarity.COMMON),
            Map.entry("RABBIT_HAT", SkyblockItemRarity.COMMON),
            Map.entry("RED_MUSHROOM", SkyblockItemRarity.COMMON),
            Map.entry("AGARIMOO_TONGUE", SkyblockItemRarity.COMMON),
            Map.entry("ICE", SkyblockItemRarity.COMMON),
            Map.entry("SNOW_BALL", SkyblockItemRarity.COMMON),
            Map.entry("PACKED_ICE", SkyblockItemRarity.COMMON),
            Map.entry("SNOW_BLOCK", SkyblockItemRarity.COMMON),
            Map.entry("CARROT_ITEM", SkyblockItemRarity.COMMON),
            Map.entry("WALNUT", SkyblockItemRarity.COMMON),
            Map.entry("GREEN_CANDY", SkyblockItemRarity.COMMON),
            Map.entry("PURPLE_CANDY", SkyblockItemRarity.UNCOMMON),
            Map.entry("HAY_BLOCK", SkyblockItemRarity.COMMON),
            Map.entry("LUCKY_HOOF", SkyblockItemRarity.UNCOMMON),
            Map.entry("WEREWOLF_SKIN", SkyblockItemRarity.UNCOMMON),
            Map.entry("SOUL_FRAGMENT", SkyblockItemRarity.UNCOMMON),
            Map.entry("WHITE_GIFT", SkyblockItemRarity.COMMON),
            Map.entry("GREEN_GIFT", SkyblockItemRarity.UNCOMMON),
            Map.entry("RED_GIFT", SkyblockItemRarity.RARE),
            Map.entry("PARTY_GIFT", SkyblockItemRarity.COMMON),
            Map.entry("SHARK_FIN", SkyblockItemRarity.COMMON),
            Map.entry("NURSE_SHARK_TOOTH", SkyblockItemRarity.COMMON),
            Map.entry("BLUE_SHARK_TOOTH", SkyblockItemRarity.UNCOMMON),
            Map.entry("TIGER_SHARK_TOOTH", SkyblockItemRarity.RARE),
            Map.entry("GREAT_WHITE_SHARK_TOOTH", SkyblockItemRarity.EPIC),
            Map.entry("ICE_HUNK", SkyblockItemRarity.UNCOMMON),
            Map.entry("BLUE_ICE_HUNK", SkyblockItemRarity.RARE),
            Map.entry("HILT_OF_TRUE_ICE", SkyblockItemRarity.LEGENDARY),
            Map.entry("ICE_ROD", SkyblockItemRarity.RARE),
            Map.entry("ICY_SINKER", SkyblockItemRarity.UNCOMMON),
            Map.entry("YETI_ROD", SkyblockItemRarity.EPIC),
            Map.entry("DEEP_SEA_ORB", SkyblockItemRarity.EPIC),
            Map.entry("PHANTOM_ROD", SkyblockItemRarity.LEGENDARY),
            Map.entry("PHANTOM_HOOK", SkyblockItemRarity.UNCOMMON),
            Map.entry("SEA_LANTERN", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_CLAY_BALL", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_SPONGE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_WET_SPONGE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_WATER_LILY", SkyblockItemRarity.UNCOMMON),
            Map.entry(
                "ENCHANTED_PRISMARINE_SHARD",
                SkyblockItemRarity.UNCOMMON
            ),
            Map.entry(
                "ENCHANTED_PRISMARINE_CRYSTALS",
                SkyblockItemRarity.UNCOMMON
            ),
            Map.entry("ENCHANTED_ROTTEN_FLESH", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_BONE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RABBIT", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RABBIT_FOOT", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_MUTTON", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_COOKED_MUTTON", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_FEATHER", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_CARROT", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_DIAMOND", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_GOLD", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_IRON", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_ICE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_PACKED_ICE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_MAGMA_CREAM", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_BLAZE_POWDER", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_NETHER_STALK", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_COAL", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_SULPHUR", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_BLAZE_ROD", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_GLOWSTONE_DUST", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_GLOWSTONE", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_QUARTZ", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RED_SAND", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_MYCELIUM", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_SLIME_BALL", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_SLIME_BLOCK", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_RAW_CHICKEN", SkyblockItemRarity.UNCOMMON),
            Map.entry("RAW_CHICKEN", SkyblockItemRarity.COMMON),
            Map.entry("SLIME_BALL", SkyblockItemRarity.COMMON),
            Map.entry("ENCHANTED_MITHRIL", SkyblockItemRarity.UNCOMMON),
            Map.entry("MITHRIL_ORE", SkyblockItemRarity.COMMON),
            Map.entry("ENCHANTED_JUNGLE_LOG", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_CLAY_BLOCK", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_MANGROVE_LOG", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_FIG_LOG", SkyblockItemRarity.UNCOMMON),
            Map.entry("ENCHANTED_SEA_LUMIES", SkyblockItemRarity.UNCOMMON),
            Map.entry("SQUID_BOOTS", SkyblockItemRarity.RARE),
            Map.entry("WATER_HYDRA_HEAD", SkyblockItemRarity.EPIC),
            Map.entry("FISH_AFFINITY_TALISMAN", SkyblockItemRarity.RARE),
            Map.entry("THE_SHREDDER", SkyblockItemRarity.LEGENDARY),
            Map.entry("SHREDDED_LINE", SkyblockItemRarity.UNCOMMON),
            Map.entry("CHUM", SkyblockItemRarity.UNCOMMON),
            Map.entry("DIVER_FRAGMENT", SkyblockItemRarity.EPIC),
            Map.entry("MOUND_OF_SEAGRASS", SkyblockItemRarity.COMMON),
            Map.entry("VIBRANT_CORAL", SkyblockItemRarity.COMMON),
            Map.entry("GILL_MEMBRANE", SkyblockItemRarity.UNCOMMON),
            Map.entry("MANGROVE_LOG", SkyblockItemRarity.COMMON),
            Map.entry("FIG_LOG", SkyblockItemRarity.COMMON),
            Map.entry("WET_BOOK", SkyblockItemRarity.UNCOMMON),
            Map.entry("WET_WATER", SkyblockItemRarity.UNCOMMON),
            Map.entry("FLEXBONE", SkyblockItemRarity.COMMON),
            Map.entry("FISH_THE_FISH", SkyblockItemRarity.COMMON),
            Map.entry("SWAMP_THE_FISH", SkyblockItemRarity.COMMON),
            Map.entry("BOUNCY_BEACH_BALL", SkyblockItemRarity.COMMON),
            Map.entry("GIANT_BOUNCY_BEACH_BALL", SkyblockItemRarity.COMMON),
            Map.entry("MANGCORE", SkyblockItemRarity.COMMON),
            Map.entry("SEA_LUMIES", SkyblockItemRarity.COMMON),
            Map.entry("SINGED_POWDER", SkyblockItemRarity.COMMON),
            Map.entry("BLAZE_ASHES", SkyblockItemRarity.COMMON),
            Map.entry("MUTATED_BLAZE_ASHES", SkyblockItemRarity.UNCOMMON),
            Map.entry("MAGMA_CHUNK", SkyblockItemRarity.COMMON),
            Map.entry("BEZOS", SkyblockItemRarity.COMMON),
            Map.entry("DIGESTED_MUSHROOMS", SkyblockItemRarity.COMMON),
            Map.entry("KADA_LEAD", SkyblockItemRarity.COMMON),
            Map.entry("MAGMAG", SkyblockItemRarity.COMMON),
            Map.entry("GAZING_PEARL", SkyblockItemRarity.UNCOMMON),
            Map.entry("TENTACLE_MEAT", SkyblockItemRarity.COMMON),
            Map.entry("LUMINO_FIBER", SkyblockItemRarity.COMMON),
            Map.entry("LEATHER_CLOTH", SkyblockItemRarity.COMMON),
            Map.entry("SPELL_POWDER", SkyblockItemRarity.COMMON),
            Map.entry("HALLOWED_SKULL", SkyblockItemRarity.RARE),
            Map.entry("REKINDLED_EMBER_FRAGMENT", SkyblockItemRarity.EPIC),
            Map.entry("FLAMES", SkyblockItemRarity.UNCOMMON),
            Map.entry("CORRUPTED_NETHER_STAR", SkyblockItemRarity.LEGENDARY),
            Map.entry("HEAVY_PEARL", SkyblockItemRarity.RARE),
            Map.entry("KUUDRA_TIER_KEY", SkyblockItemRarity.LEGENDARY),
            Map.entry("KUUDRA_HOT_TIER_KEY", SkyblockItemRarity.LEGENDARY),
            Map.entry("KUUDRA_BURNING_TIER_KEY", SkyblockItemRarity.LEGENDARY),
            Map.entry("KUUDRA_FIERY_TIER_KEY", SkyblockItemRarity.LEGENDARY),
            Map.entry("KUUDRA_INFERNAL_TIER_KEY", SkyblockItemRarity.LEGENDARY),
            Map.entry("SCORCHED_CRAB_STICK", SkyblockItemRarity.COMMON),
            Map.entry("SCUTTLER_SHELL", SkyblockItemRarity.UNCOMMON),
            Map.entry("BRIMSTONE_HANDLE", SkyblockItemRarity.EPIC),
            Map.entry("BURNT_TEXTS", SkyblockItemRarity.UNCOMMON),
            Map.entry("CHAIN_END_TIMES", SkyblockItemRarity.UNCOMMON),
            Map.entry("THUNDER_IN_A_BOTTLE", SkyblockItemRarity.RARE),
            Map.entry("STORM_IN_A_BOTTLE", SkyblockItemRarity.EPIC),
            Map.entry("HURRICANE_IN_A_BOTTLE", SkyblockItemRarity.LEGENDARY),
            Map.entry("STURDY_BONE", SkyblockItemRarity.COMMON),
            Map.entry("SEVERED_PINCER", SkyblockItemRarity.COMMON),
            Map.entry("LAVA_SHELL", SkyblockItemRarity.RARE),
            Map.entry("LAVA_WATER_ORB", SkyblockItemRarity.UNCOMMON),
            Map.entry("LUMP_OF_MAGMA", SkyblockItemRarity.RARE),
            Map.entry("SLUG_BOOTS", SkyblockItemRarity.RARE),
            Map.entry("MOOGMA_PELT", SkyblockItemRarity.COMMON),
            Map.entry("MOOGMA_LEGGINGS", SkyblockItemRarity.RARE),
            Map.entry("CUP_OF_BLOOD", SkyblockItemRarity.EPIC),
            Map.entry("BLADE_OF_THE_VOLCANO", SkyblockItemRarity.EPIC),
            Map.entry("PITCHIN_KOI", SkyblockItemRarity.EPIC),
            Map.entry("PYROCLASTIC_SCALE", SkyblockItemRarity.EPIC),
            Map.entry("FLAMING_HEART", SkyblockItemRarity.EPIC),
            Map.entry("FLAMING_CHESTPLATE", SkyblockItemRarity.LEGENDARY),
            Map.entry("ORB_OF_ENERGY", SkyblockItemRarity.RARE),
            Map.entry("STAFF_OF_THE_VOLCANO", SkyblockItemRarity.LEGENDARY),
            Map.entry("HORN_OF_TAURUS", SkyblockItemRarity.EPIC),
            Map.entry("TAURUS_HELMET", SkyblockItemRarity.LEGENDARY),
            Map.entry("THUNDER_SHARDS", SkyblockItemRarity.LEGENDARY),
            Map.entry("MAGMA_LORD_FRAGMENT", SkyblockItemRarity.LEGENDARY),
            Map.entry("RADIOACTIVE_VIAL", SkyblockItemRarity.EPIC),
            Map.entry("CHARM;1", SkyblockItemRarity.UNCOMMON),
            Map.entry("PROSPERITY;1", SkyblockItemRarity.UNCOMMON),
            Map.entry("RESPITE;1", SkyblockItemRarity.UNCOMMON),
            Map.entry("WHIPPED_MAGMA_CREAM", SkyblockItemRarity.UNCOMMON),
            Map.entry("BURNING_EYE", SkyblockItemRarity.EPIC),
            Map.entry("WITHER_SOUL", SkyblockItemRarity.UNCOMMON),
            Map.entry("SPECTRE_DUST", SkyblockItemRarity.COMMON),
            Map.entry("MAGMA_FISH", SkyblockItemRarity.COMMON),
            Map.entry("MAGMA_FISH_SILVER", SkyblockItemRarity.UNCOMMON),
            Map.entry("MAGMA_CREAM", SkyblockItemRarity.COMMON),
            Map.entry("NETHERRACK", SkyblockItemRarity.COMMON),
            Map.entry("BLAZE_POWDER", SkyblockItemRarity.COMMON),
            Map.entry("COAL", SkyblockItemRarity.COMMON),
            Map.entry("BLAZE_ROD", SkyblockItemRarity.COMMON),
            Map.entry("QUARTZ", SkyblockItemRarity.COMMON),
            Map.entry("MAGMA_CORE", SkyblockItemRarity.RARE),
            Map.entry("BLAZEN_SPHERE", SkyblockItemRarity.EPIC),
            Map.entry("WORM_MEMBRANE", SkyblockItemRarity.RARE),
            Map.entry("ETERNAL_FLAME_RING", SkyblockItemRarity.LEGENDARY),
            Map.entry("HARD_STONE", SkyblockItemRarity.COMMON),
            Map.entry("ROUGH_AMBER_GEM", SkyblockItemRarity.COMMON),
            Map.entry("ROUGH_SAPPHIRE_GEM", SkyblockItemRarity.COMMON),
            Map.entry("ROUGH_TOPAZ_GEM", SkyblockItemRarity.COMMON),
            Map.entry("ROUGH_AMETHYST_GEM", SkyblockItemRarity.COMMON),
            Map.entry("ROUGH_JADE_GEM", SkyblockItemRarity.COMMON),
            Map.entry("FLAWED_AMETHYST_GEM", SkyblockItemRarity.UNCOMMON),
            Map.entry("FLAWED_SAPPHIRE_GEM", SkyblockItemRarity.UNCOMMON),
            Map.entry("FLAWED_JADE_GEM", SkyblockItemRarity.UNCOMMON),
            Map.entry("SAND-1", SkyblockItemRarity.COMMON),
            Map.entry("SULPHUR_ORE", SkyblockItemRarity.COMMON),
            Map.entry("GOLDEN_APPLE", SkyblockItemRarity.UNCOMMON),
            Map.entry("GOLDEN_APPLE-1", SkyblockItemRarity.UNCOMMON),
            Map.entry("GOLD_RECORD", SkyblockItemRarity.RARE),
            Map.entry("GREEN_RECORD", SkyblockItemRarity.RARE),
            Map.entry("RECORD_3", SkyblockItemRarity.RARE),
            Map.entry("RECORD_4", SkyblockItemRarity.RARE),
            Map.entry("RECORD_5", SkyblockItemRarity.RARE),
            Map.entry("RECORD_6", SkyblockItemRarity.RARE),
            Map.entry("RECORD_7", SkyblockItemRarity.RARE),
            Map.entry("RECORD_8", SkyblockItemRarity.RARE),
            Map.entry("RECORD_9", SkyblockItemRarity.RARE),
            Map.entry("RECORD_10", SkyblockItemRarity.RARE),
            Map.entry("RECORD_11", SkyblockItemRarity.RARE),
            Map.entry("RECORD_12", SkyblockItemRarity.RARE),
            Map.entry("CORRUPTED_FRAGMENT", SkyblockItemRarity.EPIC),
            Map.entry("GRAND_EXP_BOTTLE", SkyblockItemRarity.UNCOMMON),
            Map.entry("TITANIC_EXP_BOTTLE", SkyblockItemRarity.RARE),
            Map.entry("BOBBIN_SCRIPTURES", SkyblockItemRarity.LEGENDARY),
            Map.entry("DYE_AQUAMARINE", SkyblockItemRarity.LEGENDARY),
            Map.entry("DYE_BONE", SkyblockItemRarity.LEGENDARY),
            Map.entry("DYE_ICEBERG", SkyblockItemRarity.LEGENDARY),
            Map.entry("DYE_TREASURE", SkyblockItemRarity.LEGENDARY),
            Map.entry("DYE_CARMINE", SkyblockItemRarity.LEGENDARY),
            Map.entry("OLD_LEATHER_BOOT", SkyblockItemRarity.COMMON),
            Map.entry("MOBY_DUCK", SkyblockItemRarity.COMMON),
            Map.entry("BUSTED_BELT_BUCKLE", SkyblockItemRarity.COMMON),
            Map.entry("RUSTY_COIN", SkyblockItemRarity.COMMON),
            Map.entry("CAN_OF_WORMS", SkyblockItemRarity.COMMON),
            Map.entry("BRONZE_BOWL", SkyblockItemRarity.COMMON),
            Map.entry("OVERFLOWING_TRASH_CAN", SkyblockItemRarity.COMMON),
            Map.entry("HALF_EATEN_MUSHROOM", SkyblockItemRarity.COMMON),
            Map.entry("TORN_CLOTH", SkyblockItemRarity.COMMON),
            Map.entry("CALCIFIED_HEART", SkyblockItemRarity.COMMON),
            Map.entry("FRIED_FEATHER", SkyblockItemRarity.COMMON),
            Map.entry("POISON_SAMPLE", SkyblockItemRarity.COMMON),
            Map.entry("ALLIGATOR_SKIN", SkyblockItemRarity.UNCOMMON),
            Map.entry("BLUE_RING", SkyblockItemRarity.UNCOMMON),
            Map.entry("OCTOPUS_TENDRIL", SkyblockItemRarity.COMMON),
            Map.entry("TIKI_MASK", SkyblockItemRarity.COMMON),
            Map.entry("TROUBLED_BUBBLE", SkyblockItemRarity.COMMON),
            Map.entry("TITANOBOA_SHED", SkyblockItemRarity.EPIC),
            Map.entry("GOLD_INGOT", SkyblockItemRarity.COMMON),
            Map.entry("BROKEN_RADAR", SkyblockItemRarity.COMMON),
            Map.entry("EDIBLE_SEAWEED", SkyblockItemRarity.COMMON),
            Map.entry("SNOWFLAKE_THE_FISH", SkyblockItemRarity.COMMON),
            Map.entry("PET_ITEM_LUCKY_CLOVER_DROP", SkyblockItemRarity.RARE),
            Map.entry("PET_ITEM_VAMPIRE_FANG", SkyblockItemRarity.RARE),
            Map.entry(
                "PET_ITEM_FISHING_SKILL_BOOST_UNCOMMON",
                SkyblockItemRarity.UNCOMMON
            ),
            Map.entry(
                "PET_ITEM_FISHING_SKILL_BOOST_RARE",
                SkyblockItemRarity.RARE
            ),
            Map.entry(
                "PET_ITEM_FISHING_SKILL_BOOST_EPIC",
                SkyblockItemRarity.EPIC
            ),
            Map.entry(
                "PET_ITEM_FORAGING_SKILL_BOOST_EPIC",
                SkyblockItemRarity.EPIC
            )
        );

    private static String displayName(String itemId) {
        if (FishingProfitItemRegistry.SKYBLOCK_COIN.equals(itemId)) {
            return "§6Fished Coins";
        }

        SkyblockItemRarity rarity = ITEM_RARITIES.get(itemId);
        if (rarity != null) {
            return rarity.colorCode() + formatDisplayName(itemId);
        }

        if (itemId.startsWith("ATTRIBUTE_SHARD_")) {
            return "§f" + formatDisplayName(itemId);
        }

        if (itemId.startsWith("ENCHANTED_")) {
            return "§a" + formatDisplayName(itemId);
        }

        return (
            "§f" + resolveTrophyFishColor(itemId) + formatDisplayName(itemId)
        );
    }

    private static String resolveTrophyFishColor(String itemId) {
        if (itemId.endsWith("_DIAMOND")) return "§6";
        if (itemId.endsWith("_GOLD")) return "§5";
        if (itemId.endsWith("_SILVER")) return "§9";
        if (itemId.endsWith("_BRONZE")) return "§a";
        return "§f";
    }

    private static String formatDisplayName(String itemId) {
        String override = DISPLAY_NAME_OVERRIDES.get(itemId);
        if (override != null) {
            return override;
        }

        String[] parts = itemId.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.toString();
    }

    private static FishingProfitTrackerData previewData() {
        FishingProfitTrackerData data = new FishingProfitTrackerData();
        data.addItem("FAIRY_LEGGINGS", 3);
        data.totalCatchAmount = 0L;
        data.sessionStartMs = System.currentTimeMillis() - 461_000L;
        return data;
    }

    private static long parseInt(String raw) {
        try {
            return Long.parseLong(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String formatCount(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String formatDuration(long millis) {
        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%dh %dm", hours, minutes % 60L);
        }

        if (minutes > 0L) {
            return String.format(Locale.US, "%dm %ds", minutes, seconds % 60L);
        }

        return seconds + "s";
    }

    private record ItemLine(String text, double profit) {}
}
