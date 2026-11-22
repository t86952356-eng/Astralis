package astralis.event.impl;

import astralis.event.EventStage;
import astralis.event.StagedEvent;

import net.minecraft.world.entity.Entity;

public class AttackEvent extends StagedEvent {
    private final Entity target;

    public AttackEvent(Entity entity2, EventStage stage) {
        super(stage);
        this.target = entity2;
    }

    public Entity getTarget() {
        return this.target;
    }
}
