package astralis.module.impl.beddefender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record PlacementTarget(float yaw, float pitch, BlockPos supportPos, Direction face) {
    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public BlockPos getSupportPos() {
        return this.supportPos;
    }

    public Direction getFace() {
        return this.face;
    }
}
