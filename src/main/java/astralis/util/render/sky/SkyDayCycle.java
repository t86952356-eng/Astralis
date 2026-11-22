package astralis.util.render.sky;

import java.util.List;

public record SkyDayCycle(int loopDays, List<FloatRange> dayRanges) {
    public SkyDayCycle(int loopDays, List<FloatRange> dayRanges) {
        dayRanges = List.copyOf(dayRanges);
        this.loopDays = loopDays;
        this.dayRanges = dayRanges;
    }

    public int getLoopDays() {
        return this.loopDays;
    }

    public List<FloatRange> getDayRanges() {
        return this.dayRanges;
    }
}
