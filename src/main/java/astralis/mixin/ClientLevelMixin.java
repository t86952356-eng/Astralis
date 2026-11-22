package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.BarrierView;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.Blocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Inject(method = "getMarkerParticleTarget", at = @At("RETURN"), cancellable = true)
    private void forceBarrierMarkerParticleTarget(CallbackInfoReturnable cir) {
        BarrierView barrierView =
                Astralis.getInstance().getModuleManager().getModule(BarrierView.class);
        if (barrierView != null && barrierView.isEnabled() && cir.getReturnValue() == null) {
            cir.setReturnValue(Blocks.BARRIER);
        }
    }
}
