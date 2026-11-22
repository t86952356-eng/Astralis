package astralis.module.impl.beddefender;

import net.minecraft.core.BlockPos;

public record PlacementCandidate(
        DefenseBlock block, BlockPos position, int bedDistance, double distanceSq) {
    public DefenseBlock getBlock() {
        return this.block;
    }

    public BlockPos getPosition() {
        return this.position;
    }

    public int getBedDistance() {
        return this.bedDistance;
    }

    public double getDistanceSq() {
        return this.distanceSq;
    }
}
