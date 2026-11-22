package com.astralis.scripting.api.entity;

import astralis.util.InventoryUtil;

import com.astralis.scripting.Script;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScriptPlayer extends ScriptEntity {
    private final Player player;

    public ScriptPlayer(Object playerObject) {
        this(playerObject, null);
    }

    public ScriptPlayer(Object playerObject, Script owner) {
        super(playerObject, owner);
        if (playerObject instanceof Player player2) {
            this.player = player2;
        } else {
            throw new IllegalArgumentException("Expected a player.");
        }
    }

    public int getFood() {
        return this.player.getFoodData().getFoodLevel();
    }

    public int getHunger() {
        return this.getFood();
    }

    public float getSaturation() {
        return this.player.getFoodData().getSaturationLevel();
    }

    public boolean isSneaking() {
        return this.player.isShiftKeyDown();
    }

    public boolean isSwimming() {
        return this.player.isSwimming();
    }

    public boolean isFlying() {
        return this.player.getAbilities().flying;
    }

    public boolean isFallFlying() {
        return this.player.isFallFlying();
    }

    public boolean isUsingItem() {
        return this.player.isUsingItem();
    }

    public boolean isBlocking() {
        return this.player.isBlocking();
    }

    public boolean isCreative() {
        return this.player.isCreative();
    }

    public boolean isSpectator() {
        return this.player.isSpectator();
    }

    @Override
    public boolean isSleeping() {
        return this.player.isSleeping();
    }

    public int getExperienceLevel() {
        return this.player.experienceLevel;
    }

    public float getExperienceProgress() {
        return this.player.experienceProgress;
    }

    public float getExperience() {
        return this.getExperienceProgress();
    }

    public int getScore() {
        return this.player.getScore();
    }

    public float getAttackCooldown() {
        return this.player.getAttackStrengthScale(0.0F);
    }

    public int getUseItemTicks() {
        return this.player.getTicksUsingItem();
    }

    @Override
    public ScriptItemStack getHeldItem() {
        return new ScriptItemStack(this.player.getMainHandItem());
    }

    public ScriptItemStack getOffhandItem() {
        return new ScriptItemStack(this.player.getOffhandItem());
    }

    public ScriptItemStack getArmorInSlot(int slot) {
        return switch (slot) {
            case 0 -> new ScriptItemStack(this.player.getItemBySlot(EquipmentSlot.FEET));
            case 1 -> new ScriptItemStack(this.player.getItemBySlot(EquipmentSlot.LEGS));
            case 2 -> new ScriptItemStack(this.player.getItemBySlot(EquipmentSlot.CHEST));
            case 3 -> new ScriptItemStack(this.player.getItemBySlot(EquipmentSlot.HEAD));
            default -> null;
        };
    }

    public ScriptItemStack getEquipment(String slotName) {
        if (slotName == null) {
            return null;
        }

        EquipmentSlot equipmentSlot =
                switch (slotName.trim().toLowerCase(Locale.ROOT)) {
                    case "head" -> EquipmentSlot.HEAD;
                    case "chest" -> EquipmentSlot.CHEST;
                    case "legs" -> EquipmentSlot.LEGS;
                    case "feet" -> EquipmentSlot.FEET;
                    case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
                    case "mainhand", "main_hand", "hand" -> EquipmentSlot.MAINHAND;
                    default -> null;
                };
        return equipmentSlot != null
                ? new ScriptItemStack(this.player.getItemBySlot(equipmentSlot))
                : null;
    }

    public ScriptItemStack getInventoryItem(int slot) {
        return slot >= 0 && slot < this.player.getInventory().getContainerSize()
                ? new ScriptItemStack(this.player.getInventory().getItem(slot))
                : null;
    }

    public List<ScriptItemStack> getInventory() {
        ArrayList items = new ArrayList(this.player.getInventory().getContainerSize());

        for (int slot = 0; slot < this.player.getInventory().getContainerSize(); slot++) {
            items.add(new ScriptItemStack(this.player.getInventory().getItem(slot)));
        }

        return List.copyOf(items);
    }

    public int getSelectedSlot() {
        return this.player.getInventory().getSelectedSlot();
    }

    @Override
    public boolean isHoldingBlock() {
        return this.player.getMainHandItem().getItem() instanceof BlockItem;
    }

    public boolean isHoldingWeapon() {
        ItemStack itemStack = this.player.getMainHandItem();
        return InventoryUtil.isWeapon(itemStack);
    }

    public void setVelocity(double x, double y, double z) {
        this.requireMutationActive();
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            this.player.setDeltaMovement(x, y, z);
        }
    }

    public void setHorizontalVelocity(double x, double z) {
        this.requireMutationActive();
        if (Double.isFinite(x) && Double.isFinite(z)) {
            this.player.setDeltaMovement(x, this.player.getDeltaMovement().y, z);
        }
    }

    public void setSprinting(boolean sprinting) {
        this.requireMutationActive();
        this.player.setSprinting(sprinting);
    }

    public void setSneaking(boolean sneaking) {
        this.requireMutationActive();
        this.player.setShiftKeyDown(sneaking);
    }

    public void setRotation(float yaw, float pitch) {
        this.requireMutationActive();
        if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
            this.player.setYRot(Mth.wrapDegrees(yaw));
            this.player.setXRot(Math.clamp(pitch, -90.0F, 90.0F));
        }
    }

    public void jump() {
        this.requireMutationActive();
        this.player.jumpFromGround();
    }
}
