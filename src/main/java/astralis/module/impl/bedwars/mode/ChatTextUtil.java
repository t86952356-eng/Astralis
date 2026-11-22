package astralis.module.impl.bedwars.mode;

public final class ChatTextUtil {
    ChatTextUtil() {}

    static String normalizeText(String text) {
        return stripFormatting(text).trim().replaceAll("\\s+", " ");
    }

    static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            char character;
            label43:
            {
                character = text.charAt(i);

                if (character != 167) {

                    if (character != '&') {
                        break label43;
                    }
                }

                if (i + 1 < text.length()) {
                    i++;
                    continue;
                }
            }

            if (character == 160) {

                builder.append(' ');
            } else {
                int characterType = Character.getType(character);

                if (characterType != 16) {
                    int recheckedType = Character.getType(character);

                    if (recheckedType != 15) {
                        builder.append(character);
                    }
                }
            }
        }

        return builder.toString();
    }
}
