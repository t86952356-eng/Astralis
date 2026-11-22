package astralis.rotation;

import astralis.event.impl.MouseUpdateEvent;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.util.Mth;

public final class PlayerViewSnapshot implements InstanceAccess {
    private boolean mouseTurnActive;
    private boolean applyingToPlayer;
    private float yaw;
    private float pitch;
    private float xBob;
    private float prevXBob;
    private float yBob;
    private float prevYBob;

    public void updateBobbing() {
        this.prevXBob = this.xBob;
        this.prevYBob = this.yBob;
        this.xBob = this.xBob + (this.pitch - this.xBob) * 0.5F;
        this.yBob = this.yBob + (this.yaw - this.yBob) * 0.5F;
    }

    public void resetOnExternalRotation() {
        if (!this.mouseTurnActive && !this.applyingToPlayer) {
            this.clearFlags();
        }
    }

    public void beginMouseTurn() {
        this.mouseTurnActive = true;
    }

    public void endMouseTurn() {
        this.mouseTurnActive = false;
    }

    public void captureFromPlayer() {
        this.yaw = mc.player.getYRot();
        this.pitch = mc.player.getXRot();
        this.xBob = mc.player.xBob;
        this.prevXBob = mc.player.xBobO;
        this.yBob = mc.player.yBob;
        this.prevYBob = mc.player.yBobO;
    }

    public float getYBob() {
        return mc.player != null ? this.yBob : 0.0F;
    }

    public float getPitch() {
        return mc.player != null ? this.pitch : 0.0F;
    }

    public float getXBob() {
        return mc.player != null ? this.xBob : 0.0F;
    }

    public float getYaw() {
        return mc.player != null ? this.yaw : 0.0F;
    }

    public float getPrevYBob() {
        return mc.player != null ? this.prevYBob : 0.0F;
    }

    public float getPrevXBob() {
        return mc.player != null ? this.prevXBob : 0.0F;
    }

    public void applyMouseDelta(MouseUpdateEvent event) {
        this.captureFromPlayer();
        this.yaw =
                this.yaw
                        + (float) (event.getOriginalDeltaX() * event.getSensitivityFactor() * 0.15F)
                                * (mc.options.invertMouseX().get() ? -1.0F : 1.0F);
        this.pitch =
                Math.clamp(
                        this.pitch
                                + (float)
                                                (event.getOriginalDeltaY()
                                                        * event.getSensitivityFactor()
                                                        * 0.15F)
                                        * (mc.options.invertMouseY().get() ? -1.0F : 1.0F),
                        -90.0F,
                        90.0F);
    }

    public void restoreToPlayer(float referenceYaw) {
        this.applyingToPlayer = true;

        try {
            float wrappedYaw = referenceYaw + Mth.wrapDegrees(this.yaw - referenceYaw);
            float yawOffset = wrappedYaw - this.yaw;
            this.yaw = wrappedYaw;
            this.yBob += yawOffset;
            this.prevYBob += yawOffset;
            mc.player.setYRot(this.yaw);
            mc.player.setXRot(this.pitch);
            mc.player.yRotO = this.yaw;
            mc.player.xRotO = this.pitch;
            mc.player.xBob = this.xBob;
            mc.player.xBobO = this.prevXBob;
            mc.player.yBob = this.yBob;
            mc.player.yBobO = this.prevYBob;
        } finally {
            this.applyingToPlayer = false;
        }
    }

    public void clearFlags() {
        this.mouseTurnActive = false;
        this.applyingToPlayer = false;
    }

    public void overrideRotation(
            float yaw, float pitch, boolean overrideYaw, boolean overridePitch) {
        if (overrideYaw) {
            this.yaw = yaw;
            this.yBob = yaw;
            this.prevYBob = yaw;
        }

        if (overridePitch) {
            this.pitch = pitch;
            this.xBob = pitch;
            this.prevXBob = pitch;
        }
    }
}
