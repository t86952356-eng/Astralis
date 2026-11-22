package astralis.event.impl;

import astralis.event.CancellableEvent;

import lombok.Generated;

import net.minecraft.world.phys.Vec3;

/**
 * Posted after the camera aligns with its entity; cancelling applies the event's camera position
 * and rotation.
 */
public class CameraSetupEvent extends CancellableEvent {
    private final float partialTick;
    private final double eyeX;
    private final double eyeY;
    private final double eyeZ;
    private final float yaw;
    private final float pitch;
    private final boolean mirrored;
    private Vec3 cameraPosition;
    private float cameraYaw;
    private float cameraPitch;

    @Generated
    public float getPartialTick() {
        return this.partialTick;
    }

    @Generated
    public double getEyeX() {
        return this.eyeX;
    }

    @Generated
    public double getEyeY() {
        return this.eyeY;
    }

    @Generated
    public double getEyeZ() {
        return this.eyeZ;
    }

    @Generated
    public float getYaw() {
        return this.yaw;
    }

    @Generated
    public float getPitch() {
        return this.pitch;
    }

    @Generated
    public boolean isMirrored() {
        return this.mirrored;
    }

    @Generated
    public Vec3 getCameraPosition() {
        return this.cameraPosition;
    }

    @Generated
    public float getCameraYaw() {
        return this.cameraYaw;
    }

    @Generated
    public float getCameraPitch() {
        return this.cameraPitch;
    }

    @Generated
    public void setCameraPosition(Vec3 vector3) {
        this.cameraPosition = vector3;
    }

    @Generated
    public void setCameraYaw(float cameraYaw) {
        this.cameraYaw = cameraYaw;
    }

    @Generated
    public void setCameraPitch(float cameraPitch) {
        this.cameraPitch = cameraPitch;
    }

    @Generated
    public CameraSetupEvent(
            float partialTick,
            double eyeX,
            double eyeY,
            double eyeZ,
            float yaw,
            float pitch,
            boolean mirrored,
            Vec3 vector3,
            float cameraYaw,
            float cameraPitch) {
        super();
        this.partialTick = partialTick;
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.mirrored = mirrored;
        this.cameraPosition = vector3;
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
    }
}
