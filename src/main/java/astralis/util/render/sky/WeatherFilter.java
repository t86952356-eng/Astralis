package astralis.util.render.sky;

import net.minecraft.util.Mth;

import java.util.List;

public record WeatherFilter(List<SkyWeather> weathers) {

    public WeatherFilter(List<SkyWeather> weathers) {
        weathers = List.copyOf(weathers);
        this.weathers = weathers;
    }

    public float computeWeight(float rainLevel, float thunderLevel) {
        float clearWeight = 1.0F - rainLevel;
        float rainWeight = rainLevel - thunderLevel;
        float totalWeight = 0.0F;
        if (this.weathers.contains(SkyWeather.CLEAR)) {
            totalWeight += clearWeight;
        }

        if (this.weathers.contains(SkyWeather.RAIN)) {
            totalWeight += rainWeight;
        }

        if (this.weathers.contains(SkyWeather.THUNDER)) {
            totalWeight += thunderLevel;
        }

        float clamped = Mth.clamp(totalWeight, 0.0F, 1.0F);
        return clamped;
    }

    public List<SkyWeather> getWeathers() {
        return this.weathers;
    }
}
