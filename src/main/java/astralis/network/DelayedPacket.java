package astralis.network;

import net.minecraft.network.protocol.Packet;

public record DelayedPacket(Packet<?> packet, long releaseTime) {
    public Packet<?> getPacket() {
        return this.packet;
    }

    public long getReleaseTime() {
        return this.releaseTime;
    }
}
