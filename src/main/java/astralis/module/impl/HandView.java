package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.PreRender2DEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

public class HandView extends Module {
    private final NumberSetting xPosition;
    private final NumberSetting yPosition;
    private final NumberSetting zPosition;
    private final NumberSetting eatingXPosition;
    private final NumberSetting eatingYPosition;
    private final NumberSetting eatingZPosition;
    private final SettingGroup positionGroup;
    private final NumberSetting xRotation;
    private final NumberSetting yRotation;
    private final NumberSetting zRotation;
    private final SettingGroup rotationGroup;
    private final BooleanSetting glow;
    private final ColorSetting glowColor;
    private final NumberSetting glowRadius;
    private final NumberSetting glowIntensity;
    private final BooleanSetting glowOverlay;
    private final ModeSetting gradient;
    private final NumberSetting gradientSpeed;
    private final BooleanSetting outline;
    private final NumberSetting outlineThickness;
    private final ModeSetting outlineColorMode;
    private final ColorSetting outlineColor;
    private final BooleanSetting chams;
    private final ModeSetting chamsColorMode;
    private final ColorSetting chamsColor;
    private final NumberSetting chamsMix;
    private final NumberSetting downsample;
    private final SettingGroup glowSettingsGroup;
    private final SettingGroup outlineGroup;
    private final SettingGroup chamsGroup;
    private RenderTarget glowTarget;

