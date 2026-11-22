package astralis.module.impl.anticheat;

import net.minecraft.core.BlockPos;

/**
 * Inferred scaffold placement sample. Component names and generated record methods remain stable.
 *
 * @param j block directly beneath the observed player
 * @param previousBlock estimated preceding block, opposite the current horizontal movement
 */
public record ScaffoldPlacementSample(BlockPos underfootBlock, BlockPos previousBlock) {
    /** Returns the estimated preceding block used to link adjacent placement samples. */
    public BlockPos getPreviousBlock() {
        return this.previousBlock;
    }
}
