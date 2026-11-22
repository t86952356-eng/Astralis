package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;

public class AntiAimSwitch extends AntiAimSpin {
    private int ticksSinceSwitch;
    private float directionSign;

    public AntiAimSwitch(AntiAim antiAim) {
        super("Switch", antiAim);
        this.directionSign = 1.0F;
    }

    @EventTarget
    @Override
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (++this.ticksSinceSwitch
                    >= this.getParent().getSwitchDelaySetting().getValue().intValue()) {
                this.ticksSinceSwitch = 0;
                this.directionSign = -this.directionSign;
            }

            this.getParent()
                    .submitRotation(
                            this.getParent()
                                    .advanceYaw(
                                            this.getParent().getSpeedSetting().getValue()
                                                    * this.directionSign),
                            this.getParent().getTargetPitch());
        }
    }

    @Override
    public void onEnable() {
        this.ticksSinceSwitch = 0;
        this.directionSign = 1.0F;
    }
}
