package astralis.module.impl.freecam;

import astralis.module.impl.Freecam;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class FreecamItemController {
    private final Freecam freecam;
    private final FreecamHotbar hotbar = new FreecamHotbar();
    private boolean useConsumed;

    public FreecamItemController(Freecam freecam) {
        this.freecam = freecam;
    }

    public boolean interceptSetItem(int slot, ItemStack itemStack) {
        return this.hotbar.saveItem(slot, itemStack);
    }

    public boolean handleUse(LocalPlayer localPlayer, ItemStack itemStack) {
        if (!this.isFreecamItem(itemStack)) {
            return false;
        }

        if (!this.useConsumed) {
            FreecamSpeedMenu.getSpeedFromItem(itemStack)
                    .ifPresentOrElse(
                            speed -> {
                                this.freecam
                                        .getSpeed()
                                        .setValueRaw(
                                                Math.clamp(
                                                        speed,
                                                        this.freecam.getSpeed().getMin(),
                                                        this.freecam.getSpeed().getMax()));
                                this.hotbar.showMainMenu(localPlayer.getInventory());
                            },
                            () -> {
                                if (FreecamSpeedMenu.isExitItem(itemStack)) {
                                    this.hotbar.showMainMenu(localPlayer.getInventory());
                                } else {
                                    FreecamItem.fromStack(itemStack).ifPresent(this::activateItem);
                                }
                            });
            this.useConsumed = true;
        }

        return true;
    }

    public boolean isHoldingPlayerController(LocalPlayer localPlayer) {
        return FreecamItem.fromStack(localPlayer.getMainHandItem())
                .filter(freecamItem -> freecamItem == FreecamItem.PLAYER_CONTROLLER)
                .isPresent();
    }

    private boolean isFreecamItem(ItemStack itemStack) {
        return FreecamItem.fromStack(itemStack).isPresent()
                || FreecamSpeedMenu.getSpeedFromItem(itemStack).isPresent()
                || FreecamSpeedMenu.isExitItem(itemStack);
    }

    public boolean isControllingPlayer(LocalPlayer localPlayer, boolean freecamActive) {
        return freecamActive && this.isHoldingPlayerController(localPlayer);
    }

    public void endSession(LocalPlayer localPlayer) {
        this.hotbar.restoreHotbar(localPlayer.getInventory());
        this.useConsumed = false;
    }

    public ItemStack getSavedSelectedItem(LocalPlayer localPlayer) {
        return this.hotbar.getSavedSelectedItem(localPlayer.getInventory());
    }

    private void activateItem(FreecamItem freecamItem) {
        switch (FreecamItemSwitchMap.switchMapFreecamItem[freecamItem.ordinal()]) {
            case 1:
                FreecamTeleportUtil.teleportToCrosshair(this.freecam);
                break;
            case 2:
                this.hotbar.showSpeedMenu(
                        InstanceAccess.mc.player.getInventory(),
                        this.freecam.getSpeed().getValue());
            case 3:
        }
    }

    public void startSession(LocalPlayer localPlayer, boolean replaceHotbar) {
        if (replaceHotbar) {
            this.hotbar.replaceHotbar(localPlayer.getInventory());
        }

        this.useConsumed = false;
    }

    public void updateUseKey(boolean useKeyDown) {
        if (!useKeyDown) {
            this.useConsumed = false;
        }
    }
}
