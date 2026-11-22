package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.misc.HypixelStats;
import astralis.util.BedwarsStats;
import astralis.util.HypixelStatsApi;
import astralis.util.PlayerListManager;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void decoratePlayerListName(
            PlayerInfo playerInfo, CallbackInfoReturnable callbackInfoReturnable) {
        HypixelStats hypixelStats =
                Astralis.getInstance().getModuleManager().getModule(HypixelStats.class);
        MutableComponent mutableComponent = Component.literal("");
        if (Astralis.getInstance()
                .getManagerRegistry()
                .getManager(PlayerListManager.class)
                .isHacker(playerInfo.getProfile().id())) {
            mutableComponent.append(Component.literal("⚠ ").withStyle(ChatFormatting.GOLD));
        }

        if (hypixelStats.isEnabled()) {
            String playerName = playerInfo.getProfile().name();
            String statsDisplay = hypixelStats.getStatsDisplay(playerName);
            String playerTag = hypixelStats.getPlayerTag(playerName);
            BedwarsStats bedwarsStats = hypixelStats.getStatsCache().get(playerName);
            if (playerTag != null) {
                mutableComponent.append(Component.literal(playerTag + " "));
            }

            if (statsDisplay != null && bedwarsStats != null) {
                mutableComponent
                        .append(
                                Component.literal(statsDisplay)
                                        .withStyle(
                                                style ->
                                                        style.withColor(
                                                                HypixelStatsApi.getPrestigeColor(
                                                                        bedwarsStats.getStars()))))
                        .append(Component.literal(" "));
            }
        }

        mutableComponent.append((Component) callbackInfoReturnable.getReturnValue());
        callbackInfoReturnable.setReturnValue(
                Astralis.getInstance()
                        .getProfileManager()
                        .decoratePlayerName(playerInfo.getProfile().id(), mutableComponent));
    }
}
