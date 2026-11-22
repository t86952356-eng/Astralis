package astralis.module.impl;

import astralis.module.Module;
import astralis.module.impl.nametags.mode.NameTagsAstralis;
import astralis.module.impl.nametags.mode.NameTagsSimple;
import astralis.setting.BooleanSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.SettingGroup;

import pub.astralis.client.module.core.Category;

public class NameTags extends Module {
    public final NumberSetting baseScale;
    public final BooleanSetting showHealth;
    public final BooleanSetting showDistance;
    public final BooleanSetting showHead;
    public final BooleanSetting monsters;
    public final BooleanSetting animals;
    public final BooleanSetting teammates;
    public final BooleanSetting invisible;
    public final BooleanSetting throughWalls;
    private final SettingGroup targetsGroup;
    private final ModeSetting mode;

    public NameTags() {

        super(MODULE_ID, "", "", Category.Misc);

        this.baseScale = new NumberSetting(BASE_SCALE_SETTING_ID, 1.0F, 0.5F, 3.0F, 0.1F);

        this.showHealth = new BooleanSetting(SHOW_HEALTH_SETTING_ID, true);

        this.showDistance = new BooleanSetting(SHOW_DISTANCE_SETTING_ID, true);

        this.showHead = new BooleanSetting(SHOW_HEAD_SETTING_ID, true);

        this.monsters = new BooleanSetting(MONSTERS_SETTING_ID, false);

        this.animals = new BooleanSetting(ANIMALS_SETTING_ID, false);

        this.teammates = new BooleanSetting(TEAMMATES_SETTING_ID, false);

        this.invisible = new BooleanSetting(INVISIBLE_SETTING_ID, true);

        this.throughWalls = new BooleanSetting(THROUGH_WALLS_SETTING_ID, true);

        this.targetsGroup =
                new SettingGroup(TARGETS_GROUP_ID)
                        .addValues(
                                this.teammates,
                                this.monsters,
                                this.animals,
                                this.invisible,
                                this.throughWalls);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new NameTagsAstralis(this))
                        .addValues(new NameTagsSimple(this));
        this.mode = modeSetting.setDefault("Astralis");

        this.addSettings(
                this.mode,
                this.baseScale,
                this.showHealth,
                this.showDistance,
                this.showHead,
                this.targetsGroup);
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    private static final String MODULE_ID = "6627272cc39fa62b8a205597250a3545";
    private static final String BASE_SCALE_SETTING_ID = "943a7167bf2a782a3e23008d59a080c9";
    private static final String SHOW_HEALTH_SETTING_ID = "648dfd02c427bd75e58554266fbef8dd";
    private static final String SHOW_DISTANCE_SETTING_ID = "edb9a4ca0d89758aa535a8597d7c11fb";
    private static final String SHOW_HEAD_SETTING_ID = "cfa6db652531d38e9d2c384f321f7e68";
    private static final String MONSTERS_SETTING_ID = "b2e39a7090da277f100b28beac526e55";
    private static final String ANIMALS_SETTING_ID = "2ba8220ce831391798985d9055975347";
    private static final String TEAMMATES_SETTING_ID = "24f4ec6ea6bb6a99768f41759110541a";
    private static final String INVISIBLE_SETTING_ID = "a63ea6c796b6ef2ae55074f6ee3726db";
    private static final String THROUGH_WALLS_SETTING_ID = "4fb927fdbee561036f15bca7ddf5b4c4";
    private static final String TARGETS_GROUP_ID = "6ef0e612f8bb85c79eef6a9949fb6be4";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
}
