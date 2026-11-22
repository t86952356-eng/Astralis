package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.FogEvent;
import astralis.event.impl.SkyColorEvent;
import astralis.event.impl.SkyboxExtractEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldTimeEvent;
import astralis.module.Module;
import astralis.setting.ActionSetting;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.setting.TextSetting;
import astralis.util.render.ColorUtil;
import astralis.util.render.sky.SkyShaderManager;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.dimension.DimensionType.Skybox;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

public class Ambience extends Module {
    private final BooleanSetting spoofTime;
    private final NumberSetting worldTime;
    private final BooleanSetting spoofSky;
    private final ModeSetting skyMode;
    private final BooleanSetting spoofSkyColor;
    private final ColorSetting skyColor;
    private final BooleanSetting spoofFog;
    private final ColorSetting fogColor;
    private final NumberSetting fogHeight;
    private final BooleanSetting customParticle;
    private final ModeSetting particleType;
    private final NumberSetting particleAmount;
    private final NumberSetting particleRange;
    private final NumberSetting spawnInterval;
    private final NumberSetting spawnHeight;
    private static RandomSource random;
    private int particleTickCounter;
    public final BooleanSetting customSkyEnabled;
    public final BooleanSetting resourcePackSkies;
    public final ColorSetting customSkyColor;
    public final NumberSetting customSkySpeed;
    public final BooleanSetting useSavedShader;
    public final BooleanSetting shader2D;
    private final TextSetting shaderName;
    private final ActionSetting pasteAndSaveButton;
    private final ActionSetting loadSavedButton;

