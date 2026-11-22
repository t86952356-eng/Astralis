package astralis.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor
    RenderTarget getEntityOutlineTarget();
}
