package astralis.module.impl.anticheat.check;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Immutable per-observation check context. The factory computes movement from the previous
 * player-state snapshot.
 *
 * @param observedPlayer observed player
 * @param playerState mutable accumulated check state
 * @param localPlayer whether this is the local player
 * @param sensitivityScale sensitivity multiplier, {@code 1.6 - sensitivity * 0.12}
 * @param scaffoldThreshold scaffold threshold, {@code 12.0 - sensitivity}
 * @param movementX change in X since the previous snapshot
 * @param movementY change in Y since the previous snapshot
 * @param movementZ change in Z since the previous snapshot
 * @param horizontalDistance horizontal distance traveled since the previous snapshot
 * @param absYawDelta absolute wrapped yaw change
 * @param yawDelta signed wrapped yaw change
 * @param absPitchDelta absolute pitch change
 * @param airborneTicks consecutive airborne observations
 * @param sprinting current sprinting state
 * @param onGround current grounded state
 */
public record CheckContext(
        Player observedPlayer,
        PlayerCheckState playerState,
        boolean localPlayer,
        double sensitivityScale,
        double scaffoldThreshold,
        double movementX,
        double movementY,
        double movementZ,
        double horizontalDistance,
        float absYawDelta,
        float yawDelta,
        float absPitchDelta,
        int airborneTicks,
        boolean sprinting,
        boolean onGround) {

    /** Builds the context and applies the sensitivity formulas. */
    public static CheckContext create(
            Player player, PlayerCheckState state, boolean self, float sensitivity) {
        double deltaX = player.getX() - state.previousX;
        double deltaY = player.getY() - state.previousY;
        double deltaZ = player.getZ() - state.previousZ;
        float signedYawChange = Mth.wrapDegrees(player.getYRot() - state.previousYaw);
        return new CheckContext(
                player,
                state,
                self,
                1.6 - sensitivity * 0.12,
                12.0 - sensitivity,
                deltaX,
                deltaY,
                deltaZ,
                Math.sqrt(deltaX * deltaX + deltaZ * deltaZ),
                Math.abs(signedYawChange),
                signedYawChange,
                Math.abs(player.getXRot() - state.previousPitch),
                state.airborneTicks,
                player.isSprinting(),
                player.onGround());
    }

    /** Returns the observed player. */
    public Player getPlayer() {
        return this.observedPlayer;
    }

    /** Returns the shared mutable check state. */
    public PlayerCheckState getPlayerState() {
        return this.playerState;
    }

    /** Returns whether the observed player is the local player. */
    public boolean isLocalPlayer() {
        return this.localPlayer;
    }

    /** Returns the sensitivity multiplier used by violation thresholds. */
    public double getSensitivityScale() {
        return this.sensitivityScale;
    }

    /** Returns the scaffold threshold derived from sensitivity. */
    public double getScaffoldThreshold() {
        return this.scaffoldThreshold;
    }

    /** Returns signed X displacement since the previous snapshot. */
    public double getDeltaX() {
        return this.movementX;
    }

    /** Returns signed vertical displacement since the previous snapshot. */
    public double getDeltaY() {
        return this.movementY;
    }

    /** Returns signed Z displacement since the previous snapshot. */
    public double getDeltaZ() {
        return this.movementZ;
    }

    /** Returns horizontal displacement length since the previous snapshot. */
    public double getHorizontalDistance() {
        return this.horizontalDistance;
    }

    /** Returns the absolute wrapped yaw change. */
    public float getAbsYawDelta() {
        return this.absYawDelta;
    }

    /** Returns the signed wrapped yaw change. */
    public float getYawDelta() {
        return this.yawDelta;
    }

    /** Returns the absolute pitch change. */
    public float getAbsPitchDelta() {
        return this.absPitchDelta;
    }

    /** Returns consecutive airborne observations. */
    public int getAirborneTicks() {
        return this.airborneTicks;
    }

    /** Returns current sprinting state. */
    public boolean isSprinting() {
        return this.sprinting;
    }

    /** Returns current grounded state. */
    public boolean isOnGround() {
        return this.onGround;
    }
}
