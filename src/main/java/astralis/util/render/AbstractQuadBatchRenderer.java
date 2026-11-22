package astralis.util.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.server.packs.resources.ResourceManager;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

public abstract class AbstractQuadBatchRenderer {
    protected static final int MAX_QUADS = 10000;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    protected final FloatBuffer vertexFloats;
    protected final int floatsPerVertex;
    protected int quadCount;
    private final GpuBuffer gpuVertexBuffer;
    private final GpuBuffer gpuIndexBuffer;
    private final GpuBuffer projectionUniform;
    private final Matrix4f projectionMatrix;
    private int lastProjectionWidth;
    private int lastProjectionHeight;
    private boolean projectionDirty;
    private final ByteBuffer projectionBytes;
    private final Vector4f scratchVector;
    private final Render2DContext context;

    public AbstractQuadBatchRenderer(
            ResourceManager resourceManager, Render2DContext context, int floatsPerVertex) {

        this.quadCount = 0;
        this.projectionMatrix = new Matrix4f();
        this.lastProjectionWidth = -1;
        this.lastProjectionHeight = -1;
        this.projectionDirty = true;

        this.projectionBytes = MemoryUtil.memAlloc(64);
        this.scratchVector = new Vector4f();
        this.context = context;
        this.floatsPerVertex = floatsPerVertex;

        this.vertexFloats = MemoryUtil.memAllocFloat(40000 * floatsPerVertex);
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Supplier vertexBufferLabel = () -> "Sph VBO";

        long vertexFloatCapacity;

        vertexFloatCapacity = 40000L * floatsPerVertex;

        this.gpuVertexBuffer =
                gpuDevice.createBuffer(vertexBufferLabel, 40, vertexFloatCapacity * 4L);

        ByteBuffer indexData = MemoryUtil.memAlloc(120000);
        int baseVertex = 0;
        int quadIndex = 0;

        while (true) {

            if (quadIndex >= 10000) {
                indexData.flip();
                Supplier indexBufferLabel = () -> "Sph EBO";

                this.gpuIndexBuffer = gpuDevice.createBuffer(indexBufferLabel, 72, indexData);
                MemoryUtil.memFree(indexData);
                Supplier projectionBufferLabel = () -> "Sph Projection UBO";

                this.projectionUniform = gpuDevice.createBuffer(projectionBufferLabel, 136, 64L);
                return;
            }

            indexData
                    .putShort((short) baseVertex)
                    .putShort((short) (baseVertex + 1))
                    .putShort((short) (baseVertex + 2));
            indexData
                    .putShort((short) (baseVertex + 2))
                    .putShort((short) (baseVertex + 3))
                    .putShort((short) baseVertex);
            baseVertex += 4;
            quadIndex++;
        }
    }

    public void releaseResources() {
        this.gpuVertexBuffer.close();
        this.gpuIndexBuffer.close();
        this.projectionUniform.close();
        MemoryUtil.memFree(this.vertexFloats);
        MemoryUtil.memFree(this.projectionBytes);
    }

    public void endBatch() {
        this.flush();
    }

    protected abstract RenderPipeline getRenderPipeline();

    protected void bindPassResources(RenderPass renderPass) {}

    public void beginBatch() {
        this.quadCount = 0;
        this.vertexFloats.clear();
        this.updateProjection();
    }

    protected void putTransformedPosition(FloatBuffer buffer, float x, float y) {
        Matrix4fStack matrix = this.context.getMatrixStack();
        if ((matrix.properties() & 4) != 0) {
            buffer.put(x).put(y);
        } else {
            int properties = matrix.properties();

            if ((properties & 8) != 0) {
                buffer.put(x + matrix.m30()).put(y + matrix.m31());
            } else {
                this.scratchVector.set(x, y, 0.0F, 1.0F);
                matrix.transform(this.scratchVector);
                buffer.put(this.scratchVector.x).put(this.scratchVector.y);
            }
        }
    }

    public Render2DContext getContext() {
        return this.context;
    }

    protected void afterDraw() {}

    protected void prepareDraw() {}

    protected void uploadExtraBuffers(CommandEncoder commandEncoder) {}

    protected void putVertexPosition(float x, float y) {
        this.putTransformedPosition(this.vertexFloats, x, y);
    }

