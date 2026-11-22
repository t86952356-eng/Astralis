package astralis.module.impl.guimove.mode;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.impl.GuiMove;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.world.Scaffold;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.util.PacketUtil;
import astralis.util.Timer;

import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayDeque;

public class GuiMoveWatchdog extends Mode<GuiMove> {
    private final BooleanSetting test;
    private final ContainerInput[] closingInputs;
    private boolean queueingPackets;
    private final ArrayDeque<Packet<?>> queuedPackets;
    private int pendingCloseContainerId;
    private final ArrayDeque<Packet<?>> delayedClicks;
    private final Timer closeTimer;

    public GuiMoveWatchdog(GuiMove guiMove) {

        super("Watchdog", guiMove);

        this.test = new BooleanSetting("f1454d73788f26245e5c8e811bd75c2a", false);
        this.closingInputs =
                new ContainerInput[] {
                    ContainerInput.QUICK_MOVE, ContainerInput.SWAP, ContainerInput.THROW
                };
        this.queueingPackets = false;
        this.queuedPackets = new ArrayDeque<>();
        this.pendingCloseContainerId = -1;
        this.delayedClicks = new ArrayDeque<>();
        this.closeTimer = new Timer();
        this.addSettings(this.test);
    }

    private void flushQueuedPackets() {
        if (!this.queueingPackets) {
            if (mc.getConnection() != null) {
                while (!this.queuedPackets.isEmpty()) {
                    Packet packet = this.queuedPackets.pollFirst();
                    if (packet != null) {
                        PacketUtil.sendSilent(packet);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.canMoveInGui() && this.parent.isMovementAllowedInCurrentScreen()) {
            this.parent.restoreKeyStates();
        }

        if (this.test.getValue() && this.pendingCloseContainerId != -1 && mc.gui.screen() == null) {

            if (this.closeTimer.hasPassed(100L)) {
                if (this.delayedClicks.isEmpty()) {

                    if (this.closeTimer.hasPassed(270L)) {
                        PacketUtil.sendSilent(
                                new ServerboundContainerClosePacket(this.pendingCloseContainerId));
                        this.pendingCloseContainerId = -1;
                    }
                } else {
                    while (!this.delayedClicks.isEmpty()) {
                        PacketUtil.sendSilent(this.delayedClicks.poll());
                    }

                    this.closeTimer.reset();
                }
            }
        }
    }

    public boolean canMoveInGui() {
        if (mc.gui.screen() instanceof EnchantmentScreen) {
            return false;
        }

        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        Scaffold scaffold = Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
        return !mc.player.isUsingItem()
                && !mc.player.isBlocking()
                && killAura.target == null
                && !mc.isLocalServer()
                && !scaffold.isEnabled();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (this.canMoveInGui()) {
            if (this.test.getValue()
                    && packetEvent.getStage() == EventStage.RECEIVE
                    && packetEvent.getPacket()
                            instanceof
                            ClientboundContainerClosePacket clientboundContainerClosePacket) {
                this.pendingCloseContainerId = clientboundContainerClosePacket.getContainerId();
                this.closeTimer.reset();
                packetEvent.setCancelled(true);
            } else {
                if (packetEvent.getPacket()
                        instanceof
                        ServerboundContainerClickPacket serverboundContainerClickPacket) {
                    if (serverboundContainerClickPacket.containerId()
                                    == mc.player.inventoryMenu.containerId
                            && ArrayUtils.contains(
                                    this.closingInputs,
                                    serverboundContainerClickPacket.containerInput())) {
                        PacketUtil.send(
                                new ServerboundContainerClosePacket(
                                        serverboundContainerClickPacket.containerId()));
                    } else if (this.test.getValue() && this.pendingCloseContainerId != -1) {
                        this.delayedClicks.add(serverboundContainerClickPacket);
                        packetEvent.setCancelled(true);
                    } else {
                        this.queueingPackets = true;
                    }
                } else if (packetEvent.getPacket() instanceof ServerboundContainerClosePacket) {
                    this.queueingPackets = false;
                } else if (packetEvent.getPacket() instanceof ClientboundLoginPacket
                        || packetEvent.getPacket() instanceof ClientboundPlayerPositionPacket) {
                    this.queuedPackets.clear();
                    this.queueingPackets = false;
                    this.delayedClicks.clear();
                    this.pendingCloseContainerId = -1;
                } else if (this.queueingPackets
                        && packetEvent.getStage() == EventStage.SEND
                        && !(packetEvent.getPacket() instanceof ServerboundKeepAlivePacket)
                        && !(packetEvent.getPacket() instanceof ServerboundPongPacket)) {
                    this.queuedPackets.add(packetEvent.getPacket());
                    packetEvent.setCancelled(true);
                }

                if (mc.gui.screen() == null) {
                    this.queueingPackets = false;
                    this.flushQueuedPackets();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.queuedPackets.clear();
        this.delayedClicks.clear();
        this.queueingPackets = false;
        this.pendingCloseContainerId = -1;
    }
}
