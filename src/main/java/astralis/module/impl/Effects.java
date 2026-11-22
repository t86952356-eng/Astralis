package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import pub.astralis.client.module.core.Category;

public class Effects extends Module {
    public final BooleanSetting shadow;
    public final NumberSetting shadowIterations;
    public final NumberSetting shadowAlpha;
    public final NumberSetting shadowDownsampleFactor;
    public final BooleanSetting blur;
    public final NumberSetting blurIterations;
    public final NumberSetting blurDownsampleFactor;
    public final BooleanSetting arrayListBlur;
    public final BooleanSetting arrayListShadow;
    public final SettingGroup arrayListGroup;
    public final BooleanSetting watermarkBlur;
    public final BooleanSetting watermarkShadow;
    public final SettingGroup watermarkGroup;
    public final BooleanSetting targetHudBlur;
    public final BooleanSetting targetHudShadow;
    public final SettingGroup targetHudGroup;
    public final BooleanSetting sessionStatsBlur;
    public final BooleanSetting sessionStatsShadow;
    public final SettingGroup sessionStatsGroup;
    public final BooleanSetting scoreboardBlur;
    public final BooleanSetting scoreboardShadow;
    public final SettingGroup scoreboardGroup;
    public final BooleanSetting nameTagsBlur;
    public final BooleanSetting nameTagsShadow;
    public final SettingGroup nameTagsGroup;
    public final BooleanSetting tabGuiBlur;
    public final BooleanSetting tabGuiShadow;
    public final SettingGroup tabGuiGroup;
    public final BooleanSetting partyHudBlur;
    public final BooleanSetting partyHudShadow;
    public final SettingGroup partyHudGroup;
    public final BooleanSetting musicViewerBlur;
    public final BooleanSetting musicViewerShadow;
    public final SettingGroup musicViewerGroup;
    public final BooleanSetting chatTabsBlur;
    public final BooleanSetting chatTabsShadow;
    public final SettingGroup chatTabsGroup;
    public final BooleanSetting bedNukerBlur;
    public final BooleanSetting bedNukerShadow;
    public final SettingGroup bedNukerGroup;
    public final SettingGroup hudElements;

