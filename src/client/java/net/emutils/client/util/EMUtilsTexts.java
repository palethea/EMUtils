package net.emutils.client.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EMUtilsTexts {
	public static final String NAME = "emutils.name";
	public static final String OPTIONS_BUTTON = "emutils.options.button";

	public static final String OPTION_ON = "emutils.option.on";
	public static final String OPTION_OFF = "emutils.option.off";
	public static final String OPTION_TOGGLE = "emutils.option.toggle";
	public static final String OPTION_VALUE = "emutils.option.value";

	public static final String HUB_TITLE = "emutils.hub.title";
	public static final String HUB_DEATH_WAYPOINTS = "emutils.hub.death_waypoints";
	public static final String HUB_AUTO_RECONNECT = "emutils.hub.auto_reconnect";
	public static final String HUB_SCREENSHOT_HELPER = "emutils.hub.screenshot_helper";
	public static final String HUB_COPY_CHAT = "emutils.hub.copy_chat";
	public static final String HUB_CHAT_FEATURES = "emutils.hub.chat_features";

	public static final String SCREEN_DEATH_WAYPOINTS = "emutils.screen.death_waypoints";
	public static final String SCREEN_AUTO_RECONNECT = "emutils.screen.auto_reconnect";
	public static final String SCREEN_SCREENSHOT_HELPER = "emutils.screen.screenshot_helper";
	public static final String SCREEN_COPY_CHAT = "emutils.screen.copy_chat";
	public static final String SCREEN_CHAT_FEATURES = "emutils.screen.chat_features";
	public static final String SCREEN_CURRENT_WAYPOINTS = "emutils.screen.current_waypoints";
	public static final String SCREEN_SCREENSHOT_GALLERY = "emutils.screen.screenshot_gallery";

	public static final String OPTION_AUTO_RECONNECT = "emutils.option.auto_reconnect";
	public static final String OPTION_SCREENSHOT_HELPER = "emutils.option.screenshot_helper";
	public static final String OPTION_SCREENSHOT_AUTO_COPY = "emutils.option.screenshot_auto_copy";
	public static final String OPTION_SCREENSHOT_GALLERY = "emutils.option.screenshot_gallery";
	public static final String OPTION_COPY_CHAT = "emutils.option.copy_chat";
	public static final String OPTION_COPY_CHAT_FORMATTING = "emutils.option.copy_chat_formatting";
	public static final String OPTION_COPY_CHAT_FEEDBACK = "emutils.option.copy_chat_feedback";
	public static final String OPTION_CHAT_TIMESTAMPS = "emutils.option.chat_timestamps";
	public static final String OPTION_CHAT_TIMESTAMP_24_HOUR = "emutils.option.chat_timestamp_24_hour";
	public static final String OPTION_SMART_CHAT_FILTERS = "emutils.option.smart_chat_filters";
	public static final String OPTION_DUPLICATE_MESSAGE_TIME_WINDOW = "emutils.option.duplicate_message_time_window";
	public static final String OPTION_DUPLICATE_MESSAGE_WINDOW = "emutils.option.duplicate_message_window";
	public static final String OPTION_CHAT_MENTION_ALERTS = "emutils.option.chat_mention_alerts";
	public static final String OPTION_DEATH_WAYPOINT = "emutils.option.death_waypoint";
	public static final String OPTION_CURRENT_WAYPOINTS = "emutils.option.current_waypoints";
	public static final String OPTION_RETRY_DELAY = "emutils.option.retry_delay";
	public static final String OPTION_WAYPOINT_OPACITY = "emutils.option.waypoint_opacity";
	public static final String OPTION_WAYPOINT_SIZE = "emutils.option.waypoint_size";
	public static final String OPTION_CLEAR_WAYPOINTS = "emutils.option.clear_waypoints";

	public static final String SUFFIX_SECONDS = "emutils.suffix.seconds";
	public static final String SUFFIX_PERCENT = "emutils.suffix.percent";

	public static final String CHAT_COPY_SUCCESS = "emutils.chat.copy.success";
	public static final String CHAT_SCREENSHOT_COPY_SUCCESS = "emutils.chat.screenshot.copy.success";
	public static final String CHAT_SCREENSHOT_COPY_FAILURE = "emutils.chat.screenshot.copy.failure";
	public static final String CHAT_SCREENSHOT_SAVED = "emutils.chat.screenshot.saved";
	public static final String CHAT_ACTION_COPY = "emutils.chat.action.copy";
	public static final String CHAT_ACTION_OPEN = "emutils.chat.action.open";
	public static final String CHAT_ACTION_FOLDER = "emutils.chat.action.folder";
	public static final String CHAT_HOVER_COPY_SCREENSHOT = "emutils.chat.hover.copy_screenshot";
	public static final String CHAT_HOVER_OPEN_IMAGE = "emutils.chat.hover.open_image";
	public static final String CHAT_HOVER_OPEN_FOLDER = "emutils.chat.hover.open_folder";
	public static final String CHAT_MENTION_TOAST_TITLE = "emutils.chat.mention.toast.title";
	public static final String CHAT_MENTION_TOAST_DESCRIPTION = "emutils.chat.mention.toast.description";

	public static final String GALLERY_EMPTY = "emutils.gallery.empty";

	public static final String DEATH_LABEL_LAST = "emutils.death_waypoint.label.last";
	public static final String DEATH_LABEL_NUMBERED = "emutils.death_waypoint.label.numbered";
	public static final String DEATH_DISTANCE = "emutils.death_waypoint.distance";
	public static final String DEATH_PROMPT = "emutils.death_waypoint.prompt";
	public static final String DEATH_ACTION_REMOVE = "emutils.death_waypoint.action.remove";
	public static final String DEATH_ACTION_KEEP = "emutils.death_waypoint.action.keep";
	public static final String DEATH_ACTION_COPY_COORDS = "emutils.death_waypoint.action.copy_coords";
	public static final String DEATH_HOVER_REMOVE = "emutils.death_waypoint.hover.remove";
	public static final String DEATH_HOVER_KEEP = "emutils.death_waypoint.hover.keep";
	public static final String DEATH_CLEARED = "emutils.death_waypoint.cleared";
	public static final String DEATH_KEPT = "emutils.death_waypoint.kept";
	public static final String DEATH_CLEARED_WORLD = "emutils.death_waypoint.cleared_world";
	public static final String DEATH_NONE_WORLD = "emutils.death_waypoint.none_world";
	public static final String DEATH_COORDS_COPIED = "emutils.death_waypoint.coords_copied";

	public static final String RECONNECT_UNAVAILABLE = "emutils.reconnect.unavailable";
	public static final String RECONNECT_COUNTDOWN = "emutils.reconnect.countdown";
	public static final String RECONNECT_RETRYING = "emutils.reconnect.retrying";

	private EMUtilsTexts() {
	}

	public static MutableText greenPrefix() {
		return Text.translatable(NAME).formatted(Formatting.GREEN);
	}

	public static Text toggleLabel(String optionKey) {
		return Text.translatable(optionKey);
	}
}
