package astralis.module.impl.airstuck.mode;

import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.AirStuck;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

public class AirStuckGrim extends Mode<AirStuck> {
    public AirStuckGrim(AirStuck airStuck) {
        super("Grim", airStuck);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        PacketUtil.sendSilent(
                new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
    }
}
