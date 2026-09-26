package net.emutils.client.emutils.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.versioned.VersionedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.CLongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_MM_Var;
import org.lwjgl.util.freetype.FT_Var_Axis;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;
import org.lwjgl.util.freetype.TT_OS2;

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
	/** OpenType tag of the weight axis in variable fonts. */
	private static final long WEIGHT_AXIS = ('w' << 24) | ('g' << 16) | ('h' << 8) | 't';
	private static final float DEFAULT_CAP_HEIGHT = 0.7F;
	private static final Map<Weight, FT_Face> FACES = new EnumMap<>(Weight.class);
	/** The font data of each face, which FreeType reads from for as long as the face lives. */
	private static final Map<Weight, ByteBuffer> FACE_DATA = new EnumMap<>(Weight.class);
	private static final Map<Weight, Float> CAP_HEIGHTS = new EnumMap<>(Weight.class);
	private static @Nullable UiFontFamily loadedFamily;
	private static @Nullable UiCodeFont loadedCodeFont;
	/** Counts font changes, so screens know to lay out their text again. */
	private static int generation;
	private static final Map<String, Float> WIDTHS = new HashMap<>();
	private static final LinkedHashMap<String, Rendered> STRINGS = new LinkedHashMap<>(64, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Rendered> eldest) {
			if (size() > MAX_CACHED_STRINGS) {
				releaseLater(eldest.getValue().texture());
				return true;
			}
			return false;
		}
	};
	/**
	 * Single characters, for text that changes all the time such as HUD values: each is rendered once
	 * per size, and the text is put together from them, instead of a new texture per string.
	 */
	private static final Map<Long, Glyph> GLYPHS = new HashMap<>();
	private static final int MAX_CACHED_GLYPHS = 2048;
	/**
	 * Evicted textures, freed on the next client tick. A cache can overflow halfway through a frame, and
	 * Minecraft only draws the frame after collecting it, so a texture freed right away could still be
	 * drawn in that frame. Ticks run between frames, so the frame that used it is finished by then.
	 */
	private static final List<Identifier> RELEASED = new ArrayList<>();
	private static int texturesMade;

	private UiFontRenderer() {
	}

	/**
	 * The weights the UI uses, with their design weight for variable fonts. Which file each comes from
	 * depends on the chosen font ({@link UiFontFamily}, {@link UiCodeFont}).
	 */
	public enum Weight {
		SEMIBOLD(600),
		EXTRABOLD(800),
		BLACK(900),
		/** The code font, such as the script editor's (#118). */
		MONO(400);

		private final int designWeight;

		Weight(int designWeight) {
			this.designWeight = designWeight;
		}

		/** The font's cap height as a fraction of its em size. */
		public float capHeight() {
			Float capHeight = CAP_HEIGHTS.get(this);
			return capHeight == null ? DEFAULT_CAP_HEIGHT : capHeight;
		}
	}

	/** A string rendered to a white texture; {@code baseline} is the baseline's row in the texture. */
	public record Rendered(Identifier texture, int width, int height, int baseline, int left) {
	}

	/** One character and how far it moves the pen, in physical pixels. */
	public record Glyph(Rendered rendered, float advance) {
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
			return loadFaces(chosenFamily(), chosenCodeFont());
		} catch (IOException | LinkageError exception) {
			EMUtilsClient.LOGGER.warn("EMUtils UI font renderer unavailable; using Minecraft's text rendering", exception);
			return false;
		}
	}

	private static UiFontFamily chosenFamily() {
		return EMUtilsClient.config() == null ? UiFontFamily.NUNITO : EMUtilsClient.config().uiFont();
	}

	private static UiCodeFont chosenCodeFont() {
		return EMUtilsClient.config() == null ? UiCodeFont.JETBRAINS_MONO : EMUtilsClient.config().uiCodeFont();
	}

	/**
	 * Loads the fonts picked in the menu settings, when they changed since the last call (#120), and
	 * drops every cached string and glyph rendered in the old ones. With Minecraft's font picked, the UI
	 * weights stay in Nunito; they're only used for the code font's neighbours then.
	 */
	public static void syncFonts() {
		if (!available()) {
			return;
		}
		UiFontFamily family = chosenFamily();
		UiCodeFont codeFont = chosenCodeFont();
		if (family == loadedFamily && codeFont == loadedCodeFont) {
			return;
		}
		try {
			if (!loadFaces(family, codeFont)) {
				EMUtilsClient.LOGGER.warn("Could not load the {} and {} fonts; keeping the previous ones.", family.displayName(), codeFont.displayName());
			}
		} catch (IOException exception) {
			EMUtilsClient.LOGGER.warn("Could not load the {} and {} fonts; keeping the previous ones.", family.displayName(), codeFont.displayName(), exception);
		}
	}

	/** Replaces every face with the chosen fonts; keeps the old ones when a new one can't be loaded. */
	private static boolean loadFaces(UiFontFamily family, UiCodeFont codeFont) throws IOException {
		UiFontFamily files = family.minecraft() ? UiFontFamily.NUNITO : family;
		Map<Weight, FT_Face> faces = new EnumMap<>(Weight.class);
		Map<Weight, ByteBuffer> data = new EnumMap<>(Weight.class);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			for (Weight weight : Weight.values()) {
				boolean mono = weight == Weight.MONO;
				String file = mono ? codeFont.file() : files.file(weight);
				boolean variable = mono ? codeFont.variable() : files.variable();
				ByteBuffer buffer;
				try {
					buffer = read(file);
				} catch (IOException exception) {
					release(faces, data);
					throw exception;
				}
				FT_Face face = file == null ? null : openFace(buffer, variable ? weight.designWeight : 0, stack);
				if (face == null) {
					MemoryUtil.memFree(buffer);
					release(faces, data);
					return false;
				}
				faces.put(weight, face);
				data.put(weight, buffer);
			}
		}
		release(FACES, FACE_DATA);
		FACES.clear();
		FACE_DATA.clear();
		FACES.putAll(faces);
		FACE_DATA.putAll(data);
		CAP_HEIGHTS.clear();
		faces.forEach((weight, face) -> CAP_HEIGHTS.put(weight, capHeightOf(face)));
		loadedFamily = family;
		loadedCodeFont = codeFont;
		generation++;
		clearCache();
		return true;
	}

	/** Closes faces before freeing the data FreeType reads them from. */
	private static void release(Map<Weight, FT_Face> faces, Map<Weight, ByteBuffer> data) {
		faces.values().forEach(FreeType::FT_Done_Face);
		data.values().forEach(MemoryUtil::memFree);
	}

	private static ByteBuffer read(@Nullable String file) throws IOException {
		if (file == null) {
			return MemoryUtil.memAlloc(0);
		}
		Identifier location = Identifier.fromNamespaceAndPath(EMUtilsClient.MOD_ID, "font/" + file);
		byte[] bytes;
		try (InputStream stream = Minecraft.getInstance().getResourceManager().open(location)) {
			bytes = stream.readAllBytes();
		}
		ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
		buffer.put(bytes).flip();
		return buffer;
	}

	/** Opens a face on {@code data}; for a variable font, at {@code designWeight} (0 keeps the default). */
	private static @Nullable FT_Face openFace(ByteBuffer data, int designWeight, MemoryStack stack) {
		PointerBuffer facePointer = stack.mallocPointer(1);
		if (FreeType.FT_New_Memory_Face(library, data, 0L, facePointer) != 0) {
			return null;
		}
		FT_Face face = FT_Face.create(facePointer.get(0));
		if (designWeight > 0) {
			setDesignWeight(face, designWeight, stack);
		}
		return face;
	}

	/** Picks a weight in a variable font: every axis at its default, and the weight axis at {@code designWeight}. */
	private static void setDesignWeight(FT_Face face, int designWeight, MemoryStack stack) {
		PointerBuffer pointer = stack.mallocPointer(1);
		if (FreeType.FT_Get_MM_Var(face, pointer) != 0) {
			return;
		}
		FT_MM_Var variations = FT_MM_Var.create(pointer.get(0));
		try {
			int axes = variations.num_axis();
			CLongBuffer coordinates = stack.mallocCLong(axes);
			for (int i = 0; i < axes; i++) {
				FT_Var_Axis axis = variations.axis().get(i);
				long value = axis.def();
				if (axis.tag() == WEIGHT_AXIS) {
					value = Math.clamp((long) designWeight << 16, axis.minimum(), axis.maximum());
				}
				coordinates.put(i, value);
			}
			FreeType.FT_Set_Var_Design_Coordinates(face, coordinates);
		} finally {
			FreeType.FT_Done_MM_Var(library, variations);
		}
	}

	/** Cap height from the font's OS/2 table, as a fraction of the em. */
	private static float capHeightOf(FT_Face face) {
		long table = FreeType.FT_Get_Sfnt_Table(face, FreeType.FT_SFNT_OS2);
		int unitsPerEm = face.units_per_EM() & 0xFFFF;
		if (table == 0L || unitsPerEm == 0) {
			return DEFAULT_CAP_HEIGHT;
		}
		int capHeight = TT_OS2.create(table).sCapHeight();
		return capHeight > 0 ? capHeight / (float) unitsPerEm : DEFAULT_CAP_HEIGHT;
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
		Rendered rendered = renderUncached(weight, pixelSize, text);
		STRINGS.put(key, rendered);
		return rendered;
	}

	/** One character at {@code pixelSize}, from the glyph cache. */
	public static Glyph glyph(Weight weight, float pixelSize, int codepoint) {
		long key = (Integer.toUnsignedLong(Float.floatToIntBits(pixelSize)) << 24) | ((long) weight.ordinal() << 21) | codepoint;
		Glyph cached = GLYPHS.get(key);
		if (cached != null) {
			return cached;
		}
		if (GLYPHS.size() >= MAX_CACHED_GLYPHS) {
			// Only reached when sizes keep changing; start over rather than track use.
			releaseGlyphs();
		}
		String text = new String(Character.toChars(codepoint));
		Glyph glyph = new Glyph(renderUncached(weight, pixelSize, text), measure(weight, pixelSize, text));
		GLYPHS.put(key, glyph);
		return glyph;
	}

	private static void releaseGlyphs() {
		for (Glyph glyph : GLYPHS.values()) {
			releaseLater(glyph.rendered().texture());
		}
		GLYPHS.clear();
	}

	private static Rendered renderUncached(Weight weight, float pixelSize, String text) {
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
		return new Rendered(id, width, height, baseline, PADDING);
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

	/** How many text textures have been made so far, for checking that live text doesn't make new ones. */
	private static void releaseLater(Identifier texture) {
		RELEASED.add(texture);
	}

	/** Frees the textures evicted since the last call; call every client tick. */
	public static void freeReleased() {
		if (RELEASED.isEmpty()) {
			return;
		}
		for (Identifier texture : RELEASED) {
			Minecraft.getInstance().getTextureManager().release(texture);
		}
		RELEASED.clear();
	}

	/** Changes whenever the fonts are reloaded. */
	public static int generation() {
		return generation;
	}

	public static int texturesMade() {
		return texturesMade;
	}

	/** Frees the cached strings, for example when the GUI scale changes or the screen closes. */
	public static void clearCache() {
		for (Rendered rendered : STRINGS.values()) {
			releaseLater(rendered.texture());
		}
		STRINGS.clear();
		WIDTHS.clear();
		releaseGlyphs();
	}
}
