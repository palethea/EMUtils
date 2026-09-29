package net.emutils.client.emutils.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.EMUtilsHudElements;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiRasterScale;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.hud.layout.HudLayoutManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import org.jspecify.annotations.Nullable;

/**
 * The custom tab list (#170): the player list while Tab is held, as an EMUtils HUD element. It shows what
 * vanilla's {@code PlayerTabOverlay} shows, from {@link TabListData}, laid out the way it lays it out (rows
 * of up to 20 players, more columns as the list grows, the header on top and the footer below), as a card in
 * the HUD Overlay's look or with vanilla's own bars, in Minecraft's font or the EMUtils UI font.
 * <p>
 * It replaces vanilla's element, so with it turned off vanilla draws its own list as before. Its place in
 * the HUD layout is a slot at least {@link #SLOT_WIDTH} wide, at the top middle of the screen by default;
 * the card lines up with the slot's left, middle or right side, depending on which third of the screen the
 * slot is in, and grows downward.
 */
public final class TabListRenderer {
	private static final Identifier PING_UNKNOWN_SPRITE = Identifier.withDefaultNamespace("icon/ping_unknown");
	private static final Identifier PING_1_SPRITE = Identifier.withDefaultNamespace("icon/ping_1");
	private static final Identifier PING_2_SPRITE = Identifier.withDefaultNamespace("icon/ping_2");
	private static final Identifier PING_3_SPRITE = Identifier.withDefaultNamespace("icon/ping_3");
	private static final Identifier PING_4_SPRITE = Identifier.withDefaultNamespace("icon/ping_4");
	private static final Identifier PING_5_SPRITE = Identifier.withDefaultNamespace("icon/ping_5");
	private static final Identifier HEART_CONTAINER_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/container_blinking");
	private static final Identifier HEART_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/container");
	private static final Identifier HEART_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/full_blinking");
	private static final Identifier HEART_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/half_blinking");
	private static final Identifier HEART_ABSORBING_FULL_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking");
	private static final Identifier HEART_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
	private static final Identifier HEART_ABSORBING_HALF_BLINKING_SPRITE = Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking");
	private static final Identifier HEART_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/half");

	/** Width of the card's slot in the HUD layout at 100%; a wider card widens it. */
	static final int SLOT_WIDTH = 300;
	/** Where vanilla's list starts, a pixel above its header's text. */
	private static final int DEFAULT_TOP = 9;
	/** Vanilla keeps the list this far from the screen's sides. */
	private static final int SCREEN_MARGIN = 50;
	private static final int MIN_CONTENT_WIDTH = 60;
	private static final int SECTION_GAP = 5;
	private static final int PING_ICON_WIDTH = 10;
	private static final int PING_ICON_HEIGHT = 8;
	private static final int PING_NUMBER_GAP = 3;
	private static final int CARD_CELL_PADDING = 4;
	private static final int CARD_COLUMN_GAP = 10;
	private static final int VANILLA_COLUMN_GAP = 5;
	/** Width of vanilla's hearts for the list objective. */
	private static final int HEARTS_WIDTH = 90;
	/** Vanilla dims spectators to this alpha. */
	private static final int SPECTATOR_COLOR = 0x91FFFFFF;
	/** Vanilla's panel color, half-opaque black. */
	private static final int PANEL_COLOR = 0x80000000;
	/** Vanilla's row color. */
	private static final int ROW_COLOR = 0x20FFFFFF;
	/** In the layout editor the list is shown with sample players until it has this many. */
	private static final int PREVIEW_PLAYERS = 8;
	private static final int GOOD_PING = 150;
	private static final int SLOW_PING = 300;

