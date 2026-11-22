package astralis.module.impl.bedwars.mode;

import astralis.event.EventTarget;
import astralis.event.impl.SlotClickEvent;
import astralis.module.impl.Bedwars;
import astralis.setting.Mode;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;

import java.util.Locale;
import java.util.Set;

public class BedwarsInstantBuy extends Mode<Bedwars> {
    private static Set<String> shopTitles;

    public BedwarsInstantBuy(Bedwars bedwars) {

        super("Instant Buy", bedwars);
    }

    @EventTarget
    public void onSlotClick(SlotClickEvent event) {
        if (!event.isCancelled() && mc.player != null && mc.gameMode != null) {
            if (Bedwars.isInBedwarsGame() && event.getContainerInput() == ContainerInput.PICKUP) {
                if (event.getSlotIndex() >= 0
                        && event.getSlot() != null
                        && event.getSlot().hasItem()) {
                    if (mc.gui.screen() instanceof ContainerScreen containerScreen
                            && containerScreen.getMenu() instanceof ChestMenu) {
                        String title =
                                ChatFormatting.stripFormatting(
                                        containerScreen.getTitle().getString());
                        if (title != null
                                && shopTitles.contains(title.trim().toLowerCase(Locale.ROOT))) {
                            if (event.getSlotIndex() < containerScreen.getMenu().slots.size()) {
                                event.setCancelled(true);
                                mc.gameMode.handleContainerInput(
                                        containerScreen.getMenu().containerId,
                                        event.getSlotIndex(),
                                        2,
                                        ContainerInput.CLONE,
                                        mc.player);
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        shopTitles =
                Set.of(
                        "quick buy",
                        "blocks",
                        "melee",
                        "armor",
                        "tools",
                        "ranged",
                        "potions",
                        "utility",
                        "rotating items",
                        "upgrades & traps");
    }
}
