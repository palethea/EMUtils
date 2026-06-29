package net.emutils.client.emutils.compat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

public final class MinecraftClientCompat {
	private MinecraftClientCompat() {
	}

	public static Screen screen(Minecraft client) {
		Object screen = tryInvokeNoArgs(client.gui, "screen");
		if (screen instanceof Screen currentScreen) {
			return currentScreen;
		}
		Object fieldValue = tryGetField(client, "screen");
		return fieldValue instanceof Screen currentScreen ? currentScreen : null;
	}

	public static ChatComponent chat(Minecraft client) {
		Object chat = invokeNoArgs(hud(client), "getChat");
		if (chat instanceof ChatComponent chatComponent) {
			return chatComponent;
		}
		throw new IllegalStateException("Minecraft GUI chat component has unexpected type: " + chat);
	}

	public static int guiTicks(Minecraft client) {
		Object ticks = invokeNoArgs(hud(client), "getGuiTicks");
		if (ticks instanceof Integer value) {
			return value;
		}
		throw new IllegalStateException("Minecraft GUI ticks has unexpected type: " + ticks);
	}

	public static boolean isHudHidden(Minecraft client) {
		Object hud = hud(client);
		Object hidden = tryInvokeNoArgs(hud, "isHidden");
		if (hidden instanceof Boolean value) {
			return value;
		}
		Object fieldValue = tryGetField(hud, "isHidden");
		return fieldValue instanceof Boolean value && value;
	}

	public static ToastManager toastManager(Minecraft client) {
		Object toastManager = tryInvokeNoArgs(client.gui, "toastManager");
		if (!(toastManager instanceof ToastManager)) {
			toastManager = tryInvokeNoArgs(client, "getToastManager");
		}
		if (toastManager instanceof ToastManager typedToastManager) {
			return typedToastManager;
		}
		throw new IllegalStateException("Minecraft toast manager has unexpected type: " + toastManager);
	}

	public static void setOverlay(Minecraft client, Overlay overlay) {
		if (tryInvokeOneArg(client.gui, "setOverlay", overlay)) {
			return;
		}
		if (tryInvokeOneArg(client, "setOverlay", overlay)) {
			return;
		}
		throw new IllegalStateException("Could not set Minecraft overlay");
	}

	public static Camera mainCamera(Minecraft client) {
		return mainCamera(client.gameRenderer);
	}

	public static Camera mainCamera(GameRenderer gameRenderer) {
		Object camera = tryInvokeNoArgs(gameRenderer, "mainCamera");
		if (!(camera instanceof Camera)) {
			camera = tryInvokeNoArgs(gameRenderer, "getMainCamera");
		}
		if (camera instanceof Camera typedCamera) {
			return typedCamera;
		}
		throw new IllegalStateException("Minecraft main camera has unexpected type: " + camera);
	}

	private static Object hud(Minecraft client) {
		Object hud = tryGetField(client.gui, "hud");
		return hud == null ? client.gui : hud;
	}

	private static Object tryGetField(Object target, String fieldName) {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field.get(target);
			} catch (NoSuchFieldException ignored) {
				type = type.getSuperclass();
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Could not read field " + fieldName + " on " + target.getClass().getName(), e);
			}
		}
		return null;
	}

	private static Object tryInvokeNoArgs(Object target, String methodName) {
		Method method = findMethod(target.getClass(), methodName, 0);
		return method == null ? null : invoke(target, method);
	}

	private static Object invokeNoArgs(Object target, String methodName) {
		Method method = findMethod(target.getClass(), methodName, 0);
		if (method == null) {
			throw new IllegalStateException("Missing method " + methodName + " on " + target.getClass().getName());
		}
		return invoke(target, method);
	}

	private static boolean tryInvokeOneArg(Object target, String methodName, Object arg) {
		Method method = findSingleArgMethod(target.getClass(), methodName, arg);
		if (method == null) {
			return false;
		}
		invoke(target, method, arg);
		return true;
	}

	private static Method findMethod(Class<?> type, String methodName, int parameterCount) {
		Class<?> current = type;
		while (current != null) {
			for (Method method : current.getDeclaredMethods()) {
				if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
					method.setAccessible(true);
					return method;
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

	private static Method findSingleArgMethod(Class<?> type, String methodName, Object arg) {
		Class<?> current = type;
		while (current != null) {
			for (Method method : current.getDeclaredMethods()) {
				if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
					continue;
				}
				Class<?> parameterType = method.getParameterTypes()[0];
				if (arg == null || parameterType.isInstance(arg)) {
					method.setAccessible(true);
					return method;
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

	private static Object invoke(Object target, Method method, Object... args) {
		try {
			return method.invoke(target, args);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException("Could not invoke " + method.getName() + " on " + target.getClass().getName(), e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException("Could not invoke " + method.getName() + " on " + target.getClass().getName(), cause);
		}
	}
}
