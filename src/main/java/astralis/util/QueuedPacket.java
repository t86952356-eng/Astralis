package astralis.util;

import net.minecraft.network.protocol.Packet;

public record QueuedPacket(Packet<?> packet, long time) {
    public Packet<?> getPacket() {
        return this.packet;
    }

    public long getTime() {
        return this.time;
    }
}
