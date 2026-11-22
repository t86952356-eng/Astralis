package astralis.mixin;

import astralis.util.player.TimerSpeed;

import net.minecraft.client.DeltaTracker.Timer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Timer.class)
public class DeltaTrackerTimerMixin {
    @Shadow private float deltaTicks;

    @Inject(
            method = "advanceGameTime(J)I",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J",
                            opcode = 181))
    private void scaleDeltaTicks(long timeMs, CallbackInfoReturnable cir) {
        this.deltaTicks = this.deltaTicks * TimerSpeed.multiplier;
    }
}
