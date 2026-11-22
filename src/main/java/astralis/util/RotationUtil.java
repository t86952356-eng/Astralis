package astralis.util;

import astralis.metadata.MetadataState;
import astralis.rotation.AxisSwitchMap;
import astralis.rotation.RotationManager;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RotationUtil implements InstanceAccess {
    private static MetadataState numericConstants;

    public static float getGcd() {
        double sensitivity = mc.options.sensitivity().get();

        double scaledSensitivity =
                sensitivity * numericConstants.getFloatParameter("gcd_sens_scale");

        double adjustedSensitivity =
                scaledSensitivity + numericConstants.getFloatParameter("gcd_sens_base");
        double sensitivityCubed = adjustedSensitivity * adjustedSensitivity * adjustedSensitivity;

        double scaledIncrement =
                sensitivityCubed * numericConstants.getDoubleParameter("gcd_pow_mul", 0.0);

        return (float) (scaledIncrement * numericConstants.getFloatParameter("gcd_factor"));
    }

    public static float rotateTowards(float currentAngle, float targetAngle, float maxStep) {
        float angleDelta = Mth.wrapDegrees(targetAngle - currentAngle);
        if (maxStep < 180.0F) {
            if (angleDelta > maxStep) {
                angleDelta = maxStep;
            }

            if (angleDelta < -maxStep) {
                angleDelta = -maxStep;
            }
        }

        return currentAngle + angleDelta;
    }

    /** Converts an angle difference into the corresponding scaled mouse movement. */
    public static double angleToMouseDelta(double angleDelta, double mouseSensitivityScale) {

        float rotationFactor = numericConstants.getFloatParameter("gcd_factor");
        return rotationFactor == 0.0F ? 0.0 : angleDelta / mouseSensitivityScale / rotationFactor;
    }

    /** Returns the angle to the target hitbox center with optional velocity prediction. */
    public static float getAngleToEntity(Entity entity, float predictionFactor) {
        if (entity == null) {
            return 360.0F;
        }

        Vec3 eyePosition = mc.player.getEyePosition();
        Vec3 targetPoint = entity.getBoundingBox().getCenter();
        if (predictionFactor > 0.0F) {
            Vec3 targetVelocity = entity.getDeltaMovement();
            double targetDistance = eyePosition.distanceTo(targetPoint);
            double predictionTime = targetDistance / Math.max(targetVelocity.length(), 0.1);
            targetPoint = targetPoint.add(targetVelocity.scale(predictionTime * predictionFactor));
        }

        Vec3 lookDirection = mc.player.getLookAngle();
        Vec3 targetDirection = targetPoint.subtract(eyePosition).normalize();
        if (lookDirection.length() != 0.0 && targetDirection.length() != 0.0) {
            double directionDot = lookDirection.dot(targetDirection);
            double angleRadians = Math.acos(Math.max(-1.0, Math.min(1.0, directionDot)));
            return (float) Math.toDegrees(angleRadians);
        } else {
            return 360.0F;
        }
    }

    /** Checks whether a ray using the current managed rotation hits the supplied entity. */
    public static boolean rayTraceHitsEntityCurrent(
            Entity entity, float range, boolean ignoreBlocks) {
        EntityHitResult hit = rayTraceEntityCurrent(range, ignoreBlocks);
        return hit != null && hit.getType() == Type.ENTITY && hit.getEntity() == entity;
    }

    public static boolean rayTraceHitsEntity(
            Entity entity, float range, boolean ignoreBlocks, float yaw, float pitch) {
        EntityHitResult hit = rayTraceEntity(range, ignoreBlocks, yaw, pitch);
        return hit != null && hit.getType() == Type.ENTITY && hit.getEntity() == entity;
    }

    public static Vec3 getClosestPointOnHitbox(Entity entity) {
        Vec3 eyePosition = getEyePos();
        AABB bounds = entity.getBoundingBox();
        double closestX = Mth.clamp(eyePosition.x(), bounds.minX, bounds.maxX);
        double closestY = Mth.clamp(eyePosition.y(), bounds.minY, bounds.maxY);
        double closestZ = Mth.clamp(eyePosition.z(), bounds.minZ, bounds.maxZ);
        return new Vec3(closestX, closestY, closestZ);
    }

    /** Returns the selected head, chest or feet aim point clamped horizontally to the hitbox. */
    public static Vec3 getAimPoint(Entity entity, String aimPoint) {
        AABB bounds = entity.getBoundingBox();
        Vec3 eyePosition = getEyePos();
        double closestX = Mth.clamp(eyePosition.x(), bounds.minX, bounds.maxX);
        double closestZ = Mth.clamp(eyePosition.z(), bounds.minZ, bounds.maxZ);

        if (aimPoint.equals("Best")) {
            return getAimPoint(entity, chooseBestAimRegion(entity));
        }

        double targetY =
                switch (aimPoint) {
                    case "Head" -> entity.getEyeY();
                    case "Feet" -> bounds.minY;
                    default -> (bounds.minY + bounds.maxY) / 2.0;
                };
        return new Vec3(closestX, targetY, closestZ);
    }

    /** Chooses the closest head/chest/feet aim region using fixed distance thresholds. */
    public static String chooseBestAimRegion(Entity entity) {

        AABB bounds = entity.getBoundingBox();
        Vec3 eyePosition = getEyePos();
        double closestX = Mth.clamp(eyePosition.x(), bounds.minX, bounds.maxX);
        double closestZ = Mth.clamp(eyePosition.z(), bounds.minZ, bounds.maxZ);
        double headDistance =
                eyePosition.distanceTo(new Vec3(closestX, entity.getEyeY(), closestZ));
        double chestDistance =
                eyePosition.distanceTo(
                        new Vec3(closestX, (bounds.minY + bounds.maxY) / 2.0, closestZ));
        double feetDistance = eyePosition.distanceTo(new Vec3(closestX, bounds.minY, closestZ));
        if (headDistance < feetDistance && chestDistance - headDistance > 0.2) {

            return "Head";
        } else if (feetDistance < headDistance && chestDistance - feetDistance > 0.2) {

            return "Feet";
        } else {

            return "Chest";
        }
    }

    public static float[] getRotationsToEntity(Entity entity) {
        Vec3 targetPoint = getClosestPointOnHitbox(entity);
        return getRotationsTo(targetPoint);
    }

    public static float[] getRotationsTo(Vec3 targetPoint) {
        double deltaX = targetPoint.x - mc.player.getX();
        double deltaY = targetPoint.y - getEyePos().y;
        double deltaZ = targetPoint.z - mc.player.getZ();
        double horizontalDistance = Mth.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));
        double yawRadians = Math.atan2(deltaZ, deltaX);

        float targetYaw =
                (float)
                        (yawRadians * numericConstants.getDoubleParameter("rad_to_deg", 0.0)
                                - 90.0);
        double pitchRadians = Math.atan2(deltaY, horizontalDistance);

        float targetPitch =
                (float) (-(pitchRadians * numericConstants.getDoubleParameter("rad_to_deg", 0.0)));
        return new float[] {
            mc.player.getYRot() + Mth.wrapDegrees(targetYaw - mc.player.getYRot()),
            mc.player.getXRot() + Mth.wrapDegrees(targetPitch - mc.player.getXRot())
        };
    }

    public static Vec3 getEyePos() {
        return new Vec3(
                mc.player.getX(),
                mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ());
    }

    public static double distanceToBox(Entity entity) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        return eyePosition.distanceTo(closestPointOnBox(eyePosition, entity.getBoundingBox()));
    }

    public static Vec3 closestPointOnBox(Vec3 point, AABB bounds) {
        double[] coordinates = new double[] {point.x, point.y, point.z};
        double[] minimums = new double[] {bounds.minX, bounds.minY, bounds.minZ};
        double[] maximums = new double[] {bounds.maxX, bounds.maxY, bounds.maxZ};

        for (int axis = 0; axis <= 2; axis++) {
            if (coordinates[axis] > maximums[axis]) {
                coordinates[axis] = maximums[axis];
            } else if (coordinates[axis] < minimums[axis]) {
                coordinates[axis] = minimums[axis];
            }
        }

        Vec3 closestPoint = new Vec3(coordinates[0], coordinates[1], coordinates[2]);
        return closestPoint;
    }

    public static double distanceToEye(Vec3 point) {
        return point.distanceTo(getEyePos());
    }

    /** Builds a look vector from a yaw/pitch pair stored in UVPair. */
    public static Vec3 directionFromRotationPair(UVPair yawPitch) {

        float degreesToRadians = numericConstants.getFloatParameter("deg_to_rad");

        float pi = numericConstants.getFloatParameter("pi_f");
        float yawCos = (float) Math.cos(-yawPitch.u() * degreesToRadians - pi);
        float yawSin = (float) Math.sin(-yawPitch.u() * degreesToRadians - pi);
        float pitchCos = (float) (-Math.cos(-yawPitch.v() * degreesToRadians));
        float pitchSin = (float) Math.sin(-yawPitch.v() * degreesToRadians);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    /** Builds a look vector from pitch followed by yaw. */
    public static Vec3 directionFromRotation(float pitch, float yaw) {

        float degreesToRadians = numericConstants.getFloatParameter("deg_to_rad");

        float pi = numericConstants.getFloatParameter("pi_f");
        float yawCos = (float) Math.cos(-yaw * degreesToRadians - pi);
        float yawSin = (float) Math.sin(-yaw * degreesToRadians - pi);
        float pitchCos = (float) (-Math.cos(-pitch * degreesToRadians));
        float pitchSin = (float) Math.sin(-pitch * degreesToRadians);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    /** Builds a look vector from pitch followed by yaw. */
    public static Vec3 getLookVector(float pitch, float yaw) {

        float degreesToRadians = numericConstants.getFloatParameter("deg_to_rad");
        float pitchRadians = pitch * degreesToRadians;
        float negativeYawRadians = -yaw * degreesToRadians;
        float yawCos = (float) Math.cos(negativeYawRadians);
        float yawSin = (float) Math.sin(negativeYawRadians);
        float pitchCos = (float) Math.cos(pitchRadians);
        float pitchSin = (float) Math.sin(pitchRadians);
        return new Vec3(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
    }

    /** Converts a direction vector into yaw and pitch angles. */
    public static float[] getRotationsFromDirection(Vec3 direction) {
        double deltaX = direction.x;
        double deltaY = direction.y;
        double deltaZ = direction.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double yawRadians = Math.atan2(deltaZ, deltaX);

        float yaw =
                (float) (yawRadians * numericConstants.getDoubleParameter("rad_to_deg", 0.0))
                        - 90.0F;
        double pitchRadians = Math.atan2(deltaY, horizontalDistance);

        float pitch =
                (float) (-(pitchRadians * numericConstants.getDoubleParameter("rad_to_deg", 0.0)));
        return new float[] {yaw, pitch};
    }

    /** Traces blocks using the current managed rotation. */
    public static BlockHitResult rayTraceBlockCurrent(double range, boolean includeFluids) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        Vec3 lookDirection =
                directionFromRotation(
                        RotationManager.getInstance().getPitch(),
                        RotationManager.getInstance().getYaw());
        Vec3 rayEnd =
                eyePosition.add(
                        lookDirection.x * range, lookDirection.y * range, lookDirection.z * range);
        return rayTraceBetween(eyePosition, rayEnd, includeFluids, false);
    }

    /** Traces between two points with explicit fluid and block-shape options. */
    public static BlockHitResult rayTraceBetween(
            Vec3 rayStart, Vec3 rayEnd, boolean includeFluids, boolean useCollisionShape) {
        return mc.level.clip(
                new ClipContext(
                        rayStart,
                        rayEnd,
                        useCollisionShape ? Block.COLLIDER : Block.OUTLINE,
                        includeFluids ? Fluid.ANY : Fluid.NONE,
                        mc.player));
    }

    public static EntityHitResult rayTraceEntityCurrent(double range, boolean ignoreBlocks) {
        return rayTraceEntity(
                range,
                ignoreBlocks,
                RotationManager.getInstance().getYaw(),
                RotationManager.getInstance().getPitch());
    }

    public static EntityHitResult rayTraceEntity(
            double range, boolean ignoreBlocks, float yaw, float pitch) {
        Vec3 eyePosition = mc.player.getEyePosition(1.0F);
        Vec3 lookDirection = directionFromRotation(pitch, yaw);
        double effectiveRange = range;
        double effectiveRangeSquared = range * range;
        double blockDistanceSquared = Double.MAX_VALUE;
        if (!ignoreBlocks) {
            BlockHitResult blockHit =
                    rayTraceBetween(
                            eyePosition, eyePosition.add(lookDirection.scale(range)), false, false);
            blockDistanceSquared = blockHit.getLocation().distanceToSqr(eyePosition);
            if (blockHit.getType() != Type.MISS) {
                effectiveRangeSquared = blockDistanceSquared;
                effectiveRange = Math.sqrt(effectiveRangeSquared);
            }
        }

        Vec3 rayEnd = eyePosition.add(lookDirection.scale(effectiveRange));
        EntityHitResult entityHit =
                ProjectileUtil.getEntityHitResult(
                        mc.player,
                        eyePosition,
                        rayEnd,
                        mc.player
                                .getBoundingBox()
                                .expandTowards(lookDirection.scale(effectiveRange))
                                .inflate(1.0, 1.0, 1.0),
                        candidate -> {
                            return candidate != mc.player
                                    && !candidate.isSpectator()
                                    && candidate.isPickable();
                        },
                        effectiveRangeSquared);
        if (entityHit == null) {
            return null;
        } else {
            return !ignoreBlocks
                            && !(entityHit.getLocation().distanceToSqr(eyePosition)
                                    < blockDistanceSquared)
                    ? null
                    : entityHit;
        }
    }

    public static float[] getRotationsTo(double targetX, double targetY, double targetZ) {
        double deltaX = targetX - mc.player.getX();
        double deltaZ = targetZ - mc.player.getZ();
        double deltaY = targetY - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(deltaY, horizontalDistance)));
        return new float[] {yaw, pitch};
    }

    public static float[] getRotationsToFace(BlockPos position, Direction face) {
        Vec3 blockCenter =
                new Vec3(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5);
        Vec3 faceOffset = new Vec3(face.getUnitVec3i()).scale(0.25);
        Vec3 targetPoint = blockCenter.add(faceOffset);
        return getRotationsTo(targetPoint.x, targetPoint.y, targetPoint.z);
    }

    public static boolean rayTraceHitsBlockFace(
            Direction face,
            BlockPos position,
            boolean requireFace,
            float range,
            float yaw,
            float pitch) {
        BlockHitResult hit = rayTraceBlockYawPitch(yaw, pitch, range);
        if (hit.getType() != Type.BLOCK) {
            return false;
        }

        BlockPos hitPosition = hit.getBlockPos();
        return hitPosition.equals(position) && (!requireFace || hit.getDirection() == face);
    }

    public static boolean rayTraceHitsAnyBlock(float yaw, float pitch, float range) {
        return rayTraceBlockYawPitch(yaw, pitch, range).getType() == Type.BLOCK;
    }

    public static BlockHitResult rayTraceBlockYawPitch(float yaw, float pitch, float range) {
        return rayTraceBlock(range, yaw, pitch);
    }

    public static Vec3 getLookVectorMth(float yaw, float pitch) {

        float degreesToRadians = numericConstants.getFloatParameter("deg_to_rad");

        float pi = numericConstants.getFloatParameter("pi_f");
        float yawCos = Mth.cos(-yaw * degreesToRadians - pi);
        float yawSin = Mth.sin(-yaw * degreesToRadians - pi);
        float pitchCos = -Mth.cos(-pitch * degreesToRadians);
        float pitchSin = Mth.sin(-pitch * degreesToRadians);
        return new Vec3(yawSin * pitchCos, pitchSin, yawCos * pitchCos);
    }

    public static float scanPitch(
            BlockPos position, Direction face, float yaw, float fallbackPitch, int maxPitch) {
        float pitchStep = 0.1F + (float) (Math.random() / 10.0 - 0.02);

        for (float candidatePitch = 50.0F;
                candidatePitch <= maxPitch;
                candidatePitch += pitchStep) {
            if (rayTraceHitsBlockFace(face, position, true, 4.5F, yaw, candidatePitch)) {
                return candidatePitch;
            }
        }

        return fallbackPitch;
    }

    public static BlockHitResult rayTraceBlock(float range, float yaw, float pitch) {
        Vec3 eyePosition = mc.getCameraEntity().getEyePosition(1.0F);
        Vec3 lookDirection = getLookVectorMth(yaw, pitch);
        Vec3 rayEnd =
                eyePosition.add(
                        lookDirection.x * range, lookDirection.y * range, lookDirection.z * range);
        return mc.level.clip(
                new ClipContext(
                        eyePosition, rayEnd, Block.COLLIDER, Fluid.NONE, mc.getCameraEntity()));
    }

    public static PlaceRotation findClosestPlaceRotation(
            BlockPos position, Direction face, float currentYaw, float currentPitch, float range) {

        BlockHitResult currentHit = rayTraceBlock(range, currentYaw, currentPitch);
        if (isHitOnFace(currentHit, position, face)) {
            return new PlaceRotation(currentYaw, currentPitch, currentHit.getLocation());
        }

        PlaceRotation closestRotation = null;
        float smallestAngularDistance = Float.POSITIVE_INFINITY;

        int sampleIndex = 0;

        while (true) {

            if (sampleIndex >= 256) {
                return closestRotation;
            }

            Vec3 targetPoint = facePoint(position, face, sampleIndex);
            float[] candidateAngles =
                    MathUtil.getRotationsFrom(
                            targetPoint.x - mc.player.getX(),
                            targetPoint.y
                                    - mc.player.getY()
                                    - mc.player.getEyeHeight(mc.player.getPose()),
                            targetPoint.z - mc.player.getZ(),
                            currentYaw,
                            currentPitch);
            BlockHitResult candidateHit =
                    rayTraceBlock(range, candidateAngles[0], candidateAngles[1]);
            if (isHitOnFace(candidateHit, position, face)) {
                float angularDistance =
                        MathUtil.rotationDistance(
                                candidateAngles[0], candidateAngles[1], currentYaw, currentPitch);
                if (!(angularDistance >= smallestAngularDistance)) {
                    smallestAngularDistance = angularDistance;
                    closestRotation =
                            new PlaceRotation(
                                    candidateAngles[0],
                                    candidateAngles[1],
                                    candidateHit.getLocation());
                }
            }

            sampleIndex++;
        }
    }

    public static PlaceRotation bruteForcePlaceRotation(
            BlockPos position,
            Direction face,
            float baseYaw,
            boolean searchBothYawDirections,
            float preferredYawDirection) {
        int yawOffset = 0;

        while (true) {

            if (yawOffset > 180) {
                return null;
            }

            for (int yawDirection = yawOffset == 0 ? 0 : -1; yawDirection <= 1; yawDirection += 2) {
                if (searchBothYawDirections
                        || yawOffset == 0
                        || yawDirection == preferredYawDirection) {
                    float candidateYaw = baseYaw + yawOffset * yawDirection;
                    int pitchOffset = 0;

                    while (true) {

                        if (pitchOffset > 90) {
                            break;
                        }

                        for (int pitchDirection = pitchOffset == 0 ? 0 : -1;
                                pitchDirection <= 1;
                                pitchDirection += 2) {
                            float candidatePitch = pitchOffset * pitchDirection;
                            BlockHitResult hit = rayTraceBlock(4.5F, candidateYaw, candidatePitch);
                            if (isHitOnFace(hit, position, face)) {
                                return new PlaceRotation(
                                        candidateYaw, candidatePitch, hit.getLocation());
                            }
                        }

                        pitchOffset += pitchOffset < 20 ? 1 : 2;
                    }
                }
            }

            yawOffset += yawOffset < 20 ? 1 : 2;
        }
    }

    private static boolean isHitOnFace(BlockHitResult hit, BlockPos position, Direction face) {
        return hit.getType() == Type.BLOCK
                && hit.getBlockPos().equals(position)
                && hit.getDirection() == face;
    }

    private static Vec3 facePoint(BlockPos position, Direction face, int sampleIndex) {
        int sampleRow = sampleIndex / 16;
        int sampleColumn = sampleIndex % 16;
        double rowOffset = MathUtil.cellCenter(sampleRow, 16);
        double columnOffset = MathUtil.cellCenter(sampleColumn, 16);
        double faceOffset = face.getAxisDirection() == AxisDirection.POSITIVE ? 1.0 : 0.0;

        return switch (AxisSwitchMap.switchMapAxis[face.getAxis().ordinal()]) {
            case 1 ->
                    new Vec3(
                            position.getX() + faceOffset,
                            position.getY() + rowOffset,
                            position.getZ() + columnOffset);
            case 2 ->
                    new Vec3(
                            position.getX() + rowOffset,
                            position.getY() + faceOffset,
                            position.getZ() + columnOffset);
            case 3 ->
                    new Vec3(
                            position.getX() + rowOffset,
                            position.getY() + columnOffset,
                            position.getZ() + faceOffset);
            default -> throw new MatchException(null, null);
        };
    }

    public static boolean rayTraceHitsBlock(
            Direction face,
            BlockPos position,
            boolean requireFace,
            float range,
            float yaw,
            float pitch) {
        BlockHitResult hit = rayTraceBlock(range, yaw, pitch);
        if (hit.getBlockPos().getX() != position.getX()
                || hit.getBlockPos().getY() != position.getY()
                || hit.getBlockPos().getZ() != position.getZ()) {
            return false;
        } else {
            return requireFace ? hit.getDirection() == face : true;
        }
    }

    public static boolean isInFov(Entity entity, float fovDegrees) {
        if (fovDegrees >= 360.0F) {
            return true;
        }

        Vec3 targetDirection = entity.position().subtract(mc.player.getEyePosition());
        float targetYaw =
                (float) (Math.toDegrees(Math.atan2(targetDirection.z, targetDirection.x)) - 90.0);
        float yawDifference = Math.abs(Mth.wrapDegrees(targetYaw - mc.player.getYRot()));
        return yawDifference <= fovDegrees / 2.0F;
    }

    /** Calculates rotations from one entity eye position to another. */
    public static float[] getRotationsBetweenEntities(Entity origin, Entity target) {
        double deltaX = target.getX() - origin.getX();
        double deltaZ = target.getZ() - origin.getZ();
        double deltaY =
                target.getY() + target.getEyeHeight() - (origin.getY() + origin.getEyeHeight());
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F);
        float pitch =
                Mth.clamp(
                        (float) (-Math.toDegrees(Math.atan2(deltaY, horizontalDistance))),
                        -90.0F,
                        90.0F);
        return new float[] {yaw, pitch};
    }

    /** Measures distance from an entity eye position to the closest target hitbox point. */
    public static double eyeToHitboxDistance(Entity origin, Entity target) {
        Vec3 eyePosition = origin.getEyePosition(1.0F);
        return eyePosition.distanceTo(closestPointOnBox(eyePosition, target.getBoundingBox()));
    }

    /** Intersects a six-block view ray with the expanded target hitbox. */
    public static Vec3 intersectViewRayWithHitbox(
            Entity origin, Entity target, double hitboxExpansion) {
        Vec3 rayStart = origin.getEyePosition(1.0F);
        Vec3 rayEnd = rayStart.add(origin.getViewVector(1.0F).scale(6.0));
        return target.getBoundingBox().inflate(hitboxExpansion).clip(rayStart, rayEnd).orElse(null);
    }

    /** Returns the angular distance to the nearest multiple of 45 degrees. */
    public static float distanceToNearest45Degrees(float angle) {
        float sectorRemainder = Math.abs(angle % 45.0F);
        return Math.min(Math.abs(45.0F - sectorRemainder), sectorRemainder);
    }

    /** Sums wrapped changes between the requested most recent yaw samples. */
    public static float sumYawChanges(List<Float> yawSamples, int sampleCount) {
        float totalYawChange = 0.0F;
        int availableSampleCount = Math.min(sampleCount, yawSamples.size() - 1);

        for (int sampleIndex = yawSamples.size() - availableSampleCount - 1;
                sampleIndex < yawSamples.size() - 1;
                sampleIndex++) {
            if (sampleIndex >= 0) {
                totalYawChange +=
                        Math.abs(
                                Mth.wrapDegrees(
                                        (Float) yawSamples.get(sampleIndex + 1)
                                                - (Float) yawSamples.get(sampleIndex)));
            }
        }

        return totalYawChange;
    }

    static {
        numericConstants = MetadataState.getInstance();
    }
}
