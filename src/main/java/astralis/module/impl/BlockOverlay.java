package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.BlockOutlineRenderEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.setting.SettingGroup;
import astralis.util.render.ColorUtil;
import astralis.util.render.FadingBlockOutline;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderUtil;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Vector3f;
import org.joml.Vector4f;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

public class BlockOverlay extends Module {
    private static RenderType outlineMaskRenderType;
    private static RenderType fillMaskRenderType;
    private final BooleanSetting outline;
    private final ColorSetting outlineColor;
    private final NumberSetting outlineWidth;
    private final BooleanSetting outlineSeeThrough;
    private final BooleanSetting fill;
    private final ColorSetting fillColor;
    private final BooleanSetting fillSeeThrough;
    private final BooleanSetting smoothing;
    private final ModeSetting smoothingMode;
    private final NumberSetting smoothingSpeed;
    private final BooleanSetting glow;
    private final ColorSetting glowColor;
    private final NumberSetting glowRadius;
    private final NumberSetting glowIntensity;
    private final BooleanSetting glowOverlay;
    private final ModeSetting gradient;
    private final NumberSetting gradientSpeed;
    private final BooleanSetting glowOutline;
    private final NumberSetting glowOutlineThickness;
    private final ModeSetting glowOutlineColorMode;
    private final ColorSetting glowOutlineColor;
    private final BooleanSetting chams;
    private final ModeSetting chamsColorMode;
    private final ColorSetting chamsColor;
    private final NumberSetting chamsMix;
    private final NumberSetting downsample;
    private Vec3 smoothedPos;
    private BlockPos lastBlockPos;
    private VoxelShape lastShape;
    private final List<FadingBlockOutline> fadingOutlines;
    private long lastFrameNanos;
    private float fadeProgress;

    public BlockOverlay() {

        super(MODULE_ID, "", "", Category.Misc);

        this.outline = new BooleanSetting(OUTLINE_SETTING_ID, true);

        this.outlineColor = new ColorSetting(COLOR_SETTING_ID, Color.WHITE);

        this.outlineWidth = new NumberSetting(WIDTH_SETTING_ID, 1.0F, 1.0F, 10.0F, 1.0F);

        this.outlineSeeThrough = new BooleanSetting(SEE_THROUGH_SETTING_ID, false);

        this.fill = new BooleanSetting(FILL_SETTING_ID, false);

        this.fillColor = new ColorSetting(COLOR_SETTING_ID, new Color(255, 255, 255, 80));

        this.fillSeeThrough = new BooleanSetting(SEE_THROUGH_SETTING_ID, false);

        this.smoothing = new BooleanSetting(SMOOTHING_SETTING_ID, false);

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] smoothingModes = new String[2];

        smoothingModes[0] = "Lerping";

        smoothingModes[1] = "Fade";
        ModeSetting modeSetting2 = modeSetting.addModes(smoothingModes);
        this.smoothingMode = modeSetting2.setDefault("Lerping");

        this.smoothingSpeed = new NumberSetting(SPEED_SETTING_ID, 12.0F, 1.0F, 30.0F, 1.0F);

        this.glow = new BooleanSetting(GLOW_SETTING_ID, false);

        this.glowColor = new ColorSetting(COLOR_SETTING_ID, Color.CYAN);

        this.glowRadius = new NumberSetting(RADIUS_SETTING_ID, 3.0F, 0.1F, 20.0F, 0.1F);

        this.glowIntensity = new NumberSetting(GLOW_INTENSITY_SETTING_ID, 1.5F, 0.1F, 5.0F, 0.1F);

        this.glowOverlay = new BooleanSetting(GLOW_OVERLAY_SETTING_ID, false);

        ModeSetting modeSetting3 = new ModeSetting(GRADIENT_SETTING_ID);
        String[] gradientModes = new String[3];

        gradientModes[0] = "Vertical";

        gradientModes[1] = "Horizontal";

        gradientModes[2] = "Diagonal";
        ModeSetting modeSetting4 = modeSetting3.addModes(gradientModes);
        this.gradient = modeSetting4.setDefault("Vertical");

        this.gradientSpeed = new NumberSetting(GRADIENT_SPEED_SETTING_ID, 0.5F, 0.0F, 5.0F, 0.1F);

        this.glowOutline = new BooleanSetting(OUTLINE_SETTING_ID, true);

        this.glowOutlineThickness =
                new NumberSetting(OUTLINE_THICKNESS_SETTING_ID, 1.5F, 0.5F, 5.0F, 0.1F);

