package astralis.network;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.impl.PacketEvent;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

import java.util.ArrayList;
import java.util.Iterator;

public final class PacketEventDispatcher {
    private static ThreadLocal<Integer> dispatchDepth = ThreadLocal.withInitial(() -> 0);

    private PacketEventDispatcher() {}

    public static Packet<?> filterPacket(Packet<?> packet, EventStage stage) {
        if (stage == EventStage.RECEIVE) {
            if (packet instanceof ClientboundBundlePacket clientboundBundlePacket) {
                return filterBundle(clientboundBundlePacket);
            }
        }

        return postPacketEvent(packet, stage);
    }

    private static Packet<?> filterBundle(ClientboundBundlePacket clientboundBundlePacket) {
        ArrayList filteredPackets = new ArrayList();
        boolean modified = false;
        Iterator iterator = clientboundBundlePacket.subPackets().iterator();

        while (iterator.hasNext()) {
            Packet packet = (Packet) iterator.next();
            Packet packet2 = postPacketEvent(packet, EventStage.RECEIVE);
            if (packet2 == null) {
                modified = true;
                continue;
            }

            if (packet2 != packet) {
                modified = true;
            }

            filteredPackets.add(packet2);
        }

        if (!modified) {
            return clientboundBundlePacket;
        }

        return filteredPackets.isEmpty() ? null : new ClientboundBundlePacket(filteredPackets);
    }

    private static Packet<?> postPacketEvent(Packet<?> originalPacket, EventStage stage) {
        if (dispatchDepth.get() >= 16) {
            return null;
        }

        dispatchDepth.set(dispatchDepth.get() + 1);

        try {
            PacketEvent packetEvent = new PacketEvent(originalPacket, stage);
            Astralis.getInstance().getEventBus().post(packetEvent);
            if (packetEvent.isCancelled()) {
                return null;
            }

            if (packetEvent.getPacket() == null) {
                return null;
            }

            PacketFlow packetFlow =
                    stage == EventStage.SEND ? PacketFlow.SERVERBOUND : PacketFlow.CLIENTBOUND;
            Packet packet = packetEvent.getPacket();
            if (packet.type().flow() != packetFlow) {
                return null;
            }

            return packetEvent.getPacket();
        } finally {
            int remainingDepth = dispatchDepth.get() - 1;
            if (remainingDepth == 0) {
                dispatchDepth.remove();
            } else {
                dispatchDepth.set(remainingDepth);
            }
        }
    }
}
