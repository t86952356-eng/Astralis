package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Xray;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
        targets =
                "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask",
        remap = false)
public class ChunkBuilderMeshingTaskMixin {
    @ModifyExpressionValue(
            method = "execute",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"),
            remap = false)
    private boolean treatBlocksAsTransparentForXray(boolean original) {
        Xray xray = Astralis.getInstance().getModuleManager().getModule(Xray.class);
        return xray != null && xray.isEnabled() ? false : original;
    }
}
