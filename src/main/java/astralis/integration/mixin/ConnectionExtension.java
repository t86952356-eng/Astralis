package astralis.integration.mixin;

import net.minecraft.network.protocol.Packet;

public interface ConnectionExtension {
    void astralis$sendSilent(Packet<?> packet);

    void astralis$receiveNoEvent(Packet<?> packet);

    void astralis$sendWithEvent(Packet<?> packet);
}
