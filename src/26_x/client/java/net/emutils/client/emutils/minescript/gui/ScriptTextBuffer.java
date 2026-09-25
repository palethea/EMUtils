package net.emutils.client.emutils.minescript.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;

/**
 * The text of a script being edited: its lines, the caret and selection, undo and redo, and every
 * editing key. It knows nothing about pixels; the editors only lay the text out and turn mouse
 * positions into positions. Editing follows Python conventions (#125): Enter keeps the indentation
 * and indents after a colon, Tab inserts four spaces, and brackets and quotes close themselves.
 */
public final class ScriptTextBuffer {
	private static final int MAX_UNDO = 100;
	/** One indentation level: four spaces, as PEP 8 and Minescript's own scripts use. */
	static final int INDENT_WIDTH = 4;
	private static final String INDENT = " ".repeat(INDENT_WIDTH);
	/** Prefixes that may come right before a string's opening quote, such as f"..." or rb'...'. */
	private static final Set<String> STRING_PREFIXES = Set.of("f", "r", "b", "u", "rb", "br", "fr", "rf");

	private final Runnable dirtyListener;
	private final Runnable caretListener;
	private final List<String> lines = new ArrayList<>();
	private final Deque<EditorState> undoStack = new ArrayDeque<>();
	private final Deque<EditorState> redoStack = new ArrayDeque<>();
	private int caretLine;
	private int caretColumn;
	private int anchorLine;
	private int anchorColumn;
	private boolean dirty;
	private boolean applyingHistory;
	/** Counts edits, so views can tell when the text changed since they last looked. */
	private int version;

	/**
	 * {@code dirtyListener} runs when the text first differs from the saved text, and
	 * {@code caretListener} whenever the caret moved, so the editor can keep it in view.
	 */
	public ScriptTextBuffer(Runnable dirtyListener, Runnable caretListener) {
		this.dirtyListener = dirtyListener;
		this.caretListener = caretListener;
		setText("");
	}

	public record Position(int line, int column) {
	}

	public void setText(String text) {
		lines.clear();
		String[] split = (text == null ? "" : text).split("\\R", -1);
		for (String line : split) {
			lines.add(line);
		}
		if (lines.isEmpty()) {
			lines.add("");
		}
		caretLine = caretColumn = anchorLine = anchorColumn = 0;
		dirty = false;
		undoStack.clear();
		redoStack.clear();
		version++;
	}

	public int version() {
		return version;
	}

	/** Selects columns {@code start} to {@code end} of {@code line}, with the caret at the end. */
	public void select(int line, int start, int end) {
		int row = Mth.clamp(line, 0, lines.size() - 1);
		int length = lines.get(row).length();
		anchorLine = row;
		anchorColumn = Mth.clamp(start, 0, length);
		caretLine = row;
		caretColumn = Mth.clamp(end, 0, length);
		caretListener.run();
	}

	public record Match(int line, int start, int end) {
	}

	/** Every place {@code query} appears, ignoring case; matches on a line don't overlap. */
	public List<Match> find(String query) {
		List<Match> matches = new ArrayList<>();
		if (query == null || query.isEmpty()) {
			return matches;
		}
		String needle = query.toLowerCase(java.util.Locale.ROOT);
		for (int line = 0; line < lines.size(); line++) {
			String haystack = lines.get(line).toLowerCase(java.util.Locale.ROOT);
			int from = 0;
			int at;
			while ((at = haystack.indexOf(needle, from)) >= 0) {
				matches.add(new Match(line, at, at + needle.length()));
				from = at + needle.length();
			}
		}
		return matches;
	}

	public String text() {
		return String.join("\n", lines);
	}

	public boolean dirty() {
		return dirty;
	}

	public void markClean() {
		dirty = false;
	}

	public int lineCount() {
		return lines.size();
	}

	public String line(int index) {
		return lines.get(index);
	}

	public int caretLine() {
		return caretLine;
	}

	public int caretColumn() {
		return caretColumn;
	}

	public boolean hasSelection() {
		return caretLine != anchorLine || caretColumn != anchorColumn;
	}

	public Position selectionStart() {
		if (caretLine < anchorLine || caretLine == anchorLine && caretColumn < anchorColumn) {
			return new Position(caretLine, caretColumn);
		}
		return new Position(anchorLine, anchorColumn);
	}

	public Position selectionEnd() {
		if (caretLine < anchorLine || caretLine == anchorLine && caretColumn < anchorColumn) {
			return new Position(anchorLine, anchorColumn);
		}
		return new Position(caretLine, caretColumn);
	}

