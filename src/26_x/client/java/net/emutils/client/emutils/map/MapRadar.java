package net.emutils.client.emutils.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The entity radar (#224): the players, mobs and items around you on the minimap and the world map. Players
 * show their face, the rest a dot in their kind's color: red for hostile mobs, green for the others, blue for
 * your own pets, and yellow for items. Those above or below you are faded, so the ones on your level stand
 * out, as they do underground in the cave view, and those far above or below, like mobs in the caves under
 * you, are left out. Only what the game shows you is on it: entities
 * invisible to you, and spectators while you aren't one, are left out.
 */
public final class MapRadar {
	/** Further above or below you than this, an entity is faded... */
	private static final int LEVEL_BLOCKS = 6;
	/** ...and further than this, it's left out. */
	private static final int HEIGHT_LIMIT = 24;
	private static final float OTHER_LEVEL_ALPHA = 0.45F;
	private static final int OUTLINE = 0xE0101010;
	private static final int NAME_TAG = 0x99000000;
	/** For UI snapshots, which have no other player: you show as one too, beside your arrow. */
	private static boolean selfForSnapshot;

	/** What an entity is shown as. Later kinds are drawn over earlier ones. */
	enum Kind {
		ITEM(0xFFFFD84A, 4),
		FRIENDLY(0xFF6BE36B, 5),
		PET(0xFF6FC8FF, 5),
		HOSTILE(0xFFFF5555, 5),
		PLAYER(0xFFFFFFFF, 0);

		final int color;
		/** The dot's size in GUI pixels at the minimap's scale; players show a face instead. */
		final int dot;

		Kind(int color, int dot) {
			this.color = color;
			this.dot = dot;
		}
	}

	/** An entity on the radar, where it is this frame. */
	record Blip(Entity entity, Kind kind, double x, double y, double z) {
		/** Its name, for the world map's tooltip. */
		Component name() {
			return entity.getDisplayName();
		}
	}

	private MapRadar() {
	}

	/** Whether the radar shows anything at all. */
	static boolean enabled(@Nullable EMUtilsConfig config) {
		return config != null && config.mapRadar();
	}

	/**
	 * The entities to show within {@code reach} blocks of a point, players last so they're drawn on top. Only
	 * where you are: the level you're in.
	 */
	static List<Blip> blips(Minecraft client, double centerX, double centerZ, double reach, float partialTick) {
		return blips(client, centerX, centerZ, reach, partialTick, HEIGHT_LIMIT);
	}

	private static List<Blip> blips(Minecraft client, double centerX, double centerZ, double reach, float partialTick, double heightLimit) {
		ClientLevel level = client.level;
		LocalPlayer self = client.player;
		EMUtilsConfig config = EMUtilsClient.config();
		if (level == null || self == null || !enabled(config)) {
			return List.of();
		}
		List<Blip> blips = new ArrayList<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == self && !selfForSnapshot || !entity.isAlive()) {
				continue;
			}
			Kind kind = kind(entity, self, config);
			if (kind == null) {
				continue;
			}
			double x = entity.xo + (entity.getX() - entity.xo) * partialTick + (entity == self ? 3.0D : 0.0D);
			double z = entity.zo + (entity.getZ() - entity.zo) * partialTick;
			if (Math.abs(x - centerX) > reach || Math.abs(z - centerZ) > reach) {
				continue;
			}
			double y = entity.yo + (entity.getY() - entity.yo) * partialTick;
			if (Math.abs(y - self.getY()) > heightLimit) {
				continue;
			}
			blips.add(new Blip(entity, kind, x, y, z));
		}
		blips.sort(Comparator.comparingInt(blip -> blip.kind().ordinal()));
		return blips;
	}

	/** How an entity shows on the radar, or null when it doesn't. */
	private static @Nullable Kind kind(Entity entity, LocalPlayer self, EMUtilsConfig config) {
		// What the game hides from you stays hidden: invisible entities, unless your team sees them.
		if (entity.isInvisibleTo(self)) {
			return null;
		}
		if (entity instanceof Player player) {
			return config.mapRadarPlayers() && (!player.isSpectator() || self.isSpectator()) ? Kind.PLAYER : null;
		}
		if (entity instanceof Enemy) {
			return config.mapRadarHostile() ? Kind.HOSTILE : null;
		}
		if (entity instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(self)) {
			return config.mapRadarFriendly() ? Kind.PET : null;
		}
		if (entity instanceof Mob) {
			return config.mapRadarFriendly() ? Kind.FRIENDLY : null;
		}
		if (entity instanceof ItemEntity) {
			return config.mapRadarItems() ? Kind.ITEM : null;
		}
		return null;
	}

	/**
	 * Draws a blip centered on the current origin: a player's face {@code face} pixels wide with an outline,
	 * or a dot {@code scale} times its kind's size. {@code playerY} fades the ones on another level.
	 */
	static void draw(GuiGraphicsExtractor context, Font font, Blip blip, double playerY, int face, float scale, boolean names, float opacity) {
		float alpha = opacity * (Math.abs(blip.y() - playerY) > LEVEL_BLOCKS ? OTHER_LEVEL_ALPHA : 1.0F);
		if (blip.kind() == Kind.PLAYER && blip.entity() instanceof AbstractClientPlayer player) {
			int half = face / 2;
			context.fill(-half - 1, -half - 1, face - half + 1, face - half + 1, fade(OUTLINE, alpha));
			PlayerFaceExtractor.extractRenderState(context, player.getSkin(), -half, -half, face, fade(0xFFFFFFFF, alpha));
			if (names) {
				// On a dark tag, like the name over a player's head, so it reads on snow as well as on stone.
				Component name = player.getDisplayName();
				int width = UiText.width(font, name, UiText.Size.SMALL);
				int height = UiText.lineHeight(font, UiText.Size.SMALL);
				int top = face - half + 2;
				context.fill(-width / 2 - 2, top, width - width / 2 + 2, top + height + 2, fade(NAME_TAG, alpha));
				UiText.drawInkCentered(context, font, name, UiText.Size.SMALL, 0.0F, top + (height + 2) / 2.0F, fade(0xFFFFFFFF, alpha));
			}
			return;
		}
		int dot = Math.max(3, Math.round(blip.kind().dot * scale));
		int outer = dot + 2;
		UiShapes.circle(context, -outer / 2, -outer / 2, outer, fade(OUTLINE, alpha));
		UiShapes.circle(context, -dot / 2, -dot / 2, dot, fade(blip.kind().color, alpha));
	}

	/** For UI snapshots: shows you on the radar, as another player would be. */
	public static void showSelfForSnapshot(boolean show) {
		selfForSnapshot = show;
	}

	/** For UI snapshots: the kinds of what the radar shows of you and the entities named {@code name}, in drawing order. */
	public static String kindsForSnapshot(Minecraft client, String name) {
		if (client.player == null) {
			return "no player";
		}
		List<String> kinds = new ArrayList<>();
		for (Blip blip : blips(client, client.player.getX(), client.player.getZ(), 64.0D, 1.0F)) {
			Component custom = blip.entity().getCustomName();
			if (blip.entity() == client.player || custom != null && custom.getString().equals(name)) {
				kinds.add(blip.kind().name());
			}
		}
		return String.join(", ", kinds);
	}

	private static int fade(int color, float opacity) {
		int alpha = Math.round((color >>> 24) * Math.clamp(opacity, 0.0F, 1.0F));
		return alpha << 24 | (color & 0x00FFFFFF);
	}
}
