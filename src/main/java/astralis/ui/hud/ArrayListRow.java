package astralis.ui.hud;

import astralis.module.Module;

public record ArrayListRow(
        Module module,
        String label,
        float x,
        float y,
        float width,
        float height,
        float targetX,
        float fullWidth,
        float animationProgress,
        int color,
        int index,
        float previousWidth) {
    public Module getModule() {
        return this.module;
    }

    public String getLabel() {
        return this.label;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHeight() {
        return this.height;
    }

    public float getTargetX() {
        return this.targetX;
    }

    public float getFullWidth() {
        return this.fullWidth;
    }

    public float getAnimationProgress() {
        return this.animationProgress;
    }

    public int getIndex() {
        return this.index;
    }

    public float getPreviousWidth() {
        return this.previousWidth;
    }
}
