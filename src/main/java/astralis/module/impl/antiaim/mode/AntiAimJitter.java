package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;

public class AntiAimJitter extends AntiAimSpin {
    public AntiAimJitter(AntiAim antiAim) {
        super("Jitter", antiAim);
    }

    @EventTarget
    @Override
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            float yaw = this.getParent().advanceYaw(this.getParent().getSpeedSetting().getValue());
            float jitterOffset =
                    (mc.player.tickCount & 1) == 0
                            ? this.getParent().getJitterSetting().getValue()
                            : -this.getParent().getJitterSetting().getValue();
            this.getParent().submitRotation(yaw + jitterOffset, this.getParent().getTargetPitch());
        }
    }
}
