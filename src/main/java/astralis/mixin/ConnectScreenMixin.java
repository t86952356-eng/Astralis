package astralis.mixin;

import astralis.util.ServerUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void rememberLastServer(
            Screen screen,
            Minecraft minecraft2,
            ServerAddress serverAddress,
            ServerData serverData,
            boolean isQuickPlay,
            TransferState transferState,
            CallbackInfo ci) {
        ServerUtil.storeLastServer(serverData, serverAddress);
    }
}
