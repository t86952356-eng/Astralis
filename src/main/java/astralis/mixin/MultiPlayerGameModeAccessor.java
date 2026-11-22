package astralis.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Invoker
    void callStartPrediction(ClientLevel clientLevel, PredictiveAction predictiveAction);

    @Invoker("ensureHasSentCarriedItem")
    void syncSlot();

    @Accessor("destroyProgress")
    float getCurrentBreakingProgress();

    @Accessor("destroyProgress")
    void setCurrentBreakingProgress(float progress);

    @Accessor("destroyDelay")
    void setBreakingCooldown(int cooldown);
}
