package net.emutils.client.emskyblock.features.inventory.bazaar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emskyblock.config.EMSkyblockConfig;
import net.emutils.client.emskyblock.context.SkyblockContext;
import net.emutils.client.emskyblock.context.SkyblockScoreboardReader;
import net.emutils.client.emskyblock.context.SkyblockTextUtils;
import net.emutils.client.emskyblock.features.inventory.common.InventoryFeatureUtils;
import net.emutils.client.emskyblock.pricing.SkyblockPrices;
import net.emutils.client.emskyblock.pricing.bazaar.BazaarProductPrice;
import net.emutils.client.emskyblock.pricing.bazaar.SkyblockItemIds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

public final class BazaarFeatures {
	public static final Identifier REORDER_CANCELLED_BUY_ORDER_ACTION = Identifier.of(EMUtilsClient.MOD_ID, "bazaar_reorder_cancelled_buy_order");

	private static final Pattern TRANSACTION = Pattern.compile(
		"\\[Bazaar] (?<type>Bought|Buy Order Setup!|Sold|Sell Offer Setup!|Order Flipped!) [\\d,]+x (?<item>.*) for (?<coins>[\\d,.]+) coins.*",
		Pattern.CASE_INSENSITIVE
	);
	private static final Pattern PRICE_PER_UNIT = Pattern.compile("Price per unit:\\s*([0-9,.]+) coins", Pattern.CASE_INSENSITIVE);
	private static final Pattern COINS_EACH = Pattern.compile("([0-9,.]+) coins each", Pattern.CASE_INSENSITIVE);
	private static final Pattern FILLED = Pattern.compile("Filled: .*/.* 100%!?$", Pattern.CASE_INSENSITIVE);
	private static final Pattern BUY_SELL_NAME = Pattern.compile("(BUY|SELL)\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern CANCELLED = Pattern.compile("\\[Bazaar].*Cancelled!.*Refunded\\s+([0-9,.]+) coins.*Buy Order!.*", Pattern.CASE_INSENSITIVE);
	private static final Pattern MISSING_ITEMS = Pattern.compile("([0-9,]+)x missing items\\.", Pattern.CASE_INSENSITIVE);
	private static final Pattern PRODUCT_TITLE = Pattern.compile("Bazaar ➜\\s*(.+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern BAZAAR_COMMAND = Pattern.compile("(?:bz|bazaar)\\s+(.+)", Pattern.CASE_INSENSITIVE);

	private static final double DAILY_LIMIT = 15_000_000_000.0D;
	private static final int[] RECIPE_MATERIAL_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};

	private static boolean inBazaarInventory;
	private static String currentSearchedItem = "";
	private static @Nullable String currentlyOpenedProduct;
	private static @Nullable String currentlyOpenedDisplayName;
	private static @Nullable String lastOpenedProduct;
	private static @Nullable String lastOpenedDisplayName;
	private static @Nullable String orderOptionProduct;
	private static @Nullable Integer latestCancelledAmount;
	private static @Nullable String lastClickedProduct;
	private static int lastClickedCount;
	private static int lastClickedCloseCount;
	private static @Nullable Double liveBuyOrderPrice;
	private static @Nullable Double liveInstantBuyPrice;
	private static double taxRate = 1.25D;
	private static boolean dailyLimitWarningSent;
	private static long lastWebsiteOpenAt;
	private static boolean inRecipeInventory;
	private static boolean purchasingCraftMaterials;
	private static List<PanelLine> craftDisplay = List.of();
	private static List<MaterialStack> recipeNeededMaterials = List.of();
	private static List<MaterialStack> purchasingMaterials = List.of();
	private static int craftMultiplier = 1;
	private static int craftPanelX;
	private static int craftPanelY;
	private static int craftPanelWidth;
	private static int craftPanelHeight;
	private static float craftPanelScale = 1.0F;

	private BazaarFeatures() {
	}

	public static void onInventoryOpen(String title, ScreenHandler handler) {
		refreshBazaarContext(title, handler);
		if (InventoryFeatureUtils.titleMatches(title, "Order options")) {
			readCancelledAmount(handler);
		}

		updateCraftMaterialState(handler);
	}

	public static void onInventoryClose() {
		inBazaarInventory = false;
		inRecipeInventory = false;
		currentlyOpenedProduct = null;
		currentlyOpenedDisplayName = null;
		liveBuyOrderPrice = null;
		liveInstantBuyPrice = null;
		if (lastClickedProduct != null && ++lastClickedCloseCount > 1) {
			lastClickedProduct = null;
			lastClickedCount = 0;
			lastClickedCloseCount = 0;
		}
	}

	public static void onChat(Text message) {
		String stripped = InventoryFeatureUtils.strip(message);
		Matcher transaction = TRANSACTION.matcher(stripped);
		if (transaction.matches()) {
			String type = transaction.group("type");
			double coins = InventoryFeatureUtils.parseDouble(transaction.group("coins"));
			double countedCoins = "Sold".equalsIgnoreCase(type) ? coins * (1.0D - taxRate / 100.0D) : coins;
			if (!"Order Flipped!".equalsIgnoreCase(type)) {
				double before = BazaarLimitStorage.coinsTowardsLimit();
				BazaarLimitStorage.addCoins(Math.min(Integer.MAX_VALUE, countedCoins));
				double after = BazaarLimitStorage.coinsTowardsLimit();
				if (after < DAILY_LIMIT) {
					dailyLimitWarningSent = false;
				} else if (!dailyLimitWarningSent && before < DAILY_LIMIT) {
					dailyLimitWarningSent = true;
					InventoryFeatureUtils.chat("You reached your daily trade limit in the Bazaar.");
				}
			}
			if (currentSearchedItem.equalsIgnoreCase(transaction.group("item"))) {
				currentSearchedItem = "";
			}
		}

		EMSkyblockConfig.Bazaar config = config();
		if (config == null || !config.cancelledBuyOrderClipboard) {
			return;
		}

		Matcher cancelled = CANCELLED.matcher(stripped);
		if (!cancelled.matches() || latestCancelledAmount == null || orderOptionProduct == null) {
			return;
		}

		String amount = Integer.toString(latestCancelledAmount);
		InventoryFeatureUtils.copyToClipboard(amount);
		String product = orderOptionProduct;
		currentSearchedItem = product;
		Text clickable = Text.literal("Bazaar buy order cancelled. Click to re-order. ")
			.formatted(Formatting.YELLOW)
			.append(Text.literal("(" + amount + "x " + product + " for " + cancelled.group(1) + " coins)")
				.formatted(Formatting.GRAY)
				.styled(style -> style
					.withClickEvent(new ClickEvent.Custom(REORDER_CANCELLED_BUY_ORDER_ACTION, Optional.of(NbtString.of(amount + "\n" + product))))
					.withHoverEvent(new HoverEvent.ShowText(Text.literal("Open Bazaar search and copy " + amount)))));
		InventoryFeatureUtils.chat(clickable);
		latestCancelledAmount = null;
	}

	public static void renderScreenOverlay(DrawContext context, ScreenHandler handler, String title, int screenX, int screenY, int mouseX, int mouseY) {
		refreshBazaarContext(title, handler);

		EMSkyblockConfig.Bazaar config = config();
		if (config == null) {
			return;
		}
		if (!purchasingCraftMaterials) {
			updateCraftMaterialState(handler);
		}

		BazaarHudRenderer.renderScreenOverlays(context, handler, config, MinecraftClient.getInstance(), mouseX, mouseY);
	}

	public static void drawSlotOverlay(DrawContext context, ScreenHandler handler, Slot slot, String title) {
		if (!InventoryFeatureUtils.topInventorySlot(slot)) {
			return;
		}

		EMSkyblockConfig.Bazaar config = config();
		if (config == null) {
			return;
		}

		if (config.openPriceWebsite && currentlyOpenedProduct != null && isProductView(title, handler) && slot.id == 22) {
			InventoryFeatureUtils.drawPriceHistoryButton(context, slot);
			return;
		}

		if (!slot.hasStack()) {
			return;
		}

		if (config.purchaseHelper && inBazaarInventory && !currentSearchedItem.isBlank()) {
			String name = InventoryFeatureUtils.itemName(slot.getStack());
			if (name.equalsIgnoreCase(currentSearchedItem) && slot.id >= 9 && slot.id <= 44) {
				InventoryFeatureUtils.highlightSlot(context, slot, 0xAA55FF55);
			}
		}

		if (config.orderHelper && isBazaarOrderInventory(title)) {
			orderHelperColor(slot.getStack()).ifPresent(color -> InventoryFeatureUtils.highlightSlot(context, slot, color));
		}

	}

	public static void onCommand(String command) {
		EMSkyblockConfig.Bazaar config = config();
		if (config == null || !config.purchaseHelper) {
			return;
		}

		Matcher matcher = BAZAAR_COMMAND.matcher(command.trim());
		if (!matcher.matches()) {
			return;
		}

		String searched = InventoryFeatureUtils.strip(matcher.group(1));
		if (!searched.isBlank()) {
			currentSearchedItem = searched;
		}
	}

	public static boolean guardSlotClick(ScreenHandler handler, String title, Slot slot) {
		if (slot == null || !InventoryFeatureUtils.topInventorySlot(slot)) {
			recordClickedItem(slot);
			return false;
		}
		recordClickedItem(slot);

		if (isBazaarOrderInventory(title)) {
			String name = InventoryFeatureUtils.itemName(slot.getStack());
			Matcher matcher = BUY_SELL_NAME.matcher(name);
			if (matcher.matches()) {
				orderOptionProduct = matcher.group(2);
			}
		}

		if (InventoryFeatureUtils.titleMatches(title, "Order options") && InventoryFeatureUtils.itemName(slot.getStack()).equalsIgnoreCase("Cancel Order")) {
			readCancelledAmount(slot.getStack());
		}

		EMSkyblockConfig.Bazaar config = config();
		if (config != null && config.openPriceWebsite && inBazaarInventory && isProductView(title, handler) && currentlyOpenedProduct != null && slot.id == 22) {
			long now = System.currentTimeMillis();
			if (now - lastWebsiteOpenAt > 300L) {
				InventoryFeatureUtils.openUrl("https://www.skyblock.bz/product/" + skyblockBzProductId(currentlyOpenedProduct));
				lastWebsiteOpenAt = now;
			}
			return true;
		}

		return false;
	}

	public static List<Text> appendTooltip(ScreenHandler handler, String title, Slot slot, List<Text> tooltip) {
		EMSkyblockConfig.Bazaar config = config();
		if (config == null || !config.openPriceWebsite || slot.id != 22 || currentlyOpenedProduct == null || !isProductView(title, handler)) {
			return tooltip;
		}
		String subject = currentlyOpenedDisplayName == null ? currentlyOpenedProduct : currentlyOpenedDisplayName;
		return InventoryFeatureUtils.priceHistoryTooltip("skyblock.bz", InventoryFeatureUtils.strip(subject));
	}

	public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
		EMSkyblockConfig.Bazaar config = config();
		if (button != 0 || config == null || !config.craftMaterialsFromBazaar || craftDisplay.isEmpty()) {
			return false;
		}
		if (!inRecipeInventory && !purchasingCraftMaterials) {
			return false;
		}
		int lineIndex = craftPanelLineAt(mouseX, mouseY);
		if (lineIndex < 0 || lineIndex >= craftDisplay.size()) {
			return false;
		}

		PanelAction action = craftDisplay.get(lineIndex).action();
		if (action == null) {
			return false;
		}
		action.run();
		return true;
	}

