package astralis.mixin;

import astralis.resourcepack.PackAutoConverter;
import astralis.ui.screen.PackBrowserScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
public class PackSelectionScreenMixin {
    @Shadow private @Nullable EditBox search;

    @Inject(method = "init", at = @At("TAIL"))
    private void addBrowseAndConverterButtons(CallbackInfo callbackInfo) {
        if (this.search != null) {
            PackSelectionScreen packSelectionScreen = (PackSelectionScreen) (Object) this;
            Button button =
                    Button.builder(
                                    Component.literal("Browse Online"),
                                    button2 ->
                                            Minecraft.getInstance()
                                                    .gui
                                                    .setScreen(
                                                            new PackBrowserScreen(
                                                                    packSelectionScreen)))
                            .bounds(
                                    this.search.getX() + this.search.getWidth() + 6,
                                    this.search.getY() - 2,
                                    100,
                                    20)
                            .build();
            ((ScreenAccessor) (Object) this).invokeAddDrawableChild(button);
            Button button3 =
                    Button.builder(
                                    getConverterButtonLabel(),
                                    button4 -> {
                                        boolean converterEnabled =
                                                !PackAutoConverter.isConverterEnabled();
                                        PackAutoConverter.setConverterEnabled(converterEnabled);
                                        button4.setMessage(getConverterButtonLabel());
                                        if (converterEnabled) {
                                            PackAutoConverter.prewarmRepository(
                                                    Minecraft.getInstance()
                                                            .getResourcePackRepository());
                                        }
                                    })
                            .bounds(
                                    Math.min(
                                            packSelectionScreen.width - 114, button.getRight() + 4),
                                    button.getY(),
                                    110,
                                    20)
                            .build();
            button3.setMessage(getConverterButtonLabel());
            ((ScreenAccessor) (Object) this).invokeAddDrawableChild(button3);
            if (PackAutoConverter.isConverterEnabled()) {
                PackAutoConverter.prewarmRepository(
                        Minecraft.getInstance().getResourcePackRepository());
            }
        }
    }

    private static Component getConverterButtonLabel() {
        return Component.literal(
                PackAutoConverter.isConverterEnabled() ? "Converter: On" : "Converter: Off");
    }
}
