package astralis.event.impl;

import astralis.event.EventStage;
import astralis.event.StagedEvent;

import net.minecraft.network.protocol.Packet;

public class PacketEvent extends StagedEvent {
    private Packet<?> packet;

    public PacketEvent(Packet<?> packet, EventStage stage) {
        super(stage);
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }

    public void setPacket(Packet<?> packet) {
        this.packet = packet;
    }

    public static int getFlowKey() {
        return 108;
    }
}
