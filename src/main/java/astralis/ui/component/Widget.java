package astralis.ui.component;

import astralis.util.render.Render2DContext;

import lombok.Generated;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Base UI element: a rectangle (x, y, width, height) with render, mask, mouse, scroll and key
 * callbacks.
 */
public class Widget {
    protected float x;
    protected float y;
    protected float width;
    protected float height;

    public Widget() {}

    public Widget(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void mouseDragged(float mouseX, float mouseY) {}

    public void release(MouseButtonEvent mouseButtonEvent) {}

    public void click(MouseButtonEvent mouseButtonEvent) {}

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    public boolean charTyped(CharacterEvent characterEvent) {
        return false;
    }

    public void render(Render2DContext renderer, float mouseX, float mouseY) {}

    public void renderMask(Render2DContext renderer) {}

    @Generated
    public float getX() {
        return this.x;
    }

    @Generated
    public float getY() {
        return this.y;
    }

    @Generated
    public float getWidth() {
        return this.width;
    }

    @Generated
    public float getHeight() {
        return this.height;
    }

    @Generated
    public void setX(float x) {
        this.x = x;
    }

    @Generated
    public void setY(float y) {
        this.y = y;
    }

    @Generated
    public void setWidth(float width) {
        this.width = width;
    }

    @Generated
    public void setHeight(float height) {
        this.height = height;
    }
}
