package net.emutils.client.gui.chat;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;

public final class ChatFeaturesSettingsScreen extends EMUtilsScreen {
	public ChatFeaturesSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_CHAT_FEATURES));
	}

	@Override
	protected void initBody() {
		GridWidget.Adder adder = initTwoColumnBody();
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT,
			() -> EMUtilsClient.config().copyChat(),
			EMUtilsClient.config()::setCopyChat
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FORMATTING,
			() -> EMUtilsClient.config().copyChatFormatting(),
			EMUtilsClient.config()::setCopyChatFormatting
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FEEDBACK,
			() -> EMUtilsClient.config().copyChatFeedback(),
			EMUtilsClient.config()::setCopyChatFeedback
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMPS,
			() -> EMUtilsClient.config().chatTimestamps(),
			EMUtilsClient.config()::setChatTimestamps
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMP_24_HOUR,
			() -> EMUtilsClient.config().chatTimestamp24Hour(),
			EMUtilsClient.config()::setChatTimestamp24Hour
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SMART_CHAT_FILTERS,
			() -> EMUtilsClient.config().smartChatFilters(),
			EMUtilsClient.config()::setSmartChatFilters
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_TIME_WINDOW,
			() -> EMUtilsClient.config().duplicateMessageTimeWindow(),
			EMUtilsClient.config()::setDuplicateMessageTimeWindow
		));
		adder.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERTS,
			() -> EMUtilsClient.config().chatMentionAlerts(),
			EMUtilsClient.config()::setChatMentionAlerts
		));
		adder.add(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_WINDOW,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MIN,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MAX,
			() -> EMUtilsClient.config().duplicateMessageWindowSeconds(),
			EMUtilsClient.config()::setDuplicateMessageWindowSeconds
		));
		adder.add(fullWidthSettingsButton(Text.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetChatDefaults();
			client.setScreen(new ChatFeaturesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}
}
