package astralis.module.impl.movement.noslow;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.PlayerAiStepEvent;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.movement.KeepSprint;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.Mode;
import astralis.util.InventoryUtil;
import astralis.util.RotationUtil;

public final class NoSlowUniversal extends Mode<NoSlowdown> {
    public NoSlowUniversal(NoSlowdown noSlowdown) {
        super("Universal", noSlowdown);
    }

    public boolean isUsingOrBlocking() {
        if (mc.player == null) {
            return false;
        }

        if (mc.player.isUsingItem()) {
            return true;
        }

        if (!InventoryUtil.isHoldingSword()) {
            return false;
        }

        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        return mc.options.keyUse.isDown()
                || killAura != null && killAura.isEnabled() && killAura.isAutoBlocking();
    }

    @EventTarget
    public void onPlayerAiStep(PlayerAiStepEvent event) {
        if (this.isUsingOrBlocking()
                && mc.player.input.hasForwardImpulse()
                && !mc.player.input.keyPresses.backward()) {
            if (InventoryUtil.isHoldingSword()) {
                KillAura killAura =
                        Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                KeepSprint keepSprint =
                        Astralis.getInstance().getModuleManager().getModule(KeepSprint.class);
                if (killAura == null
                        || !killAura.isEnabled()
                        || killAura.target == null
                        || keepSprint == null
                        || !keepSprint.shouldKeepSprint()
                        || RotationUtil.distanceToBox(killAura.target)
                                > killAura.getAutoBlockRange().getValue().floatValue()) {
                    return;
                }
            }

            mc.player.setSprinting(true);
        }
    }
}
