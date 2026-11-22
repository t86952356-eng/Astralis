package astralis.module.impl.flight.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.MoveRelativeEvent;
import astralis.event.impl.PacketEvent;
import astralis.module.impl.Flight;
import astralis.setting.Mode;
import astralis.util.BreakUtil;
import astralis.util.ChatUtil;
import astralis.util.MoveUtil;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.phys.Vec3;

public class FlightGrim extends Mode<Flight> {
    private boolean frozen;
    private boolean velocityReceived;

    public FlightGrim(Flight flight) {

        super("Grim", flight);
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        Packet packet = packetEvent.getPacket();
        if (packet instanceof ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket
                && clientboundSetEntityMotionPacket.id() == mc.player.getId()) {
            this.frozen = false;
            packetEvent.setCancelled(true);
            this.velocityReceived = true;
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (this.frozen) {
            motionEvent.setCancelled(true);
        }
    }

    @EventTarget
    public void onMoveRelative(MoveRelativeEvent event) {
        if (event.getStage() == EventStage.POST) {
            if (!this.frozen && mc.player.fallDistance > 0.0) {
                PacketUtil.sendSilent(new StatusOnly(true, false));
                mc.player.fallDistance = 0.0;
                this.frozen = true;
            }

            if (this.frozen) {
                mc.player.setDeltaMovement(Vec3.ZERO);
            }

            if (this.velocityReceived) {
                MoveUtil.setSpeed(0.3F);
                BreakUtil.setMotionY(-2.0E-4);
                this.velocityReceived = false;
            }
        }
    }

    @Override
    public void onEnable() {

        ChatUtil.print("You need to be on 1.9-1.18.2 for this to work");
        this.frozen = false;
        this.velocityReceived = false;
    }
}
