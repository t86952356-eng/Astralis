package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.Module;

import pub.astralis.client.module.core.Category;

public class NoElytraStop extends Module {
    public NoElytraStop() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (mc.player.isFallFlying()) {
            motionEvent.setOnGround(false);
        }
    }

    private static final String MODULE_ID = "9aee851d77dffe2048a398a7787b8b88";
}
