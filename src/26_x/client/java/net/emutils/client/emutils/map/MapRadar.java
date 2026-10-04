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
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * The entity radar (#224): the players, mobs and items around you on the minimap and the world map, in
 * groups ({@link RadarGroup}) that the Entity Radar settings show or hide, draw as a face or a dot, and frame
 * in a color each. Players show their skin's face and mobs theirs ({@link MapMobIcons}). Those above or below
 * you are faded, so the ones on your level stand out, as they do underground in the cave view, and those far
 * above or below, like mobs in the caves under you, are left out. Only what the game shows you is on it:
 * entities invisible to you, and spectators while you aren't one, are left out.
 */
public final class MapRadar {
	private static final float OTHER_LEVEL_ALPHA = 0.45F;
	private static final int OUTLINE = 0xE0101010;
	private static final int NAME_TAG = 0x99000000;
	private static final int NAME_SHADOW = 0xCC000000;
	/** A dot's size, as a share of the icon size; items' are smaller. */
	private static final float DOT_SHARE = 0.65F;
	private static final float ITEM_DOT_SHARE = 0.5F;
	/** For UI snapshots, which have no other player: you show as one too, beside your arrow. */
	private static boolean selfForSnapshot;

	/** An entity on the radar, where it is this frame. */
	record Blip(Entity entity, RadarGroup group, double x, double y, double z) {
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

	/** Whether the minimap shows the radar. */
	static boolean onMinimap(@Nullable EMUtilsConfig config) {
		return enabled(config) && config.mapRadarOnMinimap();
	}

	/** Whether the world map shows the radar where you are. */
	static boolean onWorldMap(@Nullable EMUtilsConfig config) {
		return enabled(config) && config.mapRadarOnWorldMap();
	}

	/**
	 * The entities to show within {@code reach} blocks of a point, the groups that matter most last, so they're
	 * drawn on top. Only where you are: the level you're in.
	 */
	static List<Blip> blips(Minecraft client, double centerX, double centerZ, double reach, float partialTick) {
		ClientLevel level = client.level;
		LocalPlayer self = client.player;
		EMUtilsConfig config = EMUtilsClient.config();
		if (level == null || self == null || !enabled(config)) {
			return List.of();
		}
		int heightLimit = config.mapRadarHideBlocks();
		List<Blip> blips = new ArrayList<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == self && !selfForSnapshot || !entity.isAlive()) {
				continue;
			}
			RadarGroup group = group(entity, self);
			if (group == null || !config.mapRadarShows(group)) {
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
			blips.add(new Blip(entity, group, x, y, z));
		}
		blips.sort(Comparator.comparingInt(blip -> blip.group().drawOrder()));
		return blips;
	}

	/** Whether a player is a server's NPC, like the ones in Hypixel's hub. */
	private static boolean npc(Player player, LocalPlayer self) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return player != self && connection != null && npc(player.getUUID(), connection.getPlayerInfo(player.getUUID()) != null);
	}

	/**
	 * Whether a player with this id is an NPC. Servers give NPCs ids of version 2, which no real player has
	 * (theirs are 4, or 3 on servers in offline mode), as Hypixel and the Citizens plugin do; that tells them
	 * apart from the moment they appear. Other NPCs are known by not being in the tab list, where every real
	 * player is, but only once the server takes them out of it: it lists them for a moment to send their skin.
	 */
	static boolean npc(java.util.UUID id, boolean inTabList) {
		return id.version() == 2 || !inTabList;
	}

	/**
	 * The group an entity is in, or null when the radar never shows it. Mobs go by the category the game spawns
	 * them in, so modded mobs land in the right group too: villagers and golems are the mobs it counts as
	 * neither creatures nor monsters.
	 */
	private static @Nullable RadarGroup group(Entity entity, LocalPlayer self) {
		// What the game hides from you stays hidden: invisible entities, unless your team sees them.
		if (entity.isInvisibleTo(self)) {
			return null;
		}
		if (entity instanceof Player player) {
			if (npc(player, self)) {
				return RadarGroup.NPCS;
			}
			return !player.isSpectator() || self.isSpectator() ? RadarGroup.PLAYERS : null;
		}
		if (entity instanceof Enemy) {
			return RadarGroup.HOSTILE;
		}
		if (entity instanceof TamableAnimal pet && pet.isTame() && pet.isOwnedBy(self)) {
			return RadarGroup.PETS;
		}
		if (entity instanceof Mob) {
			return switch (entity.getType().getCategory()) {
				case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> RadarGroup.WATER;
				case MISC -> RadarGroup.VILLAGERS;
				default -> RadarGroup.ANIMALS;
			};
		}
		if (entity instanceof ItemEntity) {
			return RadarGroup.ITEMS;
		}
		return null;
	}

	/**
	 * Draws a blip centered on the current origin, as its group's settings say: its face {@code size} pixels
	 * wide in a frame of the group's color, or a dot in that color. {@code playerY} fades the ones on another
	 * level. Names go on after, with {@link #drawNames}.
	 */
	static void draw(GuiGraphicsExtractor context, EMUtilsConfig config, Blip blip, double playerY, int size, float opacity) {
		RadarGroup group = blip.group();
		float alpha = alpha(config, blip, playerY, opacity);
		int color = config.mapRadarColor(group);
		boolean face = group.hasFaces() && config.mapRadarIcon(group) == RadarIcon.FACE;
		if (face && blip.entity() instanceof AbstractClientPlayer player) {
			int half = size / 2;
			context.fill(-half - 1, -half - 1, size - half + 1, size - half + 1, fade(color, alpha));
			PlayerFaceExtractor.extractRenderState(context, player.getSkin(), -half, -half, size, fade(0xFFFFFFFF, alpha));
			return;
		}
		if (face && MapMobIcons.draw(context, blip.entity(), size - 1, fade(color, alpha), fade(0xFFFFFFFF, alpha))) {
			return;
		}
		int dot = Math.max(3, Math.round(size * (group == RadarGroup.ITEMS ? ITEM_DOT_SHARE : DOT_SHARE)));
		int outer = dot + 2;
		UiShapes.circle(context, -outer / 2, -outer / 2, outer, fade(OUTLINE, alpha));
		UiShapes.circle(context, -dot / 2, -dot / 2, dot, fade(color | 0xFF000000, alpha));
	}

	private static float alpha(EMUtilsConfig config, Blip blip, double playerY, float opacity) {
		int fade = config.mapRadarFadeBlocks();
		return opacity * (fade > 0 && Math.abs(blip.y() - playerY) > fade ? OTHER_LEVEL_ALPHA : 1.0F);
	}

	/**
	 * Puts names under the faces of players, and of NPCs when that's on: on a dark tag like the one over a
	 * player's head, or with a shadow, in their team's color, which is how servers color ranks, or white.
	 * Nearest the middle first; with Hide Overlapping Names, a name that would overlap one already there is
	 * left out, so a crowd stays readable.
	 */
	static void drawNames(GuiGraphicsExtractor context, Font font, EMUtilsConfig config, List<Placed> placed, float centerX, float centerY, int size, double playerY, float opacity) {
		List<Placed> named = new ArrayList<>();
		for (Placed at : placed) {
			RadarGroup group = at.blip().group();
			if (group == RadarGroup.PLAYERS && config.mapRadarNames() || group == RadarGroup.NPCS && config.mapRadarNpcNames()) {
				named.add(at);
			}
		}
		named.sort(Comparator.comparingDouble(at -> (at.x() - centerX) * (at.x() - centerX) + (at.y() - centerY) * (at.y() - centerY)));
		int height = UiText.lineHeight(font, UiText.Size.SMALL) + 2;
		boolean background = config.mapRadarNameBackground();
		List<float[]> taken = new ArrayList<>();
		for (Placed at : named) {
			Component name = at.blip().name();
			int width = UiText.width(font, name, UiText.Size.SMALL) + 4;
			float left = at.x() - width / 2.0F;
			float top = at.y() + size / 2.0F + 2.0F;
			float[] tag = {left, top, left + width, top + height};
			if (config.mapRadarHideOverlappingNames() && overlapsAny(tag, taken)) {
				continue;
			}
			taken.add(tag);
			float alpha = alpha(config, at.blip(), playerY, opacity);
			int color = config.mapRadarNameColors() ? 0xFF000000 | at.blip().entity().getTeamColor() : 0xFFFFFFFF;
			context.pose().pushMatrix();
			context.pose().translate(at.x(), top);
			if (background) {
				context.fill(-width / 2, 0, width - width / 2, height, fade(NAME_TAG, alpha));
			} else {
				UiText.drawInkCentered(context, font, name, UiText.Size.SMALL, 0.5F, height / 2.0F + 0.5F, fade(NAME_SHADOW, alpha));
			}
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

	/** For UI snapshot checks: says which players with made-up ids were taken wrongly, or returns an empty text. */
	public static String npcRuleForSnapshot() {
		java.util.UUID npc = java.util.UUID.fromString("5f8a1c2e-3b4d-2e6f-8a9b-0c1d2e3f4a5b");
		java.util.UUID online = java.util.UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
		java.util.UUID offline = java.util.UUID.nameUUIDFromBytes("OfflinePlayer:Steve".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		if (!npc(npc, true)) {
			return "an NPC still in the tab list was taken for a player";
		}
		if (npc(online, true) || npc(offline, true)) {
			return "a real player was taken for an NPC";
		}
		return npc(online, false) ? "" : "a player missing from the tab list wasn't taken for an NPC";
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
		return MapMobIcons.draw(context, entity, size, RadarGroup.ANIMALS.defaultColor(), 0xFFFFFFFF);
	}

	/** For UI snapshots: shows you on the radar, as another player would be. */
	public static void showSelfForSnapshot(boolean show) {
		selfForSnapshot = show;
	}

	/** For UI snapshots: the groups of what the radar shows of you and the entities named {@code name}, in drawing order. */
	public static String kindsForSnapshot(Minecraft client, String name) {
		if (client.player == null) {
			return "no player";
		}
		List<String> kinds = new ArrayList<>();
		for (Blip blip : blips(client, client.player.getX(), client.player.getZ(), 64.0D, 1.0F)) {
			Component custom = blip.entity().getCustomName();
			if (blip.entity() == client.player || custom != null && custom.getString().equals(name)) {
				kinds.add(blip.group().name());
			}
		}
		return String.join(", ", kinds);
	}

	private static int fade(int color, float opacity) {
		int alpha = Math.round((color >>> 24) * Math.clamp(opacity, 0.0F, 1.0F));
		return alpha << 24 | (color & 0x00FFFFFF);
	}
}
