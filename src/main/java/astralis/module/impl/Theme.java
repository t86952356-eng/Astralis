package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.ui.theme.ThemePreset;

import pub.astralis.client.module.core.Category;

public class Theme extends Module {
    private final BooleanSetting customTheme;
    private final ModeSetting themes;
    private final ColorSetting firstColor;
    private final ColorSetting secondColor;
    private final BooleanSetting customMainMenu;
    private final BooleanSetting customButtons;
    private final BooleanSetting customSliders;
    private final NumberSetting backgroundAlpha;
    private final ModeSetting hudFont;
    private final SettingGroup customMcComponents;

    public Theme() {

        super(MODULE_ID, "", "", Category.Misc);

        this.customTheme = new BooleanSetting(CUSTOM_THEME_SETTING_ID, false);

        this.themes = new ModeSetting(THEMES_SETTING_ID);

        this.firstColor =
                new ColorSetting(
                        FIRST_COLOR_VALUE_SETTING_ID, ThemePreset.ASTRALIS.getPrimaryColor());

        this.secondColor =
                new ColorSetting(
                        SECOND_COLOR_VALUE_SETTING_ID, ThemePreset.ASTRALIS.getSecondaryColor());

        this.customMainMenu = new BooleanSetting(CUSTOM_MAIN_MENU_SETTING_ID, true);

        this.customButtons = new BooleanSetting(CUSTOM_BUTTONS_SETTING_ID, true);

        this.customSliders = new BooleanSetting(CUSTOM_SLIDERS_SETTING_ID, true);

        this.backgroundAlpha =
                new NumberSetting(BACKGROUND_ALPHA_SETTING_ID, 55.0F, 0.0F, 100.0F, 1.0F);

        ModeSetting modeSetting = new ModeSetting(HUD_FONT_SETTING_ID);
        String[] fontModes = new String[4];

        fontModes[0] = "Default";

        fontModes[1] = "Geist";

        fontModes[2] = "Roboto";

        fontModes[3] = "IBM Plex Sans";
        ModeSetting modeSetting2 = modeSetting.addModes(fontModes);
        this.hudFont = modeSetting2.setDefault("Default");

        this.customMcComponents =
                new SettingGroup(CUSTOM_MC_COMPONENTS_GROUP_ID)
                        .addValues(this.customButtons, this.customSliders)
                        .collapsed();
        this.setEnabled(true);

        for (ThemePreset preset : ThemePreset.values()) {
            this.themes.addMode(preset.getDisplayName());
        }

        this.themes.setDefault("Astralis");
        this.firstColor.hideIf(
                () -> {
                    return !this.customTheme.getValue();
                });
        this.secondColor.hideIf(
                () -> {
                    return !this.customTheme.getValue();
                });
        this.themes.hideIf(this.customTheme::getValue);

        Setting[] allSettings =
                new Setting[] {
                    this.customTheme,
                    this.themes,
                    this.firstColor,
                    this.secondColor,
                    this.backgroundAlpha,
                    this.hudFont,
                    null,
                    null
                };

        allSettings[6] = this.customMainMenu;

        allSettings[7] = this.customMcComponents;
        this.addSettings(allSettings);
    }

    public BooleanSetting getCustomMainMenu() {
        return this.customMainMenu;
    }

    public SettingGroup getCustomMcComponents() {
        return this.customMcComponents;
    }

    public BooleanSetting getCustomSliders() {
        return this.customSliders;
    }

    public ColorSetting getFirstColor() {
        return this.firstColor;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (!this.customTheme.getValue()) {
            ThemePreset preset = ThemePreset.fromName(this.themes.getValue().getName());
            Astralis.getInstance().getThemeManager().setSelectedThemePreset(preset);
        }
    }

    public static int getBackgroundAlpha() {
        return Math.round(
                Astralis.getInstance()
                                .getModuleManager()
                                .getModule(Theme.class)
                                .backgroundAlpha
                                .getValue()
                        / 100.0F
                        * 255.0F);
    }

    public BooleanSetting getCustomButtons() {
        return this.customButtons;
    }

    public BooleanSetting getCustomTheme() {
        return this.customTheme;
    }

    public ModeSetting getThemes() {
        return this.themes;
    }

    public ColorSetting getSecondColor() {
        return this.secondColor;
    }

    public ModeSetting getHudFont() {
        return this.hudFont;
    }

    private static final String MODULE_ID = "84dbd3fb1427d0a70a519188a1992159";
    private static final String CUSTOM_THEME_SETTING_ID = "c2220f061909ee39e0283962463af519";
    private static final String THEMES_SETTING_ID = "a85f0fde15df4c3284453cb5c5c4b0fc";
    private static final String FIRST_COLOR_VALUE_SETTING_ID =
            "64d214f84818196b4ee5e9ed157e448030adfaee2a993919e993dbb4caef6f57";
    private static final String SECOND_COLOR_VALUE_SETTING_ID =
            "fde7a4f55a846d541a67d899eeceb9431d419cb0980ad6bcb347127b71406755";
    private static final String CUSTOM_MAIN_MENU_SETTING_ID = "806c4392bbdc9a6f8af79245d8f5e01e";
    private static final String CUSTOM_BUTTONS_SETTING_ID = "dd960812551fb715fc0d3ebd24b2674e";
    private static final String CUSTOM_SLIDERS_SETTING_ID = "1b80d948f74eb58b86315c1b35742360";
    private static final String BACKGROUND_ALPHA_SETTING_ID = "f369fe00b4d5b51bb644f9d70aeb491a";
    private static final String HUD_FONT_SETTING_ID = "f91cd632c31c17d1975ec2ea187cafa5";
    private static final String CUSTOM_MC_COMPONENTS_GROUP_ID =
            "eb3ed33e69ece3a8355a7c8e0cab27ac267a2601ecf9e0f97e7e795026e1835e";
}
