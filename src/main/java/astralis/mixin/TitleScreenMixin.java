package astralis.mixin;

import astralis.Astralis;
import astralis.module.ModuleManager;
import astralis.module.impl.Theme;
import astralis.ui.component.SapphireScreen;
import astralis.ui.hud.TitleLogoRenderer;
import astralis.ui.screen.AltManagerScreen;
import astralis.ui.screen.BrowserScreen;
import astralis.ui.screen.FriendsScreen;
import astralis.ui.screen.PackBrowserScreen;
import astralis.util.render.Render2DContext;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen implements SapphireScreen {
    @Shadow private boolean fading;
    @Shadow private long fadeInStart;

    protected TitleScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addMenuButtons(CallbackInfo callbackInfo) {
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Browser"),
                                button -> this.minecraft.gui.setScreen(new BrowserScreen(this)))
                        .bounds(this.width - 105, this.height - 107, 100, 20)
                        .build());
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Social"),
                                button2 -> this.minecraft.gui.setScreen(new FriendsScreen(this)))
                        .bounds(this.width - 105, this.height - 83, 100, 20)
                        .build());
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Pack Browser"),
                                button3 ->
                                        this.minecraft.gui.setScreen(new PackBrowserScreen(this)))
                        .bounds(this.width - 105, this.height - 59, 100, 20)
                        .build());
        this.addRenderableWidget(
                Button.builder(
                                Component.literal("Alt Manager"),
                                button4 -> this.minecraft.gui.setScreen(new AltManagerScreen(this)))
                        .bounds(this.width - 105, this.height - 35, 100, 20)
                        .build());
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void renderThemedTitleScreen(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (moduleManager != null
                && !moduleManager.isEmpty()
                && moduleManager.getModule(Theme.class).getCustomMainMenu().getValue()) {
            if (this.fadeInStart == 0L && this.fading) {
                this.fadeInStart = Util.getMillis();
            }

            float alpha = 1.0F;
            if (this.fading) {
                float fadeProgress = (float) (Util.getMillis() - this.fadeInStart) / 2000.0F;
                if (fadeProgress > 1.0F) {
                    this.fading = false;
                } else {
                    fadeProgress = Mth.clamp(fadeProgress, 0.0F, 1.0F);
                    alpha = Mth.clampedMap(fadeProgress, 0.5F, 1.0F, 0.0F, 1.0F);
                }

                this.fadeWidgets(alpha);
            }

            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            callbackInfo.cancel();
        }
    }

    @Override
    public void renderSapphire(Render2DContext renderContext) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (moduleManager != null
                && !moduleManager.isEmpty()
                && moduleManager.getModule(Theme.class).getCustomMainMenu().getValue()) {
            TitleLogoRenderer.renderLogo(renderContext, this.width, this.height);
        }
    }
}
