package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.Freecam;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmedEntityRenderState.class)
public class ArmedEntityRenderStateMixin {
    @Redirect(
            method = "extractArmedEntityRenderState",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;getItemHeldByArm(Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack overrideHeldItemForFreecam(
            LivingEntity livingEntity, HumanoidArm humanoidArm) {
        ItemStack itemStack = livingEntity.getItemHeldByArm(humanoidArm);
        if (livingEntity == Minecraft.getInstance().player
                && humanoidArm == livingEntity.getMainArm()) {
            Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
            if (freecam != null && freecam.isEnabled()) {
                ItemStack itemStack2 =
                        freecam.getItemController()
                                .getSavedSelectedItem(Minecraft.getInstance().player);
                return itemStack2 != null ? itemStack2 : itemStack;
            } else {
                return itemStack;
            }
        } else {
            return itemStack;
        }
    }
}
