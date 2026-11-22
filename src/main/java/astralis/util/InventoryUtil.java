package astralis.util;

import astralis.mixin.MultiPlayerGameModeAccessor;
import astralis.module.impl.Weapon;
import astralis.util.inventory.ItemUtil;

import com.astralis.scripting.InstanceAccess;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class InventoryUtil implements InstanceAccess {

    /** Returns the best hotbar tool slot for the block at the given position. */
    public static int getBestToolSlotAt(BlockPos position) {
        return getBestToolSlot(mc.level.getBlockState(position));
    }

    public static int getBestToolSlot(BlockState blockState) {
        int bestSlot = -1;
        float bestDestroySpeed = 1.0F;
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                return bestSlot;
            }

            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                float destroySpeed = stack.getDestroySpeed(blockState);
                if (destroySpeed > 1.0F) {
                    int efficiencyLevel =
                            stack.getEnchantments().entrySet().stream()
                                    .filter(
                                            enchantment ->
                                                    enchantment
                                                            .getKey()
                                                            .is(Enchantments.EFFICIENCY))
                                    .mapToInt(Entry::getIntValue)
                                    .findFirst()
                                    .orElse(0);
                    if (efficiencyLevel > 0) {
                        destroySpeed += Mth.square(efficiencyLevel) + 1;
                    }
                }

                if (destroySpeed > bestDestroySpeed) {
                    bestDestroySpeed = destroySpeed;
                    bestSlot = slot;
                }
            }

            slot++;
        }
    }

    public static void switchToBestTool(BlockPos position) {
        int bestSlot = getBestToolSlotAt(position);
        if (bestSlot != -1 && bestSlot != mc.player.getInventory().getSelectedSlot()) {
            setSlot(bestSlot);
        }
    }

    public static void setSlot(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        ((MultiPlayerGameModeAccessor) mc.gameMode).syncSlot();
    }

    /** Returns the first matching hotbar slot, or -1 if no stack matches. */
    public static int findHotbarSlot(Predicate<ItemStack> predicate) {
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                return -1;
            }

            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (predicate.test(stack)) {
                return slot;
            }

            slot++;
        }
    }

    public static int getHotbarSlot(Item item) {
        return findHotbarSlot(stack -> stack.is(item));
    }

    /** Tests membership in the spear item tag. */
    public static boolean isSpear(ItemStack stack) {
        return stack.is(ItemTags.SPEARS);
    }

    /** Tests membership in the chest-armor item tag. */
    public static boolean isChestArmor(ItemStack stack) {
        return stack.is(ItemTags.CHEST_ARMOR);
    }

    /** Matches the resource items enabled by the four selection flags. */
    public static boolean isWantedResource(
            ItemStack stack,
            boolean includeIron,
            boolean includeGold,
            boolean includeDiamond,
            boolean includeEmerald) {
        if (stack.isEmpty()) {
            return false;
        } else if (stack.is(Items.IRON_INGOT)) {
            return includeIron;
        } else if (stack.is(Items.GOLD_INGOT)) {
            return includeGold;
        } else if (stack.is(Items.DIAMOND)) {
            return includeDiamond;
        } else {
            return stack.is(Items.EMERALD) ? includeEmerald : false;
        }
    }

    /** Checks for a byte-valued MELEE custom-data tag. */
    public static boolean hasMeleeTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.getComponents().get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getByte("MELEE").isPresent();
    }

    public static boolean isHoldingSword() {
        return mc.player.getInventory().getSelectedItem().is(ItemTags.SWORDS);
    }

    /** Tests whether the selected item is a block item other than a player head. */
    public static boolean isHoldingPlaceableBlock() {
        ItemStack stack = mc.player.getInventory().getSelectedItem();
        return stack.getItem() instanceof BlockItem && !stack.is(Items.PLAYER_HEAD);
    }

    /** Tests whether the player's main-hand item is a block item. */
    public static boolean isMainHandBlock(Player player) {
        return player.getMainHandItem().getItem() instanceof BlockItem;
    }

    public static boolean isHoldingWeapon() {
        return mc.player != null && isWeapon(mc.player.getInventory().getSelectedItem());
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Weapon weaponModule = Weapon.getInstance();
        if (weaponModule == null) {
            return isDefaultWeapon(stack);
        }

        if (stack.isEmpty()) {
            return weaponModule.getFist().getValue();
        }

        if (weaponModule.getKnockback().getValue()
                && stack.getEnchantments().entrySet().stream()
                        .anyMatch(enchantment -> enchantment.getKey().is(Enchantments.KNOCKBACK))) {
            return true;
        }

        Item item = stack.getItem();
        return weaponModule.getSwords().getValue() && stack.is(ItemTags.SWORDS)
                || weaponModule.getAxes().getValue() && item instanceof AxeItem
                || weaponModule.getMace().getValue() && item instanceof MaceItem
                || weaponModule.getTrident().getValue() && item instanceof TridentItem
                || weaponModule.getShield().getValue() && item instanceof ShieldItem;
    }

    /** Fallback classification used before the Weapon module is available. */
    private static boolean isDefaultWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        return stack.getUseAnimation() == ItemUseAnimation.BLOCK
                || item.builtInRegistryHolder().is(ItemTags.SWORDS)
                || item instanceof AxeItem
                || item instanceof MaceItem
                || item instanceof TridentItem;
    }

    /** Tests whether the selected stack has a food component. */
    public static boolean isHoldingFood() {
        FoodProperties foodProperties =
                mc.player.getInventory().getSelectedItem().get(DataComponents.FOOD);
        return foodProperties != null;
    }

    public static boolean isHoldingTool() {
        ItemStack stack = mc.player.getInventory().getSelectedItem();
        return ItemUtil.getToolType(stack) != null;
    }
}
