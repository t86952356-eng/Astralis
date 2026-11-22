package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.NoFallDamage;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public class NoFallDamageVanilla extends Mode<NoFallDamage> {
    public NoFallDamageVanilla(NoFallDamage noFallDamage) {
        super("Vanilla", noFallDamage);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (mc.player.fallDistance >= 1.5) {
            PacketUtil.send(new StatusOnly(true, mc.player.horizontalCollision));
            mc.player.fallDistance = 0.0;
        }
    }
}
