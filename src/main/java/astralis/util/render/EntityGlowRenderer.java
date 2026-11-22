package astralis.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;

public class EntityGlowRenderer extends AbstractBlurRenderer {
    private final GpuBuffer glowParamsBuffer;
    private final ByteBuffer glowParamsData;
    private final GpuBuffer enhancedGlowParamsBuffer;
    private final ByteBuffer enhancedGlowParamsData;
    private final GpuBuffer chamsParamsBuffer;
    private final ByteBuffer chamsParamsData;
    private GpuTexture cachedSourceTexture;
    private RenderTarget cachedBlurTarget;
    private long cachedFrameId;
    private int cachedBlurIterations;
    private int cachedWidth;
    private int cachedHeight;
    private int cachedScaleBits;
    private boolean cacheValid;
    private final RenderTarget enhancedGlowTarget;

    public EntityGlowRenderer(Render2DContext renderContext) {
        super(renderContext);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier labelSupplier = () -> "Sph Glow Params UBO";

        this.glowParamsBuffer = gpuDevice.createBuffer(labelSupplier, 136, 16L);
        this.glowParamsData = MemoryUtil.memAlloc(16);
        GpuDevice gpuDevice2 = RenderSystem.getDevice();
        labelSupplier = () -> "Sph Enhanced Glow Params UBO";

        this.enhancedGlowParamsBuffer = gpuDevice2.createBuffer(labelSupplier, 136, 96L);
        this.enhancedGlowParamsData = MemoryUtil.memAlloc(96);
        GpuDevice gpuDevice3 = RenderSystem.getDevice();
        labelSupplier = () -> "Sph Chams Params UBO";

        this.chamsParamsBuffer = gpuDevice3.createBuffer(labelSupplier, 136, 64L);
        this.chamsParamsData = MemoryUtil.memAlloc(64);

        this.enhancedGlowTarget =
                new TextureTarget("Sph Enhanced Glow Target", 1, 1, false, GpuFormat.RGBA8_UNORM);
    }

    @Override
    public void release() {
        super.release();
        this.glowParamsBuffer.close();
        MemoryUtil.memFree(this.glowParamsData);
        this.enhancedGlowParamsBuffer.close();
        MemoryUtil.memFree(this.enhancedGlowParamsData);
        this.chamsParamsBuffer.close();
        MemoryUtil.memFree(this.chamsParamsData);
        this.enhancedGlowTarget.destroyBuffers();
    }

    private void renderChams(
            GpuTextureView gpuTextureView,
            Color color,
            Color color2,
            float chamsMix,
            float gradientSpeed,
            float gradientAngle,
            float time,
            RenderTarget renderTarget) {
        if (AstralisPipelines.isPipelineValid(AstralisPipelines.chams)) {
            float scrollDistance = time * gradientSpeed;
            this.chamsParamsData.clear();
            putColor(this.chamsParamsData, color, 0.003921569F);
            putColor(this.chamsParamsData, color2, 0.003921569F);
            this.chamsParamsData.putFloat(chamsMix);
            this.chamsParamsData.putFloat(gradientSpeed);
            this.chamsParamsData.putFloat(gradientAngle);
            this.chamsParamsData.putFloat(time);
            this.chamsParamsData.putFloat((float) Math.cos(gradientAngle) * scrollDistance);
            this.chamsParamsData.putFloat((float) Math.sin(gradientAngle) * scrollDistance);
            this.chamsParamsData.putFloat(0.0F).putFloat(0.0F);
            this.chamsParamsData.flip();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.writeToBuffer(this.chamsParamsBuffer.slice(), this.chamsParamsData);

            try (RenderPass renderPass =
                    commandEncoder.createRenderPass(
                            () -> "Sph Chams",
                            renderTarget.getColorTextureView(),
                            Optional.empty())) {
                renderPass.setPipeline(AstralisPipelines.chams);
                renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

                renderPass.bindTexture("originalIn", gpuTextureView, linearClampSampler);

                renderPass.setUniform("SphChamsParams", this.chamsParamsBuffer.slice());
                renderPass.drawIndexed(3, 1, 0, 0, 0);
            }
        }
    }

