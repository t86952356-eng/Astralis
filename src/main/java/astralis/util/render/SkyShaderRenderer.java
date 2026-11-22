package astralis.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

public class SkyShaderRenderer extends FullscreenPassRenderer {
    private static final int SKY_PARAMS_SIZE = 112;
    private static final int CUSTOM_SKY_PARAMS_SIZE = 16;
    private final GpuBuffer skyParamsBuffer;
    private final GpuBuffer customSkyParamsBuffer;
    private final ByteBuffer skyParamsData;
    private final ByteBuffer customSkyParamsData;

    public SkyShaderRenderer(Render2DContext context) {
        super(context);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier labelSupplier = () -> "Sph Sky Params UBO";

        this.skyParamsBuffer = gpuDevice.createBuffer(labelSupplier, 136, 112L);
        GpuDevice gpuDevice2 = RenderSystem.getDevice();
        labelSupplier = () -> "Sph Custom Sky Params UBO";

        this.customSkyParamsBuffer = gpuDevice2.createBuffer(labelSupplier, 136, 16L);
        this.skyParamsData = MemoryUtil.memAlloc(112);
        this.customSkyParamsData = MemoryUtil.memAlloc(16);
    }

    @Override
    public void release() {
        this.skyParamsBuffer.close();
        this.customSkyParamsBuffer.close();
        MemoryUtil.memFree(this.skyParamsData);
        MemoryUtil.memFree(this.customSkyParamsData);
    }

    public void render(
            Color color,
            Color color2,
            float time,
            float scale,
            Matrix4f matrix4f,
            boolean useSavedShader,
            boolean shader2D) {
        RenderTarget renderTarget = this.getRenderContext().getRenderTarget();
        if (renderTarget != null) {
            this.skyParamsData.clear();
            this.skyParamsData
                    .putFloat(color.getRed() / 255.0F)
                    .putFloat(color.getGreen() / 255.0F)
                    .putFloat(color.getBlue() / 255.0F)
                    .putFloat(1.0F);
            this.skyParamsData
                    .putFloat(color2.getRed() / 255.0F)
                    .putFloat(color2.getGreen() / 255.0F)
                    .putFloat(color2.getBlue() / 255.0F)
                    .putFloat(1.0F);
            this.skyParamsData.putFloat(renderTarget.width).putFloat(renderTarget.height);
            this.skyParamsData.putFloat(time).putFloat(scale);
            matrix4f.get(this.skyParamsData);
            this.skyParamsData.flip();
            boolean useCustomSky =
                    useSavedShader
                            && AstralisPipelines.isPipelineValid(AstralisPipelines.skyCustom);
            RenderPipeline renderPipeline =
                    useCustomSky ? AstralisPipelines.skyCustom : AstralisPipelines.skyShader;
            this.customSkyParamsData.clear();
            this.customSkyParamsData
                    .putFloat(shader2D ? 1.0F : 0.0F)
                    .putFloat(0.0F)
                    .putFloat(0.0F)
                    .putFloat(0.0F);
            this.customSkyParamsData.flip();
            this.drawPass(
                    renderPipeline,
                    renderTarget,
                    false,
                    commandEncoder -> {
                        commandEncoder.writeToBuffer(
                                this.skyParamsBuffer.slice(), this.skyParamsData);
                        if (useCustomSky) {
                            commandEncoder.writeToBuffer(
                                    this.customSkyParamsBuffer.slice(), this.customSkyParamsData);
                        }
                    },
                    renderPass -> {
                        renderPass.setUniform("SphSkyParams", this.skyParamsBuffer.slice());
                        if (useCustomSky) {

                            renderPass.setUniform(
                                    "SphSkyCustomParams", this.customSkyParamsBuffer.slice());
                        }
                    });
        }
    }

    @Override
    protected RenderPipeline getDefaultPipeline() {
        return AstralisPipelines.skyShader;
    }
}