        ModeSetting modeSetting5 = new ModeSetting(OUTLINE_COLOR_SETTING_ID);
        String[] outlineColorModes = new String[2];

        outlineColorModes[0] = "Custom";

        outlineColorModes[1] = "Same";
        ModeSetting modeSetting6 = modeSetting5.addModes(outlineColorModes);
        this.glowOutlineColorMode = modeSetting6.setDefault("Same");

        this.glowOutlineColor = new ColorSetting(COLOR_SETTING_ID, Color.WHITE);

        this.chams = new BooleanSetting(CHAMS_SETTING_ID, false);

        ModeSetting modeSetting7 = new ModeSetting(CHAMS_COLOR_SETTING_ID);
        String[] chamsColorModes = new String[2];

        chamsColorModes[0] = "Custom";

        chamsColorModes[1] = "Client";
        ModeSetting modeSetting8 = modeSetting7.addModes(chamsColorModes);
        this.chamsColorMode = modeSetting8.setDefault("Client");

        this.chamsColor = new ColorSetting(COLOR_SETTING_ID, new Color(255, 50, 50, 180));

        this.chamsMix = new NumberSetting(CHAMS_MIX_SETTING_ID, 0.5F, 0.0F, 1.0F, 0.05F);

        this.downsample = new NumberSetting(DOWNSAMPLE_SETTING_ID, 0.5F, 0.1F, 1.0F, 0.05F);
        this.fadingOutlines = new java.util.ArrayList<>();
        this.fadeProgress = 1.0F;
        SettingGroup settingGroup =
                new SettingGroup(OUTLINE_SETTING_ID)
                        .addValues(
                                this.outline,
                                this.outlineColor,
                                this.outlineWidth,
                                this.outlineSeeThrough);
        SettingGroup settingGroup2 =
                new SettingGroup(FILL_SETTING_ID)
                        .addValues(this.fill, this.fillColor, this.fillSeeThrough);
        SettingGroup settingGroup3 =
                new SettingGroup(SMOOTHING_SETTING_ID)
                        .addValues(this.smoothing, this.smoothingMode, this.smoothingSpeed);
        SettingGroup settingGroup4 = new SettingGroup(GLOW_SETTINGS_GROUP_ID);

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
        SettingGroup settingGroup5 = settingGroup4.addValues(glowSettings);
        SettingGroup settingGroup6 =
                new SettingGroup(GLOW_OUTLINE_GROUP_ID)
                        .addValues(
                                this.glowOutline,
                                this.glowOutlineThickness,
                                this.glowOutlineColorMode,
                                this.glowOutlineColor);
        SettingGroup settingGroup7 =
                new SettingGroup(CHAMS_SETTING_ID)
                        .addValues(this.chams, this.chamsColorMode, this.chamsColor, this.chamsMix);
        this.fillColor.hideIf(
                () -> {
                    return !this.fill.getValue();
                });
        this.fillSeeThrough.hideIf(
                () -> {
                    return !this.fill.getValue();
                });
        this.smoothingMode.hideIf(
                () -> {
                    return !this.smoothing.getValue();
                });
        this.smoothingSpeed.hideIf(
                () -> {
                    return !this.smoothing.getValue();
                });
        this.outlineColor.hideIf(
                () -> {
                    return !this.outline.getValue();
                });
        this.outlineWidth.hideIf(
                () -> {
                    return !this.outline.getValue();
                });
        this.outlineSeeThrough.hideIf(
                () -> {
                    return !this.outline.getValue();
                });
        settingGroup5.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        settingGroup6.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        settingGroup7.hideIf(
                () -> {
                    return !this.glow.getValue();
                });
        this.glowOutlineThickness.hideIf(
                () -> {
                    return !this.glowOutline.getValue();
                });
        this.glowOutlineColorMode.hideIf(
                () -> {
                    return !this.glowOutline.getValue();
                });
        this.glowOutlineColor.hideIf(
                () -> {
                    if (this.glowOutline.getValue()) {

                        if (this.glowOutlineColorMode.is("Custom")) {
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
        ModeSetting modeSetting9 = this.glowColor.getColorMode();

        modeSetting9.setDefault("Client");

        Setting[] settings =
                new Setting[] {
                    settingGroup,
                    settingGroup2,
                    settingGroup3,
                    this.glow,
                    settingGroup5,
                    settingGroup6,
                    null
                };

        settings[6] = settingGroup7;
        this.addSettings(settings);
    }

    private Vec3 getSmoothedPosition(BlockPos blockPos) {
        Vec3 vector3 = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        if (!this.smoothing.getValue()) {
            this.smoothedPos = vector3;
            this.fadeProgress = 1.0F;
            this.lastBlockPos = blockPos;
            this.fadingOutlines.clear();
            return vector3;
        }

        if (this.smoothedPos == null) {
            this.smoothedPos = vector3;
        }

        float smoothingFactor = this.getSmoothingFactor();

        if (this.smoothingMode.is("Lerping")) {
            this.smoothedPos = this.smoothedPos.lerp(vector3, smoothingFactor);
            this.fadeProgress = 1.0F;
        }

        this.lastBlockPos = blockPos;
        return this.smoothedPos;
    }

    private void updateFade(BlockPos blockPos, VoxelShape voxelShape) {
        float smoothingFactor = this.getSmoothingFactor();
        if (this.lastBlockPos == null) {
            this.fadeProgress = 0.0F;
        } else if (!blockPos.equals(this.lastBlockPos)) {
            if (this.lastShape != null && this.fadeProgress > 0.0F) {
                this.fadingOutlines.add(
                        new FadingBlockOutline(
                                new Vec3(
                                        this.lastBlockPos.getX(),
                                        this.lastBlockPos.getY(),
                                        this.lastBlockPos.getZ()),
                                this.lastShape,
                                this.fadeProgress));
            }

            this.fadeProgress = 0.0F;
        }

        this.fadeProgress = Math.clamp(this.fadeProgress + smoothingFactor, 0.0F, 1.0F);
        Iterator iterator = this.fadingOutlines.iterator();

        while (iterator.hasNext()) {
            FadingBlockOutline fadingOutline = (FadingBlockOutline) iterator.next();
            fadingOutline.alpha -= smoothingFactor;
            if (fadingOutline.alpha <= 0.0F) {
                iterator.remove();
            }
        }

        this.lastBlockPos = blockPos;
        this.lastShape = voxelShape;
    }

    private float getGradientAngle() {

        if (this.gradient.is("Horizontal")) {
            return 0.0F;
        }

        return this.gradient.is("Diagonal") ? (float) (Math.PI / 4) : (float) (Math.PI / 2);
    }

    private static void submitFilledBox(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            AABB boundingBox,
            double x,
            double y,
            double z,
            RenderType renderType,
            int bottomColor,
            int topColor) {
        poseStack.pushPose();
        poseStack.translate(x + boundingBox.minX, y + boundingBox.minY, z + boundingBox.minZ);
        float width = (float) (boundingBox.maxX - boundingBox.minX);
        float height = (float) (boundingBox.maxY - boundingBox.minY);
        float depth = (float) (boundingBox.maxZ - boundingBox.minZ);
        submitFilledBoxGeometry(
                submitNodeCollector,
                poseStack,
                renderType,
                width,
                height,
                depth,
                bottomColor,
                topColor);
        poseStack.popPose();
    }

    @EventTarget
    public void onBlockOutlineRender(BlockOutlineRenderEvent event) {
        BlockOutlineRenderState blockOutlineRenderState = event.getRenderState();
        if (blockOutlineRenderState != null && !blockOutlineRenderState.shape().isEmpty()) {
            event.setCancelled(true);
            if (this.smoothing.getValue()) {

                if (this.smoothingMode.is("Fade")) {
                    this.updateFade(blockOutlineRenderState.pos(), blockOutlineRenderState.shape());

                    for (FadingBlockOutline fadingOutline : this.fadingOutlines) {
                        this.renderShape(
                                event,
                                fadingOutline.shape,
                                fadingOutline.position,
                                fadingOutline.alpha);
                    }

                    this.renderShape(
                            event,
                            blockOutlineRenderState.shape(),
                            new Vec3(
                                    blockOutlineRenderState.pos().getX(),
                                    blockOutlineRenderState.pos().getY(),
                                    blockOutlineRenderState.pos().getZ()),
                            this.fadeProgress);
                    return;
                }
            }

            Vec3 vector3 = this.getSmoothedPosition(blockOutlineRenderState.pos());
            this.renderShape(event, blockOutlineRenderState.shape(), vector3, 1.0F);
        }
    }

    private static void submitShapeOutlineAt(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            VoxelShape voxelShape,
            double x,
            double y,
            double z,
            RenderType renderType,
            int color,
            float lineWidth) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        submitNodeCollector.submitShapeOutline(
                poseStack, voxelShape, renderType, color, lineWidth, false);
        poseStack.popPose();
    }

    public void clearOutlineTarget() {
        if (this.isGlowActive()) {
            RenderTarget renderTarget = mc.levelRenderer.entityOutlineTarget();
            if (renderTarget != null && renderTarget.getColorTexture() != null) {
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                if (renderTarget.getDepthTexture() != null) {
                    commandEncoder.clearColorAndDepthTextures(
                            renderTarget.getColorTexture(),
                            new Vector4f(0.0F, 0.0F, 0.0F, 0.0F),
                            renderTarget.getDepthTexture(),
                            0.0);
                } else {
                    commandEncoder.clearColorTexture(
                            renderTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
                }
            }
        }
    }

    public void renderGlow() {
        if (this.isGlowActive()) {
            RenderTarget renderTarget = mc.levelRenderer.entityOutlineTarget();
            RenderTarget renderTarget2 = mc.gameRenderer.mainRenderTarget();
            Render2DContext renderContext =
                    Astralis.getInstance().getFramebufferManager().getRenderContext();
            if (renderTarget != null
                    && renderTarget.getColorTexture() != null
                    && renderContext != null
                    && renderContext.getEntityGlowRenderer() != null) {
                Color color = this.glowColor.getValue();
                Color color2 = this.glowColor.getSecondaryColor();
                Color color3;
                Color color4;
                if (!this.glowOutline.getValue()) {
                    color3 = color4 = new Color(0, 0, 0, 0);
                } else {

                    if (this.glowOutlineColorMode.is("Custom")) {
                        color3 = this.glowOutlineColor.getValue();
                        color4 = this.glowOutlineColor.getSecondaryColor();
                    } else {
                        color3 = color;
                        color4 = color2;
                    }
                }

                Color color5;
                Color color6;
                label54:
                {
                    if (this.chams.getValue()) {

                        if (this.chamsColorMode.is("Custom")) {
                            color5 = this.chamsColor.getValue();
                            color6 = this.chamsColor.getSecondaryColor();
                            break label54;
                        }
                    }

                    if (this.chams.getValue()) {
                        color5 = Astralis.getInstance().getPrimaryColor();
                        color6 = Astralis.getInstance().getSecondaryColor();
                    } else {
                        color6 = Color.BLACK;
                        color5 = Color.BLACK;
                    }
                }

                renderContext
                        .getEntityGlowRenderer()
                        .renderEnhanced(
                                renderTarget.getColorTexture(),
                                color,
                                color2,
                                color3,
                                color4,
                                this.glowRadius.getValue(),
                                this.glowOutline.getValue()
                                        ? this.glowOutlineThickness.getValue()
                                        : 0.0F,
                                this.glowIntensity.getValue(),
                                this.glowOverlay.getValue(),
                                this.gradientSpeed.getValue(),
                                this.getGradientAngle(),
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

    public boolean isGlowActive() {
        return this.isEnabled() && this.glow.getValue();
    }

    private static void submitGradientShapeOutline(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            VoxelShape voxelShape,
            double x,
            double y,
            double z,
            RenderType renderType,
            Color[] colors,
            float lineWidth) {
        AABB boundingBox = voxelShape.bounds();
        double minY = boundingBox.minY;
        double shapeHeight = Math.max(1.0E-4, boundingBox.maxY - minY);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                renderType,
                (pose, vertexConsumer) -> {
                    Vector3f vector3f = new Vector3f();
                    voxelShape.forAllEdges(
                            (x1, y1, z1, x2, y2, z2) -> {
                                vector3f.set(
                                                (float) (x2 - x1),
                                                (float) (y2 - y1),
                                                (float) (z2 - z1))
                                        .normalize();
                                int startColor = sampleGradient(colors, (y1 - minY) / shapeHeight);
                                int endColor = sampleGradient(colors, (y2 - minY) / shapeHeight);
                                vertexConsumer
                                        .addVertex(pose, (float) x1, (float) y1, (float) z1)
                                        .setColor(startColor)
                                        .setNormal(pose, vector3f)
                                        .setLineWidth(lineWidth);
                                vertexConsumer
                                        .addVertex(pose, (float) x2, (float) y2, (float) z2)
                                        .setColor(endColor)
                                        .setNormal(pose, vector3f)
                                        .setLineWidth(lineWidth);
                            });
                });
        poseStack.popPose();
    }

    private static void addColoredQuad(
            Pose pose,
            VertexConsumer vertexConsumer,
            int color1,
            int color2,
            int color3,
            int color4,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4) {
        vertexConsumer.addVertex(pose, x1, y1, z1).setColor(color1);
        vertexConsumer.addVertex(pose, x2, y2, z2).setColor(color2);
        vertexConsumer.addVertex(pose, x3, y3, z3).setColor(color3);
        vertexConsumer.addVertex(pose, x4, y4, z4).setColor(color4);
    }

    private static Color scaleAlpha(Color color, float alphaScale) {
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                Math.round(color.getAlpha() * alphaScale));
    }

    private static void submitFilledBoxGeometry(
            SubmitNodeCollector submitNodeCollector,
            PoseStack poseStack,
            RenderType renderType,
            float width,
            float height,
            float depth,
            int bottomColor,
            int topColor) {
        submitNodeCollector.submitCustomGeometry(
                poseStack,
                renderType,
                (pose, vertexConsumer) -> {
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            bottomColor,
                            bottomColor,
                            bottomColor,
                            bottomColor,
                            0.0F,
                            0.0F,
                            depth,
                            width,
                            0.0F,
                            depth,
                            width,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F);
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            topColor,
                            topColor,
                            topColor,
                            topColor,
                            0.0F,
                            height,
                            0.0F,
                            width,
                            height,
                            0.0F,
                            width,
                            height,
                            depth,
                            0.0F,
                            height,
                            depth);
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            bottomColor,
                            bottomColor,
                            topColor,
                            topColor,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            depth,
                            0.0F,
                            height,
                            depth,
                            0.0F,
                            height,
                            0.0F);
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            bottomColor,
                            bottomColor,
                            topColor,
                            topColor,
                            width,
                            0.0F,
                            depth,
                            width,
                            0.0F,
                            0.0F,
                            width,
                            height,
                            0.0F,
                            width,
                            height,
                            depth);
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            bottomColor,
                            bottomColor,
                            topColor,
                            topColor,
                            width,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            height,
                            0.0F,
                            width,
                            height,
                            0.0F);
                    addColoredQuad(
                            pose,
                            vertexConsumer,
                            bottomColor,
                            bottomColor,
                            topColor,
                            topColor,
                            0.0F,
                            0.0F,
                            depth,
                            width,
                            0.0F,
                            depth,
                            width,
                            height,
                            depth,
                            0.0F,
                            height,
                            depth);
                });
    }

