package astralis.network;

import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;

public record QueuedContainerClick(
        ServerboundContainerClickPacket clickPacket, boolean holdingCarriedItem) {
    public ServerboundContainerClickPacket getClickPacket() {
        return this.clickPacket;
    }

    public boolean isHoldingCarriedItem() {
        return this.holdingCarriedItem;
    }
}
