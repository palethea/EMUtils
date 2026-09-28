package net.emutils.client.emutils.hud;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Look-At Info (#45): a card in the HUD Overlay's look about the block or entity in the crosshair. Its
 * item, name and ID on top, then label and value lines drawn like the HUD Overlay's.
 * <p>
 * The card's width follows its content, but its place in the HUD layout is a fixed-width slot, so it
 * doesn't jump around: it lines up with the slot's left edge, middle or right edge, depending on which
 * third of the screen the slot is in.
 */
public final class LookAtInfoRenderer {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "look_at_info");
	/** Width of the card's slot in the HUD layout; wider content widens it. */
	static final int SLOT_WIDTH = 170;
	private static final int TOP_MARGIN = 4;
	private static final int ITEM_SIZE = 16;
	private static final int ITEM_GAP = 6;
	private static final int NAME_HEIGHT = 12;
	private static final int ID_HEIGHT = 10;
	private static final int HEADER_LINES_GAP = 5;
	private static final int MIN_CONTENT_WIDTH = 90;
	private static final UiText.Size NAME_SIZE = UiText.Size.BOLD;
	private static final UiText.Size ID_SIZE = UiText.Size.BODY;

	private LookAtInfoRenderer() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, ID, (context, tickCounter) -> render(context));
	}

	public static void tick(Minecraft client) {
		LookAtInfoData.tick(client, EMUtilsClient.config());
	}

	/** What the card shows: the current target, or a sample in the layout editor while nothing is targeted. */
	static LookAtInfoData shown(EMUtilsConfig config) {
		LookAtInfoData data = LookAtInfoData.current();
		return data != null ? data : LookAtInfoData.preview(config);
	}

	static int slotWidth(EMUtilsConfig config, LookAtInfoData data) {
		return Math.max(SLOT_WIDTH, cardWidth(config, data));
	}

	static int cardWidth(EMUtilsConfig config, LookAtInfoData data) {
		Font font = Minecraft.getInstance().font;
		int headerWidth = UiText.width(font, Component.literal(data.name()), NAME_SIZE);
		if (config.lookAtInfoShowId()) {
			headerWidth = Math.max(headerWidth, UiText.width(font, Component.literal(data.id()), ID_SIZE));
		}
		if (showItem(config, data)) {
			headerWidth += ITEM_SIZE + ITEM_GAP;
		}
		int linesWidth = 0;
		if (!data.lines().isEmpty()) {
			int valueWidth = 0;
			for (HudOverlayLine line : data.lines()) {
				valueWidth = Math.max(valueWidth, UiText.glyphsWidth(font, line.value(), HudOverlayRenderer.VALUE_SIZE));
			}
			int iconWidth = config.lookAtInfoShowIcons() ? HudOverlayRenderer.ICON_SIZE + HudOverlayRenderer.ICON_GAP : 0;
			linesWidth = iconWidth + HudOverlayRenderer.labelColumnWidth(font, data.lines()) + HudOverlayRenderer.LABEL_VALUE_GAP + valueWidth;
		}
		return Math.max(MIN_CONTENT_WIDTH, Math.max(headerWidth, linesWidth)) + HudOverlayRenderer.PADDING_X * 2;
	}

	static int cardHeight(EMUtilsConfig config, LookAtInfoData data) {
		int height = HudOverlayRenderer.PADDING_Y * 2 + headerHeight(config, data);
		if (!data.lines().isEmpty()) {
			height += HEADER_LINES_GAP + data.lines().size() * HudOverlayRenderer.ROW_HEIGHT;
		}
		return height;
	}

	static int defaultX(int screenWidth, int slotWidth) {
		return (screenWidth - slotWidth) / 2;
	}

	static int defaultY() {
		return TOP_MARGIN;
	}

	/** Draws the card inside its slot at ({@code slotX}, {@code slotY}), lined up by the slot's place on screen. */
	static void renderInSlot(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		LookAtInfoData data,
		int slotX,
		int slotY,
		int slotScreenX,
		int slotScreenWidth,
		int screenWidth,
		int opacityPercent
	) {
		int slotWidth = slotWidth(config, data);
		int cardWidth = cardWidth(config, data);
		int offset = switch (third(slotScreenX + slotScreenWidth / 2, screenWidth)) {
			case 0 -> 0;
			case 1 -> (slotWidth - cardWidth) / 2;
			default -> slotWidth - cardWidth;
		};
		renderCard(context, client, config, data, slotX + offset, slotY, cardWidth, cardHeight(config, data), opacityPercent);
	}

	private static int third(int centerX, int screenWidth) {
		if (centerX < screenWidth / 3) {
			return 0;
		}
		return centerX > screenWidth * 2 / 3 ? 2 : 1;
	}

	private static void renderCard(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		LookAtInfoData data,
		int x,
		int y,
		int width,
		int height,
		int opacityPercent
	) {
		UiTheme theme = UiTheme.current();
		HudOverlayRenderer.drawCard(context, theme, x, y, width, height, opacityPercent);
		Font font = client.font;
		boolean shadow = config.lookAtInfoTextShadow().shadow(opacityPercent, HudOverlayRenderer.TEXT_SHADOW_BELOW_OPACITY);

		int headerTop = y + HudOverlayRenderer.PADDING_Y;
		int headerHeight = headerHeight(config, data);
		int textX = x + HudOverlayRenderer.PADDING_X;
		if (showItem(config, data)) {
			context.item(data.icon(), textX, headerTop + (headerHeight - ITEM_SIZE) / 2);
			textX += ITEM_SIZE + ITEM_GAP;
		}
		int textTop = headerTop + (headerHeight - textHeight(config)) / 2;
		drawText(context, font, Component.literal(data.name()), NAME_SIZE, textX, textTop + NAME_HEIGHT / 2, theme.text(), shadow);
		if (config.lookAtInfoShowId()) {
			drawText(context, font, Component.literal(data.id()), ID_SIZE, textX, textTop + NAME_HEIGHT + ID_HEIGHT / 2, theme.muted(), shadow);
		}

		if (data.lines().isEmpty()) {
			return;
		}
		int linesTop = headerTop + headerHeight + HEADER_LINES_GAP;
		int dividerY = linesTop - HEADER_LINES_GAP / 2 - 1;
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		context.fill(x + HudOverlayRenderer.PADDING_X, dividerY, x + width - HudOverlayRenderer.PADDING_X, dividerY + 1, UiOpacity.apply(UiTheme.fade(theme.line(), opacity)));
		boolean showIcons = config.lookAtInfoShowIcons();
		int iconX = x + HudOverlayRenderer.PADDING_X;
		int labelX = iconX + (showIcons ? HudOverlayRenderer.ICON_SIZE + HudOverlayRenderer.ICON_GAP : 0);
		int valueX = labelX + HudOverlayRenderer.labelColumnWidth(font, data.lines()) + HudOverlayRenderer.LABEL_VALUE_GAP;
		int rowY = linesTop;
		for (HudOverlayLine line : data.lines()) {
			HudOverlayRenderer.drawLine(context, font, theme, line, showIcons, shadow, iconX, labelX, valueX, rowY);
			rowY += HudOverlayRenderer.ROW_HEIGHT;
		}
	}

	private static void drawText(GuiGraphicsExtractor context, Font font, Component text, UiText.Size size, int x, int centerY, int color, boolean shadow) {
		int top = centerY - UiText.lineHeight(font, size) / 2;
		if (shadow) {
			float offset = HudOverlayRenderer.shadowOffset();
			UiText.drawExact(context, font, text, size, x + offset, top + offset, HudOverlayRenderer.shadowColor());
		}
		UiText.draw(context, font, text, size, x, top, color);
	}

	private static boolean showItem(EMUtilsConfig config, LookAtInfoData data) {
		return config.lookAtInfoShowIcons() && !data.icon().isEmpty();
	}

	private static int textHeight(EMUtilsConfig config) {
		return NAME_HEIGHT + (config.lookAtInfoShowId() ? ID_HEIGHT : 0);
	}

	private static int headerHeight(EMUtilsConfig config, LookAtInfoData data) {
		return Math.max(textHeight(config), showItem(config, data) ? ITEM_SIZE : 0);
	}

	private static void render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || client == null || client.player == null || client.level == null) {
			return;
		}
		// The layout editor draws its own preview.
		if (!config.lookAtInfo() || HudLayoutManager.isEditing()) {
			return;
		}
		LookAtInfoData data = LookAtInfoData.current();
		if (data == null) {
			return;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		if (config.lookAtInfoHideWithDebug() && client.getDebugOverlay().showDebugScreen()) {
			return;
		}
		if (config.lookAtInfoHideInContainers() && MinecraftClientCompat.screen(client) instanceof AbstractContainerScreen<?>) {
			return;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			EMUtilsHudElements.LOOK_AT_INFO,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		context.pose().pushMatrix();
		UiRasterScale.set(layout.scaleFactor());
		try {
			context.pose().translate(layout.position().x(), layout.position().y());
			context.pose().scale(layout.scaleFactor(), layout.scaleFactor());
			renderInSlot(
				context,
				client,
				config,
				data,
				0,
				0,
				layout.position().x(),
				layout.dimensions().width(),
				context.guiWidth(),
				layout.opacityPercent()
			);
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
	}
}
