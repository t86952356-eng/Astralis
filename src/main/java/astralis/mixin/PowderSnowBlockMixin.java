package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.AntiHazard;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
    private static boolean allowWalkingOnPowderSnow(boolean canWalk, Entity entity2) {
        AntiHazard antiHazard =
                Astralis.getInstance().getModuleManager().getModule(AntiHazard.class);
        return antiHazard != null
                        && antiHazard.isEnabled()
                        && antiHazard.getAntiPowderSnowSetting().getValue()
                ? true
                : canWalk;
    }
}
