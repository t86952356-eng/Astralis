package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.LightmapExtractEvent;
import astralis.module.impl.AntiClutter;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
    @ModifyReturnValue(method = "calculateDarknessScale", at = @At("RETURN"))
    private float disableDarknessScale(float original) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        return antiClutter.isEnabled() && antiClutter.darkness.getValue() ? 0.0F : original;
    }

    @Inject(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                            shift = Shift.BEFORE))
    private void postLightmapExtractEvent(
            LightmapRenderState lightmapRenderState, float partialTick, CallbackInfo ci) {
        Astralis.getInstance().getEventBus().post(new LightmapExtractEvent(lightmapRenderState));
    }
}