	public static void searchForBazaarItem(String displayName, @Nullable Integer amount) {
		String cleanName = InventoryFeatureUtils.strip(displayName);
		if (cleanName.isBlank()) {
			return;
		}

		InventoryFeatureUtils.sendCommand("bz " + cleanName);
		if (amount != null && amount > 0) {
			InventoryFeatureUtils.copyToClipboard(Integer.toString(amount));
			InventoryFeatureUtils.chat("Opened Bazaar search for " + cleanName + " and copied " + amount + " to the clipboard.");
		} else {
			InventoryFeatureUtils.chat("Opened Bazaar search for " + cleanName + ".");
		}
		currentSearchedItem = cleanName;
	}

	private static void refreshBazaarContext(String title, ScreenHandler handler) {
		inBazaarInventory = checkIfInBazaar(title, handler);
		if (!inBazaarInventory) {
			currentlyOpenedProduct = null;
			currentlyOpenedDisplayName = null;
			lastOpenedProduct = null;
			lastOpenedDisplayName = null;
			liveBuyOrderPrice = null;
			liveInstantBuyPrice = null;
			return;
		}

		boolean openedProduct = updateOpenedProduct(title, handler);
		updateTaxRate(handler);
		updateLiveBuyPrices(handler);
		if (!openedProduct && isBazaarPurchaseFlow(title) && lastOpenedProduct != null) {
			currentlyOpenedProduct = lastOpenedProduct;
			currentlyOpenedDisplayName = lastOpenedDisplayName;
		}
	}

