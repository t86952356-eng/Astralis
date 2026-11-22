package astralis.util.render;

import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public record WorldText(
        String text, Vec3 position, float size, float yaw, Color startColor, Color endColor) {
    public WorldText(String text, Vec3 position, float size, float yaw, Color color) {
        this(text, position, size, yaw, color, color);
    }

    public String getText() {
        return this.text;
    }

    public Vec3 getPosition() {
        return this.position;
    }

    public float getSize() {
        return this.size;
    }

    public float getYaw() {
        return this.yaw;
    }

    public Color getStartColor() {
        return this.startColor;
    }

    public Color getEndColor() {
        return this.endColor;
    }
}
