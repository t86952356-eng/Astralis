package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.AntiClutter;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void cancelBossBarRender(GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.bossBar.getValue()) {
            ci.cancel();
        }
    }
}
