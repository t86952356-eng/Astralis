package astralis.event.impl;

import astralis.event.Event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Posted after {@code MultiPlayerGameMode.destroyBlock} returns. */
public final class BlockDestroyedEvent implements Event {
    private final BlockPos position;
    private final BlockState state;

    public BlockDestroyedEvent(BlockPos blockPos, BlockState blockState) {
        this.position = blockPos;
        this.state = blockState;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!(object instanceof BlockDestroyedEvent other)) {
            return false;
        } else {
            BlockPos blockPos = this.getPosition();
            BlockPos blockPos2 = other.getPosition();
            if (blockPos == null ? blockPos2 == null : blockPos.equals(blockPos2)) {
                BlockState blockState = this.getState();
                BlockState blockState2 = other.getState();
                return blockState == null ? blockState2 == null : blockState.equals(blockState2);
            } else {
                return false;
            }
        }
    }

    @Override
    public String toString() {
        String positionText = String.valueOf(this.getPosition());
        String stateText = String.valueOf(this.getState());

        return "BlockDestroyedEvent(position=" + positionText + ", state=" + stateText + ")";
    }

    @Override
    public int hashCode() {

        int result;
        BlockPos blockPos = this.getPosition();

        int positionHash;
        if (blockPos == null) {

            positionHash = 43;
        } else {
            positionHash = blockPos.hashCode();
        }

        result = 59 + positionHash;
        BlockState blockState = this.getState();

        int scaledResult = result * 59;
        int stateHash;
        if (blockState == null) {

            stateHash = 43;
        } else {
            stateHash = blockState.hashCode();
        }

        result = scaledResult + stateHash;
        return result;
    }

    public BlockState getState() {
        return this.state;
    }

    public BlockPos getPosition() {
        return this.position;
    }
}
