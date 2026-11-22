package astralis.util;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedList;
import java.util.Queue;

public class Blink extends Listener {
    private static final Queue<QueuedPacket> queue = new LinkedList<>();
    private static boolean active = false;
    private static BlinkDirection direction = BlinkDirection.OUTGOING;
    private static Vec3 lastPosition = null;

    public static void start(BlinkDirection requestedDirection) {
        if (!active) {
            queue.clear();
            direction = requestedDirection;
            active = true;
            lastPosition = null;
        }
    }

    public static Vec3 getLastPosition() {
        return lastPosition;
    }

    public static boolean isActive() {
        return active;
    }

    public static void startOutgoing() {
        start(BlinkDirection.OUTGOING);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        boolean incoming = event.getStage() == EventStage.RECEIVE;
        if (active
                && mc.level != null
                && (direction != BlinkDirection.OUTGOING || !incoming)
                && (direction != BlinkDirection.INCOMING || incoming)) {
            Packet packet = event.getPacket();
            if (!(packet instanceof ClientIntentionPacket)
                    && !(packet instanceof ServerboundStatusRequestPacket)
                    && !(packet instanceof ServerboundPingRequestPacket)
                    && !(packet instanceof ClientboundSystemChatPacket)
                    && !(packet instanceof ClientboundDisconnectPacket)
                    && !(packet instanceof ClientboundMoveEntityPacket)) {
                event.setCancelled(true);
                queue.add(new QueuedPacket(packet, System.currentTimeMillis()));
            }
        }
    }

    public static void releaseOlderThan(int ageMillis) {
        if (active) {
            while (true) {
                QueuedPacket queuedPacket = queue.peek();
                if (queuedPacket == null
                        || queuedPacket.getTime() + ageMillis >= System.currentTimeMillis()) {
                    return;
                }

                queue.poll();
                trackPosition(queuedPacket.getPacket());
                if (direction == BlinkDirection.OUTGOING) {
                    PacketUtil.send(queuedPacket.getPacket());
                } else {
                    PacketUtil.receiveNoEvent(queuedPacket.getPacket());
                }
            }
        }
    }

    private static void trackPosition(Packet<?> packet) {
        if (packet instanceof ServerboundMovePlayerPacket movementPacket
                && movementPacket.hasPosition()) {
            lastPosition =
                    new Vec3(
                            movementPacket.getX(0.0),
                            movementPacket.getY(0.0),
                            movementPacket.getZ(0.0));
        }
    }

    public static void stop() {
        if (active) {
            active = false;
            lastPosition = null;

            while (true) {
                QueuedPacket queuedPacket = queue.poll();
                if (queuedPacket == null) {
                    return;
                }

                Packet packet = queuedPacket.getPacket();
                if (direction == BlinkDirection.OUTGOING) {
                    PacketUtil.send(packet);
                } else {
                    PacketUtil.receiveNoEvent(packet);
                }
            }
        }
    }
}
