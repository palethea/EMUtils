package net.emutils.client.emutils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * World-space lines drawn the way vanilla's F3+G chunk borders are (its gizmo renderer): positions are
 * made camera-relative in doubles before they become floats, and a line that runs behind the camera is
 * cut off just in front of it. Without that cut, the line shader projects the far end behind the camera
 * to the wrong side of the screen, so long lines around the player jump and flicker as the camera turns.
 */
public final class WorldLines {
	/** How far in front of the camera, in view space, a line is cut off; the same as vanilla's. */
	private static final float NEAR = -0.05F;

	private WorldLines() {
	}

	public record Line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
	}

	/**
	 * One renderer's lines as seen from the camera this frame. Its arrays are reused from frame to frame,
	 * so drawing thousands of lines doesn't make garbage every frame; each renderer keeps its own batch.
	 */
	public static final class Batch {
		private float[] positions = new float[0];
		private int[] colors = new int[0];
		private float[] widths = new float[0];
		private int count;
		private final Vector4f start = new Vector4f();
		private final Vector4f end = new Vector4f();
		private final Vector4f startView = new Vector4f();
		private final Vector4f endView = new Vector4f();

		/** Fills the batch with {@code lines} as seen from {@code camera}, for a pose stack not moved to the camera. */
		public Batch prepare(List<Line> lines, CameraRenderState camera) {
			if (colors.length < lines.size()) {
				int capacity = Math.max(lines.size(), colors.length * 2);
				positions = Arrays.copyOf(positions, capacity * 6);
				colors = Arrays.copyOf(colors, capacity);
				widths = Arrays.copyOf(widths, capacity);
			}
			double camX = camera.pos.x;
			double camY = camera.pos.y;
			double camZ = camera.pos.z;
			Matrix4f view = camera.viewRotationMatrix;
			count = 0;
			for (Line line : lines) {
				start.set((float) (line.x1() - camX), (float) (line.y1() - camY), (float) (line.z1() - camZ), 1.0F);
				end.set((float) (line.x2() - camX), (float) (line.y2() - camY), (float) (line.z2() - camZ), 1.0F);
				start.mul(view, startView);
				end.mul(view, endView);
				boolean startBehind = startView.z > NEAR;
				boolean endBehind = endView.z > NEAR;
				if (startBehind && endBehind) {
					continue;
				}
				if (startBehind || endBehind) {
					float denominator = endView.z - startView.z;
					if (Math.abs(denominator) < 1.0E-9F) {
						continue;
					}
					float t = Math.clamp((NEAR - startView.z) / denominator, 0.0F, 1.0F);
					if (startBehind) {
						start.lerp(end, t);
					} else {
						end.set(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t, start.z + (end.z - start.z) * t, 1.0F);
					}
				}
				int i = count * 6;
				positions[i] = start.x;
				positions[i + 1] = start.y;
				positions[i + 2] = start.z;
				positions[i + 3] = end.x;
				positions[i + 4] = end.y;
				positions[i + 5] = end.z;
				colors[count] = line.color();
				widths[count] = line.width();
				count++;
			}
			return this;
		}

		public boolean isEmpty() {
			return count == 0;
		}

		public void render(PoseStack.Pose pose, VertexConsumer buffer) {
			for (int line = 0; line < count; line++) {
				int i = line * 6;
				float dx = positions[i + 3] - positions[i];
				float dy = positions[i + 4] - positions[i + 1];
				float dz = positions[i + 5] - positions[i + 2];
				buffer.addVertex(pose, positions[i], positions[i + 1], positions[i + 2])
					.setNormal(pose, dx, dy, dz).setColor(colors[line]).setLineWidth(widths[line]);
				buffer.addVertex(pose, positions[i + 3], positions[i + 4], positions[i + 5])
					.setNormal(pose, dx, dy, dz).setColor(colors[line]).setLineWidth(widths[line]);
			}
		}
	}
}
