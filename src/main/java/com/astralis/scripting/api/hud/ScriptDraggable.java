package com.astralis.scripting.api.hud;

import astralis.Astralis;
import astralis.ui.hud.Draggable;
import astralis.ui.hud.HudManager;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;

import java.util.function.BooleanSupplier;

public class ScriptDraggable implements InstanceAccess {
    private final String id;
    private String identity;
    private Draggable hudElement;
    private float width;
    private float height;
    private float x;
    private float y;
    private float normalizedX;
    private float normalizedY;
    private float scale = 1.0F;
    private boolean useNormalizedPosition;
    private boolean visible = true;
    private boolean removed;
    private Script owner;

    ScriptDraggable(String id, float width, float height, float x, float y) {
        this.id = id;
        this.validateSize(width, height);
        this.validatePosition(x, y);
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    void register(Script script) {
        if (this.removed) {
            throw new IllegalStateException("The draggable has been removed.");
        }

        if (this.hudElement != null) {
            throw new IllegalStateException("The draggable is already registered.");
        }

        this.owner = script;
        this.identity = "Script:" + script.getName() + ":" + this.id;
        this.hudElement = new Draggable(this.identity, this.width, this.height, this.x, this.y);
        float elementScale = this.scale;
        Draggable scaleTarget = this.hudElement;
        scaleTarget.setScale(elementScale);
        Draggable currentElement = this.hudElement;
        BooleanSupplier visibilitySupplier = this::isHudVisible;
        Draggable visibilityTarget = currentElement;
        visibilityTarget.setVisibleWhen(visibilitySupplier);
        if (this.useNormalizedPosition) {
            float normalizedYValue = this.normalizedY;
            float normalizedXValue = this.normalizedX;
            Draggable positionTarget = this.hudElement;
            positionTarget.setNormalizedPosition(normalizedXValue, normalizedYValue);
        }

        Astralis astralisInstance = Astralis.getInstance();
        HudManager resolvedHudManager = astralisInstance.getHudManager();
        Draggable elementToAdd = this.hudElement;
        HudManager hudManager = resolvedHudManager;
        if (!hudManager.addUniqueDraggable(elementToAdd)) {
            this.hudElement = null;
            this.owner = null;
            throw new IllegalStateException(
                    "A draggable with this identity is already registered: " + this.identity);
        }
    }

    void dispose() {
        if (this.hudElement != null) {
            Draggable widthElement = this.hudElement;
            this.width = widthElement.getWidth();
            Draggable heightElement = this.hudElement;
            this.height = heightElement.getHeight();
            Draggable xElement = this.hudElement;
            this.x = xElement.getX();
            Draggable yElement = this.hudElement;
            this.y = yElement.getY();
            Draggable normalizedXElement = this.hudElement;
            this.normalizedX = normalizedXElement.getNormalizedX();
            Draggable normalizedYElement = this.hudElement;
            this.normalizedY = normalizedYElement.getNormalizedY();
            this.useNormalizedPosition = true;
            Draggable scaleElement = this.hudElement;
            this.scale = scaleElement.getScale();
            Astralis astralisInstance = Astralis.getInstance();
            HudManager resolvedHudManager = astralisInstance.getHudManager();
            Draggable elementToRemove = this.hudElement;
            HudManager hudManager = resolvedHudManager;
            hudManager.removeDraggable(elementToRemove);
            this.hudElement = null;
        }

        this.owner = null;
        this.removed = true;
    }

    public String getId() {
        return this.id;
    }

    public String getIdentity() {
        return this.identity != null ? this.identity : this.id;
    }

    public float getX() {
        float currentX;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentX = element.getX();
        } else {
            currentX = this.x;
        }

        return currentX;
    }

    public float getY() {
        float currentY;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentY = element.getY();
        } else {
            currentY = this.y;
        }

