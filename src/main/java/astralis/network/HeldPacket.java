package astralis.network;

import net.minecraft.network.protocol.Packet;

public record HeldPacket(Packet<?> packet, long receivedAt) {
    public Packet<?> getPacket() {
        return this.packet;
    }

    public long getReceivedAt() {
        return this.receivedAt;
    }
}
