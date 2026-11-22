package astralis.util;

import net.minecraft.world.phys.Vec3;

public record PlaceRotation(float yaw, float pitch, Vec3 hitVec) {
    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public Vec3 getHitVec() {
        return this.hitVec;
    }
}
