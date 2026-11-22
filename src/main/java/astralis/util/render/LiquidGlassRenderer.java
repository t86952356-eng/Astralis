package astralis.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class LiquidGlassRenderer extends FullscreenPassRenderer {
    private static final float DEFAULT_POWER_FACTOR = 5.0F;
    private static final float DEFAULT_REFRACTION_A = 1.0F;
    private static final float DEFAULT_REFRACTION_B = 2.3F;
    private static final float DEFAULT_REFRACTION_C = 5.2F;
    private static final float DEFAULT_REFRACTION_D = 6.9F;
    private static final float DEFAULT_REFRACTION_POWER = 2.0F;
    private static final float DEFAULT_NOISE = 0.025F;
    private static final float DEFAULT_GLOW_WEIGHT = 0.25F;
    private static final float DEFAULT_GLOW_EDGE_START = 0.08F;
    private static final float DEFAULT_GLOW_EDGE_END = 0.0F;
    private final GpuBuffer paramsBuffer;
    private final ByteBuffer paramsData;

    public LiquidGlassRenderer(Render2DContext renderContext) {
        super(renderContext);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier nameSupplier = () -> "Sph Liquid Glass Params UBO";

        this.paramsBuffer = gpuDevice.createBuffer(nameSupplier, 136, 96L);
        this.paramsData = MemoryUtil.memAlloc(96);
    }

    @Override
    public void release() {
        this.paramsBuffer.close();
        MemoryUtil.memFree(this.paramsData);
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.liquidGlass;
    }

    public void render(
            GpuTexture gpuTexture,
            float x,
            float y,
            float width,
            float height,
            float radius,
            Color color,
            float powerFactor,
            float refractionA,
            float refractionB,
            float refractionC,
            float refractionD,
            float refractionPower,
            float noise,
            float glowWeight,
            float glowEdgeStart,
            float glowEdgeEnd) {
        if (gpuTexture != null) {
            this.transformPoint(x, y);
            float transformedX = this.scratchVector.x;
            float transformedY = this.scratchVector.y;
            this.transformVector(width, height);
            float transformedWidth = this.scratchVector.x;
            float transformedHeight = this.scratchVector.y;
            this.transformVector(radius, 0.0F);
            float transformedRadius = this.scratchVector.x;
            int targetWidth = this.getRenderContext().getTargetWidth();
            int targetHeight = this.getRenderContext().getTargetHeight();
            double guiScale = this.getRenderContext().getGuiScale();
            int pixelX = (int) (transformedX * guiScale);
            int pixelWidth = (int) (transformedWidth * guiScale);
            int pixelHeight = (int) (transformedHeight * guiScale);
            int pixelY = (int) (targetHeight - transformedY * guiScale - pixelHeight);
            float centerX = pixelX + pixelWidth * 0.5F;
            float centerY = pixelY + pixelHeight * 0.5F;
            float halfWidth = pixelWidth * 0.5F;
            float halfHeight = pixelHeight * 0.5F;
            float radiusPixels = transformedRadius * (float) guiScale;
            this.paramsData.clear();
            this.paramsData.putFloat(centerX).putFloat(centerY);
            this.paramsData.putFloat(halfWidth).putFloat(halfHeight);
            this.paramsData.putFloat(targetWidth).putFloat(targetHeight);
            this.paramsData.putFloat(radiusPixels);
            this.paramsData.putFloat(powerFactor);
            this.paramsData.putFloat(refractionA);
            this.paramsData.putFloat(refractionB);
            this.paramsData.putFloat(refractionC);
            this.paramsData.putFloat(refractionD);
            this.paramsData.putFloat(refractionPower);
            this.paramsData.putFloat(noise);
            this.paramsData.putFloat(glowWeight);
            this.paramsData.putFloat(glowEdgeStart);
            this.paramsData.putFloat(glowEdgeEnd);
            this.paramsData.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
            this.paramsData.putFloat(color.getRed() / 255.0F);
            this.paramsData.putFloat(color.getGreen() / 255.0F);
            this.paramsData.putFloat(color.getBlue() / 255.0F);
            this.paramsData.putFloat(color.getAlpha() / 255.0F);
            this.paramsData.flip();

            try (GpuTextureView gpuTextureView =
                    RenderSystem.getDevice().createTextureView(gpuTexture)) {
                this.drawDefaultPass(
                        this.getRenderContext().getRenderTarget(),
                        false,
                        commandEncoder ->
                                commandEncoder.writeToBuffer(
                                        this.paramsBuffer.slice(), this.paramsData),
                        renderPass -> {
                            int renderTargetWidth = this.getRenderContext().getRenderTarget().width;
                            int renderTargetHeight =
                                    this.getRenderContext().getRenderTarget().height;
                            int scissorX = Math.max(0, pixelX - 1);
                            int scissorY = Math.max(0, pixelY - 1);
                            int scissorWidth =
                                    Math.min(pixelWidth + 2, renderTargetWidth - scissorX);
                            int scissorHeight =
                                    Math.min(pixelHeight + 2, renderTargetHeight - scissorY);
                            if (scissorWidth > 0 && scissorHeight > 0) {
                                renderPass.enableScissor(
                                        scissorX, scissorY, scissorWidth, scissorHeight);
                            }

                            renderPass.bindTexture(
                                    "u_BlurredScene", gpuTextureView, linearClampSampler);

                            renderPass.setUniform(
                                    "SphLiquidGlassParams", this.paramsBuffer.slice());
                        });
            }
        }
    }

    public void renderWithDefaults(
            GpuTexture gpuTexture,
            float x,
            float y,
            float width,
            float height,
            float radius,
            Color color) {
        this.render(
                gpuTexture,
                x,
                y,
                width,
                height,
                radius,
                color,
                5.0F,
                1.0F,
                2.3F,
                5.2F,
                6.9F,
                2.0F,
                0.025F,
                0.25F,
                0.08F,
                0.0F);
    }
}