    public Effects() {

        super(MODULE_ID, "", "", Category.Misc);

        this.shadow = new BooleanSetting(SHADOW_SETTING_ID, false);

        this.shadowIterations =
                new NumberSetting(SHADOW_ITERATIONS_SETTING_ID, 10.0F, 1.0F, 20.0F, 1.0F);

        this.shadowAlpha = new NumberSetting(SHADOW_ALPHA_SETTING_ID, 225.0F, 1.0F, 225.0F, 1.0F);

        this.shadowDownsampleFactor =
                new NumberSetting(SHADOW_DOWNSAMPLE_FACTOR_SETTING_ID, 0.5F, 0.1F, 1.0F, 0.05F);

        this.blur = new BooleanSetting(BLUR_SETTING_ID, false);

        this.blurIterations =
                new NumberSetting(BLUR_ITERATIONS_SETTING_ID, 10.0F, 1.0F, 20.0F, 1.0F);

        this.blurDownsampleFactor =
                new NumberSetting(BLUR_DOWNSAMPLE_FACTOR_SETTING_ID, 0.75F, 0.1F, 1.0F, 0.05F);

        this.arrayListBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.arrayListShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.arrayListGroup =
                new SettingGroup(ARRAY_LIST_GROUP_ID)
                        .addValues(this.arrayListBlur, this.arrayListShadow)
                        .collapsed();

        this.watermarkBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.watermarkShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.watermarkGroup =
                new SettingGroup(WATERMARK_GROUP_ID)
                        .addValues(this.watermarkBlur, this.watermarkShadow)
                        .collapsed();

        this.targetHudBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.targetHudShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.targetHudGroup =
                new SettingGroup(TARGET_HUD_GROUP_ID)
                        .addValues(this.targetHudBlur, this.targetHudShadow)
                        .collapsed();

        this.sessionStatsBlur = new BooleanSetting(BLUR_SETTING_ID, false);

        this.sessionStatsShadow = new BooleanSetting(SHADOW_SETTING_ID, false);

        this.sessionStatsGroup =
                new SettingGroup(SESSION_STATS_GROUP_ID)
                        .addValues(this.sessionStatsBlur, this.sessionStatsShadow)
                        .collapsed();

        this.scoreboardBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.scoreboardShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.scoreboardGroup =
                new SettingGroup(SCOREBOARD_GROUP_ID)
                        .addValues(this.scoreboardBlur, this.scoreboardShadow)
                        .collapsed();

        this.nameTagsBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.nameTagsShadow = new BooleanSetting(SHADOW_SETTING_ID, false);

        this.nameTagsGroup =
                new SettingGroup(NAME_TAGS_GROUP_ID)
                        .addValues(this.nameTagsBlur, this.nameTagsShadow)
                        .collapsed();

        this.tabGuiBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.tabGuiShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.tabGuiGroup =
                new SettingGroup(TAB_GUI_GROUP_ID)
                        .addValues(this.tabGuiBlur, this.tabGuiShadow)
                        .collapsed();

        this.partyHudBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.partyHudShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.partyHudGroup =
                new SettingGroup(PARTY_HUD_GROUP_ID)
                        .addValues(this.partyHudBlur, this.partyHudShadow)
                        .collapsed();

        this.musicViewerBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.musicViewerShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.musicViewerGroup =
                new SettingGroup(MUSIC_VIEWER_GROUP_ID)
                        .addValues(this.musicViewerBlur, this.musicViewerShadow)
                        .collapsed();

        this.chatTabsBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.chatTabsShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.chatTabsGroup =
                new SettingGroup(CHAT_TABS_GROUP_ID)
                        .addValues(this.chatTabsBlur, this.chatTabsShadow)
                        .collapsed();

        this.bedNukerBlur = new BooleanSetting(BLUR_SETTING_ID, true);

        this.bedNukerShadow = new BooleanSetting(SHADOW_SETTING_ID, true);

        this.bedNukerGroup =
                new SettingGroup(BED_NUKER_GROUP_ID)
                        .addValues(this.bedNukerBlur, this.bedNukerShadow)
                        .collapsed();

        SettingGroup settingGroup = new SettingGroup(HUD_ELEMENTS_GROUP_ID);
        Setting[] hudElementSettings =
                new Setting[] {
                    this.arrayListGroup,
                    this.watermarkGroup,
                    this.targetHudGroup,
                    this.sessionStatsGroup,
                    this.scoreboardGroup,
                    this.nameTagsGroup,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        hudElementSettings[6] = this.tabGuiGroup;

        hudElementSettings[7] = this.partyHudGroup;

        hudElementSettings[8] = this.musicViewerGroup;

        hudElementSettings[9] = this.chatTabsGroup;

        hudElementSettings[10] = this.bedNukerGroup;
        this.hudElements = settingGroup.addValues(hudElementSettings);

        Setting[] allSettings =
                new Setting[] {
                    this.shadow,
                    this.shadowIterations,
                    this.shadowAlpha,
                    this.shadowDownsampleFactor,
                    this.blur,
                    this.blurIterations,
                    null,
                    null
                };

        allSettings[6] = this.blurDownsampleFactor;

        allSettings[7] = this.hudElements;
        this.addSettings(allSettings);
    }

    public static boolean isSessionStatsBlur() {
        return getEffectsModule().sessionStatsBlur.getValue();
    }

    public static boolean isSessionStatsShadow() {
        return getEffectsModule().sessionStatsShadow.getValue();
    }

    public static boolean isWatermarkBlur() {
        return getEffectsModule().watermarkBlur.getValue();
    }

    public static boolean isTabGuiShadow() {
        return getEffectsModule().tabGuiShadow.getValue();
    }

    public static boolean isPartyHudShadow() {
        return getEffectsModule().partyHudShadow.getValue();
    }

    public static boolean isNameTagsBlur() {
        return getEffectsModule().nameTagsBlur.getValue();
    }

    public static boolean isWatermarkShadow() {
        return getEffectsModule().watermarkShadow.getValue();
    }

    public static boolean isTargetHudBlur() {
        return getEffectsModule().targetHudBlur.getValue();
    }

    private static Effects getEffectsModule() {
        return Astralis.getInstance().getModuleManager().getModule(Effects.class);
    }

    public static boolean isScoreboardShadow() {
        return getEffectsModule().scoreboardShadow.getValue();
    }

    public static boolean isNameTagsShadow() {
        return getEffectsModule().nameTagsShadow.getValue();
    }

    public static boolean isScoreboardBlur() {
        return getEffectsModule().scoreboardBlur.getValue();
    }

    public static boolean isChatTabsBlur() {
        return getEffectsModule().chatTabsBlur.getValue();
    }

    public static boolean isBedNukerBlur() {
        return getEffectsModule().bedNukerBlur.getValue();
    }

    public static boolean isArrayListShadow() {
        return getEffectsModule().arrayListShadow.getValue();
    }

    public static boolean isChatTabsShadow() {
        return getEffectsModule().chatTabsShadow.getValue();
    }

    public static boolean isPartyHudBlur() {
        return getEffectsModule().partyHudBlur.getValue();
    }

    public static boolean isArrayListBlur() {
        return getEffectsModule().arrayListBlur.getValue();
    }

    public static boolean isBedNukerShadowActive() {
        Effects effects = getEffectsModule();
        return effects.isEnabled() && effects.bedNukerShadow.getValue();
    }

    public static boolean isTargetHudShadow() {
        return getEffectsModule().targetHudShadow.getValue();
    }

    public static boolean isTabGuiBlur() {
        return getEffectsModule().tabGuiBlur.getValue();
    }

    public static boolean isMusicViewerShadow() {
        return getEffectsModule().musicViewerShadow.getValue();
    }

    public static boolean isMusicViewerBlur() {
        return getEffectsModule().musicViewerBlur.getValue();
    }

    private static final String MODULE_ID = "5068f8760afc590fc4664be6f152944c";
    private static final String SHADOW_SETTING_ID = "cb1cbbab17ff969f254a807bbd908788";
    private static final String SHADOW_ITERATIONS_SETTING_ID =
            "76b8dde44b5b887f6e6b4d2c5fd83d904610618ad961a480911eed4ec5414d29";
    private static final String SHADOW_ALPHA_SETTING_ID = "19213ece44cfb51055d9cf15c6fbaa7b";
    private static final String SHADOW_DOWNSAMPLE_FACTOR_SETTING_ID =
            "b8fb77f82c1557d68f4ef7fe6b34fe9307059296951b163b2f15626be6e62c50";
    private static final String BLUR_SETTING_ID = "9748bb576f6f7edf1a35b848ba32739c";
    private static final String BLUR_ITERATIONS_SETTING_ID = "5feeef6f3796ac15f9bcb153af463495";
    private static final String BLUR_DOWNSAMPLE_FACTOR_SETTING_ID =
            "90c184f88af5caa81d28cd5faa4e586689ecb0b78482ff9858b263f226b0032f";
    private static final String ARRAY_LIST_GROUP_ID = "cf5871b95dba8b4018414780dc9675aa";
    private static final String WATERMARK_GROUP_ID = "fc7e3595d72796cf4fbe90ad2d3dbb23";
    private static final String TARGET_HUD_GROUP_ID = "a9d6bc67e56fd35a9740ec3e906e3634";
    private static final String SESSION_STATS_GROUP_ID = "0aed65edd50c08af00f1f7e05d0507c8";
    private static final String SCOREBOARD_GROUP_ID = "feae204841c8d7702421996f19addb77";
    private static final String NAME_TAGS_GROUP_ID = "6627272cc39fa62b8a205597250a3545";
    private static final String TAB_GUI_GROUP_ID = "697507826f227bcccbc3cde877ffd025";
    private static final String PARTY_HUD_GROUP_ID = "464990114cf1b61f51423d7e6fdf099e";
    private static final String MUSIC_VIEWER_GROUP_ID = "9079288b470e2344edd08677232cf7ad";
    private static final String CHAT_TABS_GROUP_ID = "1be3bac18ccdd2bf09d41d9475bc6513";
    private static final String BED_NUKER_GROUP_ID = "4712b2ac5c77d2da211ea5ba268fc613";
    private static final String HUD_ELEMENTS_GROUP_ID = "1ac51fdd65cff5632fe4751e44d699ee";
}
