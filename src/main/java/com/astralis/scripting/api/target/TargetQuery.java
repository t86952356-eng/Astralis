package com.astralis.scripting.api.target;

import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.entity.ScriptPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class TargetQuery {
    private final List<ScriptEntity> candidates;
    private final List<Predicate<ScriptEntity>> customFilters = new ArrayList<>();
    private final ScriptPlayer localPlayer;
    private TargetSort sort = TargetSort.DISTANCE;
    private double maxDistance = 128.0;
    private double fieldOfView = 360.0;
    private boolean playersOnly;
    private boolean livingOnly = true;
    private boolean visibleOnly;
    private boolean includeInvisible;

    public TargetQuery(List<ScriptEntity> candidates, ScriptPlayer localPlayer) {
        this.candidates =
                List.copyOf(candidates.size() > 1024 ? candidates.subList(0, 1024) : candidates);
        this.localPlayer = localPlayer;
    }

    public TargetQuery playersOnly() {
        this.playersOnly = true;
        return this;
    }

    public TargetQuery allLiving() {
        this.playersOnly = false;
        this.livingOnly = true;
        return this;
    }

    public TargetQuery allEntities() {
        this.playersOnly = false;
        this.livingOnly = false;
        return this;
    }

    public TargetQuery visibleOnly() {
        this.visibleOnly = true;
        return this;
    }

    public TargetQuery includeInvisible() {
        this.includeInvisible = true;
        return this;
    }

    public TargetQuery maximumDistance(double distance) {
        if (Double.isFinite(distance) && !(distance < 0.0) && !(distance > 128.0)) {
            this.maxDistance = distance;
            return this;
        } else {
            throw new IllegalArgumentException("Target distance must be between 0 and 128 blocks.");
        }
    }

    public TargetQuery fieldOfView(double degrees) {
        if (Double.isFinite(degrees) && !(degrees < 0.0) && !(degrees > 360.0)) {
            this.fieldOfView = degrees;
            return this;
        } else {
            throw new IllegalArgumentException(
                    "Target field of view must be between 0 and 360 degrees.");
        }
    }

    public TargetQuery sortBy(TargetSort targetSort) {
        this.sort = Objects.requireNonNull(targetSort, "sort");
        return this;
    }

    public TargetQuery filter(Predicate<ScriptEntity> filter) {
        if (this.customFilters.size() >= 16) {
            throw new IllegalStateException(
                    "A target query cannot have more than 16 custom filters.");
        }

        this.customFilters.add(Objects.requireNonNull(filter, "filter"));
        return this;
    }

    public ScriptEntity first() {
        List results = this.find(1);
        return results.isEmpty() ? null : (ScriptEntity) results.getFirst();
    }

    public List<ScriptEntity> find() {
        return this.find(256);
    }

    public List<ScriptEntity> find(int limit) {
        if (limit < 1 || limit > 256) {
            throw new IllegalArgumentException("Target result limits must be between 1 and 256.");
        }

        if (this.localPlayer == null) {
            return List.of();
        }

        Comparator<ScriptEntity> comparator =
                switch (this.sort) {
                    case DISTANCE ->
                            Comparator.comparingDouble(this.localPlayer::distanceToSquared);
                    case HEALTH -> Comparator.comparingDouble(ScriptEntity::getHealth);
                    case HURT_TIME -> Comparator.comparingInt(ScriptEntity::getHurtTime);
                    case NAME ->
                            Comparator.comparing(
                                    ScriptEntity::getName, String.CASE_INSENSITIVE_ORDER);
                };
        return this.candidates.stream()
                .filter(entity -> entity.getId() != this.localPlayer.getId())
                .filter(ScriptEntity::isAlive)
                .filter(entity -> !this.playersOnly || entity.isPlayer())
                .filter(entity -> !this.livingOnly || entity.isLiving())
                .filter(entity -> this.includeInvisible || !entity.isInvisible())
                .filter(entity -> !this.visibleOnly || this.localPlayer.canSee(entity))
                .filter(
                        entity ->
                                this.localPlayer.distanceToSquared(entity)
                                        <= this.maxDistance * this.maxDistance)
                .filter(this::isWithinFieldOfView)
                .filter(
                        entity ->
                                this.customFilters.stream()
                                        .allMatch(customFilter -> customFilter.test(entity)))
                .sorted(comparator)
                .limit(limit)
                .toList();
    }

    private boolean isWithinFieldOfView(ScriptEntity entity) {
        if (this.fieldOfView >= 360.0) {
            return true;
        }

        double angleToEntity =
                Math.toDegrees(
                                Math.atan2(
                                        entity.getZ() - this.localPlayer.getZ(),
                                        entity.getX() - this.localPlayer.getX()))
                        - 90.0;
        double yawDifference = (angleToEntity - this.localPlayer.getYaw()) % 360.0;
        if (yawDifference >= 180.0) {
            yawDifference -= 360.0;
        }

        if (yawDifference < -180.0) {
            yawDifference += 360.0;
        }

        return Math.abs(yawDifference) <= this.fieldOfView * 0.5;
    }
}
