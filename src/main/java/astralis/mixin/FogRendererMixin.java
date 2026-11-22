package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.FogEvent;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void applyFogEventOverrides(
            Camera camera,
            int renderDistance,
            DeltaTracker deltaTracker,
            float darkenAmount,
            ClientLevel clientLevel,
            CallbackInfoReturnable cir) {
        FogEvent event = new FogEvent();
        Astralis.getInstance().getEventBus().post(event);
        FogData fogData = (FogData) cir.getReturnValue();
        Color color2 = event.getColor();
        if (color2 != null) {
            fogData.color.set(
                    color2.getRed() / 255.0F,
                    color2.getGreen() / 255.0F,
                    color2.getBlue() / 255.0F,
                    1.0F);
            if (event.getSkyEndReduction() != null) {
                fogData.skyEnd = 512.0F - event.getSkyEndReduction();
            }
        }
    }
}
