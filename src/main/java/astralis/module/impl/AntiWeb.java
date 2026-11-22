package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.antiweb.mode.AntiWebGrim;
import astralis.module.impl.antiweb.mode.AntiWebVanilla;
import astralis.setting.ModeSetting;

import pub.astralis.client.module.core.Category;

public class AntiWeb extends Module {
    private final ModeSetting webMode;

    public AntiWeb() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new AntiWebGrim(this))
                        .addValues(new AntiWebVanilla(this));
        this.webMode = modeSetting.setDefault("Vanilla");
        this.addSetting(this.webMode);
    }

    @Override
    public String getSuffix() {
        return this.webMode.getValue().getName();
    }

    private static final String MODULE_ID = "a3eac9deea352bd887423718a4f19dd9";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
