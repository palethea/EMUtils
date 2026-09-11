package net.emutils.client.emutils.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;

public final class EMUtilsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new CustomHubScreen(parent);
	}
}
