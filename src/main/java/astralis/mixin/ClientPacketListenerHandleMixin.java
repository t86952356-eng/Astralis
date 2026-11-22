package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.GameJoinEvent;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.AntiClutter;
import astralis.module.impl.AntiExploit;
import astralis.module.impl.AutoHypixel;
import astralis.util.LocalProxy;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen.Reason;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerHandleMixin implements InstanceAccess {
    @Shadow private LevelLoadTracker levelLoadTracker;

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void validateParticlePacket(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (AntiExploit.isIllegalParticlesProtectionEnabled()) {
            if (!isParticlePacketValid(packet)) {
                AntiExploit.reportBlockedExploit("illegal particle packet");
                ci.cancel();
            }
        }
    }

    private static boolean isParticlePacketValid(
            ClientboundLevelParticlesPacket clientboundLevelParticlesPacket) {
        return AntiExploit.isValidCoordinate(clientboundLevelParticlesPacket.getX())
                && AntiExploit.isValidCoordinate(clientboundLevelParticlesPacket.getY())
                && AntiExploit.isValidCoordinate(clientboundLevelParticlesPacket.getZ())
                && AntiExploit.isValidFloatCoordinate(clientboundLevelParticlesPacket.getXDist())
                && AntiExploit.isValidFloatCoordinate(clientboundLevelParticlesPacket.getYDist())
                && AntiExploit.isValidFloatCoordinate(clientboundLevelParticlesPacket.getZDist())
                && AntiExploit.isValidFloatCoordinate(clientboundLevelParticlesPacket.getMaxSpeed())
                && clientboundLevelParticlesPacket.getCount() >= 0
                && clientboundLevelParticlesPacket.getCount() <= 16384;
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void validatePlayerPositionPacket(
            ClientboundPlayerPositionPacket clientboundPlayerPositionPacket, CallbackInfo ci) {
        if (AntiExploit.isInvalidPositionProtectionEnabled()
                && (!AntiExploit.isValidPosition(
                                clientboundPlayerPositionPacket.change().position())
                        || !AntiExploit.isValidPosition(
                                clientboundPlayerPositionPacket.change().deltaMovement())
                        || !AntiExploit.isValidFloatCoordinate(
                                clientboundPlayerPositionPacket.change().yRot())
                        || !AntiExploit.isValidFloatCoordinate(
                                clientboundPlayerPositionPacket.change().xRot()))) {
            AntiExploit.reportBlockedExploit("invalid player position packet");
            ci.cancel();
        }
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void postGameJoinEvent(CallbackInfo ci) {
        Astralis.getInstance().getEventBus().post(new GameJoinEvent(System.currentTimeMillis()));
    }

    @Inject(method = "handleBlockEvent", at = @At("HEAD"), cancellable = true)
    private void ignoreBlockEventWithoutLevel(
            ClientboundBlockEventPacket clientboundBlockEventPacket, CallbackInfo ci) {
        if (mc.level == null) {
            ci.cancel();
        }
    }

    @Inject(method = "startWaitingForNewLevel", at = @At("TAIL"))
    private void skipLevelLoadingScreenOverProxy(
            LocalPlayer localPlayer, ClientLevel clientLevel, Reason reason, CallbackInfo ci) {
        if (LocalProxy.isConnectedViaProxy()) {
            this.levelLoadTracker.loadingPacketsReceived();
            if (mc.gui.screen() instanceof LevelLoadingScreen) {
                mc.gui.setScreen(null);
            }
        }
    }

    @Inject(
            method = "handleSetEntityMotion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"),
            cancellable = true)
    private void postVelocityEventForMotionPacket(
            ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null
                && clientboundSetEntityMotionPacket.id()
                        == Minecraft.getInstance().player.getId()) {
            Vec3 vector3 = clientboundSetEntityMotionPacket.movement();
            VelocityEvent velocityEvent = new VelocityEvent(vector3.x, vector3.y, vector3.z, false);
            Astralis.getInstance().getEventBus().post(velocityEvent);
            if (!velocityEvent.isCancelled()) {
                Vec3 vector32 =
                        new Vec3(velocityEvent.getX(), velocityEvent.getY(), velocityEvent.getZ());
                Minecraft.getInstance().player.lerpMotion(vector32);
            }

            ci.cancel();
        }
    }

    @Inject(
            method = "handleExplosion",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"),
            cancellable = true)
    private void postVelocityEventForExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            packet.playerKnockback()
                    .ifPresent(
                            knockback -> {
                                Vec3 motion =
                                        Minecraft.getInstance()
                                                .player
                                                .getDeltaMovement()
                                                .add(knockback);
                                VelocityEvent event =
                                        new VelocityEvent(motion.x, motion.y, motion.z, true);
                                Astralis.getInstance().getEventBus().post(event);
                                if (!event.isCancelled()) {
                                    Vec3 newMotion =
                                            new Vec3(event.getX(), event.getY(), event.getZ());
                                    Minecraft.getInstance().player.setDeltaMovement(newMotion);
                                }
                            });
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientPacketListener;seenInsecureChatWarning:Z",
                            ordinal = 0))
    public boolean suppressInsecureChatWarning(boolean original) {
        return Astralis.getInstance().getModuleManager().getModule(AntiClutter.class).isEnabled()
                        && Astralis.getInstance()
                                .getModuleManager()
                                .getModule(AntiClutter.class)
                                .chatToast
                                .getValue()
                || original;
    }

    @Inject(method = "handleOpenBook", at = @At("HEAD"), cancellable = true)
    public void handleOpenBookPacket(
            ClientboundOpenBookPacket clientboundOpenBookPacket, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(
                clientboundOpenBookPacket,
                (ClientPacketListener) (Object) this,
                mc.packetProcessor());
        ItemStack itemStack = mc.player.getItemInHand(clientboundOpenBookPacket.getHand());
        BookAccess bookAccess = BookAccess.fromItem(itemStack);
        if (bookAccess != null) {
            AutoHypixel autoHypixel =
                    Astralis.getInstance().getModuleManager().getModule(AutoHypixel.class);
            if (autoHypixel.isEnabled()
                    && autoHypixel.autoAcceptPolicy.getValue()
                    && autoHypixel.acceptPolicyInBook(bookAccess)) {
                ci.cancel();
                return;
            }

            mc.gui.setScreen(new BookViewScreen(bookAccess));
        }

        ci.cancel();
    }
}
