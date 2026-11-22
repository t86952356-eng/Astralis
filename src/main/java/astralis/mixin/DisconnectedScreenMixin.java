package astralis.mixin;

import astralis.ui.screen.AltManagerScreen;
import astralis.util.ServerUtil;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow @Final private Screen parent;
    @Unique private Button reconnectButton;
    @Unique private Button altManagerButton;

    protected DisconnectedScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addButtonsOnInit(CallbackInfo ci) {
        this.layoutReconnectAndAltManagerButtons();
    }

    @Inject(method = "repositionElements", at = @At("RETURN"))
    private void addButtonsOnReposition(CallbackInfo ci) {
        this.layoutReconnectAndAltManagerButtons();
    }

    @Unique
    private void layoutReconnectAndAltManagerButtons() {
        int bottomY = 0;
        Iterator iterator = ((ScreenAccessor) (Object) this).getRenderables().iterator();

        while (iterator.hasNext()) {
            Renderable renderable = (Renderable) iterator.next();
            int isWidget = renderable instanceof AbstractWidget ? 1 : 0;
            if (isWidget != 0) {
                AbstractWidget abstractWidget = (AbstractWidget) renderable;
                if (abstractWidget != this.reconnectButton
                        && abstractWidget != this.altManagerButton) {
                    bottomY = Math.max(bottomY, abstractWidget.getY() + abstractWidget.getHeight());
                }
            }
        }

        if (bottomY == 0) {
            bottomY = this.height / 2 + 10;
        }

        if (ServerUtil.canReconnect()) {
            if (this.reconnectButton == null) {
                this.reconnectButton =
                        Button.builder(
                                        Component.literal("Reconnect"),
                                        button -> ServerUtil.reconnect(this.parent))
                                .size(200, 20)
                                .build();
                ((ScreenAccessor) (Object) this).invokeAddDrawableChild(this.reconnectButton);
            }

            this.reconnectButton.setPosition(this.width / 2 - 100, bottomY + 4);
            bottomY += 24;
        }

        if (this.altManagerButton == null) {
            this.altManagerButton =
                    Button.builder(
                                    Component.literal("Alt Manager"),
                                    button2 ->
                                            this.minecraft.gui.setScreen(
                                                    new AltManagerScreen(this)))
                            .size(200, 20)
                            .build();
            ((ScreenAccessor) (Object) this).invokeAddDrawableChild(this.altManagerButton);
        }

        this.altManagerButton.setPosition(this.width / 2 - 100, bottomY + 4);
    }
}
