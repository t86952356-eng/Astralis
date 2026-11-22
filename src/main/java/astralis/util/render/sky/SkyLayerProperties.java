package astralis.util.render.sky;

import net.minecraft.resources.Identifier;

import org.joml.Vector3f;

import java.util.List;

public record SkyLayerProperties(
        Identifier propertiesId,
        Identifier texture,
        SkyFadeTimes fadeTimes,
        float speed,
        List<FloatRange> heights,
        SkyDayCycle dayCycle,
        SkyBlendMode blendMode,
        boolean rotate,
        int transitionTicks,
        Vector3f axis,
        WeatherFilter weather,
        BiomeFilter biomes) {

    public SkyLayerProperties(
            Identifier propertiesId,
            Identifier texture,
            SkyFadeTimes fadeTimes,
            float speed,
            List<FloatRange> heights,
            SkyDayCycle dayCycle,
            SkyBlendMode blendMode,
            boolean rotate,
            int transitionTicks,
            Vector3f axis,
            WeatherFilter weather,
            BiomeFilter biomes) {
        heights = List.copyOf(heights);
        this.propertiesId = propertiesId;
        this.texture = texture;
        this.fadeTimes = fadeTimes;
        this.speed = speed;
        this.heights = heights;
        this.dayCycle = dayCycle;
        this.blendMode = blendMode;
        this.rotate = rotate;
        this.transitionTicks = transitionTicks;
        this.axis = axis;
        this.weather = weather;
        this.biomes = biomes;
    }

    public float getSpeed() {
        return this.speed;
    }

    public int getTransitionTicks() {
        return this.transitionTicks;
    }

    public WeatherFilter getWeather() {
        return this.weather;
    }

    public SkyFadeTimes getFadeTimes() {
        return this.fadeTimes;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public BiomeFilter getBiomes() {
        return this.biomes;
    }

    public static boolean isTimeInWindow(int time, int start, int end) {

        if (time >= 0) {

            if (time < 24000) {
                return start <= end ? time >= start && time <= end : time >= start || time <= end;
            }
        }

        return false;
    }

    public Vector3f getAxis() {
        return this.axis;
    }

    public SkyBlendMode getBlendMode() {
        return this.blendMode;
    }

    public SkyDayCycle getDayCycle() {
        return this.dayCycle;
    }

    public List<FloatRange> getHeights() {
        return this.heights;
    }

    public boolean isRotating() {
        return this.rotate;
    }
}
