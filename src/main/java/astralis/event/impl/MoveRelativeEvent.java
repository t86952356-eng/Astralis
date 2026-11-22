package astralis.event.impl;

import astralis.event.EventStage;
import astralis.event.StagedEvent;

import lombok.Generated;

import net.minecraft.world.phys.Vec3;

/**
 * Posted for the local player before (PRE) and after (POST) {@code Entity.moveRelative};
 * cancelling skips the rest of that call.
 */
public class MoveRelativeEvent extends StagedEvent {
    private final Vec3 input;

    public MoveRelativeEvent(Vec3 vector3, EventStage stage) {
        super(stage);
        this.input = vector3;
    }

    @Generated
    public Vec3 getInput() {
        return this.input;
    }
}