	private static boolean checkIfInBazaar(String title, ScreenHandler handler) {
		String clean = InventoryFeatureUtils.strip(title);
		if (clean.toLowerCase(Locale.ROOT).contains("bazaar")
			|| clean.matches("Bazaar ➜ .*")
			|| clean.equals("How many do you want?")
			|| clean.equals("How much do you want to pay?")
			|| clean.equals("Confirm Buy Order")
			|| clean.equals("Confirm Instant Buy")
			|| clean.equals("At what price are you selling?")
			|| clean.equals("Confirm Sell Offer")
			|| clean.equals("Order options")
			|| isBazaarOrderInventory(clean)) {
			return true;
		}

		if (hasProductActionSlots(handler)) {
			return true;
		}

		int topSize = topInventorySize(handler);
		if (isGoBackToBazaar(slot(handler, topSize - 5)) || isGoBackToBazaar(slot(handler, topSize - 6))) {
			return true;
		}

		for (Slot slot : handler.slots) {
			if (InventoryFeatureUtils.topInventorySlot(slot) && isGoBackToBazaar(slot)) {
				return true;
			}
		}

		Slot customAmount = slot(handler, 16);
		if (customAmount != null
			&& InventoryFeatureUtils.itemName(customAmount.getStack()).equalsIgnoreCase("Custom Amount")
			&& InventoryFeatureUtils.strippedLore(customAmount.getStack()).stream().anyMatch(line -> line.equalsIgnoreCase("Buy Order Quantity"))) {
			return true;
		}
		return false;
	}

