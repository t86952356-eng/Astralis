package astralis.command.impl;

import astralis.scripting.TargetedLoadResult;

public class TargetedLoadResultSwitchMap {
    static final int[] ORDINAL_TO_CASE = new int[TargetedLoadResult.values().length];

    static {
        try {
            ORDINAL_TO_CASE[TargetedLoadResult.QUEUED.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            ORDINAL_TO_CASE[TargetedLoadResult.BUSY.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            ORDINAL_TO_CASE[TargetedLoadResult.UNAVAILABLE.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