    public Ambience() {

        super(MODULE_ID, "", "", Category.Misc);

        this.spoofTime = new BooleanSetting(SPOOF_TIME_SETTING_ID, true);

        this.worldTime = new NumberSetting(WORLD_TIME_SETTING_ID, 6.0F, 0.0F, 24.0F, 0.1F);

        this.spoofSky = new BooleanSetting(SPOOF_SKY_SETTING_ID, false);

        ModeSetting modeSetting = new ModeSetting(SKY_MODE_SETTING_ID);
        String[] skyModes = new String[4];

        skyModes[0] = "Overworld";

        skyModes[1] = "End";

        skyModes[2] = "Nether";

        skyModes[3] = "None";
        ModeSetting modeSetting2 = modeSetting.addModes(skyModes);
        this.skyMode = modeSetting2.setDefault("Overworld");

        this.spoofSkyColor = new BooleanSetting(SPOOF_SKY_COLOR_SETTING_ID, false);

        this.skyColor = new ColorSetting(SKY_COLOR_SETTING_ID, new Color(6856959));

        this.spoofFog = new BooleanSetting(SPOOF_FOG_SETTING_ID, false);

        this.fogColor = new ColorSetting(FOG_COLOR_SETTING_ID, new Color(12638463));

        this.fogHeight = new NumberSetting(FOG_HEIGHT_SETTING_ID, 128.0F, 0.0F, 512.0F, 1.0F);

        this.customParticle = new BooleanSetting(CUSTOM_PARTICLE_SETTING_ID, false);

        ModeSetting modeSetting3 = new ModeSetting(SETTINGS_SETTING_ID);
        String[] particleTypes = new String[7];

        particleTypes[0] = "Cherry Blossom";

        particleTypes[1] = "Pale Oak";

        particleTypes[2] = "Ash";

        particleTypes[3] = "Snow";

        particleTypes[4] = "Soul";

        particleTypes[5] = "Fireflies";

        particleTypes[6] = "Spore Blossom";
        ModeSetting modeSetting4 = modeSetting3.addModes(particleTypes);
        this.particleType = modeSetting4.setDefault("Cherry Blossom");

        this.particleAmount = new NumberSetting(AMOUNT_SETTING_ID, 4.0F, 1.0F, 20.0F, 1.0F);

        this.particleRange = new NumberSetting(RANGE_SETTING_ID, 6.0F, 1.0F, 32.0F, 1.0F);

        this.spawnInterval = new NumberSetting(SPAWN_INTERVAL_SETTING_ID, 2.0F, 1.0F, 40.0F, 1.0F);

        this.spawnHeight = new NumberSetting(SPAWN_HEIGHT_SETTING_ID, 7.0F, 1.0F, 32.0F, 1.0F);

        this.customSkyEnabled = new BooleanSetting(ENABLED_SETTING_ID, false);

        this.resourcePackSkies = new BooleanSetting(RESOURCE_PACK_SKIES_SETTING_ID, true);

        this.customSkyColor = new ColorSetting(COLOR_SETTING_ID, new Color(657966));

        this.customSkySpeed = new NumberSetting(SPEED_SETTING_ID, 0.5F, 0.0F, 5.0F, 0.1F);

        this.useSavedShader = new BooleanSetting(USE_SAVED_SHADER_SETTING_ID, false);

        this.shader2D = new BooleanSetting(SETTING_2_D_SHADER_SETTING_ID, false);

        this.shaderName = new TextSetting("Shader Name", "393dddbd428d41c9212ce4d2aa539676");

        this.pasteAndSaveButton =
                new ActionSetting(
                        "df128f36c1f10531a6bc66a1dc78f222",
                        () -> {
                            if (SkyShaderManager.importClipboardShader(
                                    this.shaderName.getValue())) {
                                this.customSkyEnabled.setValue(true);
                                this.useSavedShader.setValue(true);
                            }
                        });

        this.loadSavedButton =
                new ActionSetting(
                        "cf02f3cb810c732eb43f344146173b91",
                        () -> {
                            if (SkyShaderManager.loadSavedShader(this.shaderName.getValue())) {
                                this.customSkyEnabled.setValue(true);
                                this.useSavedShader.setValue(true);
                            }
                        });
        this.worldTime.hideIf(
                () -> {
                    return !this.spoofTime.getValue();
                });
        this.skyMode.hideIf(
                () -> {
                    return !this.spoofSky.getValue();
                });
        this.spoofSkyColor.hideIf(
                () -> {
                    return !this.spoofSky.getValue();
                });
        this.skyColor.hideIf(
                () -> {
                    return !this.spoofSky.getValue() || !this.spoofSkyColor.getValue();
                });
        this.fogColor.hideIf(
                () -> {
                    return !this.spoofFog.getValue();
                });
        this.fogHeight.hideIf(
                () -> {
                    return !this.spoofFog.getValue();
                });
        this.particleType.hideIf(
                () -> {
                    return !this.customParticle.getValue();
                });
        this.particleAmount.hideIf(
                () -> {
                    return !this.customParticle.getValue();
                });
        this.particleRange.hideIf(
                () -> {
                    return !this.customParticle.getValue();
                });
        this.spawnInterval.hideIf(
                () -> {
                    return !this.customParticle.getValue();
                });
        this.spawnHeight.hideIf(
                () -> {
                    return !this.customParticle.getValue();
                });
        this.customSkyColor.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue();
                });
        this.customSkySpeed.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue();
                });
        this.useSavedShader.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue();
                });
        this.shader2D.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue() || !this.useSavedShader.getValue();
                });
        this.shaderName.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue() || !this.useSavedShader.getValue();
                });
        this.pasteAndSaveButton.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue() || !this.useSavedShader.getValue();
                });
        this.loadSavedButton.hideIf(
                () -> {
                    return !this.customSkyEnabled.getValue() || !this.useSavedShader.getValue();
                });
        ModeSetting modeSetting5 = this.fogColor.getColorMode();

        modeSetting5.setDefault("Client");
        SettingGroup settingGroup =
                new SettingGroup(TIME_GROUP_ID).addValues(this.spoofTime, this.worldTime);
        SettingGroup settingGroup2 =
                new SettingGroup(SKY_GROUP_ID)
                        .addValues(this.spoofSky, this.skyMode, this.spoofSkyColor, this.skyColor);
        SettingGroup settingGroup3 =
                new SettingGroup(FOG_GROUP_ID)
                        .addValues(this.spoofFog, this.fogColor, this.fogHeight);
        SettingGroup settingGroup4 = new SettingGroup(CUSTOM_SKY_GROUP_ID);

        Setting[] customSkySettings =
                new Setting[] {
                    this.customSkyEnabled,
                    this.customSkyColor,
                    this.customSkySpeed,
                    this.useSavedShader,
                    this.shader2D,
                    this.shaderName,
                    null,
                    null
                };

        customSkySettings[6] = this.pasteAndSaveButton;

        customSkySettings[7] = this.loadSavedButton;
        SettingGroup settingGroup5 = settingGroup4.addValues(customSkySettings);
        SettingGroup settingGroup6 =
                new SettingGroup(PACK_SKIES_GROUP_ID).addValues(this.resourcePackSkies);
        SettingGroup settingGroup7 = new SettingGroup(CUSTOM_PARTICLE_SETTING_ID);

        SettingGroup settingGroup8 =
                settingGroup7.addValues(
                        this.customParticle,
                        this.particleType,
                        this.particleAmount,
                        this.particleRange,
                        this.spawnInterval,
                        this.spawnHeight);

        this.addSettings(
                settingGroup,
                settingGroup2,
                settingGroup3,
                settingGroup5,
                settingGroup6,
                settingGroup8);
    }

    private ParticleOptions getParticleOptions(String particleName) {

        String name = particleName;
        byte matchIndex = -1;
        switch (name.hashCode()) {
            case -1805675620:
                if (name.equals("Cherry Blossom")) {
                    matchIndex = 0;
                }
                break;
            case -416309976:
                if (name.equals("Spore Blossom")) {

                    matchIndex = 6;
                }
                break;
            case 66134:
                if (name.equals("Ash")) {
                    matchIndex = 2;
                }
                break;
            case 2581923:
                if (name.equals("Snow")) {
                    matchIndex = 3;
                }
                break;
            case 2583059:
                if (name.equals("Soul")) {
                    matchIndex = 4;
                }
                break;
            case 1065673443:
                if (name.equals("Pale Oak")) {
                    matchIndex = 1;
                }
                break;
            case 1723967419:
                if (name.equals("Fireflies")) {
                    matchIndex = 5;
                }
        }
        return switch (matchIndex) {
            case 0 -> ParticleTypes.CHERRY_LEAVES;
            case 1 -> ParticleTypes.PALE_OAK_LEAVES;
            case 2 -> ParticleTypes.ASH;
            case 3 -> ParticleTypes.SNOWFLAKE;
            case 4 -> ParticleTypes.SOUL;
            case 5 -> ParticleTypes.FIREFLY;
            case 6 -> ParticleTypes.FALLING_SPORE_BLOSSOM;
            default -> ParticleTypes.CHERRY_LEAVES;
        };
    }

    @EventTarget
    public void onSkyboxExtract(SkyboxExtractEvent event) {
        if (this.spoofSky.getValue()) {
            event.setSkybox(
                    switch (this.skyMode.getValue().getName()) {
                        case "End" -> Skybox.END;
                        case "Nether" -> Skybox.NONE;
                        default -> Skybox.OVERWORLD;
                    });
        }
    }

    @EventTarget
    public void onWorldTime(WorldTimeEvent event) {
        if (this.spoofTime.getValue()) {
            event.setTotalTicks(this.worldTime.getValue().longValue() * 1000L);
        }
    }

    @EventTarget
    public void onFog(FogEvent event) {
        if (this.spoofFog.getValue()) {
            Color[] colors = this.fogColor.getColors();
            event.setColor(ColorUtil.animatedBlend(0.0F, colors[0], colors[1]));
            event.setSkyEndReduction(this.fogHeight.getValue());
        }
    }

    @EventTarget
    public void onSkyColor(SkyColorEvent event) {
        if (this.spoofSky.getValue() && this.spoofSkyColor.getValue()) {
            event.setColor(this.skyColor.getValue().getRGB());
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST
                && this.customParticle.getValue()
                && ++this.particleTickCounter % this.spawnInterval.getValue().intValue() == 0) {
            ParticleOptions particleOptions =
                    this.getParticleOptions(this.particleType.getValue().getName());
            int particleCount = this.particleAmount.getValue().intValue();
            double range = this.particleRange.getValue().doubleValue();
            double maxHeight = this.spawnHeight.getValue().doubleValue();

            for (int i = 0; i < particleCount; i++) {
                double x = mc.player.getX() + (random.nextDouble() - 0.5) * range * 2.0;
                double y = mc.player.getY() + 3.0 + random.nextDouble() * maxHeight;
                double z = mc.player.getZ() + (random.nextDouble() - 0.5) * range * 2.0;
                double xSpeed = (random.nextDouble() - 0.5) * 0.02;
                double ySpeed = -0.01 - random.nextDouble() * 0.02;
                double zSpeed = (random.nextDouble() - 0.5) * 0.02;
                mc.level.addParticle(particleOptions, x, y, z, xSpeed, ySpeed, zSpeed);
            }
        }
    }

    static {
        random = RandomSource.create();
    }

    private static final String MODULE_ID = "ef2bdd03812551b86968efbe646fb13b";
    private static final String SPOOF_TIME_SETTING_ID = "31c1595d90ec5e586e6e5e710f5ae20b";
    private static final String WORLD_TIME_SETTING_ID = "5420156bc1310b4263a39b4c3db06998";
    private static final String SPOOF_SKY_SETTING_ID = "dbdb02012e9e2b87ca50805c770c3e9c";
    private static final String SKY_MODE_SETTING_ID = "9a66fd3d5ffaa215cd26803dd73e9302";
    private static final String SPOOF_SKY_COLOR_SETTING_ID = "d5ebf3f64c9af8e11ed2d7f3a2daf526";
    private static final String SKY_COLOR_SETTING_ID = "e0573cfed4427c3b644934fd34b8f2f5";
    private static final String SPOOF_FOG_SETTING_ID = "3554933f5c26a9da2e2acbdde03a45c4";
    private static final String FOG_COLOR_SETTING_ID = "cf921b9c4227a2a247c4d055b4dc3206";
    private static final String FOG_HEIGHT_SETTING_ID = "5c69eac8a1c5a16e18924c00763b5409";
    private static final String CUSTOM_PARTICLE_SETTING_ID = "cb75c19ff892b84fb22bd7e43af74706";
    private static final String SETTINGS_SETTING_ID = "e1b6ffd7ce011c799b9f90fcf88693e3";
    private static final String AMOUNT_SETTING_ID = "2c41d89a7c14430b510ee7dc2aa1fe21";
    private static final String RANGE_SETTING_ID = "4ed84ffdb32f200ee53f8c90db3cab6d";
    private static final String SPAWN_INTERVAL_SETTING_ID = "063826377642c13326332f1ce77782bd";
    private static final String SPAWN_HEIGHT_SETTING_ID = "403d411cdd569e0e01e204b21a565673";
    private static final String ENABLED_SETTING_ID = "b657c6cd5532f83d481053ab9e7834f5";
    private static final String RESOURCE_PACK_SKIES_SETTING_ID =
            "bb2948d458b4f88adb9d2cb6138720006b5abc67537d7707520e957f4e2ceb20";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String SPEED_SETTING_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String USE_SAVED_SHADER_SETTING_ID = "a5a6af01f7d44acfefde6d905eba5aa5";
    private static final String SETTING_2_D_SHADER_SETTING_ID = "0de35aba48df9cbb1b15d61768e0ce81";
    private static final String TIME_GROUP_ID = "44c7010f8b7722158460369281f063f4";
    private static final String SKY_GROUP_ID = "9a4c520c034ba508e4b5205c30ade027";
    private static final String FOG_GROUP_ID = "4df240be386748f38396435a467fc46d";
    private static final String CUSTOM_SKY_GROUP_ID = "57b85fc02c6b60cfb8ef6c42c505dc94";
    private static final String PACK_SKIES_GROUP_ID = "3e63570858e690bb90ced6a2e1274a90";
}
