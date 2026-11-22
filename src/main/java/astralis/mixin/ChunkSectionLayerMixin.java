package astralis.mixin;

import astralis.module.impl.Xray;
import astralis.util.render.XrayPipelines;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionLayer.class)
public class ChunkSectionLayerMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void registerXrayPipeline(CallbackInfo ci) {
        XrayPipelines.translucentTerrainPipeline.getLocation();
    }

    @ModifyReturnValue(method = "pipeline", at = @At("RETURN"))
    private RenderPipeline replaceTranslucentPipelineForXray(RenderPipeline renderPipeline) {
        Xray xray = Xray.getInstance();
        return (Object) this == ChunkSectionLayer.TRANSLUCENT && xray != null && xray.isEnabled()
                ? XrayPipelines.translucentTerrainPipeline
                : renderPipeline;
    }
}
