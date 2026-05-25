package net.emutils.client.gui.hud;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.hud.layout.HudElementId;
import net.emutils.client.hud.layout.HudElementMetrics;
import net.emutils.client.hud.layout.HudLayoutManager;
import net.emutils.client.hud.layout.HudLayoutSnapping;
import net.emutils.client.hud.HudOverlayPlacement;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class HudLayoutEditorScreen extends Screen {
	private static final int OUTLINE_COLOR = 0xAA20F050;
	private static final int LABEL_BACKGROUND = 0xCC101725;

	private final Screen parent;
	private final Map<HudElementId, HudOverlayPlacement.PanelDimensions> dimensions = new EnumMap<>(HudElementId.class);
	@Nullable
	private HudElementId dragging;

	public HudLayoutEditorScreen(@Nullable Screen parent) {
		super(Text.translatable(EMUtilsTexts.SCREEN_HUD_LAYOUT_EDITOR));
		this.parent = parent;
	}

	@Override
	protected void init() {
		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_SAVE), button -> saveAndClose())
			.width(120)
			.position(width / 2 - 126, height - 28)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.translatable(EMUtilsTexts.HUD_LAYOUT_EDITOR_CANCEL), button -> cancelAndClose())
			.width(120)
			.position(width / 2 + 6, height - 28)
			.build());
		refreshDimensions();
		seedMissingDraftPositions();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
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

		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		for (HudElementId id : HudElementId.values()) {
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			HudOverlayPlacement.Position position = HudLayoutManager.draftPositions().get(id);
			if (panel == null || position == null || panel.width() <= 0 || panel.height() <= 0) {
				continue;
			}

			int x = position.x();
			int y = position.y();
			context.fill(x - 1, y - 1, x + panel.width() + 1, y + panel.height() + 1, OUTLINE_COLOR);
			HudElementMetrics.renderPreview(id, context, x, y, config, client);

			Text label = Text.translatable(id.labelKey());
			int labelWidth = textRenderer.getWidth(label) + 8;
			context.fill(x, y - 12, x + labelWidth, y - 1, LABEL_BACKGROUND);
			context.drawTextWithShadow(textRenderer, label, x + 4, y - 11, 0xFFFFFF);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			return true;
		}

		dragging = hitElement((int) click.x(), (int) click.y());
		return dragging != null;
	}

	@Override
	public boolean mouseReleased(Click click) {
		dragging = null;
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (dragging == null || client == null) {
			return super.mouseDragged(click, offsetX, offsetY);
		}

		EMUtilsConfig config = EMUtilsClient.config();
		HudOverlayPlacement.PanelDimensions panel = dimensions.get(dragging);
		if (config == null || panel == null) {
			return false;
		}

		int targetX = (int) click.x() - panel.width() / 2;
		int targetY = (int) click.y() - panel.height() / 2;
		HudLayoutSnapping.Bounds moving = new HudLayoutSnapping.Bounds(targetX, targetY, panel.width(), panel.height());
		List<HudLayoutSnapping.Bounds> others = new ArrayList<>();
		for (Map.Entry<HudElementId, HudOverlayPlacement.Position> entry : HudLayoutManager.draftPositions().entrySet()) {
			if (entry.getKey() == dragging) {
				continue;
			}

			HudOverlayPlacement.PanelDimensions otherPanel = dimensions.get(entry.getKey());
			if (otherPanel == null) {
				continue;
			}

			others.add(new HudLayoutSnapping.Bounds(
				entry.getValue().x(),
				entry.getValue().y(),
				otherPanel.width(),
				otherPanel.height()
			));
		}

		HudOverlayPlacement.Position snapped = HudLayoutSnapping.snap(
			new HudOverlayPlacement.Position(targetX, targetY),
			moving,
			others,
			width,
			height
		);
		HudLayoutManager.setDraftPosition(dragging, snapped.x(), snapped.y());
		return true;
	}

	private void refreshDimensions() {
		dimensions.clear();
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		for (HudElementId id : HudElementId.values()) {
			dimensions.put(id, HudElementMetrics.dimensions(id, config, client));
		}
	}

	private void seedMissingDraftPositions() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config == null || client == null) {
			return;
		}

		for (HudElementId id : HudElementId.values()) {
			if (HudLayoutManager.draftPositions().containsKey(id)) {
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
			HudLayoutManager.setDraftPosition(id, position.x(), position.y());
		}
	}

	@Nullable
	private HudElementId hitElement(int mouseX, int mouseY) {
		for (HudElementId id : HudElementId.values()) {
			HudOverlayPlacement.PanelDimensions panel = dimensions.get(id);
			HudOverlayPlacement.Position position = HudLayoutManager.draftPositions().get(id);
			if (panel == null || position == null) {
				continue;
			}

			if (mouseX >= position.x()
				&& mouseX < position.x() + panel.width()
				&& mouseY >= position.y()
				&& mouseY < position.y() + panel.height()) {
				return id;
			}
		}

		return null;
	}

	private void saveAndClose() {
		EMUtilsConfig config = EMUtilsClient.config();
		if (config != null) {
			HudLayoutManager.saveDraft(config);
		}
		closeEditor();
	}

	private void cancelAndClose() {
		HudLayoutManager.clearDraft();
		closeEditor();
	}

	private void closeEditor() {
		if (client != null) {
			client.setScreen(parent);
		}
	}
}
