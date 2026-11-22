package astralis.util.render.font;

import com.mojang.blaze3d.buffers.GpuBuffer;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public final class RetainedFontRun {
    public final MsdfFont font;
    public FloatBuffer vertexData;
    public GpuBuffer vertexBuffer;
    public int quadCount;
    private static long initialCapacity = 4840641469322626048L;

    public RetainedFontRun(MsdfFont font) {
        super();
        this.vertexData = MemoryUtil.memAllocFloat((int) initialCapacity);
        this.font = font;
    }

    public void ensureCapacity(int needed) {
        if (this.vertexData.remaining() < needed) {
            int requiredCapacity = this.vertexData.position() + needed;
            FloatBuffer newData =
                    MemoryUtil.memAllocFloat(
                            Math.max(this.vertexData.capacity() * 2, requiredCapacity));
            this.vertexData.flip();
            newData.put(this.vertexData);
            MemoryUtil.memFree(this.vertexData);
            this.vertexData = newData;
        }
    }

    public void release() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
        }

        if (this.vertexData != null) {
            MemoryUtil.memFree(this.vertexData);
        }
    }
}
