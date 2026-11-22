package astralis.util;

import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PlayerMoveEvent;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;

public class MoveUtil implements InstanceAccess {
    public static int airTicks;
    public static int groundTicks;
    public static boolean lastSentOnGround;
    public static final float BASE_WALK_SPEED = 0.221F;
    public static final float HOP_SPEED = 0.520362F;
    public static final float SLOW_SPEED = 0.3F;
    public static final float SPRINT_MULTIPLIER = 1.3F;

    public static boolean isLastSentOnGround() {
        return lastSentOnGround && mc.player.onGround();
    }

    /** Reapplies the current horizontal speed in the direction of movement input. */
    public static void strafe() {
        setSpeed(getSpeed());
    }

    /** Sets horizontal motion to the supplied speed and current input direction. */
    public static void setSpeed(double speed) {
        if (isMoving()) {
            mc.player.setDeltaMovement(
                    -Math.sin(getDirectionRad()) * speed,
                    mc.player.getDeltaMovement().y,
                    Math.cos(getDirectionRad()) * speed);
        } else {
            stopHorizontalMotion();
        }
    }

    /** Writes horizontal motion into a movement event using the current input direction. */
    public static void setMoveEventSpeed(PlayerMoveEvent movementEvent, double speed) {
        if (isMoving()) {
            movementEvent.setX(-Math.sin(getDirectionRad()) * speed);
            movementEvent.setZ(Math.cos(getDirectionRad()) * speed);
        } else {
            stopHorizontalMotion();
        }
    }

    /** Stops horizontal motion while retaining the vertical component. */
    public static void stopHorizontalMotion() {
        mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
    }

    /** Predicts ticks until downward motion collides, or returns -1 within the limit. */
    public static int ticksUntilGroundCollision(double verticalVelocity, int maxTicks) {
        if (maxTicks <= 0) {
            return -1;
        }

        AABB predictedBounds = mc.player.getBoundingBox();

        for (int tick = 0; tick < maxTicks; tick++) {
            AABB nextBounds = predictedBounds.move(0.0, verticalVelocity, 0.0);
            if (verticalVelocity < 0.0 && !mc.level.noCollision(mc.player, nextBounds)) {
                return tick;
            }

            predictedBounds = nextBounds;
            verticalVelocity = (verticalVelocity - mc.player.getGravity()) * 0.98F;
        }

        return -1;
    }

    /** Selects speed for no Speed effect, Speed I, or Speed II. */
    public static float pickSpeedByEffect(float baseSpeed, float speedOne, float speedTwo) {
        float speed = baseSpeed;

        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (effect.getEffect() == MobEffects.SPEED) {
                speed =
                        switch (effect.getAmplifier()) {
                            case 0 -> speedOne;
                            case 1 -> speedTwo;
                            default -> 0.0F;
                        };
            }
        }

        if (!mc.player.hasEffect(MobEffects.SPEED)) {
            speed = baseSpeed;
        }

