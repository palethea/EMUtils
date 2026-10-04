package net.emutils.client.emutils.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.ui.UiShapes;
import net.emutils.client.emutils.gui.ui.UiText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
 * show their face, and so do mobs ({@link MapMobIcons}), framed in their kind's color: red for hostile mobs,
 * green for the others and blue for your own pets; items, and mobs without a face, show a dot in it, yellow
 * for items. Those above or below you are faded, so the ones on your level stand
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
		/** Its name, for the world map's tooltip: a player's account name, a mob's without formatting codes. */
		Component name() {
			if (entity instanceof Player player) {
				return Component.literal(player.getScoreboardName());
			}
			String name = ChatFormatting.stripFormatting(entity.getDisplayName().getString());
			return Component.literal(name == null ? "" : name);
		}
	}

	/** A blip where it was drawn, for its name to go under it. */
	record Placed(Blip blip, float x, float y) {
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

	/**
	 * Whether a player is a server's NPC, like the ones in Hypixel's hub: drawn as players but not in the tab
	 * list, where every real player is.
	 */
	private static boolean npc(Player player, LocalPlayer self) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return player != self && connection != null && connection.getPlayerInfo(player.getUUID()) == null;
	}

	/** How an entity shows on the radar, or null when it doesn't. */
	private static @Nullable Kind kind(Entity entity, LocalPlayer self, EMUtilsConfig config) {
		// What the game hides from you stays hidden: invisible entities, unless your team sees them.
		if (entity.isInvisibleTo(self)) {
			return null;
		}
		if (entity instanceof Player player) {
			if (npc(player, self)) {
				return config.mapRadarFriendly() ? Kind.FRIENDLY : null;
			}
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
	 * Draws a blip centered on the current origin: a player's face {@code face} pixels wide with an outline (an
	 * NPC's framed like the other mobs), a mob's face with {@code mobFaces}, or a dot {@code scale} times its
	 * kind's size. {@code playerY} fades the ones on another level. Names go on after, with {@link #drawNames}.
	 */
	static void draw(GuiGraphicsExtractor context, Blip blip, double playerY, int face, float scale, boolean mobFaces, float opacity) {
		float alpha = alpha(blip, playerY, opacity);
		if (blip.entity() instanceof AbstractClientPlayer player) {
			int half = face / 2;
			context.fill(-half - 1, -half - 1, face - half + 1, face - half + 1, fade(blip.kind() == Kind.PLAYER ? OUTLINE : blip.kind().color, alpha));
			PlayerFaceExtractor.extractRenderState(context, player.getSkin(), -half, -half, face, fade(0xFFFFFFFF, alpha));
			return;
		}
		if (blip.kind() != Kind.ITEM && mobFaces && MapMobIcons.draw(context, blip.entity(), face - 1, fade(blip.kind().color, alpha), fade(0xFFFFFFFF, alpha))) {
			return;
		}
		int dot = Math.max(3, Math.round(blip.kind().dot * scale));
		int outer = dot + 2;
		UiShapes.circle(context, -outer / 2, -outer / 2, outer, fade(OUTLINE, alpha));
		UiShapes.circle(context, -dot / 2, -dot / 2, dot, fade(blip.kind().color, alpha));
	}

	private static float alpha(Blip blip, double playerY, float opacity) {
		return opacity * (Math.abs(blip.y() - playerY) > LEVEL_BLOCKS ? OTHER_LEVEL_ALPHA : 1.0F);
	}

	/**
	 * Puts the players' names under their faces, on a dark tag like the one over a player's head, in their
	 * team's color, which is how servers color ranks. Nearest the middle first; a name that would overlap one
	 * already there is left out, so a crowd stays readable.
	 */
	static void drawNames(GuiGraphicsExtractor context, Font font, List<Placed> placed, float centerX, float centerY, int face, double playerY, float opacity) {
		List<Placed> players = new ArrayList<>();
		for (Placed at : placed) {
			if (at.blip().kind() == Kind.PLAYER) {
				players.add(at);
			}
		}
		players.sort(Comparator.comparingDouble(at -> (at.x() - centerX) * (at.x() - centerX) + (at.y() - centerY) * (at.y() - centerY)));
		int height = UiText.lineHeight(font, UiText.Size.SMALL) + 2;
		List<float[]> taken = new ArrayList<>();
		for (Placed at : players) {
			Component name = at.blip().name();
			int width = UiText.width(font, name, UiText.Size.SMALL) + 4;
			float left = at.x() - width / 2.0F;
			float top = at.y() + face / 2.0F + 2.0F;
			float[] tag = {left, top, left + width, top + height};
			if (overlapsAny(tag, taken)) {
				continue;
			}
			taken.add(tag);
			float alpha = alpha(at.blip(), playerY, opacity);
			context.pose().pushMatrix();
			context.pose().translate(at.x(), top);
			context.fill(-width / 2, 0, width - width / 2, height, fade(NAME_TAG, alpha));
			int color = 0xFF000000 | at.blip().entity().getTeamColor();
			UiText.drawInkCentered(context, font, name, UiText.Size.SMALL, 0.0F, height / 2.0F, fade(color, alpha));
			context.pose().popMatrix();
		}
	}

	private static boolean overlapsAny(float[] rect, List<float[]> others) {
		for (float[] other : others) {
			if (rect[0] < other[2] && other[0] < rect[2] && rect[1] < other[3] && other[1] < rect[3]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * For UI snapshot checks: which of these name tags, nearest first, are drawn when they'd overlap; says
	 * what came out wrong, or returns an empty text.
	 */
	public static String nameOverlapForSnapshot() {
		List<float[]> taken = new ArrayList<>();
		float[][] tags = {{0, 0, 40, 10}, {30, 5, 70, 15}, {41, 0, 80, 10}, {0, 11, 40, 21}};
		StringBuilder drawn = new StringBuilder();
		for (int i = 0; i < tags.length; i++) {
			if (!overlapsAny(tags[i], taken)) {
				taken.add(tags[i]);
				drawn.append(i);
			}
		}
		return drawn.toString().equals("023") ? "" : "tags " + drawn + " were drawn instead of 0, 2 and 3";
	}

	/** For UI snapshots: the mobs named {@code name} around you that show a dot for want of a face. */
	public static String facelessForSnapshot(Minecraft client, String name) {
		if (client.level == null) {
			return "no level";
		}
		List<Entity> named = new ArrayList<>();
		for (Entity entity : client.level.entitiesForRendering()) {
			Component custom = entity.getCustomName();
			if (custom != null && custom.getString().equals(name)) {
				named.add(entity);
			}
		}
		return named.isEmpty() ? "none found" : String.join(", ", MapMobIcons.facelessForSnapshot(named));
	}

	/** For UI snapshots: draws a mob's radar face {@code size} pixels wide at the origin; false when it has none. */
	public static boolean drawFaceForSnapshot(GuiGraphicsExtractor context, Entity entity, int size) {
		return MapMobIcons.draw(context, entity, size, Kind.FRIENDLY.color, 0xFFFFFFFF);
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
