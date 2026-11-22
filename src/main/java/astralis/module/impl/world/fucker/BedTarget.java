package astralis.module.impl.world.fucker;

import net.minecraft.core.BlockPos;

/**
 * A bed-breaking plan: the first block to break, an optional instant-mode follow-up, and the bed
 * whose validity is checked throughout the operation.
 *
 * @param breakPos the bed itself or its cover block
 * @param followUpPos the bed to break after the cover, or null for a single-block plan
 * @param bedPos the bed position retained for validation and ghost-block recovery
 */
public record BedTarget(BlockPos breakPos, BlockPos followUpPos, BlockPos bedPos) {}
