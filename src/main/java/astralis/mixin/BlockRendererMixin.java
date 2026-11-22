package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Xray;
import astralis.util.OwnBedTracker;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class BlockRendererMixin {
    @Unique private BlockState astralis$xrayState;

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false)
    private void hideXrayBlockModel(
            BlockStateModel blockStateModel,
            BlockState blockState,
            BlockPos blockPos,
            BlockPos blockPos2,
            CallbackInfo ci) {
        if (blockState.getBlock() instanceof BedBlock) {
            OwnBedTracker.add(blockPos.immutable());
        }

        this.astralis$xrayState = blockState;
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null && xray.shouldHideBlock(blockState, blockPos)) {
            ci.cancel();
        }
    }

    @Inject(method = "processQuad", at = @At("HEAD"), remap = false)
    private void applyXrayQuadOpacity(MutableQuadViewImpl quad, CallbackInfo ci) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        if (xray != null
                && !xray.isTranslucencyInactive()
                && !xray.isSelectedBlock(this.astralis$xrayState)) {
            int alpha = xray.getOpacityAlpha();
            int i = 0;

            while (i < 4) {
                quad.setColor(i, ARGB.color(alpha, quad.getColor(i)));
                i++;
            }

            quad.setRenderType(ChunkSectionLayer.TRANSLUCENT);
        }
    }
}
