package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.AntiClutter;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogEnvironmentMixin {
    @Shadow
    public abstract Holder getMobEffect();

    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    boolean suppressMobEffectFog(boolean applicable) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (!antiClutter.isEnabled()) {
            return applicable;
        } else {
            return (!antiClutter.blindness.getValue()
                                    || this.getMobEffect() != MobEffects.BLINDNESS)
                            && (!antiClutter.darkness.getValue()
                                    || this.getMobEffect() != MobEffects.DARKNESS)
                    ? applicable
                    : false;
        }
    }
}
