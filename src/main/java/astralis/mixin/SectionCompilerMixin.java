package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Xray;
import astralis.util.OwnBedTracker;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    @ModifyExpressionValue(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean treatHiddenXrayBlockAsAir(
            boolean isAir, @Local BlockState blockState, @Local(name = "pos") BlockPos pos) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        return isAir || xray != null && xray.shouldHideBlock(blockState, pos);
    }

    @ModifyExpressionValue(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"))
    private boolean disableSolidRenderForXray(boolean solidRender) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        return xray != null && xray.isEnabled() ? false : solidRender;
    }

    @ModifyExpressionValue(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;forceOpaque(ZLnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean keepForceOpaqueUnlessXray(boolean forceOpaque) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        return forceOpaque && (xray == null || xray.isTranslucencyInactive());
    }

    @WrapOperation(
            method = "compile",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void trackBedBlocksOnTesselate(
            ModelBlockRenderer modelBlockRenderer,
            BlockQuadOutput blockQuadOutput,
            float x,
            float y,
            float z,
            BlockAndTintGetter blockAndTintGetter,
            BlockPos blockPos,
            BlockState blockState,
            BlockStateModel blockStateModel,
            long seed,
            Operation original) {
        if (blockState.getBlock() instanceof BedBlock) {
            OwnBedTracker.add(blockPos.immutable());
        }

        original.call(
                modelBlockRenderer,
                blockQuadOutput,
                x,
                y,
                z,
                blockAndTintGetter,
                blockPos,
                blockState,
                blockStateModel,
                seed);
    }
}
