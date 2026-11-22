package astralis.ui;

import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.resources.Identifier;

public record CustomSpriteIconEntry(
        SpriteIconButton button,
        Identifier sprite,
        int x,
        int y,
        int width,
        int height,
        float alpha) {
    public int getWidth() {
        return this.width;
    }

    public int getY() {
        return this.y;
    }

    public Identifier getSprite() {
        return this.sprite;
    }

    public SpriteIconButton getButton() {
        return this.button;
    }

    public int getX() {
        return this.x;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public int getHeight() {
        return this.height;
    }
}
