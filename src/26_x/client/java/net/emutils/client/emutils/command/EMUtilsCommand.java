package net.emutils.client.emutils.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.hub.CustomHubScreen;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class EMUtilsCommand {
	private static final SuggestionProvider<FabricClientCommandSource> TOGGLE_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(HubFeatureCatalog.toggleableIds(), builder);
	private static final SuggestionProvider<FabricClientCommandSource> RESET_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(HubFeatureCatalog.resettableIds(), builder);

	private EMUtilsCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
				literal("emutils")
					.executes(EMUtilsCommand::openHub)
					.then(
						literal("toggle").then(
							argument("feature", StringArgumentType.greedyString())
								.suggests(TOGGLE_SUGGESTIONS)
								.executes(EMUtilsCommand::toggle)
						)
					)
					.then(
						literal("preset").then(
							argument("feature", StringArgumentType.greedyString())
								.suggests(RESET_SUGGESTIONS)
								.executes(EMUtilsCommand::preset)
						)
					)
					.then(literal("export").executes(EMUtilsCommand::export))
					.then(literal("import").executes(EMUtilsCommand::importConfig))
			);
		});
	}

	private static int openHub(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return 0;
		}

		client.setScreenAndShow(new CustomHubScreen(MinecraftClientCompat.screen(client)));
		return 1;
	}

	private static int toggle(CommandContext<FabricClientCommandSource> context) {
		HubFeature feature = resolve(context);
		if (feature == null) {
			return 0;
		}

		HubFeature.Toggle toggle = feature.toggle();
		if (toggle == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_NOT_TOGGLEABLE, feature.title()));
			return 0;
		}

		boolean enabled = !toggle.getter().getAsBoolean();
		toggle.setter().accept(enabled);
		feedback(
			context,
			Component.translatable(
				enabled ? EMUtilsTexts.COMMAND_FEEDBACK_TOGGLED_ON : EMUtilsTexts.COMMAND_FEEDBACK_TOGGLED_OFF,
				feature.title()
			)
		);
		return 1;
	}

	private static int preset(CommandContext<FabricClientCommandSource> context) {
		HubFeature feature = resolve(context);
		if (feature == null) {
			return 0;
		}

		Runnable reset = feature.resetAction();
		if (reset == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_NO_RESET, feature.title()));
			return 0;
		}

		reset.run();
		feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_RESET, feature.title()));
		return 1;
	}

	private static int export(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = Minecraft.getInstance();
		EMUtilsConfig config = EMUtilsClient.config();
		if (client == null || config == null) {
			return 0;
		}

		client.keyboardHandler.setClipboard(config.toJson());
		feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_EXPORTED));
		return 1;
	}

	private static int importConfig(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return 0;
		}

		EMUtilsConfig imported = EMUtilsConfig.fromJson(client.keyboardHandler.getClipboard());
		if (imported == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_IMPORT_FAILED));
			return 0;
		}

		EMUtilsClient.replaceConfig(imported);
		feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_IMPORTED));
		return 1;
	}

	private static HubFeature resolve(CommandContext<FabricClientCommandSource> context) {
		String query = StringArgumentType.getString(context, "feature");
		HubFeature feature = HubFeatureCatalog.find(query);
		if (feature == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_UNKNOWN_FEATURE, query));
		}

		return feature;
	}

	private static void feedback(CommandContext<FabricClientCommandSource> context, Component message) {
		context.getSource().sendFeedback(EmUtilsChatPrefix.chat(message));
	}
}
