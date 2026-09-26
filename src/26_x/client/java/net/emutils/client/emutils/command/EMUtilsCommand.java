package net.emutils.client.emutils.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.compat.MinecraftClientCompat;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.settings.SettingsScreen;
import net.emutils.client.emutils.gui.hub.HubFeature;
import net.emutils.client.emutils.gui.hub.HubFeatureCatalog;
import net.emutils.client.emutils.profile.Profile;
import net.emutils.client.emutils.profile.ProfileManager;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class EMUtilsCommand {
	private static final SuggestionProvider<FabricClientCommandSource> TOGGLE_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(HubFeatureCatalog.toggleableIds(), builder);
	private static final SuggestionProvider<FabricClientCommandSource> RESET_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(HubFeatureCatalog.resettableIds(), builder);
	private static final SuggestionProvider<FabricClientCommandSource> PROFILE_SUGGESTIONS =
		(context, builder) -> SharedSuggestionProvider.suggest(EMUtilsClient.profiles().profiles().stream().map(profile -> profile.name().getString()), builder);

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
					.then(
						literal("profile")
							.executes(EMUtilsCommand::listProfiles)
							.then(
								argument("name", StringArgumentType.greedyString())
									.suggests(PROFILE_SUGGESTIONS)
									.executes(EMUtilsCommand::switchProfile)
							)
					)
			);
		});
	}

	private static int openHub(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return 0;
		}

		client.gui.setScreen(new SettingsScreen(MinecraftClientCompat.screen(client)));
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

		// Replaces the active profile's settings.
		EMUtilsConfig current = EMUtilsClient.config();
		EMUtilsConfig imported = current == null ? null : EMUtilsConfig.fromJson(client.keyboardHandler.getClipboard(), current.file());
		if (imported == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_IMPORT_FAILED));
			return 0;
		}

		EMUtilsClient.replaceConfig(imported);
		feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_IMPORTED));
		return 1;
	}

	private static int listProfiles(CommandContext<FabricClientCommandSource> context) {
		ProfileManager profiles = EMUtilsClient.profiles();
		MutableComponent names = Component.empty();
		for (Profile profile : profiles.profiles()) {
			if (!names.getSiblings().isEmpty()) {
				names.append(", ");
			}
			names.append(profile == profiles.active() ? profile.name().copy().withStyle(ChatFormatting.GREEN) : profile.name());
		}
		feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_PROFILES, profiles.active().name(), names));
		return 1;
	}

	private static int switchProfile(CommandContext<FabricClientCommandSource> context) {
		String name = StringArgumentType.getString(context, "name");
		Profile profile = EMUtilsClient.profiles().byName(name);
		if (profile == null) {
			feedback(context, Component.translatable(EMUtilsTexts.COMMAND_FEEDBACK_UNKNOWN_PROFILE, name));
			return 0;
		}
		if (!EMUtilsClient.profiles().pick(profile)) {
			feedback(context, Component.translatable(EMUtilsTexts.PROFILE_SWITCH_FAILED));
			return 0;
		}
		feedback(context, Component.translatable(EMUtilsTexts.PROFILE_SWITCHED, profile.name()));
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
