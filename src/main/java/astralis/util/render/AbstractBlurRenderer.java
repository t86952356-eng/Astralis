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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractBlurRenderer extends FullscreenPassRenderer {
    protected final RenderTarget firstBlurTarget;
    protected final RenderTarget secondBlurTarget;
    private final GpuBuffer kawaseParamsBuffer;
    private final ByteBuffer kawaseParamsData;
    private final GpuBuffer separableBlurParamsBuffer;
    private final ByteBuffer separableBlurParamsData;
    private final Reference2ObjectMap<GpuTexture, GpuTextureView> textureViewCache;

    protected AbstractBlurRenderer(Render2DContext context) {
        super(context);
        this.textureViewCache = new Reference2ObjectOpenHashMap<>(4);

        this.firstBlurTarget =
                new TextureTarget("Sph First Kawase Target", 1, 1, true, GpuFormat.RGBA8_UNORM);

        this.secondBlurTarget =
                new TextureTarget("Sph Second Kawase Target", 1, 1, true, GpuFormat.RGBA8_UNORM);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier labelSupplier = () -> "Sph Kawase Params UBO";

        this.kawaseParamsBuffer = gpuDevice.createBuffer(labelSupplier, 136, 48L);
        this.kawaseParamsData = MemoryUtil.memAlloc(48);
        GpuDevice gpuDevice2 = RenderSystem.getDevice();
        labelSupplier = () -> "Sph Separable Blur Params UBO";

        this.separableBlurParamsBuffer = gpuDevice2.createBuffer(labelSupplier, 136, 32L);
        this.separableBlurParamsData = MemoryUtil.memAlloc(32);
    }

    @Override
    public void release() {
        this.firstBlurTarget.destroyBuffers();
        this.secondBlurTarget.destroyBuffers();
        this.kawaseParamsBuffer.close();
        MemoryUtil.memFree(this.kawaseParamsData);
        this.separableBlurParamsBuffer.close();
        MemoryUtil.memFree(this.separableBlurParamsData);

        for (GpuTextureView gpuTextureView : this.textureViewCache.values()) {
            if (!gpuTextureView.isClosed()) {
                gpuTextureView.close();
            }
        }

        this.textureViewCache.clear();
    }

    protected boolean trySeparableBlur(
            GpuTextureView gpuTextureView,
            int radius,
            int blurWidth,
            int blurHeight,
            float sourceTexelWidth,
            float sourceTexelHeight,
            float blurTexelWidth,
            float blurTexelHeight,
            RenderTarget renderTarget,
            boolean opaque) {
        if (!AstralisPipelines.isPipelineValid(AstralisPipelines.separableBlur)) {
            return false;
        }

        this.resizeBlurTargets(blurWidth, blurHeight);
        this.runSeparableBlur(
                gpuTextureView,
                radius,
                sourceTexelWidth,
                sourceTexelHeight,
                blurTexelWidth,
                blurTexelHeight,
                renderTarget,
                opaque);
        return true;
    }

    protected void drawFinalKawasePass(
            RenderTarget renderTarget,
            RenderTarget renderTarget2,
            float texelWidth,
            float texelHeight,
            float centerX,
            float centerY,
            float halfWidth,
            float halfHeight,
            float cornerRadius,
            int scissorX,
            int scissorY,
            int scissorWidth,
            int scissorHeight,
            boolean opaque) {
        boolean plainCopy = cornerRadius <= 0.0F && !opaque;
        boolean opaqueCopy = cornerRadius <= 0.0F && opaque;
        RenderPipeline renderPipeline =
                plainCopy
                        ? AstralisPipelines.kawaseCopy
                        : (opaqueCopy
                                ? AstralisPipelines.kawaseOpaqueCopy
                                : AstralisPipelines.kawaseBlur);
        boolean copyPass = plainCopy || opaqueCopy;
        if (!AstralisPipelines.isPipelineValid(renderPipeline)) {
            if (!copyPass || !AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur)) {
                return;
            }

            renderPipeline = AstralisPipelines.kawaseBlur;
            copyPass = false;
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        if (!copyPass) {
            this.writeKawaseParams(
                    texelWidth,
                    texelHeight,
                    0.0F,
                    centerX,
                    centerY,
                    halfWidth,
                    halfHeight,
                    cornerRadius,
                    cornerRadius > 0.0F,
                    opaque);
            commandEncoder.writeToBuffer(this.kawaseParamsBuffer.slice(), this.kawaseParamsData);
        }

        GpuTextureView gpuTextureView = renderTarget.getColorTextureView();

        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        () -> "Sph Kawase Final",
                        renderTarget2.getColorTextureView(),
                        Optional.empty())) {
            renderPass.setPipeline(renderPipeline);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
            renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);
            if (scissorX >= 0) {
                int targetWidth = renderTarget2.width;
                int targetHeight = renderTarget2.height;
                int clampedX = Math.max(0, scissorX);
                int clampedY = Math.max(0, scissorY);
                int clampedWidth = Math.min(scissorWidth, targetWidth - clampedX);
                int clampedHeight = Math.min(scissorHeight, targetHeight - clampedY);
                if (clampedWidth > 0 && clampedHeight > 0) {
                    renderPass.enableScissor(clampedX, clampedY, clampedWidth, clampedHeight);
                }
            }

            renderPass.bindTexture("textureIn", gpuTextureView, linearClampSampler);
            if (!copyPass) {

                renderPass.setUniform("SphKawaseParams", this.kawaseParamsBuffer.slice());
            }

            renderPass.drawIndexed(3, 1, 0, 0, 0);
        }
    }

    protected RenderTarget blurTexture(
            GpuTextureView gpuTextureView,
            int radius,
            int blurWidth,
            int blurHeight,
            float sourceTexelWidth,
            float sourceTexelHeight,
            float blurTexelWidth,
            float blurTexelHeight) {
        return AstralisPipelines.isPipelineValid(AstralisPipelines.separableBlur)
                ? this.runSeparableBlur(
                        gpuTextureView,
                        radius,
                        sourceTexelWidth,
                        sourceTexelHeight,
                        blurTexelWidth,
                        blurTexelHeight,
                        this.secondBlurTarget,
                        false)
                : this.runLegacyKawase(
                        gpuTextureView,
                        radius,
                        sourceTexelWidth,
                        sourceTexelHeight,
                        blurTexelWidth,
                        blurTexelHeight);
    }

    protected boolean isBlurAvailable() {
        return AstralisPipelines.isPipelineValid(AstralisPipelines.separableBlur)
                || AstralisPipelines.isPipelineValid(AstralisPipelines.kawaseBlur);
    }

    @Override
    protected abstract RenderPipeline getDefaultPipeline();

    protected void resizeBlurTargets(int width, int height) {
        if (this.firstBlurTarget.width != width || this.firstBlurTarget.height != height) {
            this.firstBlurTarget.resize(width, height);
            this.secondBlurTarget.resize(width, height);
        }
    }

    protected GpuTextureView getOrCreateTextureView(GpuTexture gpuTexture) {
        GpuTextureView gpuTextureView = this.textureViewCache.get(gpuTexture);
        if (gpuTextureView == null || gpuTextureView.isClosed() || gpuTexture.isClosed()) {
            if (gpuTextureView != null && !gpuTextureView.isClosed()) {
                gpuTextureView.close();
            }

            if (gpuTexture.isClosed()) {
                this.textureViewCache.remove(gpuTexture);
                return null;
            }

            gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
            this.textureViewCache.put(gpuTexture, gpuTextureView);
        }

        return gpuTextureView;
    }

    private void writeKawaseParams(
            float texelWidth,
            float texelHeight,
            float offset,
            float centerX,
            float centerY,
            float halfWidth,
            float halfHeight,
            float cornerRadius,
            boolean rounded,
            boolean opaque) {
        this.kawaseParamsData.clear();
        this.kawaseParamsData.putFloat(texelWidth).putFloat(texelHeight);
        this.kawaseParamsData.putFloat(offset);
        this.kawaseParamsData.putFloat(0.0F);
        this.kawaseParamsData.putFloat(centerX).putFloat(centerY);
        this.kawaseParamsData.putFloat(halfWidth).putFloat(halfHeight);
        this.kawaseParamsData.putFloat(cornerRadius);
        this.kawaseParamsData.putInt(rounded ? 1 : 0);
        this.kawaseParamsData.putInt(opaque ? 1 : 0);
        this.kawaseParamsData.putFloat(0.0F);
        this.kawaseParamsData.flip();
    }

    protected boolean trySeparableBlurPreservingAlpha(
            GpuTextureView gpuTextureView,
            int radius,
            int blurWidth,
            int blurHeight,
            float sourceTexelWidth,
            float sourceTexelHeight,
            float blurTexelWidth,
            float blurTexelHeight,
            RenderTarget renderTarget) {
        return this.trySeparableBlur(
                gpuTextureView,
                radius,
                blurWidth,
                blurHeight,
                sourceTexelWidth,
                sourceTexelHeight,
                blurTexelWidth,
                blurTexelHeight,
                renderTarget,
                false);
    }

    private void writeSeparableBlurParams(
            float texelWidth,
            float texelHeight,
            float directionX,
            float directionY,
            float radius,
            boolean opaque) {
        this.separableBlurParamsData.clear();
        this.separableBlurParamsData.putFloat(texelWidth).putFloat(texelHeight);
        this.separableBlurParamsData.putFloat(directionX).putFloat(directionY);
        this.separableBlurParamsData.putFloat(radius);
        this.separableBlurParamsData.putFloat(opaque ? 1.0F : 0.0F);
        this.separableBlurParamsData.putFloat(0.0F).putFloat(0.0F);
        this.separableBlurParamsData.flip();
    }

    private RenderTarget runSeparableBlur(
            GpuTextureView gpuTextureView,
            int radius,
            float sourceTexelWidth,
            float sourceTexelHeight,
            float blurTexelWidth,
            float blurTexelHeight,
            RenderTarget renderTarget,
            boolean opaque) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        float blurRadius = Math.max(1, radius);
        this.writeSeparableBlurParams(
                sourceTexelWidth, sourceTexelHeight, 1.0F, 0.0F, blurRadius, opaque);
        commandEncoder.writeToBuffer(
                this.separableBlurParamsBuffer.slice(), this.separableBlurParamsData);

        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        () -> "Sph Separable Blur Horizontal",
                        this.firstBlurTarget.getColorTextureView(),
                        Optional.empty())) {
            renderPass.setPipeline(AstralisPipelines.separableBlur);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
            renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

            renderPass.bindTexture("textureIn", gpuTextureView, linearClampSampler);

            renderPass.setUniform("SphSeparableBlurParams", this.separableBlurParamsBuffer.slice());
            renderPass.drawIndexed(3, 1, 0, 0, 0);
        }

        this.writeSeparableBlurParams(
                blurTexelWidth, blurTexelHeight, 0.0F, 1.0F, blurRadius, opaque);
        commandEncoder.writeToBuffer(
                this.separableBlurParamsBuffer.slice(), this.separableBlurParamsData);

        try (RenderPass renderPass2 =
                commandEncoder.createRenderPass(
                        () -> "Sph Separable Blur Vertical",
                        renderTarget.getColorTextureView(),
                        Optional.empty())) {
            renderPass2.setPipeline(AstralisPipelines.separableBlur);
            renderPass2.setVertexBuffer(0, fullscreenVertexBuffer.slice());
            renderPass2.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

            renderPass2.bindTexture(
                    "textureIn", this.firstBlurTarget.getColorTextureView(), linearClampSampler);

            renderPass2.setUniform(
                    "SphSeparableBlurParams", this.separableBlurParamsBuffer.slice());
            renderPass2.drawIndexed(3, 1, 0, 0, 0);
        }

        return renderTarget;
    }

    protected RenderTarget blurTextureAtSize(
            GpuTextureView gpuTextureView, int radius, int width, int height) {
        float texelWidth = 1.0F / width;
        float texelHeight = 1.0F / height;
        return this.blurTexture(
                gpuTextureView,
                radius,
                width,
                height,
                texelWidth,
                texelHeight,
                texelWidth,
                texelHeight);
    }

    protected RenderTarget runLegacyKawase(
            GpuTextureView gpuTextureView,
            int iterations,
            float sourceTexelWidth,
            float sourceTexelHeight,
            float blurTexelWidth,
            float blurTexelHeight) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        this.writeKawaseParams(
                sourceTexelWidth,
                sourceTexelHeight,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                0.0F,
                false,
                false);
        commandEncoder.writeToBuffer(this.kawaseParamsBuffer.slice(), this.kawaseParamsData);

        try (RenderPass renderPass =
                commandEncoder.createRenderPass(
                        () -> "Sph Kawase 0",
                        this.firstBlurTarget.getColorTextureView(),
                        Optional.of(FullscreenPassRenderer.clearColor))) {
            renderPass.setPipeline(AstralisPipelines.kawaseBlur);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
            renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

            renderPass.bindTexture("textureIn", gpuTextureView, linearClampSampler);

            renderPass.setUniform("SphKawaseParams", this.kawaseParamsBuffer.slice());
            renderPass.drawIndexed(3, 1, 0, 0, 0);
        }

        RenderTarget renderTarget = this.firstBlurTarget;
        RenderTarget renderTarget2 = this.secondBlurTarget;

        for (int iteration = 1; iteration <= iterations; iteration++) {
            this.writeKawaseParams(
                    blurTexelWidth,
                    blurTexelHeight,
                    iteration + 0.5F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    false,
                    false);
            commandEncoder.writeToBuffer(this.kawaseParamsBuffer.slice(), this.kawaseParamsData);
            GpuTextureView gpuTextureView2 = renderTarget.getColorTextureView();
            RenderTarget renderTarget3 = renderTarget2;
            int passIndex = iteration;

            try (RenderPass renderPass2 =
                    commandEncoder.createRenderPass(
                            () -> "Sph Kawase " + passIndex,
                            renderTarget3.getColorTextureView(),
                            Optional.empty())) {
                renderPass2.setPipeline(AstralisPipelines.kawaseBlur);
                renderPass2.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                renderPass2.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);

                renderPass2.bindTexture("textureIn", gpuTextureView2, linearClampSampler);

                renderPass2.setUniform("SphKawaseParams", this.kawaseParamsBuffer.slice());
                renderPass2.drawIndexed(3, 1, 0, 0, 0);
            }

            RenderTarget renderTarget4 = renderTarget;
            renderTarget = renderTarget2;
            renderTarget2 = renderTarget4;
        }

        return renderTarget;
    }
}
