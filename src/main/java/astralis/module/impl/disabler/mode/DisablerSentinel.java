package astralis.module.impl.disabler.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Disabler;
import astralis.network.HeldPacket;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.util.MathUtil;
import astralis.util.PacketUtil;
import astralis.util.Timer;

import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

import java.util.LinkedHashSet;

public class DisablerSentinel extends Mode<Disabler> {
    private final BooleanSetting experimental;
    private final BooleanSetting lowPingSpoof;
    private final LinkedHashSet<HeldPacket> heldPackets;
    private final Timer intervalTimer;
    private long randomHoldTime;

    public DisablerSentinel(Disabler disabler) {

        super("Sentinel", disabler);

        this.experimental = new BooleanSetting("18cb31c7abd804f66c3ca2e0717358a9", false);
        this.lowPingSpoof = new BooleanSetting("655302a7831c018487547c87fe543932", false);
        this.heldPackets = new LinkedHashSet<>();
        this.intervalTimer = new Timer();

        this.randomHoldTime = -1L;
        disabler.addSettings(this.experimental, this.lowPingSpoof);
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {

        synchronized (this.heldPackets) {
            this.heldPackets.clear();
        }
        this.randomHoldTime = -1L;
        this.intervalTimer.reset();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (mc.hasSingleplayerServer()) {
                this.getParent().toggle();
            } else {
                this.sendAllPackets(false);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.sendAllPackets(true);
        } else {
            if (packetEvent.getPacket() instanceof ClientboundPingPacket
                    || packetEvent.getPacket() instanceof ClientboundKeepAlivePacket) {
                packetEvent.setCancelled(true);
                synchronized (this.heldPackets) {
                    this.heldPackets.add(
                            new HeldPacket(packetEvent.getPacket(), System.currentTimeMillis()));
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.sendAllPackets(true);
    }

    @Override
    public void onEnable() {

        this.randomHoldTime = -1L;
        this.intervalTimer.reset();
    }

    private void sendAllPackets(boolean flushAll) {
        long selectedHoldTime;

        long now = System.currentTimeMillis();

        long holdTime;
        if (mc.player.tickCount < 150) {

            holdTime = 5000L;
        } else {

            if (mc.player.tickCount < 300) {

                holdTime = 10000L;
            } else {
                label42:
                {
                    if (this.randomHoldTime >= 0L) {

                        if (!this.intervalTimer.hasPassed(10000L)) {
                            break label42;
                        }
                    }

                    this.randomHoldTime = MathUtil.randomLong(10000L, 15000L);
                    this.intervalTimer.reset();
                }

                if (this.experimental.getValue()) {
                    selectedHoldTime = this.randomHoldTime;
                } else if (this.lowPingSpoof.getValue()) {

                    selectedHoldTime = 5000L;
                } else {

                    selectedHoldTime = 20000L;
                }

                holdTime = selectedHoldTime;
            }
        }

        synchronized (this.heldPackets) {
            this.heldPackets.removeIf(
                    heldPacket -> {
                        if (!flushAll && now - heldPacket.getReceivedAt() < holdTime) {
                            return false;
                        }

                        PacketUtil.receiveNoEvent(heldPacket.getPacket());
                        return true;
                    });
        }
    }
}