	public String selectedText() {
		if (!hasSelection()) {
			return "";
		}
		Position start = selectionStart();
		Position end = selectionEnd();
		if (start.line == end.line) {
			return lines.get(start.line).substring(start.column, end.column);
		}
		StringBuilder builder = new StringBuilder(lines.get(start.line).substring(start.column)).append('\n');
		for (int i = start.line + 1; i < end.line; i++) {
			builder.append(lines.get(i)).append('\n');
		}
		builder.append(lines.get(end.line), 0, end.column);
		return builder.toString();
	}

	/** Moves the caret; with {@code selecting}, the selection stretches to it instead of collapsing. */
	public void setCaret(int line, int column, boolean selecting) {
		caretLine = Mth.clamp(line, 0, lines.size() - 1);
		caretColumn = Mth.clamp(column, 0, lines.get(caretLine).length());
		if (!selecting) {
			anchorLine = caretLine;
			anchorColumn = caretColumn;
		}
		caretListener.run();
	}

	/** Selects the word, or the run of symbols or spaces, around {@code column}. */
	public void selectWordAt(int line, int column) {
		int row = Mth.clamp(line, 0, lines.size() - 1);
		String text = lines.get(row);
		int at = Mth.clamp(column, 0, text.length());
		if (text.isEmpty()) {
			setCaret(row, 0, false);
			return;
		}
		int probe = at < text.length() ? at : at - 1;
		char kind = text.charAt(probe);
		int start = probe;
		int end = probe + 1;
		while (start > 0 && sameKind(text.charAt(start - 1), kind)) {
			start--;
		}
		while (end < text.length() && sameKind(text.charAt(end), kind)) {
			end++;
		}
		anchorLine = row;
		anchorColumn = start;
		caretLine = row;
		caretColumn = end;
		caretListener.run();
	}

	private static boolean sameKind(char character, char kind) {
		if (isWordCharacter(kind)) {
			return isWordCharacter(character);
		}
		if (Character.isWhitespace(kind)) {
			return Character.isWhitespace(character);
		}
		return !isWordCharacter(character) && !Character.isWhitespace(character);
	}

