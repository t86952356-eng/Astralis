package astralis.module.impl.combat.velocity;

import astralis.event.EventTarget;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.Velocity;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;

/** Reverses horizontal velocity by a negative percentage; zero cancels the event. */
public class VelocityReverse extends Mode<Velocity> {
    private final NumberSetting horizontalPercent;

    public VelocityReverse(Velocity parent) {

        super("Reverse", parent);

        this.horizontalPercent =
                new NumberSetting(HORIZONTAL_PERCENT_SETTING_ID, -100.0F, -100.0F, 0.0F, 1.0F);
        this.addSettings(this.horizontalPercent);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.getParent().shouldApply()) {
            float horizontalMultiplier = this.horizontalPercent.getValue() / 100.0F;
            if (horizontalMultiplier == 0.0F) {
                event.setCancelled(true);
            } else {
                event.setX(event.getX() * horizontalMultiplier);
                event.setZ(event.getZ() * horizontalMultiplier);
            }
        }
    }

    private static final String HORIZONTAL_PERCENT_SETTING_ID = "c46ad021191c598a6acf1aeb6aecaa12";
}
