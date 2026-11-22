package astralis.mixin;

import com.mojang.blaze3d.textures.GpuSampler;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.texture.AbstractTexture.class)
public interface AbstractTextureAccessor {
    @Accessor
    void setSampler(GpuSampler gpuSampler);
}
