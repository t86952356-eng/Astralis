package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.Module;
import astralis.setting.BooleanSetting;
import astralis.util.ChatUtil;
import astralis.util.InventoryUtil;

import com.mojang.datafixers.util.Pair;

import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import pub.astralis.client.module.core.Category;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MurderMystery extends Module {
    private static Set<UUID> murdererIds;
    private final Map<Integer, EnumMap<EquipmentSlot, ItemStack>> heldItemsByEntityId;
    private final BooleanSetting silentMurder;
    private int previousSlot;

    public MurderMystery() {

        super(MODULE_ID, "", "", Category.Misc);
        this.heldItemsByEntityId = new ConcurrentHashMap<>();

        this.silentMurder = new BooleanSetting(SILENT_MURDER_SETTING_ID, false);
        this.previousSlot = -1;
        this.addSetting(this.silentMurder);
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.heldItemsByEntityId.clear();
        murdererIds.clear();
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (this.silentMurder.getValue()) {
            if (attackEvent.getStage() == EventStage.PRE) {
                if (attackEvent.isCancelled() || this.previousSlot != -1) {
                    return;
                }

                int meleeSlot = InventoryUtil.findHotbarSlot(InventoryUtil::hasMeleeTag);
                int selectedSlot = mc.player.getInventory().getSelectedSlot();
                if (meleeSlot == -1 || meleeSlot == selectedSlot) {
                    return;
                }

                this.previousSlot = selectedSlot;
                InventoryUtil.setSlot(meleeSlot);
            } else if (attackEvent.getStage() == EventStage.POST) {
                this.restorePreviousSlot();
            }
        }
    }

    public static boolean isMurderer(Player player2) {
        return murdererIds.contains(player2.getUUID());
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE
                && packetEvent.getPacket()
                        instanceof ClientboundSetEquipmentPacket clientboundSetEquipmentPacket) {
            int entityId = clientboundSetEquipmentPacket.getEntity();
            if (mc.level.getEntity(entityId) instanceof Player player2) {
                EnumMap<EquipmentSlot, ItemStack> heldItems =
                        this.heldItemsByEntityId.computeIfAbsent(
                                entityId, key -> new EnumMap<>(EquipmentSlot.class));

                for (Pair pair : clientboundSetEquipmentPacket.getSlots()) {
                    if (pair.getFirst() == EquipmentSlot.MAINHAND
                            || pair.getFirst() == EquipmentSlot.OFFHAND) {
                        heldItems.put(
                                (EquipmentSlot) pair.getFirst(),
                                ((ItemStack) pair.getSecond()).copy());
                    }
                }

                boolean holdsMelee =
                        heldItems.values().stream().anyMatch(InventoryUtil::hasMeleeTag);
                if (holdsMelee && murdererIds.add(player2.getUUID())) {
                    String murdererName = player2.getDisplayName().getString();

                    ChatUtil.print("§cMurderer detected: §f" + murdererName);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.restorePreviousSlot();
        this.heldItemsByEntityId.clear();
        murdererIds.clear();
    }

    private void restorePreviousSlot() {
        if (this.previousSlot != -1) {
            int slot = this.previousSlot;
            this.previousSlot = -1;
            if (mc.player.getInventory().getSelectedSlot() != slot) {
                InventoryUtil.setSlot(slot);
            }
        }
    }

    static {
        murdererIds = ConcurrentHashMap.newKeySet();
    }

    private static final String MODULE_ID = "7d0f26405e7f3695a8ef98bbb7548388";
    private static final String SILENT_MURDER_SETTING_ID = "3de1e98fb71ce09cd51e53bcd3ba5b93";
}
