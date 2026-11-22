package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.Setting;
import astralis.setting.TextSetting;

import pub.astralis.client.module.core.Category;

public class ServerSpoofer extends Module {
    private final Setting<Boolean> preventFingerprinting;
    private final Setting<Boolean> spoofClientBrand;
    private final Setting<String> brand;

    public ServerSpoofer() {

        super(MODULE_ID, "", "", Category.Misc);

        this.preventFingerprinting = new BooleanSetting(PREVENT_FINGERPRINTING_SETTING_ID, true);

        this.spoofClientBrand = new BooleanSetting(SPOOF_CLIENT_BRAND_SETTING_ID, false);

        this.brand =
                new TextSetting(
                        "Brand",
                        "adab04ac5e6be8c962706a4c3a5c77e2f1eebcd8b709972cdc4859b3faf35fb8");
        this.brand.hideIf(
                () -> {
                    return !this.spoofClientBrand.getValue();
                });
        this.addSettings(this.preventFingerprinting, this.spoofClientBrand, this.brand);
    }

    public Setting<Boolean> getSpoofClientBrand() {
        return this.spoofClientBrand;
    }

    public Setting<String> getBrand() {
        return this.brand;
    }

    public Setting<Boolean> getPreventFingerprinting() {
        return this.preventFingerprinting;
    }

    private static final String MODULE_ID = "8e28bc172ecc1e2d6b76edfebc062c7b";
    private static final String PREVENT_FINGERPRINTING_SETTING_ID =
            "93faee94e6c977f6121e6789a63d1c78045668842b44d746125085485175a4fb";
    private static final String SPOOF_CLIENT_BRAND_SETTING_ID =
            "d8ee3c9fa92c01713b5aae2c9197cf4f71188f996a9c13cab6311dab1f3bca92";
}
