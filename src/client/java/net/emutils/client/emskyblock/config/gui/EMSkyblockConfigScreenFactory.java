package net.emutils.client.emskyblock.config.gui;

import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import net.emutils.client.emskyblock.config.EMSkyblockConfigManager;
import net.emutils.client.emhelpers.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

public final class EMSkyblockConfigScreenFactory {
	private EMSkyblockConfigScreenFactory() {
	}

	public static Screen create(@Nullable Screen parent) {
		var editor = EMSkyblockConfigManager.managed().getEditor();
		return new MoulConfigScreenComponent(
			Text.translatable(EMUtilsTexts.SCREEN_EMSKYBLOCK),
			new GuiContext(new GuiElementComponent(editor)),
			parent
			) {
				@Override
				public void close() {
					super.close();
				}

				@Override
				public void removed() {
					super.removed();
					EMSkyblockConfigManager.saveIfDirty();
				}
			};
	}
}
