package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Xray;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererImplMixin {
    @Shadow private BlockState blockState;

    @Inject(method = "transform", at = @At("HEAD"))
    private void applyXrayQuadOpacity(MutableQuadView quad, CallbackInfoReturnable cir) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null
                && !xray.isTranslucencyInactive()
                && !xray.isSelectedBlock(this.blockState)) {
            int alpha = xray.getOpacityAlpha();
            int i = 0;

            while (i < 4) {
                quad.color(i, ARGB.color(alpha, quad.color(i)));
                i++;
            }

            quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
        }
    }
}
