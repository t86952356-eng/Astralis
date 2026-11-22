package astralis.module.impl;

import astralis.Astralis;
import astralis.config.ConfigManager;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import pub.astralis.client.module.core.Category;

public class Weapon extends Module {
    private static volatile Weapon instance;
    private final BooleanSetting swords;
    private final BooleanSetting axes;
    private final BooleanSetting mace;
    private final BooleanSetting trident;
    private final BooleanSetting shield;
    private final BooleanSetting knockback;
    private final BooleanSetting fist;
    private final SettingGroup weaponTypes;

    public Weapon() {

        super(MODULE_ID, "", "", Category.Misc);

        this.swords = new BooleanSetting(SWORDS_SETTING_ID, true);

        this.axes = new BooleanSetting(AXES_SETTING_ID, true);

        this.mace = new BooleanSetting(MACE_SETTING_ID, true);

        this.trident = new BooleanSetting(TRIDENT_SETTING_ID, true);

        this.shield = new BooleanSetting(SHIELD_SETTING_ID, true);

        this.knockback = new BooleanSetting(KNOCKBACK_SETTING_ID, false);

        this.fist = new BooleanSetting(FIST_SETTING_ID, false);

        SettingGroup settingGroup = new SettingGroup(WEAPON_TYPES_GROUP_ID);
        Setting[] typeSettings =
                new Setting[] {
                    this.swords,
                    this.axes,
                    this.mace,
                    this.trident,
                    this.shield,
                    this.knockback,
                    null
                };

        typeSettings[6] = this.fist;
        this.weaponTypes = settingGroup.addValues(typeSettings).collapsed();
        instance = this;
        this.setEnabled(true);
        this.addSetting(this.weaponTypes);
        this.weaponTypes
                .getChildren()
                .forEach(
                        setting ->
                                ((BooleanSetting) setting)
                                        .addListeners(
                                                (previousValue, newValue) -> {
                                                    ConfigManager configManager =
                                                            Astralis.getInstance()
                                                                    .getConfigManager();
                                                    if (configManager != null) {
                                                        configManager.markDirty();
                                                    }
                                                }));
    }

    public SettingGroup getWeaponTypes() {
        return this.weaponTypes;
    }

    public static Weapon getInstance() {
        return instance;
    }

    public BooleanSetting getAxes() {
        return this.axes;
    }

    public BooleanSetting getMace() {
        return this.mace;
    }

    public BooleanSetting getShield() {
        return this.shield;
    }

    public BooleanSetting getTrident() {
        return this.trident;
    }

    public BooleanSetting getFist() {
        return this.fist;
    }

    public BooleanSetting getKnockback() {
        return this.knockback;
    }

    public BooleanSetting getSwords() {
        return this.swords;
    }

    private static final String MODULE_ID = "16115278e42412ef8f10cf6274740e0c";
    private static final String SWORDS_SETTING_ID = "7a4a714b95dc8bc2f392664665125a88";
    private static final String AXES_SETTING_ID = "1a6978a5904dd0b5e8e11dae1d1e8c26";
    private static final String MACE_SETTING_ID = "2c399bdea01acb7700f05162676d53c0";
    private static final String TRIDENT_SETTING_ID = "f037c361ffe1cc8059582ce18f454816";
    private static final String SHIELD_SETTING_ID = "cedaa7c1de841769a5b3d430dd77f723";
    private static final String KNOCKBACK_SETTING_ID = "11deb38ed1dd8210153f74dbbd002294";
    private static final String FIST_SETTING_ID = "542f4419f59102d44a3017468e35c68d";
    private static final String WEAPON_TYPES_GROUP_ID = "343ee7d6a54d4e1224c41f4dd718365f";
}