    public void flush() {
        if (this.quadCount != 0) {
            if (!AstralisPipelines.isPipelineValid(this.getRenderPipeline())) {
                this.quadCount = 0;
                this.vertexFloats.clear();
            } else {
                GpuTextureView gpuTextureView =
                        this.usesDepthAttachment()
                                ? this.context.getRenderTarget().getDepthTextureView()
                                : null;
                if (this.usesDepthAttachment() && gpuTextureView == null) {
                    this.quadCount = 0;
                    this.vertexFloats.clear();
                } else {
                    ScissorRect scissor = this.context.getCurrentScissor();
                    boolean hasScissor = scissor != null;
                    int scissorX = 0;
                    int scissorY = 0;
                    int scissorWidth = 0;
                    int scissorHeight = 0;
                    if (hasScissor) {
                        int targetWidth = this.context.getRenderTarget().width;
                        int targetHeight = this.context.getRenderTarget().height;
                        scissorX = Math.max(0, scissor.getX());
                        scissorY = Math.max(0, scissor.getY());
                        scissorWidth = Math.min(scissor.getWidth(), targetWidth - scissorX);
                        scissorHeight = Math.min(scissor.getHeight(), targetHeight - scissorY);
                        if (scissorWidth <= 0 || scissorHeight <= 0) {
                            this.quadCount = 0;
                            this.vertexFloats.clear();
                            return;
                        }
                    }

                    this.prepareDraw();
                    this.vertexFloats.flip();
                    int byteCount = this.vertexFloats.remaining() * 4;
                    ByteBuffer vertexBytes =
                            MemoryUtil.memByteBuffer(
                                    MemoryUtil.memAddress(this.vertexFloats), byteCount);
                    CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                    commandEncoder.writeToBuffer(
                            this.gpuVertexBuffer.slice(0L, byteCount), vertexBytes);
                    this.uploadExtraBuffers(commandEncoder);

                    try (RenderPass renderPass =
                            commandEncoder.createRenderPass(
                                    () -> "Sph Batch",
                                    this.context.getRenderTarget().getColorTextureView(),
                                    Optional.empty(),
                                    gpuTextureView,
                                    OptionalDouble.empty())) {
                        if (hasScissor) {
                            renderPass.enableScissor(
                                    scissorX, scissorY, scissorWidth, scissorHeight);
                        }

                        renderPass.setPipeline(this.getRenderPipeline());

                        renderPass.setUniform(
                                "SphProjection", this.projectionUniform.slice(0L, 64L));
                        renderPass.setVertexBuffer(0, this.gpuVertexBuffer.slice());
                        renderPass.setIndexBuffer(this.gpuIndexBuffer, IndexType.SHORT);
                        this.bindPassResources(renderPass);

                        renderPass.drawIndexed(this.quadCount * 6, 1, 0, 0, 0);
                    }

                    this.quadCount = 0;
                    this.vertexFloats.clear();
                    this.afterDraw();
                }
            }
        }
    }

    protected void renderRetained(GpuBuffer gpuBuffer, int retainedQuadCount) {
        if (retainedQuadCount != 0 && AstralisPipelines.isPipelineValid(this.getRenderPipeline())) {
            GpuTextureView gpuTextureView =
                    this.usesDepthAttachment()
                            ? this.context.getRenderTarget().getDepthTextureView()
                            : null;
            if (!this.usesDepthAttachment() || gpuTextureView != null) {
                ScissorRect scissor = this.context.getCurrentScissor();
                boolean hasScissor = scissor != null;
                int scissorX = 0;
                int scissorY = 0;
                int scissorWidth = 0;
                int scissorHeight = 0;
                if (hasScissor) {
                    int targetWidth = this.context.getRenderTarget().width;
                    int targetHeight = this.context.getRenderTarget().height;
                    scissorX = Math.max(0, scissor.getX());
                    scissorY = Math.max(0, scissor.getY());
                    scissorWidth = Math.min(scissor.getWidth(), targetWidth - scissorX);
                    scissorHeight = Math.min(scissor.getHeight(), targetHeight - scissorY);
                    if (scissorWidth <= 0 || scissorHeight <= 0) {
                        return;
                    }
                }

                this.updateProjection();
                CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
                this.uploadExtraBuffers(commandEncoder);

                try (RenderPass renderPass =
                        commandEncoder.createRenderPass(
                                () -> "Sph Retained Batch",
                                this.context.getRenderTarget().getColorTextureView(),
                                Optional.empty(),
                                gpuTextureView,
                                OptionalDouble.empty())) {
                    if (hasScissor) {
                        renderPass.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
                    }

                    renderPass.setPipeline(this.getRenderPipeline());

                    renderPass.setUniform("SphProjection", this.projectionUniform.slice(0L, 64L));
                    renderPass.setVertexBuffer(0, gpuBuffer.slice());
                    renderPass.setIndexBuffer(this.gpuIndexBuffer, IndexType.SHORT);
                    this.bindPassResources(renderPass);

                    renderPass.drawIndexed(retainedQuadCount * 6, 1, 0, 0, 0);
                }

                this.afterDraw();
            }
        }
    }

    protected boolean usesDepthAttachment() {
        return false;
    }

    protected Render2DContext getRenderContext() {
        return this.context;
    }

    private void updateProjection() {
        int width = this.context.getTargetWidth();
        int height = this.context.getTargetHeight();
        if (width != this.lastProjectionWidth
                || height != this.lastProjectionHeight
                || this.projectionDirty) {
            this.projectionMatrix.setOrtho(0.0F, width, height, 0.0F, -1.0F, 1.0F);
            this.lastProjectionWidth = width;
            this.lastProjectionHeight = height;
            this.projectionDirty = false;
            this.projectionBytes.clear();
            this.projectionMatrix.get(this.projectionBytes);
            ByteBuffer bytes = this.projectionBytes.position(0);

            bytes.limit(64);
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(this.projectionUniform.slice(), this.projectionBytes);
        }
    }
}
