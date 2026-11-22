package astralis.module.impl.flight.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.impl.Flight;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.ChatUtil;
import astralis.util.PacketUtil;
import astralis.util.player.TimerSpeed;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;

public class FlightSentinel extends Mode<Flight> {
    private final NumberSetting timer;
    private final NumberSetting teleportDistance;
    private final NumberSetting teleportDelay;
    private boolean startPacketsPending;
    private long lastTeleportTime;
    private double distanceFlown;
    private boolean warningShown;
    private double lastX;
    private double lastZ;

    public FlightSentinel(Flight flight) {

        super("Sentinel", flight);

        this.timer = new NumberSetting("94a91ed89a69b2ceb066a5e877b1732b", 0.3F, 0.1F, 1.0F, 0.01F);

        this.teleportDistance =
                new NumberSetting(
                        "f3d1033f0b3830808254952eb12fbf7130adfaee2a993919e993dbb4caef6f57",
                        4.0F,
                        1.0F,
                        8.0F,
                        1.0F);

        this.teleportDelay =
                new NumberSetting(
                        "1f3540f892c9a603054aeb4d33ae7900c530ad5c1b9524bdf78a04775fcb2903",
                        150.0F,
                        0.0F,
                        500.0F,
                        50.0F);
        this.addSettings(this.timer, this.teleportDistance, this.teleportDelay);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {

        if (this.startPacketsPending) {
            int groundDepth = 0;
            int depth = 1;

            while (true) {

                if (depth > 6) {
                    break;
                }

                if (!mc.level.getBlockState(mc.player.blockPosition().below(depth)).isAir()) {
                    groundDepth = depth;
                    break;
                }

                depth++;
            }

            double dropDistance = Math.min(6, groundDepth - 1);
            PacketUtil.send(
                    new Pos(
                            mc.player.getX(),
                            mc.player.getY() + 4.0,
                            mc.player.getZ(),
                            false,
                            mc.player.horizontalCollision));
            PacketUtil.send(
                    new Pos(
                            mc.player.getX(),
                            mc.player.getY() - dropDistance,
                            mc.player.getZ(),
                            true,
                            mc.player.horizontalCollision));
            this.startPacketsPending = false;
        }

        TimerSpeed.multiplier = this.timer.getValue();
        if (mc.player.hurtTime != 0) {
            long now = System.currentTimeMillis();
            if ((float) (now - this.lastTeleportTime) < this.teleportDelay.getValue()) {
                return;
            }

            this.lastTeleportTime = now;
            float yawRadians = (float) Math.toRadians(mc.player.getYRot());
            double targetX =
                    mc.player.getX()
                            - Math.sin(yawRadians) * this.teleportDistance.getValue().floatValue();
            double targetZ =
                    mc.player.getZ()
                            + Math.cos(yawRadians) * this.teleportDistance.getValue().floatValue();
            mc.player.setPos(targetX, mc.player.getY(), targetZ);
        }

        double deltaX = mc.player.getX() - this.lastX;
        double deltaZ = mc.player.getZ() - this.lastZ;
        this.distanceFlown = this.distanceFlown + Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        this.lastX = mc.player.getX();
        this.lastZ = mc.player.getZ();
        if (!this.warningShown && this.distanceFlown >= 75.0) {

            ChatUtil.print("Warning: Flying more than 75 blocks may lead to bans.");
            this.warningShown = true;
        }

        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x, 0.0, mc.player.getDeltaMovement().z);
    }

    @Override
    public void onDisable() {
        TimerSpeed.multiplier = 1.0F;
        this.startPacketsPending = false;
        this.lastTeleportTime = 0L;
        this.distanceFlown = 0.0;
        this.warningShown = false;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (mc.player.hurtTime != 0) {
            moveInputEvent.setJump(false);
            moveInputEvent.setSneak(false);
        }

        if (mc.player.hurtTime == 0) {
            moveInputEvent.setJump(false);
            moveInputEvent.setSneak(false);
            moveInputEvent.setForward(false);
            moveInputEvent.setBackward(false);
        }
    }

    @Override
    public void onEnable() {
        this.startPacketsPending = true;
        this.lastX = mc.player.getX();
        this.lastZ = mc.player.getZ();
        this.distanceFlown = 0.0;
        this.warningShown = false;
    }
}
