package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class AddWaypointScreen extends Screen {

    private static final int[] PRESET_COLORS = {
        0xFFFF5555, // red
        0xFF55FF55, // green
        0xFF5555FF, // blue
        0xFFFFFF55, // yellow
        0xFF55FFFF, // cyan
        0xFFFF55FF, // purple
        0xFFFFAA55, // orange
        0xFFFFFFFF, // white
        0xFF555555, // gray
    };
    private static final String[] COLOR_NAMES = {
        "Red",
        "Green",
        "Blue",
        "Yellow",
        "Cyan",
        "Purple",
        "Orange",
        "White",
        "Gray",
    };

    private final Screen parent;
    private EditBox labelField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Button colorButton;
    private Button beaconButton;
    private int colorIndex;
    private int currentColor;
    private boolean beaconEnabled;

    public AddWaypointScreen(Screen parent) {
        super(Component.translatable(EMUtilsTexts.SCREEN_ADD_WAYPOINT));
        this.parent = parent;
        this.currentColor = EMUtilsClient.config().waypointDefaultCustomColor();
        this.colorIndex = findColorIndex(this.currentColor);
    }

    @Override
    protected void init() {
        LinearLayout layout =
            LinearLayout.vertical().spacing(8);
        layout.defaultCellSetting().alignHorizontallyCenter();

        layout.addChild(new StringWidget(title, font));

        layout.addChild(new StringWidget(Component.literal("Label"), font));
        labelField = layout.addChild(
            new EditBox(
                font,
                0,
                0,
                260,
                20,
                Component.literal("Label")
            )
        );
        labelField.setHint(
            Component.translatable(EMUtilsTexts.WAYPOINT_LABEL_PLACEHOLDER)
        );

        Minecraft currentClient = Minecraft.getInstance();
        int defaultX = currentClient.player != null ? (int) currentClient.player.getX() : 0;
        int defaultY = currentClient.player != null ? (int) currentClient.player.getY() : 0;
        int defaultZ = currentClient.player != null ? (int) currentClient.player.getZ() : 0;

        LinearLayout coordRow = layout.addChild(
            LinearLayout.horizontal().spacing(8)
        );
        coordRow.addChild(new StringWidget(Component.literal("X"), font));
        xField = coordRow.addChild(
            new EditBox(font, 0, 0, 70, 20, Component.literal("X"))
        );
        xField.setValue(String.valueOf(defaultX));
        coordRow.addChild(new StringWidget(Component.literal("Y"), font));
        yField = coordRow.addChild(
            new EditBox(font, 0, 0, 70, 20, Component.literal("Y"))
        );
        yField.setValue(String.valueOf(defaultY));
        coordRow.addChild(new StringWidget(Component.literal("Z"), font));
        zField = coordRow.addChild(
            new EditBox(font, 0, 0, 70, 20, Component.literal("Z"))
        );
        zField.setValue(String.valueOf(defaultZ));

        colorButton = layout.addChild(
            Button.builder(colorLabel(), button -> cycleColor())
                .width(260)
                .build()
        );
        beaconButton = layout.addChild(
            Button.builder(beaconLabel(), button -> toggleBeacon())
                .width(260)
                .build()
        );

        LinearLayout buttons = layout.addChild(
            LinearLayout.horizontal().spacing(8)
        );
        buttons.addChild(
            Button.builder(
                Component.translatable(EMUtilsTexts.OPTION_ADD_WAYPOINT),
                button -> addWaypoint()
            )
                .width(130)
                .build()
        );
        buttons.addChild(
            Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .width(130)
                .build()
        );

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
        layout.setX((width - layout.getWidth()) / 2);
        layout.setY(height / 6);
        setInitialFocus(labelField);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent input) {
        if (labelField != null && labelField.keyPressed(input)) {
            return true;
        }
        if (xField != null && xField.keyPressed(input)) {
            return true;
        }
        if (yField != null && yField.keyPressed(input)) {
            return true;
        }
        if (zField != null && zField.keyPressed(input)) {
            return true;
        }
        if (input.isConfirmation()) {
            addWaypoint();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        return (
            (labelField != null && labelField.charTyped(input)) ||
            (xField != null && xField.charTyped(input)) ||
            (yField != null && yField.charTyped(input)) ||
            (zField != null && zField.charTyped(input)) ||
            super.charTyped(input)
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    private void cycleColor() {
        colorIndex = (colorIndex + 1) % PRESET_COLORS.length;
        currentColor = PRESET_COLORS[colorIndex];
        colorButton.setMessage(colorLabel());
    }

    private Component colorLabel() {
        return Component.literal("Color: ").append(
            Component.literal(COLOR_NAMES[colorIndex]).withStyle(ChatFormatting.BOLD)
        );
    }

    private void toggleBeacon() {
        beaconEnabled = !beaconEnabled;
        beaconButton.setMessage(beaconLabel());
    }

    private Component beaconLabel() {
        String key = beaconEnabled
            ? "emutils.waypoint.beacon_on"
            : "emutils.waypoint.beacon_off";
        return Component.translatable(key);
    }

    private static int findColorIndex(int color) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == color) {
                return i;
            }
        }
        return 0;
    }

    private void addWaypoint() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return;
        }

        String label = labelField.getValue().trim();
        if (label.isEmpty()) {
            label = "Waypoint";
        }

        int x, y, z;
        try {
            x = Integer.parseInt(xField.getValue().trim());
            y = Integer.parseInt(yField.getValue().trim());
            z = Integer.parseInt(zField.getValue().trim());
        } catch (NumberFormatException e) {
            return;
        }

        EMUtilsClient.waypoint().addCustom(
            minecraft,
            label,
            x,
            y,
            z,
            currentColor,
            beaconEnabled
        );

        if (minecraft.gui != null) {
            net.emutils.client.emutils.compat.MinecraftClientCompat.chat(minecraft)
                .addClientSystemMessage(
                    EmUtilsChatPrefix.chat(
                        Component.translatable(
                            EMUtilsTexts.WAYPOINT_ADDED,
                            label
                        ).withStyle(net.minecraft.ChatFormatting.GREEN)
                    )
                );
        }

        if (parent instanceof WaypointListScreen waypointList) {
            waypointList.refreshList();
        }
        minecraft.setScreenAndShow(parent);
    }
}
