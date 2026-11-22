package astralis.module.impl.combat.velocity;

import astralis.event.EventTarget;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.Velocity;
import astralis.setting.Mode;

/** Cancels incoming velocity events when the parent environment checks allow it. */
public class VelocityCancel extends Mode<Velocity> {
    public VelocityCancel(Velocity parent) {
        super("Cancel", parent);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.getParent().shouldApply()) {
            event.setCancelled(true);
        }
    }
}
