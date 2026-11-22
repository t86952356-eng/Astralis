package astralis.event.impl;

import astralis.event.CancellableEvent;

import net.minecraft.world.phys.Vec3;

public class WebSlowdownEvent extends CancellableEvent {
    private Vec3 multiplier;

    public WebSlowdownEvent(Vec3 vector3) {
        this.multiplier = vector3;
    }

    public Vec3 getMultiplier() {
        return this.multiplier;
    }

    public void setMultiplier(Vec3 vector3) {
        this.multiplier = vector3;
    }
}
