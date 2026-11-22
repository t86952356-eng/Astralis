package astralis.setting;

import astralis.module.Module;
import astralis.util.MathUtil;

import java.util.List;

public class NumberSetting extends Setting<Float> {
    private final Float min;
    private final Float max;
    private final Float step;

    public NumberSetting(
            String id, Module owner, Float initialValue, Float min, Float max, Float step) {
        super(id, owner, initialValue);
        this.step = step;
        this.min = min;
        this.max = max;
    }

    public NumberSetting(String id, Float initialValue, Float min, Float max, Float step) {
        super(id, initialValue);
        this.step = step;
        this.min = min;
        this.max = max;
    }

    public Float getMin() {
        return this.min;
    }

    public Float getStep() {
        return this.step;
    }

    public void setValueRaw(Float newValue) {
        float clampedValue = Math.clamp(newValue, this.min, this.max);
        super.setValueRaw(
                this.step > 0.0F ? MathUtil.roundToStep(clampedValue, this.step) : clampedValue);
    }

    public Float getMax() {
        return this.max;
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }
}
