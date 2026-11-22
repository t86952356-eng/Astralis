package astralis.util.render;

import astralis.Astralis;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

public class ColorUtil {

    /** Animates a mirrored phase between two colors, optionally blending in HSB space. */
    public static Color pingPongBlend(
            int periodMillis,
            int phaseOffset,
            Color firstColor,
            Color secondColor,
            boolean blendInHsb) {

        int phaseDegrees = (int) ((System.currentTimeMillis() / periodMillis + phaseOffset) % 360L);

        int mirroredPhase;
        if (phaseDegrees >= 180) {

            mirroredPhase = 360 - phaseDegrees;
        } else {
            mirroredPhase = phaseDegrees;
        }

        phaseDegrees = mirroredPhase * 2;
        return blendInHsb
                ? blendHsb(firstColor, secondColor, phaseDegrees / 360.0F)
                : blendRgba(firstColor, secondColor, phaseDegrees / 360.0F);
    }

    /** Blends two colors using a time-varying phase derived from UV coordinates. */
    public static Color uvWaveBlend(UVPair uv, Color firstColor, Color secondColor) {
        return weightedRgbBlend(firstColor, secondColor, uvWaveWeight(uv));
    }

    /** Interpolates an integer channel and truncates the result. */
    public static int lerpInt(int start, int end, double progress) {
        return lerp(start, end, (float) progress).intValue();
    }

    /** Creates a color from normalized RGBA channels. */
    public static Color fromRgbaFloats(float[] rgba) {
        return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    public static Color withAlphaFactor(Color color, float alphaFactor) {
        alphaFactor = Math.min(1.0F, Math.max(0.0F, alphaFactor));
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                (int) (color.getAlpha() * alphaFactor));
    }

    /** Computes a smooth animation weight from the current time and UV coordinates. */
    public static double uvWaveWeight(UVPair uv) {
        return Math.sin(System.currentTimeMillis() / 175.0 + uv.u() * 7.0E-4 + uv.v() * 7.0E-4)
                        * 0.5
                + 0.5;
    }

    /** Unpacks ARGB into normalized red, green, blue, and alpha channels. */
    public static float[] toRgbaFloats(int argb) {

        int shiftedRed = argb >> 16;

        float red = (shiftedRed & 0xFF) / 255.0F;

        int shiftedGreen = argb >> 8;

        float green = (shiftedGreen & 0xFF) / 255.0F;

        float blue = (argb & 0xFF) / 255.0F;

        int shiftedAlpha = argb >> 24;

        float alpha = (shiftedAlpha & 0xFF) / 255.0F;
        return new float[] {red, green, blue, alpha};
    }

    /** Unpacks RGB into red, green, and blue integer channels. */
    public static int[] toRgbInts(int rgb) {

        int shiftedRed = rgb >> 16;

        int red = shiftedRed & 0xFF;

        int shiftedGreen = rgb >> 8;

        int green = shiftedGreen & 0xFF;

        int blue = rgb & 0xFF;
        return new int[] {red, green, blue};
    }

    /** Creates a color from packed ARGB channels. */
    public static Color fromArgb(int argb) {

        int shiftedAlpha = argb >> 24;

        int alpha = shiftedAlpha & 0xFF;

        int shiftedRed = argb >> 16;

        int red = shiftedRed & 0xFF;

        int shiftedGreen = argb >> 8;

        int green = shiftedGreen & 0xFF;

        int blue = argb & 0xFF;
        return new Color(red, green, blue, alpha);
    }

