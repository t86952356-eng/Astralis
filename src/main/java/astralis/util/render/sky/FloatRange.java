package astralis.util.render.sky;

import java.util.Iterator;
import java.util.List;

public record FloatRange(float min, float max) {
    public boolean isWithin(float value) {
        return value >= this.min && value <= this.max;
    }

    public static boolean anyContains(List<FloatRange> ranges, float value) {
        Iterator iterator = ranges.iterator();

        while (iterator.hasNext()) {
            FloatRange range = (FloatRange) iterator.next();
            if (range.isWithin(value)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("%s..%s", this.min, this.max);
    }

    public float getMin() {
        return this.min;
    }

    public float getMax() {
        return this.max;
    }
}
