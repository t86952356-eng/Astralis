package astralis.util.inventory;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ItemUtil implements InstanceAccess {
    private static final float HEALING_EFFECT_WEIGHT = 2.5F;
    private static final float HEALTH_EFFECT_BASE_VALUE = 4.0F;

    public static ToolType getToolType(ItemStack itemStack) {
        Item item2 = itemStack.getItem();
        Tool tool = itemStack.get(DataComponents.TOOL);
        Tool tool2 = tool;
        if (tool == null) {
            return null;
        }

        tool2 = tool;

        Object rulesStream = tool2.rules().stream();
        if (((java.util.stream.Stream<Tool.Rule>) rulesStream)
                .noneMatch(
                        rule -> {
                            boolean correctForDrops = rule.correctForDrops().isPresent();
                            if (!correctForDrops) {
                                return false;
                            }

                            correctForDrops = rule.correctForDrops().get();

                            return correctForDrops;
                        })) {
            return null;
        }

        rulesStream = itemStack.get(DataComponents.WEAPON);

        Weapon weapon = (Weapon) rulesStream;
        int isMatch = item2 instanceof AxeItem ? 1 : 0;
        if (isMatch != 0) {
            return ToolType.Axe;
        }

        isMatch = item2 instanceof HoeItem ? 1 : 0;

        if (isMatch != 0) {
            return ToolType.Hoe;
        }

        isMatch = item2 instanceof ShovelItem ? 1 : 0;

        if (isMatch != 0) {
            return ToolType.Shovel;
        }

        isMatch = item2 instanceof ShearsItem ? 1 : 0;

        if (isMatch != 0) {
            return ToolType.Shears;
        }

        Weapon weapon2 = weapon;
        if (weapon == null) {
            return null;
        }

        weapon2 = weapon;

        int damagePerAttack = weapon2.itemDamagePerAttack();
        isMatch = damagePerAttack;
        byte expectedDamage;
        if (damagePerAttack == 2) {
            return ToolType.Pickaxe;
        }

        isMatch = damagePerAttack;
        expectedDamage = 1;

        return isMatch == expectedDamage ? ToolType.Sword : null;
    }

    public static int getArmorPoints(ItemStack itemStack) {
        int armorPoints = 0;
        Iterator modifierIterator =
                itemStack
                        .getOrDefault(
                                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                        .modifiers()
                        .iterator();

        while (modifierIterator.hasNext()) {
            Entry entry = (Entry) modifierIterator.next();
            Entry checkedEntry = entry;
            if (entry.attribute() != Attributes.ARMOR) {
                continue;
            }

            checkedEntry = entry;

            if (checkedEntry.modifier().operation() == Operation.ADD_VALUE) {
                armorPoints += (int) entry.modifier().amount();
                break;
            }
        }

        return armorPoints;
    }

    public static int getEnchantmentLevel(
            ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments itemEnchantments = itemStack.getEnchantments();
        Iterator enchantmentIterator = itemEnchantments.entrySet().iterator();
        int result;

        while (true) {
            if (enchantmentIterator.hasNext()) {
                it.unimi.dsi.fastutil.objects.Object2IntMap.Entry entry =
                        (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry)
                                enchantmentIterator.next();
                result = ((Holder) entry.getKey()).is(enchantment) ? 1 : 0;
                if (result != 0) {
                    return entry.getIntValue();
                }

                continue;
            }

            result = 0;
            break;
        }

        return result;
    }

    public static float getBaseAttributeValue(ItemStack itemStack, Holder<Attribute> attribute) {
        ItemAttributeModifiers itemAttributeModifiers =
                itemStack.getOrDefault(
                        DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return (float)
                itemAttributeModifiers.modifiers().stream()
                        .filter(entry -> entry.attribute().is(attribute))
                        .mapToDouble(entry -> entry.modifier().amount())
                        .sum();
    }

    public static float getArmorScore(ItemStack itemStack) {
        ItemStack itemStack2 = itemStack;
        if (!isArmor(itemStack)) {
            return 0.0F;
        }

        itemStack2 = itemStack;

        float score = getBaseAttributeValue(itemStack2, Attributes.ARMOR);
        int protectionLevel = getEnchantmentLevel(itemStack, Enchantments.PROTECTION);
        score *= protectionLevel + 1;
        score += getEnchantmentLevel(itemStack, Enchantments.THORNS);
        score += getEnchantmentLevel(itemStack, Enchantments.UNBREAKING) * 0.5F;
        score += getEnchantmentLevel(itemStack, Enchantments.PROJECTILE_PROTECTION) * 0.25F;
        float damageRatio = (float) itemStack.getDamageValue() / itemStack.getMaxDamage();
        return score - damageRatio * 0.1F;
    }

    public static float getMiningSpeedScore(ItemStack itemStack) {
        float speed =
                Optional.ofNullable(itemStack.get(DataComponents.TOOL))
                        .map(
                                tool ->
                                        tool.rules().stream()
                                                .filter(
                                                        rule -> {
                                                            boolean correctForDrops =
                                                                    rule.correctForDrops()
                                                                            .isPresent();
                                                            if (!correctForDrops) {
                                                                return false;
                                                            }

                                                            correctForDrops =
                                                                    rule.correctForDrops().get();

                                                            if (!correctForDrops) {
                                                                return false;
                                                            }

                                                            correctForDrops =
                                                                    rule.speed().isPresent();

                                                            return correctForDrops;
                                                        })
                                                .map(rule2 -> rule2.speed().get())
                                                .findAny()
                                                .orElse(0.0F))
                        .orElse(0.0F);
        float result = speed;
        if (speed > 1.0F) {
            Optional maxEfficiency =
                    itemStack.getEnchantments().entrySet().stream()
                            .filter(entry -> entry.getKey().is(Enchantments.EFFICIENCY))
                            .map(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue)
                            .max(Comparator.comparingInt(level -> level));
            if (maxEfficiency.isPresent()) {
                speed += Mth.square((Integer) maxEfficiency.get()) + 1;
            }
        }

        result = speed;

        return result;
    }

    public static double getAttackDamageScore(ItemStack itemStack) {
        float baseDamage = 0.0F;

        for (Entry entry :
                itemStack
                        .getOrDefault(
                                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                        .modifiers()) {
            Entry checkedEntry = entry;
            if (entry.attribute() != Attributes.ATTACK_DAMAGE) {
                continue;
            }

            checkedEntry = entry;

            if (checkedEntry.slot() != EquipmentSlotGroup.MAINHAND) {
                continue;
            }

            checkedEntry = entry;

            AttributeModifier attributeModifier = checkedEntry.modifier();
            if (attributeModifier.id() != Item.BASE_ATTACK_DAMAGE_ID) {
                continue;
            }

            attributeModifier = entry.modifier();

            if (attributeModifier.operation() != Operation.ADD_VALUE) {
                continue;
            }

            attributeModifier = entry.modifier();

            baseDamage = (float) attributeModifier.amount();
            break;
        }

        float sharpnessBonus = 0.0F;

        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry enchantmentEntry :
                itemStack.getEnchantments().entrySet()) {
            int sharpnessLevel =
                    ((Holder) enchantmentEntry.getKey()).is(Enchantments.SHARPNESS) ? 1 : 0;
            if (sharpnessLevel == 0) {
                continue;
            }

            sharpnessLevel = enchantmentEntry.getIntValue();

            sharpnessBonus = sharpnessLevel * 1.25F;
            break;
        }

        return baseDamage + sharpnessBonus;
    }

    public static void clickContainerSlot(int slotId, int button, ContainerInput containerInput) {
        Minecraft minecraft2 = mc;
        if (mc.gameMode == null) {
            return;
        }

        minecraft2 = mc;

        LocalPlayer localPlayer = minecraft2.player;
        if (minecraft2.player == null) {
            return;
        }

        localPlayer = mc.player;

        int stateId = localPlayer.containerMenu.getStateId();
        mc.gameMode.handleContainerInput(
                mc.player.containerMenu.containerId, slotId, button, containerInput, mc.player);
    }

    public static void swapSlots(int firstSlot, int secondSlot) {
        Minecraft minecraft2 = mc;
        if (mc.gameMode == null) {
            return;
        }

        minecraft2 = mc;

        if (minecraft2.player != null) {
            int hotbarButton;
            int containerSlot;
            int slotValue;
            int threshold;
            label70:
            {
                hotbarButton = -1;
                containerSlot = -1;
                slotValue = secondSlot;
                threshold = 36;
                if (secondSlot >= threshold) {
                    slotValue = secondSlot;
                    threshold = 44;
                    if (secondSlot <= threshold) {
                        hotbarButton = secondSlot - 36;
                        containerSlot = firstSlot;
                        break label70;
                    }
                }

                slotValue = firstSlot;
                threshold = 36;

                if (slotValue >= threshold) {
                    slotValue = firstSlot;
                    threshold = 44;
                    if (firstSlot <= threshold) {
                        hotbarButton = firstSlot - 36;
                        containerSlot = secondSlot;
                    }
                }
            }

            slotValue = hotbarButton;
            threshold = -1;

            if (slotValue != threshold) {
                clickContainerSlot(containerSlot, hotbarButton, ContainerInput.SWAP);
                return;
            }

            clickContainerSlot(firstSlot, 0, ContainerInput.PICKUP);
            clickContainerSlot(secondSlot, 0, ContainerInput.PICKUP);
            slotValue = firstSlot;
            threshold = 0;

            clickContainerSlot(slotValue, threshold, ContainerInput.PICKUP);
        }
    }

    public static boolean isArmor(ItemStack itemStack) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        Equippable equippable2 = equippable;
        if (equippable == null) {
            return false;
        }

        equippable2 = equippable;

        return equippable2.slot().getType() == Type.HUMANOID_ARMOR;
    }

    public static float getFoodScore(ItemStack itemStack) {
        float score = 0.0F;
        FoodProperties foodProperties = itemStack.get(DataComponents.FOOD);
        Object component = foodProperties;
        if (foodProperties == null) {
            return 0.0F;
        }

        score += foodProperties.nutrition();
        score += foodProperties.nutrition() * foodProperties.saturation() * 2.0F;
        component = foodProperties;

        if (((FoodProperties) component).canAlwaysEat()) {
            score *= 4.0F;
        }

        component = itemStack.get(DataComponents.CONSUMABLE);

        Consumable consumable = (Consumable) component;
        Consumable consumable2 = consumable;
        if (consumable == null) {
            return score;
        }

        consumable2 = consumable;

        Iterator consumeEffectIterator = consumable2.onConsumeEffects().iterator();

        label80:
        while (consumeEffectIterator.hasNext()) {
            ConsumeEffect consumeEffect = (ConsumeEffect) consumeEffectIterator.next();
            ConsumeEffect consumeEffect2 = consumeEffect;
            if (!(consumeEffect instanceof ApplyStatusEffectsConsumeEffect)) {
                continue;
            }

            consumeEffect2 = consumeEffect;

            ApplyStatusEffectsConsumeEffect applyStatusEffectsConsumeEffect =
                    (ApplyStatusEffectsConsumeEffect) consumeEffect2;
            ApplyStatusEffectsConsumeEffect applyStatusEffectsConsumeEffect2 =
                    applyStatusEffectsConsumeEffect;
            List<MobEffectInstance> statusEffects;
            float rawProbability;

            try {
                statusEffects = applyStatusEffectsConsumeEffect2.effects();
            } catch (Throwable e) {
                throw new MatchException(e.toString(), e);
            }

            List effects = statusEffects;
            List appliedEffects = effects;
            ApplyStatusEffectsConsumeEffect applyStatusEffectsConsumeEffect3 =
                    applyStatusEffectsConsumeEffect;

            try {
                rawProbability = applyStatusEffectsConsumeEffect3.probability();
            } catch (Throwable e) {
                throw new MatchException(e.toString(), e);
            }

            float probabilityCopy = rawProbability;
            float probability = probabilityCopy;
            Iterator effectIterator = appliedEffects.iterator();

            while (true) {
                if (!effectIterator.hasNext()) {
                    continue label80;
                }

                MobEffectInstance mobEffectInstance = (MobEffectInstance) effectIterator.next();
                float effectScore = getEffectScore(mobEffectInstance);
                boolean healthEffect = isHealthEffect(mobEffectInstance.getEffect().value());

                if (healthEffect) {
                    effectScore *= 2.5F;
                }

                score += effectScore * probability;
            }
        }

        return score;
    }

    private static boolean isHealthEffect(MobEffect mobEffect) {
        MobEffect mobEffect2 = mobEffect;
        Object knownEffect = MobEffects.REGENERATION.value();
        if (mobEffect == knownEffect) {
            return true;
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.ABSORPTION.value();

        if (mobEffect2 == knownEffect) {
            return true;
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.INSTANT_HEALTH.value();

        if (mobEffect2 == knownEffect) {
            return true;
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.HEALTH_BOOST.value();

        return mobEffect2 == knownEffect;
    }

    private static float getEffectScore(MobEffectInstance mobEffectInstance) {
        MobEffect mobEffect = mobEffectInstance.getEffect().value();
        int duration = mobEffectInstance.getDuration();
        int amplifier = mobEffectInstance.getAmplifier();
        MobEffect mobEffect2 = mobEffect;
        Object knownEffect = MobEffects.REGENERATION.value();
        if (mobEffect == knownEffect) {
            return duration * (amplifier + 1) / 50.0F;
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.ABSORPTION.value();

        if (mobEffect2 == knownEffect) {
            return 4.0F * (amplifier + 1);
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.HEALTH_BOOST.value();

        if (mobEffect2 == knownEffect) {
            return 4.0F * (amplifier + 1);
        }

        mobEffect2 = mobEffect;
        knownEffect = MobEffects.INSTANT_HEALTH.value();

        if (mobEffect2 == knownEffect) {
            return 4.0F * (float) Math.pow(2.0, amplifier);
        }

        mobEffect2 = mobEffect;

        int durationTicks = mobEffect2.isBeneficial() ? 1 : 0;
        if (durationTicks == 0) {
            return 0.0F;
        }

        durationTicks = duration;

        return durationTicks / 600.0F;
    }
}
