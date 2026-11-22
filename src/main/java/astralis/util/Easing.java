package astralis.util;

import astralis.metadata.MetadataState;

import java.util.function.Function;

public enum Easing {
    EASE_IN_OUT_SINE(progress -> -(Math.cos(readConstant("pi") * progress) - 1.0) / 2.0),
    EASE_IN_OUT_QUAD(
            progress ->
                    progress < 0.5
                            ? 2.0 * progress * progress
                            : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0),
    EASE_OUT_BACK(
            progress -> {
                double cubicContribution =
                        1.0 + readConstant("ease_back_c2") * Math.pow(progress - 1.0, 3.0);

                return cubicContribution
                        + readConstant("ease_back_c1") * Math.pow(progress - 1.0, 2.0);
            }),
    EASE_IN_BACK(
            progress -> {
                double overshoot = readConstant("ease_back_c1");
                return progress * progress * ((overshoot + 1.0) * progress - overshoot);
            }),
    EASE_IN_SINE(
            progress -> {
                double inputProgress = progress;

                return 1.0 - Math.cos(inputProgress * readConstant("pi") / 2.0);
            }),
    EASE_OUT_SINE(
            progress -> {
                double inputProgress = progress;

                return Math.sin(inputProgress * readConstant("pi") / 2.0);
            }),
    EASE_IN_QUAD(progress -> progress * progress),
    EASE_OUT_QUAD(progress -> 1.0 - (1.0 - progress) * (1.0 - progress)),
    EASE_IN_CUBIC(progress -> progress * progress * progress),
    EASE_OUT_CUBIC(progress -> 1.0 - Math.pow(1.0 - progress, 3.0)),
    EASE_IN_OUT_CUBIC(
            progress ->
                    progress < 0.5
                            ? 4.0 * progress * progress * progress
                            : 1.0 - Math.pow(-2.0 * progress + 2.0, 3.0) / 2.0),
    EASE_IN_QUART(progress -> progress * progress * progress * progress),
    EASE_OUT_QUART(progress -> 1.0 - Math.pow(1.0 - progress, 4.0)),
    EASE_IN_OUT_QUART(
            progress ->
                    progress < 0.5
                            ? 8.0 * Math.pow(progress, 4.0)
                            : 1.0 - Math.pow(-2.0 * progress + 2.0, 4.0) / 2.0),
    EASE_IN_QUINT(progress -> progress * progress * progress * progress * progress),
    EASE_OUT_QUINT(progress -> 1.0 - Math.pow(1.0 - progress, 5.0)),
    EASE_IN_OUT_QUINT(
            progress ->
                    progress < 0.5
                            ? 16.0 * Math.pow(progress, 5.0)
                            : 1.0 - Math.pow(-2.0 * progress + 2.0, 5.0) / 2.0),
    EASE_IN_EXPO(progress -> progress == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * (progress - 1.0))),
    EASE_OUT_EXPO(progress -> progress == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * progress)),
    EASE_IN_OUT_EXPO(
            progress -> {
                if (progress == 0.0) {
                    return 0.0;
                } else {
                    return progress == 1.0
                            ? 1.0
                            : progress < 0.5
                                    ? Math.pow(2.0, 20.0 * progress - 10.0) / 2.0
                                    : (2.0 - Math.pow(2.0, -20.0 * progress + 10.0)) / 2.0;
                }
            }),
    EASE_IN_CIRC(progress -> 1.0 - Math.sqrt(1.0 - progress * progress)),
    EASE_OUT_CIRC(progress -> Math.sqrt(1.0 - Math.pow(progress - 1.0, 2.0))),
    EASE_IN_OUT_CIRC(
            progress ->
                    progress < 0.5
                            ? (1.0 - Math.sqrt(1.0 - 4.0 * progress * progress)) / 2.0
                            : (Math.sqrt(1.0 - Math.pow(-2.0 * progress + 2.0, 2.0)) + 1.0) / 2.0),
    EASE_IN_OUT_BACK(
            progress -> {
                double overshoot = readConstant("ease_back_c1");

                double scaledOvershoot = overshoot * readConstant("ease_back_scale");

                return progress < 0.5
                        ? Math.pow(2.0 * progress, 2.0)
                                * ((scaledOvershoot + 1.0) * 2.0 * progress - scaledOvershoot)
                                / 2.0
                        : (Math.pow(2.0 * progress - 2.0, 2.0)
                                                * ((scaledOvershoot + 1.0) * (progress * 2.0 - 2.0)
                                                        + scaledOvershoot)
                                        + 2.0)
                                / 2.0;
            }),
    EASE_IN_ELASTIC(
            progress -> {
                if (progress != 0.0 && progress != 1.0) {

                    double angularFrequency = 2.0 * readConstant("pi") / 3.0;
                    double amplitude = -Math.pow(2.0, 10.0 * progress - 10.0);
                    double scaledProgress = progress * 10.0;

                    return amplitude
                            * Math.sin(
                                    (scaledProgress - readConstant("ease_elastic_in_shift"))
                                            * angularFrequency);
                } else {
                    return progress;
                }
            }),
    EASE_OUT_ELASTIC(
            progress -> {
                if (progress != 0.0 && progress != 1.0) {

                    double angularFrequency = 2.0 * readConstant("pi") / 3.0;
                    double amplitude = Math.pow(2.0, -10.0 * progress);
                    double scaledProgress = progress * 10.0;

                    return amplitude
                                    * Math.sin(
                                            (scaledProgress
                                                            - readConstant(
                                                                    "ease_elastic_out_shift"))
                                                    * angularFrequency)
                            + 1.0;
                } else {
                    return progress;
                }
            }),
    EASE_IN_OUT_ELASTIC(
            progress -> {
                if (progress != 0.0 && progress != 1.0) {

                    double periodDivisor = readConstant("ease_elastic_inout_div");
                    if (periodDivisor == 0.0) {
                        return 0.0;
                    }

                    double angularFrequency = 2.0 * readConstant("pi") / periodDivisor;
                    double scaledProgress = 20.0 * progress;

                    double oscillation =
                            Math.sin(
                                    (scaledProgress - readConstant("ease_elastic_inout_shift"))
                                            * angularFrequency);
                    return progress < 0.5
                            ? -(Math.pow(2.0, 20.0 * progress - 10.0) * oscillation) / 2.0
                            : Math.pow(2.0, -20.0 * progress + 10.0) * oscillation / 2.0 + 1.0;
                } else {
                    return progress;
                }
            }),
    EASE_OUT_BOUNCE(
            progress -> {
                double bounceScale = readConstant("ease_bounce_n1");

                double bounceDivisor = readConstant("ease_bounce_d1");
                if (bounceDivisor == 0.0) {
                    return 0.0;
                } else if (progress < 1.0 / bounceDivisor) {
                    return bounceScale * progress * progress;
                } else if (progress < 2.0 / bounceDivisor) {
                    double secondBounceProgress = progress;

                    double secondBounceHeight =
                            bounceScale
                                    * Math.pow(
                                            secondBounceProgress
                                                    - readConstant("ease_bounce_p2")
                                                            / bounceDivisor,
                                            2.0);

                    return secondBounceHeight + readConstant("ease_bounce_o2");
                } else if (progress < 2.5 / bounceDivisor) {
                    double thirdBounceProgress = progress;

                    double thirdBounceHeight =
                            bounceScale
                                    * Math.pow(
                                            thirdBounceProgress
                                                    - readConstant("ease_bounce_p3")
                                                            / bounceDivisor,
                                            2.0);

                    return thirdBounceHeight + readConstant("ease_bounce_o3");
                } else {
                    double fourthBounceProgress = progress;

                    double fourthBounceHeight =
                            bounceScale
                                    * Math.pow(
                                            fourthBounceProgress
                                                    - readConstant("ease_bounce_p4")
                                                            / bounceDivisor,
                                            2.0);

                    return fourthBounceHeight + readConstant("ease_bounce_o4");
                }
            }),
    EASE_IN_BOUNCE(progress -> 1.0 - EASE_OUT_BOUNCE.function.apply(1.0 - progress)),
    EASE_IN_OUT_BOUNCE(
            progress ->
                    progress < 0.5
                            ? (1.0 - EASE_OUT_BOUNCE.function.apply(1.0 - 2.0 * progress)) / 2.0
                            : (1.0 + EASE_OUT_BOUNCE.function.apply(2.0 * progress - 1.0)) / 2.0);

    private final Function<Double, Double> function;

    Easing(Function<Double, Double> function) {
        this.function = function;
    }

    private static double readConstant(String constantName) {
        return MetadataState.getInstance().getDoubleParameter(constantName, 0.0);
    }

    /** Returns the formula associated with this easing variant. */
    public Function<Double, Double> getFunction() {
        return this.function;
    }
}
