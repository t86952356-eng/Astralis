package astralis.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record BlockData(BlockPos pos, Direction face) {
    public Direction getFace() {
        return this.face;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