    @Override
    public void onDisable() {
        this.smoothedPos = null;
        this.lastBlockPos = null;
        this.lastShape = null;
        this.fadingOutlines.clear();
        this.lastFrameNanos = 0L;
        this.fadeProgress = 1.0F;
    }

    private Color[] getScaledColors(ColorSetting colorSetting, float alphaScale) {
        return new Color[] {
            scaleAlpha(colorSetting.getValue(), alphaScale),
            scaleAlpha(colorSetting.getSecondaryColor(), alphaScale)
        };
    }

    private void renderShape(
            BlockOutlineRenderEvent event, VoxelShape voxelShape, Vec3 vector3, float alpha) {
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector submitNodeCollector = event.getNodeCollector();
        double relativeX = vector3.x - event.getCameraX();
        double relativeY = vector3.y - event.getCameraY();
        double relativeZ = vector3.z - event.getCameraZ();
        RenderType renderType =
                this.outlineSeeThrough.getValue()
                        ? RenderUtil.linesNoDepthRenderType
                        : RenderTypes.lines();
        Color[] outlineColors = this.getScaledColors(this.outlineColor, alpha);
        Color[] fillColors = this.getScaledColors(this.fillColor, alpha);
        if (this.outline.getValue()) {
            submitGradientShapeOutline(
                    submitNodeCollector,
                    poseStack,
                    voxelShape,
                    relativeX,
                    relativeY,
                    relativeZ,
                    renderType,
                    outlineColors,
                    this.outlineWidth.getValue());
        }

        if (this.fill.getValue()) {
            RenderType renderType2 =
                    this.fillSeeThrough.getValue()
                            ? RenderUtil.filledNoDepthRenderType
                            : RenderTypes.debugFilledBox();

            for (AABB boundingBox : voxelShape.toAabbs()) {
                submitFilledBox(
                        submitNodeCollector,
                        poseStack,
                        boundingBox,
                        relativeX,
                        relativeY,
                        relativeZ,
                        renderType2,
                        fillColors[0].getRGB(),
                        fillColors[1].getRGB());
            }
        }

        if (this.isGlowActive()) {

            int maskColor = new Color(255, 255, 255, Math.round(255.0F * alpha)).getRGB();
            submitShapeOutlineAt(
                    submitNodeCollector,
                    poseStack,
                    voxelShape,
                    relativeX,
                    relativeY,
                    relativeZ,
                    outlineMaskRenderType,
                    maskColor,
                    Math.max(1.0F, this.outlineWidth.getValue()));
            if (this.fill.getValue()) {
                for (AABB boundingBox2 : voxelShape.toAabbs()) {
                    submitFilledBox(
                            submitNodeCollector,
                            poseStack,
                            boundingBox2,
                            relativeX,
                            relativeY,
                            relativeZ,
                            fillMaskRenderType,
                            maskColor,
                            maskColor);
                }
            }
        }
    }

