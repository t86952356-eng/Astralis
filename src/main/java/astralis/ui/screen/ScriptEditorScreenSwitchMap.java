package astralis.ui.screen;

import astralis.scripting.ReloadResult;
import astralis.ui.component.ScriptsToolbarAction;

public class ScriptEditorScreenSwitchMap {
    static int[] toolbarActionOrdinals;
    static int[] reloadResultOrdinals;

    static {
        reloadResultOrdinals = new int[ReloadResult.values().length];

        try {
            reloadResultOrdinals[ReloadResult.COMPLETED.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            reloadResultOrdinals[ReloadResult.DEFERRED.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            reloadResultOrdinals[ReloadResult.PARTIAL.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            reloadResultOrdinals[ReloadResult.PREFLIGHT_FAILED.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            reloadResultOrdinals[ReloadResult.SCAN_FAILED.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        toolbarActionOrdinals = new int[ScriptsToolbarAction.values().length];

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.NEW_EASY.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.LIST.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.OPEN_FOLDER.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.RELOAD.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.BACK.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            toolbarActionOrdinals[ScriptsToolbarAction.NONE.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
