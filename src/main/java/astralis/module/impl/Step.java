package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.step.mode.StepVanilla;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;

import pub.astralis.client.module.core.Category;

public class Step extends Module {
    private final ModeSetting mode;
    private final NumberSetting height;

    public Step() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this).addValues(new StepVanilla(this));
        this.mode = modeSetting.setDefault("Vanilla");

        this.height = new NumberSetting(HEIGHT_SETTING_ID, 1.0F, 0.5F, 5.0F, 0.5F);
        this.addSettings(this.mode, this.height);
    }

    public NumberSetting getHeight() {
        return this.height;
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    public ModeSetting getMode() {
        return this.mode;
    }

    private static final String MODULE_ID = "0d26729f69a08bb7f47e996247b95dc7";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String HEIGHT_SETTING_ID = "3b6faa77b4541c1932fe851f97facdf2";
}
