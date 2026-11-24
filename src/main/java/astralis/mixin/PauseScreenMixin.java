package astralis.mixin;

import astralis.ui.screen.FriendsScreen;
import astralis.ui.screen.PackBrowserScreen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addPauseMenuButtons(CallbackInfo callbackInfo) {
        if (((PauseScreen) (Object) this).showsPauseMenu()) {
            int bottomY = 0;
            Iterator iterator = ((ScreenAccessor) (Object) this).getRenderables().iterator();

            while (iterator.hasNext()) {
                Renderable renderable = (Renderable) iterator.next();
                int isWidget = renderable instanceof AbstractWidget ? 1 : 0;
                if (isWidget != 0) {
                    AbstractWidget abstractWidget = (AbstractWidget) renderable;
                    bottomY = Math.max(bottomY, abstractWidget.getY() + abstractWidget.getHeight());
                }
            }

            if (bottomY == 0) {
                bottomY = this.height / 2 + 10;
            }

            ((ScreenAccessor) (Object) this)
                    .invokeAddDrawableChild(
                            Button.builder(
                                            Component.literal("Social"),
                                            button ->
                                                    this.minecraft.gui.setScreen(
                                                            new FriendsScreen(this)))
                                    .size(this.minecraft.isLocalServer() ? 204 : 100, 20)
                                    .pos(this.width / 2 - 102, bottomY + 4)
                                    .build());
            if (!this.minecraft.isLocalServer()) {
                ((ScreenAccessor) (Object) this)
                        .invokeAddDrawableChild(
                                Button.builder(
                                                Component.literal("Server Selector"),
                                                button2 ->
                                                        this.minecraft.gui.setScreen(
                                                                new JoinMultiplayerScreen(this)))
                                        .size(100, 20)
                                        .pos(this.width / 2 + 2, bottomY + 4)
                                        .build());
            }

            ((ScreenAccessor) (Object) this)
                    .invokeAddDrawableChild(
                            Button.builder(
                                            Component.literal("Pack Browser"),
                                            button3 ->
                                                    this.minecraft.gui.setScreen(
                                                            new PackBrowserScreen(this)))
                                    .size(204, 20)
                                    .pos(this.width / 2 - 102, bottomY + 28)
                                    .build());
            // The in-game CEF browser (MCEF) was desktop-only and has been removed.
        }
    }
}
