package astralis.event.impl;

import astralis.event.Event;

import lombok.Generated;

import net.minecraft.world.entity.Entity;

/** Supplies the step height used by {@code Entity.collide}. */
public class StepHeightEvent implements Event {
    private Entity entity;
    private float stepHeight;

    @Generated
    public Entity getEntity() {
        return this.entity;
    }

    @Generated
    public float getStepHeight() {
        return this.stepHeight;
    }

    @Generated
    public void setEntity(Entity entity2) {
        this.entity = entity2;
    }

    @Generated
    public void setStepHeight(float stepHeight) {
        this.stepHeight = stepHeight;
    }

    @Generated
    public StepHeightEvent(Entity entity2, float stepHeight) {
        this.entity = entity2;
        this.stepHeight = stepHeight;
    }
}