	private static boolean updateOpenedProduct(String title, ScreenHandler handler) {
		OpenedProduct product = openedProductFromSlots(handler);
		if (product == null) {
			product = openedProductFromTitle(title, handler);
		}
		if (product == null) {
			return false;
		}

		currentlyOpenedProduct = product.itemId();
		currentlyOpenedDisplayName = product.displayName();
		lastOpenedProduct = currentlyOpenedProduct;
		lastOpenedDisplayName = currentlyOpenedDisplayName;
		return true;
	}

	@Nullable
	private static OpenedProduct openedProductFromSlots(ScreenHandler handler) {
		if (!hasProductActionSlots(handler)) {
			return null;
		}

		Slot item = slot(handler, 13);
		if (item == null || !item.hasStack()) {
			return null;
		}

		String displayName = SkyblockTextUtils.formattedLegacyLessResets(item.getStack().getName());
		if (displayName.isBlank()) {
			displayName = InventoryFeatureUtils.itemName(item.getStack());
		}
		String itemId = SkyblockItemIds.resolveItemId(item.getStack());
		if (itemId == null || itemId.isBlank()) {
			itemId = SkyblockItemIds.guessFromDisplayName(displayName);
		}
		return itemId == null || itemId.isBlank() ? null : new OpenedProduct(itemId, displayName);
	}

	@Nullable
	private static OpenedProduct openedProductFromTitle(String title, ScreenHandler handler) {
		if (!hasProductActionSlots(handler)) {
			return null;
		}

		Matcher matcher = PRODUCT_TITLE.matcher(InventoryFeatureUtils.strip(title));
		if (!matcher.matches()) {
			return null;
		}

		String displayName = matcher.group(1).trim();
		String itemId = SkyblockItemIds.guessFromDisplayName(displayName);
		return itemId == null || itemId.isBlank() ? null : new OpenedProduct(itemId, displayName);
	}

	private static void updateTaxRate(ScreenHandler handler) {
		Slot sell = slot(handler, 11);
		if (sell == null || !sell.hasStack() || !InventoryFeatureUtils.itemName(sell.getStack()).equalsIgnoreCase("Sell Instantly")) {
			return;
		}

		for (String line : InventoryFeatureUtils.strippedLore(sell.getStack())) {
			if (line.startsWith("Current tax:")) {
				String number = InventoryFeatureUtils.firstNumber(line);
				if (number != null) {
					taxRate = InventoryFeatureUtils.parseDouble(number);
				}
			}
		}
	}

	private static Optional<Integer> orderHelperColor(ItemStack stack) {
		String name = InventoryFeatureUtils.itemName(stack);
		Matcher nameMatcher = BUY_SELL_NAME.matcher(name);
		if (!nameMatcher.matches()) {
			return Optional.empty();
		}

		boolean buy = nameMatcher.group(1).equalsIgnoreCase("BUY");
		String itemId = SkyblockItemIds.guessFromDisplayName(nameMatcher.group(2));
		if (itemId == null) {
			return Optional.empty();
		}

		Optional<BazaarProductPrice> product = EMUtilsClient.bazaarPrices().price(itemId);
		if (product.isEmpty()) {
			return Optional.empty();
		}

		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			if (FILLED.matcher(line).find()) {
				return Optional.of(0xAA55FF55);
			}

			Matcher price = PRICE_PER_UNIT.matcher(line);
			if (price.find()) {
				double unit = InventoryFeatureUtils.parseDouble(price.group(1));
				if (buy && unit < product.get().instantSellPrice()) {
					return Optional.of(0xAAFFAA00);
				}
				if (!buy && unit > product.get().instantBuyPrice()) {
					return Optional.of(0xAAFFAA00);
				}
			}
		}