        return speed;
    }

    public static double getSpeed() {
        return Math.hypot(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().z);
    }

    public static float getDirectionRad() {
        return (float) Math.toRadians(getDirection(getBaseYaw()));
    }

    /** Returns movement direction in degrees using the stored player-view yaw. */
    public static float getDirectionDegrees() {
        return getDirection(getBaseYaw());
    }

    private static float getBaseYaw() {
        RotationManager rotationManager = RotationManager.getInstance();
        return rotationManager != null && rotationManager.isEnabled()
                ? rotationManager.getPlayerViewSnapshot().getYaw()
                : mc.player.getYRot();
    }

    public static float getDirection(float baseYaw) {
        return getDirection(baseYaw, true);
    }

    public static float getDirection(float baseYaw, boolean includeStrafe) {
        float directionYaw = baseYaw;
        float strafeScale = 1.0F;
        boolean forwardPressed = mc.options.keyUp.isDown();
        boolean backwardPressed = mc.options.keyDown.isDown();
        if (backwardPressed) {
            directionYaw += 180.0F;
        }

        if (backwardPressed) {
            strafeScale = -0.5F;
        } else if (forwardPressed) {
            strafeScale = 0.5F;
        }

        if (includeStrafe) {
            if (mc.options.keyLeft.isDown()) {
                directionYaw -= 90.0F * strafeScale;
            }

            if (mc.options.keyRight.isDown()) {
                directionYaw += 90.0F * strafeScale;
            }
        }

        return directionYaw;
    }

    public static boolean isMoving() {
        Vec2 movementInput = mc.player.input.getMoveVector();
        return movementInput.x != 0.0F || movementInput.y != 0.0F;
    }

    public static boolean isMovingDiagonally() {
        return Math.abs(mc.player.getDeltaMovement().x) > 0.05
                && Math.abs(mc.player.getDeltaMovement().z) > 0.05;
    }

    public static void applyMoveFix(MoveInputEvent event, float rotationYaw) {
        boolean forward = event.isForward();
        boolean backward = event.isBackward();
        boolean left = event.isLeft();
        boolean right = event.isRight();
        boolean fixedForward = forward;
        boolean fixedBackward = backward;
        boolean fixedLeft = left;
        boolean fixedRight = right;
        if (forward != backward || left != right) {
            float desiredMoveYaw = inputYawOffset(getBaseYaw(), forward, backward, left, right);
            float smallestYawDifference = Float.MAX_VALUE;

            for (int forwardInput = -1; forwardInput <= 1; forwardInput++) {
                for (int strafeInput = -1; strafeInput <= 1; strafeInput++) {
                    if (forwardInput != 0 || strafeInput != 0) {
                        boolean candidateForward = forwardInput > 0;
                        boolean candidateBackward = forwardInput < 0;
                        boolean candidateLeft = strafeInput < 0;
                        boolean candidateRight = strafeInput > 0;
                        float candidateMoveYaw =
                                inputYawOffset(
                                        rotationYaw,
                                        candidateForward,
                                        candidateBackward,
                                        candidateLeft,
                                        candidateRight);
                        float yawDifference =
                                Math.abs(Mth.wrapDegrees(desiredMoveYaw - candidateMoveYaw));
                        if (yawDifference < smallestYawDifference) {
                            smallestYawDifference = yawDifference;
                            fixedForward = candidateForward;
                            fixedBackward = candidateBackward;
                            fixedLeft = candidateLeft;
                            fixedRight = candidateRight;
                        }
                    }
                }
            }
        }

        event.setForward(fixedForward);
        event.setBackward(fixedBackward);
        event.setLeft(fixedLeft);
        event.setRight(fixedRight);
    }

    public static double predictMotionY(double verticalVelocity, int ticks) {
        if (ticks == 0) {
            return verticalVelocity;
        }

        double predictedVelocity = verticalVelocity;

        for (int tick = 0; tick < ticks; tick++) {
            predictedVelocity = (predictedVelocity - 0.08) * 0.98F;
        }

        return predictedVelocity;
    }

    private static float inputYawOffset(
            float baseYaw, boolean forward, boolean backward, boolean left, boolean right) {
        float yawOffset;
        if (forward == backward) {
            yawOffset = left ? -90.0F : 90.0F;
        } else if (forward) {
            if (left == right) {
                yawOffset = 0.0F;
            } else {
                yawOffset = left ? -45.0F : 45.0F;
            }
        } else if (left == right) {
            yawOffset = 180.0F;
        } else {
            yawOffset = left ? -135.0F : 135.0F;
        }

        return baseYaw + yawOffset;
    }

    public static boolean isInWeb(Player player) {
        return player.getInBlockState().getBlock() instanceof WebBlock;
    }

    /** Checks movement restrictions, hurt state, and both supplied delay counters. */
    public static boolean canCheckMovement(
            Player player, int firstDelayTicks, int secondDelayTicks) {
        return !player.isInWater()
                && !player.onClimbable()
                && !player.isPassenger()
                && !isInWeb(player)
                && player.hurtTime == 0
                && firstDelayTicks == 0
                && secondDelayTicks == 0;
    }

    public static double getBps() {
        double blocksPerSecond =
                Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo) * 20.0;
        return Math.round(blocksPerSecond * 100.0) / 100.0;
    }

    public static float getMoveYaw() {
        return getDirection(getBaseYaw());
    }

    /** Returns the movement speed estimate with fluid, sneak, sprint and effect modifiers. */
    public static double getBaseMoveSpeed() {
        boolean applyMovementModifiers = false;
        double baseSpeed;
        if (mc.player.isInWater() || mc.player.isInLava()) {
            baseSpeed = 0.11500001F;
        } else if (mc.player.isShiftKeyDown()) {
            baseSpeed = 0.066300005F;
        } else {
            baseSpeed = 0.221F;
            applyMovementModifiers = true;
        }

        if (applyMovementModifiers) {
            if (mc.player.isSprinting()) {
                baseSpeed *= 1.3F;
            }

            if (mc.player.hasEffect(MobEffects.SPEED)) {
                baseSpeed *= 1.0 + 0.2 * (mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1);
            }

            if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
                baseSpeed = 0.29F;
            }
        }

        return baseSpeed;
    }
}
