package astralis.ui.hud;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;

public class Draggable implements InstanceAccess {
    private final String id;
    private float width;
    private float height;
    private float scale;
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 2.5F;
    private BooleanSupplier visibilityCondition;
    private float normalizedX;
    private float normalizedY;

    public Draggable(String id, float width, float height, float x, float y) {

        this.scale = 1.0F;
        this.visibilityCondition = () -> true;
        this.id = id;
        this.width = width;
        this.height = height;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        this.normalizedX = screenWidth > 0 ? x / screenWidth : 0.0F;
        this.normalizedY = screenHeight > 0 ? y / screenHeight : 0.0F;
    }

    public float getNormalizedX() {
        return this.normalizedX;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getScaledHeight() {
        return this.height * this.scale;
    }

    public float getX() {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        return Mth.clamp(this.normalizedX * screenWidth, 0.0F, screenWidth - this.getScaledWidth());
    }

    public float getY() {
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        return Mth.clamp(
                this.normalizedY * screenHeight, 0.0F, screenHeight - this.getScaledHeight());
    }

    public float getScale() {
        return this.scale;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setPositionClamped(float x, float y, int screenWidth, int screenHeight) {
        float clampedX = Mth.clamp(x, 0.0F, screenWidth - this.getScaledWidth());
        float clampedY = Mth.clamp(y, 0.0F, screenHeight - this.getScaledHeight());
        this.normalizedX = screenWidth > 0 ? clampedX / screenWidth : 0.0F;
        this.normalizedY = screenHeight > 0 ? clampedY / screenHeight : 0.0F;
    }

    public float getHeight() {
        return this.height;
    }

    public void setVisibleWhen(BooleanSupplier visibilityCondition) {
        this.visibilityCondition = visibilityCondition;
    }

    public float getWidth() {
        return this.width;
    }

    public String getId() {
        return this.id;
    }

    public float getScaledWidth() {
        return this.width * this.scale;
    }

    public BooleanSupplier getVisibilityCondition() {
        return this.visibilityCondition;
    }

    public boolean isVisible() {
        try {
            return this.visibilityCondition.getAsBoolean();
        } catch (Exception ignored) {
            return true;
        }
    }

    public void setScale(float scale) {
        if (Float.isFinite(scale)) {
            this.scale = Mth.clamp(scale, 0.5F, 2.5F);
        }
    }

    public void setNormalizedPosition(float normalizedX, float normalizedY) {
        if (Float.isFinite(normalizedX) && Float.isFinite(normalizedY)) {
            this.normalizedX = Mth.clamp(normalizedX, 0.0F, 1.0F);
            this.normalizedY = Mth.clamp(normalizedY, 0.0F, 1.0F);
        }
    }

    public float getNormalizedY() {
        return this.normalizedY;
    }
}
