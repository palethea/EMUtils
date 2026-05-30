package net.emutils.client.emhelpers.util;

import net.emutils.client.emhelpers.text.EmUtilsChatPrefix;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EMUtilsTexts {

    public static final String NAME = "emutils.name";
    public static final String OPTIONS_BUTTON = "emutils.options.button";
    public static final String OPTIONS_SKYBLOCK_BUTTON =
        "emutils.options.skyblock_button";

    public static final String HUB_MODERN_TITLE = "emutils.hub.modern_title";
    public static final String HUB_MODERN_OPEN = "emutils.hub.modern_open";
    public static final String HUB_CLASSIC_OPEN = "emutils.hub.classic_open";
    public static final String HUB_EMSKYBLOCK_OPEN =
        "emutils.hub.emskyblock_open";
    public static final String HUB_OPEN_PACK_MANAGER =
        "emutils.hub.open_pack_manager";
    public static final String HUB_OPEN_SCRIPT_MANAGER =
        "emutils.hub.open_script_manager";

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
    public static final String HUB_HUD_OVERLAY = "emutils.hub.hud_overlay";
    public static final String HUB_ZOOM = "emutils.hub.zoom";
    public static final String HUB_TWEAKS = "emutils.hub.tweaks";
    public static final String HUB_PACK_MANAGER = "emutils.hub.pack_manager";
    public static final String HUB_CAPES = "emutils.hub.capes";
    public static final String HUB_SPOTIFY_PLAYER =
        "emutils.hub.spotify_player";
    public static final String HUB_INVENTORY_TOOLS =
        "emutils.hub.inventory_tools";
    public static final String HUB_SKYBLOCK = "emutils.hub.skyblock";
    public static final String HUB_EMSKYBLOCK = "emutils.hub.emskyblock";
    public static final String HUB_SCRIPT_MANAGER =
        "emutils.hub.script_manager";

    public static final String SCREEN_DEATH_WAYPOINTS =
        "emutils.screen.death_waypoints";
    public static final String SCREEN_AUTO_RECONNECT =
        "emutils.screen.auto_reconnect";
    public static final String SCREEN_SCREENSHOT_HELPER =
        "emutils.screen.screenshot_helper";
    public static final String SCREEN_COPY_CHAT = "emutils.screen.copy_chat";
    public static final String SCREEN_CHAT_FEATURES =
        "emutils.screen.chat_features";
    public static final String SCREEN_CURRENT_WAYPOINTS =
        "emutils.screen.current_waypoints";
    public static final String SCREEN_SCREENSHOT_GALLERY =
        "emutils.screen.screenshot_gallery";
    public static final String SCREEN_HUD_OVERLAY =
        "emutils.screen.hud_overlay";
    public static final String SCREEN_ZOOM = "emutils.screen.zoom";
    public static final String SCREEN_TWEAKS = "emutils.screen.tweaks";
    public static final String SCREEN_PACK_MANAGER =
        "emutils.screen.pack_manager";
    public static final String SCREEN_CAPES = "emutils.screen.capes";
    public static final String SCREEN_SPOTIFY_PLAYER =
        "emutils.screen.spotify_player";
    public static final String SCREEN_INVENTORY_TOOLS =
        "emutils.screen.inventory_tools";
    public static final String SCREEN_SKYBLOCK = "emutils.screen.skyblock";
    public static final String SCREEN_EMSKYBLOCK = "emutils.screen.emskyblock";
    public static final String SCREEN_SETTINGS_CHOOSER =
        "emutils.screen.settings_chooser";
    public static final String SETTINGS_CHOOSER_EMUTILS =
        "emutils.settings_chooser.emutils";
    public static final String SETTINGS_CHOOSER_EMSKYBLOCK =
        "emutils.settings_chooser.emskyblock";
    public static final String SCREEN_SCRIPT_MANAGER =
        "emutils.screen.script_manager";

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
    public static final String HUD_ELEMENT_SKYBLOCK_STATS =
        "emutils.hud.element.skyblock_stats";
    public static final String HUD_ELEMENT_ESTIMATED_ITEM_VALUE =
        "emutils.hud.element.estimated_item_value";
    public static final String HUD_ELEMENT_FISHING_HOOK =
        "emutils.hud.element.fishing_hook";
    public static final String HUD_ELEMENT_SEA_CREATURE_TRACKER =
        "emutils.hud.element.sea_creature_tracker";
    public static final String HUD_ELEMENT_FISHING_PROFIT_TRACKER =
        "emutils.hud.element.fishing_profit_tracker";
    public static final String OPTION_SKYBLOCK_HIDE_ACTION_BAR =
        "emutils.option.skyblock_hide_action_bar";
    public static final String OPTION_SKYBLOCK_HIDE_INVENTORY_STATUS_EFFECTS =
        "emutils.option.skyblock_hide_inventory_status_effects";
    public static final String OPTION_HUD_COORDINATES =
        "emutils.option.hud_coordinates";
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
    public static final String OPTION_TWEAK_NO_FOG =
        "emutils.option.tweak_no_fog";
    public static final String OPTION_TWEAK_CLEAR_UNDERWATER =
        "emutils.option.tweak_clear_underwater";
    public static final String OPTION_TWEAK_CLEAR_LAVA =
        "emutils.option.tweak_clear_lava";
    public static final String OPTION_TWEAK_NO_ENVIRONMENT_FOG =
        "emutils.option.tweak_no_environment_fog";
    public static final String OPTION_TWEAK_NO_HURT_CAM =
        "emutils.option.tweak_no_hurt_cam";
    public static final String OPTION_TWEAK_FREELOOK =
        "emutils.option.tweak_freelook";
    public static final String OPTION_TWEAK_SHULKER_TOOLTIP_PREVIEW =
        "emutils.option.tweak_shulker_tooltip_preview";
    public static final String OPTION_TWEAK_BUNDLE_TOOLTIP_PREVIEW =
        "emutils.option.tweak_bundle_tooltip_preview";
    public static final String OPTION_TWEAK_CLEAR_WEATHER =
        "emutils.option.tweak_clear_weather";
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
    public static final String OPTION_SKYBLOCK = "emutils.option.skyblock";
    public static final String OPTION_STORAGE_PREVIEW =
        "emutils.option.storage_preview";
    public static final String OPTION_BAZAAR_TOOLTIPS =
        "emutils.option.bazaar_tooltips";
    public static final String OPTION_AUCTION_TOOLTIPS =
        "emutils.option.auction_tooltips";
    public static final String OPTION_NPC_SELL_PRICE_TOOLTIPS =
        "emutils.option.npc_sell_price_tooltips";
    public static final String OPTION_SKYBLOCK_STATS_HUD =
        "emutils.option.skyblock_stats_hud";
    public static final String OPTION_SKYBLOCK_STATS_HIDE_ACTION_BAR =
        "emutils.option.skyblock_stats_hide_action_bar";
    public static final String OPTION_SKYBLOCK_STATS_HEALTH =
        "emutils.option.skyblock_stats_health";
    public static final String OPTION_SKYBLOCK_STATS_DEFENSE =
        "emutils.option.skyblock_stats_defense";
    public static final String OPTION_SKYBLOCK_STATS_MANA =
        "emutils.option.skyblock_stats_mana";
    public static final String OPTION_SKYBLOCK_STATS_SOULFLOW =
        "emutils.option.skyblock_stats_soulflow";
    public static final String OPTION_SKYBLOCK_STATS_POSITION =
        "emutils.option.skyblock_stats_position";
    public static final String OPTION_SKYBLOCK_STATS_BACKGROUND_OPACITY =
        "emutils.option.skyblock_stats_background_opacity";
    public static final String OPTION_SKYBLOCK_STATS_SCALE =
        "emutils.option.skyblock_stats_scale";
    public static final String OPTION_ESTIMATED_ITEM_VALUE_HUD =
        "emutils.option.estimated_item_value_hud";
    public static final String OPTION_ESTIMATED_ITEM_VALUE_POSITION =
        "emutils.option.estimated_item_value_position";
    public static final String OPTION_ESTIMATED_ITEM_VALUE_SCALE =
        "emutils.option.estimated_item_value_scale";
    public static final String OPTION_ESTIMATED_ITEM_VALUE_ENCHANTMENTS_CAP =
        "emutils.option.estimated_item_value_enchantments_cap";
    public static final String OPTION_ESTIMATED_ITEM_VALUE_EXACT_TOTAL =
        "emutils.option.estimated_item_value_exact_total";
    public static final String OPTION_SKYBLOCK_HIDE_VANILLA_STATUS =
        "emutils.option.skyblock_hide_vanilla_status";
    public static final String BAZAAR_BUY_ORDER = "emutils.bazaar.buy_order";
    public static final String BAZAAR_SELL_ORDER = "emutils.bazaar.sell_order";
    public static final String BAZAAR_INSTANT_BUY =
        "emutils.bazaar.instant_buy";
    public static final String BAZAAR_INSTANT_SELL =
        "emutils.bazaar.instant_sell";
    public static final String BAZAAR_AVERAGE_24H =
        "emutils.bazaar.average_24h";
    public static final String AUCTION_LOWEST_BIN =
        "emutils.auction.lowest_bin";
    public static final String AUCTION_AVERAGE_24H =
        "emutils.auction.average_24h";
    public static final String ESTIMATED_ITEM_VALUE_TOTAL = "emutils.eiv.total";
    public static final String NPC_SELL_PRICE = "emutils.npc.sell_price";
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
    public static final String OPTION_INVENTORY_PREVIEW =
        "emutils.option.inventory_preview";
    public static final String OPTION_PRESERVE_CONTAINER_CURSOR =
        "emutils.option.preserve_container_cursor";
    public static final String OPTION_INVENTORY_PREVIEW_OPACITY =
        "emutils.option.inventory_preview_opacity";
    public static final String INVENTORY_LOCK_COLOR_RED =
        "emutils.inventory.lock_color.red";
    public static final String INVENTORY_LOCK_COLOR_YELLOW =
        "emutils.inventory.lock_color.yellow";
    public static final String INVENTORY_LOCK_COLOR_GREEN =
        "emutils.inventory.lock_color.green";
    public static final String INVENTORY_LOCK_COLOR_BLUE =
        "emutils.inventory.lock_color.blue";
    public static final String INVENTORY_BOUND_COLOR_GRAY =
        "emutils.inventory.bound_color.gray";
    public static final String INVENTORY_BOUND_COLOR_WHITE =
        "emutils.inventory.bound_color.white";
    public static final String INVENTORY_BOUND_COLOR_DARK_BLUE =
        "emutils.inventory.bound_color.dark_blue";
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

    private EMUtilsTexts() {}

    public static MutableText greenPrefix() {
        return (MutableText) EmUtilsChatPrefix.prefix();
    }

    public static Text toggleLabel(String optionKey) {
        return Text.translatable(optionKey);
    }
}
