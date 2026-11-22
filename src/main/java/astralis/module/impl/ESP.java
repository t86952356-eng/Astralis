package astralis.module.impl;

import astralis.event.EventTarget;
import astralis.event.impl.EntityGlowCheckEvent;
import astralis.event.impl.PreRender2DEvent;
import astralis.event.impl.Render2DEvent;
import astralis.mixin.LevelRendererAccessor;
import astralis.module.Module;
import astralis.module.impl.esp.EspDirectionSwitchMap;
import astralis.module.impl.esp.PlayerPoseSnapshot;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.render.ColorUtil;
import astralis.util.render.EntityGlowRenderer;
import astralis.util.render.ModelPartPose;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ScreenProjector;
import astralis.util.render.ShapePaint;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ESP extends Module {
    private static Map<Integer, PlayerPoseSnapshot> poseSnapshots;
    private final BooleanSetting esp2D;
    private final BooleanSetting mainBox;
    private final BooleanSetting mainBoxShadow;
    private final BooleanSetting fillMainBox;
    private final ModeSetting mainBoxMode;
    private final ColorSetting fillMainBoxColor;
    private final SettingGroup mainBoxGroup;
    private final BooleanSetting healthBar;
    private final BooleanSetting healthBarShadow;
    private final ModeSetting healthBarOrientation;
    private final SettingGroup healthBarGroup;
    private final ColorSetting mainColor;
    private final ColorSetting healthBarColor;
    private final SettingGroup colorGroup;
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
    private final ColorSetting chamsColor;
    private final NumberSetting chamsMix;
    private final NumberSetting downsample;
    private final BooleanSetting skeleton;
    private final ColorSetting skeletonColor;
    private final ColorSetting murderColor;
    private final NumberSetting skeletonThickness;
    private final SettingGroup skeletonGroup;
    private float skeletonMinY;
    private float skeletonMaxY;
    private boolean targetIsMurderer;

    public ESP() {

        super(MODULE_ID, "", "", Category.Misc);

        this.esp2D = new BooleanSetting(SETTING_2_D_SETTING_ID, true);

        this.mainBox = new BooleanSetting(MAIN_BOX_SETTING_ID, true);

        this.mainBoxShadow = new BooleanSetting(MAIN_BOX_SHADOW_SETTING_ID, true);

        this.fillMainBox = new BooleanSetting(FILL_MAIN_BOX_SETTING_ID, false);

        ModeSetting modeSetting = new ModeSetting(MAIN_BOX_MODE_SETTING_ID);
        String[] mainBoxModes = new String[2];

        mainBoxModes[0] = "Full";

        mainBoxModes[1] = "Corner";
        ModeSetting modeSetting2 = modeSetting.addModes(mainBoxModes);
        this.mainBoxMode = modeSetting2.setDefault("Full");

        this.fillMainBoxColor =
                new ColorSetting(FILL_MAIN_BOX_COLOR_SETTING_ID, new Color(0, 0, 0, 150));

        this.mainBoxGroup =
                new SettingGroup(MAIN_BOX_SETTING_ID)
                        .addValues(
                                this.mainBox,
                                this.mainBoxShadow,
                                this.fillMainBox,
                                this.mainBoxMode,
                                this.fillMainBoxColor);

        this.healthBar = new BooleanSetting(HEALTH_BAR_TREE_SETTING_ID, true);

        this.healthBarShadow = new BooleanSetting(HEALTH_BAR_SHADOW_SETTING_ID, true);

        ModeSetting modeSetting3 = new ModeSetting(HEALTH_BAR_ORIENTATION_SETTING_ID);
        String[] orientationModes = new String[2];

        orientationModes[0] = "Horizontal";

        orientationModes[1] = "Vertical";
        ModeSetting modeSetting4 = modeSetting3.addModes(orientationModes);
        this.healthBarOrientation = modeSetting4.setDefault("Vertical");

        this.healthBarGroup =
                new SettingGroup(HEALTH_BAR_GROUP_ID)
                        .addValues(this.healthBar, this.healthBarShadow, this.healthBarOrientation);

        this.mainColor = new ColorSetting(MAIN_COLOR_SETTING_ID, Color.white);

        this.healthBarColor = new ColorSetting(HEALTH_BAR_COLOR_SETTING_ID, Color.GREEN);

        this.colorGroup =
                new SettingGroup(COLOR_GROUP_ID).addValues(this.mainColor, this.healthBarColor);

        this.glow = new BooleanSetting(GLOW_SETTING_ID, false);

        this.glowColor = new ColorSetting(COLOR_GROUP_ID, Color.CYAN);

        this.glowRadius = new NumberSetting(RADIUS_SETTING_ID, 2.0F, 0.1F, 20.0F, 0.1F);

        this.glowIntensity = new NumberSetting(GLOW_INTENSITY_SETTING_ID, 1.5F, 0.1F, 5.0F, 0.1F);

        this.glowOverlay = new BooleanSetting(GLOW_OVERLAY_SETTING_ID, false);

        ModeSetting modeSetting5 = new ModeSetting(GRADIENT_SETTING_ID);
        String[] gradientModes = new String[3];

        gradientModes[0] = "Vertical";

        gradientModes[1] = "Horizontal";

        gradientModes[2] = "Diagonal";
        ModeSetting modeSetting6 = modeSetting5.addModes(gradientModes);
        this.gradient = modeSetting6.setDefault("Vertical");

        this.gradientSpeed = new NumberSetting(GRADIENT_SPEED_SETTING_ID, 0.5F, 0.0F, 5.0F, 0.1F);

        this.outline = new BooleanSetting(OUTLINE_SETTING_ID, true);

        this.outlineThickness =
                new NumberSetting(OUTLINE_THICKNESS_SETTING_ID, 1.5F, 0.5F, 5.0F, 0.1F);
        ModeSetting modeSetting7 = new ModeSetting(OUTLINE_COLOR_SETTING_ID);
        String[] outlineColorModes = new String[2];

        outlineColorModes[0] = "Custom";

        outlineColorModes[1] = "Same";
        ModeSetting modeSetting8 = modeSetting7.addModes(outlineColorModes);

        this.outlineColorMode = modeSetting8.setDefault("Same");
        this.outlineColor = new ColorSetting(COLOR_GROUP_ID, Color.WHITE);

        this.chams = new BooleanSetting(CHAMS_SETTING_ID, false);

        this.chamsColor = new ColorSetting(COLOR_GROUP_ID, new Color(255, 50, 50, 180));

        this.chamsMix = new NumberSetting(CHAMS_MIX_SETTING_ID, 0.5F, 0.0F, 1.0F, 0.05F);

        this.downsample = new NumberSetting(DOWNSAMPLE_SETTING_ID, 0.5F, 0.1F, 1.0F, 0.05F);

        this.skeleton = new BooleanSetting(SKELETON_SETTING_ID, false);

        this.skeletonColor = new ColorSetting(SKELETON_COLOR_SETTING_ID, Color.WHITE);

        this.murderColor = new ColorSetting(MURDER_COLOR_SETTING_ID, Color.RED);

        this.skeletonThickness =
                new NumberSetting(SKELETON_THICKNESS_SETTING_ID, 1.5F, 0.5F, 5.0F, 0.1F);

        this.skeletonGroup =
                new SettingGroup(SKELETON_SETTING_ID)
                        .addValues(this.skeleton, this.skeletonColor, this.skeletonThickness);
        SettingGroup settingGroup =
                new SettingGroup(SETTING_2_D_SETTING_ID)
                        .addValues(
                                this.esp2D,
                                this.mainBoxGroup,
                                this.healthBarGroup,
                                this.colorGroup);
        SettingGroup settingGroup2 =
                new SettingGroup(OUTLINE_SETTING_ID)
                        .addValues(
                                this.outline,
                                this.outlineThickness,
                                this.outlineColorMode,
                                this.outlineColor);
        SettingGroup settingGroup3 =
                new SettingGroup(CHAMS_SETTING_ID)
                        .addValues(this.chams, this.chamsColor, this.chamsMix);
        SettingGroup settingGroup4 = new SettingGroup(GLOW_SETTING_ID);

        Setting[] glowSettings =
                new Setting[] {
                    this.glow,
                    this.glowColor,
                    this.glowRadius,
                    this.glowIntensity,
                    this.glowOverlay,
                    this.gradient,
                    null,
                    null
                };

        glowSettings[6] = this.gradientSpeed;

        glowSettings[7] = this.downsample;
        SettingGroup settingGroup5 = settingGroup4.addValues(glowSettings);
        this.glowColor.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.glowRadius.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.glowIntensity.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.glowOverlay.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.gradient.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.gradientSpeed.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.downsample.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.mainBoxShadow.hideIf(
                () -> {
                    return !this.mainBox.getValue();
                });
        this.fillMainBox.hideIf(
                () -> {
                    return !this.mainBox.getValue();
                });
        this.mainBoxMode.hideIf(
                () -> {
                    return !this.mainBox.getValue();
                });
        this.fillMainBoxColor.hideIf(
                () -> {
                    return !this.mainBox.getValue() || !this.fillMainBox.getValue();
                });
        this.healthBarShadow.hideIf(
                () -> {
                    return !this.healthBar.getValue();
                });
        this.healthBarOrientation.hideIf(
                () -> {
                    return !this.healthBar.getValue();
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
        this.chamsColor.hideIf(
                () -> {
                    return !this.chams.getValue();
                });
        this.chamsMix.hideIf(
                () -> {
                    return !this.chams.getValue();
                });
        this.skeletonColor.hideIf(
                () -> {
                    return !this.skeleton.getValue();
                });
        this.skeletonThickness.hideIf(
                () -> {
                    return !this.skeleton.getValue();
                });
        ModeSetting modeSetting9 = this.mainColor.getColorMode();

        modeSetting9.setDefault("Client");
        ModeSetting modeSetting10 = this.healthBarColor.getColorMode();

        modeSetting10.setDefault("Client");
        ModeSetting modeSetting11 = this.glowColor.getColorMode();

        modeSetting11.setDefault("Client");
        ModeSetting modeSetting12 = this.chamsColor.getColorMode();

        modeSetting12.setDefault("Client");
        ModeSetting modeSetting13 = this.skeletonColor.getColorMode();

        modeSetting13.setDefault("Client");
        this.addSettings(
                settingGroup, settingGroup5, settingGroup2, settingGroup3, this.skeletonGroup);
    }

    public BooleanSetting getGlow() {
        return this.glow;
    }

    private PlayerPoseSnapshot createPoseSnapshot(Player player2, float partialTick) {
        PlayerPoseSnapshot snapshot = new PlayerPoseSnapshot();
        snapshot.x = Mth.lerp(partialTick, player2.xOld, player2.getX());
        snapshot.y = Mth.lerp(partialTick, player2.yOld, player2.getY());
        snapshot.z = Mth.lerp(partialTick, player2.zOld, player2.getZ());
        snapshot.scale = player2.getScale();
        snapshot.bodyRot = Mth.rotLerp(partialTick, player2.yBodyRotO, player2.yBodyRot);
        float headYaw =
                Mth.rotLerp(partialTick, player2.yHeadRotO, player2.yHeadRot) - snapshot.bodyRot;
        float headPitch = Mth.lerp(partialTick, player2.xRotO, player2.getXRot());
        snapshot.headPose =
                new ModelPartPose(
                        0.0F,
                        0.0F,
                        0.0F,
                        (float) Math.toRadians(headPitch),
                        (float) Math.toRadians(headYaw),
                        0.0F);
        float walkPosition = player2.walkAnimation.position(partialTick);
        float walkSpeed = player2.walkAnimation.speed(partialTick);
        snapshot.bodyPose = ModelPartPose.ZERO;
        snapshot.rightArmPose =
                new ModelPartPose(
                        -5.0F,
                        2.0F,
                        0.0F,
                        Mth.cos(walkPosition * 0.6662F + (float) Math.PI) * 2.0F * walkSpeed * 0.5F,
                        0.0F,
                        0.0F);
        snapshot.leftArmPose =
                new ModelPartPose(
                        5.0F,
                        2.0F,
                        0.0F,
                        Mth.cos(walkPosition * 0.6662F) * 2.0F * walkSpeed * 0.5F,
                        0.0F,
                        0.0F);
        snapshot.rightLegPose =
                new ModelPartPose(
                        -1.9F,
                        12.0F,
                        0.0F,
                        Mth.cos(walkPosition * 0.6662F) * 1.4F * walkSpeed,
                        0.0F,
                        0.0F);
        snapshot.leftLegPose =
                new ModelPartPose(
                        1.9F,
                        12.0F,
                        0.0F,
                        Mth.cos(walkPosition * 0.6662F + (float) Math.PI) * 1.4F * walkSpeed,
                        0.0F,
                        0.0F);
        snapshot.captureTime = System.currentTimeMillis();
        return snapshot;
    }

    @EventTarget(0)
    public void onPreRender2D(PreRender2DEvent event) {
        long timestamp;
        boolean glowEnabled = this.glow.getValue();
        boolean outlineEnabled = this.outline.getValue();
        boolean chamsEnabled = this.chams.getValue();
        if ((glowEnabled || outlineEnabled || chamsEnabled)
                && event.getContext().getEntityGlowRenderer() != null) {
            RenderTarget renderTarget =
                    ((LevelRendererAccessor) mc.levelRenderer).getEntityOutlineTarget();
            if (renderTarget != null) {
                if (!glowEnabled && !outlineEnabled && chamsEnabled) {
                    EntityGlowRenderer glowRenderer = event.getContext().getEntityGlowRenderer();
                    GpuTexture gpuTexture = renderTarget.getColorTexture();
                    Color color = this.getChamsColor(true);
                    Color color2 = this.getChamsColor(false);
                    float mixAmount = this.chamsMix.getValue();
                    float gradientSpeedValue = this.gradientSpeed.getValue();
                    float gradientAngle = this.getGradientAngle();
                    long currentTime = System.currentTimeMillis();

                    glowRenderer.renderChamsOnly(
                            gpuTexture,
                            color,
                            color2,
                            mixAmount,
                            gradientSpeedValue,
                            gradientAngle,
                            (float) (currentTime % 100000L) / 1000.0F,
                            mc.gameRenderer.mainRenderTarget());
                } else {
                    Color color3 = this.glowColor.getValue();
                    Color color4 = this.glowColor.getSecondaryColor();
                    Color color5;
                    Color color6;
                    if (!outlineEnabled) {
                        color5 = color6 = new Color(0, 0, 0, 0);
                    } else {

                        if (this.outlineColorMode.is("Custom")) {
                            color5 = this.outlineColor.getValue();
                            color6 = this.outlineColor.getSecondaryColor();
                        } else {
                            color5 = this.glowColor.getValue();
                            color6 = this.glowColor.getSecondaryColor();
                        }
                    }

                    float mixAmount = chamsEnabled ? this.chamsMix.getValue() : 0.0F;
                    Color color7 = chamsEnabled ? this.getChamsColor(true) : Color.BLACK;
                    Color color8 = chamsEnabled ? this.getChamsColor(false) : Color.BLACK;
                    float gradientAngle = this.getGradientAngle();
                    timestamp = System.currentTimeMillis();

                    float animationTime = (float) (timestamp % 100000L) / 1000.0F;
                    event.getContext()
                            .getEntityGlowRenderer()
                            .renderEnhanced(
                                    renderTarget.getColorTexture(),
                                    color3,
                                    color4,
                                    color5,
                                    color6,
                                    this.glowRadius.getValue(),
                                    outlineEnabled ? this.outlineThickness.getValue() : 0.0F,
                                    glowEnabled ? this.glowIntensity.getValue() : 0.0F,
                                    glowEnabled && this.glowOverlay.getValue(),
                                    this.gradientSpeed.getValue(),
                                    gradientAngle,
                                    animationTime,
                                    mixAmount,
                                    color7,
                                    color8,
                                    mc.gameRenderer.mainRenderTarget(),
                                    mc.getWindow().getWidth(),
                                    mc.getWindow().getHeight(),
                                    this.downsample.getValue());
                }
            }
        }
    }

    private void drawFilledRect(
            Render2DContext context,
            float left,
            float top,
            float right,
            float bottom,
            Color color) {
        context.drawRect(
                RoundedRectShape.obtain()
                        .setBounds(left, top, right - left, bottom - top)
                        .setColor(color));
    }

    private boolean drawBone(
            Render2DContext context,
            Vec3 vector3,
            Vec3 vector32,
            ColorSetting colorSetting,
            float thickness) {
        if (vector3 != null && vector32 != null) {
            Vec3 vector33 = ScreenProjector.worldToScreen(vector3);
            Vec3 vector34 = ScreenProjector.worldToScreen(vector32);
            if (vector33.z == -1.0 && vector34.z == -1.0) {
                return false;
            }

            Vec3 vector35 = vector3;
            Vec3 vector36 = vector32;
            if (vector33.z == -1.0) {
                int i = 0;

                while (true) {

                    if (i >= 6) {
                        vector33 = ScreenProjector.worldToScreen(vector36);
                        vector34 = ScreenProjector.worldToScreen(vector32);
                        break;
                    }

                    Vec3 vector37 = vector35.add(vector36).scale(0.5);
                    if (ScreenProjector.worldToScreen(vector37).z == -1.0) {
                        vector35 = vector37;
                    } else {
                        vector36 = vector37;
                    }

                    i++;
                }
            } else if (vector34.z == -1.0) {
                int i = 0;

                while (true) {

                    if (i >= 6) {
                        vector33 = ScreenProjector.worldToScreen(vector3);
                        vector34 = ScreenProjector.worldToScreen(vector35);
                        break;
                    }

                    Vec3 vector38 = vector35.add(vector36).scale(0.5);
                    if (ScreenProjector.worldToScreen(vector38).z == -1.0) {
                        vector36 = vector38;
                    } else {
                        vector35 = vector38;
                    }

                    i++;
                }
            }

            if (vector33.z != -1.0 && vector34.z != -1.0) {
                float startX = (float) vector33.x;
                float startY = (float) vector33.y;
                float endX = (float) vector34.x;
                float endY = (float) vector34.y;
                float dx = endX - startX;
                float dy = endY - startY;
                float length = (float) Math.hypot(dx, dy);
                if (length <= 0.001F) {
                    return false;
                }

                float angleDegrees = (float) Math.toDegrees(Math.atan2(dy, dx));
                float halfThickness = thickness / 2.0F;
                float normalOffset = dx / length * halfThickness;
                context.pushPose();
                context.translate(startX, startY);
                context.rotateZ(angleDegrees);
                RoundedRectShape shape =
                        RoundedRectShape.obtain()
                                .setBounds(0.0F, -halfThickness, length, thickness)
                                .setRadius(halfThickness)
                                .setColor(colorSetting.getValue());
                if (this.isGradientColor(colorSetting)) {
                    shape.setPaint(
                            ShapePaint.obtain()
                                    .setQuadGradient(
                                            this.getGradientColorAt(
                                                    colorSetting, startY - normalOffset),
                                            this.getGradientColorAt(
                                                    colorSetting, endY - normalOffset),
                                            this.getGradientColorAt(
                                                    colorSetting, endY + normalOffset),
                                            this.getGradientColorAt(
                                                    colorSetting, startY + normalOffset)));
                }

                context.drawRect(shape);
                context.popPose();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void drawBones(
            Render2DContext context, ColorSetting colorSetting, float thickness, Vec3... points) {

        for (int i = 0; i < points.length; i += 2) {
            this.drawBone(context, points[i], points[i + 1], colorSetting, thickness);
        }
    }

    private void drawColoredBar(
            Render2DContext context,
            float left,
            float top,
            float right,
            float bottom,
            boolean vertical,
            boolean useHealthColor) {
        if (this.targetIsMurderer) {
            this.drawFilledRect(context, left, top, right, bottom, Color.RED);
        } else {
            ColorSetting colorSetting = useHealthColor ? this.healthBarColor : this.mainColor;
            RoundedRectShape shape =
                    RoundedRectShape.obtain()
                            .setBounds(left, top, right - left, bottom - top)
                            .setColor(colorSetting.getValue());
            if (colorSetting.isGradientMode()) {
                int startColor = colorSetting.getValue().getRGB();
                int endColor = colorSetting.getSecondaryColor().getRGB();
                shape.setPaint(
                        vertical
                                ? ShapePaint.obtain().setVerticalGradient(startColor, endColor)
                                : ShapePaint.obtain().setHorizontalGradient(startColor, endColor));
            }

            context.drawRect(shape);
        }
    }

    private Vec3 transformPoint(Matrix4f matrix4f, float x, float y, float z) {
        Vector4f vector4f = new Vector4f(x, y, z, 1.0F);
        matrix4f.transform(vector4f);
        return new Vec3(vector4f.x, vector4f.y, vector4f.z);
    }

    private float getGradientAngle() {

        if (this.gradient.is("Horizontal")) {
            return 0.0F;
        }

        return this.gradient.is("Diagonal") ? (float) (Math.PI / 4) : (float) (Math.PI / 2);
    }

    @EventTarget
    public void onEntityGlowCheck(EntityGlowCheckEvent event) {
        if (this.glow.getValue() || this.outline.getValue() || this.chams.getValue()) {
            if (event.getEntity() instanceof Player player2) {
                if (player2.isDeadOrDying()) {
                    return;
                }

                if (AntiBot.isBot(player2)) {
                    return;
                }

                event.setCancelled(true);
            }
        }
    }

    private void drawJoints(
            Render2DContext context, ColorSetting colorSetting, float radius, Vec3... joints) {

        for (Vec3 vector3 : joints) {
            this.drawJoint(context, vector3, colorSetting, radius);
        }
    }

    public boolean isEntityEffectActive() {
        return this.isEnabled()
                && (this.glow.getValue() || this.outline.getValue() || this.chams.getValue());
    }

    private boolean isGradientColor(ColorSetting colorSetting) {
        return colorSetting.isGradientMode() || colorSetting.isClientMode();
    }

    public static void capturePoseSnapshot(
            HumanoidModel<?> model, AvatarRenderState avatarRenderState) {
        PlayerPoseSnapshot snapshot = new PlayerPoseSnapshot();
        snapshot.x = avatarRenderState.x;
        snapshot.y = avatarRenderState.y;
        snapshot.z = avatarRenderState.z;
        snapshot.scale = avatarRenderState.scale;
        snapshot.bodyRot = avatarRenderState.bodyRot;
        snapshot.deathTime = avatarRenderState.deathTime;
        snapshot.pose = avatarRenderState.pose;
        snapshot.bedOrientation = avatarRenderState.bedOrientation;
        snapshot.upsideDown = avatarRenderState.isUpsideDown;
        snapshot.xRot = avatarRenderState.xRot;
        snapshot.swimAmount = avatarRenderState.swimAmount;
        snapshot.inWater = avatarRenderState.isInWater;
        snapshot.visuallySwimming = avatarRenderState.isVisuallySwimming;
        snapshot.fallFlying = avatarRenderState.isFallFlying;
        snapshot.autoSpinAttack = avatarRenderState.isAutoSpinAttack;
        snapshot.fallFlyingScale = avatarRenderState.fallFlyingScale();
        snapshot.applyFlyingYRot = avatarRenderState.shouldApplyFlyingYRot;
        snapshot.flyingYRot = avatarRenderState.flyingYRot;
        snapshot.headPose = ModelPartPose.fromModelPart(model.head);
        snapshot.bodyPose = ModelPartPose.fromModelPart(model.body);
        snapshot.rightArmPose = ModelPartPose.fromModelPart(model.rightArm);
        snapshot.leftArmPose = ModelPartPose.fromModelPart(model.leftArm);
        snapshot.rightLegPose = ModelPartPose.fromModelPart(model.rightLeg);
        snapshot.leftLegPose = ModelPartPose.fromModelPart(model.leftLeg);
        snapshot.captureTime = System.currentTimeMillis();
        poseSnapshots.put(avatarRenderState.id, snapshot);
    }

    @EventTarget(0)
    public void onRender2D(Render2DEvent render2DEvent) {
        if (mc.getEntityRenderDispatcher().camera != null
                && (this.esp2D.getValue() || this.skeleton.getValue())) {
            Render2DContext context = render2DEvent.getContext();

            for (Entity entity2 : mc.level.entitiesForRendering()) {
                if (entity2 instanceof LivingEntity livingEntity
                        && !livingEntity.isDeadOrDying()
                        && entity2 instanceof Player player2
                        && !AntiBot.isBot(player2)
                        && (player2 != mc.player || !mc.options.getCameraType().isFirstPerson())) {
                    this.targetIsMurderer = MurderMystery.isMurderer(player2);
                    AABB boundingBox = entity2.getBoundingBox();
                    if (ScreenProjector.getFrustum() == null
                            || ScreenProjector.getFrustum().isVisible(boundingBox)) {
                        Vector4d screenBounds = ScreenProjector.getEntityScreenBounds(entity2);
                        float boxScale =
                                screenBounds == null ? 1.0F : this.getBoxScale(screenBounds);
                        if (this.esp2D.getValue() && screenBounds != null) {
                            float left = (float) screenBounds.x;
                            float top = (float) screenBounds.y;
                            float right = (float) screenBounds.z;
                            float bottom = (float) screenBounds.w;
                            float shadowPadding = 0.5F * boxScale;
                            float edgeThickness = boxScale;
                            float horizontalBarGap = 2.0F * boxScale;
                            if (this.fillMainBox.getValue()) {
                                this.drawFilledRect(
                                        context,
                                        left,
                                        top,
                                        right,
                                        bottom,
                                        this.targetIsMurderer
                                                ? Color.RED
                                                : this.fillMainBoxColor.getValue());
                            }

                            if (this.mainBox.getValue()) {

                                if (this.mainBoxMode.is("Full")) {
                                    if (this.mainBoxShadow.getValue()) {
                                        this.drawFilledRect(
                                                context,
                                                left - edgeThickness - shadowPadding,
                                                top - shadowPadding,
                                                left + shadowPadding,
                                                bottom + shadowPadding,
                                                Color.BLACK);
                                        this.drawFilledRect(
                                                context,
                                                right - edgeThickness - shadowPadding,
                                                top - shadowPadding,
                                                right + shadowPadding,
                                                bottom + shadowPadding,
                                                Color.BLACK);
                                        this.drawFilledRect(
                                                context,
                                                left - edgeThickness - shadowPadding,
                                                top - shadowPadding,
                                                right + shadowPadding,
                                                top + edgeThickness + shadowPadding,
                                                Color.BLACK);
                                        this.drawFilledRect(
                                                context,
                                                left - edgeThickness - shadowPadding,
                                                bottom - edgeThickness - shadowPadding,
                                                right + shadowPadding,
                                                bottom + shadowPadding,
                                                Color.BLACK);
                                    }

                                    this.drawBoxEdge(
                                            context, left - edgeThickness, top, left, bottom, true);
                                    this.drawBoxEdge(
                                            context,
                                            left - edgeThickness,
                                            bottom - edgeThickness,
                                            right,
                                            bottom,
                                            false);
                                    this.drawBoxEdge(
                                            context,
                                            left - edgeThickness,
                                            top,
                                            right,
                                            top + edgeThickness,
                                            false);
                                    this.drawBoxEdge(
                                            context,
                                            right - edgeThickness,
                                            top,
                                            right,
                                            bottom,
                                            true);
                                } else {
                                    this.drawCornerBox(
                                            context,
                                            left,
                                            top,
                                            right,
                                            bottom,
                                            shadowPadding,
                                            boxScale);
                                }
                            }

                            float healthFraction = player2.getHealth() / player2.getMaxHealth();
                            float healthY = bottom + (top - bottom) * healthFraction;
                            float healthX = left + (right - left) * healthFraction;
                            float barThickness = boxScale;
                            float verticalBarGap = 3.0F * boxScale;
                            if (this.healthBar.getValue()) {
                                if (this.healthBarShadow.getValue()) {

                                    if (this.healthBarOrientation.is("Vertical")) {
                                        this.drawFilledRect(
                                                context,
                                                left
                                                        - verticalBarGap
                                                        - barThickness
                                                        - shadowPadding,
                                                top - shadowPadding,
                                                left - verticalBarGap + shadowPadding,
                                                bottom + shadowPadding,
                                                Color.BLACK);
                                    } else {
                                        this.drawFilledRect(
                                                context,
                                                left - shadowPadding,
                                                bottom + horizontalBarGap - shadowPadding,
                                                right + shadowPadding,
                                                bottom
                                                        + horizontalBarGap
                                                        + barThickness
                                                        + shadowPadding,
                                                Color.BLACK);
                                    }
                                }

                                if (this.healthBarOrientation.is("Vertical")) {
                                    this.drawColoredBar(
                                            context,
                                            left - verticalBarGap - barThickness,
                                            healthY,
                                            left - verticalBarGap,
                                            bottom,
                                            true,
                                            true);
                                } else {
                                    this.drawColoredBar(
                                            context,
                                            left,
                                            bottom + horizontalBarGap,
                                            healthX,
                                            bottom + horizontalBarGap + barThickness,
                                            false,
                                            true);
                                }
                            }
                        }

                        if (this.skeleton.getValue()) {
                            this.drawSkeleton(context, player2, boxScale);
                        }

                        this.targetIsMurderer = false;
                    }
                }
            }
        }
    }

    private float getBedRotation(Direction direction, float defaultRotation) {
        if (direction == null) {
            return defaultRotation;
        }

        return switch (EspDirectionSwitchMap.DIRECTION_ORDINALS[direction.ordinal()]) {
            case 1 -> 90.0F;
            case 2 -> 0.0F;
            case 3 -> 270.0F;
            case 4 -> 180.0F;
            default -> 0.0F;
        };
    }

    private void drawBoxEdge(
            Render2DContext context,
            float left,
            float top,
            float right,
            float bottom,
            boolean vertical) {
        this.drawColoredBar(context, left, top, right, bottom, vertical, false);
    }

    private int getGradientColorAt(ColorSetting colorSetting, float y) {
        float range = Math.max(1.0F, this.skeletonMaxY - this.skeletonMinY);
        float blend = Math.clamp((y - this.skeletonMinY) / range, 0.0F, 1.0F);
        return ColorUtil.lerpColor(colorSetting.getValue(), colorSetting.getSecondaryColor(), blend)
                .getRGB();
    }

    private void drawCornerBox(
            Render2DContext context,
            float left,
            float top,
            float right,
            float bottom,
            float shadowPadding,
            float edgeThickness) {
        float boxWidth = right - left;
        float boxHeight = bottom - top;
        float cornerLength = Math.max(Math.min(boxWidth, boxHeight) * 0.25F, 4.0F * shadowPadding);
        if (this.mainBoxShadow.getValue()) {
            this.drawFilledRect(
                    context,
                    left - edgeThickness - shadowPadding,
                    top - shadowPadding,
                    left + shadowPadding,
                    top + cornerLength + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    left - edgeThickness - shadowPadding,
                    top - shadowPadding,
                    left + cornerLength + shadowPadding,
                    top + edgeThickness + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    right - shadowPadding,
                    top - shadowPadding,
                    right + edgeThickness + shadowPadding,
                    top + cornerLength + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    right - cornerLength - shadowPadding,
                    top - shadowPadding,
                    right + edgeThickness + shadowPadding,
                    top + edgeThickness + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    left - edgeThickness - shadowPadding,
                    bottom - cornerLength - shadowPadding,
                    left + shadowPadding,
                    bottom + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    left - edgeThickness - shadowPadding,
                    bottom - edgeThickness - shadowPadding,
                    left + cornerLength + shadowPadding,
                    bottom + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    right - shadowPadding,
                    bottom - cornerLength - shadowPadding,
                    right + edgeThickness + shadowPadding,
                    bottom + shadowPadding,
                    Color.BLACK);
            this.drawFilledRect(
                    context,
                    right - cornerLength - shadowPadding,
                    bottom - edgeThickness - shadowPadding,
                    right + edgeThickness + shadowPadding,
                    bottom + shadowPadding,
                    Color.BLACK);
        }

        this.drawBoxEdge(context, left - edgeThickness, top, left, top + cornerLength, true);
        this.drawBoxEdge(
                context,
                left - edgeThickness,
                top,
                left + cornerLength,
                top + edgeThickness,
                false);
        this.drawBoxEdge(context, right, top, right + edgeThickness, top + cornerLength, true);
        this.drawBoxEdge(
                context,
                right - cornerLength,
                top,
                right + edgeThickness,
                top + edgeThickness,
                false);
        this.drawBoxEdge(context, left - edgeThickness, bottom - cornerLength, left, bottom, true);
        this.drawBoxEdge(
                context,
                left - edgeThickness,
                bottom - edgeThickness,
                left + cornerLength,
                bottom,
                false);
        this.drawBoxEdge(
                context, right, bottom - cornerLength, right + edgeThickness, bottom, true);
        this.drawBoxEdge(
                context,
                right - cornerLength,
                bottom - edgeThickness,
                right + edgeThickness,
                bottom,
                false);
    }

    private void drawJoint(
            Render2DContext context, Vec3 vector3, ColorSetting colorSetting, float radius) {
        Vec3 vector32 = ScreenProjector.worldToScreen(vector3);
        if (vector32.z != -1.0) {
            RoundedRectShape shape =
                    RoundedRectShape.obtain()
                            .setBounds(
                                    (float) vector32.x - radius,
                                    (float) vector32.y - radius,
                                    radius * 2.0F,
                                    radius * 2.0F)
                            .setRadius(radius)
                            .setColor(colorSetting.getValue());
            if (this.isGradientColor(colorSetting)) {
                shape.setPaint(
                        ShapePaint.obtain()
                                .setQuadGradient(
                                        this.getGradientColorAt(
                                                colorSetting, (float) vector32.y - radius),
                                        this.getGradientColorAt(
                                                colorSetting, (float) vector32.y - radius),
                                        this.getGradientColorAt(
                                                colorSetting, (float) vector32.y + radius),
                                        this.getGradientColorAt(
                                                colorSetting, (float) vector32.y + radius)));
            }

            context.drawRect(shape);
        }
    }

    private float getBoxScale(Vector4d bounds) {
        double height = bounds.w - bounds.y;
        return (float) Math.clamp(height / 100.0, 0.25, 1.5);
    }

    private Matrix4f applyPartTransform(Matrix4f matrix4f, ModelPartPose pose) {
        Matrix4f matrix4f2 = new Matrix4f(matrix4f);
        matrix4f2.translate(pose.x / 16.0F, pose.y / 16.0F, pose.z / 16.0F);
        if (pose.zRot != 0.0F || pose.yRot != 0.0F || pose.xRot != 0.0F) {
            matrix4f2.rotateZYX(pose.zRot, pose.yRot, pose.xRot);
        }

        return matrix4f2;
    }

    private void drawSkeleton(Render2DContext context, Player player2, float boxScale) {
        PlayerPoseSnapshot snapshot;
        label117:
        {
            long age;
            snapshot = poseSnapshots.get(player2.getId());
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            if (snapshot != null) {
                age = System.currentTimeMillis() - snapshot.captureTime;

                if (age <= 1000L) {
                    break label117;
                }
            }

            snapshot = this.createPoseSnapshot(player2, partialTick);
        }

        Matrix4f matrix4f = new Matrix4f();
        matrix4f.translate((float) snapshot.x, (float) snapshot.y, (float) snapshot.z);
        matrix4f.scale(snapshot.scale, snapshot.scale, snapshot.scale);
        if (snapshot.pose == Pose.SLEEPING) {
            matrix4f.rotateY(
                    (float)
                            Math.toRadians(
                                    this.getBedRotation(
                                            snapshot.bedOrientation, snapshot.bodyRot)));
            matrix4f.rotateZ((float) Math.toRadians(90.0));
            matrix4f.rotateY((float) Math.toRadians(270.0));
        } else {
            matrix4f.rotateY((float) Math.toRadians(180.0F - snapshot.bodyRot));
        }

        if (snapshot.pose != Pose.SLEEPING) {
            if (snapshot.deathTime > 0.0F) {
                float deathProgress = (snapshot.deathTime - 1.0F) / 20.0F * 1.6F;
                deathProgress = Mth.sqrt(Math.min(deathProgress, 1.0F));
                matrix4f.rotateZ((float) Math.toRadians(deathProgress * 90.0F));
            } else if (snapshot.fallFlying) {
                if (!snapshot.autoSpinAttack) {
                    matrix4f.rotateX(
                            (float)
                                    Math.toRadians(
                                            snapshot.fallFlyingScale * (-90.0F - snapshot.xRot)));
                }

                if (snapshot.applyFlyingYRot) {
                    matrix4f.rotateY(snapshot.flyingYRot);
                }
            } else if (snapshot.swimAmount > 0.0F || player2.getPose() == Pose.SWIMMING) {
                float swimPitch = snapshot.inWater ? -90.0F - snapshot.xRot : -90.0F;
                float swimFactor = snapshot.swimAmount > 0.0F ? snapshot.swimAmount : 1.0F;
                matrix4f.rotateX((float) Math.toRadians(Mth.lerp(swimFactor, 0.0F, swimPitch)));
                if (snapshot.visuallySwimming || player2.getPose() == Pose.SWIMMING) {
                    matrix4f.translate(0.0F, -1.0F, 0.3F);
                }
            }
        }

        if (snapshot.upsideDown && snapshot.pose != Pose.SLEEPING) {
            matrix4f.translate(0.0F, (player2.getBbHeight() + 0.1F) / snapshot.scale, 0.0F);
            matrix4f.rotateZ((float) Math.toRadians(180.0));
        }

        matrix4f.scale(-1.0F, -1.0F, 1.0F);
        matrix4f.translate(0.0F, -1.501F, 0.0F);

        Matrix4f matrix4f2 = this.applyPartTransform(matrix4f, snapshot.headPose);
        Matrix4f matrix4f3 = this.applyPartTransform(matrix4f, snapshot.bodyPose);
        Matrix4f matrix4f4 = this.applyPartTransform(matrix4f, snapshot.leftArmPose);
        Matrix4f matrix4f5 = this.applyPartTransform(matrix4f, snapshot.rightArmPose);
        Matrix4f matrix4f6 = this.applyPartTransform(matrix4f, snapshot.leftLegPose);
        Matrix4f matrix4f7 = this.applyPartTransform(matrix4f, snapshot.rightLegPose);
        Vec3 vector3 = this.transformPoint(matrix4f2, 0.0F, 0.0F, 0.0F);
        Vec3 vector32 = this.transformPoint(matrix4f3, 0.0F, 0.125F, 0.0F);
        Vec3 vector33 = this.transformPoint(matrix4f3, 0.0F, 0.75F, 0.0F);
        Vec3 vector34 = this.transformPoint(matrix4f4, 0.0F, 0.0F, 0.0F);
        Vec3 vector35 = this.transformPoint(matrix4f4, 0.0F, 0.375F, 0.0F);
        Vec3 vector36 = this.transformPoint(matrix4f4, 0.0F, 0.625F, 0.0F);
        Vec3 vector37 = this.transformPoint(matrix4f5, 0.0F, 0.0F, 0.0F);
        Vec3 vector38 = this.transformPoint(matrix4f5, 0.0F, 0.375F, 0.0F);
        Vec3 vector39 = this.transformPoint(matrix4f5, 0.0F, 0.625F, 0.0F);
        Vec3 vector310 = this.transformPoint(matrix4f6, 0.0F, 0.0F, 0.0F);
        Vec3 vector311 = this.transformPoint(matrix4f6, 0.0F, 0.375F, 0.0F);
        Vec3 vector312 = this.transformPoint(matrix4f6, 0.0F, 0.75F, 0.0F);
        Vec3 vector313 = this.transformPoint(matrix4f7, 0.0F, 0.0F, 0.0F);
        Vec3 vector314 = this.transformPoint(matrix4f7, 0.0F, 0.375F, 0.0F);
        Vec3 vector315 = this.transformPoint(matrix4f7, 0.0F, 0.75F, 0.0F);
        this.skeletonMinY = Float.MAX_VALUE;
        this.skeletonMaxY = -Float.MAX_VALUE;
        Vec3[] projectionPoints =
                new Vec3[] {
                    vector3, vector32, vector33, vector34, vector35, vector36, null, null, null,
                    null, null, null, null, null, null
                };

        projectionPoints[6] = vector37;

        projectionPoints[7] = vector38;

        projectionPoints[8] = vector39;

        projectionPoints[9] = vector310;

        projectionPoints[10] = vector311;

        projectionPoints[11] = vector312;

        projectionPoints[12] = vector313;

        projectionPoints[13] = vector314;

        projectionPoints[14] = vector315;

        for (Vec3 vector316 : projectionPoints) {
            Vec3 vector317 = ScreenProjector.worldToScreen(vector316);
            if (vector317.z != -1.0) {
                this.skeletonMinY = Math.min(this.skeletonMinY, (float) vector317.y);
                this.skeletonMaxY = Math.max(this.skeletonMaxY, (float) vector317.y);
            }
        }

        if (this.skeletonMinY == Float.MAX_VALUE || this.skeletonMaxY - this.skeletonMinY < 1.0F) {
            this.skeletonMinY = 0.0F;
            this.skeletonMaxY = mc.getWindow().getGuiScaledHeight();
        }

        float boneThickness = this.skeletonThickness.getValue() * boxScale;
        ColorSetting colorSetting = this.targetIsMurderer ? this.murderColor : this.skeletonColor;

        Vec3[] bonePoints =
                new Vec3[] {
                    vector3, vector32, vector32, vector33, vector32, vector34, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null
                };

        bonePoints[6] = vector34;

        bonePoints[7] = vector35;

        bonePoints[8] = vector35;

        bonePoints[9] = vector36;

        bonePoints[10] = vector32;

        bonePoints[11] = vector37;

        bonePoints[12] = vector37;

        bonePoints[13] = vector38;

        bonePoints[14] = vector38;

        bonePoints[15] = vector39;

        bonePoints[16] = vector33;

        bonePoints[17] = vector310;

        bonePoints[18] = vector310;

        bonePoints[19] = vector311;

        bonePoints[20] = vector311;

        bonePoints[21] = vector312;

        bonePoints[22] = vector33;

        bonePoints[23] = vector313;

        bonePoints[24] = vector313;

        bonePoints[25] = vector314;

        bonePoints[26] = vector314;

        bonePoints[27] = vector315;
        this.drawBones(context, colorSetting, boneThickness, bonePoints);
        float jointRadius = boneThickness * 0.5F;

        Vec3[] jointPoints =
                new Vec3[] {
                    vector3, vector32, vector33, vector34, vector35, vector36, null, null, null,
                    null, null, null, null, null, null
                };

        jointPoints[6] = vector37;

        jointPoints[7] = vector38;

        jointPoints[8] = vector39;

        jointPoints[9] = vector310;

        jointPoints[10] = vector311;

        jointPoints[11] = vector312;

        jointPoints[12] = vector313;

        jointPoints[13] = vector314;

        jointPoints[14] = vector315;
        this.drawJoints(context, colorSetting, jointRadius, jointPoints);
    }

    private Color getChamsColor(boolean primary) {
        return primary ? this.chamsColor.getValue() : this.chamsColor.getSecondaryColor();
    }

    static {
        poseSnapshots = new ConcurrentHashMap<>();
    }

    private static final String MODULE_ID = "c0a706faabe5964a4713e4369f0b28db";
    private static final String SETTING_2_D_SETTING_ID = "e439debe8b7916416395c2d682f76cd6";
    private static final String MAIN_BOX_SETTING_ID = "fda3b14678d91bf8a3beff1bde63a98e";
    private static final String MAIN_BOX_SHADOW_SETTING_ID = "ea4cd30f98176c82cb285c5cc26e6b93";
    private static final String FILL_MAIN_BOX_SETTING_ID = "80ba4c1eabe4713b3e0ce08445a25cea";
    private static final String MAIN_BOX_MODE_SETTING_ID = "e1b47a47153f6699ebdab84fcd61149e";
    private static final String FILL_MAIN_BOX_COLOR_SETTING_ID =
            "989d4d1f64b64680bf3ffed29d8c6b46a7b8d653c6cdc984a6df3f78e2e15019";
    private static final String HEALTH_BAR_TREE_SETTING_ID = "f5501a515cb3b8160b801ea61bf21c15";
    private static final String HEALTH_BAR_SHADOW_SETTING_ID =
            "3c7748dece4f55af03bd80543fa35d3105503f49bd1f870e793c0d2a68c76ca9";
    private static final String HEALTH_BAR_ORIENTATION_SETTING_ID =
            "7b592c7d880de435a49dbc6e221c8013b22e7419386de52774377d0dd676177d";
    private static final String HEALTH_BAR_GROUP_ID = "89404c8a726434fd025bebd780107c16";
    private static final String MAIN_COLOR_SETTING_ID = "e7a443138918f70b6670cfe44913b6df";
    private static final String HEALTH_BAR_COLOR_SETTING_ID = "2a5c32e40b172d56b9840c5f8d52a25f";
    private static final String COLOR_GROUP_ID = "bedea323e5b25a09af1e8e6244a33119";
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
    private static final String CHAMS_MIX_SETTING_ID = "826576572a682320293d223f8aaf6366";
    private static final String DOWNSAMPLE_SETTING_ID = "59a82b2b50d7e6e3035849c2ba77b3d9";
    private static final String SKELETON_SETTING_ID = "84804b9a53a923fd0aeb33f748f3adf2";
    private static final String SKELETON_COLOR_SETTING_ID = "61cec6508708eefd483d6cf5d92a6f1e";
    private static final String MURDER_COLOR_SETTING_ID = "9b5d5a65c27a4b768bd2c07b4cbcd5cf";
    private static final String SKELETON_THICKNESS_SETTING_ID =
            "6b159c6a8580b32cb5af656a4bc499e182445ff9128dc28cb72d956347810b0e";
}
