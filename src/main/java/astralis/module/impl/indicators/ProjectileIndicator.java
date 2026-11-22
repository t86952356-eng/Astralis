package astralis.module.impl.indicators;

import net.minecraft.world.item.ItemStack;

public record ProjectileIndicator(
        ItemStack stack,
        float centerX,
        float centerY,
        float directionX,
        float directionY,
        float radius,
        float markerX,
        float markerY,
        float rotation,
        int alpha,
        int color,
        double secondsToImpact) {
    public ItemStack getStack() {
        return this.stack;
    }

    public float getCenterX() {
        return this.centerX;
    }

    public float getCenterY() {
        return this.centerY;
    }

    public float getDirectionX() {
        return this.directionX;
    }

    public float getDirectionY() {
        return this.directionY;
    }

    public float getRadius() {
        return this.radius;
    }

    public float getMarkerX() {
        return this.markerX;
    }

    public float getMarkerY() {
        return this.markerY;
    }

    public float getRotation() {
        return this.rotation;
    }

    public int getAlpha() {
        return this.alpha;
    }

    public int getColor() {
        return this.color;
    }

    public double getSecondsToImpact() {
        return this.secondsToImpact;
    }
}
