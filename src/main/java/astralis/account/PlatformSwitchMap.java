package astralis.account;

import astralis.util.OperatingSystem;

public class PlatformSwitchMap {
    static final int[] switchMapPlatform = new int[OperatingSystem.values().length];

    static {
        try {
            switchMapPlatform[OperatingSystem.WIN.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapPlatform[OperatingSystem.LNX.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapPlatform[OperatingSystem.MAC.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
