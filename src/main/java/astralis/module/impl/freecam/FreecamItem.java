package astralis.module.impl.freecam;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

public enum FreecamItem {
    TELEPORTER(
            Items.BLAZE_ROD,
            Component.literal("Teleporter").withStyle(ChatFormatting.AQUA),
            List.of(
                    Component.literal("Right-click to teleport the camera.")
                            .withStyle(ChatFormatting.GRAY))),
    SPEED(
            Items.SUGAR,
            Component.literal("Speed").withStyle(ChatFormatting.YELLOW),
            List.of(
                    Component.literal("Right-click to choose a camera speed.")
                            .withStyle(ChatFormatting.GRAY))),
    PLAYER_CONTROLLER(
            Items.LEAD,
            Component.literal("Player Controller").withStyle(ChatFormatting.GOLD),
            List.of(
                    Component.literal("Hold right-click to move the player with Freecam rotations.")
                            .withStyle(ChatFormatting.GRAY)));

    private final Item item;
    private final Component displayName;
    private final List<Component> lore;

    FreecamItem(Item item2, Component component2, List<Component> loreLines) {
        this.item = item2;
        this.displayName = component2;
        this.lore = loreLines;
    }

    public ItemStack createStack() {
        FreecamItemBuilder builder =
                FreecamItemBuilder.of(this.item).withName(this.displayName).withLore(this.lore);

        return builder.withTag("astralis_freecam_item", this.name()).build();
    }

    public static Optional<FreecamItem> fromStack(ItemStack itemStack) {
        CustomData customData = (CustomData) itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return Optional.empty();
        }

        CompoundTag compoundTag = customData.copyTag();

        String itemName = (String) compoundTag.getString("astralis_freecam_item").orElse(null);
        if (itemName == null) {
            return Optional.empty();
        }

        try {
            FreecamItem freecamItem = valueOf(itemName);
            return itemStack.is(freecamItem.item) ? Optional.of(freecamItem) : Optional.empty();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
