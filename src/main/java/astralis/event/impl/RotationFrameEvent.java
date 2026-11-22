package astralis.event.impl;

import astralis.event.Event;

import net.minecraft.world.entity.Entity;

/**
 * Posted each frame while a managed rotation is active; handlers may adjust the yaw and pitch for
 * that frame.
 */
public class RotationFrameEvent implements Event {
    private final float partialTick;
    private final float yaw;
    private final float pitch;
    private final float targetYaw;
    private final float targetPitch;
    private final String tag;
    private final Entity targetEntity;
    private float adjustedYaw;
    private float adjustedPitch;

    public RotationFrameEvent(
            float partialTick,
            float yaw,
            float pitch,
            float targetYaw,
            float targetPitch,
            String tag,
            Entity entity2) {
        super();
        this.partialTick = partialTick;
        this.yaw = yaw;
        this.pitch = pitch;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
        this.tag = tag;
        this.targetEntity = entity2;
        this.adjustedYaw = yaw;
        this.adjustedPitch = pitch;
    }

    public float getAdjustedYaw() {
        return this.adjustedYaw;
    }

    public Entity getTargetEntity() {
        return this.targetEntity;
    }

    public float getTargetPitch() {
        return this.targetPitch;
    }

    public void addAdjustment(float yawOffset, float pitchOffset) {
        this.adjustedYaw += yawOffset;
        this.adjustedPitch += pitchOffset;
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    public float getAdjustedPitch() {
        return this.adjustedPitch;
    }

    public float getPartialTick() {
        return this.partialTick;
    }

    public float getPitch() {
        return this.pitch;
    }

    public String getTag() {
        return this.tag;
    }

    public void setAdjustedRotation(float adjustedYaw, float adjustedPitch) {
        this.adjustedYaw = adjustedYaw;
        this.adjustedPitch = adjustedPitch;
    }

    public float getYaw() {
        return this.yaw;
    }
}
