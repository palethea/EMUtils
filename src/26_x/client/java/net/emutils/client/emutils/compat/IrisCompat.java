package net.emutils.client.emutils.compat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LoadingOverlay;

public final class IrisCompat {
	private IrisCompat() {
	}

	public static boolean isIrisLoaded() {
		return FabricLoader.getInstance().isModLoaded("iris");
	}

	public static Optional<String> currentShaderPack() {
		if (!isIrisLoaded() || !areShadersEnabled()) {
			return Optional.empty();
		}

		try {
			Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
			Object name = irisClass.getMethod("getCurrentPackName").invoke(null);
			if (name instanceof String packName && !packName.isBlank()) {
				return Optional.of(packName);
			}

			Object irisConfig = irisClass.getMethod("getIrisConfig").invoke(null);
			Object configName = irisConfig.getClass().getMethod("getShaderPackName").invoke(irisConfig);
			if (configName instanceof Optional<?> optional && optional.isPresent()) {
				Object value = optional.get();
				if (value instanceof String packName && !packName.isBlank()) {
					return Optional.of(packName);
				}
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return Optional.empty();
	}

	public static boolean isActiveShaderPack(String filename) {
		return currentShaderPack().filter(pack -> pack.equals(filename)).isPresent();
	}

	public static boolean areShadersEnabled() {
		if (!isIrisLoaded()) {
			return false;
		}

		try {
			Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
			Object irisConfig = irisClass.getMethod("getIrisConfig").invoke(null);
			Boolean enabled = (Boolean) irisConfig.getClass().getMethod("areShadersEnabled").invoke(irisConfig);
			return enabled != null && enabled;
		} catch (ReflectiveOperationException ignored) {
		}
		return false;
	}

	public static boolean prepareShaderPack(String filename) {
		if (!isIrisLoaded()) {
			return false;
		}

		try {
			Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
			Path shaderFolder = (Path) irisClass.getMethod("getShaderpacksDirectory").invoke(null);
			Path packPath = shaderFolder.resolve(filename).normalize();
			if (!packPath.startsWith(shaderFolder)) {
				return false;
			}

			Boolean valid = (Boolean) irisClass.getMethod("isValidShaderpack", Path.class).invoke(null, packPath);
			if (valid == null || !valid) {
				return false;
			}

			String currentName = (String) irisClass.getMethod("getCurrentPackName").invoke(null);
			if (currentName != null && !currentName.equals(filename)) {
				irisClass.getMethod("clearShaderPackOptionQueue").invoke(null);
			}

			Object irisConfig = irisClass.getMethod("getIrisConfig").invoke(null);
			irisConfig.getClass().getMethod("setShaderPackName", String.class).invoke(irisConfig, filename);
			irisConfig.getClass().getMethod("setShadersEnabled", boolean.class).invoke(irisConfig, true);
			irisConfig.getClass().getMethod("save").invoke(irisConfig);
			return true;
		} catch (ReflectiveOperationException exception) {
			return false;
		}
	}

	public static boolean prepareDisableShaders() {
		if (!isIrisLoaded()) {
			return false;
		}

		try {
			Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
			Object irisConfig = irisClass.getMethod("getIrisConfig").invoke(null);
			irisConfig.getClass().getMethod("setShadersEnabled", boolean.class).invoke(irisConfig, false);
			irisConfig.getClass().getMethod("save").invoke(irisConfig);
			return true;
		} catch (ReflectiveOperationException exception) {
			return false;
		}
	}

	public static void reloadShaders() throws IOException {
		try {
			Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
			irisClass.getMethod("reload").invoke(null);
		} catch (ReflectiveOperationException exception) {
			Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			if (cause instanceof IOException ioException) {
				throw ioException;
			}
			throw new IOException("Failed to reload Iris shaders.", cause);
		}
	}

	public static void applyShaderPackWithLoading(Minecraft client, Screen returnScreen, String filename, Consumer<Boolean> callback) {
		runShaderReloadWithLoading(client, returnScreen, () -> prepareShaderPack(filename), callback);
	}

	public static void disableShaderPackWithLoading(Minecraft client, Screen returnScreen, Consumer<Boolean> callback) {
		runShaderReloadWithLoading(client, returnScreen, IrisCompat::prepareDisableShaders, callback);
	}

	private static void runShaderReloadWithLoading(
		Minecraft client,
		Screen returnScreen,
		BooleanSupplier prepare,
		Consumer<Boolean> callback
	) {
		if (!isIrisLoaded()) {
			callback.accept(false);
			return;
		}

		if (!prepare.getAsBoolean()) {
			callback.accept(false);
			return;
		}

		LoadingOverlay.registerTextures(client.getTextureManager());
		IrisShaderResourceReload reload = new IrisShaderResourceReload();
		net.emutils.client.emutils.compat.MinecraftClientCompat.setOverlay(client, new ShaderPackLoadingOverlay(client, reload, optional -> {
			net.emutils.client.emutils.compat.MinecraftClientCompat.setOverlay(client, null);
			client.execute(() -> {
				callback.accept(optional.isEmpty());
				client.setScreenAndShow(returnScreen);
			});
		}));
	}

	public static boolean applyShaderPack(String filename, Minecraft client, Screen parent) {
		if (!isIrisLoaded()) {
			return false;
		}

		if (!prepareShaderPack(filename)) {
			openShaderScreen(client, parent);
			return false;
		}

		try {
			reloadShaders();
			return true;
		} catch (IOException ignored) {
			openShaderScreen(client, parent);
			return false;
		}
	}

	public static boolean openShaderScreen(Minecraft client, Screen parent) {
		if (!isIrisLoaded()) {
			return false;
		}

		try {
			Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			Object api = apiClass.getMethod("getInstance").invoke(null);
			Object screen = api.getClass().getMethod("openMainIrisScreenObj", Object.class).invoke(api, parent);
			if (screen instanceof Screen irisScreen) {
				client.setScreenAndShow(irisScreen);
				return true;
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return false;
	}
}
