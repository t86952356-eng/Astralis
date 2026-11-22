package astralis.module.impl.guimove.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.module.impl.GuiMove;
import astralis.setting.Mode;
import astralis.util.MoveUtil;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

import java.util.ArrayDeque;

public class GuiMoveStop extends Mode<GuiMove> {
    private final ArrayDeque<Packet<?>> heldPackets;

    public GuiMoveStop(GuiMove guiMove) {
        super("Stop", guiMove);
        this.heldPackets = new ArrayDeque<>();
    }

    @Override
    public void onDisable() {
        this.heldPackets.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND) {
            Packet packet = packetEvent.getPacket();
            if ((packet instanceof ServerboundContainerClickPacket
                            || packet instanceof ServerboundContainerClosePacket)
                    && MoveUtil.isMoving()) {
                packetEvent.setCancelled(true);
                this.heldPackets.add(packet);
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.getParent().isMovementAllowedInCurrentScreen()) {
            this.getParent().restoreKeyStates();
        }

        if (!this.heldPackets.isEmpty()) {
            moveInputEvent.setForward(false);
            moveInputEvent.setBackward(false);
            moveInputEvent.setLeft(false);
            moveInputEvent.setRight(false);
            moveInputEvent.setJump(false);
            moveInputEvent.setSneak(false);
            moveInputEvent.setSprint(false);
            if (!MoveUtil.isMoving()) {
                while (!this.heldPackets.isEmpty()) {
                    PacketUtil.sendSilent(this.heldPackets.poll());
                }
            }
        }
    }
}
