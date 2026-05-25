package net.emutils.client.death;

import java.util.List;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.util.EMUtilsTexts;
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
		if (EMUtilsClient.zoom() != null && EMUtilsClient.zoom().shouldHideHud()) {
			return;
		}
		DeathWaypointManager manager = EMUtilsClient.deathWaypoint();
		if (!manager.shouldRender(client)) {
			return;
		}

		List<DeathLocation> locations = manager.deathsForCurrentWorld(client);
		for (DeathLocation location : locations) {
			renderWaypoint(context, client, manager, location);
		}
	}

	private static void renderWaypoint(
		WorldRenderContext context,
		MinecraftClient client,
		DeathWaypointManager manager,
		DeathLocation location
	) {
		Camera camera = context.gameRenderer().getCamera();
		MatrixStack matrices = context.matrices();
		TextRenderer textRenderer = client.textRenderer;
		int opacity = EMUtilsClient.config().deathWaypointOpacity();
		int textColor = withAlpha(0xFFFFFF, opacity);
		int backgroundColor = (Math.max(0, Math.min(255, opacity * 64 / 100)) << 24);

		double cameraX = camera.getCameraPos().x;
		double cameraY = camera.getCameraPos().y;
		double cameraZ = camera.getCameraPos().z;
		double x = DeathWaypointManager.renderX(location) - cameraX;
		double y = DeathWaypointManager.renderY(location) - cameraY;
		double z = DeathWaypointManager.renderZ(location) - cameraZ;
		float scale = manager.labelScale(client, location);

		int index = manager.labelIndex(client, location);
		Text title = index == 0
			? Text.translatable(EMUtilsTexts.DEATH_LABEL_LAST)
			: Text.translatable(EMUtilsTexts.DEATH_LABEL_NUMBERED, index + 1);
		Text lore = Text.translatable(EMUtilsTexts.DEATH_DISTANCE, manager.distanceBlocks(client, location));

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