    private float getSmoothingFactor() {
        long now = System.nanoTime();
        double deltaSeconds =
                this.lastFrameNanos == 0L
                        ? 0.0
                        : Math.min((now - this.lastFrameNanos) / 1.0E9, 0.1);
        this.lastFrameNanos = now;
        return (float)
                (1.0 - Math.exp(-this.smoothingSpeed.getValue().doubleValue() * deltaSeconds));
    }

    private static int sampleGradient(Color[] colors, double t) {
        return ColorUtil.lerpColor(colors[0], colors[1], (float) t).getRGB();
    }

    static {
        outlineMaskRenderType =
                RenderType.create(
                        "astralis_block_overlay_outline_mask",
                        RenderSetup.builder(RenderUtil.linesNoDepthPipeline)
                                .setOutputTarget(OutputTarget.OUTLINE_TARGET)
                                .createRenderSetup());
        fillMaskRenderType =
                RenderType.create(
                        "astralis_block_overlay_fill_mask",
                        RenderSetup.builder(RenderUtil.filledQuadsNoDepthPipeline)
                                .setOutputTarget(OutputTarget.OUTLINE_TARGET)
                                .createRenderSetup());
    }

    private static final String MODULE_ID = "e4e76ebc7ce6e03b4e1883099ef369b9";
    private static final String OUTLINE_SETTING_ID = "d0c073952922eca2e57c7c872e697e8b";
    private static final String COLOR_SETTING_ID = "bedea323e5b25a09af1e8e6244a33119";
    private static final String WIDTH_SETTING_ID = "31dcc570c82b23570c66a31eea1b4d23";
    private static final String SEE_THROUGH_SETTING_ID = "5256e88b5d54b0abf5d428177f7b1ccb";
    private static final String FILL_SETTING_ID = "313487a7fc60b60be598a03325fd93cc";
    private static final String SMOOTHING_SETTING_ID = "7f4b509d7f7eb6667c9c3fa128a3e30a";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String SPEED_SETTING_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String GLOW_SETTING_ID = "a2f1c3e2323f5f46f7ddad7004cf419c";
    private static final String RADIUS_SETTING_ID = "00107704ec92e058e1cce9b2a3ab2723";
    private static final String GLOW_INTENSITY_SETTING_ID = "83f803db46a2015e8834bcc6af7e551a";
    private static final String GLOW_OVERLAY_SETTING_ID = "347f4eb77944096538fd5eca47d61e13";
    private static final String GRADIENT_SETTING_ID = "b83e530dfe658ae290e7bdd875e16de1";
    private static final String GRADIENT_SPEED_SETTING_ID = "eb10ec982d4e9152cbf0e7e65d7b195a";
    private static final String OUTLINE_THICKNESS_SETTING_ID =
            "0e3714b906207e270c2913c61b74945b4610618ad961a480911eed4ec5414d29";
    private static final String OUTLINE_COLOR_SETTING_ID = "cacb720f82162eb4a75feb63eaf40894";
    private static final String CHAMS_SETTING_ID = "0e972e2d1bbbcdbddc280c0288802bc1";
    private static final String CHAMS_COLOR_SETTING_ID = "035d1d5bdbbbfac78b5c2eeca9661f16";
    private static final String CHAMS_MIX_SETTING_ID = "826576572a682320293d223f8aaf6366";
    private static final String DOWNSAMPLE_SETTING_ID = "59a82b2b50d7e6e3035849c2ba77b3d9";
    private static final String GLOW_SETTINGS_GROUP_ID = "81cd366cdc2dbee38e7de4a45ecf7484";
    private static final String GLOW_OUTLINE_GROUP_ID = "1eaca6b09a26d78d2447ee7989a5e15e";
}
