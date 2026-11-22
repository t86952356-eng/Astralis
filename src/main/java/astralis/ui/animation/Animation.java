package astralis.ui.animation;

import astralis.util.Easing;
import astralis.util.Timer;

/** Eased animation from a start value toward a target over a duration. */
public class Animation {
    private final Timer timer;
    private Easing easing;
    private long durationMillis;
    private double startValue;
    private double target;
    private double value;
    private boolean finished;

    public Animation(Easing easing, long durationMillis) {

        this.timer = new Timer();
        this.finished = true;
        this.easing = easing;
        this.durationMillis = durationMillis;
    }

    public void restart() {
        this.timer.reset();
        this.startValue = this.value;
        this.finished = false;
    }

    public double getValue() {
        return this.value;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void animateTo(double newTarget) {
        if (this.target != newTarget) {
            this.target = newTarget;
            this.restart();
        } else {
            this.finished = this.timer.hasPassed(this.durationMillis);
            if (this.finished) {
                this.value = newTarget;
                return;
            }
        }

        double easedProgress = this.easing.getFunction().apply(this.getProgress());
        if (this.value > newTarget) {
            this.value = this.startValue - (this.startValue - newTarget) * easedProgress;
        } else {
            this.value = this.startValue + (newTarget - this.startValue) * easedProgress;
        }
    }

    public void setStartValue(double startValue) {
        this.startValue = startValue;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public void setDuration(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public long getDuration() {
        return this.durationMillis;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getProgress() {
        return (double) (System.currentTimeMillis() - this.timer.getReferenceTime())
                / this.durationMillis;
    }

    public Timer getTimer() {
        return this.timer;
    }

    public double getStartValue() {
        return this.startValue;
    }

    public double getTarget() {
        return this.target;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
