package net.emutils.client.emskyblock.features.inventory.bazaar;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emskyblock.config.EMSkyblockConfig;
import net.emutils.client.emskyblock.features.fishing.trackercommon.TrackerPanelRenderer;
import net.emutils.client.emskyblock.features.inventory.common.InventoryFeatureUtils;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class BazaarHudRenderer {
	static final int ROW_HEIGHT = 10;
	static final int PADDING_X = TrackerPanelRenderer.PADDING_X;
	static final int PADDING_Y = TrackerPanelRenderer.PADDING_Y;
	private static final int MIN_WIDTH = 1;

	private BazaarHudRenderer() {
	}

	public enum ElementType {
		BEST_SELL_METHOD(HudElementId.BAZAAR_BEST_SELL_METHOD, 394, 142),
		MAX_PURSE_ITEMS(HudElementId.BAZAAR_MAX_PURSE_ITEMS, 346, 90),
		DAILY_LIMIT(HudElementId.BAZAAR_DAILY_LIMIT, 550, 150),
		CRAFT_MATERIAL_COLLECTOR(HudElementId.BAZAAR_CRAFT_MATERIAL_COLLECTOR, 50, 50);

		private final HudElementId id;
		private final int defaultX;
		private final int defaultY;

		ElementType(HudElementId id, int defaultX, int defaultY) {
			this.id = id;
			this.defaultX = defaultX;
			this.defaultY = defaultY;
		}

		public HudElementId id() {
			return id;
		}

		public int defaultX() {
			return defaultX;
		}

		public int defaultY() {
			return defaultY;
		}
	}

	public static void register() {
	}

	public static HudOverlayPlacement.PanelDimensions unscaledDimensions(ElementType type, MinecraftClient client) {
		TextRenderer textRenderer = client.textRenderer;
		if (type == ElementType.CRAFT_MATERIAL_COLLECTOR) {
			return dimensions(textRenderer, craftPreviewLegacyLines());
		}
		return dimensions(textRenderer, previewLines(type));
	}

	public static List<String> previewLines(ElementType type) {
		return switch (type) {
			case BEST_SELL_METHOD -> List.of("§eEnchanted Diamond§7 sell difference: §62.3k coins");
			case MAX_PURSE_ITEMS -> List.of(
				"§7Max items with purse",
				"§7Buy order +0.1: §e1,024x",
				"§7Instant buy: §e991x"
			);
			case DAILY_LIMIT -> List.of(
				"§aBazaar Daily Limit:",
				"§a1,250,000§7/§615B coins"
			);
			case CRAFT_MATERIAL_COLLECTOR -> craftPreviewLegacyLines();
		};
	}

	public static void renderScreenOverlays(
		DrawContext context,
		ScreenHandler handler,
		EMSkyblockConfig.Bazaar config,
		MinecraftClient client,
		int mouseX,
		int mouseY
	) {
		EMUtilsConfig baseConfig = EMUtilsClient.config();
		if (baseConfig == null) {
			return;
		}

		if (config.bestSellMethod) {
			renderLegacyElement(
				context,
				client,
				baseConfig,
				ElementType.BEST_SELL_METHOD,
				BazaarFeatures.bestSellHudLines(handler)
			);
		}
		if (config.maxPurseItems) {
			renderLegacyElement(
				context,
				client,
				baseConfig,
				ElementType.MAX_PURSE_ITEMS,
				BazaarFeatures.maxPurseHudLines()
			);
		}
		if (config.dailyLimitTracker) {
			renderLegacyElement(
				context,
				client,
				baseConfig,
				ElementType.DAILY_LIMIT,
				BazaarFeatures.dailyLimitHudLines()
			);
		}
		if (config.craftMaterialsFromBazaar && BazaarFeatures.shouldRenderCraftMaterialCollector()) {
			renderCraftMaterialCollector(context, client, baseConfig, mouseX, mouseY);
		} else {
			BazaarFeatures.clearCraftMaterialHitbox();
		}
	}

	private static void renderLegacyElement(
		DrawContext context,
		MinecraftClient client,
		EMUtilsConfig config,
		ElementType type,
		List<String> lines
	) {
		if (lines.isEmpty()) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = resolveLayout(context, client, config, type);
		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderLegacyPanel(context, client.textRenderer, lines, 0, 0, layout.opacityPercent());
		} finally {
			context.getMatrices().popMatrix();
		}
	}

	private static void renderCraftMaterialCollector(
		DrawContext context,
		MinecraftClient client,
		EMUtilsConfig config,
		int mouseX,
		int mouseY
	) {
		List<BazaarFeatures.PanelLine> display = BazaarFeatures.craftMaterialHudLines();
		if (display.isEmpty()) {
			BazaarFeatures.clearCraftMaterialHitbox();
			return;
		}

		List<String> legacyLines = new ArrayList<>(display.size());
		for (BazaarFeatures.PanelLine line : display) {
			legacyLines.add(line.legacy());
		}

		HudLayoutManager.ResolvedLayout layout = resolveLayout(context, client, config, ElementType.CRAFT_MATERIAL_COLLECTOR);
		HudOverlayPlacement.PanelDimensions dimensions = dimensions(client.textRenderer, legacyLines);
		BazaarFeatures.setCraftMaterialHitbox(
			layout.position().x(),
			layout.position().y(),
			dimensions.width(),
			dimensions.height(),
			layout.scaleFactor()
		);

		int hoverLine = BazaarFeatures.craftMaterialLineAt(mouseX, mouseY);
		context.getMatrices().pushMatrix();
		try {
			context.getMatrices().translate(layout.position().x(), layout.position().y());
			context.getMatrices().scale(layout.scaleFactor(), layout.scaleFactor());
			renderLegacyPanel(context, client.textRenderer, legacyLines, 0, 0, layout.opacityPercent());
			if (hoverLine >= 0 && hoverLine < display.size() && display.get(hoverLine).action() != null) {
				int rowY = PADDING_Y + hoverLine * ROW_HEIGHT;
				context.fill(1, rowY - 1, dimensions.width() - 1, rowY + ROW_HEIGHT, 0x44000000);
				context.drawTextWithShadow(
					client.textRenderer,
					InventoryFeatureUtils.legacyText("§f" + display.get(hoverLine).legacy()),
					PADDING_X,
					rowY,
					0xFFFFFFFF
				);
			}
		} finally {
			context.getMatrices().popMatrix();
		}

		if (hoverLine >= 0 && hoverLine < display.size() && !display.get(hoverLine).tips().isEmpty()) {
			InventoryFeatureUtils.drawPanelClamped(context, client.textRenderer, mouseX + 12, mouseY + 8, display.get(hoverLine).tips());
		}
	}

	public static void renderLegacyPanel(
		DrawContext context,
		TextRenderer textRenderer,
		List<String> lines,
		int x,
		int y,
		int opacityPercent
	) {
		if (lines.isEmpty()) {
			return;
		}

		HudOverlayPlacement.PanelDimensions dimensions = dimensions(textRenderer, lines);
		TrackerPanelRenderer.drawBackground(context, x, y, dimensions.width(), dimensions.height(), opacityPercent);
		renderLegacyLines(context, textRenderer, lines, x + PADDING_X, y + PADDING_Y);
	}

	private static void renderLegacyLines(DrawContext context, TextRenderer textRenderer, List<String> lines, int x, int y) {
		for (int index = 0; index < lines.size(); index++) {
			Text text = InventoryFeatureUtils.legacyText("§f" + lines.get(index));
			context.drawTextWithShadow(textRenderer, text, x, y + index * ROW_HEIGHT, 0xFFFFFFFF);
		}
	}

	private static HudLayoutManager.ResolvedLayout resolveLayout(
		DrawContext context,
		MinecraftClient client,
		EMUtilsConfig config,
		ElementType type
	) {
		return HudLayoutManager.resolveLayout(
			type.id(),
			config,
			context.getScaledWindowWidth(),
			context.getScaledWindowHeight(),
			client
		);
	}

	private static HudOverlayPlacement.PanelDimensions dimensions(TextRenderer textRenderer, List<String> legacyLines) {
		int width = MIN_WIDTH;
		for (String line : legacyLines) {
			width = Math.max(width, textRenderer.getWidth(InventoryFeatureUtils.legacyText("§f" + line)));
		}
		return new HudOverlayPlacement.PanelDimensions(width + PADDING_X * 2, Math.max(1, legacyLines.size() * ROW_HEIGHT + PADDING_Y * 2));
	}

	private static List<String> craftPreviewLegacyLines() {
		return List.of(
			"§7Craft §aEnchanted Diamond Block §7(§61.2M§7)",
			"§8512x §aEnchanted Diamond §61.2M",
			"§eAdd to craft material collector!"
		);
	}
}
