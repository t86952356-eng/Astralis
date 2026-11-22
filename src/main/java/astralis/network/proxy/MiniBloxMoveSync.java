package astralis.network.proxy;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.PacketEvent;
import astralis.network.MiniBloxMovePayload;
import astralis.util.Listener;
import astralis.util.LocalProxy;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;

public final class MiniBloxMoveSync extends Listener {
    private MotionEvent lastMotionEvent;

    public void setLastMotionEvent(MotionEvent motionEvent) {
        this.lastMotionEvent = motionEvent;
    }

    @EventTarget(4)
    public void onPacket(PacketEvent packetEvent) {
        if (this.isEnabled()
                && LocalProxy.isConnectedViaProxy()
                && packetEvent.getStage() == EventStage.SEND
                && !packetEvent.isCancelled()
                && packetEvent.getPacket()
                        instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            Vec2 vec2 = mc.player.input.getMoveVector();
            ClientPlayNetworking.send(
                    new MiniBloxMovePayload(
                            mc.player.xo,
                            mc.player.yo,
                            mc.player.zo,
                            this.lastMotionEvent != null
                                    ? this.lastMotionEvent.getYaw()
                                    : serverboundMovePlayerPacket.getYRot(mc.player.yRotLast),
                            this.lastMotionEvent != null
                                    ? this.lastMotionEvent.getPitch()
                                    : serverboundMovePlayerPacket.getXRot(mc.player.xRotLast),
                            vec2.y,
                            vec2.x,
                            mc.player.input.keyPresses.jump(),
                            mc.player.input.keyPresses.shift(),
                            serverboundMovePlayerPacket.isOnGround(),
                            mc.player.isSprinting()));
        }
    }
}
