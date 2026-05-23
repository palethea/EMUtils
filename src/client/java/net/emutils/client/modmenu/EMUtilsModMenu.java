package net.emutils.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.emutils.client.gui.EMUtilsHubScreen;

public final class EMUtilsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return EMUtilsHubScreen::new;
	}
}
