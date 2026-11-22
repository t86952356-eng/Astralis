package astralis.util.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;

import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class FullscreenPassRenderer {
    protected static GpuBuffer fullscreenVertexBuffer;
    protected static GpuBuffer fullscreenIndexBuffer;
    protected static GpuSampler linearClampSampler;
    public static Vector4fc clearColor;
    protected final Vector4f scratchVector = new Vector4f();
    private final Render2DContext renderContext;

    protected FullscreenPassRenderer(Render2DContext renderContext) {
        this.renderContext = renderContext;
        ensureStaticInit();
    }

    public void release() {}

    protected void drawPass(
            RenderPipeline renderPipeline,
            RenderTarget renderTarget,
            boolean clearTarget,
            Consumer<CommandEncoder> encoderConsumer,
            Consumer<RenderPass> passConsumer) {
        if (AstralisPipelines.isPipelineValid(renderPipeline)) {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (encoderConsumer != null) {
                encoderConsumer.accept(commandEncoder);
            }

            try (RenderPass renderPass =
                    commandEncoder.createRenderPass(
                            () -> "Sph Effect",
                            renderTarget.getColorTextureView(),
                            clearTarget ? Optional.of(clearColor) : Optional.empty())) {
                renderPass.setPipeline(renderPipeline);
                renderPass.setVertexBuffer(0, fullscreenVertexBuffer.slice());
                renderPass.setIndexBuffer(fullscreenIndexBuffer, IndexType.INT);
                if (passConsumer != null) {
                    passConsumer.accept(renderPass);
                }

                renderPass.drawIndexed(3, 1, 0, 0, 0);
            }
        }
    }

    protected Render2DContext getRenderContext() {
        return this.renderContext;
    }

    protected abstract RenderPipeline getDefaultPipeline();

    public static void releaseSharedBuffers() {
        if (fullscreenVertexBuffer != null) {
            fullscreenVertexBuffer.close();
            fullscreenVertexBuffer = null;
        }

        if (fullscreenIndexBuffer != null) {
            fullscreenIndexBuffer.close();
            fullscreenIndexBuffer = null;
        }
    }

    protected void transformPoint(float x, float y) {
        this.scratchVector.set(x, y, 0.0F, 1.0F);
        this.renderContext.getMatrixStack().transform(this.scratchVector);
    }

    protected void transformVector(float x, float y) {
        this.scratchVector.set(x, y, 0.0F, 0.0F);
        this.renderContext.getMatrixStack().transform(this.scratchVector);
    }

    private static void ensureStaticInit() {

        if (fullscreenVertexBuffer == null) {

            ByteBuffer vertexData = MemoryUtil.memAlloc(24);
            vertexData.putFloat(-1.0F).putFloat(-1.0F);
            vertexData.putFloat(3.0F).putFloat(-1.0F);
            vertexData.putFloat(-1.0F).putFloat(3.0F);
            vertexData.flip();
            GpuDevice gpuDevice = RenderSystem.getDevice();
            Supplier vertexLabel = () -> "Sph Fullscreen VBO";

            fullscreenVertexBuffer = gpuDevice.createBuffer(vertexLabel, 32, vertexData);
            MemoryUtil.memFree(vertexData);

            ByteBuffer indexData = MemoryUtil.memAlloc(12);
            indexData.putInt(0).putInt(1).putInt(2);
            indexData.flip();
            GpuDevice gpuDevice2 = RenderSystem.getDevice();
            Supplier indexLabel = () -> "Sph Fullscreen IBO";

            fullscreenIndexBuffer = gpuDevice2.createBuffer(indexLabel, 64, indexData);
            MemoryUtil.memFree(indexData);
            linearClampSampler =
                    RenderSystem.getSamplerCache()
                            .getSampler(
                                    AddressMode.CLAMP_TO_EDGE,
                                    AddressMode.CLAMP_TO_EDGE,
                                    FilterMode.LINEAR,
                                    FilterMode.LINEAR,
                                    false);
        }
    }

    protected void drawDefaultPass(
            RenderTarget renderTarget,
            boolean clearTarget,
            Consumer<CommandEncoder> encoderConsumer,
            Consumer<RenderPass> passConsumer) {
        this.drawPass(
                this.getDefaultPipeline(),
                renderTarget,
                clearTarget,
                encoderConsumer,
                passConsumer);
    }

    static {
        clearColor = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
    }
}
