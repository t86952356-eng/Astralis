package astralis.mixin;

import astralis.Astralis;
import astralis.module.ModuleManager;
import astralis.module.impl.ChestStealer;
import astralis.module.impl.Theme;
import astralis.ui.CustomButtonRenderer;
import astralis.ui.CustomSliderRenderer;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin implements InstanceAccess {

    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At("HEAD"),
            cancellable = true)
    private void cancelChestScreenRender(
            GuiGraphicsExtractor guiGraphicsExtractor,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (moduleManager != null) {
            if ((Object) this instanceof AbstractContainerScreen
                    && !((Object) this instanceof InventoryScreen)) {
                ChestStealer chestStealer = moduleManager.getModule(ChestStealer.class);
                if (chestStealer != null
                        && chestStealer.isEnabled()
                        && chestStealer.getSilentSetting().getValue()) {
                    callbackInfo.cancel();
                }
            }
        }
    }

    @Redirect(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/screens/Screen;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void redirectScreenBackground(
            Screen screen,
            GuiGraphicsExtractor guiGraphicsExtractor,
            int mouseX,
            int mouseY,
            float partialTick) {
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        if (moduleManager == null
                || moduleManager.isEmpty()
                || mc.level != null && !(screen instanceof LevelLoadingScreen)
                || !moduleManager.getModule(Theme.class).getCustomMainMenu().getValue()) {
            screen.extractBackground(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        }
    }

    @Inject(method = "extractBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void cancelBlurredBackground(
            GuiGraphicsExtractor guiGraphicsExtractor, CallbackInfo callbackInfo) {
        if (CustomButtonRenderer.isCustomButtonsEnabled()
                || CustomSliderRenderer.isCustomSlidersEnabled()) {
            callbackInfo.cancel();
        }
    }
}
