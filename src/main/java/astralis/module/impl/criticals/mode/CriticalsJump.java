package astralis.module.impl.criticals.mode;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.module.impl.Criticals;
import astralis.module.impl.combat.KillAura;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.BreakUtil;
import astralis.util.RotationUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class CriticalsJump extends Mode<Criticals> {
    private final NumberSetting range;

    public CriticalsJump(Criticals criticals) {

        super("Jump", criticals);

        this.range = new NumberSetting("4ed84ffdb32f200ee53f8c90db3cab6d", 4.0F, 1.0F, 6.0F, 0.1F);
        this.addSettings(this.range);
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (!this.getParent().shouldSkipCrit()
                && this.getParent().canCrit(true)
                && mc.player.onGround()) {
            if (!this.getParent().isJumpAlignedWithCooldown()) {
                if (this.hasTargetInRange()) {
                    moveInputEvent.setJump(true);
                }
            }
        }
    }

    private boolean hasTargetInRange() {
        Criticals criticals = this.getParent();
        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        if (criticals.isRequireKaTarget()) {
            LivingEntity livingEntity = killAura.target;
            return livingEntity != null
                    && !livingEntity.isDeadOrDying()
                    && RotationUtil.distanceToBox(livingEntity)
                            <= this.range.getValue().floatValue()
                    && mc.player.hasLineOfSight(livingEntity);
        } else {
            List targets =
                    BreakUtil.getTargets(
                            false,
                            true,
                            false,
                            false,
                            false,
                            this.range.getValue(),
                            TargetSortOrder.DISTANCE,
                            true);
            return targets != null && !targets.isEmpty();
        }
    }
}
