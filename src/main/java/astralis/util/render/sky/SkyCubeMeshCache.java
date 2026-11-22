package astralis.util.render.sky;

import astralis.util.render.SkyboxFace;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public final class SkyCubeMeshCache {
    private static final float HALF_SIZE = 100.0F;
    private static Map<String, GpuBuffer> buffersBySize;

    private SkyCubeMeshCache() {}

    public static GpuBuffer get(int width, int height) {
        return buffersBySize.computeIfAbsent(width + "x" + height, key -> build(width, height));
    }

    public static void clearCache() {
        Iterator iterator = buffersBySize.values().iterator();

        while (iterator.hasNext()) {
            GpuBuffer gpuBuffer = (GpuBuffer) iterator.next();
            gpuBuffer.close();
        }

        buffersBySize.clear();
    }

    private static GpuBuffer build(int width, int height) {

        float halfPixelU = width > 0 ? 0.5F / width : 0.0F;
        float halfPixelV = height > 0 ? 0.5F / height : 0.0F;

        try (ByteBufferBuilder byteBufferBuilder =
                ByteBufferBuilder.exactlySized(
                        24 * DefaultVertexFormat.POSITION_TEX.getVertexSize())) {
            BufferBuilder bufferBuilder =
                    new BufferBuilder(
                            byteBufferBuilder,
                            PrimitiveTopology.QUADS,
                            DefaultVertexFormat.POSITION_TEX);

            for (SkyboxFace face : SkyboxFace.values()) {
                float minU = face.atlasColumn * (width / 3.0F) / width + halfPixelU;
                float maxU = (face.atlasColumn + 1) * (width / 3.0F) / width - halfPixelU;
                float minV = face.atlasRow * (height / 2.0F) / height + halfPixelV;
                float maxV = (face.atlasRow + 1) * (height / 2.0F) / height - halfPixelV;
                bufferBuilder
                        .addVertex(face.faceTransform, -100.0F, -100.0F, -100.0F)
                        .setUv(minU, minV);
                bufferBuilder
                        .addVertex(face.faceTransform, -100.0F, -100.0F, 100.0F)
                        .setUv(minU, maxV);
                bufferBuilder
                        .addVertex(face.faceTransform, 100.0F, -100.0F, 100.0F)
                        .setUv(maxU, maxV);
                bufferBuilder
                        .addVertex(face.faceTransform, 100.0F, -100.0F, -100.0F)
                        .setUv(maxU, minV);
            }

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                GpuDevice gpuDevice = RenderSystem.getDevice();
                Supplier labelSupplier = () -> "Pack sky geometry";

                return gpuDevice.createBuffer(labelSupplier, 32, meshData.vertexBuffer());
            }
        }
    }

    static {
        buffersBySize = new HashMap<>();
    }
}
