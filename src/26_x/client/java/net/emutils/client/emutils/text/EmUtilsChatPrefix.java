package net.emutils.client.emutils.text;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

/**
 * Builds a gradient-color {@code [EMUtils]} chat prefix component.
 *
 * <p>The gradient goes from bright green at the start, through {@code &a} green
 * in the middle, to dark green at the end.  This is a two-segment linear
 * interpolation.
 */
public final class EmUtilsChatPrefix {

    // ── Color endpoints ──────────────────────────────────────────────
    // &a green = #55FF55
    private static final int MID_R = 85;
    private static final int MID_G = 255;
    private static final int MID_B = 85;

    // bright green (lighter than &a)
    private static final int START_R = 170;
    private static final int START_G = 255;
    private static final int START_B = 170;

    // dark green (darker than &a)
    private static final int END_R = 0;
    private static final int END_G = 170;
    private static final int END_B = 0;

    private static final String PREFIX_TEXT = "[EMUtils] ";

    private EmUtilsChatPrefix() {}

    /** Returns the gradient prefix as a reusable {@link Component}. */
    public static Component prefix() {
        MutableComponent result = Component.empty();
        // The bracket-only part ("[EMUtils]") for gradient
        String gradPart = "[EMUtils]";
        int len = gradPart.length();
        int mid = len / 2;

        for (int i = 0; i < len; i++) {
            float t;
            int r, g, b;

            if (i <= mid) {
                // bright → &a (0 .. 1)
                t = (float) i / mid;
                r = lerp(START_R, MID_R, t);
                g = lerp(START_G, MID_G, t);
                b = lerp(START_B, MID_B, t);
            } else {
                // &a → dark (0 .. 1)
                t = (float) (i - mid) / (len - 1 - mid);
                r = lerp(MID_R, END_R, t);
                g = lerp(MID_G, END_G, t);
                b = lerp(MID_B, END_B, t);
            }

            result.append(
                Component.literal(String.valueOf(gradPart.charAt(i))).setStyle(
                    Style.EMPTY.withColor(
                        TextColor.fromRgb((r << 16) | (g << 8) | b)
                    )
                )
            );
        }

        // Trailing space in gray
        result.append(Component.literal(" ").withStyle(ChatFormatting.GRAY));
        return result;
    }

    /** Wraps a message string with the gradient prefix. */
    public static Component chat(String message) {
        return prefix()
            .copy()
            .append(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    /** Wraps a pre-built {@link Component} message with the gradient prefix. */
    public static Component chat(Component message) {
        return prefix().copy().append(message);
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
