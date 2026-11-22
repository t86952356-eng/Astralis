package astralis.util;

import astralis.Astralis;
import astralis.mixin.MouseHandlerAccessor;
import astralis.module.impl.AntiBot;
import astralis.module.impl.PartySettings;
import astralis.util.player.TeamUtil;
import astralis.util.target.TargetSortOrder;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public class BreakUtil implements InstanceAccess {

    /** Returns the local player's profile name, or "Unknown" when no player exists. */
    public static String getLocalPlayerName() {
        return mc.player == null ? "Unknown" : mc.player.getGameProfile().name();
    }

    /** Returns the local player's vertical velocity. */
    public static double getMotionY() {
        return mc.player.getDeltaMovement().y;
    }

    /** Replaces vertical velocity while preserving both horizontal components. */
    public static void setMotionY(double velocityY) {
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x, velocityY, mc.player.getDeltaMovement().z);
    }

    /** Returns the local player's X velocity. */
    public static double getMotionX() {
        return mc.player.getDeltaMovement().x;
    }

    /** Replaces X velocity while preserving the Y and Z components. */
    public static void setMotionX(double velocityX) {
        mc.player.setDeltaMovement(
                velocityX, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
    }

    /** Returns the local player's Z velocity. */
    public static double getMotionZ() {
        return mc.player.getDeltaMovement().z;
    }

    /** Replaces Z velocity while preserving the X and Y components. */
    public static void setMotionZ(double velocityZ) {
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, velocityZ);
    }

    /**
     * Measures the vertical gap to the first non-air block below the player's block position.
     * Returns {@link Double#MAX_VALUE} when no block, player, or level is available.
     */
    public static double getGroundDistance() {
        if (mc.player != null && mc.level != null) {
            MutableBlockPos probePosition = mc.player.blockPosition().mutable();

            while (probePosition.getY() > mc.level.getMinY()) {
                probePosition.move(0, -1, 0);
                if (!mc.level.getBlockState(probePosition).isAir()) {
                    return Math.max(0.0, mc.player.getY() - (probePosition.getY() + 1));
                }
            }

            return Double.MAX_VALUE;
        } else {
            return Double.MAX_VALUE;
        }
    }

    /** Estimates break duration using the supplied tool in the player's selected inventory slot. */
    public static float getBreakTicks(BlockPos blockPosition, ItemStack tool) {
        ItemStack previousHeldItem = mc.player.getMainHandItem();
        mc.player
                .getInventory()
                .getNonEquipmentItems()
                .set(mc.player.getInventory().getSelectedSlot(), tool);
        BlockState blockState = mc.level.getBlockState(blockPosition);
        float breakTicks =
                (float)
                        Math.ceil(
                                1.0F
                                        / blockState.getDestroyProgress(
                                                mc.player, mc.level, blockPosition));
        mc.player
                .getInventory()
                .getNonEquipmentItems()
                .set(mc.player.getInventory().getSelectedSlot(), previousHeldItem);
        return breakTicks;
    }

    /** Checks active use of an item with a food component. */
    public static boolean isUsingItem() {
        return !mc.player.isUsingItem()
                ? false
                : mc.player.getItemInHand(mc.player.getUsedItemHand()).has(DataComponents.FOOD);
    }

    /** Returns cooldown ticks remaining before the attack-strength calculation reaches 90 percent. */
    public static float getRemainingCooldownTicks() {
        float attackCooldownTicks = mc.player.getCurrentItemAttackStrengthDelay();
        if (attackCooldownTicks <= 0.0F) {
            return 0.0F;
        }

        float elapsedCooldownTicks = mc.player.getAttackStrengthScale(0.0F) * attackCooldownTicks;
        return Math.max(attackCooldownTicks * 0.9F - 0.5F - elapsedCooldownTicks, 0.0F);
    }

    /** Computes the clamped attack-damage multiplier for the supplied elapsed cooldown ticks. */
    public static float getCooldownDamageScale(float elapsedAttackTicks) {
        float attackCooldownTicks = mc.player.getCurrentItemAttackStrengthDelay();
        if (attackCooldownTicks <= 0.0F) {
            return 1.0F;
        }

        float cooldownFraction = (elapsedAttackTicks + 0.5F) / attackCooldownTicks;
        return Math.clamp(0.2F + cooldownFraction * cooldownFraction * 0.8F, 0.0F, 1.0F);
    }

    /** Applies the entity filters and sort order, or returns null without a local world. */
    public static List<LivingEntity> getTargets(
            boolean includeAnimalsAndVillagers,
            boolean includeMobs,
            boolean includeTeammates,
            boolean includeInvisiblePlayers,
            boolean allowThroughWalls,
            float searchRadius,
            TargetSortOrder sortOrder,
            boolean respectPartyProtection) {
        return mc.player != null && mc.level != null
                ? mc
                        .level
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                                mc.player.getX(),
                                                mc.player.getY(),
                                                mc.player.getZ(),
                                                mc.player.getX(),
                                                mc.player.getY(),
                                                mc.player.getZ())
                                        .inflate(searchRadius),
                                candidate -> {
                                    if (candidate == mc.player
                                            || candidate.isDeadOrDying()
                                            || candidate.deathTime > 0) {
                                        return false;
                                    }

                                    if (AntiBot.isBot(candidate)) {
                                        return false;
                                    }

                                    if (candidate instanceof ArmorStand
                                            || candidate instanceof Bat) {
                                        return false;
                                    }

                                    if (!(candidate instanceof Animal)
                                            && !(candidate instanceof Villager)) {
                                        LivingEntity classifiedEntity = candidate;
                                        byte matchStartIndex = 0;
                                        switch (typeSwitch0(classifiedEntity, matchStartIndex)) {
                                            case 0:
                                                AbstractGolem golem =
                                                        (AbstractGolem) classifiedEntity;
                                                if (!includeMobs) {
                                                    return false;
                                                }

                                                return includeTeammates
                                                        || !TeamUtil.hasTeamColor(candidate);
                                            case 1:
                                                Mob mob = (Mob) classifiedEntity;
                                                if (!includeMobs) {
                                                    return false;
                                                }

                                                if (!(candidate instanceof Silverfish)) {
                                                    return true;
                                                }

                                                return includeTeammates
                                                        || !TeamUtil.hasTeamColor(candidate);
                                            case 2:
                                                Player player = (Player) classifiedEntity;
                                                PartySettings partySettings =
                                                        Astralis.getInstance()
                                                                .getModuleManager()
                                                                .getModule(PartySettings.class);
                                                if (respectPartyProtection
                                                        && partySettings
                                                                .getProtectPartyMembers()
                                                                .getValue()
                                                        && Astralis.getInstance()
                                                                .getPartyManager()
                                                                .hasMemberWithUuid(
                                                                        player.getUUID())) {
                                                    return false;
                                                }

                                                if (player.isInvisible()
                                                        && !includeInvisiblePlayers) {
                                                    return false;
                                                }

                                                if (!includeTeammates
                                                        && TeamUtil.isTeammate(player)) {
                                                    return false;
                                                }

                                                if (" ".equals(player.getScoreboardName())) {
                                                    return false;
                                                }
                                        }

                                        return mc.player.hasLineOfSight(candidate)
                                                || allowThroughWalls;
                                    } else {
                                        return includeAnimalsAndVillagers;
                                    }
                                })
                        .stream()
                        .sorted(
                                (firstTarget, secondTarget) ->
                                        switch (sortOrder) {
                                            case HEALTH ->
                                                    Float.compare(
                                                            firstTarget.getHealth(),
                                                            secondTarget.getHealth());
                                            case DISTANCE ->
                                                    Double.compare(
                                                            RotationUtil.distanceToBox(firstTarget),
                                                            RotationUtil.distanceToBox(
                                                                    secondTarget));
                                            case HURT_TIME ->
                                                    Integer.compare(
                                                            firstTarget.hurtTime,
                                                            secondTarget.hurtTime);
                                            case ANGLE ->
                                                    Float.compare(
                                                            RotationUtil.getAngleToEntity(
                                                                    firstTarget, 0.0F),
                                                            RotationUtil.getAngleToEntity(
                                                                    secondTarget, 0.0F));
                                        })
                        .toList()
                : null;
    }

    /** Checks whether the supplied player attacked the local player during the current tick. */
    public static boolean hasAttackedThisTick(Player attacker) {
        return mc.player.getLastAttacker() == attacker
                && mc.player.getLastHurtByMobTimestamp() == mc.player.tickCount;
    }

    /** Sends a left-button press and release through Minecraft's mouse handler. */
    public static void clickLeftMouse() {
        long windowHandle = mc.getWindow().handle();
        MouseButtonInfo buttonInfo = new MouseButtonInfo(0, 0);
        MouseHandlerAccessor mouseHandler = (MouseHandlerAccessor) mc.mouseHandler;
        mouseHandler.invokeOnButton(windowHandle, buttonInfo, 1);
        mouseHandler.invokeOnButton(windowHandle, buttonInfo, 0);
    }

    /** Sends a right-button press and release through Minecraft's mouse handler. */
    public static void clickRightMouse() {
        long windowHandle = mc.getWindow().handle();
        MouseButtonInfo buttonInfo = new MouseButtonInfo(1, 0);
        MouseHandlerAccessor mouseHandler = (MouseHandlerAccessor) mc.mouseHandler;
        mouseHandler.invokeOnButton(windowHandle, buttonInfo, 1);
        mouseHandler.invokeOnButton(windowHandle, buttonInfo, 0);
    }

    private static int typeSwitch0(LivingEntity entity, int startIndex) {
        Objects.checkIndex(startIndex, 4);
        if (entity == null) {
            return -1;
        } else if (startIndex <= 0 && entity instanceof AbstractGolem) {
            return 0;
        } else if (startIndex <= 1 && entity instanceof Mob) {
            return 1;
        } else {
            return startIndex <= 2 && entity instanceof Player ? 2 : 3;
        }
    }
}
