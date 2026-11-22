package astralis.module.impl.guimove.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.module.impl.GuiMove;
import astralis.network.QueuedContainerClick;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

import java.util.ArrayDeque;

public final class GuiMoveSpoof extends Mode<GuiMove> {
    private final ArrayDeque<QueuedContainerClick> pendingClicks;
    private boolean processing;
    private boolean sendingDirect;
    private boolean inventoryOpened;
    private int processingTicks;

    public GuiMoveSpoof(GuiMove guiMove) {
        super("Spoof", guiMove);
        this.pendingClicks = new ArrayDeque<>();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.POST) {
            if (this.processing && ++this.processingTicks >= 1) {
                QueuedContainerClick queuedClick = this.pendingClicks.poll();
                if (queuedClick != null) {
                    this.sendDirect(queuedClick.getClickPacket());
                    if (!queuedClick.isHoldingCarriedItem()) {
                        this.sendDirect(
                                new ServerboundContainerClosePacket(
                                        queuedClick.getClickPacket().containerId()));
                        this.inventoryOpened = false;
                    }
                }

                this.processing = false;
                this.processingTicks = 0;
                this.startProcessing();
            }

            if (mc.gui.screen() instanceof InventoryScreen
                    && this.getParent().isMovementAllowedInCurrentScreen()
                    && !this.processing) {
                this.getParent().restoreKeyStates();
            }
        }
    }

    private void sendDirect(Packet<?> packet) {
        this.sendingDirect = true;

        try {
            PacketUtil.send(packet);
        } finally {
            this.sendingDirect = false;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.processing) {
            moveInputEvent.setForward(false);
            moveInputEvent.setBackward(false);
            moveInputEvent.setLeft(false);
            moveInputEvent.setRight(false);
            moveInputEvent.setJump(false);
            moveInputEvent.setSneak(false);
            moveInputEvent.setSprint(false);
        }
    }

    private void startProcessing() {
        if (!this.processing && !this.pendingClicks.isEmpty()) {
            this.processing = true;
            this.processingTicks = 0;
            if (!this.inventoryOpened) {
                this.sendDirect(
                        new ServerboundPlayerCommandPacket(mc.player, Action.OPEN_INVENTORY));
                this.inventoryOpened = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (!this.sendingDirect && packetEvent.getStage() == EventStage.SEND) {
            if (packetEvent.getPacket()
                            instanceof ServerboundPlayerCommandPacket serverboundPlayerCommandPacket
                    && serverboundPlayerCommandPacket.getAction() == Action.OPEN_INVENTORY) {
                packetEvent.setCancelled(true);
            } else if (packetEvent.getPacket()
                            instanceof
                            ServerboundContainerClickPacket serverboundContainerClickPacket
                    && mc.gui.screen() instanceof InventoryScreen) {
                packetEvent.setCancelled(true);
                this.pendingClicks.add(
                        new QueuedContainerClick(
                                serverboundContainerClickPacket,
                                !mc.player.containerMenu.getCarried().isEmpty()));
                this.startProcessing();
            } else {
                if (packetEvent.getPacket() instanceof ServerboundContainerClosePacket
                        && (mc.gui.screen() instanceof InventoryScreen || this.processing)) {
                    packetEvent.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.inventoryOpened && mc.player.containerMenu.getCarried().isEmpty()) {
            this.sendDirect(
                    new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
        }

        this.pendingClicks.clear();
        this.processing = false;
        this.sendingDirect = false;
        this.inventoryOpened = false;
        this.processingTicks = 0;
    }

    @Override
    public void onEnable() {
        this.pendingClicks.clear();
        this.processing = false;
        this.sendingDirect = false;
        this.inventoryOpened = false;
        this.processingTicks = 0;
    }
}
