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
import astralis.util.render.ColorUtil;
import astralis.util.render.Render2DContext;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Vector4f;

import pub.astralis.client.module.core.Category;

import java.awt.Color;

public class Particles extends Module {
    private final NumberSetting friction;
    private final NumberSetting gravity;
    private final NumberSetting lifeTime;
    private final BooleanSetting customColor;
    private final ColorSetting particleColor;
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
    private final SettingGroup glowSettings;
    private final SettingGroup outlineSettings;
    private final SettingGroup chamsSettings;
    private RenderTarget glowTarget;
    private boolean depthCopied;

    public Particles() {

        super(MODULE_ID, "", "", Category.Misc);

        this.friction = new NumberSetting(FRICTION_SETTING_ID, 1.0F, 0.0F, 3.0F, 0.01F);

        this.gravity = new NumberSetting(GRAVITY_SETTING_ID, 1.0F, 0.0F, 3.0F, 0.01F);

        this.lifeTime = new NumberSetting(LIFE_TIME_SETTING_ID, 1.0F, 0.1F, 10.0F, 0.01F);

        this.customColor = new BooleanSetting(CUSTOM_COLOR_SETTING_ID, false);

        this.particleColor = new ColorSetting(COLOR_SETTING_ID, Color.WHITE);

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

        SettingGroup settingGroup = new SettingGroup(GLOW_SETTINGS_GROUP_ID);
        Setting[] glowSettingList =
                new Setting[] {
                    this.glowColor,
                    this.glowRadius,
                    this.glowIntensity,
                    this.glowOverlay,
                    this.gradient,
                    this.gradientSpeed,
                    null
                };
        glowSettingList[6] = this.downsample;
        this.glowSettings = settingGroup.addValues(glowSettingList);

        this.outlineSettings =
                new SettingGroup(OUTLINE_SETTING_ID)
                        .addValues(
                                this.outline,
                                this.outlineThickness,
                                this.outlineColorMode,
                                this.outlineColor);

        this.chamsSettings =
                new SettingGroup(CHAMS_SETTING_ID)
                        .addValues(this.chams, this.chamsColorMode, this.chamsColor, this.chamsMix);
        ModeSetting modeSetting7 = this.particleColor.getColorMode();

        modeSetting7.setDefault("Client");
        this.particleColor.hideIf(
                () -> {
                    return !this.customColor.getValue();
                });
        ModeSetting modeSetting8 = this.glowColor.getColorMode();

        modeSetting8.setDefault("Client");
        this.glowSettings.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.outlineSettings.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.chamsSettings.hideIf(
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

        Setting[] allSettings =
                new Setting[] {
                    this.friction,
                    this.gravity,
                    this.lifeTime,
                    this.customColor,
                    this.particleColor,
                    this.glow,
                    null,
                    null,
                    null
                };

        allSettings[6] = this.glowSettings;

        allSettings[7] = this.outlineSettings;

        allSettings[8] = this.chamsSettings;
        this.addSettings(allSettings);
    }

    public int computeParticleColor(int baseColor, int age, int maxAge) {
        if (!this.customColor.getValue()) {
            return baseColor;
        }

        Color[] colors = this.particleColor.getColors();
        float blend = maxAge <= 0 ? 0.0F : Math.clamp((float) age / maxAge, 0.0F, 1.0F);
        Color color = ColorUtil.lerpColor(colors[0], colors[1], blend);

        int baseAlpha = baseColor >>> 24;

        int alpha = Math.round((baseAlpha & 0xFF) * color.getAlpha() / 255.0F);

        int alphaBits = alpha << 24;
        int red = color.getRed();

        int alphaRed = alphaBits | red << 16;
        int green = color.getGreen();

        return alphaRed | green << 8 | color.getBlue();
    }

    public void renderGlow(RenderTarget renderTarget, RenderTarget renderTarget2) {
        if (this.isEnabled() && this.glow.getValue()) {
            if (renderTarget != null && renderTarget.getColorTexture() != null) {
                if (renderTarget2 != null && renderTarget2.getColorTexture() != null) {
                    Render2DContext renderContext =
                            Astralis.getInstance().getFramebufferManager().getRenderContext();
                    if (renderContext != null) {
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

                        float angle;
                        if (this.gradient.is("Horizontal")) {
                            angle = 0.0F;
                        } else {

                            angle =
                                    this.gradient.is("Diagonal")
                                            ? (float) (Math.PI / 4)
                                            : (float) (Math.PI / 2);
                        }

                        float gradientAngle = angle;
                        renderContext
                                .getEntityGlowRenderer()
                                .renderEnhanced(
                                        renderTarget.getColorTexture(),
                                        color,
                                        color2,
                                        color3,
                                        color4,
                                        this.glowRadius.getValue(),
                                        this.outline.getValue()
                                                ? this.outlineThickness.getValue()
                                                : 0.0F,
                                        this.glowIntensity.getValue(),
                                        this.glowOverlay.getValue(),
                                        this.gradientSpeed.getValue(),
                                        gradientAngle,
                                        (float) (System.currentTimeMillis() % 100000L) / 1000.0F,
                                        this.chams.getValue() ? this.chamsMix.getValue() : 0.0F,
                                        color5,
                                        color6,
                                        renderTarget2,
                                        mc.getWindow().getWidth(),
                                        mc.getWindow().getHeight(),
                                        this.downsample.getValue());
                    }
                }
            }
        }
    }

    public NumberSetting getGravity() {
        return this.gravity;
    }

    public ModeSetting getGradient() {
        return this.gradient;
    }

    public BooleanSetting getGlow() {
        return this.glow;
    }

    public SettingGroup getChamsSettings() {
        return this.chamsSettings;
    }

    public NumberSetting getChamsMix() {
        return this.chamsMix;
    }

    public ColorSetting getChamsColor() {
        return this.chamsColor;
    }

    public boolean isDepthCopied() {
        return this.depthCopied;
    }

    public BooleanSetting getOutline() {
        return this.outline;
    }

    public ColorSetting getParticleColor() {
        return this.particleColor;
    }

    public NumberSetting getOutlineThickness() {
        return this.outlineThickness;
    }

    public NumberSetting getGlowRadius() {
        return this.glowRadius;
    }

    public NumberSetting getGradientSpeed() {
        return this.gradientSpeed;
    }

    public void blendIntoGlowTarget(RenderTarget renderTarget) {
        if (this.isGlowActive() && this.glowTarget != null && renderTarget != null) {
            this.glowTarget.blitAndBlendToTexture(
                    renderTarget.getColorTextureView(), renderTarget.getDepthTextureView());
        }
    }

    public void copyDepthOnce(RenderTarget renderTarget) {
        if (!this.depthCopied && this.glowTarget != null && renderTarget != null) {
            this.glowTarget.copyDepthFrom(renderTarget);
            this.depthCopied = true;
        }
    }

    public RenderTarget getGlowTarget() {
        return this.glowTarget;
    }

    public boolean isGlowActive() {
        return this.isEnabled() && this.glow.getValue();
    }

    public void prepareGlowTarget(int width, int height) {
        this.depthCopied = false;
        if (this.isGlowActive()) {
            if (this.glowTarget == null) {

                this.glowTarget =
                        new TextureTarget(
                                "Particle Glow Target", width, height, true, GpuFormat.RGBA8_UNORM);
            } else if (this.glowTarget.width != width || this.glowTarget.height != height) {
                this.glowTarget.resize(width, height);
            }

            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.clearColorTexture(
                    this.glowTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            if (this.glowTarget.getDepthTexture() != null) {
                commandEncoder.clearDepthTexture(this.glowTarget.getDepthTexture(), 0.0);
            }
        }
    }

    public SettingGroup getOutlineSettings() {
        return this.outlineSettings;
    }

    public BooleanSetting getCustomColor() {
        return this.customColor;
    }

    public float applyGravity(float value) {
        return value * this.gravity.getValue();
    }

    public int applyLifetime(int ticks) {
        return Math.max(1, ticks * this.lifeTime.getValue().intValue());
    }

    public float applyFriction(float value) {
        return clampValue(value / this.friction.getValue(), 0.0F, 0.9995F);
    }

    private static float clampValue(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public ColorSetting getGlowColor() {
        return this.glowColor;
    }

    public ModeSetting getChamsColorMode() {
        return this.chamsColorMode;
    }

    public ColorSetting getOutlineColor() {
        return this.outlineColor;
    }

    public BooleanSetting getChams() {
        return this.chams;
    }

    public NumberSetting getDownsample() {
        return this.downsample;
    }

    @EventTarget
    public void onPreRender2D(PreRender2DEvent event) {
        if (this.isGlowActive() && this.glowTarget != null) {
            this.renderGlow(this.glowTarget, mc.gameRenderer.mainRenderTarget());
        }
    }

    public ModeSetting getOutlineColorMode() {
        return this.outlineColorMode;
    }

    public NumberSetting getGlowIntensity() {
        return this.glowIntensity;
    }

    public NumberSetting getLifeTime() {
        return this.lifeTime;
    }

    public BooleanSetting getGlowOverlay() {
        return this.glowOverlay;
    }

    public SettingGroup getGlowSettings() {
        return this.glowSettings;
    }

    public NumberSetting getFriction() {
        return this.friction;
    }

    private static final String MODULE_ID = "da50dc209377e498977974bbd50ca3ea";
    private static final String FRICTION_SETTING_ID = "c14f18eb8ec815cc3e26c253c983c0ec";
    private static final String GRAVITY_SETTING_ID = "36ac7882996b766497e805d27d7db577";
    private static final String LIFE_TIME_SETTING_ID = "aeb3898f6c905b00658c19292e7ff6c8";
    private static final String CUSTOM_COLOR_SETTING_ID = "02d88fda45d14e5e9750f672c79a960c";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String GLOW_SETTING_ID = "a2f1c3e2323f5f46f7ddad7004cf419c";
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
