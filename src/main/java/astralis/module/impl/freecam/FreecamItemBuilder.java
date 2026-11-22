package astralis.module.impl.freecam;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.Arrays;
import java.util.List;

public final class FreecamItemBuilder {
    private final ItemStack stack;
    private final CompoundTag customTag = new CompoundTag();

    private FreecamItemBuilder(Item item2) {
        this.stack = new ItemStack(item2);
    }

    public static FreecamItemBuilder of(Item item2) {
        return new FreecamItemBuilder(item2);
    }

    public FreecamItemBuilder withName(Component component2) {
        this.stack.set(DataComponents.CUSTOM_NAME, component2);
        return this;
    }

    public FreecamItemBuilder withCount(int count) {
        this.stack.setCount(count);
        return this;
    }

    public FreecamItemBuilder withLoreLines(Component... lines) {
        return this.withLore(Arrays.asList(lines));
    }

    public FreecamItemBuilder withLore(List<Component> loreLines) {
        this.stack.set(DataComponents.LORE, new ItemLore(List.copyOf(loreLines)));
        return this;
    }

    public FreecamItemBuilder withTag(String key, String value) {
        this.customTag.putString(key, value);
        return this;
    }

    public ItemStack build() {
        if (!this.customTag.isEmpty()) {
            this.stack.set(DataComponents.CUSTOM_DATA, CustomData.of(this.customTag));
        }

        return this.stack;
    }
}
