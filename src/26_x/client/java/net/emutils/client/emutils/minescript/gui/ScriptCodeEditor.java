package net.emutils.client.emutils.minescript.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.emutils.client.emutils.gui.ui.UiAnim;
import net.emutils.client.emutils.gui.ui.UiOpacity;
import net.emutils.client.emutils.gui.ui.UiScrollArea;
import net.emutils.client.emutils.gui.ui.UiTheme;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * The Script Manager's editor (#118): text editing through {@link ScriptTextBuffer}, set in
 * JetBrains Mono so columns line up, with line numbers, syntax colors that follow the theme,
 * smooth scrolling and a blinking caret. The screen owns it, places it and passes it input.
 */
final class ScriptCodeEditor {
	static final int LINE_HEIGHT = 13;
	private static final int PAD_TOP = 8;
	private static final int GUTTER_PAD = 10;
	private static final int CODE_GAP = 12;
	private static final int TAB_WIDTH = 4;
	private static final long BLINK_MILLIS = 530L;
	private static final int FADE_HEIGHT = 10;
	/** Find matches are amber, so they don't read as the selection (accent) or an error (warning). */
	private static final int FIND_COLOR = 0xFFE2B03A;

	private final Font font;
	private final UiAnim anim;
	private final ScriptTextBuffer buffer;
	private final UiScrollArea scroll = new UiScrollArea();
	private int x;
	private int y;
	private int width;
	private int height;
	private boolean focused;
	private boolean readOnly;
	private boolean dragging;
	private boolean draggingGutter;
	private float horizontalTarget;
	private float horizontal;
	private long caretMovedAt;
	private String findQuery = "";
	private List<ScriptTextBuffer.Match> matches = List.of();
	private int matchesVersion = -1;
	private String matchesQuery = "";
	/** The line the last run's error points at, or -1. */
	private int errorLine = -1;

	ScriptCodeEditor(Font font, UiAnim anim, Runnable dirtyListener) {
		this.font = font;
		this.anim = anim;
		this.buffer = new ScriptTextBuffer(dirtyListener, this::caretMoved);
	}

