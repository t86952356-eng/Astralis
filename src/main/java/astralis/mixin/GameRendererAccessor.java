package astralis.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.renderer.GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor
    CrossFrameResourcePool getResourcePool();
}
