package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void blockFreecamInventoryUpdate(int slot, ItemStack itemStack, CallbackInfo ci) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (this.player == localPlayer && slot >= 0 && slot < Inventory.getSelectionSize()) {
            Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
            if (freecam != null
                    && freecam.isEnabled()
                    && freecam.getItemController().interceptSetItem(slot, itemStack)) {
                ci.cancel();
            }
        }
    }
}
