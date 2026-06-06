package net.emutils.client.emutils.chat.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.chat.ChatMentionAlerts;
import net.emutils.client.emutils.config.EMUtilsConfig;
import net.emutils.client.emutils.gui.EMUtilsScreen;
import net.emhelpers.client.gui.widget.ConfigToggleButton;
import net.emhelpers.client.gui.widget.IntConfigSlider;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

public final class ChatFeaturesSettingsScreen extends EMUtilsScreen {
	private static final String[] HIGHLIGHT_STYLES = {"Bold", "Italic", "Underline", "Normal"};

	public ChatFeaturesSettingsScreen(Screen parent) {
		super(parent, Component.translatable(EMUtilsTexts.SCREEN_CHAT_FEATURES));
	}

	@Override
	protected void initBody() {
		GridLayout.RowHelper adder = initTwoColumnBody();
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT,
			() -> EMUtilsClient.config().copyChat(),
			EMUtilsClient.config()::setCopyChat
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FORMATTING,
			() -> EMUtilsClient.config().copyChatFormatting(),
			EMUtilsClient.config()::setCopyChatFormatting
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_COPY_CHAT_FEEDBACK,
			() -> EMUtilsClient.config().copyChatFeedback(),
			EMUtilsClient.config()::setCopyChatFeedback
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMPS,
			() -> EMUtilsClient.config().chatTimestamps(),
			EMUtilsClient.config()::setChatTimestamps
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_TIMESTAMP_24_HOUR,
			() -> EMUtilsClient.config().chatTimestamp24Hour(),
			EMUtilsClient.config()::setChatTimestamp24Hour
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_SMART_CHAT_FILTERS,
			() -> EMUtilsClient.config().smartChatFilters(),
			EMUtilsClient.config()::setSmartChatFilters
		));
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_DUPLICATE_MESSAGE_TIME_WINDOW,
			() -> EMUtilsClient.config().duplicateMessageTimeWindow(),
			EMUtilsClient.config()::setDuplicateMessageTimeWindow
		));
		adder.addChild(new IntConfigSlider(
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
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERTS,
			() -> EMUtilsClient.config().chatMentionAlerts(),
			EMUtilsClient.config()::setChatMentionAlerts
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_CHAT_MENTION_ALERT_VOLUME,
			EMUtilsTexts.SUFFIX_PERCENT,
			EMUtilsConfig.CHAT_MENTION_VOLUME_MIN,
			EMUtilsConfig.CHAT_MENTION_VOLUME_MAX,
			() -> EMUtilsClient.config().chatMentionAlertVolume(),
			EMUtilsClient.config()::setChatMentionAlertVolume
		));
		adder.addChild(soundCycleButton());
		adder.addChild(ConfigToggleButton.create(
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT,
			() -> EMUtilsClient.config().chatMentionHighlight(),
			EMUtilsClient.config()::setChatMentionHighlight
		));
		adder.addChild(new IntConfigSlider(
			0,
			0,
			SETTINGS_BUTTON_WIDTH,
			20,
			EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_COLOR,
			"",
			0,
			0,
			() -> 0,
			v -> {}
		));
		adder.addChild(highlightStyleButton());
		adder.addChild(fullWidthSettingsButton(Component.translatable(EMUtilsTexts.OPTION_RESET_DEFAULTS), button -> {
			EMUtilsClient.config().resetChatDefaults();
			client.setScreen(new ChatFeaturesSettingsScreen(parent));
		}), SETTINGS_COLUMNS);
	}

	private Button soundCycleButton() {
		return Button.builder(soundLabel(), button -> {
			int next = (EMUtilsClient.config().chatMentionAlertSound() + 1) % 7;
			EMUtilsClient.config().setChatMentionAlertSound(next);
			button.setMessage(soundLabel());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private Component soundLabel() {
		String[] names = ChatMentionAlerts.soundNames();
		int index = Math.max(0, Math.min(names.length - 1, EMUtilsClient.config().chatMentionAlertSound()));
		return Component.translatable(EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_CHAT_MENTION_ALERT_SOUND),
			Component.literal(names[index])
		);
	}

	private Button highlightStyleButton() {
		return Button.builder(styleLabel(), button -> {
			int next = (EMUtilsClient.config().chatMentionHighlightStyle() + 1) % 4;
			EMUtilsClient.config().setChatMentionHighlightStyle(next);
			button.setMessage(styleLabel());
		}).width(SETTINGS_BUTTON_WIDTH).build();
	}

	private Component styleLabel() {
		int index = EMUtilsClient.config().chatMentionHighlightStyle();
		return Component.translatable(EMUtilsTexts.OPTION_VALUE,
			Component.translatable(EMUtilsTexts.OPTION_CHAT_MENTION_HIGHLIGHT_STYLE),
			Component.literal(HIGHLIGHT_STYLES[index])
		);
	}
}
