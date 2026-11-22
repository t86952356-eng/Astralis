package astralis.util;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.combat.KillAura;
import astralis.network.DelayedPacket;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PacketDelay extends Listener {
    private static Queue<DelayedPacket> delayedPackets;
    private static List<Class<? extends Packet<?>>> passthroughPacketTypes;
    private static List<Class<? extends Packet<?>>> stopDelayPacketTypes;
    private static boolean delayingPackets;
    private static boolean replayingPacket;
    private static long delayMillis;
    private static Vec3 lastSentPosition;

    @EventTarget
    public void onWorldChange(WorldChangeEvent event) {
        if (delayingPackets) {
            stopAndFlush();
        }
    }

    /** Returns the position from the last replayed outgoing movement packet. */
    public static Vec3 getLastSentPosition() {
        return lastSentPosition;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (delayingPackets
                && !replayingPacket
                && event.getStage() != EventStage.RECEIVE
                && !(event.getPacket() instanceof ServerboundStatusRequestPacket)) {
            for (Class passthroughType : passthroughPacketTypes) {
                if (passthroughType.isInstance(event.getPacket())) {
                    return;
                }
            }

            for (Class stopDelayType : stopDelayPacketTypes) {
                KillAura killAura =
                        Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                if (stopDelayType.isInstance(event.getPacket())
                        && (!killAura.isEnabled()
                                || killAura.target == null
                                        && !killAura.getAutoBlockMode().is("Lag"))) {
                    stopAndFlush();
                    return;
                }
            }

            event.setCancelled(true);
            delayedPackets.add(
                    new DelayedPacket(event.getPacket(), System.currentTimeMillis() + delayMillis));
        }
    }

    private static boolean shouldStopForItemUse() {
        return BreakUtil.isUsingItem() || Blink.isActive() && mc.player.isUsingItem();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (delayingPackets && event.getStage() != EventStage.POST) {
            if (shouldStopForItemUse()) {
                stopAndFlush();
            } else {
                long now = System.currentTimeMillis();

                while (!delayedPackets.isEmpty()) {
                    DelayedPacket queuedPacket = delayedPackets.peek();
                    if (queuedPacket.getReleaseTime() > now) {
                        break;
                    }

                    delayedPackets.poll();
                    replayDelayedPacket(queuedPacket);
                }
            }
        }
    }

    public static void startDelay(long durationMillis) {
        if (!delayingPackets) {
            delayedPackets.clear();
            delayMillis = durationMillis;
            delayingPackets = true;
        }
    }

    private static void replayDelayedPacket(DelayedPacket queuedPacket) {
        Packet packet = queuedPacket.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket movementPacket) {
            lastSentPosition =
                    new Vec3(
                            movementPacket.getX(mc.player.getX()),
                            movementPacket.getY(mc.player.getY()),
                            movementPacket.getZ(mc.player.getZ()));
        }

        replayingPacket = true;
        PacketUtil.send(queuedPacket.getPacket());
        replayingPacket = false;
    }

    /** Returns whether outgoing packet delay is active. */
    public static boolean isDelaying() {
        return delayingPackets;
    }

    public static void stopAndFlush() {
        if (delayingPackets) {
            delayingPackets = false;

            while (!delayedPackets.isEmpty()) {
                replayDelayedPacket(delayedPackets.poll());
            }
        }
    }

    static {
        delayedPackets = new LinkedList<>();
        passthroughPacketTypes =
                new ArrayList<>(
                        List.of(
                                ServerboundInteractPacket.class,
                                ServerboundContainerClickPacket.class,
                                ServerboundContainerClosePacket.class,
                                ServerboundChatPacket.class));
        stopDelayPacketTypes =
                new ArrayList<>(
                        List.of(
                                ServerboundPlayerActionPacket.class,
                                ServerboundUseItemOnPacket.class));
        replayingPacket = false;
        delayMillis = 0L;
        lastSentPosition = null;
    }
}
