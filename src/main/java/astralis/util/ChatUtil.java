package astralis.util;

import astralis.Astralis;
import astralis.util.render.ColorUtil;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ChatUtil implements InstanceAccess {

    /** Sends a command through the local player's connection; the text excludes the leading slash. */
    public static void sendCommand(String command) {
        mc.player.connection.sendCommand(command);
    }

    public static void print(Object message) {
        if (mc.player != null) {
            mc.execute(
                    () -> {
                        MutableComponent prefix =
                                createGradientPrefix(
                                        Astralis.displayName,
                                        Astralis.getInstance().getPrimaryColor().getRGB(),
                                        Astralis.getInstance().getSecondaryColor().getRGB());
                        String messageText = String.valueOf(message);

                        MutableComponent formattedMessage = prefix.append(" » " + messageText);
                        mc.player.sendSystemMessage(formattedMessage);
                    });
        }
    }

    public static void print(Component message) {
        if (mc.player != null) {
            mc.execute(
                    () -> {
                        MutableComponent prefix =
                                createGradientPrefix(
                                        Astralis.displayName,
                                        Astralis.getInstance().getPrimaryColor().getRGB(),
                                        Astralis.getInstance().getSecondaryColor().getRGB());

                        MutableComponent formattedMessage = prefix.append(" » ").append(message);
                        mc.player.sendSystemMessage(formattedMessage);
                    });
        }
    }

    /** Builds the bracketed client prefix with a color interpolated for each character. */
    private static MutableComponent createGradientPrefix(
            String text, int startColor, int endColor) {
        MutableComponent prefix = Component.empty();
        prefix.append(ChatFormatting.DARK_GRAY + "[");

        for (int characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            float reverseProgress =
                    (float) (text.length() - 1 - characterIndex) / (text.length() - 1);
            Style characterStyle =
                    Style.EMPTY.withColor(ColorUtil.lerpRgb(endColor, startColor, reverseProgress));
            prefix.append(
                    Component.literal(String.valueOf(text.charAt(characterIndex)))
                            .setStyle(characterStyle));
        }

        prefix.append(ChatFormatting.DARK_GRAY + "]");
        return prefix;
    }
}
