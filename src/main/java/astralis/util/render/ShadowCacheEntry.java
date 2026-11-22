package astralis.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

public final class ShadowCacheEntry {
    public final RenderTarget maskTarget;
    public final RenderTarget shadowTarget;
    public long lastUsed;
    public long contentHash;
    public int width;
    public int height;
    public int blurSteps;
    public int scaleBits;
    public int guiScaleBits;
    public boolean valid;

    public ShadowCacheEntry(int index) {

        this.maskTarget =
                new TextureTarget(
                        "Sph Shadow Mask Cache " + index, 1, 1, false, GpuFormat.RGBA8_UNORM);

        this.shadowTarget =
                new TextureTarget("Sph Shadow Cache " + index, 1, 1, false, GpuFormat.RGBA8_UNORM);
    }
}
