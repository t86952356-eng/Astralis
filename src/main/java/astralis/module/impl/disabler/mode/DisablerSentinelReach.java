package astralis.module.impl.disabler.mode;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.module.impl.Disabler;
import astralis.module.impl.Flight;
import astralis.module.impl.combat.KillAura;
import astralis.setting.Mode;
import astralis.util.Timer;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;

public class DisablerSentinelReach extends Mode<Disabler> {
    private boolean sendingOwnPacket;
    private final Timer pongTimer;

    public DisablerSentinelReach(Disabler disabler) {
        super("Sentinel Reach", disabler);
        this.pongTimer = new Timer();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (!this.sendingOwnPacket && packetEvent.getStage() == EventStage.SEND) {
            KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
            if (killAura.isEnabled()) {
                if (mc.player.hurtTime != 0) {
                    killAura.getAttackRange().setValueRaw(3.0F);
                } else {
                    killAura.getAttackRange().setValueRaw(6.0F);
                }
            }

            if ((killAura.target != null && mc.player.hurtTime == 0
                            || Astralis.getInstance()
                                    .getModuleManager()
                                    .getModule(Flight.class)
                                    .isEnabled())
                    && packetEvent.getPacket() instanceof ServerboundPongPacket) {
                if (this.pongTimer.hasPassed(1500L)) {
                    packetEvent.setCancelled(true);
                    this.pongTimer.reset();
                } else {
                    this.sendOwnPacket(new ServerboundPongPacket(20));
                    packetEvent.setCancelled(false);
                }
            }
        }
    }

    private void sendOwnPacket(Packet<?> packet) {
        if (mc.getConnection() != null) {
            this.sendingOwnPacket = true;
            mc.getConnection().getConnection().send(packet, null);
            this.sendingOwnPacket = false;
        }
    }
}
