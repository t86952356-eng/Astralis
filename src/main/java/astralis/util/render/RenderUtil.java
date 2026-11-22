package astralis.util.render;

import astralis.mixin.AbstractTextureAccessor;
import astralis.mixin.GlDeviceAccessor;
import astralis.mixin.GpuDeviceAccessor;
import astralis.setting.ColorSetting;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer.Draw;
import net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

import java.awt.Color;
import java.util.List;

public class RenderUtil implements InstanceAccess {
    /** Filled-box triangle-strip pipeline with depth testing bypassed. */
    public static RenderPipeline filledStripNoDepthPipeline;

    /** Filled-box quad pipeline with depth testing bypassed. */
    public static RenderPipeline filledQuadsNoDepthPipeline;

    /** Line pipeline with depth testing bypassed. */
    public static RenderPipeline linesNoDepthPipeline;

    /** Filled-quad setup with upload sorting and view-offset layering. */
    public static RenderSetup filledQuadsRenderSetup;

    /** Render type used for the filled-box faces. */
    public static RenderType filledNoDepthRenderType;

    /** Line setup with view-offset layering. */
    public static RenderSetup linesRenderSetup;

    /** Render type used for lines with depth testing bypassed. */
    public static RenderType linesNoDepthRenderType;

    private static boolean legacyModeEnabled;

    static {
        setLegacyModeEnabled(false);
        Builder filledStripPipelineBuilder =
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET);
        filledStripNoDepthPipeline =
                RenderPipelines.register(
                        filledStripPipelineBuilder
                                .withLocation("pipeline/debug_filled_box")
                                .withCull(false)
                                .withDepthStencilState(
                                        new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                                .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                                .build());
        Builder filledQuadsPipelineBuilder =
                RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET);
        filledQuadsNoDepthPipeline =
                RenderPipelines.register(
                        filledQuadsPipelineBuilder
                                .withLocation("pipeline/debug_filled_box")
                                .withCull(false)
                                .withDepthStencilState(
                                        new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                                .withColorTargetState(
                                        new ColorTargetState(BlendFunction.TRANSLUCENT))
                                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                                .build());
        Builder linePipelineBuilder = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET);
        linesNoDepthPipeline =
                RenderPipelines.register(
                        linePipelineBuilder
                                .withLocation("pipeline/lines_no_depth")
                                .withDepthStencilState(
                                        new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                                .build());
        filledQuadsRenderSetup =
                RenderSetup.builder(filledQuadsNoDepthPipeline)
                        .sortOnUpload()
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .createRenderSetup();

        filledNoDepthRenderType = RenderType.create("test2", filledQuadsRenderSetup);
        linesRenderSetup =
                RenderSetup.builder(linesNoDepthPipeline)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .createRenderSetup();

        linesNoDepthRenderType = RenderType.create("lines_no_depth", linesRenderSetup);
    }

    /** Draws a setting-colored box with depth testing bypassed. */
    public static void drawSettingBox(PoseStack poseStack, AABB bounds, ColorSetting colorSetting) {
        drawBoxWithColors(poseStack, bounds, colorSetting.getColors(), true);
    }

    /** Draws a setting-colored box with the selected depth behavior. */
    public static void drawSettingBox(
            PoseStack poseStack, AABB bounds, ColorSetting colorSetting, boolean ignoreDepth) {
        drawBoxWithColors(poseStack, bounds, colorSetting.getColors(), ignoreDepth);
    }

    private static void appendColoredQuad(
            Matrix4f poseMatrix,
            VertexConsumer vertices,
            int firstColor,
            int secondColor,
            int thirdColor,
            int fourthColor,
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
        vertices.addVertex(poseMatrix, x1, y1, z1).setColor(firstColor);
        vertices.addVertex(poseMatrix, x2, y2, z2).setColor(secondColor);
        vertices.addVertex(poseMatrix, x3, y3, z3).setColor(thirdColor);
        vertices.addVertex(poseMatrix, x4, y4, z4).setColor(fourthColor);
    }

    /** Draws a solid-color box with the selected depth behavior. */
    public static void drawBox(PoseStack poseStack, AABB bounds, Color color, boolean ignoreDepth) {
        drawGradientBox(poseStack, bounds, color, color, ignoreDepth);
    }

