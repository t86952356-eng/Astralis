package astralis.event.impl;

import astralis.event.CancellableEvent;

import lombok.Generated;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Posted when any entity moves via {@code Entity.move}; handlers may replace the movement, and
 * cancelling zeroes it.
 */
public class EntityMoveEvent extends CancellableEvent {
    private Entity entity;
    private Vec3 movement;

    @Generated
    public Entity getEntity() {
        return this.entity;
    }

    @Generated
    public Vec3 getMovement() {
        return this.movement;
    }

    @Generated
    public void setEntity(Entity entity2) {
        this.entity = entity2;
    }

    @Generated
    public void setMovement(Vec3 vector3) {
        this.movement = vector3;
    }

    @Generated
    public EntityMoveEvent(Entity entity2, Vec3 vector3) {
        this.entity = entity2;
        this.movement = vector3;
    }
}
