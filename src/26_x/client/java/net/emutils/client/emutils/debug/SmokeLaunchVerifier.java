package net.emutils.client.emutils.debug;

import net.emutils.client.EMUtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SmokeLaunchVerifier {
	private static final String ENABLED_PROPERTY = "emutils.smokeLaunch";
	private static final String LAUNCH_TEST_WORLD_PROPERTY = "emutils.launchTestWorld";
	private static final String TEST_WORLD_NAME = "EMUtils Debug World";
	private static final int MIN_WORLD_TICKS = 80;
	private static final int MAX_TICKS = 2400;

	private static boolean enabled;
	private static boolean launchTestWorld;
	private static boolean registered;
	private static int ticks;
	private static int worldTicks;
	private static boolean requestedWorldCreation;
	private static boolean clickedCreateWorld;
	private static Field minecraftScreenField;

	private SmokeLaunchVerifier() {
	}

	public static void registerIfEnabled() {
		enabled = Boolean.getBoolean(ENABLED_PROPERTY);
		launchTestWorld = Boolean.getBoolean(LAUNCH_TEST_WORLD_PROPERTY);
		registered = enabled || launchTestWorld;
		ticks = 0;
		worldTicks = 0;
		requestedWorldCreation = false;
		clickedCreateWorld = false;
		if (enabled) {
			EMUtilsClient.LOGGER.info("EMUtils smoke launch verifier enabled.");
		}
		if (launchTestWorld) {
			EMUtilsClient.LOGGER.info("EMUtils debug launch will enter a singleplayer test world.");
		}
	}

	public static void tick(Minecraft client) {
		if (!registered) {
			return;
		}

		ticks++;
		if (launchTestWorld) {
			launchOrContinueTestWorld(client);
		}

		if (client.level != null && client.player != null) {
			worldTicks++;
			if (worldTicks == 1) {
				EMUtilsClient.LOGGER.info("EMUtils debug launch reached a singleplayer world.");
			}
			if (enabled && worldTicks >= MIN_WORLD_TICKS) {
				EMUtilsClient.LOGGER.info("EMUtils smoke launch verifier reached singleplayer world; stopping Minecraft.");
				client.stop();
				registered = false;
			} else if (!enabled) {
				registered = false;
			}
			return;
		}

		if (ticks > MAX_TICKS) {
			registered = false;
			throw new IllegalStateException("EMUtils debug launch timed out before reaching a singleplayer world");
		}
	}

	private static void launchOrContinueTestWorld(Minecraft client) {
		if (!requestedWorldCreation && client.isGameLoadFinished() && client.level == null) {
			requestedWorldCreation = true;
			EMUtilsClient.LOGGER.info("EMUtils debug launch is preparing a singleplayer test world.");
			CreateWorldScreen.testWorld(client, () -> {
			});
			return;
		}

		if (requestedWorldCreation && !clickedCreateWorld) {
			Screen screen = currentScreen(client);
			if (screen instanceof CreateWorldScreen createWorldScreen) {
				clickedCreateWorld = true;
				createWorldScreen.getUiState().setName(TEST_WORLD_NAME);
				invokeCreateWorld(createWorldScreen);
			}
		}
	}

	private static Screen currentScreen(Minecraft client) {
		Object guiScreen = tryInvokeNoArgs(client.gui, "screen");
		if (guiScreen instanceof Screen currentScreen) {
			return currentScreen;
		}

		if (minecraftScreenField == null) {
			minecraftScreenField = findScreenField();
		}
		try {
			Object screen = minecraftScreenField.get(client);
			return screen instanceof Screen currentScreen ? currentScreen : null;
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Failed to inspect current Minecraft screen field", exception);
		}
	}

	private static Object tryInvokeNoArgs(Object target, String methodName) {
		Method method = findMethod(target.getClass(), methodName, 0);
		return method == null ? null : invoke(target, method);
	}

	private static Method findMethod(Class<?> type, String methodName, int parameterCount) {
		Class<?> currentClass = type;
		while (currentClass != null) {
			for (Method method : currentClass.getDeclaredMethods()) {
				if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
					method.setAccessible(true);
					return method;
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return null;
	}

	private static Object invoke(Object target, Method method, Object... args) {
		try {
			return method.invoke(target, args);
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Could not invoke " + method.getName() + " on " + target.getClass().getName(), exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Could not invoke " + method.getName() + " on " + target.getClass().getName(), cause);
		}
	}

	private static Field findScreenField() {
		for (Field field : Minecraft.class.getDeclaredFields()) {
			if (Screen.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				return field;
			}
		}
		throw new IllegalStateException("Failed to find current Minecraft screen field");
	}

	private static void invokeCreateWorld(CreateWorldScreen createWorldScreen) {
		try {
			Method onCreate = CreateWorldScreen.class.getDeclaredMethod("onCreate");
			onCreate.setAccessible(true);
			onCreate.invoke(createWorldScreen);
		} catch (NoSuchMethodException | IllegalAccessException exception) {
			throw new IllegalStateException("Failed to start EMUtils singleplayer test world", exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Failed to start EMUtils singleplayer test world", cause);
		}
	}
}
