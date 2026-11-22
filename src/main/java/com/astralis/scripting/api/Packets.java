package com.astralis.scripting.api;

import astralis.util.PacketUtil;

import com.astralis.scripting.InstanceAccess;
import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptEntity;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;

public class Packets implements InstanceAccess {
    private Script owner;
    private boolean destroyed;

    public final synchronized void destroy() {
        checkScriptRuntimeCaller();
        if (!this.destroyed) {
            this.destroyed = true;
            this.owner = null;
        }
    }

    public final synchronized void bind(Script script) {
        checkScriptRuntimeCaller();
        if (script == null) {
            throw new IllegalArgumentException("Script owner cannot be null.");
        }

        if (this.destroyed) {
            throw new IllegalStateException("This packet service has already been destroyed.");
        }

        if (this.owner != null) {
            if (this.owner != script) {
                throw new IllegalStateException("This packet service is already bound.");
            }
        } else {
            this.owner = script;
        }
    }

    public void setSprinting(boolean sprinting) {
        this.ensureActive();
        if (mc.player != null) {
            ServerboundPlayerCommandPacket serverboundPlayerCommandPacket =
                    new ServerboundPlayerCommandPacket(
                            mc.player, sprinting ? Action.START_SPRINTING : Action.STOP_SPRINTING);
            PacketUtil.send(serverboundPlayerCommandPacket);
        }
    }

    public void swingHand() {
        this.ensureActive();
        if (mc.player != null) {
            ServerboundSwingPacket serverboundSwingPacket =
                    new ServerboundSwingPacket(InteractionHand.MAIN_HAND);
            PacketUtil.send(serverboundSwingPacket);
        }
    }

    public void sendLook(float yaw, float pitch, boolean onGround) {
        this.ensureActive();
        if (mc.player != null && Float.isFinite(yaw) && Float.isFinite(pitch)) {
            Rot packet =
                    new Rot(
                            Mth.wrapDegrees(yaw),
                            Math.clamp(pitch, -90.0F, 90.0F),
                            onGround,
                            false);
            PacketUtil.send(packet);
        }
    }

    public void sendLook(float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        this.ensureActive();
        if (mc.player != null && Float.isFinite(yaw) && Float.isFinite(pitch)) {
            Rot packet =
                    new Rot(
                            Mth.wrapDegrees(yaw),
                            Math.clamp(pitch, -90.0F, 90.0F),
                            onGround,
                            horizontalCollision);
            PacketUtil.send(packet);
        }
    }

    public void sendPosition(
            double x, double y, double z, boolean onGround, boolean horizontalCollision) {
        this.ensureActive();
        if (mc.player != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            Pos packet = new Pos(x, y, z, onGround, horizontalCollision);
            PacketUtil.send(packet);
        }
    }

    public void sendPosition(double x, double y, double z, boolean onGround) {
        this.ensureActive();
        if (mc.player != null && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            Pos packet = new Pos(x, y, z, onGround, false);
            PacketUtil.send(packet);
        }
    }

    public void sendChat(String message) {
        this.ensureActive();
        if (mc.player != null && message != null) {
            if (message.startsWith("/")) {
                mc.player.connection.sendCommand(message.substring(1));
            } else {
                mc.player.connection.sendChat(message);
            }
        }
    }

    public void attack(ScriptEntity target) {
        this.ensureActive();
        if (mc.player != null && target != null) {
            ServerboundAttackPacket serverboundAttackPacket =
                    new ServerboundAttackPacket(target.getId());
            PacketUtil.send(serverboundAttackPacket);
        }
    }

    public void selectSlot(int slot) {
        this.ensureActive();
        if (mc.player != null && slot >= 0 && slot <= 8) {
            mc.player.getInventory().setSelectedSlot(slot);
            ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket =
                    new ServerboundSetCarriedItemPacket(slot);
            PacketUtil.send(serverboundSetCarriedItemPacket);
        }
    }

    private synchronized void ensureActive() {
        if (this.owner == null || this.destroyed || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException("The script packet service is not active.");
        }
    }

    private static void checkScriptRuntimeCaller() {
        Class callerClass =
                StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                        .walk(
                                frames ->
                                        frames.map(StackFrame::getDeclaringClass)
                                                .filter(frameClass -> frameClass != Packets.class)
                                                .findFirst()
                                                .orElseThrow(
                                                        () ->
                                                                new SecurityException(
                                                                        "Missing script runtime caller.")));
        if (callerClass != Script.class
                && (!Script.class.isAssignableFrom(callerClass)
                        || callerClass.getClassLoader() != Script.class.getClassLoader())) {
            throw new SecurityException("Script packet ownership is managed by the runtime.");
        }
    }

    public void swingOffHand() {
        this.ensureActive();
        if (mc.player != null) {
            ServerboundSwingPacket serverboundSwingPacket =
                    new ServerboundSwingPacket(InteractionHand.OFF_HAND);
            PacketUtil.send(serverboundSwingPacket);
        }
    }

    public void sendPositionAndLook(
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            boolean onGround,
            boolean horizontalCollision) {
        this.ensureActive();
        if (mc.player != null
                && Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(z)
                && Float.isFinite(yaw)
                && Float.isFinite(pitch)) {
            PosRot posRot =
                    new PosRot(
                            x,
                            y,
                            z,
                            Mth.wrapDegrees(yaw),
                            Math.clamp(pitch, -90.0F, 90.0F),
                            onGround,
                            horizontalCollision);
            PacketUtil.send(posRot);
        }
    }

    public void sendOnGround(boolean onGround, boolean horizontalCollision) {
        this.ensureActive();
        if (mc.player != null) {
            StatusOnly statusOnly = new StatusOnly(onGround, horizontalCollision);
            PacketUtil.send(statusOnly);
        }
    }
}
