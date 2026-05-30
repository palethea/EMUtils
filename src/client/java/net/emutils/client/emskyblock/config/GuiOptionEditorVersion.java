package net.emutils.client.emskyblock.config;

import io.github.notenoughupdates.moulconfig.common.RenderContext;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import java.util.Locale;
import net.emutils.client.EMUtilsClient;
import net.fabricmc.loader.api.FabricLoader;

public class GuiOptionEditorVersion extends GuiOptionEditor {

	private static final String VERSION = FabricLoader.getInstance()
		.getModContainer(EMUtilsClient.MOD_ID)
		.map(c -> c.getMetadata().getVersion().getFriendlyString())
		.orElse("?");
	private static final StructuredText STATUS_TEXT = StructuredText.of("No updates");

	public GuiOptionEditorVersion(ProcessedOption option) {
		super(option);
	}

	@Override
	public void render(RenderContext context, int x, int y, int width) {
		var fr = context.getMinecraft().getDefaultFontRenderer();

		context.pushMatrix();
		context.translate(x + 10f, y);

		int adjustedWidth = width - 20;
		int buttonWidth = fr.getStringWidth(STATUS_TEXT.getText()) + 10;
		int buttonX = adjustedWidth - buttonWidth;
		context.drawColoredRect(buttonX, 10, buttonX + buttonWidth, 28, 0xFFFFFFFF);
		context.drawColoredRect(buttonX + 1, 11, buttonX + buttonWidth - 1, 27, 0xFF000000);
		context.drawString(
			fr,
			STATUS_TEXT,
			buttonX + 5,
			15,
			-1,
			true
		);

		int widthRemaining = adjustedWidth - buttonWidth - 10;

		context.scale(2f);
		context.drawStringCenteredScaledMaxWidth(
			StructuredText.of("§a" + VERSION),
			fr,
			widthRemaining / 4f,
			10f,
			true,
			widthRemaining / 2,
			-1
		);

		context.popMatrix();
	}

	@Override
	public int getHeight() {
		return 55;
	}

	@Override
	public boolean fulfillsSearch(String word) {
		String lower = word.toLowerCase(Locale.ROOT);
		return super.fulfillsSearch(word)
			|| lower.contains("version")
			|| lower.contains("update")
			|| lower.contains("download");
	}

	@Override
	public boolean mouseInput(int x, int y, int width, int mouseX, int mouseY) {
		return false;
	}

	@Override
	public boolean keyboardInput() {
		return false;
	}
}
