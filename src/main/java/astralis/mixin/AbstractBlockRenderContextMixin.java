package astralis.mixin;

import astralis.module.impl.Xray;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext",
        remap = false)
public abstract class AbstractBlockRenderContextMixin {
    @Shadow protected BlockState state;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, remap = false)
    private void forceXrayFaceDraw(Direction direction, CallbackInfoReturnable cir) {
        Xray xray = Xray.getInstance();
        if (xray != null && xray.isTranslucentActive() && xray.isSelectedBlock(this.state)) {
            cir.setReturnValue(true);
        }
    }
}
