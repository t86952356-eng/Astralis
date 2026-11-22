package astralis.util.render;

import lombok.Generated;

public enum ChatColorCode {
    BLACK('0', 0),
    DARK_BLUE('1', 170),
    DARK_GREEN('2', 43520),
    DARK_AQUA('3', 43690),
    DARK_RED('4', 11141120),
    DARK_PURPLE('5', 11141290),
    GOLD('6', 16755200),
    GRAY('7', 11184810),
    DARK_GRAY('8', 5592405),
    BLUE('9', 5592575),
    GREEN('a', 5635925),
    AQUA('b', 5636095),
    RED('c', 16733525),
    LIGHT_PURPLE('d', 16733695),
    YELLOW('e', 16777045),
    WHITE('f', 16777215),
    RESET('r', -1);

    private final char code;
    private final int rgb;

    @Override
    public String toString() {
        return "§" + this.code;
    }

    public static ChatColorCode fromCode(char colorCode) {
        ChatColorCode[] colors = values();
        int length = colors.length;
        int index = 0;

        while (index < length) {
            ChatColorCode color = colors[index];
            if (color.code == colorCode) {
                return color;
            }

            index++;
        }

        return null;
    }

    @Generated
    public char getCode() {
        return this.code;
    }

    @Generated
    public int getRgb() {
        return this.rgb;
    }

    @Generated
    ChatColorCode(char code, int rgb) {
        this.code = code;
        this.rgb = rgb;
    }

    static {
    }
}
