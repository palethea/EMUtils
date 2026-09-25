package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

/**
 * Renders settings UI text with FreeType into textures, one per string, at the physical pixel size.
 *
 * <p>Minecraft's text shader discards pixels under 10% alpha and blends glyph coverage without any
 * contrast adjustment, which chips the soft edges off curves and makes regular-weight text look thin.
 * Here glyphs are placed at fractional pixel positions with light (vertical-only) hinting, their
 * coverage gets a mild boost like browsers apply to text, and the result is drawn as an ordinary
 * texture, so nothing is discarded.
 */
public final class UiFontRenderer {
	/** Coverage curve: values below 1 thicken anti-aliased edges slightly. */
	private static final double COVERAGE_GAMMA = 0.8;
	/** Enough for a screen of settings plus the visible code in the script editor, token by token. */
	private static final int MAX_CACHED_STRINGS = 2000;
	private static final int PADDING = 1;
	/**
	 * FT_LOAD_TARGET_LIGHT: light, vertical-only hinting. Computed from the render mode because LWJGL 3.4.1
	 * (Minecraft 26.2) names the constant FT_FT_LOAD_TARGET_LIGHT.
	 */
	private static final int LOAD_TARGET_LIGHT = (FreeType.FT_RENDER_MODE_LIGHT & 15) << 16;

