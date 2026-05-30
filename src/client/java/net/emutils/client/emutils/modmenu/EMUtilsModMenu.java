package net.emutils.client.emutils.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.emutils.client.emutils.gui.SettingsChooserScreen;

public final class EMUtilsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SettingsChooserScreen::new;
	}
}
