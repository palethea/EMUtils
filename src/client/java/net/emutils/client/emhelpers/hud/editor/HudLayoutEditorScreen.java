package net.emutils.client.emhelpers.hud.editor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emhelpers.hud.HudOverlayPlacement;
import net.emutils.client.emhelpers.hud.layout.HudElementId;
import net.emutils.client.emhelpers.hud.layout.HudLayoutDraft;
import net.emutils.client.emhelpers.hud.layout.HudLayoutEditorChrome;
import net.emutils.client.emhelpers.hud.layout.HudLayoutElement;
import net.emutils.client.emhelpers.hud.layout.HudLayoutManager;
import net.emutils.client.emhelpers.hud.layout.HudLayoutRegistry;
import net.emutils.client.emhelpers.hud.layout.HudLayoutSnapping;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorScreen extends Screen {
	private static final int OPACITY_POPUP_WIDTH = 170;
	private static final int OPACITY_POPUP_HEIGHT = 52;
	private static final int OPACITY_SLIDER_HEIGHT = 8;

	private enum DragMode {
		MOVE,
		RESIZE
	}

	private final Screen parent;
	private final Map<HudElementId, HudOverlayPlacement.PanelDimensions> dimensions = new EnumMap<>(HudElementId.class);
	private final List<HudLayoutSnapping.GuideLine> activeGuides = new ArrayList<>();
	@Nullable
	private HudElementId dragging;
	@Nullable
	private HudElementId opacityElement;
	private DragMode dragMode = DragMode.MOVE;
	private boolean draggingOpacity;
	private boolean renderingParent;

	public HudLayoutEditorScreen(@Nullable Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_HUD_LAYOUT_EDITOR));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_RESET_ALL), button -> resetAllLayouts())
			.width(100)
			.position(width / 2 - 162, height - 28)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_SAVE), button -> saveAndClose())
			.width(100)
			.position(width / 2 - 52, height - 28)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_CANCEL), button -> cancelAndClose())
			.width(100)
			.position(width / 2 + 58, height - 28)
			.build());
		refreshDimensions();
		seedMissingDraftLayouts();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		if (parent != null && !renderingParent) {
			renderingParent = true;
			try {
				parent.render(context, mouseX, mouseY, delta);
			} finally {
				renderingParent = false;
			}
		}
		context.fill(0, 0, width, height, 0x44000000);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
		context.drawCenteredTextWithShadow(
			textRenderer,
			Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_HINT),
			width / 2,
			24,
			0xA0A0A0
		);

		HudLayoutEditorChrome.drawSnapGuides(context, activeGuides, width, height);

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

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

		renderOpacityPopup(context);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		if (click.button() == 0 && handleOpacityPopupClick(mouseX, mouseY)) {
			return true;
		}

		HudElementId hit = hitElement(mouseX, mouseY);

		if (click.button() == 1 && hit != null) {
			HudLayoutManager.setDraftScale(hit, 100);
			refreshDimensions();
			return true;
		}

		if (click.button() != 0 || hit == null) {
			return false;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(hit);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(hit);
		if (draft == null || panel == null) {
			return false;
		}

		if (HudLayoutEditorChrome.isOpacityButtonHit(mouseX, mouseY, draft.x(), draft.y())) {
			opacityElement = hit;
			draggingOpacity = false;
			activeGuides.clear();
			return true;
		}

		dragging = hit;
		if (HudLayoutEditorChrome.isResizeHandleHit(mouseX, mouseY, draft.x(), draft.y(), panel.width())) {
			dragMode = DragMode.RESIZE;
		} else {
			dragMode = DragMode.MOVE;
		}

		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		dragging = null;
		draggingOpacity = false;
		activeGuides.clear();
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (draggingOpacity) {
			applyOpacityMouse((int) click.x());
			return true;
		}

		if (dragging == null || client == null) {
			return super.mouseDragged(click, offsetX, offsetY);
		}

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null) {
			return false;
		}

		int mouseX = (int) click.x();
		int mouseY = (int) click.y();
		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(dragging);
		if (draft == null) {
			return false;
		}

		if (dragMode == DragMode.RESIZE) {
			int scale = HudLayoutRegistry.require(dragging).scaleFromResize(config, client, draft.x(), draft.y(), mouseX, mouseY);
			HudLayoutManager.setDraftScale(dragging, scale);
			refreshDimensions();
			activeGuides.clear();
			return true;
		}

		HudOverlayPlacement.PanelDimensions panel = dimensions.get(dragging);
		if (panel == null) {
			return false;
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
			if (otherPanel == null || otherDraft == null) {
				continue;
			}

			others.add(new HudLayoutSnapping.Bounds(
				otherDraft.x(),
				otherDraft.y(),
				otherPanel.width(),
				otherPanel.height()
			));
		}

		HudOverlayPlacement.Position result;
		if (isSnapModifierDown()) {
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

	private void refreshDimensions() {
		dimensions.clear();
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			dimensions.put(element.id(), HudLayoutManager.dimensions(element.id(), config, client));
		}
	}

	private void seedMissingDraftLayouts() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		for (HudLayoutElement element : HudLayoutRegistry.all()) {
			HudElementId id = element.id();
			if (HudLayoutManager.draftLayouts().containsKey(id)) {
				continue;
			}

			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			if (panel == null || panel.width() <= 0 || panel.height() <= 0) {
				continue;
			}

			HudOverlayPlacement.Position position = HudLayoutManager.resolve(
				id,
				config,
				width,
				height,
				panel,
				client
			);
			int scale = HudLayoutManager.layoutScale(id, config);
			int opacity = HudLayoutManager.layoutOpacity(id, config);
			HudLayoutManager.setDraftLayout(id, position.x(), position.y(), scale, opacity);
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

	private static int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}

	private boolean isSnapModifierDown() {
		if (client == null) {
			return false;
		}

		return InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_LEFT_CONTROL)
			|| InputUtil.isKeyPressed(client.getWindow(), InputUtil.GLFW_KEY_RIGHT_CONTROL);
	}

	private void resetAllLayouts() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		HudLayoutManager.resetAllDraftsToDefaults(client, config, width, height);
		refreshDimensions();
		activeGuides.clear();
		opacityElement = null;
	}

	private void saveAndClose() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null) {
			HudLayoutManager.saveDraft(config);
		}
		closeEditor();
	}

	private void cancelAndClose() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null) {
			HudLayoutManager.cancelEditor(config);
		} else {
			HudLayoutManager.clearDraft();
		}
		closeEditor();
	}

	private void closeEditor() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private void renderOpacityPopup(DrawContext context) {
		if (opacityElement == null) {
			return;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		if (draft == null) {
			opacityElement = null;
			return;
		}

		int popupX = opacityPopupX();
		int popupY = opacityPopupY();
		context.fill(popupX + 2, popupY + 2, popupX + OPACITY_POPUP_WIDTH + 2, popupY + OPACITY_POPUP_HEIGHT + 2, 0x77000000);
		context.fill(popupX, popupY, popupX + OPACITY_POPUP_WIDTH, popupY + OPACITY_POPUP_HEIGHT, 0xEE101725);
		context.fill(popupX, popupY, popupX + OPACITY_POPUP_WIDTH, popupY + 1, 0xFF60A8FF);
		context.drawTextWithShadow(
			textRenderer,
			Text.literal("Opacity: " + draft.opacity() + "%"),
			popupX + 8,
			popupY + 8,
			0xFFFFFF
		);

		int sliderX = popupX + 8;
		int sliderY = popupY + 32;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + OPACITY_SLIDER_HEIGHT, 0xFF2B3448);
		int fillWidth = Math.round(sliderWidth * draft.opacity() / 100.0F);
		context.fill(sliderX, sliderY, sliderX + fillWidth, sliderY + OPACITY_SLIDER_HEIGHT, 0xFF60A8FF);
		int thumbX = sliderX + fillWidth;
		context.fill(thumbX - 2, sliderY - 3, thumbX + 3, sliderY + OPACITY_SLIDER_HEIGHT + 3, 0xFFFFFFFF);
	}

	private boolean handleOpacityPopupClick(int mouseX, int mouseY) {
		if (opacityElement != null && isOpacitySliderHit(mouseX, mouseY)) {
			draggingOpacity = true;
			applyOpacityMouse(mouseX);
			return true;
		}

		if (opacityElement != null && isOpacityPopupHit(mouseX, mouseY)) {
			return true;
		}

		opacityElement = null;
		return false;
	}

	private boolean isOpacityPopupHit(int mouseX, int mouseY) {
		int popupX = opacityPopupX();
		int popupY = opacityPopupY();
		return mouseX >= popupX
			&& mouseX < popupX + OPACITY_POPUP_WIDTH
			&& mouseY >= popupY
			&& mouseY < popupY + OPACITY_POPUP_HEIGHT;
	}

	private boolean isOpacitySliderHit(int mouseX, int mouseY) {
		int sliderX = opacityPopupX() + 8;
		int sliderY = opacityPopupY() + 28;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		return mouseX >= sliderX
			&& mouseX < sliderX + sliderWidth
			&& mouseY >= sliderY
			&& mouseY < sliderY + 20;
	}

	private void applyOpacityMouse(int mouseX) {
		if (opacityElement == null) {
			return;
		}

		int sliderX = opacityPopupX() + 8;
		int sliderWidth = OPACITY_POPUP_WIDTH - 16;
		int value = Math.round(clamp(mouseX - sliderX, 0, sliderWidth) * 100.0F / sliderWidth);
		HudLayoutManager.setDraftOpacity(opacityElement, value);
	}

	private int opacityPopupX() {
		if (opacityElement == null) {
			return 8;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(opacityElement);
		int preferred = draft == null || panel == null ? 8 : draft.x() + panel.width() + 8;
		return clamp(preferred, 8, Math.max(8, width - OPACITY_POPUP_WIDTH - 8));
	}

	private int opacityPopupY() {
		if (opacityElement == null) {
			return 44;
		}

		HudLayoutDraft draft = HudLayoutManager.draftLayouts().get(opacityElement);
		int preferred = draft == null ? 44 : draft.y();
		return clamp(preferred, 44, Math.max(44, height - OPACITY_POPUP_HEIGHT - 36));
	}
}
