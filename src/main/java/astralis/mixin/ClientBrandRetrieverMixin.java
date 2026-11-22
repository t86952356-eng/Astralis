package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.ServerSpoofer;

import net.minecraft.client.ClientBrandRetriever;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ClientBrandRetrieverMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), remap = false, cancellable = true)
    private static void spoofClientBrand(CallbackInfoReturnable cir) {
        if (Astralis.getInstance().getModuleManager() != null) {
            ServerSpoofer serverSpoofer =
                    Astralis.getInstance().getModuleManager().getModule(ServerSpoofer.class);
            if (serverSpoofer != null
                    && serverSpoofer.isEnabled()
                    && serverSpoofer.getSpoofClientBrand().getValue()) {
                cir.setReturnValue(serverSpoofer.getBrand().getValue());
            }
        }
    }
}
