package astralis.module.impl.esp;

import net.minecraft.core.Direction;

public class EspDirectionSwitchMap {
    public static final int[] DIRECTION_ORDINALS = new int[Direction.values().length];

    static {
        try {
            DIRECTION_ORDINALS[Direction.SOUTH.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            DIRECTION_ORDINALS[Direction.WEST.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            DIRECTION_ORDINALS[Direction.NORTH.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            DIRECTION_ORDINALS[Direction.EAST.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
