package net.emutils.client.emutils.profile;

import net.emutils.client.emutils.gui.hub.HubIcons;
import net.minecraft.resources.Identifier;

/** The icons a profile can be shown by (#89), saved by name. */
public enum ProfileIcon {
	GLOBE(HubIcons.GLOBE),
	HOUSE(HubIcons.HOUSE),
	SWORDS(HubIcons.SWORDS),
	PICKAXE(HubIcons.PICKAXE),
	HAMMER(HubIcons.HAMMER),
	SHIELD(HubIcons.SHIELD),
	CROWN(HubIcons.CROWN),
	TROPHY(HubIcons.TROPHY),
	STAR(HubIcons.STAR),
	HEART(HubIcons.HEART),
	ZAP(HubIcons.ZAP),
	FLAME(HubIcons.FLAME),
	GEM(HubIcons.GEM),
	ROCKET(HubIcons.ROCKET),
	TREE(HubIcons.TREE_PINE),
	GAMEPAD(HubIcons.GAMEPAD);

	private final Identifier texture;

	ProfileIcon(Identifier texture) {
		this.texture = texture;
	}

	public Identifier texture() {
		return texture;
	}

	public static ProfileIcon byName(String name) {
		for (ProfileIcon icon : values()) {
			if (icon.name().equalsIgnoreCase(name)) {
				return icon;
			}
		}
		return GLOBE;
	}
}