    public void render(
            GpuTexture gpuTexture,
            Color color,
            float glowRadius,
            RenderTarget renderTarget,
            int width,
            int height,
            float downsampleFactor) {

        if (gpuTexture != null) {
            if (this.isBlurAvailable()
                    && AstralisPipelines.isPipelineValid(AstralisPipelines.glow)) {

                int blurIterations = Math.min(6, Math.max(2, (int) Math.ceil(glowRadius / 2.0F)));
                GpuTextureView gpuTextureView = this.getOrCreateTextureView(gpuTexture);
                if (gpuTextureView != null) {
                    RenderTarget renderTarget2 =
                            this.getCachedBlur(
                                    gpuTexture,
                                    gpuTextureView,
                                    blurIterations,
                                    width,
                                    height,
                                    downsampleFactor);
                    this.glowParamsData.clear();
                    this.glowParamsData.putFloat(color.getRed() * 0.003921569F);
                    this.glowParamsData.putFloat(color.getGreen() * 0.003921569F);
                    this.glowParamsData.putFloat(color.getBlue() * 0.003921569F);
                    this.glowParamsData.putFloat(color.getAlpha() * 0.003921569F);
                    this.glowParamsData.flip();
                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    commandEncoder.writeToBuffer(
                            this.glowParamsBuffer.slice(), this.glowParamsData);

                    try (RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "Sph Glow Final",
                                    renderTarget.getColorTextureView(),
                                    Optional.empty())) {
                        renderPass.setPipeline(AstralisPipelines.glow);
                        renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                        renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

                        renderPass.bindTexture(
                                "textureIn",
                                renderTarget2.getColorTextureView(),
                                linearClampSampler);

                        renderPass.setUniform("SphGlowParams", this.glowParamsBuffer.slice());
                        renderPass.drawIndexed(3, 1, 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.glow;
    }

    private RenderTarget getCachedBlur(
            GpuTexture gpuTexture,
            GpuTextureView gpuTextureView,
            int blurIterations,
            int width,
            int height,
            float downsampleFactor) {
        int scaleBits = Float.floatToIntBits(downsampleFactor);
        if (this.cacheValid
                && this.cachedFrameId == this.getRenderContext().getFrameCounter()
                && this.cachedSourceTexture == gpuTexture
                && this.cachedBlurIterations == blurIterations
                && this.cachedWidth == width
                && this.cachedHeight == height
                && this.cachedScaleBits == scaleBits) {
            return this.cachedBlurTarget;
        }

        int scaledWidth = Math.max(1, (int) (width * downsampleFactor));
        int scaledHeight = Math.max(1, (int) (height * downsampleFactor));
        this.resizeBlurTargets(scaledWidth, scaledHeight);
        this.cachedBlurTarget =
                this.blurTexture(
                        gpuTextureView,
                        blurIterations,
                        scaledWidth,
                        scaledHeight,
                        1.0F / width,
                        1.0F / height,
                        1.0F / scaledWidth,
                        1.0F / scaledHeight);
        this.cachedSourceTexture = gpuTexture;
        this.cachedFrameId = this.getRenderContext().getFrameCounter();
        this.cachedBlurIterations = blurIterations;
        this.cachedWidth = width;
        this.cachedHeight = height;
        this.cachedScaleBits = scaleBits;
        this.cacheValid = true;
        return this.cachedBlurTarget;
    }

    public void renderEnhanced(
            GpuTexture gpuTexture,
            Color color,
            Color color2,
            Color color3,
            Color color4,
            float glowRadius,
            float outlineThickness,
            float glowIntensity,
            boolean glowBehind,
            float gradientSpeed,
            float gradientAngle,
            float time,
            float chamsMix,
            Color color5,
            Color color6,
            RenderTarget renderTarget,
            int width,
            int height,
            float downsampleFactor) {

        if (gpuTexture != null) {
            boolean chamsEnabled = chamsMix > 0.0F;
            boolean glowEnabled =
                    outlineThickness > 0.0F || color.getAlpha() > 0 || color2.getAlpha() > 0;
            if (chamsEnabled || glowEnabled) {
                GpuTextureView gpuTextureView = this.getOrCreateTextureView(gpuTexture);
                if (gpuTextureView != null) {
                    if (chamsEnabled) {
                        if (!AstralisPipelines.isPipelineValid(AstralisPipelines.chams)) {
                            return;
                        }

                        this.renderChams(
                                gpuTextureView,
                                color5,
                                color6,
                                chamsMix,
                                gradientSpeed,
                                gradientAngle,
                                time,
                                renderTarget);
                    }

                    if (glowEnabled && (!(glowIntensity <= 0.0F) || !(outlineThickness <= 0.0F))) {
                        if (this.isBlurAvailable()
                                && AstralisPipelines.isPipelineValid(AstralisPipelines.handGlow)) {
                            GpuTextureView gpuTextureView2;
                            if (glowIntensity > 0.0F) {

                                int blurIterations =
                                        Math.min(
                                                6, Math.max(2, (int) Math.ceil(glowRadius / 2.0F)));
                                gpuTextureView2 =
                                        this.getCachedBlur(
                                                        gpuTexture,
                                                        gpuTextureView,
                                                        blurIterations,
                                                        width,
                                                        height,
                                                        downsampleFactor)
                                                .getColorTextureView();
                            } else {
                                gpuTextureView2 = gpuTextureView;
                            }

                            float scrollDistance = time * gradientSpeed;
                            this.enhancedGlowParamsData.clear();
                            putColor(this.enhancedGlowParamsData, color, 0.003921569F);
                            putColor(this.enhancedGlowParamsData, color2, 0.003921569F);
                            putColor(this.enhancedGlowParamsData, color3, 0.003921569F);
                            putColor(this.enhancedGlowParamsData, color4, 0.003921569F);
                            this.enhancedGlowParamsData.putFloat(outlineThickness);
                            this.enhancedGlowParamsData.putFloat(glowIntensity);
                            this.enhancedGlowParamsData.putFloat(glowBehind ? 1.0F : 0.0F);
                            this.enhancedGlowParamsData.putFloat(gradientSpeed);
                            this.enhancedGlowParamsData.putFloat(gradientAngle);
                            this.enhancedGlowParamsData.putFloat(time);
                            this.enhancedGlowParamsData.putFloat(
                                    (float) Math.cos(gradientAngle) * scrollDistance);
                            this.enhancedGlowParamsData.putFloat(
                                    (float) Math.sin(gradientAngle) * scrollDistance);
                            this.enhancedGlowParamsData.flip();
                            int scaledWidth = Math.max(1, (int) (width * downsampleFactor));
                            int scaledHeight = Math.max(1, (int) (height * downsampleFactor));
                            boolean useOffscreenPass =
                                    outlineThickness <= 0.0F
                                            && (scaledWidth < width || scaledHeight < height)
                                            && AstralisPipelines.isPipelineValid(
                                                    AstralisPipelines.handGlowOffscreen)
                                            && AstralisPipelines.isPipelineValid(
                                                    AstralisPipelines.glowComposite);
                            RenderTarget renderTarget2;
                            RenderPipeline renderPipeline;
                            if (useOffscreenPass) {
                                if (this.enhancedGlowTarget.width != scaledWidth
                                        || this.enhancedGlowTarget.height != scaledHeight) {
                                    this.enhancedGlowTarget.resize(scaledWidth, scaledHeight);
                                }

                                renderTarget2 = this.enhancedGlowTarget;
                                renderPipeline = AstralisPipelines.handGlowOffscreen;
                            } else {
                                renderTarget2 = renderTarget;
                                renderPipeline = AstralisPipelines.handGlow;
                            }

                            CommandEncoder commandEncoder =
                                    RenderSystem.getDevice().createCommandEncoder();
                            commandEncoder.writeToBuffer(
                                    this.enhancedGlowParamsBuffer.slice(),
                                    this.enhancedGlowParamsData);

                            try (RenderPass renderPass =
                                    commandEncoder.createRenderPass(
                                            () -> "Sph Enhanced Glow Final",
                                            renderTarget2.getColorTextureView(),
                                            useOffscreenPass
                                                    ? Optional.of(FullscreenPassRenderer.clearColor)
                                                    : Optional.empty())) {
                                renderPass.setPipeline(renderPipeline);
                                renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                                renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

                                renderPass.bindTexture(
                                        "textureIn", gpuTextureView2, linearClampSampler);

                                renderPass.bindTexture(
                                        "originalIn", gpuTextureView, linearClampSampler);

                                renderPass.setUniform(
                                        "SphHandGlowParams", this.enhancedGlowParamsBuffer.slice());
                                renderPass.drawIndexed(3, 1, 0, 0, 0);
                            }

                            if (useOffscreenPass) {
                                try (RenderPass renderPass2 =
                                        commandEncoder.createRenderPass(
                                                () -> "Sph Enhanced Glow Composite",
                                                renderTarget.getColorTextureView(),
                                                Optional.empty())) {
                                    renderPass2.setPipeline(AstralisPipelines.glowComposite);
                                    renderPass2.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                                    renderPass2.setIndexBuffer(
                                            fullscreenIndexBuffer, IndexType.INT);

                                    renderPass2.bindTexture(
                                            "textureIn",
                                            this.enhancedGlowTarget.getColorTextureView(),
                                            linearClampSampler);
                                    renderPass2.drawIndexed(3, 1, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void putColor(ByteBuffer buffer, Color color, float scale) {
        buffer.putFloat(color.getRed() * scale);
        buffer.putFloat(color.getGreen() * scale);
        buffer.putFloat(color.getBlue() * scale);
        buffer.putFloat(color.getAlpha() * scale);
    }

    public void renderChamsOnly(
            GpuTexture gpuTexture,
            Color color,
            Color color2,
            float chamsMix,
            float gradientSpeed,
            float gradientAngle,
            float time,
            RenderTarget renderTarget) {
        if (gpuTexture != null
                && !(chamsMix <= 0.0F)
                && AstralisPipelines.isPipelineValid(AstralisPipelines.chams)) {
            GpuTextureView gpuTextureView = this.getOrCreateTextureView(gpuTexture);
            if (gpuTextureView != null) {
                this.renderChams(
                        gpuTextureView,
                        color,
                        color2,
                        chamsMix,
                        gradientSpeed,
                        gradientAngle,
                        time,
                        renderTarget);
            }
        }
    }
}
