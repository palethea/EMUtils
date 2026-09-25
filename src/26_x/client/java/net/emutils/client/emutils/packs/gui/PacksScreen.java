package net.emutils.client.emutils.packs.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.IrisCompat;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.packs.InstalledPack;
import net.emutils.client.emutils.packs.InstalledPackIndex;
import net.emutils.client.emutils.packs.InstalledPackScanner;
import net.emutils.client.emutils.packs.PackOperationResult;
import net.emutils.client.emutils.packs.PackType;
import net.emutils.client.emutils.packs.ResourcePackController;
import net.emutils.client.emutils.packs.modrinth.ModrinthClient;
import net.emutils.client.emutils.packs.modrinth.ModrinthSearchResult;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.versioned.VersionedPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * The Pack Manager in the new UI (#110): installed resource and shader packs, and Modrinth search to find
 * and download more. Results animate in one after another, search runs as you type, and only the newest
 * search's results are ever shown.
 */
public final class PacksScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON = 20;
	private static final int SEARCH_HEIGHT = 22;
	private static final int ROW_HEIGHT = 60;
	private static final int ROW_GAP = 8;
	private static final int ROW_RADIUS = 9;
	private static final int ICON = 40;
	private static final int BUTTON_HEIGHT = 20;
	private static final int FADE_HEIGHT = 12;
	private static final int POLL_TICKS = 40;
	/** A pause in typing this long starts a search. */
	private static final long SEARCH_DELAY_NANOS = 300_000_000L;
	/** Each new row fades and rises in over this long, a little after the one above it. */
	private static final long APPEAR_NANOS = 180_000_000L;
	private static final long STAGGER_NANOS = 30_000_000L;
	private static final int MAX_STAGGERED = 12;
	private static final long ICON_FADE_NANOS = 160_000_000L;
	/** Replaced results fade out this quickly while the new ones start coming in. */
	private static final long LEAVE_NANOS = 120_000_000L;
	private static final long ENTER_DELAY_NANOS = 60_000_000L;

	/** A few threads, so a slow search never holds up the newest one. */
	private static final ExecutorService WORKERS = Executors.newFixedThreadPool(3, runnable -> {
		Thread thread = new Thread(runnable, "EMUtils Pack Manager");
		thread.setDaemon(true);
		return thread;
	});

	private enum View {
		INSTALLED,
		MODRINTH
	}

	private final ModrinthClient modrinth = new ModrinthClient();
	private final InstalledPackIndex index = InstalledPackIndex.load();
	private final UiTextField search = new UiTextField(this, 64);
	private final UiScrollArea scroll = new UiScrollArea();
	private final List<RowBox> rows = new ArrayList<>();
	private final Map<String, Long> rowAppear = new HashMap<>();
	private final Map<String, Long> iconAppear = new HashMap<>();
	private final Set<String> downloading = new HashSet<>();
	private @Nullable PackIconLoader icons;
	private PackType type = PackType.RESOURCE;
	private View view = View.INSTALLED;
	private List<InstalledPack> installed = List.of();
	private List<ModrinthSearchResult> results = List.of();
	/** The rows a new search replaced, fading out where they were. */
	private List<Row> leaving = List.of();
	private long leavingStart;
	private @Nullable PackType resultsType;
	private String resultsQuery = "";
	private boolean moreAvailable;
	private boolean loading;
	private boolean loadingMore;
	private @Nullable Component error;
	private int requestId;
	private long searchAt;
	private int pollTicks;
	private @Nullable UiConfirmDialog dialog;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;
	private int headerButtonsY;
	private int folderX;
	private int typeX;
	private int typeY;
	private int @Nullable [] typeEdges;
	private int toolbarY;
	private int viewX;
	private int @Nullable [] viewEdges;
	private int searchX;
	private int searchWidth;
	private int retryX;
	private int retryY;
	private int retryWidth;

	public PacksScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_PACK_MANAGER), parent);
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON) / 2;
		toolbarY = panelY + PADDING + headerHeight + 12;
		int bodyY = toolbarY + SEARCH_HEIGHT + 10;
		scroll.setBounds(panelX + PADDING, bodyY, panelWidth - PADDING * 2 + UiScrollArea.GUTTER, panelY + panelHeight - PADDING / 2 - bodyY);
		search.restoreFocus();
		if (installed.isEmpty()) {
			refreshInstalled(true);
		}
	}

	private PackIconLoader icons() {
		if (icons == null) {
			icons = new PackIconLoader(minecraft, () -> {
			});
		}
		return icons;
	}

	@Override
	protected void dispose() {
		if (icons != null) {
			icons.close();
			icons = null;
			iconAppear.clear();
		}
	}

	private boolean shadersAvailable() {
		return IrisCompat.isIrisLoaded() || EMUtilsClient.config().packManagerShowShadersWithoutIris();
	}

	// ---- loading --------------------------------------------------------------------------------

	/** Scans the pack folder in the background; the list only changes when the folder did. */
	private void refreshInstalled(boolean animate) {
		PackType scanned = type;
		CompletableFuture.supplyAsync(() -> {
			try {
				return InstalledPackScanner.scan(minecraft, scanned, index).stream().sorted(Comparator.comparing(InstalledPack::filename)).toList();
			} catch (IOException exception) {
				throw new RuntimeException(exception);
			}
		}, WORKERS).whenComplete((packs, failure) -> minecraft.execute(() -> {
			if (failure != null || scanned != type || packs.equals(installed)) {
				return;
			}
			installed = packs;
			if (animate && view == View.INSTALLED) {
				animateIn(installedRows().stream().map(Row::key).toList());
			}
		}));
	}

	/** Starts a Modrinth search for what's in the search field, or loads the next page of the current one. */
	private void loadModrinth(boolean more) {
		if (!EMUtilsClient.config().packManagerEnabled()) {
			error = Component.translatable(EMUtilsTexts.UI_PACK_MANAGER_OFF);
			return;
		}
		if (more && (loading || loadingMore || !moreAvailable)) {
			return;
		}
		searchAt = 0L;
		String query = search.text().trim();
		PackType searched = type;
		int id = more ? requestId : ++requestId;
		int offset = more ? results.size() : 0;
		int limit = EMUtilsClient.config().packManagerSearchLimit();
		if (more) {
			loadingMore = true;
		} else {
			loading = true;
			error = null;
		}
		CompletableFuture.supplyAsync(() -> {
			try {
				return modrinth.search(searched, query, SharedConstants.getCurrentVersion().name(), limit, offset);
			} catch (IOException | InterruptedException exception) {
				if (exception instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				throw new RuntimeException(exception);
			}
		}, WORKERS).whenComplete((page, failure) -> minecraft.execute(() -> {
			// Only the newest search counts, so typing fast never shows stale results.
			if (id != requestId) {
				return;
			}
			loadingMore = false;
			loading = false;
			if (failure != null) {
				Throwable cause = failure.getCause() == null ? failure : failure.getCause();
				Throwable root = cause.getCause() == null ? cause : cause.getCause();
				error = Component.translatable(EMUtilsTexts.PACK_ERROR, root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage());
				return;
			}
			if (!more && view == View.MODRINTH) {
				leaving = modrinthRows();
				leavingStart = System.nanoTime();
			}
			List<ModrinthSearchResult> merged = new ArrayList<>(more ? results : List.of());
			Set<String> known = new HashSet<>();
			merged.forEach(result -> known.add(result.projectId()));
			List<String> fresh = new ArrayList<>();
			for (ModrinthSearchResult result : page) {
				if (known.add(result.projectId())) {
					merged.add(result);
					fresh.add("modrinth:" + result.projectId());
				}
			}
			results = merged;
			resultsType = searched;
			resultsQuery = query;
			moreAvailable = page.size() >= limit;
			if (!more) {
				rowAppear.clear();
				scroll.reset();
			}
			animateIn(fresh, more || leaving.isEmpty() ? 0L : ENTER_DELAY_NANOS);
		}));
	}

	/** Schedules rows to fade and rise in one after another, starting now. */
	private void animateIn(List<String> keys) {
		animateIn(keys, 0L);
	}

	private void animateIn(List<String> keys, long delayNanos) {
		long start = System.nanoTime() + delayNanos;
		for (int i = 0; i < keys.size(); i++) {
			rowAppear.put(keys.get(i), start + Math.min(i, MAX_STAGGERED) * STAGGER_NANOS);
		}
	}

	@Override
	protected void beforeFrame() {
		if (searchAt != 0L && System.nanoTime() >= searchAt && view == View.MODRINTH) {
			loadModrinth(false);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (++pollTicks >= POLL_TICKS) {
			pollTicks = 0;
			refreshInstalled(false);
		}
	}

	private void searchChanged() {
		scroll.reset();
		if (view == View.MODRINTH) {
			searchAt = System.nanoTime() + SEARCH_DELAY_NANOS;
		}
	}

	private void switchType(PackType next) {
		if (type == next || (next == PackType.SHADER && !shadersAvailable())) {
			return;
		}
		type = next;
		installed = List.of();
		results = List.of();
		resultsType = null;
		error = null;
		requestId++;
		loading = false;
		loadingMore = false;
		rowAppear.clear();
		scroll.reset();
		refreshInstalled(true);
		if (view == View.MODRINTH) {
			loadModrinth(false);
		}
	}

	private void switchView(View next) {
		if (view == next) {
			return;
		}
		view = next;
		rowAppear.clear();
		scroll.reset();
		if (view == View.MODRINTH) {
			if (resultsType != type || !resultsQuery.equals(search.text().trim())) {
				loadModrinth(false);
			} else {
				animateIn(modrinthRows().stream().map(Row::key).toList());
			}
		} else {
			searchAt = 0L;
			animateIn(installedRows().stream().map(Row::key).toList());
		}
	}

	// ---- rows -----------------------------------------------------------------------------------

	private record Row(String key, String title, String description, String meta, @Nullable String iconUrl, @Nullable ModrinthSearchResult result, @Nullable InstalledPack installed) {
	}

	private List<Row> installedRows() {
		String filter = search.text().trim().toLowerCase(Locale.ROOT);
		List<Row> list = new ArrayList<>();
		for (InstalledPack pack : installed) {
			PackListItem item = PackListItem.installed(pack);
			if (!filter.isEmpty() && !item.title().toLowerCase(Locale.ROOT).contains(filter) && !pack.filename().toLowerCase(Locale.ROOT).contains(filter)) {
				continue;
			}
			// The description already says whether it's a local pack, so the second line names the file.
			String meta = pack.filename();
			list.add(new Row("installed:" + pack.filename(), item.title(), item.description(), meta, item.iconUrl(), null, pack));
		}
		return list;
	}

	private List<Row> modrinthRows() {
		List<Row> list = new ArrayList<>();
		for (ModrinthSearchResult result : results) {
			InstalledPack match = null;
			for (InstalledPack pack : installed) {
				if (pack.record() != null && pack.record().matches(type, result.projectId())) {
					match = pack;
				}
			}
			String author = result.author() == null ? "" : result.author();
			String meta = Component.translatable(EMUtilsTexts.UI_PACK_META, author, compact(result.downloads())).getString();
			list.add(new Row("modrinth:" + result.projectId(), result.displayTitle(), result.description() == null ? "" : result.description(), meta, result.iconUrl(), result, match));
		}
		return list;
	}

	/** 1234 as "1.2K", 1234567 as "1.2M". */
	private static String compact(int value) {
		if (value >= 1_000_000) {
			return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0).replace(".0M", "M");
		}
		if (value >= 1_000) {
			return String.format(Locale.ROOT, "%.1fK", value / 1_000.0).replace(".0K", "K");
		}
		return Integer.toString(value);
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = dialog == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		List<Row> list = view == View.INSTALLED ? installedRows() : modrinthRows();
		drawHeader(context, theme, list.size(), hoverX, hoverY);
		drawToolbar(context, theme, hoverX, hoverY);
		drawRows(context, theme, list, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int count, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		Component subtitle;
		if (view == View.INSTALLED) {
			subtitle = Component.translatable(EMUtilsTexts.UI_PACK_INSTALLED_COUNT, count);
		} else if (resultsQuery.isEmpty()) {
			subtitle = Component.translatable(EMUtilsTexts.UI_PACK_MOST_DOWNLOADED, SharedConstants.getCurrentVersion().name());
		} else {
			subtitle = Component.translatable(EMUtilsTexts.UI_PACK_RESULTS_FOR, resultsQuery);
		}
		UiText.draw(context, font, UiText.ellipsize(font, subtitle, UiText.Size.BODY, panelWidth / 2), UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, theme.muted());

		folderX = panelX + panelWidth - PADDING - HEADER_BUTTON;
		boolean folderHovered = contains(mouseX, mouseY, folderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON);
		UiWidgets.iconButton(context, theme, folderX, headerButtonsY, HEADER_BUTTON, HubIcons.FOLDER, folderHovered ? 1.0F : 0.0F);
		if (folderHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_PACK_OPEN_FOLDER), mouseX, mouseY);
		}

		List<Component> labels = List.of(Component.translatable(EMUtilsTexts.PACK_TAB_RESOURCE_PACKS), Component.translatable(EMUtilsTexts.PACK_TAB_SHADER_PACKS));
		typeX = folderX - 8 - UiWidgets.segmentedWidth(font, labels);
		typeY = headerButtonsY + (HEADER_BUTTON - UiWidgets.SEGMENT_HEIGHT) / 2;
		int[] edges = UiWidgets.segmentEdges(font, typeX, labels);
		int hovered = -1;
		for (int i = 0; i < 2; i++) {
			if (contains(mouseX, mouseY, edges[i], typeY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
				hovered = i;
			}
		}
		if (hovered == 1 && !shadersAvailable()) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_NEEDS_MOD, "Iris"), mouseX, mouseY);
			hovered = -1;
		}
		typeEdges = UiWidgets.segmented(context, font, theme, typeX, typeY, labels, anim.transition("pack-type", type == PackType.SHADER ? 1.0F : 0.0F, 0.18F), hovered);
	}

	private void drawToolbar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int right = panelX + panelWidth - PADDING;
		List<Component> labels = List.of(Component.translatable(EMUtilsTexts.PACK_TAB_INSTALLED), Component.translatable(EMUtilsTexts.PACK_TAB_MODRINTH));
		viewX = left;
		int viewY = toolbarY + (SEARCH_HEIGHT - UiWidgets.SEGMENT_HEIGHT) / 2;
		int[] edges = UiWidgets.segmentEdges(font, viewX, labels);
		int hovered = -1;
		for (int i = 0; i < 2; i++) {
			if (contains(mouseX, mouseY, edges[i], viewY, edges[i + 1] - edges[i], UiWidgets.SEGMENT_HEIGHT)) {
				hovered = i;
			}
		}
		viewEdges = UiWidgets.segmented(context, font, theme, viewX, viewY, labels, anim.transition("pack-view", view == View.MODRINTH ? 1.0F : 0.0F, 0.18F), hovered);

		searchX = edges[2] + 10;
		searchWidth = right - searchX;
		UiShapes.borderedRect(context, searchX, toolbarY, searchWidth, SEARCH_HEIGHT, 8, theme.surface(), search.focused() ? theme.accent() : theme.line());
		int center = toolbarY + SEARCH_HEIGHT / 2;
		UiIcons.draw(context, HubIcons.SEARCH, searchX + 8, center - 5, 10, theme.textSecondary());
		Component placeholder = Component.translatable(view == View.MODRINTH ? EMUtilsTexts.UI_PACK_SEARCH_MODRINTH : EMUtilsTexts.UI_PACK_FILTER_INSTALLED);
		search.draw(context, font, theme, searchX + 24, center, searchWidth - 32, placeholder);

		// While a search runs, a short bar slides along the bottom of the field.
		float busy = anim.towards("pack-busy", loading ? 1.0F : 0.0F, 14.0F);
		if (busy > 0.01F) {
			int barWidth = Math.max(24, searchWidth / 4);
			double phase = (System.nanoTime() / 900_000_000.0) % 1.0;
			float barX = searchX + 6 + (float) (phase * (searchWidth - 12 + barWidth)) - barWidth;
			context.enableScissor(searchX + 6, toolbarY + SEARCH_HEIGHT - 3, searchX + searchWidth - 6, toolbarY + SEARCH_HEIGHT - 1);
			int whole = (int) Math.floor(barX);
			context.pose().pushMatrix();
			context.pose().translate(barX - whole, 0.0F);
			UiShapes.pill(context, whole, toolbarY + SEARCH_HEIGHT - 3, barWidth, 2, UiTheme.fade(theme.accent(), busy));
			context.pose().popMatrix();
			context.disableScissor();
		}
	}

	private void drawRows(GuiGraphicsExtractor context, UiTheme theme, List<Row> list, int mouseX, int mouseY) {
		boolean showMoreRow = view == View.MODRINTH && loadingMore;
		int count = list.size() + (showMoreRow ? 1 : 0);
		scroll.setContentHeight(count == 0 ? 0 : count * (ROW_HEIGHT + ROW_GAP) - ROW_GAP + FADE_HEIGHT);
		scroll.animate(anim, "packs-scroll", mouseX, mouseY);
		rows.clear();
		boolean mouseInList = scroll.contains(mouseX, mouseY) && !scroll.dragging();
		// The current results dim while a new search loads, instead of the list flashing empty.
		float dim = 1.0F - 0.45F * anim.towards("pack-dim", loading && !list.isEmpty() ? 1.0F : 0.0F, 12.0F);
		long now = System.nanoTime();
		scroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, scroll.offset() - scroll.exactOffset());
		int width = scroll.contentWidth();
		int y = scroll.y() + FADE_HEIGHT / 2 - scroll.offset();
		// Results a new search replaced fade out where they were, under the new ones coming in.
		float leave = leaving.isEmpty() ? 0.0F : 1.0F - Math.clamp((now - leavingStart) / (float) LEAVE_NANOS, 0.0F, 1.0F);
		if (leave <= 0.0F) {
			leaving = List.of();
		} else {
			UiOpacity.set(openProgress() * leave * 0.55F);
			int leavingY = y;
			for (Row row : leaving) {
				if (leavingY > scroll.y() + scroll.height()) {
					break;
				}
				drawRow(context, theme, row, scroll.x(), leavingY, width, Integer.MIN_VALUE / 2, mouseY);
				leavingY += ROW_HEIGHT + ROW_GAP;
			}
		}
		for (Row row : list) {
			if (y + ROW_HEIGHT >= scroll.y() && y <= scroll.y() + scroll.height()) {
				long start = rowAppear.getOrDefault(row.key(), 0L);
				float t = start == 0L ? 1.0F : Math.clamp((now - start) / (float) APPEAR_NANOS, 0.0F, 1.0F);
				float appear = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
				if (appear > 0.0F) {
					UiOpacity.set(openProgress() * appear * dim);
					context.pose().pushMatrix();
					context.pose().translate(0.0F, (1.0F - appear) * 8.0F);
					rows.add(drawRow(context, theme, row, scroll.x(), y, width, mouseInList && appear >= 1.0F ? mouseX : Integer.MIN_VALUE / 2, mouseY));
					context.pose().popMatrix();
				}
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		UiOpacity.set(openProgress());
		if (showMoreRow) {
			drawSpinner(context, theme, scroll.x() + width / 2, y + ROW_HEIGHT / 2);
		}
		context.pose().popMatrix();

		retryWidth = 0;
		if (list.isEmpty()) {
			drawEmpty(context, theme, width, mouseX, mouseY);
		} else if (view == View.MODRINTH && moreAvailable && !loading && !loadingMore && error == null && scroll.offset() >= scroll.maxScroll() - ROW_HEIGHT) {
			// Near the end of the results: load the next page, which animates in below.
			loadModrinth(true);
		}
		scroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private void drawEmpty(GuiGraphicsExtractor context, UiTheme theme, int width, int mouseX, int mouseY) {
		int centerX = scroll.x() + width / 2;
		int top = scroll.y() + Math.max(20, scroll.height() / 2 - 34);
		if (view == View.MODRINTH && (loading || (resultsType != type && error == null))) {
			drawSpinner(context, theme, centerX, top + 20);
			return;
		}
		Component text;
		if (error != null) {
			text = error;
		} else if (view == View.INSTALLED && !search.text().isBlank()) {
			text = Component.translatable(EMUtilsTexts.UI_PACK_NO_MATCH, search.text().trim());
		} else if (view == View.INSTALLED) {
			text = Component.translatable(type == PackType.SHADER ? EMUtilsTexts.PACK_SECTION_INSTALLED_EMPTY_SHADERS : EMUtilsTexts.PACK_SECTION_INSTALLED_EMPTY_PACKS);
		} else {
			text = Component.translatable(type == PackType.SHADER ? EMUtilsTexts.PACK_STATUS_EMPTY_SHADERS : EMUtilsTexts.PACK_STATUS_EMPTY_PACKS);
		}
		int iconSize = 22;
		UiIcons.draw(context, error != null ? HubIcons.CLOUD_OFF : HubIcons.PACKAGE, centerX - iconSize / 2, top, iconSize, theme.muted());
		Component line = UiText.ellipsize(font, text, UiText.Size.BODY, width - 20);
		UiText.draw(context, font, line, UiText.Size.BODY, centerX - UiText.width(font, line, UiText.Size.BODY) / 2, top + iconSize + 10, theme.muted());
		if (error != null && view == View.MODRINTH) {
			Component retry = Component.translatable(EMUtilsTexts.UI_RETRY);
			retryWidth = UiWidgets.buttonWidth(font, retry) + 8;
			retryX = centerX - retryWidth / 2;
			retryY = top + iconSize + 26;
			UiWidgets.button(context, font, theme, retryX, retryY, retryWidth, BUTTON_HEIGHT, retry, UiWidgets.ButtonStyle.SURFACE, contains(mouseX, mouseY, retryX, retryY, retryWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		}
	}

	/** Three dots that pulse one after another. */
	private static void drawSpinner(GuiGraphicsExtractor context, UiTheme theme, int centerX, int centerY) {
		double time = System.nanoTime() / 1_000_000_000.0;
		for (int i = 0; i < 3; i++) {
			float pulse = 0.35F + 0.65F * (float) Math.max(0.0, Math.sin(time * 6.0 - i * 0.7));
			UiShapes.circle(context, centerX - 11 + i * 9, centerY - 2, 4, UiTheme.fade(theme.textSecondary(), pulse));
		}
	}

	private RowBox drawRow(GuiGraphicsExtractor context, UiTheme theme, Row row, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		float hover = anim.towards("pack:" + row.key(), hovered, 16.0F);
		UiShapes.borderedRect(context, x, y, width, ROW_HEIGHT, ROW_RADIUS, UiTheme.mix(theme.surface(), theme.surfaceHover(), hover), theme.border());

		int iconX = x + 10;
		int iconY = y + (ROW_HEIGHT - ICON) / 2;
		drawIcon(context, theme, row, iconX, iconY);

		// Right side: the delete icon for installed packs, and the main action button.
		int right = x + width - 10;
		int deleteX = 0;
		if (row.installed() != null) {
			deleteX = right - BUTTON_HEIGHT;
			boolean deleteHovered = contains(mouseX, mouseY, deleteX, y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2, BUTTON_HEIGHT, BUTTON_HEIGHT);
			UiWidgets.ghostIconButton(context, theme, deleteX, y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2, BUTTON_HEIGHT, HubIcons.TRASH, deleteHovered ? theme.warning() : theme.muted(), deleteHovered ? 1.0F : 0.0F);
			if (deleteHovered) {
				showTooltip(Component.translatable(EMUtilsTexts.PACK_DELETE), mouseX, mouseY);
			}
			right = deleteX - 6;
		}
		Action action = action(row);
		int actionWidth = 0;
		int actionX = 0;
		int actionY = y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
		if (action != null) {
			actionWidth = Math.max(64, UiWidgets.buttonWidth(font, action.label()) + 10);
			actionX = right - actionWidth;
			boolean actionHovered = action.enabled() && contains(mouseX, mouseY, actionX, actionY, actionWidth, BUTTON_HEIGHT);
			UiWidgets.button(context, font, theme, actionX, actionY, actionWidth, BUTTON_HEIGHT, action.label(), action.enabled() ? action.style() : UiWidgets.ButtonStyle.GHOST, actionHovered ? 1.0F : 0.0F);
			right = actionX - 10;
		}

		// Left side: title and status, description, author and downloads.
		int textX = iconX + ICON + 12;
		int textWidth = right - textX;
		Component status = status(row);
		int badgeWidth = status == null ? 0 : UiText.width(font, status, UiText.Size.SMALL) + 8;
		Component name = UiText.ellipsize(font, Component.literal(row.title()), UiText.Size.BOLD, textWidth - (badgeWidth > 0 ? badgeWidth + 6 : 0));
		int titleCenter = y + 16;
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, textX, titleCenter, theme.text());
		if (status != null) {
			boolean active = row.installed() != null && isActive(row.installed());
			int badgeX = textX + UiText.width(font, name, UiText.Size.BOLD) + 6;
			UiWidgets.badge(context, font, badgeX, titleCenter - (UiText.lineHeight(font, UiText.Size.SMALL) + 5) / 2, status, active ? UiTheme.fade(theme.accent(), 0.2F) : theme.segmentBackground(), active ? theme.accent() : theme.textSecondary());
		}
		UiText.drawCentered(context, font, UiText.ellipsize(font, Component.literal(row.description()), UiText.Size.BODY, textWidth), UiText.Size.BODY, textX, y + 31, theme.textSecondary());
		UiText.drawCentered(context, font, UiText.ellipsize(font, Component.literal(row.meta()), UiText.Size.BODY, textWidth), UiText.Size.BODY, textX, y + 45, theme.muted());
		return new RowBox(row, actionX, actionY, actionWidth, deleteX);
	}

	/**
	 * The pack's icon, or a plain symbol while it loads (pulsing) and for packs without one. An icon that
	 * was still loading while its row was on screen fades in; one that was ready already shows with its row.
	 */
	private void drawIcon(GuiGraphicsExtractor context, UiTheme theme, Row row, int x, int y) {
		UiShapes.roundedRect(context, x, y, ICON, ICON, 8, theme.segmentBackground());
		String url = row.iconUrl();
		PackIconLoader.IconResult icon = icons().resolve(url, PackIcons.RESOURCE_PACK);
		long now = System.nanoTime();
		if (url != null && icon.state() == PackIconLoader.State.LOADING) {
			iconAppear.put(url, Long.MAX_VALUE);
		}
		if (url != null && icon.state() == PackIconLoader.State.LOADED && icon.width() > 0) {
			Long seen = iconAppear.get(url);
			if (seen != null && seen == Long.MAX_VALUE) {
				iconAppear.put(url, now);
				seen = now;
			}
			float fade = seen == null ? 1.0F : Math.clamp((now - seen) / (float) ICON_FADE_NANOS, 0.0F, 1.0F);
			float scale = Math.min(ICON / (float) icon.width(), ICON / (float) icon.height());
			float drawWidth = icon.width() * scale;
			float drawHeight = icon.height() * scale;
			context.pose().pushMatrix();
			context.pose().translate(x + (ICON - drawWidth) / 2.0F, y + (ICON - drawHeight) / 2.0F);
			context.pose().scale(scale, scale);
			context.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), 0, 0, 0.0F, 0.0F, icon.width(), icon.height(), icon.width(), icon.height(), icon.width(), icon.height(), UiOpacity.apply(UiTheme.fade(0xFFFFFFFF, fade)));
			context.pose().popMatrix();
			if (fade >= 1.0F) {
				return;
			}
			// The symbol fades out underneath while the icon fades in.
			drawSymbol(context, theme, row, x, y, 1.0F - fade);
			return;
		}
		boolean pending = icon.state() == PackIconLoader.State.LOADING;
		drawSymbol(context, theme, row, x, y, pending ? 0.55F + 0.45F * (float) Math.sin(now / 250_000_000.0) : 1.0F);
	}

	private void drawSymbol(GuiGraphicsExtractor context, UiTheme theme, Row row, int x, int y, float alpha) {
		int symbol = 18;
		boolean shader = row.installed() != null ? row.installed().type() == PackType.SHADER : type == PackType.SHADER;
		UiIcons.draw(context, shader ? HubIcons.SPARKLES : HubIcons.PACKAGE, x + (ICON - symbol) / 2, y + (ICON - symbol) / 2, symbol, UiTheme.fade(theme.muted(), alpha));
	}

	private boolean isActive(InstalledPack pack) {
		return pack.type() == PackType.RESOURCE ? pack.enabled() : IrisCompat.isActiveShaderPack(pack.filename());
	}

	private @Nullable Component status(Row row) {
		if (row.installed() == null) {
			return null;
		}
		if (isActive(row.installed())) {
			return Component.translatable(row.installed().type() == PackType.RESOURCE ? EMUtilsTexts.PACK_STATUS_ENABLED : EMUtilsTexts.PACK_STATUS_SELECTED);
		}
		return view == View.MODRINTH ? Component.translatable(EMUtilsTexts.PACK_STATUS_INSTALLED) : null;
	}

	private record Action(Component label, UiWidgets.ButtonStyle style, boolean enabled, Runnable run) {
	}

	/** The row's main action: download, enable or disable a resource pack, or apply or turn off a shader. */
	private @Nullable Action action(Row row) {
		InstalledPack pack = row.installed();
		if (pack == null) {
			if (row.result() == null) {
				return null;
			}
			if (downloading.contains(row.result().projectId())) {
				return new Action(Component.translatable(EMUtilsTexts.UI_PACK_DOWNLOADING), UiWidgets.ButtonStyle.GHOST, false, () -> {
				});
			}
			return new Action(Component.translatable(EMUtilsTexts.PACK_DOWNLOAD), UiWidgets.ButtonStyle.PRIMARY, true, () -> download(row.result()));
		}
		if (pack.type() == PackType.RESOURCE) {
			return pack.enabled()
				? new Action(Component.translatable(EMUtilsTexts.PACK_DISABLE), UiWidgets.ButtonStyle.OUTLINE, true, () -> setEnabled(pack, false))
				: new Action(Component.translatable(EMUtilsTexts.PACK_ENABLE), UiWidgets.ButtonStyle.TONAL, true, () -> setEnabled(pack, true));
		}
		if (!IrisCompat.isIrisLoaded()) {
			return new Action(Component.translatable(EMUtilsTexts.UI_NEEDS_MOD, "Iris"), UiWidgets.ButtonStyle.GHOST, false, () -> {
			});
		}
		return IrisCompat.isActiveShaderPack(pack.filename())
			? new Action(Component.translatable(EMUtilsTexts.PACK_TURN_OFF), UiWidgets.ButtonStyle.OUTLINE, true, () -> turnOffShader(row))
			: new Action(Component.translatable(EMUtilsTexts.PACK_APPLY), UiWidgets.ButtonStyle.TONAL, true, () -> applyShader(row, pack));
	}

	private void showTooltip(Component text, int mouseX, int mouseY) {
		tooltip = text;
		tooltipX = mouseX;
		tooltipY = mouseY;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && dialog == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (dialog != null) {
			dialog.render(context, theme, mouseX, mouseY, width, height);
			if (dialog.isClosed()) {
				dialog = null;
			}
		}
	}

	// ---- actions --------------------------------------------------------------------------------

	private void download(ModrinthSearchResult result) {
		String projectId = result.projectId();
		PackType target = type;
		downloading.add(projectId);
		CompletableFuture.supplyAsync(() -> {
			try {
				ResourcePackController.install(modrinth, minecraft, target, result, index);
				return PackOperationResult.ok("Installed " + result.displayTitle() + ".");
			} catch (IOException | InterruptedException exception) {
				if (exception instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				return PackOperationResult.error(exception.getMessage());
			}
		}, WORKERS).thenAccept(outcome -> minecraft.execute(() -> {
			downloading.remove(projectId);
			report(outcome);
			refreshInstalled(false);
		}));
	}

	private void setEnabled(InstalledPack pack, boolean enabled) {
		report(ResourcePackController.setResourcePackEnabled(minecraft, pack.filename(), enabled));
		refreshInstalled(false);
	}

	private void applyShader(Row row, InstalledPack pack) {
		IrisCompat.applyShaderPackWithLoading(minecraft, this, pack.filename(), success -> {
			chat(Component.translatable(success ? EMUtilsTexts.PACK_SHADER_APPLIED : EMUtilsTexts.PACK_SHADER_APPLY_FAILED, row.title()).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
			refreshInstalled(false);
		});
	}

	private void turnOffShader(Row row) {
		IrisCompat.disableShaderPackWithLoading(minecraft, this, success -> {
			chat(Component.translatable(success ? EMUtilsTexts.PACK_SHADER_DISABLED : EMUtilsTexts.PACK_SHADER_DISABLE_FAILED, row.title()).withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
			refreshInstalled(false);
		});
	}

	private void askToDelete(InstalledPack pack) {
		dialog = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.PACK_DELETE_TITLE),
			Component.translatable(EMUtilsTexts.PACK_DELETE_MESSAGE, pack.filename()),
			Component.translatable(EMUtilsTexts.PACK_DELETE),
			() -> delete(pack)
		);
	}

	private void delete(InstalledPack pack) {
		// An enabled resource pack is turned off first, so the game isn't left using a deleted file.
		if (pack.type() == PackType.RESOURCE && pack.enabled()) {
			PackOperationResult disabled = ResourcePackController.setResourcePackEnabled(minecraft, pack.filename(), false);
			if (!disabled.success()) {
				report(disabled);
				return;
			}
		}
		CompletableFuture.supplyAsync(() -> ResourcePackController.deleteInstalledFile(minecraft, index, pack), WORKERS)
			.thenAccept(outcome -> minecraft.execute(() -> {
				report(outcome);
				refreshInstalled(false);
			}));
	}

	private void report(PackOperationResult result) {
		chat(Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED));
	}

	private void chat(Component message) {
		MinecraftClientCompat.chat(minecraft).addClientSystemMessage(EmUtilsChatPrefix.chat(message));
	}

	// ---- input ----------------------------------------------------------------------------------

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (closing()) {
			return true;
		}
		double mouseX = click.x();
		double mouseY = click.y();
		boolean left = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (dialog != null) {
			if (left) {
				dialog.mouseClicked(mouseX, mouseY);
			}
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		boolean onSearch = contains(mouseX, mouseY, searchX, toolbarY, searchWidth, SEARCH_HEIGHT);
		search.setFocused(onSearch);
		if (onSearch) {
			search.click(font, mouseX, click.hasShiftDown());
			return true;
		}
		if (contains(mouseX, mouseY, folderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON)) {
			VersionedPlatform.openFile(ResourcePackController.folder(minecraft, type).toFile());
			return true;
		}
		if (typeEdges != null && contains(mouseX, mouseY, typeX, typeY, typeEdges[2] - typeX, UiWidgets.SEGMENT_HEIGHT)) {
			switchType(mouseX < typeEdges[1] ? PackType.RESOURCE : PackType.SHADER);
			return true;
		}
		if (viewEdges != null && contains(mouseX, mouseY, viewX, toolbarY, viewEdges[2] - viewX, SEARCH_HEIGHT)) {
			switchView(mouseX < viewEdges[1] ? View.INSTALLED : View.MODRINTH);
			return true;
		}
		if (retryWidth > 0 && contains(mouseX, mouseY, retryX, retryY, retryWidth, BUTTON_HEIGHT)) {
			loadModrinth(false);
			return true;
		}
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (scroll.contains(mouseX, mouseY)) {
			for (RowBox box : rows) {
				Action action = action(box.row());
				if (action != null && action.enabled() && contains(mouseX, mouseY, box.actionX(), box.actionY(), box.actionWidth(), BUTTON_HEIGHT)) {
					action.run().run();
					return true;
				}
				if (box.row().installed() != null && contains(mouseX, mouseY, box.deleteX(), box.actionY(), BUTTON_HEIGHT, BUTTON_HEIGHT)) {
					askToDelete(box.row().installed());
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (dialog == null && scroll.mouseDragged(click.y())) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		if (scroll.mouseReleased()) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing() || dialog != null) {
			return true;
		}
		if (scroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		if (dialog != null) {
			dialog.keyPressed(input);
			return true;
		}
		if (search.focused() && input.isConfirmation()) {
			if (view == View.MODRINTH) {
				loadModrinth(false);
			}
			return true;
		}
		if (!search.focused() && (input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0) && input.key() == InputConstants.KEY_F) {
			search.setFocused(true);
			return true;
		}
		if (search.keyPressed(input, this::searchChanged)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (closing() || dialog != null) {
			return true;
		}
		if (search.charTyped(input, this::searchChanged)) {
			return true;
		}
		return super.charTyped(input);
	}

	@Override
	public void onClose() {
		search.setFocused(false);
		super.onClose();
	}

	/** Switches to the Modrinth view and searches for {@code query}; used by UI snapshots. */
	public void searchModrinthForSnapshot(String query) {
		search.setText(query);
		search.setFocused(false);
		if (view != View.MODRINTH) {
			view = View.MODRINTH;
			rowAppear.clear();
		}
		loadModrinth(false);
	}

	private record RowBox(Row row, int actionX, int actionY, int actionWidth, int deleteX) {
	}
}
