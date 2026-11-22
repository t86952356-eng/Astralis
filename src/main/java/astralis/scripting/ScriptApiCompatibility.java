package astralis.scripting;

import com.astralis.scripting.Script;

public final class ScriptApiCompatibility {
    public static final String API_MISMATCH_MESSAGE =
            "This script was compiled against a changed client API. Rebuild Astralis and fully restart Minecraft before loading it again.";

    private ScriptApiCompatibility() {}

    public static boolean isApiMismatch(Throwable throwable) {

        Throwable current = throwable;

        for (int depth = 0; current != null; depth++) {

            if (depth >= 16) {
                break;
            }

            if (current instanceof IncompatibleClassChangeError) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private static boolean scriptFieldHasType(String fieldName, String typeName) {
        try {
            return Script.class.getDeclaredField(fieldName).getType().getName().equals(typeName);
        } catch (NoSuchFieldException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isScriptApiCompatible() {

        if (scriptFieldHasType("draggables", "com.astralis.scripting.api.hud.Draggables")) {

            if (scriptFieldHasType("screens", "com.astralis.scripting.api.screen.ScriptScreens")) {
                return true;
            }
        }

        return false;
    }
}
