package astralis.util;

import astralis.metadata.MetadataState;

import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class MathUtil {
    private static Random randomSource;

    public static double cellCenter(int cellIndex, int cellCount) {
        return (cellIndex + 0.5) / cellCount;
    }

    private static float zeroSmallDelta(float angleDelta) {
        if (Math.abs(angleDelta) <= 1.0F) {
            return 0.0F;
        }

        float clampedDelta = Math.clamp(angleDelta, -180.0F, 180.0F);
        float randomDamping = Math.clamp(randomFloat(-0.1F, 0.1F), 0.0F, 1.0F);
        return clampedDelta * (1.0F - randomDamping * 0.5F);
    }

    /** Scales a shared random sample between the supplied bounds, allowing equal bounds. */
    public static double randomDouble(double minimum, double maximum) {
        if (minimum > maximum) {

            throw new IllegalArgumentException("min must be < max");
        } else {
            return minimum + (maximum - minimum) * randomSource.nextDouble();
        }
    }

    /** Returns a random long between the inclusive bounds. */
    public static long randomLong(long minimum, long maximum) {
        if (minimum > maximum) {

            throw new IllegalArgumentException("min must be <= max");
        } else {
            return randomSource.nextLong(minimum, maximum + 1L);
        }
    }

    /** Computes exponential smoothing with the elapsed time capped at a quarter second. */
    public static float smoothingFactor(float responseRate, double deltaSeconds) {
        if (deltaSeconds <= 0.0) {
            return 0.0F;
        }

        double clampedDeltaSeconds = Math.min(deltaSeconds, 0.25);
        return (float) (1.0 - Math.exp(-responseRate * clampedDeltaSeconds));
    }

    public static int randomInt(int minimum, int maximum) {
        if (minimum > maximum) {

            throw new IllegalArgumentException("min must be <= max");
        } else {
            return randomSource.nextInt(maximum - minimum + 1) + minimum;
        }
    }

    public static float randomFloat(float minimum, float maximum) {
        if (minimum > maximum) {

            throw new IllegalArgumentException("min must be < max");
        } else {
            float result = minimum + (maximum - minimum) * randomSource.nextFloat();
            return result;
        }
    }

    public static float roundToStep(float value, float step) {
        if (Float.isFinite(value) && Float.isFinite(step) && !(step <= 0.0F)) {
            BigDecimal decimalStep = new BigDecimal(Float.toString(step));
            return new BigDecimal(Float.toString(value))
                    .divide(decimalStep, 0, RoundingMode.HALF_UP)
                    .multiply(decimalStep)
                    .floatValue();
        } else {
            return value;
        }
    }

    public static float rotationDistance(
            float firstYaw, float firstPitch, float secondYaw, float secondPitch) {
        return Math.abs(Mth.wrapDegrees(firstYaw - secondYaw)) + Math.abs(firstPitch - secondPitch);
    }

    public static float[] getRotationsFrom(
            double deltaX, double deltaY, double deltaZ, float currentYaw, float currentPitch) {
        MetadataState clientState = MetadataState.getInstance();

        double radiansToDegrees = clientState.getDoubleParameter("rad_to_deg", 0.0);
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yawDelta =
                Mth.wrapDegrees(
                        (float) (Math.atan2(deltaZ, deltaX) * radiansToDegrees)
                                - 90.0F
                                - currentYaw);
        float pitchDelta =
                Mth.wrapDegrees(
                        (float) (-Math.atan2(deltaY, horizontalDistance) * radiansToDegrees)
                                - currentPitch);
        return new float[] {
            quantize(currentYaw + zeroSmallDelta(yawDelta)),
            quantize(currentPitch + zeroSmallDelta(pitchDelta))
        };
    }

    private static float quantize(float angle) {
        MetadataState clientState = MetadataState.getInstance();

        float angleStep = clientState.getFloatParameter("quantize_angle");
        return angleStep == 0.0F ? 0.0F : angle - angle % angleStep;
    }

    static {
        randomSource = new Random();
    }
}
