package astralis.setting;

import astralis.util.MathUtil;

import java.util.List;

public class RangeSetting extends Setting<float[]> {
    private final float min;
    private final float max;
    private final float step;

    public RangeSetting(
            String id, float initialLow, float initialHigh, float min, float max, float step) {
        super(id, new float[] {initialLow, initialHigh});
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public float getMax() {
        return this.max;
    }

    public float getLow() {
        return this.getValue()[0];
    }

    public void setRange(float low, float high) {
        float snappedLow = this.snap(low);
        float snappedHigh = this.snap(high);
        this.setValueRaw(
                new float[] {Math.min(snappedLow, snappedHigh), Math.max(snappedLow, snappedHigh)});
    }

    public void setLow(float low) {
        float snappedLow = this.snap(low);
        this.setValueRaw(
                new float[] {Math.min(snappedLow, this.getValue()[1]), this.getValue()[1]});
    }

    private float snap(float value) {
        float clampedValue = Math.clamp(value, this.min, this.max);
        return this.step > 0.0F ? MathUtil.roundToStep(clampedValue, this.step) : clampedValue;
    }

    public float getStep() {
        return this.step;
    }

    public void setHigh(float high) {
        float snappedHigh = this.snap(high);
        this.setValueRaw(
                new float[] {this.getValue()[0], Math.max(snappedHigh, this.getValue()[0])});
    }

    public float getMin() {
        return this.min;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }

    public float getHigh() {
        return this.getValue()[1];
    }
}
