package astralis.util.chat;

import net.minecraft.ChatFormatting;

public class ChatFormattingSwitchMap {
    static int[] switchTable = new int[ChatFormatting.values().length];

    static {
        try {
            switchTable[ChatFormatting.BLACK.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_BLUE.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_GREEN.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_AQUA.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_RED.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_PURPLE.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.GOLD.ordinal()] = 7;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.GRAY.ordinal()] = 8;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.DARK_GRAY.ordinal()] = 9;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.BLUE.ordinal()] = 10;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.GREEN.ordinal()] = 11;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.AQUA.ordinal()] = 12;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.RED.ordinal()] = 13;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.LIGHT_PURPLE.ordinal()] = 14;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.YELLOW.ordinal()] = 15;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.WHITE.ordinal()] = 16;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.BOLD.ordinal()] = 17;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.ITALIC.ordinal()] = 18;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.UNDERLINE.ordinal()] = 19;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.STRIKETHROUGH.ordinal()] = 20;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.OBFUSCATED.ordinal()] = 21;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchTable[ChatFormatting.RESET.ordinal()] = 22;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
