package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.ItemUseAnimationEvent;
import astralis.event.impl.RenderHeldItemEvent;
import astralis.module.impl.Animations;
import astralis.module.impl.HandView;
import astralis.rotation.RotationManager;
import astralis.util.render.SwordArmRenderEvent;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin implements InstanceAccess {
    @Unique private ItemUseAnimationEvent renderBlockStateEvent;
    @Shadow @Final private static float ITEM_POS_Y;
    @Shadow private ItemStack mainHandItem;

    @Unique
    private RotationManager getActiveRotationManager(LocalPlayer localPlayer) {
        RotationManager rotationManager = RotationManager.getInstance();
        return localPlayer == mc.player && rotationManager != null && rotationManager.isEnabled()
                ? rotationManager
                : null;
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;getXRot(F)F"))
    private float getSpoofedXRot(LocalPlayer localPlayer, float partialTick) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getPitch()
                : localPlayer.getXRot(partialTick);
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
    private float getSpoofedViewXRot(LocalPlayer localPlayer, float partialTick) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getPitch()
                : localPlayer.getViewXRot(partialTick);
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
    private float getSpoofedViewYRot(LocalPlayer localPlayer, float partialTick) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getYaw()
                : localPlayer.getViewYRot(partialTick);
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/player/LocalPlayer;xBobO:F",
                            opcode = 180))
    private float getSpoofedXBobOld(LocalPlayer localPlayer) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getPrevXBob()
                : localPlayer.xBobO;
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/player/LocalPlayer;xBob:F",
                            opcode = 180))
    private float getSpoofedXBob(LocalPlayer localPlayer) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getXBob()
                : localPlayer.xBob;
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/player/LocalPlayer;yBobO:F",
                            opcode = 180))
    private float getSpoofedYBobOld(LocalPlayer localPlayer) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getPrevYBob()
                : localPlayer.yBobO;
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "FIELD",
                            target = "Lnet/minecraft/client/player/LocalPlayer;yBob:F",
                            opcode = 180))
    private float getSpoofedYBob(LocalPlayer localPlayer) {
        RotationManager rotationManager = this.getActiveRotationManager(localPlayer);
        return rotationManager != null
                ? rotationManager.getPlayerViewSnapshot().getYBob()
                : localPlayer.yBob;
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack postMainHandRenderHeldItemEvent(LocalPlayer localPlayer) {
        ItemStack itemStack = localPlayer.getMainHandItem();
        RenderHeldItemEvent renderHeldItemEvent =
                new RenderHeldItemEvent(itemStack, InteractionHand.MAIN_HAND);
        Astralis.getInstance().getEventBus().post(renderHeldItemEvent);
        return renderHeldItemEvent.getStack();
    }

    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/LocalPlayer;getOffhandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack postOffhandRenderHeldItemEvent(LocalPlayer localPlayer) {
        ItemStack itemStack = localPlayer.getOffhandItem();
        RenderHeldItemEvent renderHeldItemEvent =
                new RenderHeldItemEvent(itemStack, InteractionHand.OFF_HAND);
        Astralis.getInstance().getEventBus().post(renderHeldItemEvent);
        return renderHeldItemEvent.getStack();
    }

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack postArmRenderHeldItemEvent(
            ItemStack itemStack, @Local(argsOnly = true) InteractionHand hand) {
        RenderHeldItemEvent renderHeldItemEvent = new RenderHeldItemEvent(itemStack, hand);
        Astralis.getInstance().getEventBus().post(renderHeldItemEvent);
        return renderHeldItemEvent.getStack();
    }

    @Redirect(
            method = "submitHandsWithItems",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/renderer/ItemInHandRenderer;mainHandItem:Lnet/minecraft/world/item/ItemStack;",
                            opcode = 180,
                            ordinal = 0))
    private ItemStack postMainHandFieldRenderHeldItemEvent(ItemInHandRenderer itemInHandRenderer) {
        ItemStack itemStack = this.mainHandItem;
        RenderHeldItemEvent renderHeldItemEvent =
                new RenderHeldItemEvent(itemStack, InteractionHand.MAIN_HAND);
        Astralis.getInstance().getEventBus().post(renderHeldItemEvent);
        return renderHeldItemEvent.getStack();
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void cancelShieldArmRender(
            AbstractClientPlayer abstractClientPlayer,
            float partialTick,
            float pitch,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci) {
        if (itemStack.getItem() instanceof ShieldItem && this.getAnimationsModule().isEnabled()) {
            ci.cancel();
        }
    }

    @ModifyArg(
            method = "applyItemArmTransform",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
            index = 1)
    private float useRestingItemPositionY(float original) {
        Animations animations = this.getAnimationsModule();
        return animations.getDisableItemEquipOffsetSetting().getValue() && animations.isEnabled()
                ? ITEM_POS_Y
                : original;
    }

    @ModifyExpressionValue(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
    private Item treatSwordAsShield(Item item2) {
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)
                        && this.getAnimationsModule().isEnabled()
                        && this.getAnimationsModule().getHideShieldWhenBlockingSetting().getValue()
                ? Items.SHIELD
                : item2;
    }

    @Redirect(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;",
                            ordinal = 0))
    private ItemUseAnimation forceBlockUseAnimation(ItemStack itemStack) {
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)
                        && (this.renderBlockStateEvent != null
                                        && this.renderBlockStateEvent.isUsing()
                                || Minecraft.getInstance().options.keyUse.isDown())
                        && this.getAnimationsModule().isEnabled()
                ? ItemUseAnimation.BLOCK
                : itemStack.getUseAnimation();
    }

    @Redirect(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z",
                            ordinal = 1))
    private boolean forceUsingItemWhileBlocking(AbstractClientPlayer abstractClientPlayer) {
        this.renderBlockStateEvent = new ItemUseAnimationEvent();
        Astralis.getInstance().getEventBus().post(this.renderBlockStateEvent);
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)
                        && this.getAnimationsModule().isEnabled()
                ? this.renderBlockStateEvent != null && this.renderBlockStateEvent.isUsing()
                        || Minecraft.getInstance().options.keyUse.isDown()
                : abstractClientPlayer.isUsingItem();
    }

    @Redirect(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;",
                            ordinal = 1))
    private InteractionHand forceMainHandWhileBlocking(AbstractClientPlayer abstractClientPlayer) {
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)
                        && (this.renderBlockStateEvent != null
                                        && this.renderBlockStateEvent.isUsing()
                                || Minecraft.getInstance().options.keyUse.isDown())
                        && this.getAnimationsModule().isEnabled()
                ? InteractionHand.MAIN_HAND
                : abstractClientPlayer.getUsedItemHand();
    }

    @Redirect(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I",
                            ordinal = 2))
    private int forceBlockUseRemainingTicks(AbstractClientPlayer abstractClientPlayer) {
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)
                        && (this.renderBlockStateEvent != null
                                        && this.renderBlockStateEvent.isUsing()
                                || Minecraft.getInstance().options.keyUse.isDown())
                        && this.getAnimationsModule().isEnabled()
                ? 7200
                : abstractClientPlayer.getUseItemRemainingTicks();
    }

    @ModifyArg(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                            ordinal = 4),
            index = 2)
    private float zeroEquipProgress(float original) {
        return this.getAnimationsModule().isEnabled()
                        && this.getAnimationsModule().getDisableItemEquipOffsetSetting().getValue()
                ? 0.0F
                : original;
    }

    @Inject(
            method = "submitArmWithItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;",
                            shift = Shift.AFTER))
    private void postSwordArmRenderEvent(
            AbstractClientPlayer abstractClientPlayer,
            float partialTick,
            float pitch,
            InteractionHand interactionHand,
            float swingProgress,
            ItemStack itemStack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci) {
        if (this.getAnimationsModule().isEnabled()
                && mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS)) {
            HumanoidArm humanoidArm =
                    interactionHand == InteractionHand.MAIN_HAND
                            ? abstractClientPlayer.getMainArm()
                            : abstractClientPlayer.getMainArm().getOpposite();
            SwordArmRenderEvent event =
                    new SwordArmRenderEvent(poseStack, swingProgress, humanoidArm, equipProgress);
            Astralis.getInstance().getEventBus().post(event);
        }
    }

    @Unique
    private Animations getAnimationsModule() {
        return Astralis.getInstance().getModuleManager().getModule(Animations.class);
    }

    @Inject(method = "applyItemArmTransform", at = @At("TAIL"))
    private void applyHandViewTransform(
            PoseStack poseStack, HumanoidArm humanoidArm, float inverseArmHeight, CallbackInfo ci) {
        HandView handView = Astralis.getInstance().getModuleManager().getModule(HandView.class);
        if (handView != null && handView.isEnabled()) {
            ItemUseAnimation itemUseAnimation = mc.player.getUseItem().getUseAnimation();
            if (mc.player.isUsingItem()
                    && (itemUseAnimation == ItemUseAnimation.EAT
                            || itemUseAnimation == ItemUseAnimation.DRINK)
                    && (mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
                                    ? mc.player.getMainArm()
                                    : mc.player.getMainArm().getOpposite())
                            == humanoidArm) {
                poseStack.translate(
                        handView.getEatingXPosition().getValue(),
                        handView.getEatingYPosition().getValue(),
                        handView.getEatingZPosition().getValue());
            } else {
                poseStack.translate(
                        handView.getXPosition().getValue(),
                        handView.getYPosition().getValue(),
                        handView.getZPosition().getValue());
            }

            poseStack.mulPose(Axis.XP.rotationDegrees(handView.getXRotation().getValue()));
            poseStack.mulPose(Axis.YP.rotationDegrees(handView.getYRotation().getValue()));
            poseStack.mulPose(Axis.ZP.rotationDegrees(handView.getZRotation().getValue()));
        }
    }
}
