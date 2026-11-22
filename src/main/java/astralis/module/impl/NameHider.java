package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.TextSetting;

import pub.astralis.client.module.core.Category;

public class NameHider extends Module {
    private final TextSetting replacementName;

    public NameHider() {

        super(MODULE_ID, "", "", Category.Misc);

        this.replacementName = new TextSetting("Name", "09e30a46a99d26be314012b0d0834a65");
        this.addSetting(this.replacementName);
    }

    public TextSetting getReplacementName() {
        return this.replacementName;
    }

    private static final String MODULE_ID = "2ecbf844679d09a4c96a1a9f9ea15c5f";
}
