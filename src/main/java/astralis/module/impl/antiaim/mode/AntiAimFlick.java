package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;

public class AntiAimFlick extends AntiAimSpin {
    public AntiAimFlick(AntiAim antiAim) {
        super("Flick", antiAim);
    }

    @EventTarget
    @Override
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            this.getParent()
                    .submitRotation(
                            this.getParent()
                                    .advanceYaw(
                                            180.0F
                                                    + this.getParent()
                                                            .getJitterSetting()
                                                            .getValue()),
                            this.getParent().getTargetPitch());
        }
    }
}
