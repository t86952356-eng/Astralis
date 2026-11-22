package astralis.module.impl.combat.velocity;

import astralis.event.EventTarget;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.Velocity;
import astralis.setting.Mode;

/** Applies a fixed horizontal multiplier while retaining vertical velocity. */
public class VelocityMatrix extends Mode<Velocity> {
    public VelocityMatrix(Velocity parent) {
        super("Matrix", parent);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.getParent().shouldApply()) {
            event.setX(event.getX() * 0.6);
            event.setZ(event.getZ() * 0.6);
        }
    }
}
