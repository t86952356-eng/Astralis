package astralis.module.impl.beddefender;

import net.minecraft.core.Direction;

public class BedDefenderDirectionSwitchMap {
    public static int[] directionOrdinalMap;

    static {
        directionOrdinalMap = new int[Direction.values().length];

        try {
            directionOrdinalMap[Direction.UP.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            directionOrdinalMap[Direction.DOWN.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            directionOrdinalMap[Direction.SOUTH.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            directionOrdinalMap[Direction.NORTH.ordinal()] = 4;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            directionOrdinalMap[Direction.EAST.ordinal()] = 5;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            directionOrdinalMap[Direction.WEST.ordinal()] = 6;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
