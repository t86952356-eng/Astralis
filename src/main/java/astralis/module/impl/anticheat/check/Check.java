package astralis.module.impl.anticheat.check;

import astralis.module.impl.AntiCheat;
import astralis.setting.BooleanSetting;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Player;

/** Shared check configuration and flag reporting. */
public abstract class Check implements InstanceAccess {
    private final AntiCheat antiCheat;
    private final CheckType checkType;
    private final BooleanSetting enabledSetting;

    protected Check(AntiCheat antiCheat, CheckType checkType, BooleanSetting enabledSetting) {
        this.antiCheat = antiCheat;
        this.checkType = checkType;
        this.enabledSetting = enabledSetting;
    }

    /** Observes one player snapshot; checks override this hook when needed. */
    public void inspectPlayer(CheckContext context) {}

    /** Observes an outgoing packet and the local player state. */
    public void inspectOutgoingPacket(Packet<?> packet, PlayerCheckState state) {}

    /** Reports a flag through the owner using this check category. */
    protected final void flag(Player player, PlayerCheckState state, String details) {
        this.antiCheat.getFlagReporter().reportFlag(player, state, this.checkType, details);
    }

    /** Returns the configured enabled state for this check. */
    public final boolean isCheckEnabled() {
        return this.enabledSetting.getValue();
    }
}
