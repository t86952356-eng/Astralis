package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.ModuleManager;
import astralis.setting.BooleanSetting;

import pub.astralis.client.module.core.Category;

public class IRC extends Module {
    private final BooleanSetting pingSound;

    public IRC() {

        super(MODULE_ID, "", "", Category.Misc);

        this.pingSound = new BooleanSetting(PING_SOUND_SETTING_ID, true);
        this.setEnabled(true);
        this.addSetting(this.pingSound);
    }

    public BooleanSetting getPingSound() {
        return this.pingSound;
    }

    public static boolean isPingSoundEnabled() {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (moduleManager == null) {
            return true;
        }

        IRC irc = moduleManager.getModule(IRC.class);
        return irc == null || irc.pingSound.getValue();
    }

    private static final String MODULE_ID = "a3e5358924ead53ac8caddc8db9dd6d8";
    private static final String PING_SOUND_SETTING_ID = "d218edd7a9c5f68e68c333b30cb5cc92";
}