	private static final String[] SAMPLE_NAMES = {
		"Alex", "Steve_99", "Notch", "Herobrine", "DiamondDan", "CreeperSlayer", "Luna", "PixelPete", "Redstoner", "Zombie_Zed",
		"BlockBuilder", "EnderFan", "IronIvy", "Gold_Gwen", "NetherNed", "Mineshaft", "Pigman", "SkyWalker", "Cobble", "Obsidian",
		"Lapis_Lee", "Quartz", "Sheepish", "Wolf_Pack", "Golem", "Villager_Vic", "Axolotl", "Dolphin", "Ocelot", "Panda_Pat",
		"Trident", "Elytra_Eli", "Beacon", "Anvil_Andy", "Piston", "Comparator", "Observer", "Hopper_Hal", "Dropper", "Sculk",
		"Warden", "Allay", "Camel_Cal", "Sniffer", "Frog_Fred", "Tadpole", "Glow_Squid", "Goat_Greg"
	};
	private static final int[] SAMPLE_PINGS = {23, 45, 88, 120, 160, 210, 320, 450, 700, 1200, -1, 60};

	private static final Map<UUID, HealthState> HEALTH_STATES = new HashMap<>();
	private static @Nullable TabListData snapshotData;

	private TabListRenderer() {
	}

	/** One laid-out tab list: its text ready to draw, and the size of the card. */
	private record Layout(
		ServerText text,
		boolean vanilla,
		TabListPing ping,
		boolean heads,
		int lineHeight,
		int rowHeight,
		int headSize,
		int headSlot,
		int cellPadding,
		int pingGap,
		int rightMargin,
		int columns,
		int rows,
		int columnWidth,
		int columnGap,
		int maxName,
		int extra,
		int pingNumberWidth,
		List<ServerText.Prepared> header,
		List<ServerText.Prepared> footer,
		List<ServerText.Prepared> names,
		List<ServerText.Prepared> scores,
		List<ServerText.Prepared> pings,
		int paddingX,
		int contentWidth,
		int gridWidth,
		int width,
		int height,
		int headerTop,
		int mainTop,
		int rowsTop,
		int footerTop
	) {
	}

	/** How long a health change of the list objective's hearts blinks, as vanilla times it. */
	private static final class HealthState {
		private int lastValue;
		private int displayedValue;
		private long lastUpdateTick;
		private long blinkUntilTick;

		HealthState(int initial) {
			this.displayedValue = initial;
			this.lastValue = initial;
		}

		void update(int current, long gameTime) {
			if (current != lastValue) {
				long duration = current < lastValue ? 20L : 10L;
				blinkUntilTick = gameTime + duration;
				lastValue = current;
				lastUpdateTick = gameTime;
			}
			if (gameTime - lastUpdateTick > 20L) {
				displayedValue = current;
			}
		}

		int displayedValue() {
			return displayedValue;
		}

