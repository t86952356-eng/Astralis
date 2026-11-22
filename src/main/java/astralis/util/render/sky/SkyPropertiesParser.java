package astralis.util.render.sky;

import astralis.Astralis;

import net.minecraft.resources.Identifier;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Predicate;

public final class SkyPropertiesParser {
    private static Vector3f defaultAxis;
    private static final float MIN_AXIS_LENGTH_SQUARED = 1.0E-5F;

    private SkyPropertiesParser() {}

    static @Nullable SkyLayerProperties parse(
            Properties properties, Identifier identifier, Predicate<Identifier> textureExists) {

        Identifier identifier2 = resolveTexture(properties.getProperty("source"), identifier);
        if (identifier2 != null && textureExists.test(identifier2)) {
            SkyFadeTimes fadeTimes = parseFadeTimes(properties);
            float speed = parseFloatOrDefault(properties.getProperty("speed"), 1.0F);
            List heights = parseRanges(properties.getProperty("heights"), true);
            String days = properties.getProperty("days");

            SkyDayCycle dayCycle = parseDayCycle(days, properties.getProperty("daysLoop"));
            SkyBlendMode blendMode =
                    SkyBlendMode.fromNameOrDefault(
                            properties.getProperty("blend", ""), SkyBlendMode.ADD);
            boolean rotate = parseBooleanOrDefault(properties.getProperty("rotate"), true);
            int transitionTicks =
                    (int) (parseFloatOrDefault(properties.getProperty("transition"), 1.0F) * 20.0F);
            Vector3f vector3f = parseAxis(properties.getProperty("axis"));
            WeatherFilter weather = parseWeather(properties.getProperty("weather"));
            BiomeFilter biomes = parseBiomes(properties.getProperty("biomes"));
            return new SkyLayerProperties(
                    identifier,
                    identifier2,
                    fadeTimes,
                    speed,
                    heights,
                    dayCycle,
                    blendMode,
                    rotate,
                    transitionTicks,
                    vector3f,
                    weather,
                    biomes);
        } else {
            Astralis.LOGGER.warn(
                    "[Pack Skies] Skipping {} - texture '{}' is missing or unreadable",
                    identifier,
                    identifier2);
            return null;
        }
    }

    private static @Nullable Identifier resolveTexture(String source, Identifier identifier) {
        if (source != null && !source.isBlank()) {
            String trimmed = source.trim();

            if (trimmed.startsWith("./")) {
                String propertiesPath = identifier.getPath();

                String fileName = propertiesPath.substring(propertiesPath.lastIndexOf(47) + 1);
                return identifier.withPath(propertiesPath.replace(fileName, trimmed.substring(2)));
            }

            String[] parts = trimmed.split("/", 3);
            if (parts.length == 3) {
                String prefix = parts[0];

                if (prefix.equals("assets")) {
                    return Identifier.fromNamespaceAndPath(parts[1], parts[2]);
                }
            }

            Identifier identifier2 = Identifier.tryParse(trimmed);
            if (identifier2 == null) {

                Astralis.LOGGER.warn(
                        "[Pack Skies] Failed to read texture source '{}' as a resource location",
                        trimmed);
            }

            return identifier2;
        } else {
            String path = identifier.getPath();

            return identifier.withPath(path.replace(".properties", ".png"));
        }
    }

    private static SkyFadeTimes parseFadeTimes(Properties properties) {

        String startFadeInText = properties.getProperty("startFadeIn");

        String endFadeInText = properties.getProperty("endFadeIn");
        String endFadeOutText = properties.getProperty("endFadeOut");
        if (startFadeInText != null && endFadeInText != null && endFadeOutText != null) {
            int startFadeIn = parseTimeOfDay(startFadeInText);
            int endFadeIn = parseTimeOfDay(endFadeInText);
            int endFadeOut = parseTimeOfDay(endFadeOutText);
            int startFadeOut;
            if (properties.containsKey("startFadeOut")) {
                startFadeOut = parseTimeOfDay(properties.getProperty("startFadeOut"));
            } else {
                startFadeOut = endFadeOut - (endFadeIn - startFadeIn);
                if (startFadeIn <= startFadeOut && endFadeIn >= startFadeOut) {
                    startFadeOut = endFadeOut;
                }
            }

            return new SkyFadeTimes(startFadeIn, endFadeIn, startFadeOut, endFadeOut, false);
        } else {
            return new SkyFadeTimes(0, 0, 0, 0, true);
        }
    }

    private static int parseTimeOfDay(String text) {
        String[] parts = text.trim().split(":");
        if (parts.length == 2) {
            int minutes = parseIntOrDefault(parts[1], -1);
            int hours = parseIntOrDefault(parts[0], -1);
            if (hours >= 0) {

                if (hours <= 23 && minutes >= 0) {

                    if (minutes <= 59) {
                        int shiftedHours = hours - 6;
                        if (shiftedHours < 0) {
                            shiftedHours += 24;
                        }

                        return shiftedHours * 1000 + (int) (minutes / 60.0F * 1000.0F);
                    }
                }
            }
        }

        Astralis.LOGGER.warn("[Pack Skies] Invalid time '{}' in sky properties", text);
        return -1;
    }

