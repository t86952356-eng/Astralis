package astralis.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class SandboxShaderRenderer extends FullscreenPassRenderer {
    private final GpuBuffer paramsBuffer;
    private final ByteBuffer paramsData;

    public SandboxShaderRenderer(Render2DContext renderContext) {
        super(renderContext);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier nameSupplier = () -> "Sph Sandbox Params UBO";

        this.paramsBuffer = gpuDevice.createBuffer(nameSupplier, 136, 64L);
        this.paramsData = MemoryUtil.memAlloc(64);
    }

    @Override
    public void release() {
        this.paramsBuffer.close();
        MemoryUtil.memFree(this.paramsData);
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.sandbox;
    }

    public void render(
            float x,
            float y,
            float width,
            float height,
            float time,
            Color color,
            Color color2,
            float alpha) {
        int targetHeight = this.getRenderContext().getTargetHeight();
        float guiScale = this.getRenderContext().getGuiScale();
        int pixelX = (int) (x * guiScale);
        int pixelWidth = (int) (width * guiScale);
        int pixelHeight = (int) (height * guiScale);
        int pixelY = (int) (targetHeight - y * guiScale - pixelHeight);
        this.paramsData.clear();
        this.paramsData.putFloat(pixelWidth).putFloat(pixelHeight);
        this.paramsData.putFloat(pixelX).putFloat(pixelY);
        this.paramsData
                .putFloat(color.getRed() / 255.0F)
                .putFloat(color.getGreen() / 255.0F)
                .putFloat(color.getBlue() / 255.0F)
                .putFloat(1.0F);
        this.paramsData
                .putFloat(color2.getRed() / 255.0F)
                .putFloat(color2.getGreen() / 255.0F)
                .putFloat(color2.getBlue() / 255.0F)
                .putFloat(1.0F);
        this.paramsData.putFloat(time);
        this.paramsData.putFloat(alpha);
        this.paramsData.putFloat(0.0F).putFloat(0.0F);
        this.paramsData.flip();
        this.drawDefaultPass(
                this.getRenderContext().getRenderTarget(),
                false,
                commandEncoder ->
                        commandEncoder.writeToBuffer(this.paramsBuffer.slice(), this.paramsData),
                renderPass -> {
                    renderPass.setUniform("SphSandboxParams", this.paramsBuffer.slice());
                    int renderTargetWidth = this.getRenderContext().getRenderTarget().width;
                    int renderTargetHeight = this.getRenderContext().getRenderTarget().height;
                    int scissorX = Math.max(0, pixelX);
                    int scissorY = Math.max(0, pixelY);
                    int scissorWidth = Math.min(pixelWidth, renderTargetWidth - scissorX);
                    int scissorHeight = Math.min(pixelHeight, renderTargetHeight - scissorY);
                    if (scissorWidth > 0 && scissorHeight > 0) {
                        renderPass.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
                    }
                });
    }
}