    public static int waveBlend(float phaseOffset, int firstArgb, int secondArgb) {
        double timeSeconds = System.currentTimeMillis() / 1000.0;
        double phaseRadians = phaseOffset / 100.0F;
        double firstWeight = (Math.sin(timeSeconds * 3.0 + phaseRadians) + 1.0) / 2.0;
        Color firstColor = new Color(firstArgb, true);
        Color secondColor = new Color(secondArgb, true);
        int red =
                (int)
                        (firstColor.getRed() * firstWeight
                                + secondColor.getRed() * (1.0 - firstWeight));
        int green =
                (int)
                        (firstColor.getGreen() * firstWeight
                                + secondColor.getGreen() * (1.0 - firstWeight));
        int blue =
                (int)
                        (firstColor.getBlue() * firstWeight
                                + secondColor.getBlue() * (1.0 - firstWeight));
        int alpha =
                (int)
                        (firstColor.getAlpha() * firstWeight
                                + secondColor.getAlpha() * (1.0 - firstWeight));
        return new Color(red, green, blue, alpha).getRGB();
    }

    /** Interpolates HSB channels and preserves the interpolated alpha. */
    public static Color blendHsb(Color firstColor, Color secondColor, float progress) {
        progress = Math.min(1.0F, Math.max(0.0F, progress));
        float[] firstHsb =
                Color.RGBtoHSB(
                        firstColor.getRed(), firstColor.getGreen(), firstColor.getBlue(), null);
        float[] secondHsb =
                Color.RGBtoHSB(
                        secondColor.getRed(), secondColor.getGreen(), secondColor.getBlue(), null);
        Color blendedColor =
                Color.getHSBColor(
                        lerpFloat(firstHsb[0], secondHsb[0], progress),
                        lerpFloat(firstHsb[1], secondHsb[1], progress),
                        lerpFloat(firstHsb[2], secondHsb[2], progress));
        return withAlphaFactor(
                blendedColor,
                lerpInt(firstColor.getAlpha(), secondColor.getAlpha(), progress) / 255.0F);
    }

    /** Interpolates red, green, blue, and alpha channels. */
    public static Color blendRgba(Color firstColor, Color secondColor, float progress) {
        progress = Math.min(1.0F, Math.max(0.0F, progress));
        return new Color(
                lerpInt(firstColor.getRed(), secondColor.getRed(), progress),
                lerpInt(firstColor.getGreen(), secondColor.getGreen(), progress),
                lerpInt(firstColor.getBlue(), secondColor.getBlue(), progress),
                lerpInt(firstColor.getAlpha(), secondColor.getAlpha(), progress));
    }

