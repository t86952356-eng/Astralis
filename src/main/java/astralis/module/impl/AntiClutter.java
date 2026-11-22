package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import pub.astralis.client.module.core.Category;

public class AntiClutter extends Module {
    public final BooleanSetting blindness;
    public final BooleanSetting darkness;
    public final BooleanSetting nausea;
    public final BooleanSetting fireOverlay;
    public final NumberSetting fireYOffset;
    public final BooleanSetting portalOverlay;
    public final BooleanSetting fluidOverlay;
    public final BooleanSetting inWallOverlay;
    public final BooleanSetting vignetteShadow;
    public final BooleanSetting totemAnimation;
    public final BooleanSetting chatToast;
    public final BooleanSetting crosshair;
    public final BooleanSetting bossBar;
    public final BooleanSetting statusEffectsHud;

    public AntiClutter() {

        super(MODULE_ID, "", "", Category.Misc);

        this.blindness = new BooleanSetting(BLINDNESS_SETTING_ID, true);

        this.darkness = new BooleanSetting(DARKNESS_SETTING_ID, true);

        this.nausea = new BooleanSetting(NAUSEA_SETTING_ID, true);

        this.fireOverlay = new BooleanSetting(FIRE_OVERLAY_SETTING_ID, false);

        this.fireYOffset = new NumberSetting(FIRE_Y_OFFSET_SETTING_ID, 0.0F, -0.4F, 0.4F, 0.02F);

        this.portalOverlay = new BooleanSetting(PORTAL_OVERLAY_SETTING_ID, true);

        this.fluidOverlay = new BooleanSetting(FLUID_OVERLAY_SETTING_ID, true);

        this.inWallOverlay = new BooleanSetting(IN_WALL_OVERLAY_SETTING_ID, true);

        this.vignetteShadow = new BooleanSetting(VIGNETTE_SHADOW_SETTING_ID, true);

        this.totemAnimation = new BooleanSetting(TOTEM_ANIMATION_SETTING_ID, false);

        this.chatToast = new BooleanSetting(CHAT_TOAST_SETTING_ID, true);

        this.crosshair = new BooleanSetting(CROSSHAIR_SETTING_ID, false);

        this.bossBar = new BooleanSetting(BOSS_BAR_SETTING_ID, false);

        this.statusEffectsHud = new BooleanSetting(STATUS_EFFECTS_HUD_SETTING_ID, false);
        SettingGroup settingGroup = new SettingGroup(REMOVALS_HUD_GROUP_ID);

        Setting[] removalSettings =
                new Setting[] {
                    this.fireOverlay,
                    this.fireYOffset,
                    this.portalOverlay,
                    this.fluidOverlay,
                    this.inWallOverlay,
                    this.vignetteShadow,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        removalSettings[6] = this.totemAnimation;

        removalSettings[7] = this.chatToast;

        removalSettings[8] = this.crosshair;

        removalSettings[9] = this.bossBar;

        removalSettings[10] = this.statusEffectsHud;
        SettingGroup settingGroup2 = settingGroup.addValues(removalSettings);
        SettingGroup settingGroup3 =
                new SettingGroup(REMOVALS_EFFECTS_GROUP_ID)
                        .addValues(this.blindness, this.darkness, this.nausea);
        this.addSettings(settingGroup3, settingGroup2);
    }

    private static final String MODULE_ID = "ea08d367b28b0eecb2c894f4adac8786";
    private static final String BLINDNESS_SETTING_ID = "a19f61666ddc4901e69fc06e8eb4fc23";
    private static final String DARKNESS_SETTING_ID = "d8d45f47a1b8be404acdfc3dc3d377b0";
    private static final String NAUSEA_SETTING_ID = "b0ab48b3cc556b40487b39c11aedc04b";
    private static final String FIRE_OVERLAY_SETTING_ID = "57ed2b8c634f30d816f4f6c428a79f73";
    private static final String FIRE_Y_OFFSET_SETTING_ID = "85efc08396cfa2fc716d446f99e2863e";
    private static final String PORTAL_OVERLAY_SETTING_ID = "afa0cc7a359d117cb3a38225b41fdd44";
    private static final String FLUID_OVERLAY_SETTING_ID = "bf32b653d3ae68fb51b4e7a5476b8e31";
    private static final String IN_WALL_OVERLAY_SETTING_ID = "04df323cd86ea79ca9a98e9cfa96c2fa";
    private static final String VIGNETTE_SHADOW_SETTING_ID = "d78b7764af609b87c2ecca5a8c83b22b";
    private static final String TOTEM_ANIMATION_SETTING_ID = "547921747df2593f8c386da6bfe766cb";
    private static final String CHAT_TOAST_SETTING_ID = "87b228146f0a4988c61dc506daffb15e";
    private static final String CROSSHAIR_SETTING_ID = "1e89da791bcdb8643a65e580512835fe";
    private static final String BOSS_BAR_SETTING_ID = "3d544e071069fb8a7205f7515a71a50a";
    private static final String STATUS_EFFECTS_HUD_SETTING_ID =
            "f88502ef22201ba27477e16d16116b1ef57d689add095477864064ce2c52faad";
    private static final String REMOVALS_HUD_GROUP_ID = "c4f7b852c349d679d03699b26c0ee78a";
    private static final String REMOVALS_EFFECTS_GROUP_ID =
            "73ae6fd0c9ee6920c47906bf28ad3b854610618ad961a480911eed4ec5414d29";
}
