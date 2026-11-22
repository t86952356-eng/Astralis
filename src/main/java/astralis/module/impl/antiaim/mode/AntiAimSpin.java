package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;
import astralis.setting.Mode;

public class AntiAimSpin extends Mode<AntiAim> {

    public AntiAimSpin(AntiAim antiAim) {
        super("Spin", antiAim);
    }

    protected AntiAimSpin(String modeName, AntiAim antiAim) {
        super(modeName, antiAim);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            this.getParent()
                    .submitRotation(
                            this.getParent()
                                    .advanceYaw(this.getParent().getSpeedSetting().getValue()),
                            this.getParent().getTargetPitch());
        }
    }
}
