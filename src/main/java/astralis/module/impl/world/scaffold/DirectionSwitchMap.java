package astralis.module.impl.world.scaffold;

import net.minecraft.core.Direction;

public class DirectionSwitchMap {
    public static int[] switchMapDirection;

    static {
        switchMapDirection = new int[Direction.values().length];

        try {
            switchMapDirection[Direction.DOWN.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapDirection[Direction.UP.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapDirection[Direction.NORTH.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapDirection[Direction.SOUTH.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapDirection[Direction.WEST.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapDirection[Direction.EAST.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
