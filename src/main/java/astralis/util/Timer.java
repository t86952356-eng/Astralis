package astralis.util;

/** Measures elapsed wall-clock time from a reset or an explicitly supplied reference time. */
public class Timer {
    private long referenceTimeMillis;

    public Timer() {
        this.reset();
    }

    /** Sets the timer's reference time in milliseconds since the Unix epoch. */
    public void setReferenceTime(long timestampMillis) {
        this.referenceTimeMillis = timestampMillis;
    }

    public void reset() {
        this.referenceTimeMillis = System.currentTimeMillis();
    }

    /** Returns the reference timestamp in milliseconds since the Unix epoch. */
    public long getReferenceTime() {
        return this.referenceTimeMillis;
    }

    /** Returns the elapsed wall-clock time since the current reference timestamp. */
    public long getElapsedTime() {
        return System.currentTimeMillis() - this.referenceTimeMillis;
    }

    public boolean hasPassed(long durationMillis) {
        return System.currentTimeMillis() - durationMillis >= this.referenceTimeMillis;
    }
}
