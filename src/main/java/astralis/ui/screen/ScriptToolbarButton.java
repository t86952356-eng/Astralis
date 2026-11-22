package astralis.ui.screen;

import astralis.ui.component.ScriptsToolbarAction;

public record ScriptToolbarButton(
        String label, float x, float y, float width, ScriptsToolbarAction action, boolean primary) {
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

    public ScriptsToolbarAction getAction() {
        return this.action;
    }

    public boolean isPrimary() {
        return this.primary;
    }
}
