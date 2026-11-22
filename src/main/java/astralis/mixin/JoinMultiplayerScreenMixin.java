package astralis.mixin;

import astralis.ui.screen.AltManagerScreen;
import astralis.ui.screen.MiniBloxScreen;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Unique private Button altManagerButton;
    @Unique private Button miniBloxButton;

    protected JoinMultiplayerScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "repositionElements", at = @At("RETURN"))
    private void addAltManagerButtons(CallbackInfo ci) {
        if (this.altManagerButton == null) {
            this.altManagerButton =
                    Button.builder(
                                    Component.literal("Alt Manager"),
                                    button ->
                                            InstanceAccess.mc.gui.setScreen(
                                                    new AltManagerScreen(
                                                            InstanceAccess.mc.gui.screen())))
                            .size(100, 20)
                            .build();
            this.addRenderableWidget(this.altManagerButton);
        }

        if (this.miniBloxButton == null) {
            this.miniBloxButton =
                    Button.builder(
                                    Component.literal("MiniBlox"),
                                    button2 ->
                                            InstanceAccess.mc.gui.setScreen(
                                                    new MiniBloxScreen(this)))
                            .size(110, 20)
                            .build();
            this.addRenderableWidget(this.miniBloxButton);
        }

        this.altManagerButton.setPosition(this.width - 105, this.height - 35);
        this.miniBloxButton.setPosition(5, 5);
    }
}
