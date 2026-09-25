package net.emutils.client.emutils.minescript.gui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;

/**
 * The text of a script being edited: its lines, the caret and selection, undo and redo, and every
 * editing key. It knows nothing about pixels, so the classic editor and the new UI's editor (#118)
 * share it and behave the same; they only lay the text out and turn mouse positions into positions.
 */
public final class ScriptTextBuffer {
	private static final int MAX_UNDO = 100;

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
			return false;
		}
		return switch (input.key()) {
			case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
				insertText("\n");
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
				insertText(input.hasShiftDown() ? "" : "\t");
				yield true;
			}
			default -> false;
		};
	}

	public boolean charTyped(CharacterEvent input) {
		if (!input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return false;
		}
		insertText(input.codepointAsString());
		return true;
	}

	private static boolean hasControlOrSuper(KeyEvent input) {
		return input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0;
	}

	private void insertText(String text) {
		pushUndo();
		deleteSelection();
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
		markDirty();
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
			lines.set(caretLine, line.substring(0, caretColumn - 1) + line.substring(caretColumn));
			setCaret(caretLine, caretColumn - 1, false);
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
