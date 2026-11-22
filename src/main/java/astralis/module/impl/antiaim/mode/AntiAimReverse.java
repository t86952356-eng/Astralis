package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;

public class AntiAimReverse extends AntiAimSpin {
    public AntiAimReverse(AntiAim antiAim) {
        super("Reverse", antiAim);
    }

    @EventTarget
    @Override
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            this.getParent()
                    .submitRotation(
                            this.getParent()
                                    .advanceYaw(-this.getParent().getSpeedSetting().getValue()),
                            this.getParent().getTargetPitch());
        }
    }
}
