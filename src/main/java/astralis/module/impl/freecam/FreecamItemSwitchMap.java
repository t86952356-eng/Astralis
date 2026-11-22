package astralis.module.impl.freecam;

public class FreecamItemSwitchMap {
    static final int[] switchMapFreecamItem = new int[FreecamItem.values().length];

    static {
        try {
            switchMapFreecamItem[FreecamItem.TELEPORTER.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapFreecamItem[FreecamItem.SPEED.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapFreecamItem[FreecamItem.PLAYER_CONTROLLER.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
