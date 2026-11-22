package com.astralis.scripting.api.math;

public final class ScriptNoise {
    private ScriptNoise() {}

    public static double value(long seed, double x) {
        long floorX = (long) Math.floor(x);
        return lerp(latticeValue(seed, floorX), latticeValue(seed, floorX + 1L), fade(x - floorX));
    }

    public static double value(long seed, double x, double y) {
        long floorX = (long) Math.floor(x);
        long floorY = (long) Math.floor(y);
        double fadeX = fade(x - floorX);
        double fadeY = fade(y - floorY);
        return lerp(
                lerp(
                        latticeValue(seed, floorX, floorY),
                        latticeValue(seed, floorX + 1L, floorY),
                        fadeX),
                lerp(
                        latticeValue(seed, floorX, floorY + 1L),
                        latticeValue(seed, floorX + 1L, floorY + 1L),
                        fadeX),
                fadeY);
    }

    public static double fractal(
            long seed, double x, double y, int octaves, double lacunarity, double persistence) {
        if (octaves < 1 || octaves > 16) {
            throw new IllegalArgumentException("Octaves must be between 1 and 16.");
        }

        if (Double.isFinite(lacunarity)
                && !(lacunarity <= 0.0)
                && Double.isFinite(persistence)
                && !(persistence < 0.0)
                && !(persistence > 1.0)) {
            double frequency = 1.0;
            double amplitude = 1.0;
            double sum = 0.0;
            double amplitudeSum = 0.0;

            for (int octave = 0; octave < octaves; octave++) {
                sum +=
                        value(seed + octave * -7046029254386353131L, x * frequency, y * frequency)
                                * amplitude;
                amplitudeSum += amplitude;
                frequency *= lacunarity;
                amplitude *= persistence;
            }

            return amplitudeSum > 0.0 ? sum / amplitudeSum : 0.0;
        } else {
            throw new IllegalArgumentException(
                    "Noise parameters are outside their supported bounds.");
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    private static double latticeValue(long seed, long... coords) {
        long hash = seed;

        for (long coord : coords) {
            hash ^= coord + -7046029254386353131L + (hash << 6) + (hash >>> 2);
            hash = (hash ^ hash >>> 30) * -4658895280553007687L;
            hash = (hash ^ hash >>> 27) * -7723592293110705685L;
            hash ^= hash >>> 31;
        }

        return (hash >>> 11) * 1.110223E-16F * 2.0 - 1.0;
    }
}
