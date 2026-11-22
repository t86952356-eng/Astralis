package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.module.Module;

import net.minecraft.world.entity.LivingEntity;

import pub.astralis.client.module.core.Category;

public class CritAssist extends Module {
    private boolean suppressForward;

    public CritAssist() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    @Override
    public void onDisable() {
        this.suppressForward = false;
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.PRE) {
            this.suppressForward = false;
            if (!mc.player.onGround() && attackEvent.getTarget() instanceof LivingEntity) {
                Criticals criticals =
                        Astralis.getInstance().getModuleManager().getModule(Criticals.class);
                if (criticals.isFallingCritReady(true)) {
                    this.suppressForward = true;
                    if (mc.player.isSprinting()) {
                        attackEvent.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.suppressForward) {
            moveInputEvent.setForward(false);
            moveInputEvent.setSprint(false);
            this.suppressForward = false;
        }
    }

    private static final String MODULE_ID = "2abbe63e55876471d80998079827d3d6";
}
