package astralis.module.impl.movement.noslow;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.ItemUseSlowdownEvent;
import astralis.event.impl.ItemUseSprintEvent;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.Mode;
import astralis.util.InventoryUtil;

public final class NoSlowMiniBlox extends Mode<NoSlowdown> {
    public NoSlowMiniBlox(NoSlowdown noSlowdown) {
        super("MiniBlox", noSlowdown);
    }

    @EventTarget
    public void onItemUseSlowdown(ItemUseSlowdownEvent event) {
        if (this.computeBlocking()) {
            event.setCancelled(true);
            event.setMultiplier(0.0F);
        }
    }

    public boolean isBlocking() {
        return this.computeBlocking();
    }

    private boolean computeBlocking() {
        if (!InventoryUtil.isHoldingSword()) {
            return false;
        }

        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        return mc.player.isUsingItem()
                || mc.options.keyUse.isDown()
                || killAura.isEnabled() && killAura.isAutoBlocking();
    }

    @EventTarget
    public void onItemUseSprint(ItemUseSprintEvent event) {
        if (this.computeBlocking()) {
            event.setCancelled(true);
        }
    }
}
