package net.emutils.client.emutils.util;

import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class EMUtilsTexts {

    public static final String NAME = "emutils.name";
    public static final String OPTIONS_BUTTON = "emutils.options.button";

    public static final String HUB_MODERN_TITLE = "emutils.hub.modern_title";
    public static final String HUB_MODERN_OPEN = "emutils.hub.modern_open";
    public static final String HUB_CLASSIC_OPEN = "emutils.hub.classic_open";
    public static final String HUB_ACTION_OPEN = "emutils.hub.action.open";
    public static final String HUB_ACTION_UNAVAILABLE =
        "emutils.hub.action.unavailable";
    public static final String HUB_OPEN_PACK_MANAGER =
        "emutils.hub.open_pack_manager";
    public static final String HUB_OPEN_SCRIPT_MANAGER =
        "emutils.hub.open_script_manager";
    public static final String HUB_OPEN_COMMAND_SHORTCUTS =
        "emutils.hub.open_command_shortcuts";
    public static final String HUB_SEARCH_PLACEHOLDER =
        "emutils.hub.search_placeholder";
    public static final String HUB_EMPTY_SEARCH = "emutils.hub.empty_search";
    public static final String HUB_GROUP_RENDER = "emutils.hub.group.render";
    public static final String HUB_GROUP_HUD = "emutils.hub.group.hud";
    public static final String HUB_GROUP_UTILITY = "emutils.hub.group.utility";
    public static final String HUB_GROUP_MANAGEMENT =
        "emutils.hub.group.management";
    public static final String HUB_GROUP_QOL = "emutils.hub.group.qol";
    public static final String HUB_FEATURE_CHAT_DESC =
        "emutils.hub.feature.chat.desc";
    public static final String HUB_FEATURE_WAYPOINTS_DESC =
        "emutils.hub.feature.waypoints.desc";
    public static final String HUB_FEATURE_CURRENT_WAYPOINTS_DESC =
        "emutils.hub.feature.current_waypoints.desc";
    public static final String HUB_FEATURE_AUTO_RECONNECT_DESC =
        "emutils.hub.feature.auto_reconnect.desc";
    public static final String HUB_FEATURE_SCREENSHOT_DESC =
        "emutils.hub.feature.screenshot.desc";
    public static final String HUB_FEATURE_SCREENSHOT_GALLERY_DESC =
        "emutils.hub.feature.screenshot_gallery.desc";
    public static final String HUB_FEATURE_PACK_MANAGER_DESC =
        "emutils.hub.feature.pack_manager.desc";
    public static final String HUB_FEATURE_SCRIPT_MANAGER_DESC =
        "emutils.hub.feature.script_manager.desc";
    public static final String HUB_FEATURE_COMMAND_SHORTCUTS_DESC =
        "emutils.hub.feature.command_shortcuts.desc";
    public static final String HUB_FEATURE_HUD_DESC =
        "emutils.hub.feature.hud.desc";
    public static final String HUB_FEATURE_FOOD_HUD_DESC =
        "emutils.hub.feature.food_hud.desc";
    public static final String HUB_FEATURE_ZOOM_DESC =
        "emutils.hub.feature.zoom.desc";
    public static final String HUB_FEATURE_CAPES_DESC =
        "emutils.hub.feature.capes.desc";
    public static final String HUB_FEATURE_SPOTIFY_DESC =
        "emutils.hub.feature.spotify.desc";
    public static final String HUB_FEATURE_INVENTORY_DESC =
        "emutils.hub.feature.inventory.desc";
    public static final String HUB_FEATURE_FULLBRIGHT_DESC =
        "emutils.hub.feature.fullbright.desc";
    public static final String HUB_FEATURE_CLEAR_WEATHER_DESC =
        "emutils.hub.feature.clear_weather.desc";
    public static final String HUB_FEATURE_NO_FIRE_OVERLAY_DESC =
        "emutils.hub.feature.no_fire_overlay.desc";
    public static final String HUB_FEATURE_LOW_FIRE_OVERLAY_DESC =
        "emutils.hub.feature.low_fire_overlay.desc";
    public static final String HUB_FEATURE_NO_NAUSEA_DESC =
        "emutils.hub.feature.no_nausea.desc";
    public static final String HUB_FEATURE_NO_SPYGLASS_OVERLAY_DESC =
        "emutils.hub.feature.no_spyglass_overlay.desc";
    public static final String HUB_FEATURE_FAST_PLACE_DESC =
        "emutils.hub.feature.fast_place.desc";
    public static final String HUB_FEATURE_ANTI_DURABILITY_BREAK_DESC =
        "emutils.hub.feature.anti_durability_break.desc";
    public static final String HUB_FEATURE_AUTO_TOOL_DESC =
        "emutils.hub.feature.auto_tool.desc";
    public static final String HUB_FEATURE_AUTO_FLIGHT_DESC =
        "emutils.hub.feature.auto_flight.desc";
    public static final String HUB_FEATURE_SAFE_WALK_DESC =
        "emutils.hub.feature.safe_walk.desc";
    public static final String HUB_FEATURE_PLACE_BELOW_DESC =
        "emutils.hub.feature.place_below.desc";
    public static final String HUB_FEATURE_FREE_CAMERA_DESC =
        "emutils.hub.feature.free_camera.desc";
    public static final String HUB_FEATURE_NO_FOG_DESC =
        "emutils.hub.feature.no_fog.desc";
    public static final String HUB_FEATURE_CLEAR_UNDERWATER_DESC =
        "emutils.hub.feature.clear_underwater.desc";
    public static final String HUB_FEATURE_CLEAR_LAVA_DESC =
        "emutils.hub.feature.clear_lava.desc";
    public static final String HUB_FEATURE_NO_ENVIRONMENT_FOG_DESC =
        "emutils.hub.feature.no_environment_fog.desc";
    public static final String HUB_FEATURE_NO_NETHER_PARTICLES_DESC =
        "emutils.hub.feature.no_nether_particles.desc";
    public static final String HUB_FEATURE_NO_HURT_CAM_DESC =
        "emutils.hub.feature.no_hurt_cam.desc";
    public static final String HUB_FEATURE_FREELOOK_DESC =
        "emutils.hub.feature.freelook.desc";
    public static final String HUB_FEATURE_BEACON_RADIUS_DESC =
        "emutils.hub.feature.beacon_radius.desc";
    public static final String HUB_FEATURE_OWN_NAMETAG_DESC =
        "emutils.hub.feature.own_nametag.desc";
    public static final String HUB_FEATURE_SHULKER_PREVIEW_DESC =
        "emutils.hub.feature.shulker_preview.desc";
    public static final String HUB_FEATURE_BUNDLE_PREVIEW_DESC =
        "emutils.hub.feature.bundle_preview.desc";

    public static final String OPTION_ON = "emutils.option.on";
    public static final String OPTION_OFF = "emutils.option.off";
    public static final String OPTION_TOGGLE = "emutils.option.toggle";
    public static final String OPTION_VALUE = "emutils.option.value";
    public static final String OPTION_RESET_DEFAULTS =
        "emutils.option.reset_defaults";

    public static final String HUB_TITLE = "emutils.hub.title";
    public static final String HUB_DEATH_WAYPOINTS =
        "emutils.hub.death_waypoints";
    public static final String HUB_AUTO_RECONNECT =
        "emutils.hub.auto_reconnect";
    public static final String HUB_SCREENSHOT_HELPER =
        "emutils.hub.screenshot_helper";
    public static final String HUB_COPY_CHAT = "emutils.hub.copy_chat";
    public static final String HUB_CHAT_FEATURES = "emutils.hub.chat_features";
    public static final String HUB_MANAGERS = "emutils.hub.managers";
    public static final String HUB_HUD_OVERLAY = "emutils.hub.hud_overlay";
    public static final String HUB_FOOD_HUD = "emutils.hub.food_hud";
    public static final String HUB_ZOOM = "emutils.hub.zoom";
    public static final String HUB_TWEAKS = "emutils.hub.tweaks";
    public static final String HUB_CAPES = "emutils.hub.capes";
    public static final String HUB_SPOTIFY_PLAYER =
        "emutils.hub.spotify_player";
    public static final String HUB_INVENTORY_TOOLS =
        "emutils.hub.inventory_tools";
    public static final String SCREEN_DEATH_WAYPOINTS =
        "emutils.screen.death_waypoints";
    public static final String SCREEN_AUTO_RECONNECT =
        "emutils.screen.auto_reconnect";
    public static final String SCREEN_SCREENSHOT_HELPER =
        "emutils.screen.screenshot_helper";
    public static final String SCREEN_COPY_CHAT = "emutils.screen.copy_chat";
    public static final String SCREEN_CHAT_FEATURES =
        "emutils.screen.chat_features";
    public static final String SCREEN_MANAGERS = "emutils.screen.managers";
    public static final String SCREEN_CURRENT_WAYPOINTS =
        "emutils.screen.current_waypoints";
    public static final String SCREEN_SCREENSHOT_GALLERY =
        "emutils.screen.screenshot_gallery";
    public static final String SCREEN_HUD_OVERLAY =
        "emutils.screen.hud_overlay";
    public static final String SCREEN_FOOD_HUD = "emutils.screen.food_hud";
    public static final String SCREEN_ZOOM = "emutils.screen.zoom";
    public static final String SCREEN_TWEAKS = "emutils.screen.tweaks";
    public static final String SCREEN_PACK_MANAGER =
        "emutils.screen.pack_manager";
    public static final String SCREEN_CAPES = "emutils.screen.capes";
    public static final String SCREEN_SPOTIFY_PLAYER =
        "emutils.screen.spotify_player";
    public static final String SCREEN_INVENTORY_TOOLS =
        "emutils.screen.inventory_tools";
    public static final String SCREEN_SETTINGS_CHOOSER =
        "emutils.screen.settings_chooser";
    public static final String SETTINGS_CHOOSER_EMUTILS =
        "emutils.settings_chooser.emutils";
    public static final String SCREEN_SCRIPT_MANAGER =
        "emutils.screen.script_manager";
    public static final String SCREEN_COMMAND_SHORTCUTS =
        "emutils.screen.command_shortcuts";
    public static final String SCREEN_ADD_COMMAND_SHORTCUT =
        "emutils.screen.add_command_shortcut";
    public static final String SCREEN_EDIT_COMMAND_SHORTCUT =
        "emutils.screen.edit_command_shortcut";

    public static final String OPTION_AUTO_RECONNECT =
        "emutils.option.auto_reconnect";
    public static final String OPTION_AUTO_RECONNECT_MAX_TRIES =
        "emutils.option.auto_reconnect_max_tries";
    public static final String OPTION_AUTO_RECONNECT_UNLIMITED =
        "emutils.option.auto_reconnect_unlimited";
    public static final String OPTION_SCREENSHOT_HELPER =
        "emutils.option.screenshot_helper";
    public static final String OPTION_SCREENSHOT_AUTO_COPY =
        "emutils.option.screenshot_auto_copy";
    public static final String OPTION_SCREENSHOT_METADATA =
        "emutils.option.screenshot_metadata";
    public static final String OPTION_SCREENSHOT_GALLERY =
        "emutils.option.screenshot_gallery";
    public static final String OPTION_SCREENSHOT_SORT =
        "emutils.option.screenshot_sort";
    public static final String OPTION_SCREENSHOT_DELETE_CONFIRMATION =
        "emutils.option.screenshot_delete_confirmation";
    public static final String OPTION_SCREENSHOT_MAX_COUNT =
        "emutils.option.screenshot_max_count";
    public static final String OPTION_COPY_CHAT = "emutils.option.copy_chat";
    public static final String OPTION_COPY_CHAT_FORMATTING =
        "emutils.option.copy_chat_formatting";
    public static final String OPTION_COPY_CHAT_FEEDBACK =
        "emutils.option.copy_chat_feedback";
    public static final String OPTION_CHAT_TIMESTAMPS =
        "emutils.option.chat_timestamps";
    public static final String OPTION_CHAT_TIMESTAMP_24_HOUR =
        "emutils.option.chat_timestamp_24_hour";
    public static final String OPTION_SMART_CHAT_FILTERS =
        "emutils.option.smart_chat_filters";
    public static final String OPTION_DUPLICATE_MESSAGE_TIME_WINDOW =
        "emutils.option.duplicate_message_time_window";
    public static final String OPTION_DUPLICATE_MESSAGE_WINDOW =
        "emutils.option.duplicate_message_window";
    public static final String OPTION_CHAT_MENTION_ALERTS =
        "emutils.option.chat_mention_alerts";
    public static final String OPTION_CHAT_MENTION_HIGHLIGHT =
        "emutils.option.chat_mention_highlight";
    public static final String OPTION_CHAT_MENTION_HIGHLIGHT_COLOR =
        "emutils.option.chat_mention_highlight_color";
    public static final String OPTION_CHAT_MENTION_HIGHLIGHT_STYLE =
        "emutils.option.chat_mention_highlight_style";
    public static final String OPTION_CHAT_MENTION_ALERT_VOLUME =
        "emutils.option.chat_mention_alert_volume";
    public static final String OPTION_CHAT_MENTION_ALERT_SOUND =
        "emutils.option.chat_mention_alert_sound";
    public static final String OPTION_COMMAND_SHORTCUTS =
        "emutils.option.command_shortcuts";
    public static final String OPTION_MANAGE_COMMAND_SHORTCUTS =
        "emutils.option.manage_command_shortcuts";
    public static final String OPTION_ADD_COMMAND_SHORTCUT =
        "emutils.option.add_command_shortcut";
    public static final String OPTION_CLEAR_COMMAND_SHORTCUTS =
        "emutils.option.clear_command_shortcuts";
    public static final String OPTION_DEATH_WAYPOINT =
        "emutils.option.death_waypoint";
    public static final String OPTION_DEATH_WAYPOINT_AUTO_COPY =
        "emutils.option.death_waypoint_auto_copy";
    public static final String OPTION_DEATH_COORD_FORMAT =
        "emutils.option.death_coord_format";
    public static final String OPTION_CURRENT_WAYPOINTS =
        "emutils.option.current_waypoints";
    public static final String OPTION_RETRY_DELAY =
        "emutils.option.retry_delay";
    public static final String OPTION_WAYPOINT_OPACITY =
        "emutils.option.waypoint_opacity";
    public static final String OPTION_WAYPOINT_SIZE =
        "emutils.option.waypoint_size";
    public static final String OPTION_CLEAR_WAYPOINTS =
        "emutils.option.clear_waypoints";
    public static final String OPTION_HUD_OVERLAY =
        "emutils.option.hud_overlay";
    public static final String OPTION_HUD_POSITION =
        "emutils.option.hud_position";
    public static final String OPTION_HUD_SHOW_ICONS =
        "emutils.option.hud_show_icons";
    public static final String OPTION_HUD_BACKGROUND_OPACITY =
        "emutils.option.hud_background_opacity";
    public static final String OPTION_HUD_SCALE = "emutils.option.hud_scale";
    public static final String OPTION_HUD_HIDE_WITH_DEBUG =
        "emutils.option.hud_hide_with_debug";
    public static final String OPTION_HUD_LAYOUT_MODE =
        "emutils.option.hud_layout_mode";
    public static final String OPTION_HUD_LAYOUT_EDITOR =
        "emutils.option.hud_layout_editor";
    public static final String SCREEN_HUD_LAYOUT_EDITOR =
        "emutils.screen.hud_layout_editor";
    public static final String HUD_LAYOUT_EDITOR_HINT =
        "emutils.hud.layout_editor.hint";
    public static final String HUD_LAYOUT_EDITOR_SAVE =
        "emutils.hud.layout_editor.save";
    public static final String HUD_LAYOUT_EDITOR_CANCEL =
        "emutils.hud.layout_editor.cancel";
    public static final String HUD_LAYOUT_EDITOR_RESET_ALL =
        "emutils.hud.layout_editor.reset_all";
    public static final String HUD_LAYOUT_MODE_ANCHOR =
        "emutils.hud.layout_mode.anchor";
    public static final String HUD_LAYOUT_MODE_CUSTOM =
        "emutils.hud.layout_mode.custom";
    public static final String HUD_ELEMENT_INFO_OVERLAY =
        "emutils.hud.element.info_overlay";
    public static final String HUD_ELEMENT_SPOTIFY =
        "emutils.hud.element.spotify";
    public static final String HUD_ELEMENT_INVENTORY_PREVIEW =
        "emutils.hud.element.inventory_preview";
    public static final String OPTION_HUD_COORDINATES =
        "emutils.option.hud_coordinates";
    public static final String OPTION_HUD_NETHER_COORDINATES =
        "emutils.option.hud_nether_coordinates";
    public static final String OPTION_HUD_CHUNK_REGION =
        "emutils.option.hud_chunk_region";
    public static final String OPTION_HUD_BIOME = "emutils.option.hud_biome";
    public static final String OPTION_HUD_PING = "emutils.option.hud_ping";
    public static final String OPTION_HUD_FPS = "emutils.option.hud_fps";
    public static final String OPTION_HUD_FACING = "emutils.option.hud_facing";
    public static final String OPTION_HUD_MEMORY = "emutils.option.hud_memory";
    public static final String OPTION_HUD_SERVER_TIME =
        "emutils.option.hud_server_time";
    public static final String OPTION_HUD_REAL_TIME =
        "emutils.option.hud_real_time";
    public static final String OPTION_FOOD_HUD = "emutils.option.food_hud";
    public static final String OPTION_FOOD_HUD_SATURATION_OVERLAY =
        "emutils.option.food_hud_saturation_overlay";
    public static final String OPTION_FOOD_HUD_HELD_FOOD_OVERLAY =
        "emutils.option.food_hud_held_food_overlay";
    public static final String OPTION_FOOD_HUD_OFFHAND_OVERLAY =
        "emutils.option.food_hud_offhand_overlay";
    public static final String OPTION_FOOD_HUD_EXHAUSTION_UNDERLAY =
        "emutils.option.food_hud_exhaustion_underlay";
    public static final String OPTION_FOOD_HUD_TOOLTIPS =
        "emutils.option.food_hud_tooltips";
    public static final String OPTION_FOOD_HUD_TOOLTIP_ALWAYS =
        "emutils.option.food_hud_tooltip_always";
    public static final String OPTION_FOOD_HUD_VANILLA_ANIMATIONS =
        "emutils.option.food_hud_vanilla_animations";
    public static final String OPTION_ZOOM = "emutils.option.zoom";
    public static final String OPTION_ZOOM_AMOUNT =
        "emutils.option.zoom_amount";
    public static final String OPTION_ZOOM_CINEMATIC_CAMERA =
        "emutils.option.zoom_cinematic_camera";
    public static final String OPTION_ZOOM_HIDE_HAND =
        "emutils.option.zoom_hide_hand";
    public static final String OPTION_ZOOM_SMOOTH_TRANSITION =
        "emutils.option.zoom_smooth_transition";
    public static final String OPTION_ZOOM_TRANSITION_SPEED =
        "emutils.option.zoom_transition_speed";
    public static final String OPTION_ZOOM_HIDE_HUD =
        "emutils.option.zoom_hide_hud";
    public static final String OPTION_ZOOM_OUT_SPEED =
        "emutils.option.zoom_out_speed";
    public static final String OPTION_TWEAK_FULLBRIGHT =
        "emutils.option.tweak_fullbright";
    public static final String OPTION_TWEAK_FULLBRIGHT_STRENGTH =
        "emutils.option.tweak_fullbright_strength";
    public static final String OPTION_TWEAK_NO_FOG =
        "emutils.option.tweak_no_fog";
    public static final String OPTION_TWEAK_CLEAR_UNDERWATER =
        "emutils.option.tweak_clear_underwater";
    public static final String OPTION_TWEAK_CLEAR_LAVA =
        "emutils.option.tweak_clear_lava";
    public static final String OPTION_TWEAK_NO_ENVIRONMENT_FOG =
        "emutils.option.tweak_no_environment_fog";
    public static final String OPTION_TWEAK_NO_NETHER_PARTICLES =
        "emutils.option.tweak_no_nether_particles";
    public static final String OPTION_TWEAK_NO_HURT_CAM =
        "emutils.option.tweak_no_hurt_cam";
    public static final String OPTION_TWEAK_FREELOOK =
        "emutils.option.tweak_freelook";
    public static final String OPTION_BEACON_RADIUS_OUTLINE =
        "emutils.option.beacon_radius_outline";
    public static final String OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW =
        "emutils.option.tweak_shulker_tooltip_preview";
    public static final String OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW =
        "emutils.option.tweak_bundle_tooltip_preview";
    public static final String OPTION_TWEAK_CLEAR_WEATHER =
        "emutils.option.tweak_clear_weather";
    public static final String OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN =
        "emutils.option.tweak_clear_weather_hide_rain";
    public static final String OPTION_TWEAK_CLEAR_WEATHER_HIDE_SNOW =
        "emutils.option.tweak_clear_weather_hide_snow";
    public static final String OPTION_TWEAK_CLEAR_WEATHER_HIDE_RAIN_EFFECTS =
        "emutils.option.tweak_clear_weather_hide_rain_effects";
    public static final String OPTION_TWEAK_NO_FIRE_OVERLAY =
        "emutils.option.tweak_no_fire_overlay";
    public static final String OPTION_TWEAK_LOW_FIRE_OVERLAY =
        "emutils.option.tweak_low_fire_overlay";
    public static final String OPTION_TWEAK_NO_NAUSEA =
        "emutils.option.tweak_no_nausea";
    public static final String OPTION_TWEAK_NO_SPYGLASS_OVERLAY =
        "emutils.option.tweak_no_spyglass_overlay";
    public static final String OPTION_TWEAK_FAST_PLACE =
        "emutils.option.tweak_fast_place";
    public static final String OPTION_TWEAK_ANTI_DURABILITY_BREAK =
        "emutils.option.tweak_anti_durability_break";
    public static final String OPTION_AUTO_TOOL =
        "emutils.option.auto_tool";
    public static final String OPTION_AUTO_TOOL_MODE =
        "emutils.option.auto_tool_mode";
    public static final String OPTION_AUTO_TOOL_MODE_LEGIT =
        "emutils.option.auto_tool_mode.legit";
    public static final String OPTION_AUTO_TOOL_MODE_UNFAIR =
        "emutils.option.auto_tool_mode.unfair";
    public static final String OPTION_TWEAK_SAFE_WALK =
        "emutils.option.tweak_safe_walk";
    public static final String OPTION_TWEAK_PLACE_BELOW =
        "emutils.option.tweak_place_below";
    public static final String OPTION_TWEAK_FREE_CAMERA =
        "emutils.option.tweak_free_camera";
    public static final String OPTION_AUTO_FLIGHT_GEAR =
        "emutils.option.auto_flight_gear";
    public static final String OPTION_TWEAK_AUTO_SWITCH_ELYTRA =
        "emutils.option.tweak_auto_switch_elytra";
    public static final String OPTION_TWEAK_AUTO_SWITCH_ROCKETS =
        "emutils.option.tweak_auto_switch_rockets";
    public static final String OPTION_AUTO_SWITCH_ROCKETS_HOTBAR_SLOT =
        "emutils.option.auto_switch_rockets_hotbar_slot";
    public static final String OPTION_AUTO_FLIGHT_IGNORE_SHORT_FALLS =
        "emutils.option.auto_flight_ignore_short_falls";
    public static final String OPTION_AUTO_FLIGHT_DOUBLE_JUMP =
        "emutils.option.auto_flight_double_jump";
    public static final String OPTION_TWEAK_OWN_NAMETAG =
        "emutils.option.tweak_own_nametag";
    public static final String OPTION_PACK_MANAGER =
        "emutils.option.pack_manager";
    public static final String OPTION_PACK_MANAGER_SHOW_SHADERS_WITHOUT_IRIS =
        "emutils.option.pack_manager_show_shaders_without_iris";
    public static final String OPTION_PACK_MANAGER_SEARCH_LIMIT =
        "emutils.option.pack_manager_search_limit";
    public static final String OPTION_CUSTOM_CAPES =
        "emutils.option.custom_capes";
    public static final String OPTION_CAPE_OPTIFINE =
        "emutils.option.cape.optifine";
    public static final String OPTION_CAPE_LABYMOD =
        "emutils.option.cape.labymod";
    public static final String OPTION_CAPE_MINECRAFTCAPES =
        "emutils.option.cape.minecraftcapes";
    public static final String OPTION_CAPE_COSMETICA =
        "emutils.option.cape.cosmetica";
    public static final String OPTION_CAPE_CLOAKSPLUS =
        "emutils.option.cape.cloaksplus";
    public static final String OPTION_CAPE_PREFERRED_PROVIDER =
        "emutils.option.cape.preferred_provider";
    public static final String OPTION_CAPE_PREFERRED_AUTO =
        "emutils.option.cape.preferred.auto";
    public static final String CAPES_HINT = "emutils.capes.hint";
    public static final String OPTION_SPOTIFY_PLAYER =
        "emutils.option.spotify_player";
    public static final String OPTION_SPOTIFY_HUD_OVERLAY =
        "emutils.option.spotify_hud_overlay";
    public static final String OPTION_SPOTIFY_HUD_BACKGROUND_OPACITY =
        "emutils.option.spotify_hud_background_opacity";
    public static final String OPTION_SPOTIFY_HUD_SCALE =
        "emutils.option.spotify_hud_scale";
    public static final String OPTION_INVENTORY_TOOLS =
        "emutils.option.inventory_tools";
    public static final String OPTION_SLOT_LOCKING =
        "emutils.option.slot_locking";
    public static final String OPTION_SLOT_BINDING =
        "emutils.option.slot_binding";
    public static final String OPTION_SLOT_LOCK_COLOR =
        "emutils.option.slot_lock_color";
    public static final String OPTION_BOUND_SLOT_COLOR =
        "emutils.option.bound_slot_color";
    public static final String OPTION_SLOT_BINDING_SHOW_ICONS =
        "emutils.option.slot_binding_show_icons";
    public static final String OPTION_SLOT_BINDING_LOCK_BOUND_SLOTS =
        "emutils.option.slot_binding_lock_bound_slots";
    public static final String OPTION_HOVER_TRANSFER =
        "emutils.option.hover_transfer";
    public static final String OPTION_SORT_BUTTONS =
        "emutils.option.sort_buttons";
    public static final String OPTION_SORT_SPEED =
        "emutils.option.sort_speed";
    public static final String OPTION_QUICK_STACK =
        "emutils.option.quick_stack";
    public static final String OPTION_QUICK_STACK_SPEED =
        "emutils.option.quick_stack_speed";
    public static final String OPTION_INVENTORY_PREVIEW =
        "emutils.option.inventory_preview";
    public static final String OPTION_PRESERVE_CONTAINER_CURSOR =
        "emutils.option.preserve_container_cursor";
    public static final String OPTION_INVENTORY_PREVIEW_OPACITY =
        "emutils.option.inventory_preview_opacity";
    public static final String INVENTORY_LOCK_COLOR_RED =
        "emutils.container.lock_color.red";
    public static final String INVENTORY_LOCK_COLOR_YELLOW =
        "emutils.container.lock_color.yellow";
    public static final String INVENTORY_LOCK_COLOR_GREEN =
        "emutils.container.lock_color.green";
    public static final String INVENTORY_LOCK_COLOR_BLUE =
        "emutils.container.lock_color.blue";
    public static final String INVENTORY_BOUND_COLOR_GRAY =
        "emutils.container.bound_color.gray";
    public static final String INVENTORY_BOUND_COLOR_WHITE =
        "emutils.container.bound_color.white";
    public static final String INVENTORY_BOUND_COLOR_DARK_BLUE =
        "emutils.container.bound_color.dark_blue";
    public static final String SPOTIFY_PLAYER_TITLE =
        "emutils.spotify.player.title";
    public static final String SPOTIFY_PLAYER_NO_TRACK =
        "emutils.spotify.player.no_track";
    public static final String SPOTIFY_PLAYER_UNAVAILABLE =
        "emutils.spotify.player.unavailable";
    public static final String SPOTIFY_PLAYER_HINT =
        "emutils.spotify.player.hint";
    public static final String SPOTIFY_PREVIOUS =
        "emutils.spotify.action.previous";
    public static final String SPOTIFY_PLAY_PAUSE =
        "emutils.spotify.action.play_pause";
    public static final String SPOTIFY_NEXT = "emutils.spotify.action.next";
    public static final String HUD_ANCHOR_TOP_LEFT =
        "emutils.hud.anchor.top_left";
    public static final String HUD_ANCHOR_TOP_CENTER =
        "emutils.hud.anchor.top_center";
    public static final String HUD_ANCHOR_TOP_RIGHT =
        "emutils.hud.anchor.top_right";
    public static final String HUD_ANCHOR_BOTTOM_LEFT =
        "emutils.hud.anchor.bottom_left";
    public static final String HUD_ANCHOR_BOTTOM_CENTER =
        "emutils.hud.anchor.bottom_center";
    public static final String HUD_ANCHOR_BOTTOM_RIGHT =
        "emutils.hud.anchor.bottom_right";
    public static final String HUD_COORDS = "emutils.hud.coords";
    public static final String HUD_CHUNK_REGION = "emutils.hud.chunk_region";
    public static final String HUD_BIOME = "emutils.hud.biome";
    public static final String HUD_PING = "emutils.hud.ping";
    public static final String HUD_FPS = "emutils.hud.fps";
    public static final String HUD_FACING = "emutils.hud.facing";
    public static final String HUD_MEMORY = "emutils.hud.memory";
    public static final String HUD_SERVER_TIME = "emutils.hud.server_time";
    public static final String HUD_REAL_TIME = "emutils.hud.real_time";

    public static final String SUFFIX_SECONDS = "emutils.suffix.seconds";
    public static final String SUFFIX_PERCENT = "emutils.suffix.percent";

    public static final String CHAT_COPY_SUCCESS = "emutils.chat.copy.success";
    public static final String CHAT_SCREENSHOT_COPY_SUCCESS =
        "emutils.chat.screenshot.copy.success";
    public static final String CHAT_SCREENSHOT_COPY_FAILURE =
        "emutils.chat.screenshot.copy.failure";
    public static final String CHAT_SCREENSHOT_DELETE_SUCCESS =
        "emutils.chat.screenshot.delete.success";
    public static final String CHAT_SCREENSHOT_DELETE_FAILURE =
        "emutils.chat.screenshot.delete.failure";
    public static final String CHAT_SCREENSHOT_SAVED =
        "emutils.chat.screenshot.saved";
    public static final String CHAT_ACTION_COPY = "emutils.chat.action.copy";
    public static final String CHAT_ACTION_OPEN = "emutils.chat.action.open";
    public static final String CHAT_ACTION_FOLDER =
        "emutils.chat.action.folder";
    public static final String CHAT_HOVER_COPY_SCREENSHOT =
        "emutils.chat.hover.copy_screenshot";
    public static final String CHAT_HOVER_OPEN_IMAGE =
        "emutils.chat.hover.open_image";
    public static final String CHAT_HOVER_OPEN_FOLDER =
        "emutils.chat.hover.open_folder";
    public static final String CHAT_MENTION_TOAST_TITLE =
        "emutils.chat.mention.toast.title";
    public static final String CHAT_MENTION_TOAST_DESCRIPTION =
        "emutils.chat.mention.toast.description";

    public static final String GALLERY_EMPTY = "emutils.gallery.empty";
    public static final String GALLERY_ACTION_DELETE =
        "emutils.gallery.action.delete";
    public static final String GALLERY_DELETE_TITLE =
        "emutils.gallery.delete.title";
    public static final String GALLERY_DELETE_MESSAGE =
        "emutils.gallery.delete.message";
    public static final String GALLERY_SORT_NEWEST_FIRST =
        "emutils.gallery.sort.newest_first";
    public static final String GALLERY_SORT_OLDEST_FIRST =
        "emutils.gallery.sort.oldest_first";

    public static final String DEATH_LABEL_LAST =
        "emutils.death_waypoint.label.last";
    public static final String DEATH_LABEL_NUMBERED =
        "emutils.death_waypoint.label.numbered";
    public static final String DEATH_DISTANCE =
        "emutils.death_waypoint.distance";
    public static final String DEATH_PROMPT = "emutils.death_waypoint.prompt";
    public static final String DEATH_ACTION_REMOVE =
        "emutils.death_waypoint.action.remove";
    public static final String DEATH_ACTION_KEEP =
        "emutils.death_waypoint.action.keep";
    public static final String DEATH_ACTION_COPY_COORDS =
        "emutils.death_waypoint.action.copy_coords";
    public static final String DEATH_HOVER_REMOVE =
        "emutils.death_waypoint.hover.remove";
    public static final String DEATH_HOVER_KEEP =
        "emutils.death_waypoint.hover.keep";
    public static final String DEATH_CLEARED = "emutils.death_waypoint.cleared";
    public static final String DEATH_KEPT = "emutils.death_waypoint.kept";
    public static final String DEATH_CLEARED_WORLD =
        "emutils.death_waypoint.cleared_world";
    public static final String DEATH_NONE_WORLD =
        "emutils.death_waypoint.none_world";
    public static final String DEATH_COORDS_COPIED =
        "emutils.death_waypoint.coords_copied";
    public static final String DEATH_COORD_FORMAT_PLAIN =
        "emutils.death_waypoint.coord_format.plain";
    public static final String DEATH_COORD_FORMAT_COMMA =
        "emutils.death_waypoint.coord_format.comma";
    public static final String DEATH_COORD_FORMAT_TP_COMMAND =
        "emutils.death_waypoint.coord_format.tp_command";

    // Waypoint system (new unified)
    public static final String HUB_WAYPOINTS = "emutils.hub.waypoints";
    public static final String SCREEN_WAYPOINTS = "emutils.screen.waypoints";
    public static final String SCREEN_ADD_WAYPOINT =
        "emutils.screen.add_waypoint";
    public static final String OPTION_WAYPOINTS = "emutils.option.waypoints";
    public static final String OPTION_WAYPOINT_AUTO_COPY =
        "emutils.option.waypoint_auto_copy";
    public static final String OPTION_WAYPOINT_COORD_FORMAT =
        "emutils.option.waypoint_coord_format";
    public static final String OPTION_WAYPOINT_DEFAULT_DEATH_COLOR =
        "emutils.option.waypoint_default_death_color";
    public static final String OPTION_WAYPOINT_DEFAULT_CUSTOM_COLOR =
        "emutils.option.waypoint_default_custom_color";
    public static final String OPTION_ADD_WAYPOINT =
        "emutils.option.add_waypoint";
    public static final String WAYPOINT_TYPE_DEATH =
        "emutils.waypoint.type.death";
    public static final String WAYPOINT_TYPE_CUSTOM =
        "emutils.waypoint.type.custom";
    public static final String WAYPOINT_DEFAULT_DEATH_LABEL =
        "emutils.waypoint.default_death_label";
    public static final String WAYPOINT_DISTANCE = "emutils.waypoint.distance";
    public static final String WAYPOINT_PROMPT = "emutils.waypoint.prompt";
    public static final String WAYPOINT_ACTION_REMOVE =
        "emutils.waypoint.action.remove";
    public static final String WAYPOINT_ACTION_KEEP =
        "emutils.waypoint.action.keep";
    public static final String WAYPOINT_ACTION_COPY_COORDS =
        "emutils.waypoint.action.copy_coords";
    public static final String WAYPOINT_ACTION_HIDE =
        "emutils.waypoint.action.hide";
    public static final String WAYPOINT_ACTION_SHOW =
        "emutils.waypoint.action.show";
    public static final String WAYPOINT_HOVER_REMOVE =
        "emutils.waypoint.hover.remove";
    public static final String WAYPOINT_HOVER_KEEP =
        "emutils.waypoint.hover.keep";
    public static final String WAYPOINT_CLEARED = "emutils.waypoint.cleared";
    public static final String WAYPOINT_KEPT = "emutils.waypoint.kept";
    public static final String WAYPOINT_CLEARED_WORLD =
        "emutils.waypoint.cleared_world";
    public static final String WAYPOINT_NONE_WORLD =
        "emutils.waypoint.none_world";
    public static final String WAYPOINT_COORDS_COPIED =
        "emutils.waypoint.coords_copied";
    public static final String WAYPOINT_COORD_FORMAT_PLAIN =
        "emutils.waypoint.coord_format.plain";
    public static final String WAYPOINT_COORD_FORMAT_COMMA =
        "emutils.waypoint.coord_format.comma";
    public static final String WAYPOINT_COORD_FORMAT_TP_COMMAND =
        "emutils.waypoint.coord_format.tp_command";
    public static final String WAYPOINT_LABEL_PLACEHOLDER =
        "emutils.waypoint.label_placeholder";
    public static final String WAYPOINT_ADDED = "emutils.waypoint.added";

    public static final String RECONNECT_UNAVAILABLE =
        "emutils.reconnect.unavailable";
    public static final String RECONNECT_COUNTDOWN =
        "emutils.reconnect.countdown";
    public static final String RECONNECT_COUNTDOWN_ATTEMPTS =
        "emutils.reconnect.countdown_attempts";
    public static final String RECONNECT_RETRYING =
        "emutils.reconnect.retrying";
    public static final String RECONNECT_EXHAUSTED =
        "emutils.reconnect.exhausted";

    public static final String PACK_SECTION_INSTALLED =
        "emutils.pack.section.installed";
    public static final String PACK_SECTION_SEARCH =
        "emutils.pack.section.search";
    public static final String PACK_SECTION_INSTALLED_EMPTY_PACKS =
        "emutils.pack.section.installed_empty_packs";
    public static final String PACK_SECTION_INSTALLED_EMPTY_SHADERS =
        "emutils.pack.section.installed_empty_shaders";
    public static final String PACK_SECTION_SEARCH_HINT =
        "emutils.pack.section.search_hint";
    public static final String PACK_STATUS_NOT_INSTALLED =
        "emutils.pack.status.not_installed";
    public static final String PACK_TAB_RESOURCE_PACKS =
        "emutils.pack.tab.resource_packs";
    public static final String PACK_TAB_SHADER_PACKS =
        "emutils.pack.tab.shader_packs";
    public static final String PACK_TAB_INSTALLED =
        "emutils.pack.tab.installed";
    public static final String PACK_TAB_MODRINTH = "emutils.pack.tab.modrinth";
    public static final String PACK_SEARCH_PLACEHOLDER =
        "emutils.pack.search.placeholder";
    public static final String PACK_SEARCH = "emutils.pack.action.search";
    public static final String PACK_REFRESH = "emutils.pack.action.refresh";
    public static final String PACK_DOWNLOAD = "emutils.pack.action.download";
    public static final String PACK_DELETE = "emutils.pack.action.delete";
    public static final String PACK_ENABLE = "emutils.pack.action.enable";
    public static final String PACK_DISABLE = "emutils.pack.action.disable";
    public static final String PACK_APPLY = "emutils.pack.action.apply";
    public static final String PACK_TURN_OFF = "emutils.pack.action.turn_off";
    public static final String PACK_SHADER_APPLIED =
        "emutils.pack.shader.applied";
    public static final String PACK_SHADER_APPLY_FAILED =
        "emutils.pack.shader.apply_failed";
    public static final String PACK_SHADER_DISABLED =
        "emutils.pack.shader.disabled";
    public static final String PACK_SHADER_DISABLE_FAILED =
        "emutils.pack.shader.disable_failed";
    public static final String PACK_OPEN_IRIS = "emutils.pack.action.open_iris";
    public static final String PACK_STATUS_INSTALLED =
        "emutils.pack.status.installed";
    public static final String PACK_STATUS_ENABLED =
        "emutils.pack.status.enabled";
    public static final String PACK_STATUS_SELECTED =
        "emutils.pack.status.selected";
    public static final String PACK_STATUS_IDLE = "emutils.pack.status.idle";
    public static final String PACK_STATUS_LOADING =
        "emutils.pack.status.loading";
    public static final String PACK_STATUS_EMPTY_PACKS =
        "emutils.pack.status.empty_packs";
    public static final String PACK_STATUS_EMPTY_SHADERS =
        "emutils.pack.status.empty_shaders";
    public static final String PACK_STATUS_IRIS_REQUIRED =
        "emutils.pack.status.iris_required";
    public static final String PACK_ERROR = "emutils.pack.error";
    public static final String PACK_DELETE_TITLE = "emutils.pack.delete.title";
    public static final String PACK_DELETE_MESSAGE =
        "emutils.pack.delete.message";
    public static final String SCRIPT_MANAGER_REQUIRES_MINESCRIPT =
        "emutils.script_manager.requires_minescript";
    public static final String SCRIPT_MANAGER_EMPTY =
        "emutils.script_manager.empty";
    public static final String SCRIPT_MANAGER_READY =
        "emutils.script_manager.ready";
    public static final String SCRIPT_MANAGER_OPEN_FOLDER =
        "emutils.script_manager.open_folder";
    public static final String SCRIPT_MANAGER_NEW_SCRIPT =
        "emutils.script_manager.new_script";
    public static final String SCRIPT_MANAGER_REFRESH =
        "emutils.script_manager.refresh";
    public static final String SCRIPT_MANAGER_RUN =
        "emutils.script_manager.run";
    public static final String SCRIPT_MANAGER_SAVE =
        "emutils.script_manager.save";
    public static final String SCRIPT_MANAGER_SET_KEYBIND =
        "emutils.script_manager.set_keybind";
    public static final String SCRIPT_MANAGER_CLEAR_KEYBIND =
        "emutils.script_manager.clear_keybind";
    public static final String SCRIPT_MANAGER_DELETE =
        "emutils.script_manager.delete";
    public static final String SCRIPT_MANAGER_DELETE_TITLE =
        "emutils.script_manager.delete_title";
    public static final String SCRIPT_MANAGER_DELETE_MESSAGE =
        "emutils.script_manager.delete_message";
    public static final String SCRIPT_MANAGER_DELETED =
        "emutils.script_manager.deleted";
    public static final String SCRIPT_MANAGER_CREATE =
        "emutils.script_manager.create";
    public static final String SCRIPT_MANAGER_CREATE_HINT =
        "emutils.script_manager.create_hint";
    public static final String SCRIPT_MANAGER_LOADED =
        "emutils.script_manager.loaded";
    public static final String SCRIPT_MANAGER_READ_ONLY =
        "emutils.script_manager.read_only";
    public static final String SCRIPT_MANAGER_SAVED =
        "emutils.script_manager.saved";
    public static final String SCRIPT_MANAGER_RUNNING =
        "emutils.script_manager.running";
    public static final String SCRIPT_MANAGER_STOPPED =
        "emutils.script_manager.stopped";
    public static final String SCRIPT_MANAGER_UNSAFE_COMMAND =
        "emutils.script_manager.unsafe_command";
    public static final String SCRIPT_MANAGER_CAPTURE_KEY =
        "emutils.script_manager.capture_key";
    public static final String SCRIPT_MANAGER_KEYBIND_SET =
        "emutils.script_manager.keybind_set";
    public static final String SCRIPT_MANAGER_KEYBIND_CLEARED =
        "emutils.script_manager.keybind_cleared";
    public static final String SCRIPT_MANAGER_KEYBIND_TITLE =
        "emutils.script_manager.keybind_title";
    public static final String SCRIPT_MANAGER_KEYBIND_HINT =
        "emutils.script_manager.keybind_hint";
    public static final String SCRIPT_MANAGER_KEYBIND_UNSET =
        "emutils.script_manager.keybind_unset";
    public static final String SCRIPT_MANAGER_KEYBIND_PREVIEW =
        "emutils.script_manager.keybind_preview";
    public static final String SCRIPT_MANAGER_DUPLICATE_TITLE =
        "emutils.script_manager.duplicate_title";
    public static final String SCRIPT_MANAGER_DUPLICATE_MESSAGE =
        "emutils.script_manager.duplicate_message";
    public static final String SCRIPT_MANAGER_REPLACE_KEYBIND =
        "emutils.script_manager.replace_keybind";
    public static final String SCRIPT_MANAGER_UNSAVED_TITLE =
        "emutils.script_manager.unsaved_title";
    public static final String SCRIPT_MANAGER_UNSAVED_MESSAGE =
        "emutils.script_manager.unsaved_message";
    public static final String SCRIPT_MANAGER_DISCARD =
        "emutils.script_manager.discard";
    public static final String COMMAND_SHORTCUT_NONE =
        "emutils.command_shortcut.none";
    public static final String COMMAND_SHORTCUT_ACTION_EDIT =
        "emutils.command_shortcut.action.edit";
    public static final String COMMAND_SHORTCUT_ACTION_DELETE =
        "emutils.command_shortcut.action.delete";
    public static final String COMMAND_SHORTCUT_ACTION_SAVE =
        "emutils.command_shortcut.action.save";
    public static final String COMMAND_SHORTCUT_TYPE_COMMAND =
        "emutils.command_shortcut.type.command";
    public static final String COMMAND_SHORTCUT_TYPE_CHAT =
        "emutils.command_shortcut.type.chat";
    public static final String COMMAND_SHORTCUT_FIELD_NAME =
        "emutils.command_shortcut.field.name";
    public static final String COMMAND_SHORTCUT_FIELD_TEXT =
        "emutils.command_shortcut.field.text";
    public static final String COMMAND_SHORTCUT_NAME_PLACEHOLDER =
        "emutils.command_shortcut.name_placeholder";
    public static final String COMMAND_SHORTCUT_TEXT_PLACEHOLDER =
        "emutils.command_shortcut.text_placeholder";
    public static final String COMMAND_SHORTCUT_KEY_UNSET =
        "emutils.command_shortcut.key_unset";
    public static final String COMMAND_SHORTCUT_KEY_VALUE =
        "emutils.command_shortcut.key_value";
    public static final String COMMAND_SHORTCUT_COMMAND_VALUE =
        "emutils.command_shortcut.command_value";
    public static final String COMMAND_SHORTCUT_CAPTURE_KEY =
        "emutils.command_shortcut.capture_key";
    public static final String COMMAND_SHORTCUT_TEXT_REQUIRED =
        "emutils.command_shortcut.text_required";
    public static final String COMMAND_SHORTCUT_KEY_REQUIRED =
        "emutils.command_shortcut.key_required";
    public static final String COMMAND_SHORTCUT_DUPLICATE_KEY =
        "emutils.command_shortcut.duplicate_key";
    public static final String COMMAND_SHORTCUT_CLEAR_TITLE =
        "emutils.command_shortcut.clear_title";
    public static final String COMMAND_SHORTCUT_CLEAR_MESSAGE =
        "emutils.command_shortcut.clear_message";

    private EMUtilsTexts() {}

    public static MutableComponent greenPrefix() {
        return (MutableComponent) EmUtilsChatPrefix.prefix();
    }

    public static Component toggleLabel(String optionKey) {
        return Component.translatable(optionKey);
    }
}
