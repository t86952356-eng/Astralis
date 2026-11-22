package astralis.module.impl.guimove.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.module.impl.GuiMove;
import astralis.setting.Mode;
import astralis.util.PacketUtil;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayDeque;

public final class GuiMoveWatchdogBlinkless extends Mode<GuiMove> {
    private final ArrayDeque<ServerboundContainerClickPacket> queuedClicks;
    private boolean suppressingSprint;

    public GuiMoveWatchdogBlinkless(GuiMove guiMove) {
        super("Watchdog Blinkless", guiMove);
        this.queuedClicks = new ArrayDeque<>();
    }

    private boolean hasPendingInteraction() {
        return !this.queuedClicks.isEmpty() || !mc.player.containerMenu.getCarried().isEmpty();
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.parent.getAllowInventory().getValue()
                && mc.gui.screen() instanceof InventoryScreen
                && !mc.player.isUsingItem()
                && !this.hasPendingInteraction()) {
            if (mc.player.tickCount % 4 == 1) {
                PacketUtil.sendSilent(
                        new ServerboundContainerClosePacket(mc.player.inventoryMenu.containerId));
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND) {
            if (this.suppressingSprint
                    && packetEvent.getPacket()
                            instanceof ServerboundPlayerCommandPacket serverboundPlayerCommandPacket
                    && serverboundPlayerCommandPacket.getAction() == Action.START_SPRINTING) {
                packetEvent.setCancelled(true);
                mc.player.setSprinting(false);
            } else if (packetEvent.getPacket()
                            instanceof
                            ServerboundContainerClosePacket serverboundContainerClosePacket
                    && serverboundContainerClosePacket.getContainerId()
                            == mc.player.inventoryMenu.containerId) {
                if (!(mc.gui.screen() instanceof InventoryScreen)
                        || !this.suppressingSprint && this.queuedClicks.isEmpty()) {
                    if (!this.queuedClicks.isEmpty()) {
                        this.flushQueuedClicks();
                        this.suppressingSprint = false;
                    }
                } else {
                    packetEvent.setCancelled(true);
                }
            } else if (packetEvent.getPacket()
                            instanceof
                            ServerboundContainerClickPacket serverboundContainerClickPacket
                    && serverboundContainerClickPacket.containerId()
                            == mc.player.inventoryMenu.containerId) {
                if (serverboundContainerClickPacket.containerInput()
                        == ContainerInput.QUICK_CRAFT) {
                    int quickCraftHeader =
                            AbstractContainerMenu.getQuickcraftHeader(
                                    serverboundContainerClickPacket.buttonNum());
                    if (this.suppressingSprint
                            || quickCraftHeader == 0
                                    && this.parent.getAllowInventory().getValue()
                                    && mc.gui.screen() instanceof InventoryScreen) {
                        if (quickCraftHeader == 0) {
                            this.queuedClicks.clear();
                            this.suppressingSprint = true;
                            this.stopSprinting();
                        }

                        packetEvent.setCancelled(true);
                        this.queuedClicks.addLast(serverboundContainerClickPacket);
                        if (quickCraftHeader == 2) {
                            this.flushQueuedClicks();
                            this.suppressingSprint = this.hasPendingInteraction();
                        }
                    }
                } else if (!this.suppressingSprint) {
                    if (serverboundContainerClickPacket.containerInput()
                                    == ContainerInput.QUICK_MOVE
                            || serverboundContainerClickPacket.containerInput()
                                    == ContainerInput.SWAP
                            || serverboundContainerClickPacket.containerInput()
                                    == ContainerInput.THROW) {
                        PacketUtil.send(
                                new ServerboundContainerClosePacket(
                                        serverboundContainerClickPacket.containerId()));
                    }
                }
            }
        }
    }

    @EventTarget(4)
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.parent.getAllowInventory().getValue()
                && mc.gui.screen() instanceof InventoryScreen
                && this.hasPendingInteraction()) {
            if (!this.suppressingSprint) {
                this.suppressingSprint = true;
                this.stopSprinting();
            }

            moveInputEvent.setSprint(false);
            mc.player.setSprinting(false);
        } else if (this.queuedClicks.isEmpty()) {
            this.suppressingSprint = false;
        }

        if (this.parent.getAllowInventory().getValue()
                && mc.gui.screen() instanceof InventoryScreen) {
            this.parent.restoreKeyStates();
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && !this.queuedClicks.isEmpty()) {
            this.flushQueuedClicks();
        }

        this.queuedClicks.clear();
        this.suppressingSprint = false;
    }

    private void stopSprinting() {
        mc.player.setSprinting(false);
        PacketUtil.sendSilent(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
    }

    private void flushQueuedClicks() {
        this.stopSprinting();

        while (!this.queuedClicks.isEmpty()) {
            PacketUtil.sendSilent(this.queuedClicks.removeFirst());
        }
    }
}
