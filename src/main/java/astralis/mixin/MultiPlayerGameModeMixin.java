package astralis.mixin;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.BlockDestroyedEvent;
import astralis.module.impl.Freecam;
import astralis.module.impl.combat.KillAura;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin implements InstanceAccess {
    @Unique private BlockState astralis$destroyedBlockState;

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void handleFreecamUseItem(
            Player player2,
            InteractionHand interactionHand,
            CallbackInfoReturnable callbackInfoReturnable) {
        if (this.handleFreecamUse(player2, interactionHand)) {
            callbackInfoReturnable.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void handleFreecamUseItemOn(
            LocalPlayer localPlayer,
            InteractionHand interactionHand,
            BlockHitResult blockHitResult,
            CallbackInfoReturnable callbackInfoReturnable) {
        if (this.handleFreecamUse(localPlayer, interactionHand)) {
            callbackInfoReturnable.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void handleFreecamInteract(
            Player player2,
            Entity entity2,
            EntityHitResult entityHitResult,
            InteractionHand interactionHand,
            CallbackInfoReturnable callbackInfoReturnable) {
        if (this.handleFreecamUse(player2, interactionHand)) {
            callbackInfoReturnable.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    private boolean handleFreecamUse(Player player2, InteractionHand interactionHand) {
        Freecam freecam = Astralis.getInstance().getModuleManager().getModule(Freecam.class);
        return player2 instanceof LocalPlayer localPlayer
                && freecam != null
                && freecam.isEnabled()
                && freecam.getItemController()
                        .handleUse(localPlayer, player2.getItemInHand(interactionHand));
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void cancelStartDestroyThroughWalls(
            BlockPos blockPos, Direction direction, CallbackInfoReturnable callbackInfoReturnable) {
        if (Astralis.getInstance()
                .getModuleManager()
                .getModule(KillAura.class)
                .isThroughWallsActive()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void cancelContinueDestroyThroughWalls(
            BlockPos blockPos, Direction direction, CallbackInfoReturnable callbackInfoReturnable) {
        if (Astralis.getInstance()
                .getModuleManager()
                .getModule(KillAura.class)
                .isThroughWallsActive()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "stopDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void cancelStopDestroyThroughWalls(CallbackInfo callbackInfo) {
        if (Astralis.getInstance()
                .getModuleManager()
                .getModule(KillAura.class)
                .isThroughWallsActive()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void captureDestroyedBlockState(
            BlockPos blockPos, CallbackInfoReturnable callbackInfoReturnable) {
        this.astralis$destroyedBlockState = mc.level.getBlockState(blockPos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void postBlockDestroyedEvent(
            BlockPos blockPos, CallbackInfoReturnable callbackInfoReturnable) {
        BlockState blockState = this.astralis$destroyedBlockState;
        this.astralis$destroyedBlockState = null;
        if (callbackInfoReturnable.getReturnValueZ() && blockState != null && !blockState.isAir()) {
            Astralis.getInstance()
                    .getEventBus()
                    .post(new BlockDestroyedEvent(blockPos, blockState));
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void postPreAttackEvent(Player player2, Entity entity2, CallbackInfo callbackInfo) {
        AttackEvent attackEvent = new AttackEvent(entity2, EventStage.PRE);
        Astralis.getInstance().getEventBus().post(attackEvent);
        if (attackEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "attack", at = @At("TAIL"), cancellable = true)
    private void postPostAttackEvent(Player player2, Entity entity2, CallbackInfo callbackInfo) {
        AttackEvent attackEvent = new AttackEvent(entity2, EventStage.POST);
        Astralis.getInstance().getEventBus().post(attackEvent);
        if (attackEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
