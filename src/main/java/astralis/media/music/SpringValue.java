package astralis.media.music;

import lombok.Generated;

public final class SpringValue {
    private final double frequency;
    private final double dampingRatio;
    private double value;
    private double velocity;
    private double target;

    public SpringValue(double initialValue, double frequency, double dampingRatio) {
        this.frequency = frequency;
        this.dampingRatio = dampingRatio;
        this.target = initialValue;
        this.value = initialValue;
    }

    public boolean isSettledAt(double expectedTarget) {
        return Math.abs(this.target - expectedTarget) < 1.0E-6
                && Math.abs(this.value - this.target) < 0.001
                && Math.abs(this.velocity) < 0.01;
    }

    public double advance(double deltaTime) {
        double omega = this.frequency * 2.0 * Math.PI;
        double decay = Math.exp(-this.dampingRatio * omega * deltaTime);
        double dampedScale = Math.sqrt(1.0 - this.dampingRatio * this.dampingRatio);
        double cosine = Math.cos(deltaTime * omega * dampedScale);
        double sine = Math.sin(deltaTime * omega * dampedScale);
        double sineOverScale = sine / dampedScale;
        double sineOverOmega = sine / (omega * dampedScale);
        double displacement = this.value - this.target;
        this.value =
                (displacement * (cosine + sineOverScale * this.dampingRatio)
                                        + this.velocity * sineOverOmega)
                                * decay
                        + this.target;
        this.velocity =
                (this.velocity * (cosine - sineOverScale * this.dampingRatio)
                                - displacement * sineOverScale * omega)
                        * decay;
        return this.value;
    }

    @Generated
    public void setTarget(double target) {
        this.target = target;
    }
}
