package net.emutils.client.gui.chat;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.config.EMUtilsConfig;
import net.emutils.client.gui.EMUtilsScreen;
import net.emutils.client.gui.widget.ConfigToggleButton;
import net.emutils.client.gui.widget.IntConfigSlider;
import net.emutils.client.util.EMUtilsTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;

public final class ChatFeaturesSettingsScreen extends EMUtilsScreen {
	public ChatFeaturesSettingsScreen(Screen parent) {
		super(parent, Text.translatable(EMUtilsTexts.SCREEN_CHAT_FEATURES));
	}

	@Override
	protected void initBody() {
		DirectionalLayoutWidget body = DirectionalLayoutWidget.vertical().spacing(4);
		body.getMainPositioner().alignHorizontalCenter();
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT,
			() -> EMUtilsClient.config().copyChat(),
			EMUtilsClient.config()::setCopyChat
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FORMATTING,
			() -> EMUtilsClient.config().copyChatFormatting(),
			EMUtilsClient.config()::setCopyChatFormatting
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FEEDBACK,
			() -> EMUtilsClient.config().copyChatFeedback(),
			EMUtilsClient.config()::setCopyChatFeedback
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMPS,
			() -> EMUtilsClient.config().chatTimestamps(),
			EMUtilsClient.config()::setChatTimestamps
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMP_24_HOUR,
			() -> EMUtilsClient.config().chatTimestamp24Hour(),
			EMUtilsClient.config()::setChatTimestamp24Hour
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SMART_CHAT_FILTERS,
			() -> EMUtilsClient.config().smartChatFilters(),
			EMUtilsClient.config()::setSmartChatFilters
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_TIME_WINDOW,
			() -> EMUtilsClient.config().duplicateMessageTimeWindow(),
			EMUtilsClient.config()::setDuplicateMessageTimeWindow
		));
		body.add(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERTS,
			() -> EMUtilsClient.config().chatMentionAlerts(),
			EMUtilsClient.config()::setChatMentionAlerts
		));
		body.add(new IntConfigSlider(
			0,
			0,
			200,
			20,
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_WINDOW,
			EMUtilsTexts.SUFFIX_SECONDS,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MIN,
			EMUtilsConfig.DUPLICATE_MESSAGE_WINDOW_MAX,
			() -> EMUtilsClient.config().duplicateMessageWindowSeconds(),
			EMUtilsClient.config()::setDuplicateMessageWindowSeconds
		));
		layout.addBody(body);
	}
}
