package net.emutils.client.emutils.debug;

import java.util.ArrayList;
import java.util.List;
import net.emutils.client.emutils.gui.ui.UiText;
import net.emutils.client.emutils.map.MapRadar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * For UI snapshots: every kind of mob's face on the entity radar (#224), large, in a grid with its name, so
 * the ones that don't come out well can be seen at a glance. The mobs are made on the client only, never
 * added to the world.
 */
final class RadarFaceGallery extends Screen {
	private static final int ICON = 18;
	private static final int CELL_WIDTH = 52;
	private static final int CELL_HEIGHT = 36;
	private final List<Entity> mobs = new ArrayList<>();
	private int faceless;

	RadarFaceGallery() {
		super(Component.literal("Radar faces"));
	}

	@Override
	protected void init() {
		mobs.clear();
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (type == EntityTypes.PLAYER || type == EntityTypes.ENDER_DRAGON || type == EntityTypes.WITHER) {
				continue;
			}
			try {
				Entity entity = type.create(client.level, EntitySpawnReason.COMMAND);
				if (entity instanceof LivingEntity && !(entity instanceof Player)) {
					// Never added to the world, so they get ids of their own, which no real entity has.
					entity.setId(-1000 - mobs.size());
					mobs.add(entity);
				}
			} catch (RuntimeException ignored) {
				// One that can't be made away from a server isn't shown.
			}
		}
	}

	/** How many mobs had no face and showed nothing, last frame. */
	int faceless() {
		return faceless;
	}

	int count() {
		return mobs.size();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, width, height, 0xFF6E8A4E);
		int columns = Math.max(1, (width - 8) / CELL_WIDTH);
		faceless = 0;
		for (int i = 0; i < mobs.size(); i++) {
			Entity mob = mobs.get(i);
			int x = 4 + (i % columns) * CELL_WIDTH + CELL_WIDTH / 2;
			int y = 6 + (i / columns) * CELL_HEIGHT + ICON / 2;
			context.pose().pushMatrix();
			context.pose().translate(x, y);
			if (!MapRadar.drawFaceForSnapshot(context, mob, ICON)) {
				faceless++;
				context.fill(-ICON / 2, -ICON / 2, ICON / 2, ICON / 2, 0xFF000000);
			}
			context.pose().popMatrix();
			String name = mob.getType().toShortString();
			Component label = Component.literal(name.length() > 13 ? name.substring(0, 12) + "…" : name);
			UiText.drawInkCentered(context, font, label, UiText.Size.SMALL, x, y + ICON / 2 + 6, 0xFFFFFFFF);
		}
	}
}
