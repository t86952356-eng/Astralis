package astralis.util.render.sky;

import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import org.joml.Matrix4f;

import java.util.List;

public final class SkyLayer {
    private static final float MIN_VISIBLE_ALPHA = 1.0E-4F;
    private final SkyLayerProperties properties;
    private float fade = -1.0F;

    SkyLayer(SkyLayerProperties properties) {
        this.properties = properties;
    }

    private float computeRotationDegrees(ClientLevel clientLevel, float skyAngle) {
        float baseRotation = 0.0F;
        if (this.properties.getSpeed() != Math.round(this.properties.getSpeed())) {
            long dayNumber = (clientLevel.getOverworldClockTime() + 18000L) / 24000L;
            baseRotation = (float) dayNumber * (this.properties.getSpeed() % 1.0F) % 1.0F;
        }

        return 360.0F * (baseRotation + skyAngle * this.properties.getSpeed());
    }

    private boolean matchesCameraLocation(ClientLevel clientLevel) {
        Entity entity2 = Minecraft.getInstance().getCameraEntity();
        if (entity2 == null) {
            return false;
        }

        if (!this.properties.getBiomes().isEmpty()) {
            Holder biome = clientLevel.getBiome(entity2.blockPosition());
            ResourceKey resourceKey = (ResourceKey) biome.unwrapKey().orElse(null);
            if (resourceKey == null
                    || !this.properties.getBiomes().matches(resourceKey.identifier())) {
                return false;
            }
        }

        BlockPos blockPos = entity2.getOnPos();
        return this.properties.getHeights().isEmpty()
                || FloatRange.anyContains(this.properties.getHeights(), blockPos.getY());
    }

    void updateFade(boolean active, ClientLevel clientLevel) {
        this.fade = active ? this.computeTargetFade(clientLevel) : -1.0F;
    }

    public SkyLayerProperties getProperties() {
        return this.properties;
    }

    private static float stepFade(float max, float min, float current, int ticks, boolean fadeIn) {
        if (ticks == 0) {
            return current;
        }

        if (fadeIn && max == current) {
            return max;
        }

        if (!fadeIn && current == min) {
            return min;
        }

        float step = (max - min) / ticks;
        return Mth.clamp(fadeIn ? current + step : current - step, min, max);
    }

    void queueSubmit(
            Matrix4f matrix4f,
            ClientLevel clientLevel,
            int dayTime,
            float skyAngle,
            float rainLevel,
            float thunderFraction,
            List<SkySubmit> submits) {
        float weatherWeight =
                this.properties.getWeather().computeWeight(rainLevel, thunderFraction);
        float fadeFactor = this.properties.getFadeTimes().getFadeFactor(dayTime);
        float alpha = Mth.clamp(this.fade * weatherWeight * fadeFactor, 0.0F, 1.0F);
        if (!(alpha < 1.0E-4F)) {
            Matrix4f matrix4f2 = new Matrix4f(matrix4f);
            if (this.properties.isRotating()) {
                matrix4f2.rotate(
                        Axis.of(this.properties.getAxis())
                                .rotationDegrees(
                                        this.computeRotationDegrees(clientLevel, skyAngle)));
            }

            submits.add(
                    new SkySubmit(
                            this.properties.getTexture(),
                            this.properties.getBlendMode(),
                            matrix4f2,
                            this.properties.getBlendMode().getColor(alpha),
                            alpha));
        }
    }

    boolean isActiveAt(long clockTime, int dayTime) {
        SkyFadeTimes fadeTimes = this.properties.getFadeTimes();
        if (!fadeTimes.isAlwaysVisible()
                && SkyLayerProperties.isTimeInWindow(
                        dayTime, fadeTimes.getEndFadeOut(), fadeTimes.getStartFadeIn())) {
            return false;
        }

        SkyDayCycle dayCycle = this.properties.getDayCycle();
        if (dayCycle.getDayRanges().isEmpty()) {
            return true;
        }

        long elapsed = clockTime - fadeTimes.getStartFadeIn();

        while (elapsed < 0L) {
            elapsed += 24000L * dayCycle.getLoopDays();
        }

        int dayIndex = (int) (elapsed / 24000L) % dayCycle.getLoopDays();
        return FloatRange.anyContains(dayCycle.getDayRanges(), dayIndex);
    }

    private float computeTargetFade(ClientLevel clientLevel) {
        BiomeFilter biomes = this.properties.getBiomes();
        if (biomes.isEmpty() && this.properties.getHeights().isEmpty()) {
            return 1.0F;
        } else {
            boolean matches = this.matchesCameraLocation(clientLevel);
            if (this.fade == -1.0F) {
                return matches ? 1.0F : 0.0F;
            } else {
                return stepFade(
                        1.0F, 0.0F, this.fade, this.properties.getTransitionTicks(), matches);
            }
        }
    }
}
