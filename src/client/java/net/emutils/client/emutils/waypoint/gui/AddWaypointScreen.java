package net.emutils.client.emutils.waypoint.gui;

import net.emutils.client.EMUtilsClient;
import net.emutils.client.emutils.text.EmUtilsChatPrefix;
import net.emutils.client.emutils.util.EMUtilsPaths;
import net.emutils.client.emutils.util.EMUtilsTexts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
    private TextFieldWidget labelField;
    private TextFieldWidget xField;
    private TextFieldWidget yField;
    private TextFieldWidget zField;
    private ButtonWidget colorButton;
    private ButtonWidget beaconButton;
    private int colorIndex;
    private int currentColor;
    private boolean beaconEnabled;

    public AddWaypointScreen(Screen parent) {
        super(Text.translatable(EMUtilsTexts.SCREEN_ADD_WAYPOINT));
        this.parent = parent;
        this.currentColor = EMUtilsClient.config().waypointDefaultCustomColor();
        this.colorIndex = findColorIndex(this.currentColor);
    }

    @Override
    protected void init() {
        DirectionalLayoutWidget layout =
            DirectionalLayoutWidget.vertical().spacing(8);
        layout.getMainPositioner().alignHorizontalCenter();

        layout.add(new TextWidget(title, textRenderer));

        layout.add(new TextWidget(Text.literal("Label"), textRenderer));
        labelField = layout.add(
            new TextFieldWidget(
                textRenderer,
                0,
                0,
                260,
                20,
                Text.literal("Label")
            )
        );
        labelField.setPlaceholder(
            Text.translatable(EMUtilsTexts.WAYPOINT_LABEL_PLACEHOLDER)
        );

        MinecraftClient client = MinecraftClient.getInstance();
        int defaultX = client.player != null ? (int) client.player.getX() : 0;
        int defaultY = client.player != null ? (int) client.player.getY() : 0;
        int defaultZ = client.player != null ? (int) client.player.getZ() : 0;

        DirectionalLayoutWidget coordRow = layout.add(
            DirectionalLayoutWidget.horizontal().spacing(8)
        );
        coordRow.add(new TextWidget(Text.literal("X"), textRenderer));
        xField = coordRow.add(
            new TextFieldWidget(textRenderer, 0, 0, 70, 20, Text.literal("X"))
        );
        xField.setText(String.valueOf(defaultX));
        coordRow.add(new TextWidget(Text.literal("Y"), textRenderer));
        yField = coordRow.add(
            new TextFieldWidget(textRenderer, 0, 0, 70, 20, Text.literal("Y"))
        );
        yField.setText(String.valueOf(defaultY));
        coordRow.add(new TextWidget(Text.literal("Z"), textRenderer));
        zField = coordRow.add(
            new TextFieldWidget(textRenderer, 0, 0, 70, 20, Text.literal("Z"))
        );
        zField.setText(String.valueOf(defaultZ));

        colorButton = layout.add(
            ButtonWidget.builder(colorLabel(), button -> cycleColor())
                .width(260)
                .build()
        );
        beaconButton = layout.add(
            ButtonWidget.builder(beaconLabel(), button -> toggleBeacon())
                .width(260)
                .build()
        );

        DirectionalLayoutWidget buttons = layout.add(
            DirectionalLayoutWidget.horizontal().spacing(8)
        );
        buttons.add(
            ButtonWidget.builder(
                Text.translatable(EMUtilsTexts.OPTION_ADD_WAYPOINT),
                button -> addWaypoint()
            )
                .width(130)
                .build()
        );
        buttons.add(
            ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
                .width(130)
                .build()
        );

        layout.forEachChild(this::addDrawableChild);
        layout.refreshPositions();
        layout.setPosition((width - layout.getWidth()) / 2, height / 6);
        setInitialFocus(labelField);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
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
        if (input.isEnter()) {
            addWaypoint();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        return (
            (labelField != null && labelField.charTyped(input)) ||
            (xField != null && xField.charTyped(input)) ||
            (yField != null && yField.charTyped(input)) ||
            (zField != null && zField.charTyped(input)) ||
            super.charTyped(input)
        );
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private void cycleColor() {
        colorIndex = (colorIndex + 1) % PRESET_COLORS.length;
        currentColor = PRESET_COLORS[colorIndex];
        colorButton.setMessage(colorLabel());
    }

    private Text colorLabel() {
        return Text.literal("Color: ").append(
            Text.literal(COLOR_NAMES[colorIndex]).formatted(Formatting.BOLD)
        );
    }

    private void toggleBeacon() {
        beaconEnabled = !beaconEnabled;
        beaconButton.setMessage(beaconLabel());
    }

    private Text beaconLabel() {
        String key = beaconEnabled
            ? "emutils.waypoint.beacon_on"
            : "emutils.waypoint.beacon_off";
        return Text.translatable(key);
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
        if (client == null || client.player == null || client.world == null) {
            return;
        }

        String label = labelField.getText().trim();
        if (label.isEmpty()) {
            label = "Waypoint";
        }

        int x, y, z;
        try {
            x = Integer.parseInt(xField.getText().trim());
            y = Integer.parseInt(yField.getText().trim());
            z = Integer.parseInt(zField.getText().trim());
        } catch (NumberFormatException e) {
            return;
        }

        EMUtilsClient.waypoint().addCustom(
            client,
            label,
            x,
            y,
            z,
            currentColor,
            beaconEnabled
        );

        if (client.inGameHud != null) {
            client.inGameHud
                .getChatHud()
                .addMessage(
                    EmUtilsChatPrefix.chat(
                        Text.translatable(
                            EMUtilsTexts.WAYPOINT_ADDED,
                            label
                        ).formatted(net.minecraft.util.Formatting.GREEN)
                    )
                );
        }

        client.setScreen(parent);
    }
}
