package astralis.rotation;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.JumpYawEvent;
import astralis.event.impl.MouseUpdateEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.MovementYawEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.RotationFrameEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.util.Listener;
import astralis.util.MoveUtil;
import astralis.util.RotationUtil;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;

public class RotationManager extends Listener {
    private Rotation currentRotation;
    private final PlayerViewSnapshot playerViewSnapshot = new PlayerViewSnapshot();
    private boolean submittedThisTick;
    private boolean returningToPlayerView;
    private boolean steppedThisTick;
    private boolean hasMovementSnapshot;
    private boolean hasInterpolationOrigin;
    private boolean applyingMouseMovement;
    private float movementYaw;
    private float movementPitch;
    private float interpolationStartYaw;
    private float interpolationStartPitch;
    private float partialTick = 1.0F;
    private float mouseStartYaw;
    private float mouseStartPitch;

    /** Updates the active rotation from a yaw/pitch pair, using the supplied turn speeds. */
    public void submitAngles(float[] targetAngles, float yawSpeed, float pitchSpeed) {
        if (this.currentRotation != null) {
            this.currentRotation.update(targetAngles[0], targetAngles[1], yawSpeed, pitchSpeed);
            this.currentRotation.setTickStepped(false);
            this.currentRotation.setApplyYawToPlayerView(false);
            this.currentRotation.setApplyPitchToPlayerView(false);
            this.currentRotation.setTag(null);
        } else {
            this.currentRotation =
                    new Rotation(targetAngles[0], targetAngles[1], yawSpeed, pitchSpeed);
        }

        this.activateRotation();
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()
                && this.currentRotation != null
                && this.currentRotation.isTickStepped()) {
            this.stepTickRotation();
            this.finishReturnIfAligned();
        }
    }

    public float getFlyingPitch() {
        return this.getMovementPitch();
    }

    private void clearRotation(boolean restorePlayerView) {
        if (restorePlayerView && mc.player != null) {
            this.playerViewSnapshot.restoreToPlayer(this.getMovementYaw());
        }

        this.setEnabled(false);
        this.currentRotation = null;
        this.returningToPlayerView = false;
        this.steppedThisTick = false;
        this.hasMovementSnapshot = false;
        this.hasInterpolationOrigin = false;
        this.applyingMouseMovement = false;
        this.playerViewSnapshot.clearFlags();
    }

    /** Advances tick state and starts returning when no rotation was submitted. */
    public void beginClientTick() {
        if (this.currentRotation != null) {
            if (!this.submittedThisTick && !this.returningToPlayerView) {
                this.beginReturnToPlayerView();
                if (this.currentRotation == null) {
                    return;
                }
            }

            this.submittedThisTick = false;
            this.steppedThisTick = false;
            if (this.returningToPlayerView) {
                this.updateReturnTarget();
            }

            if (!this.currentRotation.isTickStepped()) {
                this.captureInterpolationOrigin();
            }

            this.hasMovementSnapshot = false;
        }
    }

    /** Applies render-time interpolation and publishes the current rotation event. */
    public void updateFrame(float partialTick) {
        if (mc.player != null
                && !mc.player.isDeadOrDying()
                && this.isEnabled()
                && this.currentRotation != null) {
            this.partialTick = partialTick;
            if (!this.currentRotation.isTickStepped()) {
                this.updateReturnTarget();
                this.interpolateRotation(partialTick, false);
                if (this.finishReturnIfAligned()) {
                    return;
                }
            }

            Astralis.getInstance()
                    .getEventBus()
                    .post(
                            new RotationFrameEvent(
                                    partialTick,
                                    this.currentRotation.getYaw(),
                                    this.currentRotation.getPitch(),
                                    this.currentRotation.getTargetYaw(),
                                    this.currentRotation.getTargetPitch(),
                                    this.currentRotation.getTag() != null
                                            ? this.currentRotation.getTag()
                                            : "",
                                    this.currentRotation.getTargetEntity()));
        }
    }

    /** Handles accumulated mouse movement once while preserving its starting rotation. */
    public void processMouseMovement() {
        if (!this.applyingMouseMovement
                && this.isEnabled()
                && this.currentRotation != null
                && !this.currentRotation.isTickStepped()
                && mc.player != null
                && mc.mouseHandler.isMouseGrabbed()) {
            this.mouseStartYaw = this.currentRotation.getYaw();
            this.mouseStartPitch = this.currentRotation.getPitch();
            this.applyingMouseMovement = true;

            try {
                mc.mouseHandler.handleAccumulatedMovement();
            } finally {
                this.applyingMouseMovement = false;
            }
        }
    }

    /** Returns the captured movement yaw, falling back to the current rotation. */
    public float getMovementYaw() {
        return this.hasMovementSnapshot ? this.movementYaw : this.getYaw();
    }

    /** Returns the captured movement pitch, falling back to the current rotation. */
    public float getMovementPitch() {
        return this.hasMovementSnapshot ? this.movementPitch : this.getPitch();
    }

    public float getPitch() {
        return this.currentRotation != null
                ? this.currentRotation.getPitch()
                : (mc.player != null ? mc.player.getXRot() : 0.0F);
    }

    /** Returns the saved player-view state used by movement and camera integration. */
    public PlayerViewSnapshot getPlayerViewSnapshot() {
        return this.playerViewSnapshot;
    }

    public static RotationManager getInstance() {
        return Astralis.getInstance().getManagerRegistry().getManager(RotationManager.class);
    }

    private void activateRotation() {
        if (mc.player != null && this.currentRotation != null) {
            this.playerViewSnapshot.captureFromPlayer();
            this.setEnabled(true);
            this.submittedThisTick = true;
            this.returningToPlayerView = false;
            if (!this.currentRotation.isTickStepped() && !this.hasInterpolationOrigin) {
                this.captureInterpolationOrigin();
            }

            this.partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            if (this.currentRotation.isTickStepped()) {
                this.stepTickRotation();
            } else {
                this.interpolateRotation(this.partialTick, false);
            }
        }
    }

    public float getYaw() {
        return this.currentRotation != null
                ? this.currentRotation.getYaw()
                : (mc.player != null ? mc.player.getYRot() : 0.0F);
    }

    /** Returns the visual yaw of the current rotation or the player yaw. */
    public float getVisualYaw() {
        return this.currentRotation != null
                ? this.currentRotation.getVisualYaw()
                : (mc.player != null ? mc.player.getYRot() : 0.0F);
    }

    /** Returns the visual pitch of the current rotation or the player pitch. */
    public float getVisualPitch() {
        return this.currentRotation != null
                ? this.currentRotation.getVisualPitch()
                : (mc.player != null ? mc.player.getXRot() : 0.0F);
    }

    public Rotation getCurrent() {
        return this.currentRotation;
    }

    /** Applies configured player-view replacements and restores the saved player view. */
    public void syncPlayerView() {
        if (this.currentRotation != null && !this.returningToPlayerView) {
            this.playerViewSnapshot.overrideRotation(
                    this.applyingMouseMovement ? this.mouseStartYaw : this.currentRotation.getYaw(),
                    this.applyingMouseMovement
                            ? this.mouseStartPitch
                            : this.currentRotation.getPitch(),
                    this.currentRotation.isApplyYawToPlayerView(),
                    this.currentRotation.isApplyPitchToPlayerView());
        }

        this.playerViewSnapshot.restoreToPlayer(this.getMovementYaw());
    }

    private boolean shouldFixMovement() {
        return this.isEnabled()
                && this.currentRotation != null
                && this.currentRotation.getMoveFix() != MoveFixMode.NONE;
    }

    private void captureInterpolationOrigin() {
        if (this.currentRotation != null) {
            this.interpolationStartYaw = this.getMovementYaw();
            this.interpolationStartPitch = this.getMovementPitch();
            this.hasInterpolationOrigin = true;
        }
    }

    private boolean shouldApplySilentMoveFix() {
        return this.shouldFixMovement() && this.currentRotation.getMoveFix() == MoveFixMode.SILENT;
    }

    private void beginReturnToPlayerView() {
        if (mc.player != null && this.currentRotation != null) {
            this.playerViewSnapshot.captureFromPlayer();
            this.currentRotation.setCustomVisual(false);
            this.returningToPlayerView = true;
            this.updateReturnTarget();
            this.hasInterpolationOrigin = false;
            this.finishReturnIfAligned();
        } else {
            this.clearRotation(true);
        }
    }

    @EventTarget(4)
    public void onMouseUpdate(MouseUpdateEvent event) {
        if (!event.isCancelled()) {
            if (this.isEnabled()
                    && this.currentRotation != null
                    && !this.currentRotation.isTickStepped()
                    && !(event.getSensitivityFactor() <= 0.0)) {
                this.updateReturnTarget();
                this.interpolateRotation(
                        this.applyingMouseMovement ? 1.0F : this.partialTick,
                        this.applyingMouseMovement);
                event.setDeltaX(
                        RotationUtil.angleToMouseDelta(
                                        Mth.wrapDegrees(
                                                this.currentRotation.getYaw()
                                                        - mc.player.getYRot()),
                                        event.getSensitivityFactor())
                                * (mc.options.invertMouseX().get() ? -1.0 : 1.0));
                event.setDeltaY(
                        RotationUtil.angleToMouseDelta(
                                        this.currentRotation.getPitch() - mc.player.getXRot(),
                                        event.getSensitivityFactor())
                                * (mc.options.invertMouseY().get() ? -1.0 : 1.0));
                event.disableSmoothCamera();
                this.finishReturnIfAligned();
            }
        }
    }

    @EventTarget
    public void onJumpYaw(JumpYawEvent event) {
        if (this.shouldFixMovement()) {
            if (this.shouldApplySilentMoveFix()) {
                this.stepTickRotation();
                if (this.finishReturnIfAligned()) {
                    return;
                }
            }

            event.setYaw(this.getMovementYaw());
        }
    }

    public void resetImmediately() {
        this.clearRotation(true);
    }

    @EventTarget(1)
    public void onMouseUpdate2(MouseUpdateEvent event) {
        if (!event.isCancelled() && mc.player != null) {
            this.playerViewSnapshot.applyMouseDelta(event);
        }
    }

    @EventTarget
    public void onMovementYaw(MovementYawEvent event) {
        if (this.shouldFixMovement()) {
            if (this.shouldApplySilentMoveFix()) {
                this.stepTickRotation();
                if (this.finishReturnIfAligned()) {
                    return;
                }
            }

            event.setYaw(this.getMovementYaw());
        }
    }

    private boolean finishReturnIfAligned() {
        if (this.returningToPlayerView && this.currentRotation != null) {
            float alignmentThreshold = RotationUtil.getGcd() * 1.5F;
            if (Math.abs(
                                    Mth.wrapDegrees(
                                            this.currentRotation.getYaw()
                                                    - this.playerViewSnapshot.getYaw()))
                            <= alignmentThreshold
                    && Math.abs(
                                    this.currentRotation.getPitch()
                                            - this.playerViewSnapshot.getPitch())
                            <= alignmentThreshold) {
                this.clearRotation(true);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void captureMovementRotation() {
        if (this.currentRotation != null) {
            this.movementYaw = this.currentRotation.getYaw();
            this.movementPitch = this.currentRotation.getPitch();
            this.hasMovementSnapshot = true;
        }
    }

    public void submit(Rotation rotation) {
        if (this.currentRotation != null) {
            if (this.returningToPlayerView
                    || rotation.getPriority().getPriorityLevel()
                            >= this.currentRotation.getPriority().getPriorityLevel()) {
                this.currentRotation.update(
                        rotation.getTargetYaw(),
                        rotation.getTargetPitch(),
                        rotation.getYawSpeed(),
                        rotation.getPitchSpeed());
                this.currentRotation.setPriority(rotation.getPriority());
                this.currentRotation.setTickStepped(rotation.isTickStepped());
                this.currentRotation.setApplyYawToPlayerView(rotation.isApplyYawToPlayerView());
                this.currentRotation.setApplyPitchToPlayerView(rotation.isApplyPitchToPlayerView());
                this.currentRotation.setTag(rotation.getTag());
                this.currentRotation.setCustomVisual(rotation.isCustomVisual());
                if (rotation.isCustomVisual()) {
                    this.currentRotation.setVisualYaw(rotation.getVisualYaw());
                    this.currentRotation.setVisualPitch(rotation.getVisualPitch());
                }
            }
        } else {
            this.currentRotation = rotation;
        }

        this.activateRotation();
    }

    public void reset() {
        if (this.currentRotation != null) {
            this.beginReturnToPlayerView();
        }
    }

    @EventTarget(1)
    public void onMoveInput(MoveInputEvent event) {
        if (this.shouldApplySilentMoveFix()) {
            this.stepTickRotation();
            if (!this.finishReturnIfAligned()) {
                MoveUtil.applyMoveFix(event, this.getMovementYaw());
            }
        }
    }

    public float getFlyingYaw() {
        return this.getMovementYaw();
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent event) {
        this.clearRotation(false);
    }

    private void stepTickRotation() {
        if (this.currentRotation != null
                && this.currentRotation.isTickStepped()
                && !this.steppedThisTick) {
            this.currentRotation.step();
            this.currentRotation.applyGcd();
            this.steppedThisTick = true;
            this.captureMovementRotation();
            this.currentRotation.setLast(
                    this.currentRotation.getYaw(), this.currentRotation.getPitch());
        }
    }

    private void updateReturnTarget() {
        if (this.returningToPlayerView && this.currentRotation != null) {
            this.currentRotation.setTarget(
                    this.playerViewSnapshot.getYaw(),
                    this.playerViewSnapshot.getPitch(),
                    this.currentRotation.getYawSpeed(),
                    this.currentRotation.getPitchSpeed());
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && this.currentRotation != null) {
            if (event.getPacket() instanceof ServerboundMovePlayerPacket movementPacket
                    && event.getStage() == EventStage.SEND
                    && movementPacket.hasRotation()) {
                this.forceRotation(movementPacket.getYRot(1.0F), movementPacket.getXRot(1.0F));
            }

            if (event.getPacket() instanceof ClientboundPlayerPositionPacket positionCorrection) {
                float correctedYaw = positionCorrection.change().yRot();
                float correctedPitch = positionCorrection.change().xRot();
                this.currentRotation.setRotation(correctedYaw, correctedPitch);
                this.currentRotation.setLast(correctedYaw, correctedPitch);
                this.currentRotation.update(
                        correctedYaw,
                        correctedPitch,
                        this.currentRotation.getYawSpeed(),
                        this.currentRotation.getPitchSpeed());
                this.movementYaw = correctedYaw;
                this.movementPitch = correctedPitch;
                this.hasMovementSnapshot = true;
                this.hasInterpolationOrigin = false;
                this.finishReturnIfAligned();
            }
        }
    }

    public void submit(float targetYaw, float targetPitch, float yawSpeed, float pitchSpeed) {
        if (this.currentRotation != null) {
            this.currentRotation.update(targetYaw, targetPitch, yawSpeed, pitchSpeed);
            this.currentRotation.setTickStepped(false);
            this.currentRotation.setApplyYawToPlayerView(false);
            this.currentRotation.setApplyPitchToPlayerView(false);
            this.currentRotation.setTag(null);
        } else {
            this.currentRotation = new Rotation(targetYaw, targetPitch, yawSpeed, pitchSpeed);
        }

        this.activateRotation();
    }

    private void interpolateRotation(float partialTick, boolean captureMovement) {
        if (this.currentRotation != null && !this.currentRotation.isTickStepped()) {
            if (!this.hasInterpolationOrigin) {
                this.captureInterpolationOrigin();
            }

            this.currentRotation.smoothTowards(
                    this.interpolationStartYaw, this.interpolationStartPitch, partialTick);
            this.currentRotation.applyGcd();
            if (captureMovement) {
                this.captureMovementRotation();
            }
        }
    }

    public void forceRotation(float yaw, float pitch) {
        if (this.currentRotation != null) {
            this.currentRotation.setRotation(yaw, pitch);
            this.currentRotation.setLast(yaw, pitch);
            this.movementYaw = yaw;
            this.movementPitch = pitch;
            this.hasMovementSnapshot = true;
            this.hasInterpolationOrigin = false;
        }
    }
}