	private static boolean initialized;
	private static boolean available;
	private static long library;
	private static final Map<Weight, FT_Face> FACES = new EnumMap<>(Weight.class);
	private static final Map<String, Float> WIDTHS = new HashMap<>();
	private static final LinkedHashMap<String, Rendered> STRINGS = new LinkedHashMap<>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Rendered> eldest) {
			if (size() > MAX_CACHED_STRINGS) {
				Minecraft.getInstance().getTextureManager().release(eldest.getValue().texture());
				return true;
			}
			return false;
		}
	};
	private static int texturesMade;

	private UiFontRenderer() {
	}

	public enum Weight {
		SEMIBOLD("nunito_semibold.ttf", 0.705F),
		EXTRABOLD("nunito_extrabold.ttf", 0.705F),
		BLACK("nunito_black.ttf", 0.705F),
		/** JetBrains Mono, for code such as the script editor's (#118). */
		MONO("jetbrains_mono_regular.ttf", 0.73F);

		private final String file;
		private final float capHeight;

		Weight(String file, float capHeight) {
			this.file = file;
			this.capHeight = capHeight;
		}

		/** The font's cap height as a fraction of its em size. */
		public float capHeight() {
			return capHeight;
		}
	}

	/** A string rendered to a white texture; {@code baseline} is the baseline's row in the texture. */
	public record Rendered(Identifier texture, int width, int height, int baseline, int left) {
	}

	public static boolean available() {
		if (!initialized) {
			initialized = true;
			available = initialize();
		}
		return available;
	}

	private static boolean initialize() {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer pointer = stack.mallocPointer(1);
			if (FreeType.FT_Init_FreeType(pointer) != 0) {
				return false;
			}
			library = pointer.get(0);
			for (Weight weight : Weight.values()) {
				FT_Face face = loadFace(weight, stack);
				if (face == null) {
					return false;
				}
				FACES.put(weight, face);
			}
			return true;
		} catch (IOException | LinkageError exception) {
			EMUtilsClient.LOGGER.warn("EMUtils UI font renderer unavailable; using Minecraft's text rendering", exception);
			return false;
		}
	}

	private static @Nullable FT_Face loadFace(Weight weight, MemoryStack stack) throws IOException {
		Identifier location = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "font/" + weight.file);
		byte[] bytes;
		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
			bytes = stream.readAllBytes();
		}
		// FreeType reads the font from this buffer for as long as the face lives, which is the whole game.
		ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
		data.put(bytes).flip();
		PointerBuffer facePointer = stack.mallocPointer(1);
		if (FreeType.FT_New_Memory_Face(library, data, 0L, facePointer) != 0) {
			MemoryUtil.memFree(data);
			return null;
		}
		return FT_Face.create(facePointer.get(0));
	}

	/** Width of {@code text} in physical pixels at {@code pixelSize} pixels per em. */
	public static float measure(Weight weight, float pixelSize, String text) {
		String key = weight.ordinal() + "|" + pixelSize + "|" + text;
		Float cached = WIDTHS.get(key);
		if (cached != null) {
			return cached;
		}
		if (WIDTHS.size() > 4000) {
			WIDTHS.clear();
		}
		FT_Face face = FACES.get(weight);
		setSize(face, pixelSize);
		float pen = 0.0F;
		for (int i = 0; i < text.length(); ) {
			int codepoint = text.codePointAt(i);
			i += Character.charCount(codepoint);
			if (FreeType.FT_Load_Glyph(face, FreeType.FT_Get_Char_Index(face, codepoint), LOAD_TARGET_LIGHT) == 0) {
				pen += face.glyph().linearHoriAdvance() / 65536.0F;
			}
		}
		WIDTHS.put(key, pen);
		return pen;
	}

	public static Rendered render(Weight weight, float pixelSize, String text) {
		String key = weight.ordinal() + "|" + pixelSize + "|" + text;
		Rendered cached = STRINGS.get(key);
		if (cached != null) {
			return cached;
		}

		FT_Face face = FACES.get(weight);
		setSize(face, pixelSize);
		int ascender = (int) Math.ceil(face.size().metrics().ascender() / 64.0);
		int descender = (int) Math.ceil(-face.size().metrics().descender() / 64.0);
		int width = (int) Math.ceil(measure(weight, pixelSize, text)) + PADDING * 2 + 2;
		int height = ascender + descender + PADDING * 2;
		int baseline = PADDING + ascender;
		NativeImage image = new NativeImage(Math.max(1, width), Math.max(1, height), true);

		try (MemoryStack stack = MemoryStack.stackPush()) {
			FT_Vector shift = FT_Vector.malloc(stack);
			float pen = PADDING;
			for (int i = 0; i < text.length(); ) {
				int codepoint = text.codePointAt(i);
				i += Character.charCount(codepoint);
				int whole = (int) Math.floor(pen);
				// Shift each glyph by the pen's fraction so spacing follows the font exactly.
				shift.x(Math.round((pen - whole) * 64.0F)).y(0L);
				FreeType.FT_Set_Transform(face, null, shift);
				int index = FreeType.FT_Get_Char_Index(face, codepoint);
				if (FreeType.FT_Load_Glyph(face, index, LOAD_TARGET_LIGHT) == 0) {
					FT_GlyphSlot slot = face.glyph();
					if (FreeType.FT_Render_Glyph(slot, FreeType.FT_RENDER_MODE_NORMAL) == 0) {
						blitGlyph(image, slot, whole + slot.bitmap_left(), baseline - slot.bitmap_top());
					}
					pen += slot.linearHoriAdvance() / 65536.0F;
				}
			}
			FreeType.FT_Set_Transform(face, null, null);
		}

		Identifier id = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "ui_text/" + texturesMade++);
		Minecraft.getInstance().getTextureManager().register(id, VersionedTextures.smoothTexture(() -> "EMUtils UI text", image));
		Rendered rendered = new Rendered(id, width, height, baseline, PADDING);
		STRINGS.put(key, rendered);
		return rendered;
	}

	private static void blitGlyph(NativeImage image, FT_GlyphSlot slot, int left, int top) {
		FT_Bitmap bitmap = slot.bitmap();
		int rows = bitmap.rows();
		int columns = bitmap.width();
		int pitch = bitmap.pitch();
		if (rows <= 0 || columns <= 0) {
			return;
		}
		ByteBuffer buffer = bitmap.buffer(Math.abs(pitch) * rows);
		for (int row = 0; row < rows; row++) {
			int y = top + row;
			if (y < 0 || y >= image.getHeight()) {
				continue;
			}
			for (int column = 0; column < columns; column++) {
				int x = left + column;
				if (x < 0 || x >= image.getWidth()) {
					continue;
				}
				int coverage = buffer.get(row * pitch + column) & 0xFF;
				if (coverage == 0) {
					continue;
				}
				int alpha = (int) Math.round(Math.pow(coverage / 255.0, COVERAGE_GAMMA) * 255.0);
				int existing = (image.getPixel(x, y) >>> 24) & 0xFF;
				image.setPixel(x, y, (Math.max(existing, alpha) << 24) | 0xFFFFFF);
			}
		}
	}

	private static void setSize(FT_Face face, float pixelSize) {
		FreeType.FT_Set_Char_Size(face, 0L, Math.round(pixelSize * 64.0F), 72, 72);
	}

	/** Frees the cached strings, for example when the GUI scale changes or the screen closes. */
	public static void clearCache() {
		for (Rendered rendered : STRINGS.values()) {
			Minecraft.getInstance().getTextureManager().release(rendered.texture());
		}
		STRINGS.clear();
		WIDTHS.clear();
	}
}
