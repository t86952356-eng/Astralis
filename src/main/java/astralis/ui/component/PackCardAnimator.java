package astralis.ui.component;

public final class PackCardAnimator {
    private final long staggerDelayMs;
    private long startTimeMs;
    private long lastUpdateMs;
    private long textureReadyMs;
    private boolean started;
    private boolean textureWasReady;
    private float hoverProgress;

    PackCardAnimator(int staggerIndex) {
        super();
        this.staggerDelayMs = Math.clamp(staggerIndex, 0, 5) * 20L;
    }

    PackCardAnimationState advance(long nowMs, boolean hovered, boolean textureReady) {
        if (!this.started) {
            this.startTimeMs = nowMs;
            this.lastUpdateMs = nowMs;
            this.started = true;
        }

        float hoverStep = 1.0F - (float) Math.exp(-Math.max(0L, nowMs - this.lastUpdateMs) / 90.0);
        this.hoverProgress =
                this.hoverProgress + ((hovered ? 1.0F : 0.0F) - this.hoverProgress) * hoverStep;
        this.lastUpdateMs = nowMs;
        if (textureReady && !this.textureWasReady) {
            this.textureReadyMs = nowMs;
        }

        this.textureWasReady = textureReady;
        return new PackCardAnimationState(
                smoothstep((float) (nowMs - this.startTimeMs - this.staggerDelayMs) / 240.0F),
                textureReady ? smoothstep((float) (nowMs - this.textureReadyMs) / 280.0F) : 0.0F,
                this.hoverProgress);
    }

    private static float smoothstep(float value) {
        float clamped = Math.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
