package net.emutils.client.death;

import net.emutils.client.EMUtilsClient;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public final class DeathWaypointRenderer {
	private DeathWaypointRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(DeathWaypointRenderer::render);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		DeathWaypointManager manager = EMUtilsClient.deathWaypoint();
		if (!manager.shouldRender(client)) {
			return;
		}

		Camera camera = context.gameRenderer().getCamera();
		MatrixStack matrices = context.matrices();
		TextRenderer textRenderer = client.textRenderer;
		int opacity = EMUtilsClient.config().deathWaypointOpacity();
		int textColor = withAlpha(0xFFFFFF, opacity);
		int backgroundColor = (Math.max(0, Math.min(255, opacity * 64 / 100)) << 24);

		double cameraX = camera.getCameraPos().x;
		double cameraY = camera.getCameraPos().y;
		double cameraZ = camera.getCameraPos().z;
		double x = manager.renderX() - cameraX;
		double y = manager.renderY() - cameraY;
		double z = manager.renderZ() - cameraZ;
		float scale = manager.labelScale(client);

		Text title = Text.literal("Last Death");
		int distance = manager.distanceBlocks(client);
		Text lore = Text.literal(distance + " blocks away");

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
