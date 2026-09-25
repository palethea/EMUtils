package net.emutils.client.emutils.minescript.gui;

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
import net.emutils.client.versioned.VersionedInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The classic Script Manager's editor; the editing itself lives in {@link ScriptTextBuffer}. */
public final class PythonScriptEditorWidget extends AbstractWidget {
	private static final int LINE_HEIGHT = 12;
	private static final int PADDING = 4;
	private static final int LINE_NUMBER_WIDTH = 32;
	private static final int SCROLLBAR_WIDTH = 6;
	private static final int SCROLLBAR_MARGIN = 2;
	private static final Identifier SCROLLER_TEXTURE = Identifier.withDefaultNamespace("widget/scroller");
	private static final Identifier SCROLLER_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("widget/scroller_background");

	private final Font textRenderer;
	private final ScriptTextBuffer buffer;
	private int firstVisibleLine;
	private int horizontalScroll;
	private boolean dragging;
	private boolean scrollbarDragging;

	public PythonScriptEditorWidget(Minecraft client, int x, int y, int width, int height, Runnable dirtyListener) {
		super(x, y, width, height, Component.empty());
		this.textRenderer = client.font;
		this.buffer = new ScriptTextBuffer(dirtyListener, this::ensureCaretVisible);
	}

	public void setText(String text) {
		buffer.setText(text);
		firstVisibleLine = horizontalScroll = 0;
	}

	public String text() {
		return buffer.text();
	}

	public boolean dirty() {
		return buffer.dirty();
	}

	public void markClean() {
		buffer.markClean();
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
			if (lineIndex >= buffer.lineCount()) {
				break;
			}
			renderLineNumber(context, lineIndex, gutterRight, contentTop + i * LINE_HEIGHT);
		}

		context.enableScissor(contentX, contentTop, scrollbarX, contentBottom);
		for (int i = 0; i < visibleLines; i++) {
			int lineIndex = firstVisibleLine + i;
			if (lineIndex >= buffer.lineCount()) {
				break;
			}

			int lineY = contentTop + i * LINE_HEIGHT;
			renderSelection(context, lineIndex, contentX - horizontalScroll, lineY);
			renderLine(context, buffer.line(lineIndex), contentX - horizontalScroll, lineY);
		}
		context.disableScissor();

		if (isFocused()) {
			String caretText = buffer.line(buffer.caretLine());
			int caretX = contentX + textRenderer.width(caretText.substring(0, Math.min(buffer.caretColumn(), caretText.length()))) - horizontalScroll;
			int caretY = contentTop + (buffer.caretLine() - firstVisibleLine) * LINE_HEIGHT;
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
		int color = lineIndex == buffer.caretLine() && isFocused() ? 0xFFCCCCCC : 0xFF707070;
		int textX = gutterRight - textRenderer.width(label) - 2;
		context.text(textRenderer, label, textX, lineY, color, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput builder) {
		builder.add(NarratedElementType.TITLE, Component.literal("Python script editor"));
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
				int color = PythonTokens.KEYWORDS.contains(word) ? 0xFF569CD6 : 0xFFD4D4D4;
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
		if (!buffer.hasSelection()) {
			return;
		}
		ScriptTextBuffer.Position start = buffer.selectionStart();
		ScriptTextBuffer.Position end = buffer.selectionEnd();
		if (lineIndex < start.line() || lineIndex > end.line()) {
			return;
		}
		String line = buffer.line(lineIndex);
		int startColumn = lineIndex == start.line() ? start.column() : 0;
		int endColumn = lineIndex == end.line() ? end.column() : line.length();
		if (startColumn == endColumn && lineIndex != end.line()) {
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
			buffer.setCaret(line, 0, click.hasShiftDown());
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
			buffer.setCaret(line, buffer.line(line).length(), true);
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
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		// This editor is not an EditBox, so it has to request text input itself.
		VersionedInput.setTextInputFocus(this, focused);
	}

	@Override
	public boolean keyPressed(KeyEvent input) {
		if (!isFocused()) {
			return false;
		}
		return buffer.keyPressed(input, visibleLineCount());
	}

	@Override
	public boolean charTyped(CharacterEvent input) {
		if (!isFocused()) {
			return false;
		}
		return buffer.charTyped(input);
	}

	private void moveCaretToMouse(double mouseX, double mouseY, boolean selecting) {
		int line = lineAtMouseY(mouseY);
		String text = buffer.line(line);
		int localX = (int)mouseX - contentX() + horizontalScroll;
		int column = 0;
		while (column < text.length() && textRenderer.width(text.substring(0, column + 1)) < localX) {
			column++;
		}
		buffer.setCaret(line, column, selecting);
	}

	private int lineAtMouseY(double mouseY) {
		return Mth.clamp(firstVisibleLine + (int)((mouseY - getY() - PADDING) / LINE_HEIGHT), 0, buffer.lineCount() - 1);
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
		return Math.max(0, buffer.lineCount() - visibleLineCount());
	}

	private int maxHorizontalScroll() {
		int widest = 0;
		for (int i = 0; i < buffer.lineCount(); i++) {
			widest = Math.max(widest, textRenderer.width(buffer.line(i)));
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
		int contentHeight = Math.max(trackHeight, buffer.lineCount() * LINE_HEIGHT);
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

	private void ensureCaretVisible() {
		int visible = visibleLineCount();
		int caretLine = buffer.caretLine();
		if (caretLine < firstVisibleLine) {
			firstVisibleLine = caretLine;
		} else if (caretLine >= firstVisibleLine + visible) {
			firstVisibleLine = Math.max(0, caretLine - visible + 1);
		}
		int caretPixel = textRenderer.width(buffer.line(caretLine).substring(0, buffer.caretColumn()));
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
}
