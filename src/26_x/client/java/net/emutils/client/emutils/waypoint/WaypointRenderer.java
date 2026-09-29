package net.emutils.client.emutils.waypoint;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;

public final class WaypointRenderer {
	private static final int BEACON_HEIGHT = 256;
	private static final double BEACON_WIDTH = 0.08;
	private static final Identifier BEAM_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/beacon/beacon_beam.png");
	private static final Identifier LABEL_HUD_ID = Identifier.fromNamespaceAndPath("emutils", "waypoint_labels");

	private WaypointRenderer() {
	}

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(WaypointRenderer::render);
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, LABEL_HUD_ID, WaypointMarkerRenderer::render);
	}

	private static void render(LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		WaypointManager manager = EMUtilsClient.waypoint();
		if (!manager.shouldRender(client)) {
			return;
		}

		List<Waypoint> waypoints = manager.waypointsForCurrentWorld(client);
		for (Waypoint waypoint : waypoints) {
			if (waypoint.hidden()) {
				continue;
			}
			if (waypoint.beaconEnabled()) {
				renderBeacon(context, waypoint);
			}
		}
	}

	private static void renderBeacon(LevelRenderContext context, Waypoint waypoint) {
		Camera camera = net.emutils.client.emutils.compat.MinecraftClientCompat.mainCamera(context.gameRenderer());
		SubmitNodeCollector collector = context.submitNodeCollector();
		PoseStack matrices = context.poseStack();

		double cameraX = camera.position().x;
		double cameraY = camera.position().y;
		double cameraZ = camera.position().z;

		double x = waypoint.x() + 0.5 - cameraX;
		double y = waypoint.y() - cameraY;
		double z = waypoint.z() + 0.5 - cameraZ;

		int color = waypoint.color();
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		float width = (float) BEACON_WIDTH;

		int segments = 8;
		float segmentHeight = BEACON_HEIGHT / (float) segments;

		matrices.pushPose();
		try {
			matrices.translate(x, y, z);
			collector.submitCustomGeometry(matrices, RenderTypes.beaconBeam(BEAM_TEXTURE, true), (pose, buffer) -> {
				for (int i = 0; i < segments; i++) {
					float y0 = i * segmentHeight;
					float y1 = (i + 1) * segmentHeight;
					float fade = 1.0F - (float) i / segments;
					int alpha = (int) (fade * 80);
					int packed = (alpha << 24) | (r << 16) | (g << 8) | b;

					float v0 = (float) i / segments;
					float v1 = (float) (i + 1) / segments;

					addBeamVertex(buffer, pose, -width, y0, 0.0F, packed, 0.0F, v0);
					addBeamVertex(buffer, pose, -width, y1, 0.0F, packed, 0.0F, v1);
					addBeamVertex(buffer, pose, width, y1, 0.0F, packed, 1.0F, v1);
					addBeamVertex(buffer, pose, width, y0, 0.0F, packed, 1.0F, v0);

					addBeamVertex(buffer, pose, width, y0, 0.0F, packed, 0.0F, v0);
					addBeamVertex(buffer, pose, width, y1, 0.0F, packed, 0.0F, v1);
					addBeamVertex(buffer, pose, -width, y1, 0.0F, packed, 1.0F, v1);
					addBeamVertex(buffer, pose, -width, y0, 0.0F, packed, 1.0F, v0);

					addBeamVertex(buffer, pose, 0.0F, y0, -width, packed, 0.0F, v0);
					addBeamVertex(buffer, pose, 0.0F, y1, -width, packed, 0.0F, v1);
					addBeamVertex(buffer, pose, 0.0F, y1, width, packed, 1.0F, v1);
					addBeamVertex(buffer, pose, 0.0F, y0, width, packed, 1.0F, v0);

					addBeamVertex(buffer, pose, 0.0F, y0, width, packed, 0.0F, v0);
					addBeamVertex(buffer, pose, 0.0F, y1, width, packed, 0.0F, v1);
					addBeamVertex(buffer, pose, 0.0F, y1, -width, packed, 1.0F, v1);
					addBeamVertex(buffer, pose, 0.0F, y0, -width, packed, 1.0F, v0);
				}
			});
		} finally {
			matrices.popPose();
		}
	}

	private static void addBeamVertex(
		VertexConsumer buffer,
		PoseStack.Pose pose,
		float x,
		float y,
		float z,
		int color,
		float u,
		float v
	) {
		buffer.addVertex(pose, x, y, z)
			.setColor(color)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(15728880)
			.setNormal(pose, 0.0F, 1.0F, 0.0F);
	}
}
