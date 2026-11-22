package astralis.module.impl.movement.noslow;

import astralis.event.EventTarget;
import astralis.event.impl.ItemUseSlowdownEvent;
import astralis.event.impl.ItemUseSprintEvent;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.Mode;

public class NoSlowVanilla extends Mode<NoSlowdown> {
    public NoSlowVanilla(NoSlowdown noSlowdown) {
        super("Vanilla", noSlowdown);
    }

    @EventTarget
    public void onItemUseSprint(ItemUseSprintEvent event) {
        if (this.parent.shouldNoSlow()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onItemUseSlowdown(ItemUseSlowdownEvent event) {
        if (this.parent.shouldNoSlow()) {
            event.setCancelled(true);
            event.setMultiplier(this.parent.getSlowdownMultiplier().getValue());
        }
    }
}
