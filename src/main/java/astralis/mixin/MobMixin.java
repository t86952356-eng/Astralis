package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.EntityControl;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {
    @Inject(method = "isSaddled", at = @At("HEAD"), cancellable = true)
    private void forceSaddledForEntityControl(CallbackInfoReturnable callbackInfo) {
        if ((Object) this instanceof AbstractHorse) {
            EntityControl entityControl =
                    Astralis.getInstance().getModuleManager().getModule(EntityControl.class);
            if (entityControl.isEnabled()) {
                callbackInfo.setReturnValue(true);
            }
        }
    }
}
