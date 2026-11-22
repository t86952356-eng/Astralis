package astralis.mixin;

import astralis.rotation.RotationManager;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerboundUseItemPacket.class)
public class ServerboundUseItemPacketMixin {

    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true)
    private static float overrideUseItemYaw(float yaw) {
        return RotationManager.getInstance().getMovementYaw();
    }

    @ModifyVariable(
            method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true)
    private static float overrideUseItemPitch(float pitch) {
        return RotationManager.getInstance().getMovementPitch();
    }
}
