package astralis.module.impl.airstuck.mode;

import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.impl.AirStuck;
import astralis.setting.Mode;

public class AirStuckUpdate extends Mode<AirStuck> {
    public AirStuckUpdate(AirStuck airStuck) {
        super("Update", airStuck);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!event.isCancelled()) {
            event.setCancelled(true);
        }
    }
}
