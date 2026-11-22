package astralis.module.impl.movement;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.VelocityEvent;
import astralis.module.Module;
import astralis.module.impl.movement.keepsprint.KeepSprintUniversal;
import astralis.module.impl.movement.keepsprint.KeepSprintVanilla;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

/** Selects sprint preservation and optionally schedules one sprint stop after knockback. */
public class KeepSprint extends Module {
    private final ModeSetting mode;
    private final BooleanSetting stopSprintingOnKnockback;
    private boolean pendingSprintStop;

    public KeepSprint() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new KeepSprintVanilla(this))
                        .addValues(new KeepSprintUniversal(this));
        this.mode = modeSetting.setDefault("Vanilla");

        this.stopSprintingOnKnockback =
                new BooleanSetting(STOP_SPRINTING_ON_KNOCKBACK_SETTING_ID, false);
        this.addSettings(this.mode, this.stopSprintingOnKnockback);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.stopSprintingOnKnockback.getValue() && !event.isExplosion()) {
            this.pendingSprintStop = true;
        }
    }

    public boolean shouldKeepSprint() {
        if (!this.isEnabled()) {
            return false;
        }

        return this.mode.is("Universal")
                ? true
                : ((KeepSprintVanilla) this.mode.getValue()).shouldKeepSprint();
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    @Override
    public void onDisable() {
        this.pendingSprintStop = false;
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent event) {
        // Consume the stop once on the next movement-input event.
        if (this.pendingSprintStop) {
            event.setSprint(false);
            this.pendingSprintStop = false;
        }
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    public BooleanSetting getStopSprintingOnKB() {
        return this.stopSprintingOnKnockback;
    }

    private static final String MODULE_ID = "ef1f1d77b42c8f7a1addf89f36bb3fd3";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String STOP_SPRINTING_ON_KNOCKBACK_SETTING_ID =
            "6681cc24b77a5aa5c805845c0a5bd45da9fa5334edfcddcd5cde5732b101f2f7";
}
