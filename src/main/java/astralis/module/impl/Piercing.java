package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.util.InventoryUtil;

import pub.astralis.client.module.core.Category;

public class Piercing extends Module {
    private final BooleanSetting weaponsOnly;

    public Piercing() {

        super(MODULE_ID, "", "", Category.Misc);

        this.weaponsOnly = new BooleanSetting(WEAPONS_ONLY_SETTING_ID, true);
        this.addSettings(this.weaponsOnly);
    }

    public BooleanSetting getWeaponsOnly() {
        return this.weaponsOnly;
    }

    public boolean canPierce() {
        return !this.weaponsOnly.getValue() ? true : InventoryUtil.isHoldingWeapon();
    }

    private static final String MODULE_ID = "c5c5937824272f941e67d01f34b6f201";
    private static final String WEAPONS_ONLY_SETTING_ID = "4eb1bdc15306b6e3ba66bf2f0f432243";
}
