package astralis.module.impl.criticals.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.Criticals;
import astralis.setting.Mode;

public class CriticalsOffGround extends Mode<Criticals> {
    public CriticalsOffGround(Criticals criticals) {
        super("Off Ground", criticals);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (!this.getParent().shouldSkipCrit()) {
            if (mc.player.onGround()) {
                motionEvent.setOnGround(false);
            }
        }
    }
}