		return Optional.empty();
	}

	private static Optional<String> bestSellLine(ScreenHandler handler, String productId, String displayName) {
		Optional<BazaarProductPrice> product = EMUtilsClient.bazaarPrices().price(productId);
		if (product.isEmpty()) {
			return Optional.empty();
		}

		int count = InventoryFeatureUtils.countInPlayerInventory(handler, productId);
		if (productId.equals(lastClickedProduct)) {
			count += lastClickedCount;
		}
		if (count <= 0) {
			return Optional.empty();
		}

		double diff = Math.max(0.0D, product.get().instantBuyPrice() - product.get().instantSellPrice()) * count;
		return Optional.of(displayName + "§7 sell difference: §6" + formatCompact(diff) + " coins");
	}

	static List<String> bestSellHudLines(ScreenHandler handler) {
		if (currentlyOpenedProduct == null || currentlyOpenedDisplayName == null) {
			return List.of();
		}
		return List.of(bestSellLine(handler, currentlyOpenedProduct, currentlyOpenedDisplayName)
			.orElse("§7Best sell method: §8No matching items in inventory"));
	}

	static List<String> maxPurseHudLines() {
		return currentlyOpenedProduct == null ? List.of() : maxPurseLines(currentlyOpenedProduct);
	}

	static List<String> dailyLimitHudLines() {
		if (!inBazaarInventory) {
			return List.of();
		}

		double coins = BazaarLimitStorage.coinsTowardsLimit();
		List<String> lines = new ArrayList<>();
		lines.add("§aBazaar Daily Limit:");
		lines.add(limitColor(coins) + formatCoins((long) coins) + "§7/§615B coins");
		if (coins >= DAILY_LIMIT) {
			lines.add("§cLimit reached!");
		}
		return lines;
	}

	private static List<String> maxPurseLines(String productId) {
		Optional<BazaarProductPrice> product = EMUtilsClient.bazaarPrices().price(productId);
		double purse = currentPurse();
		if (purse <= 0.0D) {
			return List.of("§7Max items with purse", "§cPurse not detected");
		}

		double orderPrice = liveBuyOrderPrice != null
			? liveBuyOrderPrice
			: product.map(value -> value.instantSellPrice() + 0.1D).orElse(0.0D);
		double instantPrice = liveInstantBuyPrice != null
			? liveInstantBuyPrice
			: product.map(BazaarProductPrice::instantBuyPrice).orElse(0.0D);
		if (orderPrice <= 0.0D && instantPrice <= 0.0D) {
			return List.of("§7Max items with purse", "§cPrice data unavailable");
		}

		List<String> lines = new ArrayList<>();
		lines.add("§7Max items with purse");
		if (orderPrice > 0.0D) {
			lines.add("§7Buy order +0.1: §e" + formatCoins((long) Math.floor(purse / orderPrice)) + "x");
		}
		if (instantPrice > 0.0D) {
			lines.add("§7Instant buy: §e" + formatCoins((long) Math.floor(purse / instantPrice)) + "x");
		}
		return lines;
	}

	private static double currentPurse() {
		double purse = SkyblockContext.purse();
		if (purse > 0.0D) {
			return purse;
		}

		double piggyBank = SkyblockContext.piggyBank();
		if (piggyBank > 0.0D) {
			return piggyBank;
		}

		SkyblockScoreboardReader.ParsedScoreboard scoreboard = SkyblockScoreboardReader.read(MinecraftClient.getInstance());
		if (scoreboard.purse() > 0.0D) {
			return scoreboard.purse();
		}
		return scoreboard.piggyBank();
	}

	private static boolean isRecipeView(ScreenHandler handler) {
		Slot crafting = slot(handler, 23);
		Slot supercraft = slot(handler, 32);
		return crafting != null && supercraft != null
			&& InventoryFeatureUtils.itemName(crafting.getStack()).equalsIgnoreCase("Crafting Table")
			&& InventoryFeatureUtils.itemName(supercraft.getStack()).equalsIgnoreCase("Supercraft");
	}

	private static void updateCraftMaterialState(ScreenHandler handler) {
		EMSkyblockConfig.Bazaar config = config();
		if (config == null || !config.craftMaterialsFromBazaar) {
			inRecipeInventory = false;
			if (!purchasingCraftMaterials) {
				craftDisplay = List.of();
			}
			return;
		}

		boolean recipeView = isRecipeView(handler);
		inRecipeInventory = recipeView && !purchasingCraftMaterials;
		if (inRecipeInventory) {
			showRecipe(calculateMaterialsNeeded(handler), recipeName(handler));
		} else if (!purchasingCraftMaterials) {
			craftDisplay = List.of();
			recipeNeededMaterials = List.of();
		}
	}

	private static String recipeName(ScreenHandler handler) {
		Slot slot = slot(handler, 25);
		return slot != null && slot.hasStack() ? InventoryFeatureUtils.itemName(slot.getStack()) : "Recipe";
	}

	private static List<MaterialStack> calculateMaterialsNeeded(ScreenHandler handler) {
		Map<String, MaterialStack> materials = new LinkedHashMap<>();
		for (int materialSlot : RECIPE_MATERIAL_SLOTS) {
			Slot slot = slot(handler, materialSlot);
			if (slot == null || !slot.hasStack()) {
				continue;
			}

			ItemStack stack = slot.getStack();
			String itemId = SkyblockItemIds.resolveItemId(stack);
			if (itemId == null || itemId.isBlank()) {
				continue;
			}

			String legacyName = SkyblockTextUtils.formattedLegacyLessResets(stack.getName());
			if (legacyName.isBlank()) {
				legacyName = InventoryFeatureUtils.itemName(stack);
			}
			String plainName = InventoryFeatureUtils.itemName(stack);
			SkyblockPrices.PriceResult price = EMUtilsClient.skyblockPrices().price(itemId, stack);
			MaterialStack material = new MaterialStack(itemId, legacyName, plainName, stack.getCount(), price.amount(), price.source(), price.known());
			materials.merge(itemId, material, MaterialStack::merge);
		}
		return List.copyOf(materials.values());
	}

	private static void showRecipe(List<MaterialStack> recipeMaterials, String recipeName) {
		List<MaterialStack> neededMaterials = recipeMaterials.stream()
			.filter(MaterialStack::purchasable)
			.toList();
		recipeNeededMaterials = neededMaterials;

		List<PanelLine> lines = new ArrayList<>();
		lines.add(PanelLine.text("§7Craft " + recipeName + " §7(§6" + formatCompact(totalPrice(recipeMaterials, 1)) + "§7)"));
		for (MaterialStack material : recipeMaterials) {
			String line = "§8" + formatCoins(material.amount()) + "x " + material.legacyName();
			if (material.purchasable()) {
				line += " §6" + formatCompact(material.totalPrice(1));
			}
			lines.add(PanelLine.text(line));
		}
		if (!neededMaterials.isEmpty()) {
			lines.add(PanelLine.clickable(
				"§eAdd to craft material collector!",
				List.of("§eClick here to help purchasing the items!"),
				() -> addToPurchasing(recipeNeededMaterials)
			));
		}
		craftDisplay = List.copyOf(lines);
	}

	private static void addToPurchasing(List<MaterialStack> neededMaterials) {
		purchasingMaterials = List.copyOf(neededMaterials);
		craftMultiplier = 1;
		purchasingCraftMaterials = true;
		inRecipeInventory = false;
		updatePurchasingDisplay();
	}

	private static void updatePurchasingDisplay() {
		List<PanelLine> lines = new ArrayList<>();
		lines.add(PanelLine.text("§7Buy items:"));
		for (MaterialStack material : purchasingMaterials) {
			int amount = material.amount() * craftMultiplier;
			String line = "§8" + formatCoins(amount) + "x " + material.legacyName() + " §6" + formatCompact(material.totalPrice(craftMultiplier));
			lines.add(PanelLine.clickable(line, buyTips(material, amount), () -> buyMaterial(material, amount)));
		}
		lines.add(PanelLine.clickable("§eStop!", List.of("§eClick here to stop this view!"), () -> {
			purchasingCraftMaterials = false;
			purchasingMaterials = List.of();
			craftDisplay = List.of();
		}));
		addMultiplierLines(lines);
		craftDisplay = List.copyOf(lines);
	}

	private static void addMultiplierLines(List<PanelLine> lines) {
		int[] multipliers = {1, 5, 16, 32, 64, 512};
		for (int multiplier : multipliers) {
			boolean selected = multiplier == craftMultiplier;
			String nameColor = selected ? "§a" : "§e";
			String priceColor = selected ? "§6" : "§7";
			String line = nameColor + "Multiply x" + multiplier + " " + priceColor + formatCompact(totalPrice(purchasingMaterials, multiplier));
			if (selected) {
				lines.add(PanelLine.text(line));
			} else {
				lines.add(PanelLine.clickable(line, List.of("§eClick here to multiply the items needed times " + multiplier + "!"), () -> {
					craftMultiplier = multiplier;
					updatePurchasingDisplay();
				}));
			}
		}
	}

	private static List<String> buyTips(MaterialStack material, int amount) {
		if (material.source() == SkyblockPrices.Source.BAZAAR) {
			return List.of(
				"§eClick to open Bazaar search.",
				"§7Amount copied: §e" + formatCoins(amount)
			);
		}
		if (material.source() == SkyblockPrices.Source.AUCTION) {
			return List.of("§eClick to open Auction House search.");
		}
		return List.of("§cNo AH or BZ price source is available.");
	}

	private static void buyMaterial(MaterialStack material, int amount) {
		if (material.source() == SkyblockPrices.Source.BAZAAR) {
			searchForBazaarItem(material.plainName(), amount);
		} else if (material.source() == SkyblockPrices.Source.AUCTION) {
			InventoryFeatureUtils.sendCommand("ah " + material.plainName());
		} else {
			InventoryFeatureUtils.chat("Could not find " + material.plainName() + " on AH or BZ.");
		}
	}

	private static double totalPrice(List<MaterialStack> materials, int multiplier) {
		double total = 0.0D;
		for (MaterialStack material : materials) {
			total += material.totalPrice(multiplier);
		}
		return total;
	}

	static boolean shouldRenderCraftMaterialCollector() {
		return (inRecipeInventory || purchasingCraftMaterials) && !craftDisplay.isEmpty();
	}

	static List<PanelLine> craftMaterialHudLines() {
		return craftDisplay;
	}

	static void setCraftMaterialHitbox(int x, int y, int width, int height, float scale) {
		craftPanelX = x;
		craftPanelY = y;
		craftPanelWidth = Math.max(0, width);
		craftPanelHeight = Math.max(0, height);
		craftPanelScale = scale <= 0.0F ? 1.0F : scale;
	}

	static void clearCraftMaterialHitbox() {
		setCraftMaterialHitbox(0, 0, 0, 0, 1.0F);
	}

	static int craftMaterialLineAt(double mouseX, double mouseY) {
		return craftPanelLineAt(mouseX, mouseY);
	}

	private static int craftPanelLineAt(double mouseX, double mouseY) {
		int scaledWidth = Math.round(craftPanelWidth * craftPanelScale);
		int scaledHeight = Math.round(craftPanelHeight * craftPanelScale);
		if (scaledWidth <= 0 || scaledHeight <= 0 || mouseX < craftPanelX || mouseX >= craftPanelX + scaledWidth || mouseY < craftPanelY || mouseY >= craftPanelY + scaledHeight) {
			return -1;
		}
		double localY = (mouseY - craftPanelY) / craftPanelScale;
		int lineIndex = (int) ((localY - BazaarHudRenderer.PADDING_Y) / BazaarHudRenderer.ROW_HEIGHT);
		return lineIndex < 0 || lineIndex >= craftDisplay.size() ? -1 : lineIndex;
	}

	private static boolean hasProductActionSlots(ScreenHandler handler) {
		Slot buy = slot(handler, 10);
		Slot sell = slot(handler, 11);
		return (buy != null && buy.hasStack() && InventoryFeatureUtils.itemName(buy.getStack()).equalsIgnoreCase("Buy Instantly"))
			|| (sell != null && sell.hasStack() && InventoryFeatureUtils.itemName(sell.getStack()).equalsIgnoreCase("Sell Instantly"));
	}

	private static boolean isProductView(String title, ScreenHandler handler) {
		if (hasProductActionSlots(handler)) {
			return true;
		}
		return currentlyOpenedProduct != null && PRODUCT_TITLE.matcher(InventoryFeatureUtils.strip(title)).matches();
	}

	private static void readCancelledAmount(ScreenHandler handler) {
		Slot cancel = slot(handler, 11);
		if (cancel != null && cancel.hasStack() && InventoryFeatureUtils.itemName(cancel.getStack()).equalsIgnoreCase("Cancel Order")) {
			readCancelledAmount(cancel.getStack());
		}
	}

	private static void readCancelledAmount(ItemStack stack) {
		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			Matcher matcher = MISSING_ITEMS.matcher(line);
			if (matcher.find()) {
				latestCancelledAmount = (int) InventoryFeatureUtils.parseLong(matcher.group(1));
				return;
			}
		}
	}

	private static boolean isBazaarOrderInventory(String title) {
		String clean = InventoryFeatureUtils.strip(title);
		return clean.equals("Your Bazaar Orders") || clean.equals("Co-op Bazaar Orders");
	}

	public static boolean tryHandleClick(ClickEvent.Custom custom, MinecraftClient client) {
		if (!REORDER_CANCELLED_BUY_ORDER_ACTION.equals(custom.id())) {
			return false;
		}

		custom.payload()
			.flatMap(element -> element instanceof NbtString string ? string.asString() : Optional.empty())
			.ifPresent(payload -> {
				int separator = payload.indexOf('\n');
				if (separator <= 0 || separator + 1 >= payload.length()) {
					return;
				}

				Integer amount = parseInteger(payload.substring(0, separator));
				String product = payload.substring(separator + 1).trim();
				searchForBazaarItem(product, amount);
			});
		return true;
	}

	private static void updateLiveBuyPrices(ScreenHandler handler) {
		liveBuyOrderPrice = null;
		liveInstantBuyPrice = null;
		for (Slot slot : handler.slots) {
			if (!InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
				continue;
			}

			String name = InventoryFeatureUtils.itemName(slot.getStack());
			if (name.equalsIgnoreCase("Create Buy Order")) {
				readFirstPrice(slot.getStack(), COINS_EACH).ifPresent(value -> liveBuyOrderPrice = value + 0.1D);
			} else if (name.equalsIgnoreCase("Buy Instantly")) {
				readFirstPrice(slot.getStack(), PRICE_PER_UNIT).ifPresent(value -> liveInstantBuyPrice = value);
			}
		}
	}

	private static Optional<Double> readFirstPrice(ItemStack stack, Pattern pattern) {
		for (String line : InventoryFeatureUtils.strippedLore(stack)) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				double price = InventoryFeatureUtils.parseDouble(matcher.group(1));
				if (price > 0.0D) {
					return Optional.of(price);
				}
			}
		}
		return Optional.empty();
	}

	private static boolean isGoBackToBazaar(@Nullable Slot slot) {
		if (slot == null || !slot.hasStack() || !InventoryFeatureUtils.itemName(slot.getStack()).equalsIgnoreCase("Go Back")) {
			return false;
		}
		return InventoryFeatureUtils.strippedLore(slot.getStack()).stream()
			.anyMatch(line -> line.toLowerCase(Locale.ROOT).contains("to bazaar"));
	}

	private static boolean isBazaarPurchaseFlow(String title) {
		String clean = InventoryFeatureUtils.strip(title);
		return clean.equals("How many do you want?")
			|| clean.equals("How much do you want to pay?")
			|| clean.equals("Confirm Buy Order")
			|| clean.equals("Confirm Instant Buy")
			|| clean.equals("At what price are you selling?")
			|| clean.equals("Confirm Sell Offer")
			|| clean.equals("Order options");
	}

	private static void recordClickedItem(Slot slot) {
		if (slot == null || InventoryFeatureUtils.topInventorySlot(slot) || !slot.hasStack()) {
			return;
		}

		String itemId = SkyblockItemIds.resolveItemId(slot.getStack());
		if (itemId == null || itemId.isBlank()) {
			return;
		}

		lastClickedProduct = itemId;
		lastClickedCount = slot.getStack().getCount();
		lastClickedCloseCount = 0;
	}

	private static int topInventorySize(ScreenHandler handler) {
		int max = 0;
		for (Slot slot : handler.slots) {
			if (InventoryFeatureUtils.topInventorySlot(slot)) {
				max = Math.max(max, slot.id + 1);
			}
		}
		return max;
	}

	private static String skyblockBzProductId(String productId) {
		return productId.contains(";") ? "ENCHANTMENT_" + productId.replace(";", "_") : productId;
	}

	private static String limitColor(double coins) {
		double remaining = Math.max(0.0D, DAILY_LIMIT - coins);
		double percentage = remaining / DAILY_LIMIT;
		if (percentage > 0.5D) {
			return "§a";
		}
		if (percentage > 0.25D) {
			return "§e";
		}
		if (percentage > 0.0D) {
			return "§6";
		}
		return "§c";
	}

	@Nullable
	private static Integer parseInteger(String value) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
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

	private static String formatCompact(double value) {
		double abs = Math.abs(value);
		if (abs >= 1_000_000_000.0D) {
			return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0D);
		}
		if (abs >= 1_000_000.0D) {
			return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
		}
		if (abs >= 1_000.0D) {
			return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
		}
		return String.format(Locale.ROOT, "%,.0f", value);
	}

	private static String formatCoins(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	private static EMSkyblockConfig.Bazaar config() {
		EMSkyblockConfig config = InventoryFeatureUtils.config();
		return config == null ? null : config.inventory.bazaar;
	}

	record PanelLine(String legacy, @Nullable PanelAction action, List<String> tips) {
		private PanelLine(String legacy, @Nullable PanelAction action) {
			this(legacy, action, List.of());
		}

		private static PanelLine text(String legacy) {
			return new PanelLine(legacy, null, List.of());
		}

		private static PanelLine clickable(String legacy, List<String> tips, PanelAction action) {
			return new PanelLine(legacy, action, tips);
		}
	}

	private record OpenedProduct(String itemId, String displayName) {
	}

	@FunctionalInterface
	interface PanelAction {
		void run();
	}

	private record MaterialStack(
		String itemId,
		String legacyName,
		String plainName,
		int amount,
		double unitPrice,
		SkyblockPrices.Source source,
		boolean knownPrice
	) {
		private static MaterialStack merge(MaterialStack left, MaterialStack right) {
			MaterialStack priceSource = left.knownPrice ? left : right;
			return new MaterialStack(
				left.itemId,
				left.legacyName,
				left.plainName,
				left.amount + right.amount,
				priceSource.unitPrice,
				priceSource.source,
				priceSource.knownPrice
			);
		}

		private boolean purchasable() {
			return knownPrice && (source == SkyblockPrices.Source.BAZAAR || source == SkyblockPrices.Source.AUCTION);
		}

		private double totalPrice(int multiplier) {
			return knownPrice ? unitPrice * amount * multiplier : 0.0D;
		}
	}
}
