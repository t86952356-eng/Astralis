package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.module.Module;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.gameevent.GameEvent;

import pub.astralis.client.module.core.Category;

public class CrystalOptimizer extends Module {
    public CrystalOptimizer() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    private void removeCrystal(Entity entity2) {
        entity2.remove(RemovalReason.KILLED);
        entity2.gameEvent(GameEvent.ENTITY_DIE);
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.POST) {
            if (attackEvent.getTarget() instanceof EndCrystal endCrystal) {
                this.removeCrystal(endCrystal);
            }
        }
    }

    private static final String MODULE_ID =
            "5267f78e61354555f0fc68f03a52eac9c8da9162bde5b1f4280ff549af70a03d";
}
