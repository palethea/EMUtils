package net.emutils.client.emutils.screenshot;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.imageio.ImageIO;
import net.emutils.client.EMUtilsClient;

final class WindowsScreenshotClipboard {
	private static final int CF_DIB = 8;
	private static final int GMEM_MOVEABLE = 0x0002;
	private static final int BI_RGB = 0;
	private static final int HEADER_BYTES = 40;

	private WindowsScreenshotClipboard() {
	}

	static boolean copyImage(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null) {
				return false;
			}

			return copyDib(image);
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Unable to copy screenshot image with Windows clipboard API.", exception);
			return false;
		}
	}

	private static boolean copyDib(BufferedImage image) {
		byte[] dib = createDib(image);
		Pointer handle = Kernel32.INSTANCE.GlobalAlloc(GMEM_MOVEABLE, dib.length);
		if (Pointer.nativeValue(handle) == 0L) {
			return false;
		}

		boolean clipboardOwnsHandle = false;
		try {
			Pointer memory = Kernel32.INSTANCE.GlobalLock(handle);
			if (Pointer.nativeValue(memory) == 0L) {
				return false;
			}
			try {
				memory.write(0L, dib, 0, dib.length);
			} finally {
				Kernel32.INSTANCE.GlobalUnlock(handle);
			}

			if (!openClipboard()) {
				return false;
			}

			try {
				if (!User32.INSTANCE.EmptyClipboard()) {
					return false;
				}

				Pointer result = User32.INSTANCE.SetClipboardData(CF_DIB, handle);
				clipboardOwnsHandle = Pointer.nativeValue(result) != 0L;
				return clipboardOwnsHandle;
			} finally {
				User32.INSTANCE.CloseClipboard();
			}
		} finally {
			if (!clipboardOwnsHandle) {
				Kernel32.INSTANCE.GlobalFree(handle);
			}
		}
	}

	private static boolean openClipboard() {
		for (int attempt = 0; attempt < 5; attempt++) {
			if (User32.INSTANCE.OpenClipboard(Pointer.NULL)) {
				return true;
			}

			try {
				Thread.sleep(25L);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		return false;
	}

	private static byte[] createDib(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		byte[] bytes = new byte[HEADER_BYTES + width * height * 4];
		ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(HEADER_BYTES);
		buffer.putInt(width);
		buffer.putInt(-height);
		buffer.putShort((short) 1);
		buffer.putShort((short) 32);
		buffer.putInt(BI_RGB);
		buffer.putInt(width * height * 4);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);
		buffer.putInt(0);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = image.getRGB(x, y);
				buffer.put((byte) (argb & 0xFF));
				buffer.put((byte) ((argb >> 8) & 0xFF));
				buffer.put((byte) ((argb >> 16) & 0xFF));
				buffer.put((byte) ((argb >> 24) & 0xFF));
			}
		}

		return bytes;
	}

	private interface User32 extends Library {
		User32 INSTANCE = Native.load("user32", User32.class);

		boolean OpenClipboard(Pointer owner);

		boolean EmptyClipboard();

		Pointer SetClipboardData(int format, Pointer handle);

		boolean CloseClipboard();
	}

	private interface Kernel32 extends Library {
		Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

		Pointer GlobalAlloc(int flags, long bytes);

		Pointer GlobalLock(Pointer handle);

		boolean GlobalUnlock(Pointer handle);

		Pointer GlobalFree(Pointer handle);
	}
}
