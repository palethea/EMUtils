package net.emutils.client.emhelpers.hud.editor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutDraft;
import net.emutils.client.emhelpers.hud.layout.HudLayoutEditorChrome;
import net.emutils.client.emhelpers.hud.layout.HudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emhelpers.hud.layout.HudLayoutRegistry;
import net.emutils.client.emhelpers.hud.layout.HudLayoutSnapping;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorOverlay {
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;
	private static final int OPACITY_POPUP_WIDTH = 170;
	private static final int OPACITY_POPUP_HEIGHT = 52;
	private static final int OPACITY_SLIDER_HEIGHT = 8;

	private enum DragMode {
		MOVE,
		RESIZE
	}

	@Nullable
	private static HudLayoutEditorOverlay active;

	private final Map<HudElementId, HudOverlayPlacement.PanelDimensions> dimensions = new EnumMap<>(HudElementId.class);
	private final List<HudLayoutSnapping.GuideLine> activeGuides = new ArrayList<>();
	private final ButtonWidget resetButton;
	private final ButtonWidget saveButton;
	private final ButtonWidget cancelButton;
	@Nullable
	private HudElementId dragging;
	@Nullable
	private HudElementId opacityElement;
	private DragMode dragMode = DragMode.MOVE;
	private boolean draggingOpacity;

	private HudLayoutEditorOverlay(MinecraftClient client) {
		this.resetButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_RESET_ALL), button -> resetAllLayouts())
			.width(BUTTON_WIDTH)
			.build();
		this.saveButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_SAVE), button -> saveAndClose())
			.width(BUTTON_WIDTH)
			.build();
		this.cancelButton = ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_CANCEL), button -> cancelAndClose())
			.width(BUTTON_WIDTH)
			.build();
		refreshDimensions(client);
	}

	public static boolean open(MinecraftClient client) {
		if (active != null) {
			return true;
		}
		if (!HudLayoutManager.beginEditorSession(client)) {
			return false;
		}

		active = new HudLayoutEditorOverlay(client);
		return true;
	}

	public static boolean isActive() {
		return active != null;
	}

	public static void cancelActive() {
		if (active != null) {
			active.cancelAndClose();
		}
	}

	public static void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (active != null) {
			active.render0(context, mouseX, mouseY, delta);
		}
	}

	public static boolean handleMouseClicked(Click click) {
		return active != null && active.mouseClicked(click);
	}

	public static boolean handleMouseReleased(Click click) {
		if (active == null) {
			return false;
		}

		active.dragging = null;
		active.draggingOpacity = false;
		active.activeGuides.clear();
		return true;
	}

	public static boolean handleMouseDragged(Click click, double offsetX, double offsetY) {
		return active != null && active.mouseDragged(click, offsetX, offsetY);
	}

	public static boolean handleKeyPressed(KeyInput input) {
		if (active == null) {
			return false;
		}

		if (input.key() == InputUtil.GLFW_KEY_ESCAPE) {
			active.cancelAndClose();
		}
		return true;
	}

	private void render0(DrawContext context, int mouseX, int mouseY, @SuppressWarnings("unused") float delta) {
		MinecraftClient client = MinecraftClient.getInstance();
		EMUtilsConfig config = EMUtilsClient.config();
		if (client == null || config == null) {
			active = null;
			return;
		}

		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();
		context.fill(0, 0, width, height, 0x66000000);

		TextRenderer textRenderer = client.textRenderer;
		context.drawCenteredTextWithShadow(textRenderer, Text.translatable(EMUtilsTexts.SCREEN_HUD_LAYOUT_EDITOR), width / 2, 12, 0xFFFFFF);
		context.drawCenteredTextWithShadow(
			textRenderer,
			Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_HINT),
			width / 2,
			24,
			0xA0A0A0
		);

		HudLayoutEditorChrome.drawSnapGuides(context, activeGuides, width, height);

		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			HudElementId id = element.id();
			HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(id);
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (draft == null || panel == null || panel.width() <= 0 || panel.height() <= 0) {
				continue;
			}

			int x = draft.x();
			int y = draft.y();
			HudLayoutEditorChrome.drawOutline(context, x, y, panel.width(), panel.height());
			element.renderPreview(context, x, y, config, client, draft.scale());
			HudLayoutEditorChrome.drawOpacityButton(context, x, y);
			HudLayoutEditorChrome.drawResizeHandle(context, x, y, panel.width());
		}

		renderOpacityPopup(context, textRenderer);
		renderButtons(context, mouseX, mouseY, delta, width, height);
	}

	private void renderButtons(DrawContext context, int mouseX, int mouseY, float delta, int width, int height) {
		positionButtons(width, height);
		resetButton.render(context, mouseX, mouseY, delta);
		saveButton.render(context, mouseX, mouseY, delta);
		cancelButton.render(context, mouseX, mouseY, delta);
	}

	private boolean mouseClicked(Click click) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			active = null;
			return true;
		}

		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		positionButtons(width, height);
		if (resetButton.mouseClicked(click, false) || saveButton.mouseClicked(click, false) || cancelButton.mouseClicked(click, false)) {
			return true;
		}
		if (click.button() == 0) {
			if (handleOpacityPopupClick(mouseX, mouseY, width, height)) {
				return true;
			}
		}

		HudElementId hit = hitElement(mouseX, mouseY);
		if (click.button() == 1 && hit != null) {
			HudLayoutManager.setDraftScale(hit, 100);
			refreshDimensions(client);
			return true;
		}
		if (click.button() != 0 || hit == null) {
			return true;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(hit);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(hit);
		if (draft == null || panel == null) {
			return true;
		}

		if (HudLayoutEditorChrome.isOpacityButtonHit(mouseX, mouseY, draft.x(), draft.y())) {
			opacityElement = hit;
			draggingOpacity = false;
			activeGuides.clear();
			return true;
		}

		dragging = hit;
		dragMode = HudLayoutEditorChrome.isResizeHandleHit(mouseX, mouseY, draft.x(), draft.y(), panel.width())
			? DragMode.RESIZE
			: DragMode.MOVE;
		return true;
	}

	private boolean mouseDragged(Click click, double offsetX, double offsetY) {
		MinecraftClient client = MinecraftClient.getInstance();
		EMUtilsConfig config = EMUtilsClient.config();
		if (client == null || config == null) {
			active = null;
			return true;
		}

		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		if (draggingOpacity) {
			applyOpacityMouse((int) click.x(), width);
			return true;
		}
		if (dragging == null) {
			return true;
		}

		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(dragging);
		if (draft == null) {
			return true;
		}

		if (dragMode == DragMode.RESIZE) {
			int scale = HudLayoutRegistry.require(dragging).scaleFromResize(config, client, draft.x(), draft.y(), mouseX, mouseY);
			HudLayoutManager.setDraftScale(dragging, scale);
			refreshDimensions(client);
			activeGuides.clear();
			return true;
		}

		HudOverlayPlacement.PanelDimensions panel = dimensions.get(dragging);
		if (panel == null) {
			return true;
		}

		int targetX = mouseX - panel.width() / 2;
		int targetY = mouseY - panel.height() / 2;
		HudLayoutSnapping.Bounds moving = new HudLayoutSnapping.Bounds(targetX, targetY, panel.width(), panel.height());
		List<HudLayoutSnapping.Bounds> others = new ArrayList<>();
		for (Map.Entry<HudElementId, HudLayoutDraft> entry : HudLayoutManager.draftLayouts().entrySet()) {
			if (entry.getKey() == dragging) {
				continue;
			}

			HudOverlayPlacement.PanelDimensions otherPanel = dimensions.get(entry.getKey());
			HudLayoutDraft otherDraft = entry.getValue();
			if (otherPanel != null && otherDraft != null) {
				others.add(new HudLayoutSnapping.Bounds(otherDraft.x(), otherDraft.y(), otherPanel.width(), otherPanel.height()));
			}
		}

		HudOverlayPlacement.Position result;
		if (isSnapModifierDown(client)) {
			HudLayoutSnapping.SnapResult snapped = HudLayoutSnapping.snap(
				new HudOverlayPlacement.Position(targetX, targetY),
				moving,
				others,
				width,
				height
			);
			result = snapped.position();
			activeGuides.clear();
			activeGuides.addAll(snapped.guides());
		} else {
			result = new HudOverlayPlacement.Position(
				clamp(targetX, 0, Math.max(0, width - panel.width())),
				clamp(targetY, 0, Math.max(0, height - panel.height()))
			);
			activeGuides.clear();
		}

		HudLayoutManager.setDraftPosition(dragging, result.x(), result.y());
		return true;
	}

	private void refreshDimensions(MinecraftClient client) {
		dimensions.clear();
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return;
		}

		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			dimensions.put(element.id(), HudLayoutManager.dimensions(element.id(), config, client));
		}
	}

	@Nullable
	private HudElementId hitElement(int mouseX, int mouseY) {
		HudElementId hit = null;
		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			HudElementId id = element.id();
			HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(id);
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (draft == null || panel == null) {
				continue;
			}

			if (HudLayoutEditorChrome.isResizeHandleHit(mouseX, mouseY, draft.x(), draft.y(), panel.width())) {
				return id;
			}
			if (mouseX >= draft.x()
				&& mouseX < draft.x() + panel.width()
				&& mouseY >= draft.y()
				&& mouseY < draft.y() + panel.height()) {
				hit = id;
			}
		}
		return hit;
	}

	private void resetAllLayouts() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null) {
			active = null;
			return;
		}

		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return;
		}

		HudLayoutManager.resetAllDraftsToDefaults(client, config, width, height);
		refreshDimensions(client);
		activeGuides.clear();
		opacityElement = null;
	}

	private void saveAndClose() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null) {
			HudLayoutManager.saveDraft(config);
		}
		active = null;
	}

	private void cancelAndClose() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null) {
			HudLayoutManager.cancelEditor(config);
		} else {
			HudLayoutManager.clearDraft();
		}
		active = null;
	}

	private void renderOpacityPopup(DrawContext context, TextRenderer textRenderer) {
		if (opacityElement == null) {
			return;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		if (draft == null) {
			opacityElement = null;
			return;
		}

		int width = context.getScaledWindowWidth();
		int height = context.getScaledWindowHeight();
		int popupX = opacityPopupX(width);
		int popupY = opacityPopupY(height);
		context.fill(popupX + 2, popupY + 2, popupX + OPACITY_POPUP_WIDTH + 2, popupY + OPACITY_POPUP_HEIGHT + 2, 0x77000000);
		context.fill(popupX, popupY, popupX + OPACITY_POPUP_WIDTH, popupY + OPACITY_POPUP_HEIGHT, 0xEE101725);
		context.fill(popupX, popupY, popupX + OPACITY_POPUP_WIDTH, popupY + 1, 0xFF60A8FF);
		context.drawTextWithShadow(textRenderer, Text.literal("Opacity: " + draft.opacity() + "%"), popupX + 8, popupY + 8, 0xFFFFFF);

		int sliderX = popupX + 8;
		int sliderY = popupY + 32;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + OPACITY_SLIDER_HEIGHT, 0xFF2B3448);
		int fillWidth = Math.round(sliderWidth * draft.opacity() / 100.0F);
		context.fill(sliderX, sliderY, sliderX + fillWidth, sliderY + OPACITY_SLIDER_HEIGHT, 0xFF60A8FF);
		int thumbX = sliderX + fillWidth;
		context.fill(thumbX - 2, sliderY - 3, thumbX + 3, sliderY + OPACITY_SLIDER_HEIGHT + 3, 0xFFFFFFFF);
	}

	private boolean handleOpacityPopupClick(int mouseX, int mouseY, int width, int height) {
		if (opacityElement != null && isOpacitySliderHit(mouseX, mouseY, width, height)) {
			draggingOpacity = true;
			applyOpacityMouse(mouseX, width);
			return true;
		}

		if (opacityElement != null && isOpacityPopupHit(mouseX, mouseY, width, height)) {
			return true;
		}

		opacityElement = null;
		return false;
	}

	private boolean isOpacityPopupHit(int mouseX, int mouseY, int width, int height) {
		int popupX = opacityPopupX(width);
		int popupY = opacityPopupY(height);
		return inRect(mouseX, mouseY, popupX, popupY, OPACITY_POPUP_WIDTH, OPACITY_POPUP_HEIGHT);
	}

	private boolean isOpacitySliderHit(int mouseX, int mouseY, int width, int height) {
		int sliderX = opacityPopupX(width) + 8;
		int sliderY = opacityPopupY(height) + 28;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		return mouseX >= sliderX
			&& mouseX < sliderX + sliderWidth
			&& mouseY >= sliderY
			&& mouseY < sliderY + 20;
	}

	private void applyOpacityMouse(int mouseX, int width) {
		if (opacityElement == null) {
			return;
		}

		int sliderX = opacityPopupX(width) + 8;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		int value = Math.round(clamp(mouseX - sliderX, 0, sliderWidth) * 100.0F / sliderWidth);
		HudLayoutManager.setDraftOpacity(opacityElement, value);
	}

	private int opacityPopupX(int width) {
		if (opacityElement == null) {
			return 8;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(opacityElement);
		int preferred = draft == null || panel == null ? 8 : draft.x() + panel.width() + 8;
		return clamp(preferred, 8, Math.max(8, width - OPACITY_POPUP_WIDTH - 8));
	}

	private int opacityPopupY(int height) {
		if (opacityElement == null) {
			return 44;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		int preferred = draft == null ? 44 : draft.y();
		return clamp(preferred, 44, Math.max(44, height - OPACITY_POPUP_HEIGHT - 36));
	}

	private static boolean isSnapModifierDown(MinecraftClient client) {
		return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_CONTROL);
	}

	private static int resetX(int width) {
		return width / 2 - 162;
	}

	private static int saveX(int width) {
		return width / 2 - 52;
	}

	private static int cancelX(int width) {
		return width / 2 + 58;
	}

	private static int buttonY(int height) {
		return height - 28;
	}

	private void positionButtons(int width, int height) {
		int y = buttonY(height);
		resetButton.setDimensionsAndPosition(BUTTON_WIDTH, BUTTON_HEIGHT, resetX(width), y);
		saveButton.setDimensionsAndPosition(BUTTON_WIDTH, BUTTON_HEIGHT, saveX(width), y);
		cancelButton.setDimensionsAndPosition(BUTTON_WIDTH, BUTTON_HEIGHT, cancelX(width), y);
	}

	private static boolean inRect(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}
}
