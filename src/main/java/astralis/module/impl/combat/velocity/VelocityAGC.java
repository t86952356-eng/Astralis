package astralis.module.impl.combat.velocity;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.combat.Velocity;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;

/** Scales horizontal motion on PRE ticks while hurt and airborne. */
public class VelocityAGC extends Mode<Velocity> {
    private final NumberSetting horizontalMultiplier;

    public VelocityAGC(Velocity parent) {

        super("AGC", parent);

        this.horizontalMultiplier =
                new NumberSetting(HORIZONTAL_MULTIPLIER_SETTING_ID, 0.8F, 0.75F, 1.0F, 0.01F);
        this.addSettings(this.horizontalMultiplier);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getStage() == EventStage.PRE
                && this.getParent().shouldApply()
                && mc.player.hurtTime > 0
                && !mc.player.onGround()) {
            mc.player.setDeltaMovement(
                    mc.player.getDeltaMovement().x
                            * this.horizontalMultiplier.getValue().doubleValue(),
                    mc.player.getDeltaMovement().y,
                    mc.player.getDeltaMovement().z
                            * this.horizontalMultiplier.getValue().doubleValue());
        }
    }

    private static final String HORIZONTAL_MULTIPLIER_SETTING_ID =
            "bcd670cc478f9ceec389d4be874576f1";
}