    /** Draws six solid faces using coordinates relative to the supplied pose stack. */
    public static void drawBoxRelative(PoseStack poseStack, AABB bounds, Color color) {
        poseStack.pushPose();
        poseStack.translate(bounds.minX, bounds.minY, bounds.minZ);
        Matrix4f poseMatrix = poseStack.last().pose();
        StagedVertexBuffer vertexBuffer = getStagedVertexBuffer();
        Draw draw =
                vertexBuffer.appendDraw(
                        filledNoDepthRenderType.format(),
                        filledNoDepthRenderType.primitiveTopology());
        VertexConsumer vertices = vertexBuffer.getVertexBuilder(draw);
        int argb = color.getRGB();
        float width = (float) (bounds.maxX - bounds.minX);
        float height = (float) (bounds.maxY - bounds.minY);
        float depth = (float) (bounds.maxZ - bounds.minZ);
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        appendSolidQuad(
                poseMatrix,
                vertices,
                argb,
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
        vertexBuffer.upload();
        PreparedRenderType preparedRenderType = filledNoDepthRenderType.prepare();
        ExecuteInfo executeInfo = vertexBuffer.getExecuteInfo(draw);
        if (executeInfo != null) {
            preparedRenderType.drawFromBuffer(executeInfo);
        }

        vertexBuffer.endDraw();
        poseStack.popPose();
    }

    public static void drawBox(PoseStack poseStack, AABB bounds, Color color) {
        drawBox(poseStack, bounds, color, true);
    }

    /** Returns the OpenGL texture identifier, or zero for another backend. */
    public static int getGlTextureId(GpuTexture texture) {
        return texture instanceof GlTexture glTexture ? glTexture.glId() : 0;
    }

    /** Gets the OpenGL framebuffer for the target color and optional depth attachments. */
    public static int getGlFramebufferId(RenderTarget renderTarget) {
        if (!Render2DContext.isOpenGlBackend()) {
            return 0;
        }

        if (renderTarget.getColorTexture() instanceof GlTexture colorTexture) {
            GpuDevice gpuDevice = RenderSystem.getDevice();
            if (((GpuDeviceAccessor) gpuDevice).getBackend()
                    instanceof GlDeviceAccessor glBackend) {
                DirectStateAccess directStateAccess = glBackend.invokeDirectStateAccess();
                FrameBufferCache frameBufferCache = glBackend.getFrameBufferCache();
                GlTexture depthTexture =
                        renderTarget.getDepthTexture() instanceof GlTexture glDepthTexture
                                ? glDepthTexture
                                : null;
                return frameBufferCache.getFbo(
                        directStateAccess, List.of(colorTexture), depthTexture);
            } else {

                throw new IllegalStateException("Only OpenGL backend is supported");
            }
        } else {

            throw new IllegalStateException("Expected GL texture attachment");
        }
    }

    /** Checks whether a point lies inside the rectangle, including its edges. */
    public static boolean isInsideRect(
            double pointX, double pointY, float left, float top, float width, float height) {
        return pointX >= left && pointX <= left + width && pointY >= top && pointY <= top + height;
    }

    private static void drawGradientBox(
            PoseStack poseStack,
            AABB bounds,
            Color bottomColor,
            Color topColor,
            boolean ignoreDepth) {
        poseStack.pushPose();
        Vec3 cameraPosition = mc.gameRenderer.mainCamera().position();
        poseStack.translate(
                bounds.minX - cameraPosition.x,
                bounds.minY - cameraPosition.y,
                bounds.minZ - cameraPosition.z);
        Matrix4f poseMatrix = poseStack.last().pose();
        StagedVertexBuffer vertexBuffer = getStagedVertexBuffer();
        RenderType renderType =
                ignoreDepth ? filledNoDepthRenderType : RenderTypes.debugFilledBox();
        Draw draw = vertexBuffer.appendDraw(renderType.format(), renderType.primitiveTopology());
        VertexConsumer vertices = vertexBuffer.getVertexBuilder(draw);
        int bottomArgb = bottomColor.getRGB();
        int topArgb = topColor.getRGB();
        float width = (float) (bounds.maxX - bounds.minX);
        float height = (float) (bounds.maxY - bounds.minY);
        float depth = (float) (bounds.maxZ - bounds.minZ);
        appendColoredQuad(
                poseMatrix,
                vertices,
                bottomArgb,
                bottomArgb,
                bottomArgb,
                bottomArgb,
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
        appendColoredQuad(
                poseMatrix,
                vertices,
                topArgb,
                topArgb,
                topArgb,
                topArgb,
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
        appendColoredQuad(
                poseMatrix,
                vertices,
                bottomArgb,
                bottomArgb,
                topArgb,
                topArgb,
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
        appendColoredQuad(
                poseMatrix,
                vertices,
                bottomArgb,
                bottomArgb,
                topArgb,
                topArgb,
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
        appendColoredQuad(
                poseMatrix,
                vertices,
                bottomArgb,
                bottomArgb,
                topArgb,
                topArgb,
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
        appendColoredQuad(
                poseMatrix,
                vertices,
                bottomArgb,
                bottomArgb,
                topArgb,
                topArgb,
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
        vertexBuffer.upload();
        PreparedRenderType preparedRenderType = renderType.prepare();
        ExecuteInfo executeInfo = vertexBuffer.getExecuteInfo(draw);
        if (executeInfo != null) {
            preparedRenderType.drawFromBuffer(executeInfo);
        }

        vertexBuffer.endDraw();
        poseStack.popPose();
    }

    public static void setLegacyModeEnabled(boolean legacyState) {
        legacyModeEnabled = legacyState;
    }

    /** Gets a mod texture identifier after applying clamped linear sampling. */
    public static int getClampedTextureId(String texturePath) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();

        AbstractTexture texture =
                textureManager.getTexture(
                        Identifier.fromNamespaceAndPath("astralis-recode", texturePath));
        ((AbstractTextureAccessor) texture)
                .setSampler(
                        RenderSystem.getSamplerCache()
                                .getSampler(
                                        AddressMode.CLAMP_TO_EDGE,
                                        AddressMode.CLAMP_TO_EDGE,
                                        FilterMode.LINEAR,
                                        FilterMode.LINEAR,
                                        false));
        return getGlTextureId(texture.getTexture());
    }

    private static void appendSolidQuad(
            Matrix4f poseMatrix,
            VertexConsumer vertices,
            int color,
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
        vertices.addVertex(poseMatrix, x1, y1, z1).setColor(color);
        vertices.addVertex(poseMatrix, x2, y2, z2).setColor(color);
        vertices.addVertex(poseMatrix, x3, y3, z3).setColor(color);
        vertices.addVertex(poseMatrix, x4, y4, z4).setColor(color);
    }

    /** Returns the game renderer staged vertex buffer. */
    public static StagedVertexBuffer getStagedVertexBuffer() {
        return mc.gameRenderer.renderBuffers().stagedVertexBuffer();
    }

    private static void drawBoxWithColors(
            PoseStack poseStack, AABB bounds, Color[] colors, boolean ignoreDepth) {
        drawGradientBox(poseStack, bounds, colors[0], colors[1], ignoreDepth);
    }
}
