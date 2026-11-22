package com.astralis.scripting.api.entity;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.math.ScriptBoundingBox;
import com.astralis.scripting.api.math.ScriptVector3;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;

import java.util.List;
import java.util.Objects;

public class ScriptEntity {
    private final Entity entity;
    private final Script owner;

    public ScriptEntity(Object entityObject) {
        this(entityObject, null);
    }

    public ScriptEntity(Object entityObject, Script owner) {
        if (entityObject instanceof Entity entity2) {
            this.entity = entity2;
            this.owner = owner;
        } else {
            throw new IllegalArgumentException("Expected an entity.");
        }
    }

    public int getId() {
        return this.entity.getId();
    }

    public double getX() {
        return this.entity.getX();
    }

    public double getY() {
        return this.entity.getY();
    }

    public double getZ() {
        return this.entity.getZ();
    }

    public ScriptVector3 getPosition() {
        return new ScriptVector3(this.getX(), this.getY(), this.getZ());
    }

    public double getPreviousX() {
        return this.entity.xo;
    }

    public double getPreviousY() {
        return this.entity.yo;
    }

    public double getPreviousZ() {
        return this.entity.zo;
    }

    public ScriptVector3 getPreviousPosition() {
        return new ScriptVector3(this.getPreviousX(), this.getPreviousY(), this.getPreviousZ());
    }

    public int getBlockX() {
        return this.entity.getBlockX();
    }

    public int getBlockY() {
        return this.entity.getBlockY();
    }

    public int getBlockZ() {
        return this.entity.getBlockZ();
    }

    public float getYaw() {
        return this.entity.getYRot();
    }

    public float getPitch() {
        return this.entity.getXRot();
    }

    public float getHeadYaw() {
        return this.entity.getYHeadRot();
    }

    public float getPreviousYaw() {
        return this.entity.yRotO;
    }

    public float getPreviousPitch() {
        return this.entity.xRotO;
    }

    public double getLookX() {
        return this.entity.getLookAngle().x;
    }

    public double getLookY() {
        return this.entity.getLookAngle().y;
    }

    public double getLookZ() {
        return this.entity.getLookAngle().z;
    }

    public ScriptVector3 getLookVector() {
        return new ScriptVector3(this.getLookX(), this.getLookY(), this.getLookZ());
    }

    public double getVelocityX() {
        return this.entity.getDeltaMovement().x;
    }

    public double getVelocityY() {
        return this.entity.getDeltaMovement().y;
    }

    public double getVelocityZ() {
        return this.entity.getDeltaMovement().z;
    }

    public ScriptVector3 getVelocity() {
        return new ScriptVector3(this.getVelocityX(), this.getVelocityY(), this.getVelocityZ());
    }

    public double getSpeed() {
        return this.entity.getDeltaMovement().length();
    }

    public double getHorizontalSpeed() {
        return this.entity.getDeltaMovement().horizontalDistance();
    }

    public float getWidth() {
        return this.entity.getBbWidth();
    }

    public float getHeight() {
        return this.entity.getBbHeight();
    }

    public float getEyeHeight() {
        return this.entity.getEyeHeight();
    }

    public double getFallDistance() {
        return this.entity.fallDistance;
    }

    public int getTicksExisted() {
        return this.entity.tickCount;
    }

    public int getAir() {
        return this.entity.getAirSupply();
    }

    public double[] getBoundingBox() {
        return new double[] {
            this.entity.getBoundingBox().minX,
            this.entity.getBoundingBox().minY,
            this.entity.getBoundingBox().minZ,
            this.entity.getBoundingBox().maxX,
            this.entity.getBoundingBox().maxY,
            this.entity.getBoundingBox().maxZ
        };
    }

    public ScriptBoundingBox getBounds() {
        return new ScriptBoundingBox(
                this.entity.getBoundingBox().minX,
                this.entity.getBoundingBox().minY,
                this.entity.getBoundingBox().minZ,
                this.entity.getBoundingBox().maxX,
                this.entity.getBoundingBox().maxY,
                this.entity.getBoundingBox().maxZ);
    }

    public String getName() {
        return this.entity.getName().getString();
    }

    public String getDisplayName() {
        return this.entity.getDisplayName().getString();
    }

    public String getUuid() {
        return this.entity.getStringUUID();
    }

