package astralis.module.impl.combat.velocity;

import astralis.event.EventTarget;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.Velocity;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;

/** Scales event components by percentage; two zero percentages cancel the event. */
public class VelocityPacket extends Mode<Velocity> {
    private final NumberSetting horizontalPercent;
    private final NumberSetting verticalPercent;

    public VelocityPacket(Velocity parent) {

        super("Packet", parent);

        this.horizontalPercent =
                new NumberSetting(HORIZONTAL_PERCENT_SETTING_ID, 0.0F, 0.0F, 100.0F, 1.0F);

        this.verticalPercent =
                new NumberSetting(VERTICAL_PERCENT_SETTING_ID, 0.0F, 0.0F, 100.0F, 1.0F);
        this.addSettings(this.horizontalPercent, this.verticalPercent);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.getParent().shouldApply()) {
            float horizontalMultiplier = this.horizontalPercent.getValue() / 100.0F;
            float verticalMultiplier = this.verticalPercent.getValue() / 100.0F;
            if (horizontalMultiplier == 0.0F && verticalMultiplier == 0.0F) {
                event.setCancelled(true);
            } else {
                event.setX(event.getX() * horizontalMultiplier);
                event.setZ(event.getZ() * horizontalMultiplier);
                event.setY(event.getY() * verticalMultiplier);
            }
        }
    }

    private static final String HORIZONTAL_PERCENT_SETTING_ID = "c46ad021191c598a6acf1aeb6aecaa12";
    private static final String VERTICAL_PERCENT_SETTING_ID = "a879619dda1838e2c99b530e2709db0f";
}
