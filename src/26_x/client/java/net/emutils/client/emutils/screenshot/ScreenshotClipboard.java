package net.emutils.client.emutils.screenshot;

import net.emutils.client.EMUtilsClient;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public final class ScreenshotClipboard {
	private ScreenshotClipboard() {
	}

	public static boolean copyImage(File file) {
		if (isWindows() && WindowsScreenshotClipboard.copyImage(file)) {
			return true;
		}

		if (copyWithWlCopy(file)) {
			return true;
		}

		return copyWithAwt(file);
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}

	private static boolean copyWithWlCopy(File file) {
		return copyWithWlCopyCommand("wl-copy", file) || copyWithWlCopyCommand("/usr/bin/wl-copy", file);
	}

	private static boolean copyWithWlCopyCommand(String command, File file) {
		try {
			Process process = new ProcessBuilder(command, "--type", "image/png")
				.redirectInput(file)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();

			if (!process.waitFor(2, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return false;
			}

			return process.exitValue() == 0;
		} catch (IOException exception) {
			return false;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static boolean copyWithAwt(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			if (image == null) {
				return false;
			}

			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(image), null);
			return true;
		} catch (IOException | RuntimeException exception) {
			EMUtilsClient.LOGGER.warn("Unable to copy screenshot image to clipboard.", exception);
			return false;
		}
	}

	private record ImageTransferable(Image image) implements Transferable {
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] {DataFlavor.imageFlavor};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return DataFlavor.imageFlavor.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (!isDataFlavorSupported(flavor)) {
				throw new UnsupportedFlavorException(flavor);
			}
			return image;
		}
	}
}
