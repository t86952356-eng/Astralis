package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Posted with the collision shape returned by {@code BlockBehaviour.getCollisionShape}; cancelling
 * (with the stored shape) replaces it.
 */
public class BlockCollisionShapeEvent implements Event {
    public BlockState blockState;
    public BlockPos blockPos;
    public VoxelShape shape;

    @Generated
    public BlockState getBlockState() {
        return this.blockState;
    }

    @Generated
    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    @Generated
    public VoxelShape getShape() {
        return this.shape;
    }

    @Generated
    public void setBlockState(BlockState blockState) {
        this.blockState = blockState;
    }

    @Generated
    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    @Generated
    public void setShape(VoxelShape voxelShape) {
        this.shape = voxelShape;
    }

    @Generated
    public BlockCollisionShapeEvent(
            BlockState blockState, BlockPos blockPos, VoxelShape voxelShape) {
        super();
        this.blockState = blockState;
        this.blockPos = blockPos;
        this.shape = voxelShape;
    }
}
