package astralis.util.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class LegacyFormatParser {

    private LegacyFormatParser() {}

    public static Component parse(String text) {
        return parseWithPlaceholder(text, '\u0000', null);
    }

    public static Component parseWithPlaceholder(
            String text, char placeholder, Component component) {
        MutableComponent mutableComponent = Component.empty();
        Style style = Style.EMPTY;
        int segmentStart = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == 57344 && i + 1 < text.length()) {
                appendSegment(mutableComponent, text, segmentStart, i, style);
                int iconStart = i + 1;
                int iconEnd = iconStart + Character.charCount(text.codePointAt(iconStart));
                mutableComponent.append(
                        Component.literal(text.substring(iconStart, iconEnd))
                                .withStyle(style.withFont(new Resource(IconFont.iconFontId))));
                i = iconEnd - 1;
                segmentStart = iconEnd;
            } else if (placeholder != 0 && text.charAt(i) == placeholder) {
                appendSegment(mutableComponent, text, segmentStart, i, style);
                if (component != null) {
                    mutableComponent.append(component);
                }

                segmentStart = i + 1;
            } else if (text.charAt(i) == 167 && i + 1 < text.length()) {
                char formatCode = Character.toLowerCase(text.charAt(i + 1));
                ChatFormatting chatFormatting = ChatFormatting.getByCode(formatCode);
                if (formatCode == 120 && i + 13 < text.length()) {
                    StringBuilder hexDigits = new StringBuilder(6);
                    boolean validHex = true;

                    for (int digit = 0; digit < 6; digit++) {
                        int digitIndex = i + 2 + digit * 2;
                        if (text.charAt(digitIndex) != 167
                                || Character.digit(text.charAt(digitIndex + 1), 16) < 0) {
                            validHex = false;
                            break;
                        }

                        hexDigits.append(text.charAt(digitIndex + 1));
                    }

                    if (validHex) {
                        appendSegment(mutableComponent, text, segmentStart, i, style);
                        style = Style.EMPTY.withColor(Integer.parseInt(hexDigits.toString(), 16));
                        i += 13;
                        segmentStart = i + 1;
                        continue;
                    }
                }

                if (chatFormatting != null) {
                    appendSegment(mutableComponent, text, segmentStart, i, style);
                    style = applyFormatting(style, chatFormatting);
                    segmentStart = ++i + 1;
                }
            }
        }

        appendSegment(mutableComponent, text, segmentStart, text.length(), style);
        return mutableComponent;
    }

    private static void appendSegment(
            MutableComponent mutableComponent, String text, int start, int end, Style style) {
        if (start < end) {
            mutableComponent.append(Component.literal(text.substring(start, end)).withStyle(style));
        }
    }

    private static Style applyFormatting(Style style, ChatFormatting chatFormatting) {

        return switch (ChatFormattingSwitchMap.switchTable[chatFormatting.ordinal()]) {
            case 1 -> Style.EMPTY.withColor(0);
            case 2 -> Style.EMPTY.withColor(170);
            case 3 -> Style.EMPTY.withColor(43520);
            case 4 -> Style.EMPTY.withColor(43690);
            case 5 -> Style.EMPTY.withColor(11141120);
            case 6 -> Style.EMPTY.withColor(11141290);
            case 7 -> Style.EMPTY.withColor(16755200);
            case 8 -> Style.EMPTY.withColor(11184810);
            case 9 -> Style.EMPTY.withColor(5592405);
            case 10 -> Style.EMPTY.withColor(5592575);
            case 11 -> Style.EMPTY.withColor(5635925);
            case 12 -> Style.EMPTY.withColor(5636095);
            case 13 -> Style.EMPTY.withColor(16733525);
            case 14 -> Style.EMPTY.withColor(16733695);
            case 15 -> Style.EMPTY.withColor(16777045);
            case 16 -> Style.EMPTY.withColor(16777215);
            case 17 -> style.withBold(true);
            case 18 -> style.withItalic(true);
            case 19 -> style.withUnderlined(true);
            case 20 -> style.withStrikethrough(true);
            case 21 -> style.withObfuscated(true);
            case 22 -> Style.EMPTY;
            default -> style;
        };
    }
}
