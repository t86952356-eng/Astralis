package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.speed.mode.SpeedLegit;
import astralis.module.impl.speed.mode.SpeedMiniBlox;
import astralis.module.impl.speed.mode.SpeedMotion;
import astralis.module.impl.speed.mode.SpeedSentinel;
import astralis.module.impl.speed.mode.SpeedVerus;
import astralis.module.impl.speed.mode.SpeedVulcan;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class Speed extends Module {
    private final BooleanSetting onlyWhileMoving;
    private final ModeSetting mode;

    public Speed() {

        super(MODULE_ID, "", "", Category.Misc);

        this.onlyWhileMoving = new BooleanSetting(ONLY_WHILE_MOVING_SETTING_ID, true);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new SpeedLegit(this))
                        .addValues(new SpeedMotion(this))
                        .addValues(new SpeedMiniBlox(this))
                        .addValues(new SpeedSentinel(this))
                        .addValues(new SpeedVerus(this))
                        .addValues(new SpeedVulcan(this));
        this.mode = modeSetting.setDefault("Legit");
        this.addSettings(this.mode, this.onlyWhileMoving);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public BooleanSetting getOnlyWhileMoving() {
        return this.onlyWhileMoving;
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    private static final String MODULE_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String ONLY_WHILE_MOVING_SETTING_ID =
            "310618532c9803c36cbff378c9e6eaf3777db141a70cd71d410bd5fda5304c1f";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
