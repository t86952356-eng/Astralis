package astralis.command.impl;

import net.minecraft.resources.Identifier;

public record FloatingImage(
        Identifier texture,
        int width,
        int height,
        int textureWidth,
        int textureHeight,
        float speedX,
        float speedY,
        float phase) {
    public int getWidth() {
        return this.width;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public int getTextureWidth() {
        return this.textureWidth;
    }

    public int getTextureHeight() {
        return this.textureHeight;
    }

    public int getHeight() {
        return this.height;
    }

    public float getSpeedX() {
        return this.speedX;
    }

    public float getPhase() {
        return this.phase;
    }

    public float getSpeedY() {
        return this.speedY;
    }
}
