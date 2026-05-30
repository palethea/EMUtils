package net.emutils.client.emskyblock.api.modapi;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import net.emutils.client.emskyblock.api.SkyblockApiManager;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HypixelModApiBridge {
	private static final Logger LOGGER = LoggerFactory.getLogger("emutils.hypixel_mod_api");

	private HypixelModApiBridge() {
	}

	public static void tryRegister(SkyblockApiManager apiManager) {
		if (!FabricLoader.getInstance().isModLoaded("hypixel-mod-api")) {
			return;
		}

		try {
			Class<?> modApiClass = Class.forName("net.hypixel.modapi.HypixelModAPI");
			Class<?> helloClass = Class.forName("net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket");
			Class<?> locationClass = Class.forName("net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket");
			Object modApi = modApiClass.getMethod("getInstance").invoke(null);

			subscribe(modApi, locationClass);
			registerHandler(modApi, helloClass, packet -> handleHello(apiManager, packet));
			registerHandler(modApi, locationClass, packet -> handleLocation(apiManager, packet));
			apiManager.markModApiAvailable();
			LOGGER.info("Registered Hypixel Mod API bridge.");
		} catch (ReflectiveOperationException | RuntimeException exception) {
			LOGGER.warn("Hypixel Mod API is present but could not be registered; using in-game fallbacks.", exception);
		}
	}

	private static void subscribe(Object modApi, Class<?> packetClass) throws ReflectiveOperationException {
		for (Method method : modApi.getClass().getMethods()) {
			if (!method.getName().equals("subscribeToEventPacket") || method.getParameterCount() != 1) {
				continue;
			}

			method.invoke(modApi, packetClass);
			return;
		}
	}

	private static void registerHandler(Object modApi, Class<?> packetClass, PacketConsumer consumer) throws ReflectiveOperationException {
		for (Method method : modApi.getClass().getMethods()) {
			if (!method.getName().equals("createHandler") || method.getParameterCount() != 2) {
				continue;
			}

			Class<?> handlerType = method.getParameterTypes()[1];
			Object handler = Proxy.newProxyInstance(
				handlerType.getClassLoader(),
				new Class<?>[] { handlerType },
				(proxy, invoked, args) -> {
					if (args != null && args.length > 0 && args[0] != null) {
						consumer.accept(args[0]);
					}
					return null;
				}
			);
			method.invoke(modApi, packetClass, handler);
			return;
		}
	}

	private static void handleHello(SkyblockApiManager apiManager, Object packet) {
		String environment = stringValue(read(packet, "environment", "getEnvironment"));
		boolean alpha = environment != null && !environment.equalsIgnoreCase("PRODUCTION");
		apiManager.onModApiHello(alpha);
	}

	private static void handleLocation(SkyblockApiManager apiManager, Object packet) {
		apiManager.onModApiLocation(
			stringValue(read(packet, "serverName", "getServerName")),
			stringValue(read(packet, "serverType", "getServerType")),
			stringValue(read(packet, "lobbyName", "getLobbyName")),
			stringValue(read(packet, "mode", "getMode")),
			stringValue(read(packet, "map", "getMap"))
		);
	}

	@Nullable
	private static Object read(Object target, String... methodNames) {
		for (String methodName : methodNames) {
			try {
				Method method = target.getClass().getMethod(methodName);
				return unwrapOptional(method.invoke(target));
			} catch (ReflectiveOperationException ignored) {
			}
		}
		return null;
	}

	@Nullable
	private static Object unwrapOptional(@Nullable Object value) {
		if (value instanceof Optional<?> optional) {
			return optional.orElse(null);
		}
		return value;
	}

	@Nullable
	private static String stringValue(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Enum<?> enumValue) {
			return enumValue.name();
		}
		return value.toString();
	}

	@FunctionalInterface
	private interface PacketConsumer {
		void accept(Object packet);
	}
}