	void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		scroll.setBounds(x, y, width, height);
		updateContentHeight();
	}

	/** Shows {@code text}; a read-only script can be scrolled, selected and copied, but not changed. */
	void setText(String text, boolean readOnly) {
		buffer.setText(text);
		this.readOnly = readOnly;
		scroll.reset();
		anim.set("code-scroll", 0.0F);
		horizontalTarget = 0.0F;
		horizontal = 0.0F;
		anim.set("code-h", 0.0F);
		updateContentHeight();
	}

	String text() {
		return buffer.text();
	}

	boolean dirty() {
		return buffer.dirty();
	}

	void markClean() {
		buffer.markClean();
	}

	int caretLine() {
		return buffer.caretLine();
	}

	int caretColumn() {
		return buffer.caretColumn();
	}

	/** Changes with every edit, so the screen can tell when the script was changed. */
	int version() {
		return buffer.version();
	}

	/** Marks {@code line} (0-based) as where the last run failed, or clears it with -1. */
	void setErrorLine(int line) {
		errorLine = line;
	}

	/** Moves the caret to the start of {@code line}'s code and brings it into view. */
	void goToLine(int line) {
		int row = Mth.clamp(line, 0, buffer.lineCount() - 1);
		String text = buffer.line(row);
		buffer.setCaret(row, text.length() - text.stripLeading().length(), false);
	}

	/** The selected text when it's on one line, to start a search with. */
	@Nullable String singleLineSelection() {
		if (!buffer.hasSelection() || buffer.selectionStart().line() != buffer.selectionEnd().line()) {
			return null;
		}
		return buffer.selectedText();
	}

	// ---- find -----------------------------------------------------------------------------------

	void setFindQuery(String query) {
		findQuery = query == null ? "" : query;
	}

	List<ScriptTextBuffer.Match> matches() {
		if (matchesVersion != buffer.version() || !matchesQuery.equals(findQuery)) {
			matches = buffer.find(findQuery);
			matchesVersion = buffer.version();
			matchesQuery = findQuery;
		}
		return matches;
	}

	/** The index of the match that is selected, or -1. */
	int currentMatch() {
		if (!buffer.hasSelection()) {
			return -1;
		}
		ScriptTextBuffer.Position start = buffer.selectionStart();
		ScriptTextBuffer.Position end = buffer.selectionEnd();
		List<ScriptTextBuffer.Match> all = matches();
		for (int i = 0; i < all.size(); i++) {
			ScriptTextBuffer.Match match = all.get(i);
			if (match.line() == start.line() && match.start() == start.column() && match.line() == end.line() && match.end() == end.column()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Selects the next match after the selection (or the previous one before it), wrapping around.
	 * With {@code fromCaret}, the match at the caret itself counts, so typing a query finds it in place.
	 */
	void findNext(boolean forward, boolean fromCaret) {
		List<ScriptTextBuffer.Match> all = matches();
		if (all.isEmpty()) {
			return;
		}
		ScriptTextBuffer.Position from = fromCaret || !forward ? buffer.selectionStart() : buffer.selectionEnd();
		ScriptTextBuffer.Match target = null;
		if (forward) {
			for (ScriptTextBuffer.Match match : all) {
				if (match.line() > from.line() || match.line() == from.line() && match.start() >= from.column()) {
					target = match;
					break;
				}
			}
			if (target == null) {
				target = all.getFirst();
			}
		} else {
			for (int i = all.size() - 1; i >= 0; i--) {
				ScriptTextBuffer.Match match = all.get(i);
				if (match.line() < from.line() || match.line() == from.line() && match.start() < from.column()) {
					target = match;
					break;
				}
			}
			if (target == null) {
				target = all.getLast();
			}
		}
		buffer.select(target.line(), target.start(), target.end());
	}

	boolean focused() {
		return focused;
	}

	void setFocused(boolean focused) {
		if (this.focused != focused) {
			caretMovedAt = Util.getMillis();
		}
		this.focused = focused;
		// Not an EditBox, so it asks for text input itself (needed on SDL, 26.3+).
		VersionedInput.setTextInputFocus(this, focused);
	}

	/** Re-requests text input after the screen was re-initialized while the editor kept focus. */
	void restoreFocus() {
		if (focused) {
			VersionedInput.setTextInputFocus(this, true);
		}
	}

	boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	// ---- geometry -------------------------------------------------------------------------------

	private float advance() {
		return UiText.advance(font, UiText.Size.CODE);
	}

	private int gutterWidth() {
		int digits = Math.max(2, String.valueOf(buffer.lineCount()).length());
		return GUTTER_PAD + Math.round(digits * advance()) + CODE_GAP;
	}

	private int codeX() {
		return x + gutterWidth();
	}

	private int codeWidth() {
		return Math.max(1, x + width - UiScrollArea.GUTTER - 4 - codeX());
	}

	/** Column on screen of character {@code column}, with tabs expanded to the next multiple of four. */
	private static int visualColumn(String line, int column) {
		int visual = 0;
		for (int i = 0; i < column && i < line.length(); i++) {
			visual = line.charAt(i) == '\t' ? (visual / TAB_WIDTH + 1) * TAB_WIDTH : visual + 1;
		}
		return visual;
	}

	/** The character whose left half contains screen column {@code visual}, the inverse of {@link #visualColumn}. */
	private static int columnAt(String line, float visual) {
		int current = 0;
		for (int i = 0; i < line.length(); i++) {
			int next = line.charAt(i) == '\t' ? (current / TAB_WIDTH + 1) * TAB_WIDTH : current + 1;
			if (visual < (current + next) / 2.0F) {
				return i;
			}
			current = next;
		}
		return line.length();
	}

	private static String expandTabs(String text, int startVisual) {
		if (text.indexOf('\t') < 0) {
			return text;
		}
		StringBuilder builder = new StringBuilder();
		int visual = startVisual;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\t') {
				int next = (visual / TAB_WIDTH + 1) * TAB_WIDTH;
				builder.append(" ".repeat(next - visual));
				visual = next;
			} else {
				builder.append(c);
				visual++;
			}
		}
		return builder.toString();
	}

	private void updateContentHeight() {
		scroll.setContentHeight(PAD_TOP * 2 + buffer.lineCount() * LINE_HEIGHT);
	}

	private float maxHorizontal() {
		int widest = 0;
		for (int i = 0; i < buffer.lineCount(); i++) {
			String line = buffer.line(i);
			widest = Math.max(widest, visualColumn(line, line.length()));
		}
		return Math.max(0.0F, widest * advance() + 12.0F - codeWidth());
	}

	/** Keeps the caret in view, scrolling smoothly when it moves off an edge. */
	private void caretMoved() {
		caretMovedAt = Util.getMillis();
		updateContentHeight();
		int top = PAD_TOP + buffer.caretLine() * LINE_HEIGHT;
		double target = scroll.target();
		if (top - 4 < target) {
			scroll.scrollTo(top - 4);
		} else if (top + LINE_HEIGHT + 4 > target + height) {
			scroll.scrollTo(top + LINE_HEIGHT + 4 - height);
		}
		float caretX = visualColumn(buffer.line(buffer.caretLine()), buffer.caretColumn()) * advance();
		int room = codeWidth();
		if (caretX < horizontalTarget + 4.0F) {
			horizontalTarget = Math.max(0.0F, caretX - room / 3.0F);
		} else if (caretX > horizontalTarget + room - 8.0F) {
			horizontalTarget = caretX - room + room / 3.0F;
		}
		horizontalTarget = Mth.clamp(horizontalTarget, 0.0F, maxHorizontal());
	}

	private int lineAt(double mouseY) {
		return Mth.clamp((int) Math.floor((mouseY - y - PAD_TOP + scroll.exactOffset()) / LINE_HEIGHT), 0, buffer.lineCount() - 1);
	}

	private int columnAtMouse(int line, double mouseX) {
		return columnAt(buffer.line(line), (float) ((mouseX - codeX() + horizontal) / advance()));
	}

	// ---- drawing --------------------------------------------------------------------------------

	/**
	 * Draws the editor over {@code background}; {@code lightness} (0 dark, 1 light) picks the syntax
	 * colors, blending while the theme crossfades.
	 */
	void draw(GuiGraphicsExtractor context, UiTheme theme, float lightness, int background, int mouseX, int mouseY) {
		scroll.animate(anim, "code-scroll", mouseX, mouseY);
		horizontal = anim.towards("code-h", horizontalTarget, 18.0F);
		float advance = advance();
		int capHeight = UiText.lineHeight(font, UiText.Size.CODE);
		float textInset = (LINE_HEIGHT - capHeight) / 2.0F;
		int gutterRight = codeX() - CODE_GAP;
		int codeX = codeX();
		int codeRight = codeX + codeWidth();
		int caretLine = buffer.caretLine();
		ScriptTextBuffer.Position selectionStart = buffer.selectionStart();
		ScriptTextBuffer.Position selectionEnd = buffer.selectionEnd();
		boolean selection = buffer.hasSelection();

		int first = Math.max(0, (scroll.offset() - PAD_TOP) / LINE_HEIGHT);
		int last = Math.min(buffer.lineCount() - 1, (scroll.offset() + height - PAD_TOP) / LINE_HEIGHT + 1);
		float offset = scroll.exactOffset();

		scroll.begin(context);
		for (int line = first; line <= last; line++) {
			float top = y + PAD_TOP + line * LINE_HEIGHT - offset;
			if (line == errorLine) {
				// The line the last run failed on: a warning tint across it and a bar at the gutter's edge.
				fill(context, x, top, x + width - UiScrollArea.GUTTER, top + LINE_HEIGHT, UiTheme.fade(theme.warning(), 0.14F));
				fill(context, x, top, x + 2, top + LINE_HEIGHT, theme.warning());
			} else if (focused && !selection && line == caretLine) {
				fill(context, x, top, x + width - UiScrollArea.GUTTER, top + LINE_HEIGHT, UiTheme.fade(theme.text(), 0.05F));
			}
			String number = String.valueOf(line + 1);
			int numberColor = line == errorLine
				? theme.warning()
				: line == caretLine && focused ? theme.textSecondary() : UiTheme.fade(theme.muted(), 0.8F);
			UiText.drawExact(context, font, Component.literal(number), UiText.Size.CODE, gutterRight - number.length() * advance, top + textInset, numberColor);
		}

		context.enableScissor(codeX - 2, y, codeRight, y + height);
		int selectionColor = UiTheme.fade(theme.accent(), focused ? 0.32F : 0.2F);
		List<ScriptTextBuffer.Match> found = findQuery.isEmpty() ? List.of() : matches();
		int current = found.isEmpty() ? -1 : currentMatch();
		for (int i = 0; i < found.size(); i++) {
			ScriptTextBuffer.Match match = found.get(i);
			if (match.line() < first || match.line() > last) {
				continue;
			}
			String text = buffer.line(match.line());
			float top = y + PAD_TOP + match.line() * LINE_HEIGHT - offset;
			float startX = codeX - horizontal + visualColumn(text, match.start()) * advance;
			float endX = codeX - horizontal + visualColumn(text, match.end()) * advance;
			fill(context, startX, top + 1.0F, endX, top + LINE_HEIGHT - 1.0F, UiTheme.fade(FIND_COLOR, i == current ? 0.55F : 0.25F));
		}
		for (int line = first; line <= last; line++) {
			String text = buffer.line(line);
			float top = y + PAD_TOP + line * LINE_HEIGHT - offset;
			float left = codeX - horizontal;
			if (selection && line >= selectionStart.line() && line <= selectionEnd.line()) {
				int startColumn = line == selectionStart.line() ? selectionStart.column() : 0;
				int endColumn = line == selectionEnd.line() ? selectionEnd.column() : text.length();
				float startX = left + visualColumn(text, startColumn) * advance;
				// A selected line break shows as a little extra width past the line's end.
				float endX = left + visualColumn(text, endColumn) * advance + (line < selectionEnd.line() ? advance * 0.6F : 0.0F);
				fill(context, startX, top, Math.max(startX + 1.0F, endX), top + LINE_HEIGHT, selectionColor);
			}
			for (PythonTokens.Token token : PythonTokens.tokenize(text)) {
				int start = visualColumn(text, token.start());
				String piece = expandTabs(text.substring(token.start(), token.end()), start);
				if (piece.isBlank()) {
					continue;
				}
				float pieceX = left + start * advance;
				if (pieceX > codeRight || pieceX + piece.length() * advance < codeX - 2) {
					continue;
				}
				UiText.drawExact(context, font, Component.literal(piece), UiText.Size.CODE, pieceX, top + textInset, color(token.kind(), theme, lightness));
			}
		}
		long sinceMove = Util.getMillis() - caretMovedAt;
		if (focused && (sinceMove < BLINK_MILLIS || (sinceMove / BLINK_MILLIS) % 2L == 0L)) {
			String text = buffer.line(caretLine);
			float caretX = codeX - horizontal + visualColumn(text, buffer.caretColumn()) * advance;
			float top = y + PAD_TOP + caretLine * LINE_HEIGHT - offset;
			fill(context, caretX - 0.5F, top + 1.0F, caretX + 0.5F, top + LINE_HEIGHT - 1.0F, theme.accent());
		}
		context.disableScissor();
		scroll.end(context, background, FADE_HEIGHT, UiTheme.fade(theme.text(), 0.25F), UiTheme.fade(theme.text(), 0.45F));
	}

	/** Syntax colors: VS Code's Dark+ in the dark theme and a matching light set, blended by {@code lightness}. */
	private static int color(PythonTokens.Kind kind, UiTheme theme, float lightness) {
		return switch (kind) {
			case KEYWORD -> UiTheme.mix(0xFF569CD6, 0xFF0550AE, lightness);
			case STRING -> UiTheme.mix(0xFFCE9178, 0xFFA31515, lightness);
			case NUMBER -> UiTheme.mix(0xFFB5CEA8, 0xFF098658, lightness);
			case COMMENT -> UiTheme.mix(0xFF6A9955, 0xFF5F7A63, lightness);
			case TEXT -> theme.text();
		};
	}

	/** A filled rect at fractional positions, snapped to whole screen pixels by translating the pose. */
	private static void fill(GuiGraphicsExtractor context, float left, float top, float right, float bottom, int color) {
		int wholeLeft = (int) Math.floor(left);
		int wholeTop = (int) Math.floor(top);
		context.pose().pushMatrix();
		context.pose().translate(left - wholeLeft, top - wholeTop);
		context.fill(wholeLeft, wholeTop, wholeLeft + Math.max(1, Math.round(right - left)), wholeTop + Math.max(1, Math.round(bottom - top)), UiOpacity.apply(color));
		context.pose().popMatrix();
	}

	// ---- input ----------------------------------------------------------------------------------

	boolean mouseClicked(double mouseX, double mouseY, boolean shift, boolean doubled) {
		if (scroll.mouseClicked(mouseX, mouseY)) {
			return true;
		}
		if (!contains(mouseX, mouseY)) {
			return false;
		}
		setFocused(true);
		int line = lineAt(mouseY);
		if (mouseX < codeX() - CODE_GAP / 2) {
			buffer.setCaret(line, 0, shift);
			draggingGutter = true;
		} else if (doubled && !shift) {
			buffer.selectWordAt(line, columnAtMouse(line, mouseX));
		} else {
			buffer.setCaret(line, columnAtMouse(line, mouseX), shift);
			dragging = true;
		}
		return true;
	}

	boolean mouseDragged(double mouseX, double mouseY) {
		if (scroll.mouseDragged(mouseY)) {
			return true;
		}
		if (draggingGutter) {
			int line = lineAt(mouseY);
			buffer.setCaret(line, buffer.line(line).length(), true);
			return true;
		}
		if (dragging) {
			int line = lineAt(mouseY);
			buffer.setCaret(line, columnAtMouse(line, mouseX), true);
			return true;
		}
		return false;
	}

	boolean mouseReleased() {
		boolean wasActive = dragging || draggingGutter;
		dragging = false;
		draggingGutter = false;
		return scroll.mouseReleased() || wasActive;
	}

	boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, boolean shift) {
		if (!contains(mouseX, mouseY)) {
			return false;
		}
		double sideways = horizontalAmount != 0.0 ? horizontalAmount : shift ? verticalAmount : 0.0;
		if (sideways != 0.0) {
			horizontalTarget = Mth.clamp(horizontalTarget - (float) sideways * advance() * 4.0F, 0.0F, maxHorizontal());
			return true;
		}
		scroll.scroll(mouseX, mouseY, verticalAmount);
		return true;
	}

	boolean keyPressed(KeyEvent input) {
		if (!focused) {
			return false;
		}
		if (readOnly && !allowedWhenReadOnly(input)) {
			return true;
		}
		return buffer.keyPressed(input, Math.max(1, height / LINE_HEIGHT - 1));
	}

	boolean charTyped(CharacterEvent input) {
		if (!focused) {
			return false;
		}
		return readOnly || buffer.charTyped(input);
	}

	/** Moving, selecting and copying still work in a read-only script. */
	private static boolean allowedWhenReadOnly(KeyEvent input) {
		if (input.isCopy() || input.isSelectAll()) {
			return true;
		}
		return List.of(
			InputConstants.KEY_LEFT, InputConstants.KEY_RIGHT, InputConstants.KEY_UP, InputConstants.KEY_DOWN,
			InputConstants.KEY_HOME, InputConstants.KEY_END, InputConstants.KEY_PAGEUP, InputConstants.KEY_PAGEDOWN
		).contains(input.key());
	}
}
