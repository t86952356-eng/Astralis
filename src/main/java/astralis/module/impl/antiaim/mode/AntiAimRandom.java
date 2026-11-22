package astralis.module.impl.antiaim.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AntiAim;

import net.minecraft.util.Mth;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAimRandom extends AntiAimSpin {
    public AntiAimRandom(AntiAim antiAim) {
        super("Random", antiAim);
    }

    @EventTarget
    @Override
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            this.getParent()
                    .submitRotation(
                            Mth.wrapDegrees(ThreadLocalRandom.current().nextFloat(-180.0F, 180.0F)),
                            this.getParent().getTargetPitch());
        }
    }
}
