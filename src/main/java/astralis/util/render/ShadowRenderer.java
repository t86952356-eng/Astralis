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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShadowRenderer extends AbstractBlurRenderer {
    private final GpuBuffer rectParamsBuffer;
    private final ByteBuffer rectParamsData;
    private final GpuBuffer compositeParamsBuffer;
    private final ByteBuffer compositeParamsData;
    private final RenderTarget shapesTarget;
    private final List<ShadowCacheEntry> cacheEntries;
    private long useCounter;

    public ShadowRenderer(Render2DContext context) {
        super(context);
        this.cacheEntries = new ArrayList<>(4);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier labelSupplier = () -> "Sph Shadow Rect Params UBO";

        this.rectParamsBuffer = gpuDevice.createBuffer(labelSupplier, 136, 48L);
        this.rectParamsData = MemoryUtil.memAlloc(48);
        GpuDevice gpuDevice2 = RenderSystem.getDevice();
        labelSupplier = () -> "Sph Shadow Composite Params UBO";

        this.compositeParamsBuffer = gpuDevice2.createBuffer(labelSupplier, 136, 16L);
        this.compositeParamsData = MemoryUtil.memAlloc(16);

        this.shapesTarget =
                new TextureTarget("Sph Shadow Shapes Target", 1, 1, true, GpuFormat.RGBA8_UNORM);

        for (int i = 0; i < 4; i++) {
            this.cacheEntries.add(new ShadowCacheEntry(i));
        }
    }

    @Override
    public void release() {
        super.release();
        this.rectParamsBuffer.close();
        MemoryUtil.memFree(this.rectParamsData);
        this.compositeParamsBuffer.close();
        MemoryUtil.memFree(this.compositeParamsData);
        this.shapesTarget.destroyBuffers();

        for (ShadowCacheEntry entry : this.cacheEntries) {
            entry.maskTarget.destroyBuffers();
            entry.shadowTarget.destroyBuffers();
        }

        this.cacheEntries.clear();
    }

    public void renderRectShadow(
            float x, float y, float width, float height, float radius, Color color, int blurSteps) {
        if (AstralisPipelines.isPipelineValid(AstralisPipelines.shadowRect)
                && AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur)) {
            int targetWidth = this.getRenderContext().getTargetWidth();
            int targetHeight = this.getRenderContext().getTargetHeight();
            float guiScale = this.getRenderContext().getGuiScale();
            int pixelX = (int) (x * guiScale);
            int pixelY = (int) (targetHeight - y * guiScale - height * guiScale);
            int pixelWidth = (int) (width * guiScale);
            int pixelHeight = (int) (height * guiScale);
            float pixelRadius = radius * guiScale;
            this.resizeBlurTargets(targetWidth, targetHeight);
            int blurSpread = blurSteps * 4;

            int padding = blurSpread + 10;
            int regionX = Math.max(0, pixelX - padding);
            int regionY = Math.max(0, pixelY - padding);
            int regionWidth = Math.min(targetWidth - regionX, pixelWidth + padding * 2);
            int regionHeight = Math.min(targetHeight - regionY, pixelHeight + padding * 2);
            this.rectParamsData.clear();
            this.rectParamsData.putFloat(pixelX).putFloat(pixelY);
            this.rectParamsData.putFloat(pixelWidth).putFloat(pixelHeight);
            this.rectParamsData.putFloat(pixelRadius);
            this.rectParamsData.putFloat(0.0F);
            this.rectParamsData.putFloat(0.0F).putFloat(0.0F);
            this.rectParamsData.putFloat(color.getRed() / 255.0F);
            this.rectParamsData.putFloat(color.getGreen() / 255.0F);
            this.rectParamsData.putFloat(color.getBlue() / 255.0F);
            this.rectParamsData.putFloat(color.getAlpha() / 255.0F);
            this.rectParamsData.flip();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.writeToBuffer(this.rectParamsBuffer.slice(), this.rectParamsData);

            try (RenderPass renderPass =
                    commandEncoder.createRenderPass(
                            () -> "Sph Shadow Rect",
                            this.secondBlurTarget.getColorTextureView(),
                            Optional.of(FullscreenPassRenderer.clearColor))) {
                renderPass.setPipeline(AstralisPipelines.shadowRect);
                renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);
                int passWidth = this.secondBlurTarget.width;
                int passHeight = this.secondBlurTarget.height;
                int scissorX = Math.max(0, regionX);
                int scissorY = Math.max(0, regionY);
                int scissorWidth = Math.min(regionWidth, passWidth - scissorX);
                int scissorHeight = Math.min(regionHeight, passHeight - scissorY);
                if (scissorWidth > 0 && scissorHeight > 0) {
                    renderPass.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
                }

                renderPass.setUniform("SphShadowRectParams", this.rectParamsBuffer.slice());
                renderPass.drawIndexed(3, 1, 0, 0, 0);
            }

            RenderTarget renderTarget =
                    this.runLegacyKawase(
                            this.secondBlurTarget.getColorTextureView(),
                            Math.max(1, blurSteps),
                            1.0F / targetWidth,
                            1.0F / targetHeight,
                            1.0F / targetWidth,
                            1.0F / targetHeight);
            this.drawFinalKawasePass(
                    renderTarget,
                    this.getRenderContext().getRenderTarget(),
                    1.0F / targetWidth,
                    1.0F / targetHeight,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    regionX,
                    regionY,
                    regionWidth,
                    regionHeight,
                    false);
        }
    }

    private ShadowCacheEntry findCacheEntry(
            long contentHash, int width, int height, int blurSteps, float scale, float guiScale) {
        int scaleBits = Float.floatToIntBits(scale);
        int guiScaleBits = Float.floatToIntBits(guiScale);
        Iterator iterator = this.cacheEntries.iterator();

        while (iterator.hasNext()) {
            ShadowCacheEntry entry = (ShadowCacheEntry) iterator.next();
            if (entry.valid
                    && entry.contentHash == contentHash
                    && entry.width == width
                    && entry.height == height
                    && entry.blurSteps == blurSteps
                    && entry.scaleBits == scaleBits
                    && entry.guiScaleBits == guiScaleBits) {
                entry.lastUsed = ++this.useCounter;
                return entry;
            }
        }

        return null;
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.shadowRect;
    }

    public void renderRecordedShapes(Color color, int blurSteps, float scale) {
        if (AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur)
                && AstralisPipelines.isPipelineValid(AstralisPipelines.shadowComposite)) {
            Render2DContext context = this.getRenderContext();
            if (context.hasShadowRects()) {
                int targetWidth = context.getTargetWidth();
                int targetHeight = context.getTargetHeight();
                int blurWidth = Math.max(1, (int) (targetWidth * scale));
                int blurHeight = Math.max(1, (int) (targetHeight * scale));
                int steps = Math.max(1, blurSteps);
                long contentHash = context.computeShadowMaskHash();
                RenderTarget renderTarget = context.getRenderTarget();
                ShadowCacheEntry cacheEntry =
                        this.findCacheEntry(
                                contentHash,
                                targetWidth,
                                targetHeight,
                                steps,
                                scale,
                                context.getGuiScale());
                if (cacheEntry == null) {
                    cacheEntry = this.acquireCacheEntry();
                    cacheEntry.valid = false;
                    if (cacheEntry.maskTarget.width != targetWidth
                            || cacheEntry.maskTarget.height != targetHeight) {
                        cacheEntry.maskTarget.resize(targetWidth, targetHeight);
                    }

                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "Sph Recorded Shadow Clear",
                                    cacheEntry.maskTarget.getColorTextureView(),
                                    Optional.of(FullscreenPassRenderer.clearColor));
                    if (renderPass != null) {
                        renderPass.close();
                    }

                    context.setRenderTarget(cacheEntry.maskTarget);
                    context.replayShadowMask();
                    context.setRenderTarget(renderTarget);
                    this.resizeBlurTargets(blurWidth, blurHeight);
                    RenderTarget renderTarget2 =
                            this.runLegacyKawase(
                                    cacheEntry.maskTarget.getColorTextureView(),
                                    steps,
                                    1.0F / targetWidth,
                                    1.0F / targetHeight,
                                    1.0F / blurWidth,
                                    1.0F / blurHeight);
                    if (cacheEntry.shadowTarget.width != blurWidth
                            || cacheEntry.shadowTarget.height != blurHeight) {
                        cacheEntry.shadowTarget.resize(blurWidth, blurHeight);
                    }

                    this.drawFinalKawasePass(
                            renderTarget2,
                            cacheEntry.shadowTarget,
                            1.0F / blurWidth,
                            1.0F / blurHeight,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            -1,
                            -1,
                            -1,
                            -1,
                            false);
                    cacheEntry.contentHash = contentHash;
                    cacheEntry.width = targetWidth;
                    cacheEntry.height = targetHeight;
                    cacheEntry.blurSteps = steps;
                    cacheEntry.scaleBits = Float.floatToIntBits(scale);
                    cacheEntry.guiScaleBits = Float.floatToIntBits(context.getGuiScale());
                    cacheEntry.lastUsed = ++this.useCounter;
                    cacheEntry.valid = true;
                }

                this.compositeShadow(
                        renderTarget, cacheEntry.shadowTarget, cacheEntry.maskTarget, color);
            }
        }
    }

    public void render(
            GpuTexture gpuTexture,
            Color color,
            int blurSteps,
            RenderTarget renderTarget,
            int width,
            int height) {
        if (gpuTexture != null) {
            if (AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur)
                    && AstralisPipelines.isPipelineValid(AstralisPipelines.shadowComposite)) {
                this.resizeBlurTargets(width, height);
                GpuTextureView gpuTextureView = this.getOrCreateTextureView(gpuTexture);
                if (gpuTextureView != null) {
                    RenderTarget renderTarget2 =
                            this.runLegacyKawase(
                                    gpuTextureView,
                                    Math.max(1, blurSteps),
                                    1.0F / width,
                                    1.0F / height,
                                    1.0F / width,
                                    1.0F / height);
                    this.writeCompositeColor(color);
                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    commandEncoder.writeToBuffer(
                            this.compositeParamsBuffer.slice(), this.compositeParamsData);

                    try (RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "Sph Shadow Composite",
                                    renderTarget.getColorTextureView(),
                                    Optional.empty())) {
                        renderPass.setPipeline(AstralisPipelines.shadowComposite);
                        renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                        renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

                        renderPass.bindTexture(
                                "u_Texture",
                                renderTarget2.getColorTextureView(),
                                linearClampSampler);

                        renderPass.bindTexture(
                                "u_OriginalTexture", gpuTextureView, linearClampSampler);

                        renderPass.setUniform(
                                "SphShadowCompositeParams", this.compositeParamsBuffer.slice());
                        renderPass.drawIndexed(3, 1, 0, 0, 0);
                    }
                }
            }
        }
    }

    private ShadowCacheEntry acquireCacheEntry() {
        ShadowCacheEntry oldestEntry = this.cacheEntries.get(0);

        for (ShadowCacheEntry entry : this.cacheEntries) {
            if (!entry.valid) {
                return entry;
            }

            if (entry.lastUsed < oldestEntry.lastUsed) {
                oldestEntry = entry;
            }
        }

        return oldestEntry;
    }

    private void compositeShadow(
            RenderTarget renderTarget,
            RenderTarget renderTarget2,
            RenderTarget renderTarget3,
            Color color) {
        this.writeCompositeColor(color);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.writeToBuffer(this.compositeParamsBuffer.slice(), this.compositeParamsData);

        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        () -> "Sph Shadow Shapes Composite",
                        renderTarget.getColorTextureView(),
                        Optional.empty())) {
            renderPass.setPipeline(AstralisPipelines.shadowComposite);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
            renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

            renderPass.bindTexture(
                    "u_Texture", renderTarget2.getColorTextureView(), linearClampSampler);

            renderPass.bindTexture(
                    "u_OriginalTexture", renderTarget3.getColorTextureView(), linearClampSampler);

            renderPass.setUniform("SphShadowCompositeParams", this.compositeParamsBuffer.slice());
            renderPass.drawIndexed(3, 1, 0, 0, 0);
        }
    }

    public void renderShapes(
            Consumer<Render2DContext> shapeDrawer, Color color, int blurSteps, float scale) {
        if (AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur)
                && AstralisPipelines.isPipelineValid(AstralisPipelines.shadowComposite)) {
            Render2DContext context = this.getRenderContext();
            int targetWidth = context.getTargetWidth();
            int targetHeight = context.getTargetHeight();
            int blurWidth = Math.max(1, (int) (targetWidth * scale));
            int blurHeight = Math.max(1, (int) (targetHeight * scale));
            if (this.shapesTarget.width != targetWidth
                    || this.shapesTarget.height != targetHeight) {
                this.shapesTarget.resize(targetWidth, targetHeight);
            }

            context.flush();
            RenderTarget renderTarget = context.getRenderTarget();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderPass renderPass =
                    commandEncoder.createRenderPass(
                            () -> "Sph Shadow Clear",
                            this.shapesTarget.getColorTextureView(),
                            Optional.of(FullscreenPassRenderer.clearColor));
            if (renderPass != null) {
                renderPass.close();
            }

            context.setRenderTarget(this.shapesTarget);
            context.beginHashing();
            shapeDrawer.accept(context);
            context.flush();
            long contentHash = context.endHashing();
            context.setRenderTarget(renderTarget);
            int steps = Math.max(1, blurSteps);
            ShadowCacheEntry cachedEntry =
                    this.findCacheEntry(
                            contentHash,
                            targetWidth,
                            targetHeight,
                            steps,
                            scale,
                            context.getGuiScale());
            RenderTarget renderTarget2;
            if (cachedEntry != null) {
                renderTarget2 = cachedEntry.shadowTarget;
            } else {
                this.resizeBlurTargets(blurWidth, blurHeight);
                RenderTarget renderTarget3 =
                        this.runLegacyKawase(
                                this.shapesTarget.getColorTextureView(),
                                steps,
                                1.0F / targetWidth,
                                1.0F / targetHeight,
                                1.0F / blurWidth,
                                1.0F / blurHeight);
                ShadowCacheEntry newEntry = this.acquireCacheEntry();
                if (newEntry.shadowTarget.width != blurWidth
                        || newEntry.shadowTarget.height != blurHeight) {
                    newEntry.shadowTarget.resize(blurWidth, blurHeight);
                }

                this.drawFinalKawasePass(
                        renderTarget3,
                        newEntry.shadowTarget,
                        1.0F / blurWidth,
                        1.0F / blurHeight,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        -1,
                        -1,
                        -1,
                        -1,
                        false);
                newEntry.contentHash = contentHash;
                newEntry.width = targetWidth;
                newEntry.height = targetHeight;
                newEntry.blurSteps = steps;
                newEntry.scaleBits = Float.floatToIntBits(scale);
                newEntry.guiScaleBits = Float.floatToIntBits(context.getGuiScale());
                newEntry.valid = true;
                newEntry.lastUsed = ++this.useCounter;
                renderTarget2 = newEntry.shadowTarget;
            }

            this.compositeShadow(renderTarget, renderTarget2, this.shapesTarget, color);
        }
    }

    private void writeCompositeColor(Color color) {
        this.compositeParamsData.clear();
        this.compositeParamsData.putFloat(color.getRed() * 0.003921569F);
        this.compositeParamsData.putFloat(color.getGreen() * 0.003921569F);
        this.compositeParamsData.putFloat(color.getBlue() * 0.003921569F);
        this.compositeParamsData.putFloat(color.getAlpha() * 0.003921569F);
        this.compositeParamsData.flip();
    }
}
