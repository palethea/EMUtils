package net.emutils.client.emutils.waypoint;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class WaypointRenderer {
	private static final int BEACON_HEIGHT = 256;
	private static final double BEACON_WIDTH = 0.5;
	private static final Identifier BEAM_TEXTURE = Identifier.of("minecraft", "textures/entity/beacon_beam.png");

	private WaypointRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(WaypointRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
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

	private static void renderBeacon(WorldRenderContext context, Waypoint waypoint) {
		Camera camera = context.gameRenderer().getCamera();
		VertexConsumerProvider consumers = context.consumers();
		if (consumers == null) {
			return;
		}

		double cameraX = camera.getCameraPos().x;
		double cameraY = camera.getCameraPos().y;
		double cameraZ = camera.getCameraPos().z;

		float cx = (float) (waypoint.x() + 0.5 - cameraX);
		float cz = (float) (waypoint.z() + 0.5 - cameraZ);
		float baseY = (float) (waypoint.y() - cameraY);

		int color = waypoint.color();
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;

		float w = (float) BEACON_WIDTH;

		VertexConsumer buffer = consumers.getBuffer(RenderLayers.entityTranslucent(BEAM_TEXTURE, false));

		int segments = 8;
		float segmentHeight = BEACON_HEIGHT / (float) segments;
		int light = 15728880;

		for (int i = 0; i < segments; i++) {
			float y0 = baseY + i * segmentHeight;
			float y1 = baseY + (i + 1) * segmentHeight;
			float fade = 1.0F - (float) i / segments;
			int alpha = (int) (fade * 80);
			int packed = (alpha << 24) | (r << 16) | (g << 8) | b;

			float x0 = cx - w;
			float x1 = cx + w;
			float z0 = cz - w;
			float z1 = cz + w;

			float v0 = (float) i / segments;
			float v1 = (float) (i + 1) / segments;

			// North face (-Z)
			buffer.vertex(x0, y0, z0).color(packed).texture(0, v0).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y1, z0).color(packed).texture(0, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y1, z0).color(packed).texture(1, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y0, z0).color(packed).texture(1, v0).overlay(0, 0).light(light).normal(0, 1, 0);

			// South face (+Z)
			buffer.vertex(x1, y0, z1).color(packed).texture(0, v0).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y1, z1).color(packed).texture(0, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y1, z1).color(packed).texture(1, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y0, z1).color(packed).texture(1, v0).overlay(0, 0).light(light).normal(0, 1, 0);

			// West face (-X)
			buffer.vertex(x0, y0, z1).color(packed).texture(0, v0).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y1, z1).color(packed).texture(0, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y1, z0).color(packed).texture(1, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x0, y0, z0).color(packed).texture(1, v0).overlay(0, 0).light(light).normal(0, 1, 0);

			// East face (+X)
			buffer.vertex(x1, y0, z0).color(packed).texture(0, v0).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y1, z0).color(packed).texture(0, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y1, z1).color(packed).texture(1, v1).overlay(0, 0).light(light).normal(0, 1, 0);
			buffer.vertex(x1, y0, z1).color(packed).texture(1, v0).overlay(0, 0).light(light).normal(0, 1, 0);
		}
	}

	private static void renderWaypoint(
		WorldRenderContext context,
		MinecraftClient client,
		WaypointManager manager,
		Waypoint waypoint
	) {
		Camera camera = context.gameRenderer().getCamera();
		MatrixStack matrices = context.matrices();
		TextRenderer textRenderer = client.textRenderer;
		int opacity = EMUtilsClient.config().waypointOpacity();
		int waypointColor = waypoint.color();
		int textColor = withAlpha(waypointColor, opacity);
		int backgroundColor = (Math.max(0, Math.min(255, opacity * 64 / 100)) << 24);

		double cameraX = camera.getCameraPos().x;
		double cameraY = camera.getCameraPos().y;
		double cameraZ = camera.getCameraPos().z;
		double x = WaypointManager.renderX(waypoint) - cameraX;
		double y = WaypointManager.renderY(waypoint) - cameraY;
		double z = WaypointManager.renderZ(waypoint) - cameraZ;
		float scale = manager.labelScale(client, waypoint);

		Text title = Text.literal(waypoint.label());
		Text lore = Text.translatable(EMUtilsTexts.WAYPOINT_DISTANCE, manager.distanceBlocks(client, waypoint));

		matrices.push();
		matrices.translate(x, y, z);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		matrices.scale(-scale, -scale, scale);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		int light = 15728880;
		float titleWidth = -textRenderer.getWidth(title) / 2.0F;
		float loreWidth = -textRenderer.getWidth(lore) / 2.0F;

		textRenderer.draw(
			title,
			titleWidth,
			0.0F,
			textColor,
			false,
			matrix,
			context.consumers(),
			TextRenderer.TextLayerType.SEE_THROUGH,
			backgroundColor,
			light
		);
		textRenderer.draw(
			lore,
			loreWidth,
			10.0F,
			textColor,
			false,
			matrix,
			context.consumers(),
			TextRenderer.TextLayerType.SEE_THROUGH,
			backgroundColor,
			light
		);

		matrices.pop();
	}

	private static int withAlpha(int color, int opacityPercent) {
		int alpha = Math.max(0, Math.min(255, opacityPercent * 255 / 100));
		return (color & 0x00FFFFFF) | (alpha << 24);
	}
}
