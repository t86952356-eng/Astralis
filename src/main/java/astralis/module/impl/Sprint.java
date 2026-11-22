package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.sprint.mode.SprintLegit;
import astralis.module.impl.sprint.mode.SprintOmni;
import astralis.module.impl.sprint.mode.SprintRotation;
import astralis.setting.ModeSetting;

public class Sprint extends Module {
    private final ModeSetting mode;

    public Sprint() {

        super(MODULE_ID);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new SprintLegit(this))
                        .addValues(new SprintRotation(this))
                        .addValues(new SprintOmni(this));
        this.mode = modeSetting.setDefault("Legit");
        this.addSettings(this.mode);
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    private static final String MODULE_ID = "6badeead0dd64c9c74479c7b0856f22c";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