    public HandView() {

        super(MODULE_ID, "", "", Category.Misc);

        this.xPosition = new NumberSetting(X_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        this.yPosition = new NumberSetting(Y_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        this.zPosition = new NumberSetting(Z_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        this.eatingXPosition =
                new NumberSetting(EATING_X_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        this.eatingYPosition =
                new NumberSetting(EATING_Y_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        this.eatingZPosition =
                new NumberSetting(EATING_Z_POSITION_SETTING_ID, 0.0F, -1.0F, 1.0F, 0.025F);

        SettingGroup settingGroup = new SettingGroup(POSITION_GROUP_ID);
        this.positionGroup =
                settingGroup.addValues(
                        this.xPosition,
                        this.yPosition,
                        this.zPosition,
                        this.eatingXPosition,
                        this.eatingYPosition,
                        this.eatingZPosition);

        this.xRotation = new NumberSetting(X_ROTATION_SETTING_ID, 0.0F, -180.0F, 180.0F, 1.0F);

        this.yRotation = new NumberSetting(Y_ROTATION_SETTING_ID, 0.0F, -180.0F, 180.0F, 1.0F);

        this.zRotation = new NumberSetting(Z_ROTATION_SETTING_ID, 0.0F, -180.0F, 180.0F, 1.0F);

        this.rotationGroup =
                new SettingGroup(ROTATION_GROUP_ID)
                        .addValues(this.xRotation, this.yRotation, this.zRotation);

        this.glow = new BooleanSetting(GLOW_SETTING_ID, false);

        this.glowColor = new ColorSetting(COLOR_SETTING_ID, Color.CYAN);

        this.glowRadius = new NumberSetting(RADIUS_SETTING_ID, 3.0F, 0.1F, 20.0F, 0.1F);

        this.glowIntensity = new NumberSetting(GLOW_INTENSITY_SETTING_ID, 1.5F, 0.1F, 5.0F, 0.1F);

        this.glowOverlay = new BooleanSetting(GLOW_OVERLAY_SETTING_ID, false);

        ModeSetting modeSetting = new ModeSetting(GRADIENT_SETTING_ID);
        String[] gradientModes = new String[3];

        gradientModes[0] = "Vertical";

        gradientModes[1] = "Horizontal";

        gradientModes[2] = "Diagonal";
        ModeSetting modeSetting2 = modeSetting.addModes(gradientModes);
        this.gradient = modeSetting2.setDefault("Vertical");

        this.gradientSpeed = new NumberSetting(GRADIENT_SPEED_SETTING_ID, 0.5F, 0.0F, 5.0F, 0.1F);

        this.outline = new BooleanSetting(OUTLINE_SETTING_ID, true);

        this.outlineThickness =
                new NumberSetting(OUTLINE_THICKNESS_SETTING_ID, 1.5F, 0.5F, 5.0F, 0.1F);

        ModeSetting modeSetting3 = new ModeSetting(OUTLINE_COLOR_SETTING_ID);
        String[] outlineColorModes = new String[2];

        outlineColorModes[0] = "Custom";

        outlineColorModes[1] = "Same";
        ModeSetting modeSetting4 = modeSetting3.addModes(outlineColorModes);
        this.outlineColorMode = modeSetting4.setDefault("Same");

        this.outlineColor = new ColorSetting(COLOR_SETTING_ID, Color.WHITE);

        this.chams = new BooleanSetting(CHAMS_SETTING_ID, false);

        ModeSetting modeSetting5 = new ModeSetting(CHAMS_COLOR_SETTING_ID);
        String[] chamsColorModes = new String[2];

        chamsColorModes[0] = "Custom";

        chamsColorModes[1] = "Client";
        ModeSetting modeSetting6 = modeSetting5.addModes(chamsColorModes);
        this.chamsColorMode = modeSetting6.setDefault("Client");

        this.chamsColor = new ColorSetting(COLOR_SETTING_ID, new Color(255, 50, 50, 180));

        this.chamsMix = new NumberSetting(CHAMS_MIX_SETTING_ID, 0.5F, 0.0F, 1.0F, 0.05F);

        this.downsample = new NumberSetting(DOWNSAMPLE_SETTING_ID, 0.5F, 0.1F, 1.0F, 0.05F);

        SettingGroup settingGroup2 = new SettingGroup(GLOW_SETTINGS_GROUP_ID);
        Setting[] glowSettings =
                new Setting[] {
                    this.glowColor,
                    this.glowRadius,
                    this.glowIntensity,
                    this.glowOverlay,
                    this.gradient,
                    this.gradientSpeed,
                    null
                };
        glowSettings[6] = this.downsample;
        this.glowSettingsGroup = settingGroup2.addValues(glowSettings);

        this.outlineGroup =
                new SettingGroup(OUTLINE_SETTING_ID)
                        .addValues(
                                this.outline,
                                this.outlineThickness,
                                this.outlineColorMode,
                                this.outlineColor);

        this.chamsGroup =
                new SettingGroup(CHAMS_SETTING_ID)
                        .addValues(this.chams, this.chamsColorMode, this.chamsColor, this.chamsMix);
        this.glowSettingsGroup.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.outlineGroup.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.chamsGroup.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.outlineThickness.hideIf(
                () -> {
                    return !this.outline.getValue();
                });
        this.outlineColorMode.hideIf(
                () -> {
                    return !this.outline.getValue();
                });
        this.outlineColor.hideIf(
                () -> {
                    if (this.outline.getValue()) {

                        if (this.outlineColorMode.is("Custom")) {
                            return false;
                        }
                    }

                    return true;
                });
        this.chamsColorMode.hideIf(
                () -> {
                    return !this.chams.getValue();
                });
        this.chamsColor.hideIf(
                () -> {
                    if (this.chams.getValue()) {

                        if (this.chamsColorMode.is("Custom")) {
                            return false;
                        }
                    }

                    return true;
                });
        this.chamsMix.hideIf(
                () -> {
                    return !this.chams.getValue();
                });
        ModeSetting modeSetting7 = this.glowColor.getColorMode();

        modeSetting7.setDefault("Client");

        this.addSettings(
                this.positionGroup,
                this.rotationGroup,
                this.glow,
                this.glowSettingsGroup,
                this.outlineGroup,
                this.chamsGroup);
    }

    public SettingGroup getRotationGroup() {
        return this.rotationGroup;
    }

    public SettingGroup getPositionGroup() {
        return this.positionGroup;
    }

    public BooleanSetting getChams() {
        return this.chams;
    }

    public NumberSetting getOutlineThickness() {
        return this.outlineThickness;
    }

    public ModeSetting getOutlineColorMode() {
        return this.outlineColorMode;
    }

    public NumberSetting getEatingZPosition() {
        return this.eatingZPosition;
    }

    public NumberSetting getXPosition() {
        return this.xPosition;
    }

    public NumberSetting getEatingYPosition() {
        return this.eatingYPosition;
    }

    public NumberSetting getYRotation() {
        return this.yRotation;
    }

    public NumberSetting getZRotation() {
        return this.zRotation;
    }

    public NumberSetting getZPosition() {
        return this.zPosition;
    }

    public NumberSetting getEatingXPosition() {
        return this.eatingXPosition;
    }

    public NumberSetting getYPosition() {
        return this.yPosition;
    }

    public NumberSetting getXRotation() {
        return this.xRotation;
    }

    @EventTarget(1)
    public void onPreRender2D(PreRender2DEvent event) {
        if (this.glow.getValue()
                && mc.options.getCameraType().isFirstPerson()
                && this.glowTarget != null
                && this.glowTarget.getColorTexture() != null) {
            Color color = this.glowColor.getValue();
            Color color2 = this.glowColor.getSecondaryColor();
            Color color3;
            Color color4;
            if (!this.outline.getValue()) {
                color3 = color4 = new Color(0, 0, 0, 0);
            } else {

                if (this.outlineColorMode.is("Custom")) {
                    color3 = this.outlineColor.getValue();
                    color4 = this.outlineColor.getSecondaryColor();
                } else {
                    color3 = color;
                    color4 = color2;
                }
            }

            float chamsMixAmount = this.chams.getValue() ? this.chamsMix.getValue() : 0.0F;
            Color color5;
            Color color6;
            if (this.chams.getValue()) {

                if (this.chamsColorMode.is("Custom")) {
                    color5 = this.chamsColor.getValue();
                    color6 = this.chamsColor.getSecondaryColor();
                } else {
                    color5 = Astralis.getInstance().getPrimaryColor();
                    color6 = Astralis.getInstance().getSecondaryColor();
                }
            } else {
                color6 = Color.BLACK;
                color5 = Color.BLACK;
            }

            float gradientAngle = this.getGradientAngle();
            float animationTime = (float) (System.currentTimeMillis() % 100000L) / 1000.0F;
            event.getContext()
                    .getEntityGlowRenderer()
                    .renderEnhanced(
                            this.glowTarget.getColorTexture(),
                            color,
                            color2,
                            color3,
                            color4,
                            this.glowRadius.getValue(),
                            this.outline.getValue() ? this.outlineThickness.getValue() : 0.0F,
                            this.glowIntensity.getValue(),
                            this.glowOverlay.getValue(),
                            this.gradientSpeed.getValue(),
                            gradientAngle,
                            animationTime,
                            chamsMixAmount,
                            color5,
                            color6,
                            mc.gameRenderer.mainRenderTarget(),
                            mc.getWindow().getWidth(),
                            mc.getWindow().getHeight(),
                            this.downsample.getValue());
        }
    }

    public BooleanSetting getGlow() {
        return this.glow;
    }

    public boolean isGlowActive() {
        return this.isEnabled() && this.glow.getValue();
    }

    public RenderTarget getGlowTarget() {
        return this.glowTarget;
    }

    public RenderTarget getOrCreateGlowTarget(int width, int height) {
        if (this.glowTarget == null) {

            this.glowTarget =
                    new TextureTarget(
                            "Hand Glow Target", width, height, true, GpuFormat.RGBA8_UNORM);
        } else if (this.glowTarget.width != width || this.glowTarget.height != height) {
            this.glowTarget.resize(width, height);
        }

        return this.glowTarget;
    }

    public BooleanSetting getOutline() {
        return this.outline;
    }

    public NumberSetting getChamsMix() {
        return this.chamsMix;
    }

    public SettingGroup getOutlineGroup() {
        return this.outlineGroup;
    }

    public ColorSetting getOutlineColor() {
        return this.outlineColor;
    }

    public SettingGroup getChamsGroup() {
        return this.chamsGroup;
    }

    public NumberSetting getGlowRadius() {
        return this.glowRadius;
    }

    public BooleanSetting getGlowOverlay() {
        return this.glowOverlay;
    }

    public ModeSetting getGradient() {
        return this.gradient;
    }

    public NumberSetting getGlowIntensity() {
        return this.glowIntensity;
    }

    public NumberSetting getDownsample() {
        return this.downsample;
    }

    private float getGradientAngle() {

        if (this.gradient.is("Horizontal")) {
            return 0.0F;
        }

        return this.gradient.is("Diagonal") ? (float) (Math.PI / 4) : (float) (Math.PI / 2);
    }

    public ColorSetting getGlowColor() {
        return this.glowColor;
    }

    public ModeSetting getChamsColorMode() {
        return this.chamsColorMode;
    }

    public SettingGroup getGlowSettingsGroup() {
        return this.glowSettingsGroup;
    }

    public ColorSetting getChamsColor() {
        return this.chamsColor;
    }

    public NumberSetting getGradientSpeed() {
        return this.gradientSpeed;
    }

    private static final String MODULE_ID = "20e636e68b8bf021b89c5f97bd752257";
    private static final String X_POSITION_SETTING_ID = "155826c85dd5dca1d42f1d6f491c4122";
    private static final String Y_POSITION_SETTING_ID = "80094df8f4759d6ae009cb90bafede63";
    private static final String Z_POSITION_SETTING_ID = "88559a473febe091f0f07690eb55c623";
    private static final String EATING_X_POSITION_SETTING_ID =
            "293d5d728b23a5e1aea3e032b52a7faf6c929b1bc78be2211a9b316078e9efdb";
    private static final String EATING_Y_POSITION_SETTING_ID =
            "aa7622c753b2c3bc8543a246ea19d22e6c929b1bc78be2211a9b316078e9efdb";
    private static final String EATING_Z_POSITION_SETTING_ID =
            "84c5fd799344f43609ba19ee55ee72256c929b1bc78be2211a9b316078e9efdb";
    private static final String POSITION_GROUP_ID = "3e04f9460f293db851991fed83f6912b";
    private static final String X_ROTATION_SETTING_ID = "9fa12732237f2ed3726df358eae64826";
    private static final String Y_ROTATION_SETTING_ID = "eb4796a13c630d1315595ec4d6635993";
    private static final String Z_ROTATION_SETTING_ID = "7d84fc19b6c409484c0feac024e72dac";
    private static final String ROTATION_GROUP_ID = "7a6f64085334e22b812fc54f8a90150b";
    private static final String GLOW_SETTING_ID = "a2f1c3e2323f5f46f7ddad7004cf419c";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String RADIUS_SETTING_ID = "00107704ec92e058e1cce9b2a3ab2723";
    private static final String GLOW_INTENSITY_SETTING_ID = "83f803db46a2015e8834bcc6af7e551a";
    private static final String GLOW_OVERLAY_SETTING_ID = "347f4eb77944096538fd5eca47d61e13";
    private static final String GRADIENT_SETTING_ID = "b83e530dfe658ae290e7bdd875e16de1";
    private static final String GRADIENT_SPEED_SETTING_ID = "eb10ec982d4e9152cbf0e7e65d7b195a";
    private static final String OUTLINE_SETTING_ID = "d0c073952922eca2e57c7c872e697e8b";
    private static final String OUTLINE_THICKNESS_SETTING_ID =
            "0e3714b906207e270c2913c61b74945b4610618ad961a480911eed4ec5414d29";
    private static final String OUTLINE_COLOR_SETTING_ID = "cacb720f82162eb4a75feb63eaf40894";
    private static final String CHAMS_SETTING_ID = "0e972e2d1bbbcdbddc280c0288802bc1";
    private static final String CHAMS_COLOR_SETTING_ID = "035d1d5bdbbbfac78b5c2eeca9661f16";
    private static final String CHAMS_MIX_SETTING_ID = "826576572a682320293d223f8aaf6366";
    private static final String DOWNSAMPLE_SETTING_ID = "59a82b2b50d7e6e3035849c2ba77b3d9";
    private static final String GLOW_SETTINGS_GROUP_ID = "81cd366cdc2dbee38e7de4a45ecf7484";
}
