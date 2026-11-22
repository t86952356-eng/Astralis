package astralis.module.impl;

import astralis.Astralis;
import astralis.module.Module;
import astralis.module.impl.bedwars.mode.BedwarsAlerts;
import astralis.module.impl.bedwars.mode.BedwarsAutoChest;
import astralis.module.impl.bedwars.mode.BedwarsBowUtils;
import astralis.module.impl.bedwars.mode.BedwarsInstantBuy;
import astralis.module.impl.bedwars.mode.BedwarsUpgradesDisplay;
import astralis.network.hypixel.HypixelLocation;
import astralis.setting.BooleanSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import net.hypixel.data.type.GameType;

import pub.astralis.client.module.core.Category;

public class Bedwars extends Module {

    public Bedwars() {

        super(MODULE_ID, "", "", Category.Misc);
        BedwarsAutoChest autoChest = new BedwarsAutoChest(this);

        BooleanSetting booleanSetting =
                new BooleanSetting(AUTO_CHEST_SETTING_ID, this, true, autoChest);
        autoChest
                .getSettingsGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting.getValue();
                        });
        autoChest
                .getValuablesGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting.getValue();
                        });
        BedwarsAlerts alerts = new BedwarsAlerts(this);

        BooleanSetting booleanSetting2 = new BooleanSetting(ALERTS_SETTING_ID, this, true, alerts);
        alerts.getSettingsGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting2.getValue();
                        });
        alerts.getItemsGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting2.getValue();
                        });
        BedwarsInstantBuy instantBuy = new BedwarsInstantBuy(this);
        BedwarsBowUtils bowUtils = new BedwarsBowUtils(this);
        BedwarsUpgradesDisplay upgradesDisplay = new BedwarsUpgradesDisplay(this);

        BooleanSetting booleanSetting3 =
                new BooleanSetting(UPGRADES_DISPLAY_SETTING_ID, this, true, upgradesDisplay);
        upgradesDisplay.setToggleSetting(booleanSetting3);
        upgradesDisplay
                .getSettingsGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting3.getValue();
                        });
        upgradesDisplay
                .getShowGroup()
                .hideIf(
                        () -> {
                            return !booleanSetting3.getValue();
                        });
        Setting[] settings = new Setting[5];

        settings[0] =
                new SettingGroup(AUTO_CHEST_SETTING_ID)
                        .addValues(
                                booleanSetting,
                                autoChest.getSettingsGroup(),
                                autoChest.getValuablesGroup());

        settings[1] = new BooleanSetting(INSTANT_BUY_SETTING_ID, this, true, instantBuy);

        settings[2] =
                new SettingGroup(ALERTS_SETTING_ID)
                        .addValues(
                                booleanSetting2, alerts.getSettingsGroup(), alerts.getItemsGroup());

        settings[3] = new BooleanSetting(BOW_UTILS_SETTING_ID, this, false, bowUtils);

        settings[4] =
                new SettingGroup(UPGRADES_DISPLAY_SETTING_ID)
                        .addValues(
                                booleanSetting3,
                                upgradesDisplay.getSettingsGroup(),
                                upgradesDisplay.getShowGroup());
        this.addSettings(settings);
    }

    public static boolean isInBedwarsGame() {
        HypixelLocation location = Astralis.getInstance().getHypixelApi().getLocation();
        if (location == null || location.getServerType() != GameType.BEDWARS) {
            return false;
        }

        if (location.getLobbyName() != null && !location.getLobbyName().isBlank()) {
            return false;
        }

        if (location.getMode() != null) {
            String mode = location.getMode();

            if (mode.equalsIgnoreCase("LOBBY")) {
                return false;
            }
        }

        return true;
    }

    private static final String MODULE_ID = "40e42d73be14e5074b369ca067b55e62";
    private static final String AUTO_CHEST_SETTING_ID = "80e87925499ca1645cd8fdf499e461ee";
    private static final String ALERTS_SETTING_ID = "a3a9598b608679b5b4165f6252d80bdd";
    private static final String UPGRADES_DISPLAY_SETTING_ID = "0272a10431eb2d624baeb25f8d47a5ff";
    private static final String INSTANT_BUY_SETTING_ID = "6c8356218f142bf19ca658f4ba97d12e";
    private static final String BOW_UTILS_SETTING_ID = "0236f409719eee48f89c09b1c532bf07";
}