    private static SkyDayCycle parseDayCycle(String daysText, String loopText) {
        if (daysText == null) {

            return new SkyDayCycle(8, List.of());
        } else {

            int loopDays = Math.max(1, parseIntOrDefault(loopText, 8));
            return new SkyDayCycle(loopDays, parseRanges(daysText, false));
        }
    }

    private static Vector3f parseAxis(String text) {
        if (text != null && !text.isBlank()) {
            String trimmed = text.trim();

            String[] parts = trimmed.split(" +");
            if (parts.length == 3) {
                Vector3f vector3f =
                        new Vector3f(
                                parseFloatSilently(parts[0], Float.MAX_VALUE),
                                parseFloatSilently(parts[1], Float.MAX_VALUE),
                                parseFloatSilently(parts[2], Float.MAX_VALUE));
                if (vector3f.lengthSquared() > 1.0E-5F) {
                    return new Vector3f(vector3f.z, vector3f.y, -vector3f.x);
                }
            }

            Astralis.LOGGER.warn(
                    "[Pack Skies] Invalid axis '{}', falling back to the X axis", text);
            return defaultAxis;
        } else {
            return defaultAxis;
        }
    }

    private static List<FloatRange> parseRanges(String text, boolean allowNegative) {
        if (text != null && !text.isBlank()) {
            ArrayList ranges = new ArrayList();
            String trimmed = text.trim();

            for (String token : trimmed.split("\\s*,\\s*|\\s+")) {
                FloatRange range = parseRange(token, allowNegative);
                if (range != null) {
                    ranges.add(range);
                }
            }

            return ranges;
        } else {
            return List.of();
        }
    }

    private static FloatRange parseRange(String text, boolean allowNegative) {
        String trimmed = text.trim();
        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length == 2) {
                String startText = parts[0];

                int start = parseIntOrDefault(startText, Integer.MIN_VALUE);
                String endText = parts[1];

                int end = parseIntOrDefault(endText, Integer.MIN_VALUE);

                if (start != Integer.MIN_VALUE) {

                    if (end != Integer.MIN_VALUE && (allowNegative || start >= 0 && end >= 0)) {
                        return new FloatRange(start, end);
                    }
                }
            }

            return null;
        } else {
            String valueText =
                    allowNegative && trimmed.startsWith("(") && trimmed.endsWith(")")
                            ? trimmed.substring(1, trimmed.length() - 1)
                            : trimmed;

            int value = parseIntOrDefault(valueText, Integer.MIN_VALUE);

            return value == Integer.MIN_VALUE || !allowNegative && value < 0
                    ? null
                    : new FloatRange(value, value);
        }
    }

    private static WeatherFilter parseWeather(String text) {
        if (text != null && !text.isBlank()) {
            ArrayList weathers = new ArrayList();
            String trimmed = text.trim();

            for (String token : trimmed.split(" +")) {
                try {
                    weathers.add(SkyWeather.fromName(token));
                } catch (IllegalArgumentException ignored) {

                    Astralis.LOGGER.warn("[Pack Skies] Unknown weather condition '{}'", token);
                }
            }

            return weathers.isEmpty()
                    ? new WeatherFilter(List.of(SkyWeather.CLEAR))
                    : new WeatherFilter(weathers);
        } else {
            return new WeatherFilter(List.of(SkyWeather.CLEAR));
        }
    }

    private static BiomeFilter parseBiomes(String text) {
        if (text != null && !text.isBlank()) {
            String body = text.trim();
            boolean inverted = body.startsWith("!");
            if (inverted) {
                body = body.substring(1);
            }

            ArrayList biomes = new ArrayList();
            String names = body.trim();

            for (String token : names.split(" +")) {
                Identifier identifier = Identifier.tryParse(token.trim().toLowerCase(Locale.ROOT));
                if (identifier != null) {
                    biomes.add(identifier);
                } else {

                    Astralis.LOGGER.warn("[Pack Skies] Unknown biome location '{}'", token);
                }
            }

            return new BiomeFilter(biomes, inverted);
        } else {
            return new BiomeFilter(List.of(), false);
        }
    }

    private static boolean parseBooleanOrDefault(String text, boolean defaultValue) {
        return text == null ? defaultValue : Boolean.parseBoolean(text.trim());
    }

    private static float parseFloatOrDefault(String text, float defaultValue) {
        if (text == null) {
            return defaultValue;
        }

        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException ignored) {

            Astralis.LOGGER.warn("[Pack Skies] Invalid number '{}'", text);
            return defaultValue;
        }
    }

    private static int parseIntOrDefault(String text, int defaultValue) {
        if (text == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static float parseFloatSilently(String text, float defaultValue) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static {
        defaultAxis = new Vector3f(1.0F, 0.0F, 0.0F);
    }
}
