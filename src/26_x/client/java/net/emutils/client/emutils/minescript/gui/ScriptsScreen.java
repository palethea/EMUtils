package net.emutils.client.emutils.minescript.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinescriptCompat;
import net.emutils.client.emutils.gui.hub.HubIcons;
import net.emutils.client.emutils.gui.ui.UiConfirmDialog;
import net.emutils.client.emutils.gui.ui.UiContextMenu;
import net.emutils.client.emutils.gui.ui.UiIcons;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiPanelScreen;
import net.emutils.client.emutils.gui.ui.UiPromptDialog;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.gui.ui.UiTextField;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiWidgets;
import net.emutils.client.emutils.minescript.MinescriptKeyBinding;
import net.emutils.client.emutils.minescript.MinescriptKeybindStore;
import net.emutils.client.emutils.minescript.MinescriptPython;
import net.emutils.client.emutils.minescript.MinescriptScript;
import net.emutils.client.emutils.minescript.MinescriptScriptRepository;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.emutils.client.versioned.VersionedInput;
import net.emutils.client.versioned.VersionedPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * The Script Manager in the new UI (#118): the Minescript folder as a tree on the left, and the selected
 * script in an editor card on the right, with run, save, keybind, rename and delete. The tree has
 * right-click menus for scripts and folders, the editor finds text with Ctrl+F, and when a run fails the
 * footer says why and the editor marks the line (#125).
 */
public final class ScriptsScreen extends UiPanelScreen {
	private static final int PADDING = 16;
	private static final int HEADER_BUTTON = 20;
	private static final int BUTTON_HEIGHT = 20;
	private static final int FIELD_HEIGHT = 22;
	private static final int LIST_WIDTH = 204;
	private static final int ROW_HEIGHT = 22;
	private static final int INDENT = 11;
	private static final int CARD_HEADER = 38;
	private static final int FOOTER = 22;
	private static final int FADE_HEIGHT = 10;
	private static final int BANNER = 42;
	private static final int FIND_WIDTH = 260;
	private static final int FIND_HEIGHT = 26;
	private static final int FIND_BUTTON = 18;
	private static final URI PYTHON_DOWNLOADS = URI.create("https://www.python.org/downloads/");
	private static final int RUNNING_POLL_TICKS = 10;
	/** How long a status message stays before it fades. */
	private static final long STATUS_MILLIS = 5000L;

	private enum Tone {
		NEUTRAL,
		GOOD,
		WARNING
	}

	private final MinescriptScriptRepository repository = new MinescriptScriptRepository();
	private final MinescriptKeybindStore keybindStore = MinescriptKeybindStore.load();
	private final UiTextField filter = new UiTextField(this, 64);
	private final UiScrollArea listScroll = new UiScrollArea();
	private final ScriptCodeEditor editor;
	private final Set<String> collapsed = new HashSet<>();
	private final List<RowBox> rowBoxes = new ArrayList<>();
	private List<MinescriptScript> scripts = List.of();
	private boolean scanned;
	private @Nullable MinescriptScript selected;
	/** The folder a new script goes into: the last folder clicked, or the selected script's folder. */
	private String folder = "";
	private boolean running;
	private int pollTicks;
	private @Nullable Component status;
	private Tone statusTone = Tone.NEUTRAL;
	private long statusAt;
	private @Nullable UiConfirmDialog confirm;
	private @Nullable UiPromptDialog prompt;
	private @Nullable ScriptKeybindDialog keybind;
	private @Nullable Component tooltip;
	private int tooltipX;
	private int tooltipY;
	private int headerButtonsY;
	private int newX;
	private int newWidth;
	private int refreshX;
	private int folderX;
	private int bodyY;
	private int listX;
	private int cardX;
	private int cardWidth;
	private int cardHeight;
	private int runX;
	private int runWidth;
	private int saveX;
	private int saveWidth;
	private int keybindX;
	private int deleteX;
	private int actionsY;
	/** The Python warning's height while it shows, so the editor sits below it. */
	private int bannerHeight;
	private int bannerButtonX;
	private int bannerButtonY;
	private int bannerButtonWidth;
	private @Nullable UiContextMenu menu;
	private final UiTextField findField = new UiTextField(this, 128);
	private boolean findOpen;
	private int findX;
	private int findY;
	private int findPrevX;
	private int findNextX;
	private int findCloseX;
	private int newFolderX;
	private int renameX;
	/** Why the open script's last run failed, and the editor version it was shown at. */
	private MinescriptCompat.@Nullable ScriptError error;
	private int errorVersion;
	private int errorX;
	private int errorWidth;

	public ScriptsScreen(@Nullable Screen parent) {
		super(Component.translatable(EMUtilsTexts.SCREEN_SCRIPT_MANAGER), parent);
		this.editor = new ScriptCodeEditor(Minecraft.getInstance().font, anim, () -> {
		});
	}

	@Override
	protected int maxPanelWidth() {
		return 860;
	}

	@Override
	protected int maxPanelHeight() {
		return 500;
	}

	@Override
	protected void layout() {
		int headerHeight = UiText.lineHeight(font, UiText.Size.HEADING) + 6 + UiText.lineHeight(font, UiText.Size.BODY);
		headerButtonsY = panelY + PADDING + (headerHeight - HEADER_BUTTON) / 2;
		bodyY = panelY + PADDING + headerHeight + 14;
		int bodyBottom = panelY + panelHeight - PADDING;
		listX = panelX + PADDING;
		int listTop = bodyY + FIELD_HEIGHT + 8;
		listScroll.setBounds(listX, listTop, LIST_WIDTH + UiScrollArea.GUTTER, bodyBottom - listTop);
		cardX = listX + LIST_WIDTH + UiScrollArea.GUTTER + 10;
		cardWidth = panelX + panelWidth - PADDING - cardX;
		cardHeight = bodyBottom - bodyY;
		placeEditor();
		filter.restoreFocus();
		editor.restoreFocus();
		if (!scanned) {
			refreshScripts();
			MinescriptPython.checkIfStale();
		}
	}

	private void placeEditor() {
		editor.setBounds(cardX + 1, bodyY + CARD_HEADER + bannerHeight, cardWidth - 2, cardHeight - CARD_HEADER - FOOTER - bannerHeight);
	}

	// ---- scripts --------------------------------------------------------------------------------

	/**
	 * Re-reads the Minescript folder. Without Minescript the scripts can still be browsed and edited,
	 * since they're only files, but only if the folder exists; running and keybinds need Minescript.
	 */
	private void refreshScripts() {
		scanned = true;
		if (!MinescriptCompat.isLoaded() && !Files.isDirectory(repository.root())) {
			scripts = List.of();
			return;
		}
		try {
			scripts = repository.scan();
		} catch (IOException exception) {
			setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
		}
	}

	private boolean minescript() {
		return MinescriptCompat.isLoaded();
	}

	private int scriptCount() {
		return (int) scripts.stream().filter(script -> !script.directory()).count();
	}

	private boolean isSelected(MinescriptScript script) {
		return selected != null && selected.relativePath().equals(script.relativePath());
	}

	/** The rows to show: the tree with collapsed folders hidden, or while filtering, every matching script. */
	private List<MinescriptScript> visibleScripts() {
		String query = filter.text().trim().toLowerCase(Locale.ROOT);
		List<MinescriptScript> visible = new ArrayList<>();
		for (MinescriptScript script : scripts) {
			if (!query.isEmpty()) {
				if (!script.directory() && script.relativePath().toLowerCase(Locale.ROOT).contains(query)) {
					visible.add(script);
				}
				continue;
			}
			if (!hiddenByCollapsedParent(script)) {
				visible.add(script);
			}
		}
		return visible;
	}

	private boolean hiddenByCollapsedParent(MinescriptScript script) {
		for (String folder : collapsed) {
			if (script.relativePath().startsWith(folder + "/")) {
				return true;
			}
		}
		return false;
	}

	private static String folderOf(MinescriptScript script) {
		if (script.directory()) {
			return script.relativePath();
		}
		int slash = script.relativePath().lastIndexOf('/');
		return slash <= 0 ? "" : script.relativePath().substring(0, slash);
	}

	/** Opens {@code script} in the editor, asking first if the open one has unsaved changes. */
	private void selectScript(MinescriptScript script) {
		if (isSelected(script)) {
			return;
		}
		whenSaved(() -> loadScript(script));
	}

	private void loadScript(MinescriptScript script) {
		selected = script;
		folder = folderOf(script);
		running = isRunning();
		error = null;
		editor.setErrorLine(-1);
		try {
			if (script.editable()) {
				editor.setText(repository.read(script), false);
				setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_LOADED, script.relativePath()), Tone.NEUTRAL);
			} else {
				editor.setText("# " + script.displayName() + " is runnable but read-only in EMUtils.\n", true);
				setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_READ_ONLY), Tone.WARNING);
			}
		} catch (IOException exception) {
			setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
		}
	}

	/** Runs {@code action} right away, or after the player agrees to discard unsaved changes. */
	private void whenSaved(Runnable action) {
		if (!editor.dirty()) {
			action.run();
			return;
		}
		confirm = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_TITLE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAVED_MESSAGE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DISCARD),
			() -> {
				editor.markClean();
				action.run();
			}
		);
	}

	private void saveSelected() {
		if (selected == null || !selected.editable()) {
			return;
		}
		try {
			repository.write(selected, editor.text());
			editor.markClean();
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVED), Tone.GOOD);
			refreshScripts();
		} catch (IOException exception) {
			setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
		}
	}

	private boolean canRun() {
		return selected != null && selected.runnable() && minescript();
	}

	private void runSelected() {
		if (!canRun()) {
			return;
		}
		if (editor.dirty()) {
			saveSelected();
		}
		String command = selected.commandName();
		if (!repository.isSafeCommand(command)) {
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAFE_COMMAND), Tone.WARNING);
			return;
		}
		error = null;
		editor.setErrorLine(-1);
		switch (MinescriptCompat.toggleCommand(command)) {
			case STARTED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_RUNNING, command), Tone.GOOD);
			case STOPPED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_STOPPED, command), Tone.NEUTRAL);
			case FAILED -> {
			}
		}
		running = isRunning();
	}

	private boolean isRunning() {
		return canRun() && !MinescriptCompat.findActiveJobIdsForCommand(selected.commandName()).isEmpty();
	}

	private void askToDelete(@Nullable MinescriptScript target) {
		if (target == null || !target.editable() || target.directory()) {
			return;
		}
		confirm = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_TITLE),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE_MESSAGE, target.relativePath()),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE),
			() -> {
				try {
					repository.delete(target);
					if (isSelected(target)) {
						selected = null;
						editor.setText("", false);
						editor.setFocused(false);
					}
					refreshScripts();
					setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETED), Tone.NEUTRAL);
				} catch (IOException exception) {
					setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
				}
			}
		);
	}

	private void askForNewScript() {
		whenSaved(() -> {
			editor.setFocused(false);
			filter.setFocused(false);
			prompt = new UiPromptDialog(
				font,
				anim,
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT),
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE_HINT),
				folder.isBlank() ? "new_script.py" : folder + "/new_script.py",
				Component.translatable(EMUtilsTexts.UI_SCRIPT_NAME_PLACEHOLDER),
				Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE),
				name -> {
					try {
						MinescriptScript script = repository.createScript(name);
						refreshScripts();
						loadScript(script);
						editor.setFocused(true);
						return null;
					} catch (IOException exception) {
						return Component.literal(String.valueOf(exception.getMessage()));
					}
				}
			);
		});
	}

	/** Asks for a new name or path for a script or folder, and moves it there with its keybinds. */
	private void askToRename(MinescriptScript item) {
		editor.setFocused(false);
		filter.setFocused(false);
		findField.setFocused(false);
		String path = item.relativePath();
		int nameStart = path.lastIndexOf('/') + 1;
		int dot = path.lastIndexOf('.');
		int nameEnd = item.directory() || dot <= nameStart ? path.length() : dot;
		prompt = new UiPromptDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAME_TITLE),
			Component.translatable(item.directory() ? EMUtilsTexts.UI_SCRIPT_RENAME_FOLDER_MESSAGE : EMUtilsTexts.UI_SCRIPT_RENAME_MESSAGE),
			path,
			Component.translatable(item.directory() ? EMUtilsTexts.UI_SCRIPT_NEW_FOLDER_PLACEHOLDER : EMUtilsTexts.UI_SCRIPT_NAME_PLACEHOLDER),
			Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAME_CONFIRM),
			name -> rename(item, name)
		).select(nameStart, nameEnd);
	}

	/** Renames or moves {@code item}; returns an error to show in the dialog, or null when it worked. */
	private @Nullable Component rename(MinescriptScript item, String name) {
		String from = item.relativePath();
		String to;
		try {
			to = repository.move(item, name);
		} catch (IOException exception) {
			return Component.literal(String.valueOf(exception.getMessage()));
		}
		if (to.equals(from)) {
			return null;
		}
		if (item.directory()) {
			keybindStore.rename(from, to, true);
			Set<String> moved = new HashSet<>();
			collapsed.removeIf(path -> {
				if (path.equals(from) || path.startsWith(from + "/")) {
					moved.add(to + path.substring(from.length()));
					return true;
				}
				return false;
			});
			collapsed.addAll(moved);
		} else if (item.commandName() != null) {
			String command = to.substring(0, to.lastIndexOf('.'));
			keybindStore.rename(item.commandName(), command, false);
			MinescriptCompat.renameCommand(item.commandName(), command);
		}
		EMUtilsClient.minescriptKeybinds().reload();
		if (folder.equals(from) || folder.startsWith(from + "/")) {
			folder = to + folder.substring(from.length());
		}
		refreshScripts();
		// The open script moves along without reloading, so unsaved changes stay in the editor.
		if (selected != null) {
			String open = selected.relativePath();
			String renamed = open.equals(from) ? to : open.startsWith(from + "/") ? to + open.substring(from.length()) : null;
			if (renamed != null) {
				for (MinescriptScript script : scripts) {
					if (script.relativePath().equals(renamed)) {
						selected = script;
						break;
					}
				}
			}
		}
		setStatus(Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAMED, to), Tone.GOOD);
		return null;
	}

	private void askForNewFolder(String parent) {
		editor.setFocused(false);
		filter.setFocused(false);
		findField.setFocused(false);
		String suggestion = "new_folder";
		String initial = parent.isBlank() ? suggestion : parent + "/" + suggestion;
		prompt = new UiPromptDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_FOLDER),
			Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_FOLDER_MESSAGE),
			initial,
			Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_FOLDER_PLACEHOLDER),
			Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_CREATE),
			name -> {
				try {
					String created = repository.createFolder(name);
					folder = created;
					for (String path = created; path.contains("/"); path = path.substring(0, path.lastIndexOf('/'))) {
						collapsed.remove(path.substring(0, path.lastIndexOf('/')));
					}
					refreshScripts();
					setStatus(Component.translatable(EMUtilsTexts.UI_SCRIPT_FOLDER_CREATED, created), Tone.GOOD);
					return null;
				} catch (IOException exception) {
					return Component.literal(String.valueOf(exception.getMessage()));
				}
			}
		).select(initial.length() - suggestion.length(), initial.length());
	}

	private void askToDeleteFolder(MinescriptScript target) {
		String path = target.relativePath();
		int count = repository.countScripts(target);
		confirm = new UiConfirmDialog(
			font,
			anim,
			Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER_TITLE),
			count == 0
				? Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER_EMPTY, path)
				: count == 1
					? Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER_MESSAGE_ONE, path)
					: Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER_MESSAGE, path, count),
			Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER),
			() -> {
				try {
					repository.deleteFolder(target);
				} catch (IOException exception) {
					refreshScripts();
					setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
					return;
				}
				keybindStore.removeFolder(path);
				EMUtilsClient.minescriptKeybinds().reload();
				if (selected != null && selected.relativePath().startsWith(path + "/")) {
					selected = null;
					editor.setText("", false);
					editor.setFocused(false);
				}
				collapsed.removeIf(folderPath -> folderPath.equals(path) || folderPath.startsWith(path + "/"));
				if (folder.equals(path) || folder.startsWith(path + "/")) {
					int slash = path.lastIndexOf('/');
					folder = slash < 0 ? "" : path.substring(0, slash);
				}
				refreshScripts();
				setStatus(Component.translatable(EMUtilsTexts.UI_SCRIPT_FOLDER_DELETED, path), Tone.NEUTRAL);
			}
		);
	}

	/** The right-click menu for a script, a folder, or (with null) the empty space in the list. */
	private void openMenu(@Nullable MinescriptScript script, int mouseX, int mouseY) {
		List<UiContextMenu.Item> items = new ArrayList<>();
		if (script == null) {
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT), () -> {
				folder = "";
				askForNewScript();
			}));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_FOLDER), () -> askForNewFolder("")));
		} else if (script.directory()) {
			String path = script.relativePath();
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_SCRIPT_HERE), () -> {
				folder = path;
				collapsed.remove(path);
				askForNewScript();
			}));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SCRIPT_NEW_FOLDER_HERE), () -> askForNewFolder(path)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAME), () -> askToRename(script)));
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.UI_SCRIPT_DELETE_FOLDER), true, true, () -> askToDeleteFolder(script)));
		} else {
			boolean runnable = script.runnable() && minescript();
			boolean scriptRunning = runnable && !MinescriptCompat.findActiveJobIdsForCommand(script.commandName()).isEmpty();
			items.add(new UiContextMenu.Item(Component.translatable(scriptRunning ? EMUtilsTexts.UI_SCRIPT_STOP : EMUtilsTexts.UI_SCRIPT_RUN), runnable, false, () -> runFromMenu(script)));
			items.add(UiContextMenu.Item.of(Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAME), () -> askToRename(script)));
			items.add(new UiContextMenu.Item(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE), script.editable(), true, () -> askToDelete(script)));
		}
		editor.setFocused(false);
		filter.setFocused(false);
		menu = new UiContextMenu(font, anim, mouseX, mouseY, items);
	}

	/** Runs or stops a script from its menu; the open one goes through Run, which saves it first. */
	private void runFromMenu(MinescriptScript script) {
		if (isSelected(script)) {
			runSelected();
			return;
		}
		String command = script.commandName();
		if (!repository.isSafeCommand(command)) {
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_UNSAFE_COMMAND), Tone.WARNING);
			return;
		}
		switch (MinescriptCompat.toggleCommand(command)) {
			case STARTED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_RUNNING, command), Tone.GOOD);
			case STOPPED -> setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_STOPPED, command), Tone.NEUTRAL);
			case FAILED -> {
			}
		}
	}

	// ---- find -----------------------------------------------------------------------------------

	/** Opens the find bar, starting from the selected text when it's on one line. */
	private void openFind() {
		if (selected == null) {
			return;
		}
		String selection = editor.singleLineSelection();
		if (selection != null && !selection.isBlank()) {
			findField.setText(selection);
		} else {
			findField.select(0, findField.text().length());
		}
		findOpen = true;
		editor.setFocused(false);
		filter.setFocused(false);
		findField.setFocused(true);
		editor.setFindQuery(findField.text());
	}

	private void closeFind() {
		findOpen = false;
		findField.setFocused(false);
		editor.setFindQuery("");
		if (selected != null) {
			editor.setFocused(true);
		}
	}

	private void findChanged() {
		editor.setFindQuery(findField.text());
		editor.findNext(true, true);
	}

	private void openKeybind() {
		if (canRun()) {
			showKeybindDialog();
		}
	}

	private void showKeybindDialog() {
		String command = selected.commandName();
		editor.setFocused(false);
		keybind = new ScriptKeybindDialog(font, anim, command, keybindStore, binding -> {
			keybindStore.put(binding);
			EMUtilsClient.minescriptKeybinds().reload();
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_SET, binding.displayName()), Tone.GOOD);
		}, () -> {
			keybindStore.remove(command);
			EMUtilsClient.minescriptKeybinds().reload();
			setStatus(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_KEYBIND_CLEARED), Tone.NEUTRAL);
		});
	}

	private void setStatus(Component message, Tone tone) {
		status = message;
		statusTone = tone;
		statusAt = Util.getMillis();
	}

	@Override
	public void tick() {
		super.tick();
		if (++pollTicks >= RUNNING_POLL_TICKS) {
			pollTicks = 0;
			running = isRunning();
			pollError();
		}
		if (error != null && (selected == null || editor.version() != errorVersion)) {
			// Edited since it failed, or closed (deleted): the error no longer applies.
			if (selected != null) {
				MinescriptCompat.dismissError(selected.commandName());
			}
			error = null;
			editor.setErrorLine(-1);
		}
	}

	/** Picks up why the open script's last run failed, once it has finished. */
	private void pollError() {
		MinescriptCompat.ScriptError latest = canRun() ? MinescriptCompat.lastError(selected.commandName()) : null;
		if (latest != null && !latest.equals(error)) {
			error = latest;
			errorVersion = editor.version();
			status = null;
			editor.setErrorLine(latest.line() - 1);
		} else if (latest == null && error != null) {
			error = null;
			editor.setErrorLine(-1);
		}
	}

	private boolean dialogOpen() {
		return confirm != null || prompt != null || keybind != null;
	}

	// ---- drawing --------------------------------------------------------------------------------

	@Override
	protected void drawPanel(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		tooltip = null;
		boolean interactive = !dialogOpen() && menu == null && !closing();
		int hoverX = interactive ? mouseX : Integer.MIN_VALUE / 2;
		int hoverY = interactive ? mouseY : Integer.MIN_VALUE / 2;
		drawHeader(context, theme, hoverX, hoverY);
		drawFilter(context, theme);
		drawList(context, theme, hoverX, hoverY);
		drawCard(context, theme, hoverX, hoverY);
	}

	private void drawHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int left = panelX + PADDING;
		int top = panelY + PADDING;
		UiText.draw(context, font, title, UiText.Size.HEADING, left, top, theme.text());
		boolean missing = !minescript();
		Component subtitle = missing
			? Component.translatable(EMUtilsTexts.UI_NEEDS_MOD, "Minescript")
			: Component.translatable(EMUtilsTexts.UI_SCRIPT_COUNT, scriptCount());
		UiText.draw(context, font, subtitle, UiText.Size.BODY, left, top + UiText.lineHeight(font, UiText.Size.HEADING) + 6, missing ? theme.warning() : theme.muted());

		Component newLabel = Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_NEW_SCRIPT);
		newWidth = UiWidgets.buttonWidth(font, newLabel) + 10;
		newX = panelX + panelWidth - PADDING - newWidth;
		UiWidgets.button(context, font, theme, newX, headerButtonsY, newWidth, HEADER_BUTTON, newLabel, UiWidgets.ButtonStyle.PRIMARY, contains(mouseX, mouseY, newX, headerButtonsY, newWidth, HEADER_BUTTON) ? 1.0F : 0.0F);
		refreshX = newX - 8 - HEADER_BUTTON;
		headerIcon(context, theme, refreshX, HubIcons.REFRESH_CW, EMUtilsTexts.SCRIPT_MANAGER_REFRESH, mouseX, mouseY);
		folderX = refreshX - 6 - HEADER_BUTTON;
		headerIcon(context, theme, folderX, HubIcons.FOLDER, EMUtilsTexts.SCRIPT_MANAGER_OPEN_FOLDER, mouseX, mouseY);
		newFolderX = folderX - 6 - HEADER_BUTTON;
		headerIcon(context, theme, newFolderX, HubIcons.FOLDER_PLUS, EMUtilsTexts.UI_SCRIPT_NEW_FOLDER, mouseX, mouseY);
	}

	private void headerIcon(GuiGraphicsExtractor context, UiTheme theme, int x, Identifier icon, String tooltipKey, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON);
		UiWidgets.iconButton(context, theme, x, headerButtonsY, HEADER_BUTTON, icon, hovered ? 1.0F : 0.0F);
		if (hovered) {
			showTooltip(Component.translatable(tooltipKey), mouseX, mouseY);
		}
	}

	private void drawFilter(GuiGraphicsExtractor context, UiTheme theme) {
		UiShapes.borderedRect(context, listX, bodyY, LIST_WIDTH, FIELD_HEIGHT, 8, theme.surface(), filter.focused() ? theme.accent() : theme.line());
		int center = bodyY + FIELD_HEIGHT / 2;
		UiIcons.draw(context, HubIcons.SEARCH, listX + 8, center - 5, 10, theme.textSecondary());
		filter.draw(context, font, theme, listX + 24, center, LIST_WIDTH - 32, Component.translatable(EMUtilsTexts.UI_SCRIPT_FILTER));
	}

	private void drawList(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		List<MinescriptScript> visible = visibleScripts();
		listScroll.setContentHeight(visible.isEmpty() ? 0 : visible.size() * ROW_HEIGHT + FADE_HEIGHT);
		listScroll.animate(anim, "scripts-list", mouseX, mouseY);
		rowBoxes.clear();
		boolean filtering = !filter.text().isBlank();
		boolean mouseInList = listScroll.contains(mouseX, mouseY) && !listScroll.dragging();
		listScroll.begin(context);
		context.pose().pushMatrix();
		context.pose().translate(0.0F, listScroll.offset() - listScroll.exactOffset());
		int width = listScroll.contentWidth();
		int y = listScroll.y() + FADE_HEIGHT / 2 - listScroll.offset();
		for (MinescriptScript script : visible) {
			if (y + ROW_HEIGHT >= listScroll.y() && y <= listScroll.y() + listScroll.height()) {
				drawRow(context, theme, script, filtering, listScroll.x(), y, width, mouseInList ? mouseX : Integer.MIN_VALUE / 2, mouseY);
			}
			rowBoxes.add(new RowBox(script, y));
			y += ROW_HEIGHT;
		}
		context.pose().popMatrix();
		if (visible.isEmpty()) {
			Component text = filtering
				? Component.translatable(EMUtilsTexts.UI_SCRIPT_NO_MATCH, filter.text().trim())
				: Component.translatable(minescript() || !scripts.isEmpty() ? EMUtilsTexts.SCRIPT_MANAGER_EMPTY : EMUtilsTexts.SCRIPT_MANAGER_REQUIRES_MINESCRIPT);
			int top = listScroll.y() + 24;
			int iconSize = 18;
			UiIcons.draw(context, HubIcons.FILE_CODE, listScroll.x() + (width - iconSize) / 2, top, iconSize, theme.muted());
			List<Component> lines = UiText.wrap(font, text, UiText.Size.BODY, width - 20);
			for (int i = 0; i < lines.size(); i++) {
				Component line = lines.get(i);
				UiText.draw(context, font, line, UiText.Size.BODY, listScroll.x() + (width - UiText.width(font, line, UiText.Size.BODY)) / 2, top + iconSize + 10 + i * 11, theme.muted());
			}
		}
		listScroll.end(context, theme.panel(), FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	private void drawRow(GuiGraphicsExtractor context, UiTheme theme, MinescriptScript script, boolean filtering, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		boolean chosen = isSelected(script);
		float hover = anim.towards("script:" + script.relativePath(), hovered, 16.0F);
		if (chosen) {
			UiShapes.roundedRect(context, x, y + 1, width, ROW_HEIGHT - 2, 6, theme.segmentSelected());
		} else if (hover > 0.01F) {
			UiShapes.roundedRect(context, x, y + 1, width, ROW_HEIGHT - 2, 6, UiTheme.fade(theme.hover(), hover));
		}
		int center = y + ROW_HEIGHT / 2;
		int left = x + 6 + (filtering ? 0 : script.depth() * INDENT);
		int textColor = script.editable() || script.directory() ? theme.text() : theme.muted();
		// The open script is marked by its row and an accent-colored icon.
		int iconColor = chosen ? theme.accent() : theme.textSecondary();
		if (script.directory()) {
			// The chevron turns as the folder opens and closes.
			float open = anim.transition("folder:" + script.relativePath(), collapsed.contains(script.relativePath()) ? 0.0F : 1.0F, 0.15F);
			context.pose().pushMatrix();
			context.pose().translate(left + 4.0F, center);
			context.pose().rotate((float) Math.toRadians(90.0F * open));
			UiIcons.draw(context, HubIcons.CHEVRON_RIGHT, -4, -4, 8, UiTheme.fade(iconColor, 0.8F));
			context.pose().popMatrix();
			UiIcons.draw(context, HubIcons.FOLDER, left + 12, center - 6, 12, iconColor);
		} else {
			UiIcons.draw(context, HubIcons.FILE_CODE, left + (filtering ? 0 : 12), center - 6, 12, iconColor);
		}
		int textX = left + (filtering ? 16 : 28);
		int right = x + width - 6;
		if (!script.directory() && script.commandName() != null) {
			MinescriptKeyBinding binding = keybindStore.get(script.commandName()).orElse(null);
			if (binding != null) {
				Component key = Component.literal(binding.displayName());
				int keyWidth = UiWidgets.keycapWidth(font, key);
				right -= keyWidth;
				UiWidgets.keycap(context, font, theme, right, center - UiWidgets.KEYCAP_HEIGHT / 2, key, false, false, 0.0F);
				right -= 6;
			}
		}
		String name = filtering ? script.relativePath() : script.displayName();
		UiText.drawCentered(context, font, UiText.ellipsize(font, Component.literal(name), UiText.Size.BODY, right - textX), UiText.Size.BODY, textX, center, textColor);
	}

	private void drawCard(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		UiShapes.borderedRect(context, cardX, bodyY, cardWidth, cardHeight, 10, theme.surface(), theme.border());
		MinescriptPython.Result python = minescript() ? MinescriptPython.result() : null;
		int banner = python != null && python.broken() ? BANNER : 0;
		if (banner != bannerHeight) {
			bannerHeight = banner;
			placeEditor();
		}
		bannerButtonWidth = 0;
		if (python != null && banner > 0) {
			drawPythonBanner(context, theme, python, selected == null ? bodyY : bodyY + CARD_HEADER, selected == null, mouseX, mouseY);
		}
		if (selected == null) {
			int iconSize = 24;
			int centerX = cardX + cardWidth / 2;
			int top = bodyY + banner / 2 + cardHeight / 2 - 30;
			UiIcons.draw(context, HubIcons.FILE_CODE, centerX - iconSize / 2, top, iconSize, theme.muted());
			Component text = Component.translatable(EMUtilsTexts.UI_SCRIPT_PICK);
			UiText.draw(context, font, text, UiText.Size.BODY, centerX - UiText.width(font, text, UiText.Size.BODY) / 2, top + iconSize + 12, theme.muted());
			return;
		}
		drawCardHeader(context, theme, mouseX, mouseY);
		context.fill(cardX + 1, bodyY + CARD_HEADER - 1, cardX + cardWidth - 1, bodyY + CARD_HEADER, UiOpacity.apply(theme.line()));
		editor.draw(context, theme, lightness(), theme.surface(), mouseX, mouseY);
		if (findOpen) {
			drawFindBar(context, theme, mouseX, mouseY);
		}
		drawFooter(context, theme, mouseX, mouseY);
	}

	/**
	 * The warning when Minescript's Python doesn't work (#122): what's wrong, and a button that fixes it,
	 * switching to a working Python, or opening python.org when none is installed.
	 */
	private void drawPythonBanner(GuiGraphicsExtractor context, UiTheme theme, MinescriptPython.Result python, int top, boolean cardTop, int mouseX, int mouseY) {
		int tint = UiTheme.mix(theme.surface(), theme.warning(), 0.1F);
		if (cardTop) {
			// The card's top corners are rounded; round the tint the same way inside the border.
			UiShapes.roundedRect(context, cardX + 1, top + 1, cardWidth - 2, BANNER - 1, 9, tint);
			context.fill(cardX + 1, top + 10, cardX + cardWidth - 1, top + BANNER, UiOpacity.apply(tint));
		} else {
			context.fill(cardX + 1, top, cardX + cardWidth - 1, top + BANNER, UiOpacity.apply(tint));
		}
		context.fill(cardX + 1, top + BANNER - 1, cardX + cardWidth - 1, top + BANNER, UiOpacity.apply(theme.line()));

		MinescriptPython.Python suggestion = python.suggestion();
		Component label = suggestion != null
			? Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_USE, suggestion.version())
			: python.searching()
				? Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_SEARCHING)
				: Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_GET);
		boolean enabled = !python.searching() || suggestion != null;
		bannerButtonWidth = UiWidgets.buttonWidth(font, label) + 10;
		bannerButtonX = cardX + cardWidth - 12 - bannerButtonWidth;
		bannerButtonY = top + (BANNER - BUTTON_HEIGHT) / 2;
		boolean hovered = enabled && contains(mouseX, mouseY, bannerButtonX, bannerButtonY, bannerButtonWidth, BUTTON_HEIGHT);
		UiWidgets.button(context, font, theme, bannerButtonX, bannerButtonY, bannerButtonWidth, BUTTON_HEIGHT, label, enabled ? UiWidgets.ButtonStyle.PRIMARY : UiWidgets.ButtonStyle.GHOST, hovered ? 1.0F : 0.0F);
		if (hovered) {
			showTooltip(Component.literal(suggestion != null ? suggestion.path() : PYTHON_DOWNLOADS.toString()), mouseX, mouseY);
		}

		int left = cardX + 12;
		UiIcons.draw(context, HubIcons.WRENCH, left, top + BANNER / 2 - 6, 12, theme.warning());
		int textX = left + 20;
		int room = bannerButtonX - 12 - textX;
		Component message = switch (python.problem()) {
			case NOT_SET -> Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_NOT_SET);
			case TOO_OLD -> Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_TOO_OLD, python.configuredPython() == null ? "?" : python.configuredPython().version());
			default -> Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_NOT_WORKING);
		};
		Component detail;
		if (suggestion == null && !python.searching()) {
			detail = Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_NONE_FOUND);
		} else if (python.configured() == null) {
			detail = Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_NO_LINE);
		} else {
			int labelWidth = UiText.width(font, Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_CONFIGURED, ""), UiText.Size.BODY);
			detail = Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_CONFIGURED, ellipsizeStart(python.configured(), room - labelWidth));
		}
		int lineHeight = UiText.lineHeight(font, UiText.Size.BODY);
		int textTop = top + (BANNER - lineHeight * 2 - 3) / 2;
		UiText.draw(context, font, UiText.ellipsize(font, message, UiText.Size.LABEL, room), UiText.Size.LABEL, textX, textTop, theme.text());
		UiText.draw(context, font, UiText.ellipsize(font, detail, UiText.Size.BODY, room), UiText.Size.BODY, textX, textTop + lineHeight + 3, theme.textSecondary());
	}

	/** Shortens a path from the start, since its end (the file) says the most. */
	private String ellipsizeStart(String text, int maxWidth) {
		if (UiText.width(font, Component.literal(text), UiText.Size.BODY) <= maxWidth) {
			return text;
		}
		for (int start = 1; start < text.length(); start++) {
			String shortened = "\u2026" + text.substring(start);
			if (UiText.width(font, Component.literal(shortened), UiText.Size.BODY) <= maxWidth) {
				return shortened;
			}
		}
		return "\u2026";
	}

	private void fixPython() {
		MinescriptPython.Result python = MinescriptPython.result();
		MinescriptPython.Python suggestion = python.suggestion();
		if (suggestion != null) {
			try {
				MinescriptPython.use(suggestion);
				setStatus(Component.translatable(EMUtilsTexts.UI_SCRIPT_PYTHON_FIXED, suggestion.version()), Tone.GOOD);
			} catch (IOException exception) {
				setStatus(Component.literal(String.valueOf(exception.getMessage())), Tone.WARNING);
			}
		} else if (!python.searching()) {
			VersionedPlatform.openUri(PYTHON_DOWNLOADS);
		}
	}

	private void drawCardHeader(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int center = bodyY + CARD_HEADER / 2;
		actionsY = center - BUTTON_HEIGHT / 2;
		int right = cardX + cardWidth - 10;

		// Right to left: run or stop, save, keybind, delete.
		boolean runnable = canRun();
		Component runLabel = Component.translatable(running ? EMUtilsTexts.UI_SCRIPT_STOP : EMUtilsTexts.SCRIPT_MANAGER_RUN);
		runWidth = Math.max(56, UiWidgets.buttonWidth(font, runLabel) + 22);
		runX = right - runWidth;
		boolean runHovered = runnable && contains(mouseX, mouseY, runX, actionsY, runWidth, BUTTON_HEIGHT);
		UiWidgets.button(context, font, theme, runX, actionsY, runWidth, BUTTON_HEIGHT, Component.empty(), !runnable ? UiWidgets.ButtonStyle.GHOST : running ? UiWidgets.ButtonStyle.OUTLINE : UiWidgets.ButtonStyle.PRIMARY, runHovered ? 1.0F : 0.0F);
		int runContent = 10 + 5 + UiText.width(font, runLabel, UiText.Size.LABEL);
		int runLeft = runX + (runWidth - runContent) / 2;
		int runColor = !runnable ? UiTheme.fade(theme.muted(), 0.7F) : running ? theme.text() : 0xFFFFFFFF;
		UiIcons.draw(context, running ? HubIcons.SQUARE : HubIcons.PLAY, runLeft, center - 5, 10, runColor);
		UiText.drawCentered(context, font, runLabel, UiText.Size.LABEL, runLeft + 15, center, runColor);
		if (!minescript() && contains(mouseX, mouseY, runX, actionsY, runWidth, BUTTON_HEIGHT)) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_NEEDS_MOD, "Minescript"), mouseX, mouseY);
		}

		Component saveLabel = Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_SAVE);
		saveWidth = UiWidgets.buttonWidth(font, saveLabel) + 8;
		saveX = runX - 6 - saveWidth;
		boolean canSave = selected.editable() && editor.dirty();
		if (canSave) {
			UiWidgets.button(context, font, theme, saveX, actionsY, saveWidth, BUTTON_HEIGHT, saveLabel, UiWidgets.ButtonStyle.TONAL, contains(mouseX, mouseY, saveX, actionsY, saveWidth, BUTTON_HEIGHT) ? 1.0F : 0.0F);
		} else {
			// Nothing to save: just the dimmed label, so it doesn't look clickable.
			UiText.drawCentered(context, font, saveLabel, UiText.Size.LABEL, saveX + (saveWidth - UiText.width(font, saveLabel, UiText.Size.LABEL)) / 2, center, UiTheme.fade(theme.muted(), 0.7F));
		}

		keybindX = saveX - 6 - BUTTON_HEIGHT;
		boolean keybindHovered = runnable && contains(mouseX, mouseY, keybindX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT);
		UiWidgets.ghostIconButton(context, theme, keybindX, actionsY, BUTTON_HEIGHT, HubIcons.KEYBOARD, runnable ? keybindHovered ? theme.text() : theme.textSecondary() : UiTheme.fade(theme.muted(), 0.5F), keybindHovered ? 1.0F : 0.0F);
		if (keybindHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_SET_KEYBIND), mouseX, mouseY);
		}
		deleteX = keybindX - 2 - BUTTON_HEIGHT;
		boolean deletable = selected.editable();
		boolean deleteHovered = deletable && contains(mouseX, mouseY, deleteX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT);
		UiWidgets.ghostIconButton(context, theme, deleteX, actionsY, BUTTON_HEIGHT, HubIcons.TRASH, deletable ? deleteHovered ? theme.warning() : theme.textSecondary() : UiTheme.fade(theme.muted(), 0.5F), deleteHovered ? 1.0F : 0.0F);
		if (deleteHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.SCRIPT_MANAGER_DELETE), mouseX, mouseY);
		}
		renameX = deleteX - 2 - BUTTON_HEIGHT;
		boolean renameHovered = contains(mouseX, mouseY, renameX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT);
		UiWidgets.ghostIconButton(context, theme, renameX, actionsY, BUTTON_HEIGHT, HubIcons.PENCIL, renameHovered ? theme.text() : theme.textSecondary(), renameHovered ? 1.0F : 0.0F);
		if (renameHovered) {
			showTooltip(Component.translatable(EMUtilsTexts.UI_SCRIPT_RENAME_TOOLTIP), mouseX, mouseY);
		}

		// Left: the script's name, an unsaved dot, and its keybind.
		int left = cardX + 12;
		UiIcons.draw(context, HubIcons.FILE_CODE, left, center - 6, 12, theme.textSecondary());
		int textX = left + 18;
		int room = renameX - 10 - textX;
		MinescriptKeyBinding binding = selected.commandName() == null ? null : keybindStore.get(selected.commandName()).orElse(null);
		Component key = binding == null ? null : Component.literal(binding.displayName());
		int keyWidth = key == null ? 0 : UiWidgets.keycapWidth(font, key) + 8;
		int dotWidth = editor.dirty() ? 12 : 0;
		Component name = UiText.ellipsize(font, Component.literal(selected.relativePath()), UiText.Size.BOLD, Math.max(20, room - keyWidth - dotWidth));
		UiText.drawCentered(context, font, name, UiText.Size.BOLD, textX, center, theme.text());
		int after = textX + UiText.width(font, name, UiText.Size.BOLD);
		float dirty = anim.towards("script-dirty", editor.dirty(), 14.0F);
		if (dirty > 0.01F) {
			UiShapes.circle(context, after + 6, center - 3, 6, UiTheme.fade(theme.accent(), dirty));
			if (contains(mouseX, mouseY, after + 3, center - 6, 12, 12)) {
				showTooltip(Component.translatable(EMUtilsTexts.UI_SCRIPT_UNSAVED), mouseX, mouseY);
			}
		}
		if (key != null) {
			UiWidgets.keycap(context, font, theme, after + 8 + dotWidth, center - UiWidgets.KEYCAP_HEIGHT / 2, key, false, false, 0.0F);
		}
	}

	/** The find bar, floating at the editor's top right like in code editors. */
	private void drawFindBar(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		findX = cardX + cardWidth - 1 - UiScrollArea.GUTTER - 8 - FIND_WIDTH;
		findY = bodyY + CARD_HEADER + bannerHeight + 6;
		UiShapes.shadow(context, findX, findY, FIND_WIDTH, FIND_HEIGHT, 7, 8, theme.shadow());
		UiShapes.borderedRect(context, findX, findY, FIND_WIDTH, FIND_HEIGHT, 7, theme.surface(), findField.focused() ? theme.accent() : theme.line());
		int center = findY + FIND_HEIGHT / 2;
		int buttonY = center - FIND_BUTTON / 2;
		findCloseX = findX + FIND_WIDTH - 4 - FIND_BUTTON;
		findNextX = findCloseX - 2 - FIND_BUTTON;
		findPrevX = findNextX - 2 - FIND_BUTTON;
		findButton(context, theme, findPrevX, buttonY, HubIcons.CHEVRON_UP, EMUtilsTexts.UI_SCRIPT_FIND_PREVIOUS, mouseX, mouseY);
		findButton(context, theme, findNextX, buttonY, HubIcons.CHEVRON_DOWN, EMUtilsTexts.UI_SCRIPT_FIND_NEXT, mouseX, mouseY);
		findButton(context, theme, findCloseX, buttonY, HubIcons.X, EMUtilsTexts.UI_SCRIPT_FIND_CLOSE, mouseX, mouseY);

		List<ScriptTextBuffer.Match> matches = editor.matches();
		int current = editor.currentMatch();
		Component count = findField.text().isEmpty()
			? Component.empty()
			: matches.isEmpty()
				? Component.translatable(EMUtilsTexts.UI_SCRIPT_FIND_NONE)
				: current >= 0
					? Component.translatable(EMUtilsTexts.UI_SCRIPT_FIND_COUNT, current + 1, matches.size())
					: Component.translatable(EMUtilsTexts.UI_SCRIPT_FIND_FOUND, matches.size());
		int countWidth = UiText.width(font, count, UiText.Size.BODY);
		int countX = findPrevX - 6 - countWidth;
		UiText.drawCentered(context, font, count, UiText.Size.BODY, countX, center, matches.isEmpty() && !findField.text().isEmpty() ? theme.warning() : theme.muted());
		UiIcons.draw(context, HubIcons.SEARCH, findX + 8, center - 5, 10, theme.textSecondary());
		findField.draw(context, font, theme, findX + 24, center, countX - 8 - (findX + 24), Component.translatable(EMUtilsTexts.UI_SCRIPT_FIND_PLACEHOLDER));
	}

	private void findButton(GuiGraphicsExtractor context, UiTheme theme, int x, int y, Identifier icon, String tooltipKey, int mouseX, int mouseY) {
		boolean hovered = contains(mouseX, mouseY, x, y, FIND_BUTTON, FIND_BUTTON);
		UiWidgets.ghostIconButton(context, theme, x, y, FIND_BUTTON, icon, hovered ? theme.text() : theme.textSecondary(), hovered ? 1.0F : 0.0F);
		if (hovered) {
			showTooltip(Component.translatable(tooltipKey), mouseX, mouseY);
		}
	}

	private void drawFooter(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		int top = bodyY + cardHeight - FOOTER;
		context.fill(cardX + 1, top, cardX + cardWidth - 1, top + 1, UiOpacity.apply(theme.line()));
		int center = top + FOOTER / 2;
		Component position = selected.editable()
			? Component.translatable(EMUtilsTexts.UI_SCRIPT_CURSOR, editor.caretLine() + 1, editor.caretColumn() + 1)
			: Component.translatable(EMUtilsTexts.UI_SCRIPT_READ_ONLY);
		int positionWidth = UiText.width(font, position, UiText.Size.BODY);
		UiText.drawCentered(context, font, position, UiText.Size.BODY, cardX + cardWidth - 12 - positionWidth, center, theme.muted());
		if (status != null) {
			long age = Util.getMillis() - statusAt;
			float alpha = age < STATUS_MILLIS ? 1.0F : 1.0F - Math.clamp((age - STATUS_MILLIS) / 400.0F, 0.0F, 1.0F);
			if (alpha <= 0.0F) {
				status = null;
				return;
			}
			int color = switch (statusTone) {
				case GOOD -> theme.accent();
				case WARNING -> theme.warning();
				case NEUTRAL -> theme.textSecondary();
			};
			Component line = UiText.ellipsize(font, status, UiText.Size.BODY, cardWidth - 36 - positionWidth);
			UiText.drawCentered(context, font, line, UiText.Size.BODY, cardX + 12, center, UiTheme.fade(color, alpha));
			errorWidth = 0;
			return;
		}
		errorWidth = 0;
		if (error != null) {
			// Why the last run failed; clicking it jumps to the line.
			Component message = error.line() > 0
				? Component.translatable(EMUtilsTexts.UI_SCRIPT_ERROR_LINE, error.line(), error.message())
				: Component.literal(error.message());
			Component line = UiText.ellipsize(font, message, UiText.Size.BODY, cardWidth - 50 - positionWidth);
			errorX = cardX + 12;
			UiIcons.draw(context, HubIcons.X, errorX, center - 5, 10, theme.warning());
			UiText.drawCentered(context, font, line, UiText.Size.BODY, errorX + 16, center, theme.warning());
			errorWidth = 16 + UiText.width(font, line, UiText.Size.BODY);
			if (error.line() > 0 && contains(mouseX, mouseY, errorX, top, errorWidth, FOOTER)) {
				showTooltip(Component.translatable(EMUtilsTexts.UI_SCRIPT_ERROR_GO_TO), mouseX, mouseY);
			}
		}
	}

	private void showTooltip(Component text, int mouseX, int mouseY) {
		tooltip = text;
		tooltipX = mouseX;
		tooltipY = mouseY;
	}

	@Override
	protected void drawOverlay(GuiGraphicsExtractor context, UiTheme theme, int mouseX, int mouseY) {
		if (tooltip != null && !dialogOpen() && menu == null) {
			UiWidgets.tooltip(context, font, theme, tooltip, tooltipX, tooltipY, width, height);
		}
		if (menu != null) {
			menu.render(context, theme, mouseX, mouseY, width, height);
			if (menu.isClosed()) {
				menu = null;
			}
		}
		if (confirm != null) {
			confirm.render(context, theme, mouseX, mouseY, width, height);
			if (confirm.isClosed()) {
				confirm = null;
			}
		}
		if (prompt != null) {
			prompt.render(context, theme, mouseX, mouseY, width, height);
			if (prompt.isClosed()) {
				prompt = null;
			}
		}
		if (keybind != null) {
			keybind.render(context, theme, mouseX, mouseY, width, height);
			if (keybind.isClosed()) {
				keybind = null;
			}
		}
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
		if (menu != null) {
			// Any click closes the menu; one on an item also runs it.
			menu.mouseClicked(mouseX, mouseY);
			menu = null;
			return true;
		}
		if (dialogOpen()) {
			if (left) {
				if (confirm != null) {
					confirm.mouseClicked(mouseX, mouseY);
				} else if (prompt != null) {
					prompt.mouseClicked(mouseX, mouseY, click.hasShiftDown());
				} else if (keybind != null) {
					keybind.mouseClicked(mouseX, mouseY);
				}
			}
			return true;
		}
		if (click.button() == InputConstants.MOUSE_BUTTON_RIGHT && listScroll.contains(mouseX, mouseY)) {
			MinescriptScript target = null;
			for (RowBox box : rowBoxes) {
				if (mouseY >= box.y() && mouseY < box.y() + ROW_HEIGHT) {
					target = box.script();
					break;
				}
			}
			openMenu(target, (int) mouseX, (int) mouseY);
			return true;
		}
		if (!left) {
			return super.mouseClicked(click, doubled);
		}
		if (findOpen && clickFindBar(mouseX, mouseY, click.hasShiftDown())) {
			return true;
		}
		findField.setFocused(false);
		boolean onFilter = contains(mouseX, mouseY, listX, bodyY, LIST_WIDTH, FIELD_HEIGHT);
		filter.setFocused(onFilter);
		if (onFilter) {
			editor.setFocused(false);
			filter.click(font, mouseX, click.hasShiftDown());
			return true;
		}
		if (contains(mouseX, mouseY, newX, headerButtonsY, newWidth, HEADER_BUTTON)) {
			askForNewScript();
			return true;
		}
		if (contains(mouseX, mouseY, newFolderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON)) {
			askForNewFolder(folder);
			return true;
		}
		if (selected != null && errorWidth > 0 && error != null && error.line() > 0 && contains(mouseX, mouseY, errorX, bodyY + cardHeight - FOOTER, errorWidth, FOOTER)) {
			editor.goToLine(error.line() - 1);
			editor.setFocused(true);
			return true;
		}
		if (contains(mouseX, mouseY, refreshX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON)) {
			refreshScripts();
			MinescriptPython.check();
			return true;
		}
		if (bannerButtonWidth > 0 && contains(mouseX, mouseY, bannerButtonX, bannerButtonY, bannerButtonWidth, BUTTON_HEIGHT)) {
			fixPython();
			return true;
		}
		if (contains(mouseX, mouseY, folderX, headerButtonsY, HEADER_BUTTON, HEADER_BUTTON)) {
			try {
				Files.createDirectories(repository.root());
			} catch (IOException ignored) {
			}
			VersionedPlatform.openFile(repository.root().toFile());
			return true;
		}
		if (listScroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (listScroll.contains(mouseX, mouseY)) {
			editor.setFocused(false);
			for (RowBox box : rowBoxes) {
				if (mouseY >= box.y() && mouseY < box.y() + ROW_HEIGHT) {
					clickRow(box.script());
					return true;
				}
			}
			return true;
		}
		if (selected != null && clickCardHeader(mouseX, mouseY)) {
			return true;
		}
		if (selected != null && editor.mouseClicked(mouseX, mouseY, click.hasShiftDown(), doubled)) {
			return true;
		}
		editor.setFocused(false);
		return super.mouseClicked(click, doubled);
	}

	/** Handles a click on the find bar: its buttons, or placing the caret in its field. */
	private boolean clickFindBar(double mouseX, double mouseY, boolean shift) {
		if (!contains(mouseX, mouseY, findX, findY, FIND_WIDTH, FIND_HEIGHT)) {
			return false;
		}
		int buttonY = findY + FIND_HEIGHT / 2 - FIND_BUTTON / 2;
		if (contains(mouseX, mouseY, findCloseX, buttonY, FIND_BUTTON, FIND_BUTTON)) {
			closeFind();
		} else if (contains(mouseX, mouseY, findNextX, buttonY, FIND_BUTTON, FIND_BUTTON)) {
			editor.findNext(true, false);
		} else if (contains(mouseX, mouseY, findPrevX, buttonY, FIND_BUTTON, FIND_BUTTON)) {
			editor.findNext(false, false);
		} else {
			editor.setFocused(false);
			filter.setFocused(false);
			findField.setFocused(true);
			findField.click(font, mouseX, shift);
		}
		return true;
	}

	private void clickRow(MinescriptScript script) {
		if (script.directory()) {
			folder = script.relativePath();
			if (!collapsed.remove(script.relativePath())) {
				collapsed.add(script.relativePath());
			}
			return;
		}
		selectScript(script);
	}

	private boolean clickCardHeader(double mouseX, double mouseY) {
		if (contains(mouseX, mouseY, runX, actionsY, runWidth, BUTTON_HEIGHT)) {
			runSelected();
			return true;
		}
		if (contains(mouseX, mouseY, saveX, actionsY, saveWidth, BUTTON_HEIGHT)) {
			saveSelected();
			return true;
		}
		if (contains(mouseX, mouseY, keybindX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
			openKeybind();
			return true;
		}
		if (contains(mouseX, mouseY, deleteX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
			askToDelete(selected);
			return true;
		}
		if (contains(mouseX, mouseY, renameX, actionsY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
			askToRename(selected);
			return true;
		}
		return contains(mouseX, mouseY, cardX, bodyY, cardWidth, CARD_HEADER);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		if (!dialogOpen() && (listScroll.mouseDragged(click.y()) || editor.mouseDragged(click.x(), click.y()))) {
			return true;
		}
		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		boolean list = listScroll.mouseReleased();
		boolean code = editor.mouseReleased();
		if (list || code) {
			return true;
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing() || dialogOpen()) {
			return true;
		}
		menu = null;
		if (listScroll.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		if (selected != null && editor.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount, VersionedInput.isShiftDown(minecraft))) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (closing()) {
			return true;
		}
		// A dialog that is closing lets keys through, so typing right after Enter reaches the editor.
		if (confirm != null && !confirm.closing()) {
			confirm.keyPressed(input);
			return true;
		}
		if (prompt != null && !prompt.closing()) {
			prompt.keyPressed(input);
			return true;
		}
		if (keybind != null && !keybind.closing()) {
			keybind.keyPressed(input);
			return true;
		}
		if (menu != null) {
			if (input.isEscape()) {
				menu = null;
			}
			return true;
		}
		boolean command = input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0;
		if (command && input.key() == InputConstants.KEY_S) {
			saveSelected();
			return true;
		}
		if (command && input.key() == InputConstants.KEY_F) {
			// In the editor (or its find bar) Ctrl+F finds in the script; anywhere else it filters the list.
			if (selected != null && (editor.focused() || findField.focused())) {
				openFind();
			} else {
				editor.setFocused(false);
				findField.setFocused(false);
				filter.setFocused(true);
			}
			return true;
		}
		if (findOpen && input.key() == InputConstants.KEY_F3) {
			editor.findNext(!input.hasShiftDown(), false);
			return true;
		}
		if (input.key() == InputConstants.KEY_F2 && selected != null && !filter.focused() && !findField.focused()) {
			askToRename(selected);
			return true;
		}
		if (findField.focused()) {
			if (input.isEscape()) {
				closeFind();
			} else if (input.isConfirmation()) {
				editor.findNext(!input.hasShiftDown(), false);
			} else {
				findField.keyPressed(input, this::findChanged);
			}
			return true;
		}
		if (findOpen && input.isEscape() && editor.focused()) {
			closeFind();
			return true;
		}
		if (filter.keyPressed(input, listScroll::reset)) {
			return true;
		}
		if (editor.keyPressed(input)) {
			return true;
		}
		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (closing()) {
			return true;
		}
		if (prompt != null && !prompt.closing()) {
			prompt.charTyped(input);
			return true;
		}
		if ((confirm != null && !confirm.closing()) || (keybind != null && !keybind.closing()) || menu != null) {
			return true;
		}
		if (findField.focused()) {
			findField.charTyped(input, this::findChanged);
			return true;
		}
		if (filter.charTyped(input, listScroll::reset)) {
			return true;
		}
		if (editor.charTyped(input)) {
			return true;
		}
		return super.charTyped(input);
	}

	/** Asks before throwing away unsaved changes, like the classic screen. */
	@Override
	public void onClose() {
		if (dialogOpen()) {
			return;
		}
		whenSaved(() -> {
			filter.setFocused(false);
			editor.setFocused(false);
			super.onClose();
		});
	}

	/** Opens {@code relativePath} in the editor; used by UI snapshots. */
	public void openForSnapshot(String relativePath) {
		refreshScripts();
		for (MinescriptScript script : scripts) {
			if (script.relativePath().equals(relativePath)) {
				loadScript(script);
				editor.setFocused(true);
				return;
			}
		}
	}

	/** Opens the keybind dialog for the open script, even without Minescript; used by UI snapshots. */
	public void openKeybindForSnapshot() {
		if (selected != null && selected.commandName() != null) {
			showKeybindDialog();
		}
	}

	/** Opens the new script dialog; used by UI snapshots, after throwing away any edits. */
	public void newScriptForSnapshot() {
		editor.markClean();
		askForNewScript();
	}

	/** Presses Run/Stop for the open script, as the button does; used by UI snapshots. */
	public void runForSnapshot() {
		runSelected();
	}

	/** Whether the open script has a running job, as the Run/Stop button shows it; used by UI snapshots. */
	public boolean runningForSnapshot() {
		return isRunning();
	}

	/** The open script's path inside the minescript folder, or null; used by UI snapshots. */
	public @Nullable String selectedForSnapshot() {
		return selected == null ? null : selected.relativePath();
	}

	/** Presses the Python warning's button; used by UI snapshots. */
	public void fixPythonForSnapshot() {
		fixPython();
	}

	/** The open script's text in the editor, saved or not; used by UI snapshots. */
	public String editorTextForSnapshot() {
		return editor.text();
	}

	/** Why the open script's last run failed, as the footer shows it; used by UI snapshots. */
	public MinescriptCompat.@Nullable ScriptError errorForSnapshot() {
		return error;
	}

	/** The find bar's count: the selected match (1-based, 0 for none) and how many there are; used by UI snapshots. */
	public int[] findForSnapshot() {
		return new int[] {editor.currentMatch() + 1, editor.matches().size()};
	}

	/** Opens the right-click menu for {@code relativePath} (null for the list's empty space); used by UI snapshots. */
	public void menuForSnapshot(@Nullable String relativePath, int mouseX, int mouseY) {
		MinescriptScript target = null;
		for (MinescriptScript script : scripts) {
			if (script.relativePath().equals(relativePath)) {
				target = script;
			}
		}
		openMenu(target, mouseX, mouseY);
	}

	/** Opens the rename dialog for {@code relativePath}; used by UI snapshots. */
	public void renameForSnapshot(String relativePath) {
		for (MinescriptScript script : scripts) {
			if (script.relativePath().equals(relativePath)) {
				askToRename(script);
			}
		}
	}

	/** Opens the delete dialog for the folder {@code relativePath}; used by UI snapshots. */
	public void deleteFolderForSnapshot(String relativePath) {
		for (MinescriptScript script : scripts) {
			if (script.directory() && script.relativePath().equals(relativePath)) {
				askToDeleteFolder(script);
			}
		}
	}

	/** Opens the new folder dialog in {@code parent}; used by UI snapshots. */
	public void newFolderForSnapshot(String parent) {
		askForNewFolder(parent);
	}

	/** Every script and folder in the list, by path; used by UI snapshots. */
	public List<String> pathsForSnapshot() {
		return scripts.stream().map(MinescriptScript::relativePath).toList();
	}

	/** Throws away edits so the screen can close without asking; used by UI snapshots. */
	public void discardForSnapshot() {
		editor.markClean();
	}

	private record RowBox(MinescriptScript script, int y) {
	}
}
