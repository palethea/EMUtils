package net.emutils.client.emutils.minescript.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class PythonScriptEditorWidget extends AbstractWidget {
	private static final int LINE_HEIGHT = 12;
	private static final int PADDING = 4;
	private static final int LINE_NUMBER_WIDTH = 32;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_MARGIN = 2;
	private static final Identifier SCROLLER_TEXTURE = Identifier.withDefaultNamespace("widget/scroller");
	private static final Identifier SCROLLER_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/scroller_background");
	private static final Set<String> KEYWORDS = Set.of(
		"and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
		"if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
		"True", "False", "None"
	);

	private static final int MAX_UNDO = 100;

	private final Minecraft client;
	private final Font textRenderer;
	private final Runnable dirtyListener;
	private final List<String> lines = new ArrayList<>();
	private final Deque<EditorState> undoStack = new ArrayDeque<>();
	private final Deque<EditorState> redoStack = new ArrayDeque<>();
	private int caretLine;
	private int caretColumn;
	private int anchorLine;
	private int anchorColumn;
	private int firstVisibleLine;
	private int horizontalScroll;
	private boolean dirty;
	private boolean dragging;
	private boolean scrollbarDragging;
	private boolean applyingHistory;

	public PythonScriptEditorWidget(Minecraft client, int x, int y, int width, int height, Runnable dirtyListener) {
		super(x, y, width, height, Component.empty());
		this.client = client;
		this.textRenderer = client.font;
		this.dirtyListener = dirtyListener;
		setText("");
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
		caretLine = caretColumn = anchorLine = anchorColumn = firstVisibleLine = horizontalScroll = 0;
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

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(getX(), getY(), getX() + width, getY() + height, 0xDD101010);
		context.outline(getX(), getY(), width, height, isFocused() ? 0xFF69A7D8 : 0xFF444444);

		int gutterX = getX() + PADDING;
		int gutterRight = gutterX + LINE_NUMBER_WIDTH;
		int contentTop = getY() + PADDING;
		int contentBottom = getY() + height - PADDING;
		int contentX = gutterRight + 4;
		int scrollbarX = scrollbarX();
		context.fill(gutterX, contentTop, gutterRight, contentBottom, 0xDD0A0A0A);
		context.fill(gutterRight, contentTop, gutterRight + 1, contentBottom, 0xFF333333);

		int visibleLines = visibleLineCount();
		for (int i = 0; i < visibleLines; i++) {
			int lineIndex = firstVisibleLine + i;
			if (lineIndex >= lines.size()) {
				break;
			}
			renderLineNumber(context, lineIndex, gutterRight, contentTop + i * LINE_HEIGHT);
		}

		context.enableScissor(contentX, contentTop, scrollbarX, contentBottom);
		for (int i = 0; i < visibleLines; i++) {
			int lineIndex = firstVisibleLine + i;
			if (lineIndex >= lines.size()) {
				break;
			}

			int lineY = contentTop + i * LINE_HEIGHT;
			renderSelection(context, lineIndex, contentX - horizontalScroll, lineY);
			renderLine(context, lines.get(lineIndex), contentX - horizontalScroll, lineY);
		}
		context.disableScissor();

		if (isFocused()) {
			int caretX = contentX + textRenderer.width(lines.get(caretLine).substring(0, Math.min(caretColumn, lines.get(caretLine).length()))) - horizontalScroll;
			int caretY = contentTop + (caretLine - firstVisibleLine) * LINE_HEIGHT;
			if (caretY >= contentTop && caretY < contentBottom) {
				context.enableScissor(contentX, contentTop, scrollbarX, contentBottom);
				context.fill(caretX, caretY, caretX + 1, caretY + LINE_HEIGHT - 2, 0xFFFFFFFF);
				context.disableScissor();
			}
		}

		drawScrollbar(context, mouseX, mouseY);
	}

	private void renderLineNumber(GuiGraphicsExtractor context, int lineIndex, int gutterRight, int lineY) {
		String label = String.valueOf(lineIndex + 1);
		int color = lineIndex == caretLine && isFocused() ? 0xFFCCCCCC : 0xFF707070;
		int textX = gutterRight - textRenderer.width(label) - 2;
		context.text(textRenderer, label, textX, lineY, color, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, Component.literal("Python script editor"));
	}

	private static boolean hasControlOrSuper(KeyEvent input) {
		return input.hasControlDown() || (input.modifiers() & InputConstants.MOD_SUPER) != 0;
	}

	private void renderLine(GuiGraphicsExtractor context, String line, int x, int y) {
		int drawX = x;
		int index = 0;
		while (index < line.length()) {
			char c = line.charAt(index);
			if (c == '#') {
				drawX = drawSegment(context, line.substring(index), drawX, y, 0xFF6A9955);
				break;
			}
			if (c == '"' || c == '\'') {
				int end = index + 1;
				while (end < line.length() && line.charAt(end) != c) {
					if (line.charAt(end) == '\\') {
						end++;
					}
					end++;
				}
				end = Math.min(line.length(), end + 1);
				drawX = drawSegment(context, line.substring(index, end), drawX, y, 0xFFCE9178);
				index = end;
				continue;
			}
			if (Character.isDigit(c)) {
				int end = index + 1;
				while (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '.')) {
					end++;
				}
				drawX = drawSegment(context, line.substring(index, end), drawX, y, 0xFFB5CEA8);
				index = end;
				continue;
			}
			if (Character.isJavaIdentifierStart(c)) {
				int end = index + 1;
				while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
					end++;
				}
				String word = line.substring(index, end);
				int color = KEYWORDS.contains(word) ? 0xFF569CD6 : 0xFFD4D4D4;
				drawX = drawSegment(context, word, drawX, y, color);
				index = end;
				continue;
			}
			drawX = drawSegment(context, String.valueOf(c), drawX, y, 0xFFD4D4D4);
			index++;
		}
	}

	private int drawSegment(GuiGraphicsExtractor context, String segment, int x, int y, int color) {
		context.text(textRenderer, segment, x, y, color, false);
		return x + textRenderer.width(segment);
	}

	private void renderSelection(GuiGraphicsExtractor context, int lineIndex, int x, int y) {
		if (!hasSelection()) {
			return;
		}
		Position start = selectionStart();
		Position end = selectionEnd();
		if (lineIndex < start.line || lineIndex > end.line) {
			return;
		}
		String line = lines.get(lineIndex);
		int startColumn = lineIndex == start.line ? start.column : 0;
		int endColumn = lineIndex == end.line ? end.column : line.length();
		if (startColumn == endColumn && lineIndex != end.line) {
			endColumn = line.length();
		}
		int selectionStartX = x + textRenderer.width(line.substring(0, Math.min(startColumn, line.length())));
		int selectionEndX = x + textRenderer.width(line.substring(0, Math.min(endColumn, line.length())));
		context.fill(selectionStartX, y, Math.max(selectionStartX + 1, selectionEndX), y + LINE_HEIGHT - 2, 0x885A8FCE);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
		if (!isMouseOver(click.x(), click.y())) {
			return false;
		}
		if (scrollbarVisible() && isInScrollbar(click.x(), click.y())) {
			scrollbarDragging = true;
			scrollToMouseY(click.y());
			setFocused(true);
			return true;
		}
		setFocused(true);
		if (isInLineNumberGutter(click.x())) {
			int line = lineAtMouseY(click.y());
			setCaret(line, 0, click.hasShiftDown());
			dragging = true;
			return true;
		}
		moveCaretToMouse(click.x(), click.y(), click.hasShiftDown());
		dragging = true;
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
		if (scrollbarDragging) {
			scrollToMouseY(click.y());
			return true;
		}
		if (!dragging) {
			return false;
		}
		if (isInLineNumberGutter(click.x())) {
			int line = lineAtMouseY(click.y());
			setCaret(line, lines.get(line).length(), true);
			return true;
		}
		moveCaretToMouse(click.x(), click.y(), true);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent click) {
		dragging = false;
		scrollbarDragging = false;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		if (horizontalAmount != 0.0) {
			horizontalScroll = Mth.clamp(horizontalScroll - (int)(horizontalAmount * 12), 0, maxHorizontalScroll());
			return true;
		}
		firstVisibleLine = Mth.clamp(firstVisibleLine - (int)Math.signum(verticalAmount), 0, maxScrollLine());
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (!isFocused()) {
			return false;
		}
		if (input.isSelectAll()) {
			anchorLine = 0;
			anchorColumn = 0;
			caretLine = lines.size() - 1;
			caretColumn = lines.get(caretLine).length();
			ensureCaretVisible();
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
				moveVertical(-visibleLineCount(), input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_PAGEDOWN -> {
				moveVertical(visibleLineCount(), input.hasShiftDown());
				yield true;
			}
			case InputConstants.KEY_TAB -> {
				insertText(input.hasShiftDown() ? "" : "\t");
				yield true;
			}
			default -> false;
		};
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (!isFocused() || !input.isAllowedChatCharacter() || input.codepoint() == '\t') {
			return false;
		}
		insertText(input.codepointAsString());
		return true;
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

	private String selectedText() {
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

	private boolean hasSelection() {
		return caretLine != anchorLine || caretColumn != anchorColumn;
	}

	private Position selectionStart() {
		if (caretLine < anchorLine || caretLine == anchorLine && caretColumn < anchorColumn) {
			return new Position(caretLine, caretColumn);
		}
		return new Position(anchorLine, anchorColumn);
	}

	private Position selectionEnd() {
		if (caretLine < anchorLine || caretLine == anchorLine && caretColumn < anchorColumn) {
			return new Position(anchorLine, anchorColumn);
		}
		return new Position(caretLine, caretColumn);
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

	private void moveCaretToMouse(double mouseX, double mouseY, boolean selecting) {
		int line = lineAtMouseY(mouseY);
		String text = lines.get(line);
		int localX = (int)mouseX - contentX() + horizontalScroll;
		int column = 0;
		while (column < text.length() && textRenderer.width(text.substring(0, column + 1)) < localX) {
			column++;
		}
		setCaret(line, column, selecting);
	}

	private int lineAtMouseY(double mouseY) {
		return Mth.clamp(firstVisibleLine + (int)((mouseY - getY() - PADDING) / LINE_HEIGHT), 0, lines.size() - 1);
	}

	private boolean isInLineNumberGutter(double mouseX) {
		int gutterX = getX() + PADDING;
		return mouseX >= gutterX && mouseX < gutterX + LINE_NUMBER_WIDTH;
	}

	private int contentX() {
		return getX() + PADDING + LINE_NUMBER_WIDTH + 4;
	}

	private int scrollbarX() {
		return getRight() - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
	}

	private int codeAreaWidth() {
		return Math.max(1, scrollbarX() - contentX());
	}

	private boolean scrollbarVisible() {
		return maxScrollLine() > 0;
	}

	private int maxScrollLine() {
		return Math.max(0, lines.size() - visibleLineCount());
	}

	private int maxHorizontalScroll() {
		int widest = 0;
		for (String line : lines) {
			widest = Math.max(widest, textRenderer.width(line));
		}
		return Math.max(0, widest - codeAreaWidth());
	}

	private int scrollbarTrackTop() {
		return getY() + PADDING;
	}

	private int scrollbarTrackHeight() {
		return height - PADDING * 2;
	}

	private int getScrollbarThumbHeight() {
		int trackHeight = scrollbarTrackHeight();
		int contentHeight = Math.max(trackHeight, lines.size() * LINE_HEIGHT);
		return Mth.clamp(trackHeight * trackHeight / contentHeight, 8, trackHeight);
	}

	private int getScrollbarThumbY() {
		int maxScroll = maxScrollLine();
		if (maxScroll <= 0) {
			return scrollbarTrackTop();
		}
		int trackHeight = scrollbarTrackHeight();
		int thumbHeight = getScrollbarThumbHeight();
		return scrollbarTrackTop() + firstVisibleLine * (trackHeight - thumbHeight) / maxScroll;
	}

	private boolean isInScrollbar(double mouseX, double mouseY) {
		return mouseX >= scrollbarX() && mouseX <= scrollbarX() + SCROLLBAR_WIDTH
			&& mouseY >= scrollbarTrackTop() && mouseY < scrollbarTrackTop() + scrollbarTrackHeight();
	}

	private void scrollToMouseY(double mouseY) {
		int maxScroll = maxScrollLine();
		if (maxScroll <= 0) {
			firstVisibleLine = 0;
			return;
		}
		int trackHeight = scrollbarTrackHeight();
		int thumbHeight = getScrollbarThumbHeight();
		double ratio = Mth.clamp((mouseY - scrollbarTrackTop() - thumbHeight / 2.0) / (trackHeight - thumbHeight), 0.0, 1.0);
		firstVisibleLine = Mth.clamp((int)Math.round(ratio * maxScroll), 0, maxScroll);
	}

	private void drawScrollbar(GuiGraphicsExtractor context, int mouseX, int mouseY) {
		if (!scrollbarVisible()) {
			return;
		}
		int x = scrollbarX();
		int trackTop = scrollbarTrackTop();
		int trackHeight = scrollbarTrackHeight();
		int thumbHeight = getScrollbarThumbHeight();
		int thumbY = getScrollbarThumbY();
		context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_BACKGROUND_TEXTURE, x, trackTop, SCROLLBAR_WIDTH, trackHeight);
		context.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_TEXTURE, x, thumbY, SCROLLBAR_WIDTH, thumbHeight);
	}

	private void setCaret(int line, int column, boolean selecting) {
		caretLine = Mth.clamp(line, 0, lines.size() - 1);
		caretColumn = Mth.clamp(column, 0, lines.get(caretLine).length());
		if (!selecting) {
			anchorLine = caretLine;
			anchorColumn = caretColumn;
		}
		ensureCaretVisible();
	}

	private void ensureCaretVisible() {
		int visible = visibleLineCount();
		if (caretLine < firstVisibleLine) {
			firstVisibleLine = caretLine;
		} else if (caretLine >= firstVisibleLine + visible) {
			firstVisibleLine = Math.max(0, caretLine - visible + 1);
		}
		int caretPixel = textRenderer.width(lines.get(caretLine).substring(0, caretColumn));
		int availableWidth = codeAreaWidth();
		if (caretPixel - horizontalScroll > availableWidth) {
			horizontalScroll = caretPixel - availableWidth;
		} else if (caretPixel < horizontalScroll) {
			horizontalScroll = caretPixel;
		}
	}

	private int visibleLineCount() {
		return Math.max(1, (height - PADDING * 2) / LINE_HEIGHT);
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
		ensureCaretVisible();
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

	private record Position(int line, int column) {
	}
}