    public String getType() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType()).toString();
    }

    public boolean isPlayer() {
        return this.entity instanceof Player;
    }

    public boolean isSleeping() {
        return this.entity instanceof Player player2 && player2.isSleeping();
    }

    public ScriptItemStack getHeldItem() {
        return this.entity instanceof Player player2
                ? new ScriptItemStack(player2.getMainHandItem())
                : null;
    }

    public boolean isHoldingBlock() {
        return this.entity instanceof Player player2
                && player2.getMainHandItem().getItem() instanceof BlockItem;
    }

    public boolean isLiving() {
        return this.entity instanceof LivingEntity;
    }

    public boolean isAlive() {
        return this.entity.isAlive();
    }

    public boolean isDead() {
        return !this.entity.isAlive();
    }

    public boolean isRemoved() {
        return this.entity.isRemoved();
    }

    public boolean isCollided() {
        return this.entity.horizontalCollision || this.entity.verticalCollision;
    }

    public boolean isCollidedHorizontally() {
        return this.entity.horizontalCollision;
    }

    public boolean isCollidedVertically() {
        return this.entity.verticalCollision;
    }

    public boolean isOnGround() {
        return this.entity.onGround();
    }

    public boolean isInWater() {
        return this.entity.isInWater();
    }

    public boolean isInLava() {
        return this.entity.isInLava();
    }

    public boolean isOnFire() {
        return this.entity.isOnFire();
    }

    public boolean isInvisible() {
        return this.entity.isInvisible();
    }

    public boolean isCrouching() {
        return this.entity.isCrouching();
    }

    public boolean isSprinting() {
        return this.entity.isSprinting();
    }

    public boolean isPassenger() {
        return this.entity.isPassenger();
    }

    public boolean isVehicle() {
        return this.entity.isVehicle();
    }

    public ScriptEntity getRidingEntity() {
        return this.entity.getVehicle() != null
                ? new ScriptEntity(this.entity.getVehicle(), this.owner)
                : null;
    }

    public ScriptEntity getRiddenByEntity() {
        return this.entity.getFirstPassenger() != null
                ? new ScriptEntity(this.entity.getFirstPassenger(), this.owner)
                : null;
    }

    public boolean hasNoGravity() {
        return this.entity.isNoGravity();
    }

    public float getHealth() {
        return this.entity instanceof LivingEntity livingEntity ? livingEntity.getHealth() : 0.0F;
    }

    public float getMaxHealth() {
        return this.entity instanceof LivingEntity livingEntity
                ? livingEntity.getMaxHealth()
                : 0.0F;
    }

    public float getAbsorption() {
        return this.entity instanceof LivingEntity livingEntity
                ? livingEntity.getAbsorptionAmount()
                : 0.0F;
    }

    public int getArmorValue() {
        return this.entity instanceof LivingEntity livingEntity ? livingEntity.getArmorValue() : 0;
    }

    public int getHurtTime() {
        return this.entity instanceof LivingEntity livingEntity ? livingEntity.hurtTime : 0;
    }

    public int getMaxHurtTime() {
        return this.entity instanceof LivingEntity livingEntity ? livingEntity.hurtDuration : 0;
    }

    public List<ScriptStatusEffect> getStatusEffects() {
        return this.entity instanceof LivingEntity livingEntity
                ? livingEntity.getActiveEffects().stream()
                        .map(
                                mobEffectInstance ->
                                        new ScriptStatusEffect(
                                                Objects.toString(
                                                        BuiltInRegistries.MOB_EFFECT.getKey(
                                                                mobEffectInstance
                                                                        .getEffect()
                                                                        .value()),
                                                        "unknown"),
                                                mobEffectInstance.getAmplifier(),
                                                mobEffectInstance.getDuration(),
                                                mobEffectInstance.isAmbient(),
                                                mobEffectInstance.isVisible()))
                        .toList()
                : List.of();
    }

    public int getSwingProgress() {
        return this.entity instanceof LivingEntity livingEntity ? livingEntity.swingTime : 0;
    }

    public double getBlocksPerSecond() {
        return this.entity.getDeltaMovement().horizontalDistance() * 20.0;
    }

    public double distanceTo(ScriptEntity other) {
        return this.entity.distanceTo(other.entity);
    }

    public double distanceTo(double x, double y, double z) {
        return Math.sqrt(this.entity.distanceToSqr(x, y, z));
    }

    public double distanceToSquared(ScriptEntity other) {
        return this.entity.distanceToSqr(other.entity);
    }

    public double distanceToSquared(double x, double y, double z) {
        return this.entity.distanceToSqr(x, y, z);
    }

    public boolean canSee(ScriptEntity other) {
        return this.entity instanceof LivingEntity livingEntity
                && livingEntity.hasLineOfSight(other.entity);
    }

    public void setPosition(double x, double y, double z) {
        this.requireMutationActive();
        if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
            this.entity.setPos(x, y, z);
        }
    }

    public void setYaw(float yaw) {
        this.requireMutationActive();
        if (Float.isFinite(yaw)) {
            this.entity.setYRot(Mth.wrapDegrees(yaw));
        }
    }

    public void setPitch(float pitch) {
        this.requireMutationActive();
        if (Float.isFinite(pitch)) {
            this.entity.setXRot(Math.clamp(pitch, -90.0F, 90.0F));
        }
    }

    protected final void requireMutationActive() {
        if (this.owner == null || !this.owner.isRuntimeActive()) {
            throw new IllegalStateException(
                    "Entity mutations require an active script-owned wrapper.");
        }
    }
}