		boolean isBlinking(long gameTime) {
			return blinkUntilTick > gameTime && (blinkUntilTick - gameTime) % 6L >= 3L;
		}
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, vanilla -> (context, tickCounter) -> {
			if (!render(context)) {
				vanilla.extractRenderState(context, tickCounter);
			}
		});
	}

	// ---- What is shown -------------------------------------------------------------------------

	/** The list to draw, in the order picked: the server's, or a made-up one while there is none. */
	static TabListData shown(Minecraft client, EMUtilsConfig config) {
		TabListData data = snapshotData;
		if (data == null) {
			data = TabListData.current(client);
			if (data == null || HudLayoutManager.isEditing()) {
				data = preview(client, data);
			}
		}
		return data.sorted(config.tabListSort());
	}

	/**
	 * What the layout editor shows: the real list, filled up with sample players and, where the server sets
	 * none, a sample header and footer, so the card has a size worth placing.
	 */
	private static TabListData preview(Minecraft client, @Nullable TabListData live) {
		List<TabListData.Entry> entries = new ArrayList<>();
		if (live != null) {
			entries.addAll(live.entries());
		}
		int missing = PREVIEW_PLAYERS - entries.size();
		if (missing > 0) {
			entries.addAll(fakeEntries(client, missing, 0, entries.size()));
		}
		Component header = live != null && live.header() != null ? live.header() : Component.translatable("emutils.tab_list.sample.header").withStyle(ChatFormatting.YELLOW);
		Component footer = live != null && live.footer() != null ? live.footer() : Component.translatable("emutils.tab_list.sample.footer").withStyle(ChatFormatting.GRAY);
		return new TabListData(entries, header, footer, live != null && live.scores(), live != null && live.hearts(), true);
	}

	/**
	 * Made-up players for the layout editor and the UI snapshots. The first one when {@code offset} is 0 is
	 * this player. {@code scoreMode} is 0 for no scores, 1 for numbers and 2 for hearts.
	 */
	private static List<TabListData.Entry> fakeEntries(Minecraft client, int count, int scoreMode, int offset) {
		Identifier face = client.player == null ? null : client.player.getSkin().body().texturePath();
		List<TabListData.Entry> entries = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			int index = offset + i;
			boolean self = index == 0 && offset == 0;
			String name = self && client.player != null ? client.player.getScoreboardName() : SAMPLE_NAMES[index % SAMPLE_NAMES.length];
			boolean spectator = index % 7 == 6;
			ChatFormatting color = switch (index % 5) {
				case 1 -> ChatFormatting.AQUA;
				case 2 -> ChatFormatting.GREEN;
				case 3 -> ChatFormatting.GOLD;
				default -> ChatFormatting.WHITE;
			};
			Component display = Component.literal(name).withStyle(color);
			if (spectator) {
				display = display.copy().withStyle(ChatFormatting.ITALIC);
			}
			int score = scoreMode == 2 ? 20 - (index * 3) % 21 : Math.max(0, 100 - index * 4);
			Component scoreText = scoreMode == 1 ? Component.literal(Integer.toString(score)).withStyle(ChatFormatting.YELLOW) : null;
			entries.add(new TabListData.Entry(
				UUID.nameUUIDFromBytes(("emutils-sample-" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				name,
				display,
				spectator,
				SAMPLE_PINGS[index % SAMPLE_PINGS.length],
				0,
				"",
				face,
				true,
				false,
				score,
				scoreText,
				self
			));
		}
		return entries;
	}

	/** Puts made-up players in the list, for UI snapshots. {@code scoreMode}: 0 none, 1 numbers, 2 hearts. */
	public static void showFakePlayersForSnapshot(Minecraft client, int count, int scoreMode, boolean headerAndFooter) {
		Component header = headerAndFooter
			? Component.literal("EMUtils Network").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD).append(Component.literal("\nplay.emutils.example").withStyle(ChatFormatting.YELLOW))
			: null;
		Component footer = headerAndFooter ? Component.literal("Rank: ").withStyle(ChatFormatting.GRAY).append(Component.literal("VIP+").withStyle(ChatFormatting.GREEN)) : null;
		snapshotData = new TabListData(fakeEntries(client, count, scoreMode, 0), header, footer, scoreMode != 0, scoreMode == 2, true);
	}

	public static void clearFakePlayersForSnapshot() {
		snapshotData = null;
	}

	/** The players in the order the list shows them, as names joined by commas, for UI snapshot checks. */
	public static String orderForSnapshot(Minecraft client, EMUtilsConfig config) {
		return shown(client, config).entries().stream().map(TabListData.Entry::profileName).collect(Collectors.joining(","));
	}

	/** The players' pings in the order the list shows them, joined by commas, for UI snapshot checks. */
	public static String pingsForSnapshot(Minecraft client, EMUtilsConfig config) {
		return shown(client, config).entries().stream().map(entry -> Integer.toString(entry.latency())).collect(Collectors.joining(","));
	}

	/** The list's columns and rows, as {@code columns x rows}, for UI snapshot checks. */
	public static String gridForSnapshot(Minecraft client, EMUtilsConfig config) {
		Layout layout = layout(client, config, shown(client, config), maxContentWidth(client, HudLayoutManager.layoutScale(EMUtilsHudElements.TAB_LIST, config)));
		return layout.columns() + "x" + layout.rows();
	}

	/** The card's width, for UI snapshot checks. */
	public static int widthForSnapshot(Minecraft client, EMUtilsConfig config) {
		return layout(client, config, shown(client, config), maxContentWidth(client, HudLayoutManager.layoutScale(EMUtilsHudElements.TAB_LIST, config))).width();
	}

	// ---- Layout --------------------------------------------------------------------------------

	private static int maxContentWidth(Minecraft client, int scalePercent) {
		int screenWidth = client.getWindow().getGuiScaledWidth();
		return Math.max(MIN_CONTENT_WIDTH, screenWidth * 100 / Math.max(1, scalePercent) - SCREEN_MARGIN);
	}

	private static Layout layout(Minecraft client, EMUtilsConfig config, TabListData data, int maxContent) {
		boolean vanilla = config.tabListStyle() == HudStyle.VANILLA;
		ServerText text = new ServerText(client.font, config.tabListFont());
		TabListPing pingMode = config.tabListPing();
		List<TabListData.Entry> entries = data.entries();
		int lineHeight = text.rowHeight(vanilla);
		int rowHeight = Math.max(config.tabListRowHeight(), lineHeight);
		int headSize = Math.max(8, vanilla ? rowHeight - 1 : rowHeight - 2);
		boolean heads = config.tabListHeads() && data.online();
		int headSlot = heads ? headSize + 1 : 0;
		int cellPadding = vanilla ? 0 : CARD_CELL_PADDING;
		int pingGap = vanilla ? 2 : 8;
		int rightMargin = vanilla ? 1 : 0;
		int paddingX = vanilla ? 1 : HudOverlayRenderer.PADDING_X;
		int columnGap = vanilla ? VANILLA_COLUMN_GAP : CARD_COLUMN_GAP;
		int count = entries.size();

		List<ServerText.Prepared> names = new ArrayList<>(count);
		List<ServerText.Prepared> scores = new ArrayList<>(count);
		List<ServerText.Prepared> pings = new ArrayList<>(count);
		int space = text.width(" ");
		int maxName = 0;
		int extra = 0;
		int pingNumberWidth = 0;
		for (TabListData.Entry entry : entries) {
			ServerText.Prepared name = text.prepare(entry.name(), false, maxContent);
			names.add(name);
			maxName = Math.max(maxName, name.width());
			ServerText.Prepared score = text.prepare(data.scores() && !data.hearts() && entry.scoreText() != null ? entry.scoreText() : Component.empty(), false, maxContent);
			scores.add(score);
			extra = Math.max(extra, score.width() > 0 ? space + score.width() : 0);
			ServerText.Prepared ping = text.prepare(Component.literal(entry.latency() < 0 ? "?" : entry.latency() + "ms"), false, maxContent);
			pings.add(ping);
			pingNumberWidth = Math.max(pingNumberWidth, ping.width());
		}
		if (data.scores() && data.hearts()) {
			extra = HEARTS_WIDTH;
		}
		int pingWidth = switch (pingMode) {
			case BARS -> PING_ICON_WIDTH;
			case NUMBER -> pingNumberWidth;
			case BOTH -> pingNumberWidth + PING_NUMBER_GAP + PING_ICON_WIDTH;
		};

		// As many columns as it takes to keep the rows at vanilla's 20, or as many as the setting allows.
		int limit = config.tabListMaxColumns();
		int rows = count;
		int columns = 1;
		while (rows > PlayerTabOverlay.MAX_ROWS_PER_COL && (limit == 0 || columns < limit)) {
			columns++;
			rows = (count + columns - 1) / columns;
		}
		int cell = cellPadding * 2 + headSlot + maxName + extra + pingGap + pingWidth + rightMargin;
		int available = maxContent - (columns - 1) * columnGap;
		int columnWidth = Math.min(cell * columns, available) / columns;
		int squeeze = Math.max(0, cell - columnWidth);
		if (squeeze > 0) {
			// Too wide for the screen: names give way, and are cut with "...".
			int nameLimit = Math.max(8, maxName - squeeze);
			maxName = 0;
			for (int i = 0; i < count; i++) {
				ServerText.Prepared name = text.prepare(entries.get(i).name(), false, nameLimit);
				names.set(i, name);
				maxName = Math.max(maxName, name.width());
			}
		}
		int gridWidth = columnWidth * columns + (columns - 1) * columnGap;

		List<ServerText.Prepared> header = data.header() == null ? List.of() : text.lines(data.header(), maxContent);
		List<ServerText.Prepared> footer = data.footer() == null ? List.of() : text.lines(data.footer(), maxContent);
		int contentWidth = gridWidth;
		for (ServerText.Prepared line : header) {
			contentWidth = Math.max(contentWidth, line.width());
		}
		for (ServerText.Prepared line : footer) {
			contentWidth = Math.max(contentWidth, line.width());
		}

		int headerTop;
		int mainTop = 0;
		int rowsTop;
		int footerTop = 0;
		int height;
		if (vanilla) {
			// Three panels: a pixel of room above the header's text, the rows a pixel inside their panel, and
			// the panels touching, as vanilla draws them.
			headerTop = 1;
			mainTop = header.isEmpty() ? 0 : 1 + header.size() * lineHeight - 1;
			rowsTop = mainTop + 1;
			int mainBottom = rowsTop + rows * rowHeight;
			footerTop = mainBottom + 1;
			height = footer.isEmpty() ? mainBottom : footerTop + footer.size() * lineHeight;
		} else {
			int y = HudOverlayRenderer.PADDING_Y;
			headerTop = y;
			if (!header.isEmpty()) {
				y += header.size() * lineHeight + SECTION_GAP;
			}
			rowsTop = y;
			y += rows * rowHeight;
			if (!footer.isEmpty()) {
				y += SECTION_GAP;
				footerTop = y;
				y += footer.size() * lineHeight;
			}
			height = y + HudOverlayRenderer.PADDING_Y;
		}
		return new Layout(
			text, vanilla, pingMode, heads, lineHeight, rowHeight, headSize, headSlot, cellPadding, pingGap, rightMargin,
			columns, rows, columnWidth, columnGap, maxName, extra, pingNumberWidth,
			header, footer, names, scores, pings,
			paddingX, contentWidth, gridWidth, contentWidth + paddingX * 2, height, headerTop, mainTop, rowsTop, footerTop
		);
	}

	/** The slot's size: the card's height, and its width or {@link #SLOT_WIDTH}, whichever is more. */
	static HudOverlayPlacement.PanelDimensions slotDimensions(Minecraft client, EMUtilsConfig config) {
		Layout layout = layout(client, config, shown(client, config), maxContentWidth(client, HudLayoutManager.layoutScale(EMUtilsHudElements.TAB_LIST, config)));
		return new HudOverlayPlacement.PanelDimensions(Math.max(SLOT_WIDTH, layout.width()), layout.height());
	}

	static HudOverlayPlacement.Position defaultPosition(int screenWidth, HudOverlayPlacement.PanelDimensions dimensions) {
		return new HudOverlayPlacement.Position((screenWidth - dimensions.width()) / 2, DEFAULT_TOP);
	}

	/**
	 * Draws the card inside its slot, which is at ({@code slotX}, {@code slotY}) in the current transform
	 * and at {@code screenX} with width {@code screenWidth} on screen. The card lines up with the slot by
	 * the slot's third of the screen.
	 */
	static void renderInSlot(
		GuiGraphicsExtractor context,
		Minecraft client,
		EMUtilsConfig config,
		TabListData data,
		int slotX,
		int slotY,
		int screenX,
		int screenWidth,
		int opacityPercent
	) {
		int scale = HudLayoutManager.layoutScale(EMUtilsHudElements.TAB_LIST, config);
		Layout layout = layout(client, config, data, maxContentWidth(client, scale));
		int slotWidth = Math.max(SLOT_WIDTH, layout.width());
		int center = screenX + screenWidth / 2;
		int guiWidth = context.guiWidth();
		int offsetX;
		if (center < guiWidth / 3) {
			offsetX = 0;
		} else if (center > guiWidth * 2 / 3) {
			offsetX = slotWidth - layout.width();
		} else {
			offsetX = (slotWidth - layout.width()) / 2;
		}
		draw(context, client, config, layout, data, slotX + offsetX, slotY, opacityPercent);
	}

	// ---- Drawing -------------------------------------------------------------------------------

	private static void draw(GuiGraphicsExtractor context, Minecraft client, EMUtilsConfig config, Layout layout, TabListData data, int x, int y, int opacityPercent) {
		UiTheme theme = UiTheme.current();
		float opacity = Math.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
		boolean shadow = config.tabListTextShadow().shadow(opacityPercent, HudOverlayRenderer.TEXT_SHADOW_BELOW_OPACITY);
		boolean vanilla = layout.vanilla();
		int textColor = vanilla ? 0xFFFFFFFF : theme.text();
		int spectatorColor = vanilla ? SPECTATOR_COLOR : UiTheme.fade(theme.text(), 0.57F);
		int lineHeight = layout.lineHeight();
		int rowHeight = layout.rowHeight();
		List<TabListData.Entry> entries = data.entries();

		if (vanilla) {
			int panel = UiTheme.fade(PANEL_COLOR, opacity);
			if (!layout.header().isEmpty()) {
				context.fill(x, y, x + layout.width(), y + 1 + layout.header().size() * lineHeight, panel);
			}
			context.fill(x, y + layout.mainTop(), x + layout.width(), y + layout.rowsTop() + layout.rows() * rowHeight, panel);
			if (!layout.footer().isEmpty()) {
				context.fill(x, y + layout.rowsTop() + layout.rows() * rowHeight, x + layout.width(), y + layout.height(), panel);
			}
		} else {
			HudOverlayRenderer.drawCard(context, theme, x, y, layout.width(), layout.height(), opacityPercent);
			int lineColor = UiOpacity.apply(UiTheme.fade(theme.line(), opacity));
			if (!layout.header().isEmpty()) {
				int dividerY = layout.rowsTop() - SECTION_GAP / 2 - 1;
				context.fill(x + layout.paddingX(), y + dividerY, x + layout.width() - layout.paddingX(), y + dividerY + 1, lineColor);
			}
			if (!layout.footer().isEmpty()) {
				int dividerY = layout.footerTop() - SECTION_GAP / 2 - 1;
				context.fill(x + layout.paddingX(), y + dividerY, x + layout.width() - layout.paddingX(), y + dividerY + 1, lineColor);
			}
		}

		for (int i = 0; i < layout.header().size(); i++) {
			ServerText.Prepared line = layout.header().get(i);
			line.draw(context, x + layout.paddingX() + (layout.contentWidth() - line.width()) / 2, y + layout.headerTop() + i * lineHeight, lineHeight, textColor, shadow);
		}
		for (int i = 0; i < layout.footer().size(); i++) {
			ServerText.Prepared line = layout.footer().get(i);
			line.draw(context, x + layout.paddingX() + (layout.contentWidth() - line.width()) / 2, y + layout.footerTop() + i * lineHeight, lineHeight, textColor, shadow);
		}

		int rowBackground = UiTheme.fade(client.options.getBackgroundColor(ROW_COLOR), opacity);
		int gridX = x + layout.paddingX() + (layout.contentWidth() - layout.gridWidth()) / 2;
		long guiTicks = client.gui.hud.getGuiTicks();
		Set<UUID> present = entries.stream().map(TabListData.Entry::id).collect(Collectors.toSet());
		HEALTH_STATES.keySet().removeIf(id -> !present.contains(id));
		for (int i = 0; i < layout.rows() * layout.columns(); i++) {
			int column = i / layout.rows();
			int row = i % layout.rows();
			int cellX = gridX + column * (layout.columnWidth() + layout.columnGap());
			int cellY = y + layout.rowsTop() + row * rowHeight;
			if (vanilla) {
				context.fill(cellX, cellY, cellX + layout.columnWidth(), cellY + rowHeight - 1, rowBackground);
			}
			if (i >= entries.size()) {
				continue;
			}
			TabListData.Entry entry = entries.get(i);
			if (entry.self() && config.tabListHighlightSelf()) {
				int highlight = UiTheme.fade(theme.accent(), 0.30F);
				if (vanilla) {
					context.fill(cellX, cellY, cellX + layout.columnWidth(), cellY + rowHeight - 1, highlight);
				} else {
					UiShapes.roundedRect(context, cellX, cellY, layout.columnWidth(), rowHeight, 4, highlight);
				}
			}
			int contentX = cellX + layout.cellPadding();
			if (layout.heads() && entry.face() != null) {
				PlayerFaceExtractor.extractRenderState(context, entry.face(), contentX, cellY + (rowHeight - layout.headSize()) / 2, layout.headSize(), entry.hat(), entry.flipped(), -1);
			}
			int nameX = contentX + layout.headSlot();
			layout.names().get(i).draw(context, nameX, cellY, rowHeight, entry.spectator() ? spectatorColor : textColor, shadow);
			if (data.scores() && !entry.spectator()) {
				int start = nameX + layout.maxName() + 1;
				int end = start + layout.extra();
				if (end - start > 5) {
					if (data.hearts()) {
						drawHearts(context, client, layout, cellY, start, end, entry, guiTicks, textColor, shadow);
					} else if (layout.scores().get(i).width() > 0) {
						ServerText.Prepared score = layout.scores().get(i);
						score.draw(context, end - score.width(), cellY, rowHeight, textColor, shadow);
					}
				}
			}
			drawPing(context, theme, config, layout, entry, layout.pings().get(i), cellX + layout.columnWidth() - layout.cellPadding() - layout.rightMargin(), cellY, vanilla ? 0xFFAAAAAA : theme.textSecondary(), shadow);
		}
	}

	private static void drawPing(
		GuiGraphicsExtractor context,
		UiTheme theme,
		EMUtilsConfig config,
		Layout layout,
		TabListData.Entry entry,
		ServerText.Prepared number,
		int right,
		int cellY,
		int plainColor,
		boolean shadow
	) {
		int latency = entry.latency();
		int numberRight = right;
		if (layout.ping().bars()) {
			Identifier sprite;
			if (latency < 0) {
				sprite = PING_UNKNOWN_SPRITE;
			} else if (latency < 150) {
				sprite = PING_5_SPRITE;
			} else if (latency < 300) {
				sprite = PING_4_SPRITE;
			} else if (latency < 600) {
				sprite = PING_3_SPRITE;
			} else if (latency < 1000) {
				sprite = PING_2_SPRITE;
			} else {
				sprite = PING_1_SPRITE;
			}
			context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, right - PING_ICON_WIDTH, cellY + (layout.rowHeight() - PING_ICON_HEIGHT) / 2, PING_ICON_WIDTH, PING_ICON_HEIGHT);
			numberRight = right - PING_ICON_WIDTH - PING_NUMBER_GAP;
		}
		if (layout.ping().number()) {
			int color = plainColor;
			if (config.tabListPingColors() && latency >= 0) {
				color = latency < GOOD_PING ? theme.hud() : latency < SLOW_PING ? theme.utility() : theme.warning();
			}
			number.draw(context, numberRight - number.width(), cellY, layout.rowHeight(), color, shadow);
		}
	}

	/** The list objective's hearts, or its value as text when there's no room, as vanilla draws them. */
	private static void drawHearts(GuiGraphicsExtractor context, Minecraft client, Layout layout, int y, int start, int end, TabListData.Entry entry, long guiTicks, int textColor, boolean shadow) {
		int score = entry.score();
		HealthState state = HEALTH_STATES.computeIfAbsent(entry.id(), id -> new HealthState(score));
		state.update(score, guiTicks);
		int filled = Mth.positiveCeilDiv(Math.max(score, state.displayedValue()), 2);
		int total = Math.max(score, Math.max(state.displayedValue(), 20)) / 2;
		boolean blinking = state.isBlinking(guiTicks);
		if (filled <= 0) {
			return;
		}
		int spacing = Mth.floor(Math.min((float) (end - start - 4) / (float) total, 9.0F));
		int rowY = y + (layout.rowHeight() - 9) / 2;
		if (spacing <= 3) {
			float ratio = Mth.clamp(score / 20.0F, 0.0F, 1.0F);
			int color = ((int) ((1.0F - ratio) * 255.0F) << 16) | ((int) (ratio * 255.0F) << 8);
			float hp = score / 2.0F;
			Component full = Component.translatable("multiplayer.player.list.hp", hp);
			Component shown = end - client.font.width(full) >= start ? full : Component.literal(Float.toString(hp));
			ServerText.Prepared text = layout.text().prepare(shown, false, Integer.MAX_VALUE);
			text.draw(context, (end + start - text.width()) / 2, y, layout.rowHeight(), ARGB.opaque(color), shadow);
			return;
		}
		Identifier container = blinking ? HEART_CONTAINER_BLINKING_SPRITE : HEART_CONTAINER_SPRITE;
		for (int i = filled; i < total; i++) {
			heart(context, container, start + i * spacing, rowY);
		}
		for (int i = 0; i < filled; i++) {
			int x = start + i * spacing;
			heart(context, container, x, rowY);
			if (blinking) {
				if (i * 2 + 1 < state.displayedValue()) {
					heart(context, HEART_FULL_BLINKING_SPRITE, x, rowY);
				}
				if (i * 2 + 1 == state.displayedValue()) {
					heart(context, HEART_HALF_BLINKING_SPRITE, x, rowY);
				}
			}
			if (i * 2 + 1 < score) {
				heart(context, i >= 10 ? HEART_ABSORBING_FULL_BLINKING_SPRITE : HEART_FULL_SPRITE, x, rowY);
			}
			if (i * 2 + 1 == score) {
				heart(context, i >= 10 ? HEART_ABSORBING_HALF_BLINKING_SPRITE : HEART_HALF_SPRITE, x, rowY);
			}
		}
	}

	private static void heart(GuiGraphicsExtractor context, Identifier sprite, int x, int y) {
		context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9);
	}

	/**
	 * Draws the tab list in place of vanilla's while Tab is held. Returns {@code false} to leave it to
	 * vanilla, which is when the custom tab list is turned off.
	 */
	private static boolean render(GuiGraphicsExtractor context) {
		EMUtilsConfig config = EMUtilsClient.config();
		Minecraft client = Minecraft.getInstance();
		if (config == null || !config.tabList()) {
			return false;
		}
		// The layout editor draws its own preview.
		if (client == null || client.player == null || client.level == null || HudLayoutManager.isEditing()) {
			return true;
		}
		// Vanilla's rule for showing the list: Tab is held, and there's someone else to list, or a list objective.
		Objective objective = client.level.getScoreboard().getDisplayObjective(DisplaySlot.LIST);
		boolean forced = snapshotData != null;
		boolean show = forced || (client.options.keyPlayerList.isDown()
			&& (!client.isLocalServer() || client.player.connection.getListedOnlinePlayers().size() > 1 || objective != null));
		if (!forced) {
			// Vanilla's list also announces itself to the narrator when it becomes visible.
			client.gui.hud.getTabList().setVisible(show);
		}
		if (!show) {
			HEALTH_STATES.clear();
			return true;
		}
		if (MinecraftClientCompat.isHudHidden(client)) {
			return true;
		}
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return true;
		}

		TabListData data = shown(client, config);
		HudLayoutManager.ResolvedLayout layout = HudLayoutManager.resolveLayout(
			EMUtilsHudElements.TAB_LIST,
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
			renderInSlot(context, client, config, data, 0, 0, layout.position().x(), layout.dimensions().width(), layout.opacityPercent());
		} finally {
			UiRasterScale.reset();
			context.pose().popMatrix();
		}
		return true;
	}
}
