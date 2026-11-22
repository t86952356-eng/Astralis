package astralis.module.impl.antiweb.mode;

import astralis.event.EventTarget;
import astralis.event.impl.WebSlowdownEvent;
import astralis.module.impl.AntiWeb;
import astralis.setting.Mode;

public class AntiWebVanilla extends Mode<AntiWeb> {

    public AntiWebVanilla(AntiWeb antiWeb) {
        super("Vanilla", antiWeb);
    }

    @EventTarget
    public void onWebSlowdown(WebSlowdownEvent webSlowdownEvent) {
        if (!mc.options.keyJump.isDown() && mc.options.keyShift.isDown()) {}

        webSlowdownEvent.setCancelled(true);
    }
}
