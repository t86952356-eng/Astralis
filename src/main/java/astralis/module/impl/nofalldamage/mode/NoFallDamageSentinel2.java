package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.mixin.ServerboundMovePlayerPacketAccessor;
import astralis.module.impl.NoFallDamage;
import astralis.setting.Mode;

import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFallDamageSentinel2 extends Mode<NoFallDamage> {
    public NoFallDamageSentinel2(NoFallDamage noFallDamage) {
        super("Sentinel 2", noFallDamage);
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND) {
            if (packetEvent.getPacket()
                    instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
                if (!(mc.player.fallDistance < 2.5)) {
                    ServerboundMovePlayerPacketAccessor accessor =
                            (ServerboundMovePlayerPacketAccessor) serverboundMovePlayerPacket;
                    double posY = serverboundMovePlayerPacket.getY(0.0);
                    accessor.setY(posY - posY % 0.015625);
                    accessor.setOnGround(true);
                    mc.player.setPos(mc.player.xo, mc.player.yo, mc.player.zo);
                    mc.player.setDeltaMovement(
                            mc.player
                                    .getDeltaMovement()
                                    .with(Axis.Y, mc.player.getDeltaMovement().y + 0.1));
                    mc.player.fallDistance = 0.0;
                }
            }
        }
    }
}
