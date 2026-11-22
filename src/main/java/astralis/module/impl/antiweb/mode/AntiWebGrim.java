package astralis.module.impl.antiweb.mode;

import astralis.event.EventTarget;
import astralis.event.impl.WebSlowdownEvent;
import astralis.module.impl.AntiWeb;
import astralis.setting.Mode;
import astralis.util.MoveUtil;

public class AntiWebGrim extends Mode<AntiWeb> {
    public AntiWebGrim(AntiWeb antiWeb) {
        super("Grim", antiWeb);
    }

    @EventTarget
    public void onWebSlowdown(WebSlowdownEvent webSlowdownEvent) {
        if (mc.options.keyJump.isDown() || mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x,
                    mc.options.keyJump.isDown() ? 1.1 : -3.28,
                    mc.player.getDeltaMovement().z);
        }

        MoveUtil.setSpeed(0.644);
    }
}