	/** Handles an editing or navigation key; {@code pageLines} is how far Page Up and Down move. */
	public boolean keyPressed(KeyEvent input, int pageLines) {
		Minecraft client = Minecraft.getInstance();
		if (input.isSelectAll()) {
			anchorLine = 0;
			anchorColumn = 0;
			caretLine = lines.size() - 1;
			caretColumn = lines.get(caretLine).length();
			caretListener.run();
			return true;
		}
		if (input.isCopy()) {
			client.keyboardHandler.setClipboard(selectedText());
			return true;
		}
		if (input.isCut()) {
			client.keyboardHandler.setClipboard(selectedText());
			if (hasSelection()) {
				pushUndo();
				deleteSelection();
				markDirty();
			}
			return true;
		}
		if (input.isPaste()) {
			insertText(client.keyboardHandler.getClipboard());
			return true;
		}
		if (hasControlOrSuper(input)) {
			if (input.key() == InputConstants.KEY_Z) {
				if (input.hasShiftDown()) {
					redo();
				} else {
					undo();
				}
				return true;
			}
			if (input.key() == InputConstants.KEY_Y) {
				redo();
				return true;
			}
			if (input.key() == InputConstants.KEY_BACKSPACE) {
				deleteWordBackward();
				return true;
			}
			if (input.key() == InputConstants.KEY_DELETE) {
				deleteWordForward();
				return true;
			}
			if (isToggleComment(input)) {
				toggleComment();
				return true;
			}
			if (input.key() == InputConstants.KEY_D && !input.hasShiftDown()) {
				duplicateLines();
				return true;
			}
			return false;
		}
		return switch (input.key()) {
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				newline();
				yield true;
			}
			case InputConstants.KEY_BACKSPACE -> {
				backspace();
				yield true;
			}
			case InputConstants.KEY_DELETE -> {
				deleteForward();
				yield true;
			}
			case InputConstants.KEY_LEFT -> {
				moveHorizontal(-1, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_RIGHT -> {
				moveHorizontal(1, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_UP -> {
				moveVertical(-1, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_DOWN -> {
				moveVertical(1, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_HOME -> {
				setCaret(caretLine, 0, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_END -> {
				setCaret(caretLine, lines.get(caretLine).length(), input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_PAGEUP -> {
				moveVertical(-pageLines, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_PAGEDOWN -> {
				moveVertical(pageLines, input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_TAB -> {
				if (input.hasShiftDown()) {
					outdentLines();
				} else if (hasSelection() && selectionStart().line() != selectionEnd().line()) {
					indentLines();
				} else {
					// Spaces up to the next indentation stop, so columns stay aligned.
					int column = hasSelection() ? selectionStart().column() : caretColumn;
					insertText(" ".repeat(INDENT_WIDTH - column % INDENT_WIDTH));
				}
				yield true;
			}
			default -> false;
		};
	}

	public boolean charTyped(CharacterEvent input) {
		if (!input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return false;
		}
		String typed = input.codepointAsString();
		if (typed.length() == 1 && typePaired(typed.charAt(0))) {
			return true;
		}
		insertText(typed);
		return true;
	}

	/** Ctrl+/: the / key where the layout has it, or Shift+7, which types / on Nordic and German layouts. */
	private static boolean isToggleComment(KeyEvent input) {
		return input.key() == InputConstants.KEY_SLASH
			|| VersionedInput.shortcutKey(input) == '/'
			|| (input.key() == InputConstants.KEY_7 && input.hasShiftDown());
	}

	private static char closerFor(char opener) {
		return switch (opener) {
			case '(' -> ')';
			case '[' -> ']';
			case '{' -> '}';
			case '"' -> '"';
			case '\'' -> '\'';
			default -> 0;
		};
	}

	private static boolean isPair(char opener, char closer) {
		return closerFor(opener) != 0 && closerFor(opener) == closer;
	}

	/**
	 * Brackets and quotes: typing one adds its closer, typing a closer that is already next steps over
	 * it, and a selection gets wrapped. Returns false to type the character normally.
	 */
	private boolean typePaired(char typed) {
		String line = lines.get(caretLine);
		char next = caretColumn < line.length() ? line.charAt(caretColumn) : 0;
		boolean quote = typed == '"' || typed == '\'';
		if (!hasSelection() && next == typed && (quote || typed == ')' || typed == ']' || typed == '}')) {
			setCaret(caretLine, caretColumn + 1, false);
			return true;
		}
		char closer = closerFor(typed);
		if (closer == 0) {
			return false;
		}
		if (hasSelection()) {
			if (selectionStart().line() != selectionEnd().line()) {
				return false;
			}
			Position start = selectionStart();
			Position end = selectionEnd();
			pushUndo();
			String text = lines.get(start.line());
			lines.set(start.line(), text.substring(0, start.column()) + typed + text.substring(start.column(), end.column()) + closer + text.substring(end.column()));
			select(start.line(), start.column() + 1, end.column() + 1);
			markDirty();
			return true;
		}
		String before = line.substring(0, caretColumn);
		if (quote) {
			// A third quote in a row opens a triple-quoted string: close it with three as well.
			if (before.endsWith(String.valueOf(typed).repeat(2)) && next != typed) {
				insertPair(String.valueOf(typed), String.valueOf(typed).repeat(3));
				return true;
			}
			// Don't pair an apostrophe in a word (don't) unless the word is a string prefix (f", rb').
			String word = before.substring(wordStartBefore(before, before.length()));
			if (!word.isEmpty() && isWordCharacter(word.charAt(word.length() - 1))
				&& !STRING_PREFIXES.contains(word.toLowerCase(java.util.Locale.ROOT))) {
				return false;
			}
		}
		// Only close when nothing would end up stuck inside the pair.
		if (next != 0 && !Character.isWhitespace(next) && ")]},:;".indexOf(next) < 0) {
			return false;
		}
		insertPair(String.valueOf(typed), String.valueOf(closer));
		return true;
	}

	private void insertPair(String opening, String closing) {
		pushUndo();
		String line = lines.get(caretLine);
		lines.set(caretLine, line.substring(0, caretColumn) + opening + closing + line.substring(caretColumn));
		setCaret(caretLine, caretColumn + opening.length(), false);
		markDirty();
	}

	/** Enter: keeps the line's indentation, adds a level after a colon, and opens up an empty bracket pair. */
	private void newline() {
		pushUndo();
		deleteSelection();
		String line = lines.get(caretLine);
		String before = line.substring(0, caretColumn);
		String after = line.substring(caretColumn);
		String leading = leadingWhitespace(line);
		String indentation = leading.substring(0, Math.min(caretColumn, leading.length()));
		String trimmed = before.stripTrailing();
		char last = trimmed.isEmpty() ? 0 : trimmed.charAt(trimmed.length() - 1);
		String stripped = after.stripLeading();
		if (closerFor(last) != 0 && last != '"' && last != '\'' && !stripped.isEmpty() && stripped.charAt(0) == closerFor(last)) {
			// foo(|) becomes three lines, with the caret indented on the middle one.
			insertRaw("\n" + indentation + INDENT + "\n" + indentation);
			setCaret(caretLine - 1, indentation.length() + INDENT_WIDTH, false);
		} else if (last == ':' || (closerFor(last) != 0 && last != '"' && last != '\'')) {
			insertRaw("\n" + indentation + INDENT);
		} else {
			insertRaw("\n" + indentation);
		}
		markDirty();
	}

	private static String leadingWhitespace(String line) {
		int end = 0;
		while (end < line.length() && (line.charAt(end) == ' ' || line.charAt(end) == '\t')) {
			end++;
		}
		return line.substring(0, end);
	}

	/** The lines the selection touches, or the caret's line; a selection ending at column 0 leaves that line out. */
	private int[] selectedLines() {
		if (!hasSelection()) {
			return new int[] {caretLine, caretLine};
		}
		Position start = selectionStart();
		Position end = selectionEnd();
		int last = end.line() > start.line() && end.column() == 0 ? end.line() - 1 : end.line();
		return new int[] {start.line(), last};
	}

	/** Moves the caret and the selection's anchor along when {@code delta} characters were added or removed at {@code column}. */
	private void shiftColumns(int line, int column, int delta) {
		if (caretLine == line && caretColumn >= column) {
			caretColumn = Math.max(column, caretColumn + delta);
		}
		if (anchorLine == line && anchorColumn >= column) {
			anchorColumn = Math.max(column, anchorColumn + delta);
		}
	}

	private void indentLines() {
		int[] range = selectedLines();
		pushUndo();
		for (int line = range[0]; line <= range[1]; line++) {
			if (lines.get(line).isBlank()) {
				continue;
			}
			lines.set(line, INDENT + lines.get(line));
			shiftColumns(line, 0, INDENT_WIDTH);
		}
		caretListener.run();
		markDirty();
	}

	/** Shift+Tab: removes one indentation level (up to four spaces, or a tab) from each line. */
	private void outdentLines() {
		int[] range = selectedLines();
		boolean changed = false;
		for (int line = range[0]; line <= range[1]; line++) {
			String text = lines.get(line);
			int remove = text.startsWith("\t") ? 1 : 0;
			if (remove == 0) {
				while (remove < INDENT_WIDTH && remove < text.length() && text.charAt(remove) == ' ') {
					remove++;
				}
			}
			if (remove == 0) {
				continue;
			}
			if (!changed) {
				pushUndo();
				changed = true;
			}
			lines.set(line, text.substring(remove));
			shiftColumns(line, 0, -remove);
		}
		if (changed) {
			caretListener.run();
			markDirty();
		}
	}

	/** Ctrl+/: comments the lines out with "# " at their shared indentation, or back in if they all are. */
	private void toggleComment() {
		int[] range = selectedLines();
		int indent = Integer.MAX_VALUE;
		boolean allCommented = true;
		for (int line = range[0]; line <= range[1]; line++) {
			String text = lines.get(line);
			if (text.isBlank()) {
				continue;
			}
			String leading = leadingWhitespace(text);
			indent = Math.min(indent, leading.length());
			allCommented &= text.startsWith("#", leading.length());
		}
		if (indent == Integer.MAX_VALUE) {
			return;
		}
		pushUndo();
		for (int line = range[0]; line <= range[1]; line++) {
			String text = lines.get(line);
			if (text.isBlank()) {
				continue;
			}
			if (allCommented) {
				int at = leadingWhitespace(text).length();
				int length = text.startsWith("# ", at) ? 2 : 1;
				lines.set(line, text.substring(0, at) + text.substring(at + length));
				shiftColumns(line, at, -length);
			} else {
				lines.set(line, text.substring(0, indent) + "# " + text.substring(indent));
				shiftColumns(line, indent, 2);
			}
		}
		caretListener.run();
		markDirty();
	}

	/** Ctrl+D: copies the current or selected lines below themselves and moves the caret onto the copy. */
	private void duplicateLines() {
		int[] range = selectedLines();
		int count = range[1] - range[0] + 1;
		pushUndo();
		lines.addAll(range[1] + 1, new ArrayList<>(lines.subList(range[0], range[1] + 1)));
		caretLine += count;
		anchorLine += count;
		caretListener.run();
		markDirty();
	}

	private static boolean hasControlOrSuper(KeyEvent input) {
		return input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0;
	}

	private void insertText(String text) {
		pushUndo();
		deleteSelection();
		insertRaw(text);
		markDirty();
	}

	/** Inserts {@code text} at the caret without recording undo or marking the text changed. */
	private void insertRaw(String text) {
		String[] split = (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
		String line = lines.get(caretLine);
		String before = line.substring(0, caretColumn);
		String after = line.substring(caretColumn);
		if (split.length == 1) {
			lines.set(caretLine, before + split[0] + after);
			setCaret(caretLine, caretColumn + split[0].length(), false);
		} else {
			lines.set(caretLine, before + split[0]);
			for (int i = 1; i < split.length; i++) {
				lines.add(caretLine + i, split[i]);
			}
			int newLine = caretLine + split.length - 1;
			lines.set(newLine, lines.get(newLine) + after);
			setCaret(newLine, split[split.length - 1].length(), false);
		}
	}

	private void backspace() {
		if (hasSelection()) {
			pushUndo();
			deleteSelection();
			markDirty();
			return;
		}
		if (caretColumn > 0) {
			pushUndo();
			String line = lines.get(caretLine);
			String before = line.substring(0, caretColumn);
			int remove = 1;
			int end = caretColumn;
			if (before.isBlank() && !before.contains("\t")) {
				// In the indentation, remove back to the previous indentation stop.
				remove = (caretColumn - 1) % INDENT_WIDTH + 1;
			} else if (caretColumn < line.length() && isPair(line.charAt(caretColumn - 1), line.charAt(caretColumn))) {
				// Between an empty pair, such as (|), remove both.
				end = caretColumn + 1;
			}
			lines.set(caretLine, line.substring(0, caretColumn - remove) + line.substring(end));
			setCaret(caretLine, caretColumn - remove, false);
			markDirty();
		} else if (caretLine > 0) {
			pushUndo();
			int previousLength = lines.get(caretLine - 1).length();
			lines.set(caretLine - 1, lines.get(caretLine - 1) + lines.remove(caretLine));
			setCaret(caretLine - 1, previousLength, false);
			markDirty();
		}
	}

	private void deleteWordBackward() {
		if (hasSelection()) {
			pushUndo();
			deleteSelection();
			markDirty();
			return;
		}
		if (caretColumn == 0) {
			if (caretLine > 0) {
				pushUndo();
				int previousLength = lines.get(caretLine - 1).length();
				lines.set(caretLine - 1, lines.get(caretLine - 1) + lines.remove(caretLine));
				setCaret(caretLine - 1, previousLength, false);
				markDirty();
			}
			return;
		}

		String line = lines.get(caretLine);
		int wordStart = wordStartBefore(line, caretColumn);
		if (wordStart == caretColumn) {
			wordStart = caretColumn - 1;
		}
		pushUndo();
		lines.set(caretLine, line.substring(0, wordStart) + line.substring(caretColumn));
		setCaret(caretLine, wordStart, false);
		markDirty();
	}

	private static int wordStartBefore(String line, int column) {
		int index = Math.min(column, line.length());
		while (index > 0 && Character.isWhitespace(line.charAt(index - 1))) {
			index--;
		}
		if (index <= 0) {
			return 0;
		}

		char previous = line.charAt(index - 1);
		if (isWordCharacter(previous)) {
			while (index > 0 && isWordCharacter(line.charAt(index - 1))) {
				index--;
			}
			return index;
		}

		while (index > 0 && !Character.isWhitespace(line.charAt(index - 1)) && !isWordCharacter(line.charAt(index - 1))) {
			index--;
		}
		return index;
	}

	private void deleteWordForward() {
		if (hasSelection()) {
			pushUndo();
			deleteSelection();
			markDirty();
			return;
		}
		String line = lines.get(caretLine);
		if (caretColumn >= line.length()) {
			if (caretLine < lines.size() - 1) {
				pushUndo();
				lines.set(caretLine, line + lines.remove(caretLine + 1));
				markDirty();
			}
			return;
		}

		int wordEnd = wordEndAfter(line, caretColumn);
		if (wordEnd == caretColumn) {
			wordEnd = caretColumn + 1;
		}
		pushUndo();
		lines.set(caretLine, line.substring(0, caretColumn) + line.substring(wordEnd));
		markDirty();
	}

	private static int wordEndAfter(String line, int column) {
		int index = Math.max(column, 0);
		while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
			index++;
		}
		if (index >= line.length()) {
			return line.length();
		}

		if (isWordCharacter(line.charAt(index))) {
			while (index < line.length() && isWordCharacter(line.charAt(index))) {
				index++;
			}
			return index;
		}

		while (index < line.length() && !Character.isWhitespace(line.charAt(index)) && !isWordCharacter(line.charAt(index))) {
			index++;
		}
		return index;
	}

	private static boolean isWordCharacter(char character) {
		return Character.isLetterOrDigit(character) || character == '_';
	}

	private void deleteForward() {
		if (hasSelection()) {
			pushUndo();
			deleteSelection();
			markDirty();
			return;
		}
		String line = lines.get(caretLine);
		if (caretColumn < line.length()) {
			pushUndo();
			lines.set(caretLine, line.substring(0, caretColumn) + line.substring(caretColumn + 1));
			markDirty();
		} else if (caretLine < lines.size() - 1) {
			pushUndo();
			lines.set(caretLine, line + lines.remove(caretLine + 1));
			markDirty();
		}
	}

	private void deleteSelection() {
		if (!hasSelection()) {
			return;
		}
		Position start = selectionStart();
		Position end = selectionEnd();
		if (start.line == end.line) {
			String line = lines.get(start.line);
			lines.set(start.line, line.substring(0, start.column) + line.substring(end.column));
		} else {
			String merged = lines.get(start.line).substring(0, start.column) + lines.get(end.line).substring(end.column);
			for (int i = end.line; i > start.line; i--) {
				lines.remove(i);
			}
			lines.set(start.line, merged);
		}
		setCaret(start.line, start.column, false);
	}

	private void moveHorizontal(int amount, boolean selecting) {
		if (amount < 0 && caretColumn > 0) {
			setCaret(caretLine, caretColumn - 1, selecting);
		} else if (amount < 0 && caretLine > 0) {
			setCaret(caretLine - 1, lines.get(caretLine - 1).length(), selecting);
		} else if (amount > 0 && caretColumn < lines.get(caretLine).length()) {
			setCaret(caretLine, caretColumn + 1, selecting);
		} else if (amount > 0 && caretLine < lines.size() - 1) {
			setCaret(caretLine + 1, 0, selecting);
		}
	}

	private void moveVertical(int amount, boolean selecting) {
		int targetLine = Mth.clamp(caretLine + amount, 0, lines.size() - 1);
		setCaret(targetLine, Math.min(caretColumn, lines.get(targetLine).length()), selecting);
	}

	private void markDirty() {
		if (!dirty) {
			dirty = true;
			dirtyListener.run();
		}
	}

	private void undo() {
		if (undoStack.isEmpty()) {
			return;
		}
		applyingHistory = true;
		redoStack.push(captureState());
		applyState(undoStack.pop());
		applyingHistory = false;
		markDirty();
	}

	private void redo() {
		if (redoStack.isEmpty()) {
			return;
		}
		applyingHistory = true;
		undoStack.push(captureState());
		applyState(redoStack.pop());
		applyingHistory = false;
		markDirty();
	}

	private void pushUndo() {
		version++;
		if (applyingHistory) {
			return;
		}
		EditorState current = captureState();
		if (!undoStack.isEmpty() && undoStack.peek().equals(current)) {
			return;
		}
		undoStack.push(current);
		while (undoStack.size() > MAX_UNDO) {
			undoStack.removeLast();
		}
		redoStack.clear();
	}

	private EditorState captureState() {
		return new EditorState(new ArrayList<>(lines), caretLine, caretColumn, anchorLine, anchorColumn);
	}

	private void applyState(EditorState state) {
		version++;
		lines.clear();
		lines.addAll(state.lines());
		caretLine = state.caretLine();
		caretColumn = state.caretColumn();
		anchorLine = state.anchorLine();
		anchorColumn = state.anchorColumn();
		caretListener.run();
	}

	private record EditorState(List<String> lines, int caretLine, int caretColumn, int anchorLine, int anchorColumn) {
		@Override
		public boolean equals(Object object) {
			if (!(object instanceof EditorState other)) {
				return false;
			}
			return caretLine == other.caretLine
				&& caretColumn == other.caretColumn
				&& anchorLine == other.anchorLine
				&& anchorColumn == other.anchorColumn
				&& Objects.equals(lines, other.lines);
		}

		@Override
		public int hashCode() {
			return Objects.hash(lines, caretLine, caretColumn, anchorLine, anchorColumn);
		}
	}
}
