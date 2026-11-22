package astralis.ui.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class TextInputUtil {
    private static final Map<Object, Boolean> selectAllOwners = new WeakHashMap<>();

    private TextInputUtil() {}

    public static void insertText(
            Object owner,
            String currentText,
            String insertedText,
            Consumer<String> textSetter,
            int maxLength,
            IntPredicate codePointFilter) {
        boolean wasSelectAll;
        synchronized (selectAllOwners) {
            wasSelectAll = selectAllOwners.remove(owner) != null;
        }

        String baseText = wasSelectAll ? "" : currentText;
        int remainingCapacity =
                Math.max(0, maxLength - baseText.codePointCount(0, baseText.length()));
        StringBuilder builder = new StringBuilder(baseText);
        insertedText
                .codePoints()
                .filter(codePointFilter)
                .limit(remainingCapacity)
                .forEach(builder::appendCodePoint);
        textSetter.accept(builder.toString());
    }

    public static boolean isPrintableCodePoint(int codePoint) {
        return Character.isValidCodePoint(codePoint) && !Character.isISOControl(codePoint);
    }

    public static void clearSelectAll(Object owner) {
        synchronized (selectAllOwners) {
            selectAllOwners.remove(owner);
        }
    }

    public static boolean handleControlShortcut(
            Object owner,
            KeyEvent keyEvent,
            String text,
            Consumer<String> textSetter,
            int maxLength,
            IntPredicate codePointFilter) {
        if (!keyEvent.hasControlDown()) {
            return false;
        }

        switch (keyEvent.key()) {
            case 65:
                synchronized (selectAllOwners) {
                    selectAllOwners.put(owner, true);
                    return true;
                }
            case 67:
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
                return true;
            case 86:
                String clipboardText = Minecraft.getInstance().keyboardHandler.getClipboard();
                insertText(owner, text, clipboardText, textSetter, maxLength, codePointFilter);
                return true;
            case 88:
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
                textSetter.accept("");
                clearSelectAll(owner);
                return true;
            default:
                return false;
        }
    }

    public static void handleBackspace(Object owner, String text, Consumer<String> textSetter) {
        boolean wasSelectAll;
        synchronized (selectAllOwners) {
            wasSelectAll = selectAllOwners.remove(owner) != null;
        }

        if (wasSelectAll) {
            textSetter.accept("");
        } else if (!text.isEmpty()) {
            textSetter.accept(text.substring(0, text.offsetByCodePoints(text.length(), -1)));
        }
    }
}