        return currentY;
    }

    public float getWidth() {
        float currentWidth;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentWidth = element.getWidth();
        } else {
            currentWidth = this.width;
        }

        return currentWidth;
    }

    public float getHeight() {
        float currentHeight;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentHeight = element.getHeight();
        } else {
            currentHeight = this.height;
        }

        return currentHeight;
    }

    public float getScaledWidth() {
        float currentScaledWidth;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentScaledWidth = element.getScaledWidth();
        } else {
            currentScaledWidth = this.width * this.scale;
        }

        return currentScaledWidth;
    }

    public float getScaledHeight() {
        float currentScaledHeight;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentScaledHeight = element.getScaledHeight();
        } else {
            currentScaledHeight = this.height * this.scale;
        }

        return currentScaledHeight;
    }

    public float getScale() {
        float currentScale;
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            currentScale = element.getScale();
        } else {
            currentScale = this.scale;
        }

        return currentScale;
    }

    public float getNormalizedX() {
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            return element.getNormalizedX();
        } else if (this.useNormalizedPosition) {
            return this.normalizedX;
        } else {
            return mc.getWindow().getGuiScaledWidth() > 0
                    ? Math.clamp(this.x / mc.getWindow().getGuiScaledWidth(), 0.0F, 1.0F)
                    : 0.0F;
        }
    }

    public float getNormalizedY() {
        if (this.hudElement != null) {
            Draggable element = this.hudElement;
            return element.getNormalizedY();
        } else if (this.useNormalizedPosition) {
            return this.normalizedY;
        } else {
            return mc.getWindow().getGuiScaledHeight() > 0
                    ? Math.clamp(this.y / mc.getWindow().getGuiScaledHeight(), 0.0F, 1.0F)
                    : 0.0F;
        }
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isRegistered() {
        return this.hudElement != null;
    }

    public void setPosition(float x, float y) {
        this.ensureActive();
        this.validatePosition(x, y);
        this.x = x;
        this.y = y;
        this.useNormalizedPosition = false;
        if (this.hudElement != null) {
            Draggable currentElement = this.hudElement;
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            float targetY = y;
            float targetX = x;
            Draggable positionTarget = currentElement;
            positionTarget.setPositionClamped(targetX, targetY, screenWidth, screenHeight);
        }
    }

    public void setNormalizedPosition(float normalizedX, float normalizedY) {
        this.ensureActive();
        this.validatePosition(normalizedX, normalizedY);
        this.normalizedX = Math.clamp(normalizedX, 0.0F, 1.0F);
        this.normalizedY = Math.clamp(normalizedY, 0.0F, 1.0F);
        this.useNormalizedPosition = true;
        if (this.hudElement != null) {
            float clampedY = this.normalizedY;
            float clampedX = this.normalizedX;
            Draggable normalizedTarget = this.hudElement;
            normalizedTarget.setNormalizedPosition(clampedX, clampedY);
        }
    }

    public void setSize(float width, float height) {
        this.ensureActive();
        this.validateSize(width, height);
        this.width = width;
        this.height = height;
        if (this.hudElement != null) {
            float newWidth = width;
            Draggable widthTarget = this.hudElement;
            widthTarget.setWidth(newWidth);
            float newHeight = height;
            Draggable heightTarget = this.hudElement;
            heightTarget.setHeight(newHeight);
        }
    }

    public void setScale(float scale) {
        this.ensureActive();
        if (!Float.isFinite(scale)) {
            throw new IllegalArgumentException("Draggable scale must be finite.");
        }

        this.scale = Math.clamp(scale, 0.5F, 2.5F);
        if (this.hudElement != null) {
            float clampedScale = this.scale;
            Draggable scaleTarget = this.hudElement;
            scaleTarget.setScale(clampedScale);
        }
    }

    public void setVisible(boolean visible) {
        this.ensureActive();
        this.visible = visible;
    }

    private boolean isHudVisible() {
        return this.visible && this.owner != null && this.owner.isEnabled();
    }

    private void ensureActive() {
        if (this.removed || this.owner != null && !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("The draggable has been removed.");
        }
    }

    private void validateSize(float width, float height) {
        if (!Float.isFinite(width)
                || !Float.isFinite(height)
                || width <= 0.0F
                || height <= 0.0F
                || width > 1000000.0F
                || height > 1000000.0F) {
            throw new IllegalArgumentException(
                    "Draggable dimensions must be positive and at most 1,000,000.");
        }
    }

    private void validatePosition(float x, float y) {
        if (!Float.isFinite(x)
                || !Float.isFinite(y)
                || Math.abs(x) > 3.3554432E7F
                || Math.abs(y) > 3.3554432E7F) {
            throw new IllegalArgumentException(
                    "Draggable coordinates are outside supported bounds.");
        }
    }
}
