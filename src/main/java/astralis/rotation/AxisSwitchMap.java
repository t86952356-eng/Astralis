package astralis.rotation;

import net.minecraft.core.Direction.Axis;

public class AxisSwitchMap {
    public static final int[] switchMapAxis = new int[Axis.values().length];

    static {
        try {
            switchMapAxis[Axis.X.ordinal()] = 1;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapAxis[Axis.Y.ordinal()] = 2;
        } catch (NoSuchFieldError ignored) {
        }

        try {
            switchMapAxis[Axis.Z.ordinal()] = 3;
        } catch (NoSuchFieldError ignored) {
        }
    }
}
