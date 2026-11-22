package com.astralis.scripting.api.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ScriptItemStack {
    private final ItemStack stack;

    public ScriptItemStack(Object stackObject) {
        if (stackObject instanceof ItemStack itemStack) {
            this.stack = itemStack;
        } else {
            throw new IllegalArgumentException("Expected an item stack.");
        }
    }

    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    public String getName() {
        return this.stack.getHoverName().getString();
    }

    public String getItemId() {
        return BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString();
    }

    public String getLegacyId() {
        String itemId = this.getItemId();
        String path = itemId.substring(itemId.indexOf(58) + 1).toLowerCase(Locale.ROOT);
        if (path.endsWith("_wool")) {
            return "wool";
        }

        if (path.endsWith("_planks")) {
            return "planks";
        }

        if (path.endsWith("_stained_glass")) {
            return "stained_glass";
        }

        if (path.endsWith("_stained_glass_pane")) {
            return "stained_glass_pane";
        }

        if (path.endsWith("_terracotta")) {
            return "stained_hardened_clay";
        }

        if (path.endsWith("_bed")) {
            return "bed";
        }

        return switch (path) {
            case "short_grass" -> "tallgrass";
            case "lily_pad" -> "waterlily";
            case "cobweb" -> "web";
            default -> path;
        };
    }

    public boolean matches(String query) {
        if (query != null && !query.isBlank()) {
            String lowerQuery = query.toLowerCase(Locale.ROOT);
            return this.getItemId().equalsIgnoreCase(lowerQuery)
                    || this.getItemId()
                            .substring(this.getItemId().indexOf(58) + 1)
                            .equalsIgnoreCase(lowerQuery)
                    || this.getLegacyId().equalsIgnoreCase(lowerQuery);
        } else {
            return false;
        }
    }

    public int getCount() {
        return this.stack.getCount();
    }

    public int getDamage() {
        return this.stack.getDamageValue();
    }

    public int getMaxDamage() {
        return this.stack.getMaxDamage();
    }

    public int getDurability() {
        return Math.max(0, this.stack.getMaxDamage() - this.stack.getDamageValue());
    }

    public int getMaxStackSize() {
        return this.stack.getMaxStackSize();
    }

    public boolean isDamageable() {
        return this.stack.isDamageableItem();
    }

    public boolean isDamaged() {
        return this.stack.isDamaged();
    }

    public boolean isStackable() {
        return this.stack.isStackable();
    }

    public boolean isBlock() {
        return !this.stack.isEmpty() && this.stack.getItem() instanceof BlockItem;
    }

    public boolean isEnchanted() {
        return this.stack.isEnchanted();
    }

    public double getArmorPoints() {
        return this.getAttributeValue(Attributes.ARMOR);
    }

    public double getArmorToughness() {
        return this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    public double getAttackDamage() {
        return this.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public String getEquipmentSlot() {
        Equippable equippable = this.stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot().getName() : "";
    }

    public boolean isFood() {
        return this.stack.has(DataComponents.FOOD);
    }

    public Map<String, Integer> getEnchantments() {
        LinkedHashMap result = new LinkedHashMap();
        this.stack
                .getEnchantments()
                .entrySet()
                .forEach(
                        entry ->
                                result.put(
                                        entry.getKey()
                                                .unwrapKey()
                                                .map(key -> key.identifier().toString())
                                                .orElse("unknown"),
                                        entry.getIntValue()));
        return Map.copyOf(result);
    }

    private double getAttributeValue(Holder<Attribute> attribute) {
        if (this.stack.isEmpty()) {
            return 0.0;
        }

        ItemAttributeModifiers itemAttributeModifiers =
                this.stack.getOrDefault(
                        DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        EquipmentSlot equipmentSlot =
                this.stack.get(DataComponents.EQUIPPABLE) != null
                        ? this.stack.get(DataComponents.EQUIPPABLE).slot()
                        : EquipmentSlot.MAINHAND;
        return itemAttributeModifiers.compute(attribute, 0.0, equipmentSlot);
    }
}
