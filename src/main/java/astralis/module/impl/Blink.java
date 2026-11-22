package astralis.module.impl;

import astralis.module.Module;

import pub.astralis.client.module.core.Category;

public class Blink extends Module {
    public Blink() {
        super(MODULE_ID, "", "", Category.Misc);
    }

    @Override
    public void onDisable() {
        if (astralis.util.Blink.isActive()) {
            astralis.util.Blink.stop();
        }
    }

    @Override
    public void onEnable() {
        if (!astralis.util.Blink.isActive()) {
            astralis.util.Blink.startOutgoing();
        }
    }

    private static final String MODULE_ID = "d425860306af3c4f85c83d8ef4d114b0";
}
