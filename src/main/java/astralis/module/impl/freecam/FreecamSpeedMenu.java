package astralis.module.impl.freecam;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.Optional;

public final class FreecamSpeedMenu {
    private static String speedTag = "freecam-speed";
    private static String actionTag = "freecam-speed-action";
    private static String exitAction = "exit";
    private static float[] speedOptions = {0.1F, 0.5F, 1.0F, 2.0F, 3.0F, 5.0F, 7.5F, 10.0F};

    private FreecamSpeedMenu() {}

    static void fillSpeedMenu(Inventory inventory, float selectedSpeed) {
        int index = 0;

        while (index < speedOptions.length) {
            float speed = speedOptions[index];
            boolean isSelected = Float.compare(speed, selectedSpeed) == 0;
            NonNullList nonNullList = inventory.getNonEquipmentItems();
            FreecamItemBuilder itemBuilder =
                    FreecamItemBuilder.of(Items.SUGAR_CANE).withCount(index + 1);

            FreecamItemBuilder namedBuilder =
                    itemBuilder.withName(
                            Component.literal(String.format(Locale.ROOT, "Speed: %.1f", speed))
                                    .withStyle(
                                            isSelected
                                                    ? ChatFormatting.GREEN
                                                    : ChatFormatting.YELLOW));
            Component[] loreLines = new Component[1];
            String loreText;
            if (isSelected) {

                loreText = "Selected";
            } else {

                loreText = "Right-click to select.";
            }

            loreLines[0] =
                    Component.literal(loreText)
                            .withStyle(
                                    isSelected ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
            itemBuilder = namedBuilder.withLoreLines(loreLines);
            nonNullList.set(
                    index, itemBuilder.withTag("freecam-speed", Float.toString(speed)).build());
            index++;
        }

        NonNullList nonNullList2 = inventory.getNonEquipmentItems();

        FreecamItemBuilder barrierBuilder = FreecamItemBuilder.of(Items.BARRIER);

        FreecamItemBuilder namedBarrierBuilder =
                barrierBuilder.withName(Component.literal("Back").withStyle(ChatFormatting.RED));
        Component[] backLore = new Component[1];

        backLore[0] =
                Component.literal("Right-click to return to Freecam controls.")
                        .withStyle(ChatFormatting.GRAY);
        FreecamItemBuilder loreBarrierBuilder = namedBarrierBuilder.withLoreLines(backLore);

        nonNullList2.set(8, loreBarrierBuilder.withTag("freecam-speed-action", "exit").build());
        inventory.setSelectedSlot(0);
    }

    public static Optional<Float> getSpeedFromItem(ItemStack itemStack) {
        if (!itemStack.is(Items.SUGAR_CANE)) {
            return Optional.empty();
        }

        String speedText = getStringTag(itemStack, "freecam-speed");
        if (speedText == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Float.parseFloat(speedText));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isExitItem(ItemStack itemStack) {

        if (itemStack.is(Items.BARRIER)) {

            if ("exit".equals(getStringTag(itemStack, "freecam-speed-action"))) {
                return true;
            }
        }

        return false;
    }

    private static String getStringTag(ItemStack itemStack, String key) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag().getString(key).orElse(null);
    }
}
