package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.NoFallDamage;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.BreakUtil;

public class NoFallDamageSentinel extends Mode<NoFallDamage> {
    private final NumberSetting spoofGroundTicks;

    public NoFallDamageSentinel(NoFallDamage noFallDamage) {

        super("Sentinel", noFallDamage);

        this.spoofGroundTicks =
                new NumberSetting(
                        "bdab30e2930e43d7a791e9c1a50a2ff575f5d12ac6196a8f66b4a16733722abd",
                        1.0F,
                        1.0F,
                        10.0F,
                        1.0F);
        this.addSettings(this.spoofGroundTicks);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (mc.player.fallDistance > 2.5) {
            int interval = Math.max(1, this.spoofGroundTicks.getValue().intValue());
            if (mc.player.tickCount % interval == 0) {
                motionEvent.setY(motionEvent.getY() + 1.0E-7);
                motionEvent.setOnGround(true);
            }
        }

        double groundDistance = BreakUtil.getGroundDistance() + mc.player.getDeltaMovement().y;
        boolean landingImminent =
                mc.player.getDeltaMovement().y < -0.3
                        && groundDistance < 3.3
                        && mc.player.fallDistance > 3.3;
        if (landingImminent) {
            mc.player.fallDistance = 0.0;
            BreakUtil.setMotionY(0.01);
        }
    }
}
