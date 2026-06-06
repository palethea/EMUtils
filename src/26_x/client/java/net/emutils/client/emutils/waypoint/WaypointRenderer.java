package net.emutils.client.emutils.waypoint;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

public final class WaypointRenderer {
	private static final int BEACON_HEIGHT = 256;
	private static final double BEACON_WIDTH = 0.5;
	private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/beacon_beam.png");

	private WaypointRenderer() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WaypointRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (!manager.shouldRender(client)) {
			return;
		}

		List<Waypoint> waypoints = manager.waypointsForCurrentWorld(client);
		for (Waypoint waypoint : waypoints) {
			if (waypoint.beaconEnabled()) {
				renderBeacon(context, waypoint);
			}
			renderWaypoint(context, client, manager, waypoint);
		}
	}

	private static void renderBeacon(LevelRenderContext context, Waypoint waypoint) {
		// 26.x moved custom world rendering to the submit/extraction pipeline; keep labels active
		// while avoiding the old immediate-mode beacon buffer path.
	}

	private static void renderWaypoint(
		LevelRenderContext context,
		Minecraft client,
		WaypointManager manager,
		Waypoint waypoint
	) {
		Camera camera = context.gameRenderer().getMainCamera();
		PoseStack matrices = context.poseStack();
		MultiBufferSource consumers = context.bufferSource();
		Font textRenderer = client.font;
		int opacity = EMUtilsClient.config().waypointOpacity();
		int waypointColor = waypoint.color();
		int textColor = withAlpha(waypointColor, opacity);
		int backgroundColor = (Math.max(0, Math.min(255, opacity * 64 / 100)) << 24);

		double cameraX = camera.position().x;
		double cameraY = camera.position().y;
		double cameraZ = camera.position().z;
		double x = WaypointManager.renderX(waypoint) - cameraX;
		double y = WaypointManager.renderY(waypoint) - cameraY;
		double z = WaypointManager.renderZ(waypoint) - cameraZ;
		float scale = manager.labelScale(client, waypoint);

		Component title = Component.literal(waypoint.label());
		Component lore = Component.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, manager.distanceBlocks(client, waypoint));

		matrices.pushPose();
		matrices.translate(x, y, z);
		matrices.mulPose(Axis.YP.rotationDegrees(-camera.yaw()));
		matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
		matrices.scale(-scale, -scale, scale);

		Matrix4f matrix = matrices.last().pose();
		int light = 15728880;
		float titleWidth = -textRenderer.width(title) / 2.0F;
		float loreWidth = -textRenderer.width(lore) / 2.0F;

		textRenderer.drawInBatch(
			title,
			titleWidth,
			0.0F,
			textColor,
			false,
			matrix,
			consumers,
			Font.DisplayMode.SEE_THROUGH,
			backgroundColor,
			light
		);
		textRenderer.drawInBatch(
			lore,
			loreWidth,
			10.0F,
			textColor,
			false,
			matrix,
			consumers,
			Font.DisplayMode.SEE_THROUGH,
			backgroundColor,
			light
		);

		matrices.popPose();
	}

	private static int withAlpha(int color, int opacityPercent) {
		int alpha = Math.max(0, Math.min(255, opacityPercent * 255 / 100));
		return (color & 0x00FFFFFF) | (alpha << 24);
	}
}