    /** Animates a blend between the current primary and secondary theme colors. */
    public static Color animatedThemeColor(float phaseOffset) {
        return animatedBlend(
                phaseOffset,
                Astralis.getInstance().getPrimaryColor(),
                Astralis.getInstance().getSecondaryColor());
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** Blends RGB channels using the supplied weight for the first color. */
    static Color weightedRgbBlend(Color firstColor, Color secondColor, double firstWeight) {
        double secondWeight = 1.0 - firstWeight;
        int red = (int) (firstColor.getRed() * firstWeight + secondColor.getRed() * secondWeight);
        int green =
                (int) (firstColor.getGreen() * firstWeight + secondColor.getGreen() * secondWeight);
        int blue =
                (int) (firstColor.getBlue() * firstWeight + secondColor.getBlue() * secondWeight);
        return new Color(red, green, blue);
    }

    /** Animates an RGBA blend using a time-based sine wave. */
    public static Color animatedBlend(float phaseOffset, Color firstColor, Color secondColor) {
        double timeSeconds = System.currentTimeMillis() / 1000.0;
        double progress = (Math.sin(timeSeconds * 2.5 + phaseOffset * 0.025) + 1.0) / 2.0;
        return blendRgba(firstColor, secondColor, (float) progress);
    }

    /** Selects the legacy chat color code for an entity health fraction. */
    public static String healthColorCode(LivingEntity entity) {

        float healthFraction = entity.getHealth() / entity.getMaxHealth();
        if (healthFraction > 0.75) {

            return "§a";
        } else if (healthFraction > 0.5) {

            return "§e";
        } else if (healthFraction > 0.25) {

            return "§6";
        } else {

            return "§c";
        }
    }

    /** Interpolates a floating-point channel. */
    public static float lerpFloat(float start, float end, double progress) {
        return lerp(start, end, (float) progress).floatValue();
    }

    /** Animates packed ARGB channels using an explicitly supplied time. */
    public static int animatedBlendArgb(
            float phaseOffset, int firstArgb, int secondArgb, double timeSeconds) {
        double progress = (Math.sin(timeSeconds * 2.5 + phaseOffset * 0.025) + 1.0) * 0.5;

        int firstAlpha = firstArgb >>> 24;

        int firstShiftedRed = firstArgb >> 16;

        int firstRed = firstShiftedRed & 0xFF;

        int firstShiftedGreen = firstArgb >> 8;

        int firstGreen = firstShiftedGreen & 0xFF;

        int firstBlue = firstArgb & 0xFF;

        int secondAlpha = secondArgb >>> 24;

        int secondShiftedRed = secondArgb >> 16;

        int secondRed = secondShiftedRed & 0xFF;

        int secondShiftedGreen = secondArgb >> 8;

        int secondGreen = secondShiftedGreen & 0xFF;

        int secondBlue = secondArgb & 0xFF;
        int alpha = (int) (firstAlpha + (secondAlpha - firstAlpha) * progress);
        int red = (int) (firstRed + (secondRed - firstRed) * progress);
        int green = (int) (firstGreen + (secondGreen - firstGreen) * progress);
        int blue = (int) (firstBlue + (secondBlue - firstBlue) * progress);

        int packedAlpha = alpha << 24;

        int packedAlphaRed = packedAlpha | red << 16;

        return packedAlphaRed | green << 8 | blue;
    }

    /** Packs a color with its red and blue channels exchanged. */
    public static int toPackedBgr(Color color) {
        return new Color(color.getBlue(), color.getGreen(), color.getRed()).getRGB();
    }

    /** Interpolates between two values without clamping the supplied progress. */
    public static Double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    /** Interpolates packed RGB channels without adding an alpha channel. */
    public static int lerpRgb(int firstRgb, int secondRgb, float progress) {

        int firstShiftedRed = firstRgb >> 16;

        int firstRed = firstShiftedRed & 0xFF;

        int firstShiftedGreen = firstRgb >> 8;

        int firstGreen = firstShiftedGreen & 0xFF;

        int firstBlue = firstRgb & 0xFF;

        int secondShiftedRed = secondRgb >> 16;

        int secondRed = secondShiftedRed & 0xFF;

        int secondShiftedGreen = secondRgb >> 8;

        int secondGreen = secondShiftedGreen & 0xFF;

        int secondBlue = secondRgb & 0xFF;
        int red = (int) (firstRed + progress * (secondRed - firstRed));
        int green = (int) (firstGreen + progress * (secondGreen - firstGreen));
        int blue = (int) (firstBlue + progress * (secondBlue - firstBlue));

        int packedRed = red << 16;

        return packedRed | green << 8 | blue;
    }

    /**
     * Interpolates RGBA channels with progress clamped to the unit interval. Equivalent to {@link
     * #blendRgba} up to rounding, but computed in float precision.
     */
    public static Color lerpColor(Color firstColor, Color secondColor, float progress) {
        progress = Math.min(1.0F, Math.max(0.0F, progress));
        int red =
                (int)
                        (firstColor.getRed()
                                + (secondColor.getRed() - firstColor.getRed()) * progress);
        int green =
                (int)
                        (firstColor.getGreen()
                                + (secondColor.getGreen() - firstColor.getGreen()) * progress);
        int blue =
                (int)
                        (firstColor.getBlue()
                                + (secondColor.getBlue() - firstColor.getBlue()) * progress);
        int alpha =
                (int)
                        (firstColor.getAlpha()
                                + (secondColor.getAlpha() - firstColor.getAlpha()) * progress);
        return new Color(red, green, blue, alpha);
    }
}
