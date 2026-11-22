package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Posted before a block entity's render state is extracted, with its block position. */
public class BlockEntityRenderEvent implements Event {
    private final BlockEntity blockEntity;
    private Vec3 position;

    @Generated
    public BlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    @Generated
    public Vec3 getPosition() {
        return this.position;
    }

    @Generated
    public void setPosition(Vec3 vector3) {
        this.position = vector3;
    }

    @Generated
    public BlockEntityRenderEvent(BlockEntity blockEntity, Vec3 vector3) {
        super();
        this.blockEntity = blockEntity;
        this.position = vector3;
    }
}
