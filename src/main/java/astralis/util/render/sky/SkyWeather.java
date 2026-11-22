package astralis.util.render.sky;

public enum SkyWeather {
    CLEAR,
    RAIN,
    THUNDER;

    public static SkyWeather fromName(String name) {
        return valueOf(name.toUpperCase());
    }
}
