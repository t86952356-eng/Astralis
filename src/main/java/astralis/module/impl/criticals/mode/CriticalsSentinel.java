package astralis.module.impl.criticals.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.Criticals;
import astralis.setting.Mode;

public class CriticalsSentinel extends Mode<Criticals> {

    public CriticalsSentinel(Criticals criticals) {
        super("Sentinel", criticals);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {}
}
