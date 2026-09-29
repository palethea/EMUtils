package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * The custom scoreboard (#169): the server's sidebar as an EMUtils HUD element. It shows what vanilla
 * shows, from {@link ScoreboardData}, as a card in the HUD Overlay's look or with vanilla's own bars, in
 * Minecraft's font or the EMUtils UI font.
 * <p>
 * It replaces vanilla's scoreboard element, so with it turned off vanilla draws its own as before. Its
 * place in the HUD layout is a slot that is always tall enough for 15 lines and at least
 * {@link #SLOT_WIDTH} wide, so it doesn't jump around as the server changes the sidebar. The card lines up
 * with the slot's left, middle or right side, and its top, middle or bottom, depending on which third of the
 * screen the slot is in. By default the slot is at the right edge, vertically centered, where vanilla draws
 * the sidebar.
 */
public final class ScoreboardRenderer {
	/** Width of the card's slot in the HUD layout at 100%; a wider card widens it. */
	static final int SLOT_WIDTH = 190;
	private static final int RIGHT_MARGIN = 4;
	/** Space kept free at the screen's edges when a card is wider than the screen allows. */
	private static final int SCREEN_EDGE = 4;
	private static final int MIN_CARD_WIDTH = 80;
	/** Between a name and its score, in the card style. */
	private static final int CARD_SCORE_GAP = 10;
	private static final int VANILLA_PADDING_X = 2;
	private static final int TITLE_LINES_GAP = 5;

	private ScoreboardRenderer() {
	}

	/** One sidebar laid out: its text ready to draw, and how big the card is. */
	private record Card(
		ServerText text,
		ServerText.Prepared title,
		List<ServerText.Prepared> names,
		List<ServerText.Prepared> scores,
		boolean vanilla,
		int rowHeight,
		int paddingX,
		int contentWidth,
		int width,
		int height
	) {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, vanilla -> (context, tickCounter) -> {
			if (!render(context)) {
				vanilla.extractRenderState(context, tickCounter);
			}
		});
	}

	/** The sidebar's own lines, or a sample in the layout editor while the server sends none. */
	static ScoreboardData shown(Minecraft client) {
		ScoreboardData data = ScoreboardData.current(client);
		return data != null ? data : ScoreboardData.sample();
	}

	private static int maxCardWidth(Minecraft client, int scalePercent) {
		int screenWidth = client.getWindow().getGuiScaledWidth();
		return Math.max(MIN_CARD_WIDTH, screenWidth * 100 / Math.max(1, scalePercent) - SCREEN_EDGE * 2);
	}

	private static Card layout(Minecraft client, EMUtilsConfig config, ScoreboardData data, int maxWidth) {
		boolean vanilla = config.scoreboardStyle() == HudStyle.VANILLA;
		ServerText text = new ServerText(client.font, config.scoreboardFont());
		int rowHeight = text.rowHeight(vanilla);
		int paddingX = vanilla ? VANILLA_PADDING_X : HudOverlayRenderer.PADDING_X;
		int maxContent = Math.max(1, maxWidth - paddingX * 2);
		int gap = vanilla ? text.width(": ") : CARD_SCORE_GAP;

		ServerText.Prepared title = text.prepare(data.title(), config.scoreboardTitleBold(), maxContent);
		List<ServerText.Prepared> names = new ArrayList<>();
		List<ServerText.Prepared> scores = new ArrayList<>();
		int contentWidth = title.width();
		for (ScoreboardData.Line line : data.lines()) {
			ServerText.Prepared score = text.prepare(config.scoreboardShowNumbers() ? line.score() : Component.empty(), false, maxContent);
			int reserved = score.width() > 0 ? gap + score.width() : 0;
			ServerText.Prepared name = text.prepare(line.name(), false, Math.max(1, maxContent - reserved));
			names.add(name);
			scores.add(score);
			contentWidth = Math.max(contentWidth, name.width() + reserved);
		}
		int lines = names.size();
		int height;
		if (vanilla) {
			// The title bar, then the body, which has a pixel of room above its first line.
			height = rowHeight + 1 + lines * rowHeight;
		} else if (lines == 0) {
			height = HudOverlayRenderer.PADDING_Y * 2 + rowHeight;
		} else {
			height = HudOverlayRenderer.PADDING_Y * 2 + rowHeight + TITLE_LINES_GAP + lines * rowHeight;
		}
		return new Card(text, title, names, scores, vanilla, rowHeight, paddingX, contentWidth, contentWidth + paddingX * 2, height);
	}

	private static Card layout(Minecraft client, EMUtilsConfig config, ScoreboardData data) {
		return layout(client, config, data, maxCardWidth(client, HudLayoutManager.layoutScale(EMUtilsHudElements.SCOREBOARD, config)));
	}

	/** The slot's width: the card's, if that is wider than {@link #SLOT_WIDTH}. */
	private static int slotWidth(Card card) {
		return Math.max(SLOT_WIDTH, card.width());
	}

	/** The slot's height: what a card with 15 lines needs, whatever the sidebar has now. */
	private static int slotHeight(Minecraft client, EMUtilsConfig config) {
		boolean vanilla = config.scoreboardStyle() == HudStyle.VANILLA;
		int rowHeight = new ServerText(client.font, config.scoreboardFont()).rowHeight(vanilla);
		int lines = ScoreboardData.MAX_LINES;
		if (vanilla) {
			return rowHeight + 1 + lines * rowHeight;
		}
		return HudOverlayRenderer.PADDING_Y * 2 + rowHeight + TITLE_LINES_GAP + lines * rowHeight;
	}

	static HudOverlayPlacement.PanelDimensions slotDimensions(Minecraft client, EMUtilsConfig config) {
		Card card = layout(client, config, shown(client));
		return new HudOverlayPlacement.PanelDimensions(slotWidth(card), slotHeight(client, config));
	}

	static HudOverlayPlacement.Position defaultPosition(int screenWidth, int screenHeight, HudOverlayPlacement.PanelDimensions dimensions) {
		return new HudOverlayPlacement.Position(screenWidth - dimensions.width() - RIGHT_MARGIN, (screenHeight - dimensions.height()) / 2);
	}

	/**
	 * Draws the card inside its slot, which is at ({@code slotX}, {@code slotY}) in the current transform
	 * and at ({@code screenX}, {@code screenY}) with size {@code screenWidth} x {@code screenHeight} on
	 * screen. The card lines up with the slot by the slot's third of the screen.
	 */
	static void renderInSlot(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		ScoreboardData data,
		int slotX,
		int slotY,
		int screenX,
		int screenY,
		int screenWidth,
		int screenHeight,
		int opacityPercent
	) {
		Card card = layout(client, config, data);
		int slotWidth = slotWidth(card);
		int slotHeight = slotHeight(client, config);
		int offsetX = switch (third(screenX + screenWidth / 2, context.guiWidth())) {
			case 0 -> 0;
			case 1 -> (slotWidth - card.width()) / 2;
			default -> slotWidth - card.width();
		};
		int offsetY = switch (third(screenY + screenHeight / 2, context.guiHeight())) {
			case 0 -> 0;
			case 1 -> (slotHeight - card.height()) / 2;
			default -> slotHeight - card.height();
		};
		draw(context, client, config, card, slotX + offsetX, slotY + offsetY, opacityPercent);
	}

	private static int third(int center, int size) {
		if (center < size / 3) {
			return 0;
		}
		return center > size * 2 / 3 ? 2 : 1;
	}

	private static void draw(GuiGraphicsExtractor context, Minecraft client, EMUtilsConfig config, Card card, int x, int y, int opacityPercent) {
		UiTheme theme = UiTheme.current();
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		boolean shadow = config.scoreboardTextShadow().shadow(opacityPercent, HudOverlayRenderer.TEXT_SHADOW_BELOW_OPACITY);
		int textColor = card.vanilla() ? 0xFFFFFFFF : theme.text();
		int rowHeight = card.rowHeight();
		int lines = card.names().size();

		int titleTop = y;
		int linesTop;
		if (card.vanilla()) {
			// The dark bars vanilla draws, with the same alpha.
			context.fill(x, y, x + card.width(), y + rowHeight, UiTheme.fade(client.options.getBackgroundColor(0.4F), opacity));
			context.fill(x, y + rowHeight, x + card.width(), y + card.height(), UiTheme.fade(client.options.getBackgroundColor(0.3F), opacity));
			titleTop += card.text().titleNudge();
			linesTop = y + rowHeight + 1;
		} else {
			HudOverlayRenderer.drawCard(context, theme, x, y, card.width(), card.height(), opacityPercent);
			titleTop += HudOverlayRenderer.PADDING_Y;
			linesTop = titleTop + rowHeight + TITLE_LINES_GAP;
			if (lines > 0) {
				int dividerY = linesTop - TITLE_LINES_GAP / 2 - 1;
				context.fill(x + card.paddingX(), dividerY, x + card.width() - card.paddingX(), dividerY + 1, UiOpacity.apply(UiTheme.fade(theme.line(), opacity)));
			}
		}

		int titleX = x + card.paddingX();
		if (config.scoreboardTitleAlignment() == ScoreboardTitleAlignment.CENTERED) {
			titleX += (card.contentWidth() - card.title().width()) / 2;
		}
		card.title().draw(context, titleX, titleTop, rowHeight, textColor, shadow);

		// Vanilla's scores end at the bar's edge, where the card's keep to its padding.
		int scoresRight = x + card.width() - (card.vanilla() ? 0 : card.paddingX());
		for (int i = 0; i < lines; i++) {
			int rowTop = linesTop + i * rowHeight;
			card.names().get(i).draw(context, x + card.paddingX(), rowTop, rowHeight, textColor, shadow);
			ServerText.Prepared score = card.scores().get(i);
			if (score.width() > 0) {
				score.draw(context, scoresRight - score.width(), rowTop, rowHeight, textColor, shadow);
			}
		}
	}

	/** How many lines the sidebar shows, or -1 without one, for UI snapshot checks. */
	public static int linesForSnapshot(Minecraft client) {
		ScoreboardData data = ScoreboardData.current(client);
		return data == null ? -1 : data.lines().size();
	}

	/** A line's name and score, as text joined by a bar, for UI snapshot checks. */
	public static String lineForSnapshot(Minecraft client, int index) {
		ScoreboardData data = ScoreboardData.current(client);
		if (data == null || index >= data.lines().size()) {
			return "";
		}
		ScoreboardData.Line line = data.lines().get(index);
		return line.name().getString() + "|" + line.score().getString();
	}

	/** The card's width in the sidebar's own place at the layout's scale, for UI snapshot checks. */
	public static int cardWidthForSnapshot(Minecraft client, EMUtilsConfig config) {
		return layout(client, config, shown(client)).width();
	}

	/**
	 * Draws the sidebar in place of vanilla's. Returns {@code false} to leave it to vanilla, which is when
	 * the custom scoreboard is turned off.
	 */
	private static boolean render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || !config.scoreboard()) {
			return false;
		}
		// The layout editor draws its own preview.
		if (client == null || client.player == null || client.level == null || HudLayoutManager.isEditing()) {
			return true;
		}
		if (MinecraftClientCompat.isHudHidden(client)) {
			return true;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return true;
		}
		if (config.scoreboardHideWithDebug() && client.getDebugOverlay().showDebugScreen()) {
			return true;
		}
		ScoreboardData data = ScoreboardData.current(client);
		if (data == null) {
			return true;
		}

		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			EMUtilsHudElements.SCOREBOARD,
			config,
			context.guiWidth(),
			context.guiHeight(),
			client
		);
		// Like vanilla's, on a layer of its own.
		context.nextStratum();
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
				layout.position().y(),
				layout.dimensions().width(),
				layout.dimensions().height(),
				layout.opacityPercent()
			);
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
		return true;
	}
}
