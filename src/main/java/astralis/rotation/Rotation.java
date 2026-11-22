package astralis.rotation;

import astralis.metadata.MetadataState;
import astralis.util.RotationUtil;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Rotation implements InstanceAccess {
    private static MetadataState numericConstants;
    private float targetYaw;
    private float targetPitch;
    private float yawSpeed;
    private float pitchSpeed;
    private float yaw;
    private float pitch;
    private float lastYaw;
    private float lastPitch;
    private final MoveFixMode moveFix;
    private String tag;
    private Entity targetEntity;
    private float visualYaw;
    private float visualPitch;
    private boolean tickStepped;
    private boolean customVisual;
    private boolean visualPitchFollows;
    private boolean applyYawToPlayerView;
    private boolean applyPitchToPlayerView;
    private long timestamp;
    private RotationPriority priority;

    public Rotation(
            float targetYaw,
            float targetPitch,
            float yawSpeed,
            float pitchSpeed,
            RotationPriority priority,
            MoveFixMode moveFix) {
        this(targetYaw, targetPitch, yawSpeed, pitchSpeed, moveFix);
        this.priority = priority;
    }

    public Rotation(
            float targetYaw,
            float targetPitch,
            float yawSpeed,
            float pitchSpeed,
            RotationPriority priority) {
        this(targetYaw, targetPitch, yawSpeed, pitchSpeed);
        this.priority = priority;
    }

    public Rotation(float targetYaw, float targetPitch, float yawSpeed, float pitchSpeed) {
        this(targetYaw, targetPitch, yawSpeed, pitchSpeed, MoveFixMode.SILENT);
    }

    public Rotation(
            float targetYaw,
            float targetPitch,
            float yawSpeed,
            float pitchSpeed,
            MoveFixMode moveFix) {

        this.tickStepped = false;
        this.priority = RotationPriority.STANDARD;
        float playerYaw = mc.player.getYRot();
        this.targetYaw = playerYaw + Mth.wrapDegrees(targetYaw - playerYaw);
        this.targetPitch = targetPitch;
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
        this.yaw = yawSpeed >= 180.0F ? this.targetYaw : playerYaw;
        this.pitch = pitchSpeed >= 180.0F ? targetPitch : mc.player.getXRot();
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;
        this.updateVisual(this.yaw, this.pitch);
        this.timestamp = System.currentTimeMillis();
        this.moveFix = moveFix;
    }

    public void setLast(float yaw, float pitch) {
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }

    public Vec3 getLookVec() {
        float degreesToRadians = numericConstants.getFloatParameter("deg_to_rad");
        float yawCos = Mth.cos(-this.getYaw() * degreesToRadians);
        float yawSin = Mth.sin(-this.getYaw() * degreesToRadians);
        float pitchCos = Mth.cos(this.getPitch() * degreesToRadians);
        float pitchSin = Mth.sin(this.getPitch() * degreesToRadians);
        return new Vec3(yawSin * pitchCos, -pitchSin, yawCos * pitchCos);
    }

    public void setPriority(RotationPriority priority) {
        this.priority = priority;
    }

    public float getLastYaw() {
        return this.lastYaw;
    }

    public float getVisualPitch() {
        return this.visualPitch;
    }

    public boolean isVisualPitchFollows() {
        return this.visualPitchFollows;
    }

    public float getVisualYaw() {
        return this.visualYaw;
    }

    public void setTarget(float targetYaw, float targetPitch, float yawSpeed, float pitchSpeed) {
        this.targetYaw = this.yaw + Mth.wrapDegrees(targetYaw - this.yaw);
        this.targetPitch = targetPitch;
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
        this.timestamp = System.currentTimeMillis();
    }

    public void step() {
        this.setRotation(
                RotationUtil.rotateTowards(this.lastYaw, this.targetYaw, this.yawSpeed),
                RotationUtil.rotateTowards(this.lastPitch, this.targetPitch, this.pitchSpeed));
    }

    public float getTargetPitch() {
        return this.targetPitch;
    }

    public Entity getTargetEntity() {
        return this.targetEntity;
    }

    public void applyGcd() {
        float rotationIncrement = RotationUtil.getGcd();
        float yawDelta = Mth.wrapDegrees(this.yaw - this.lastYaw);
        float pitchDelta = this.pitch - this.lastPitch;
        float snappedYawDelta = yawDelta - yawDelta % rotationIncrement;
        float snappedPitchDelta = pitchDelta - pitchDelta % rotationIncrement;
        this.setRotation(this.lastYaw + snappedYawDelta, this.lastPitch + snappedPitchDelta);
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public boolean isTickStepped() {
        return this.tickStepped;
    }

    public float getYawSpeed() {
        return this.yawSpeed;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int compareWith(Rotation other) {
        return 0;
    }

    /** Controls whether applying this rotation also replaces the stored player-view yaw. */
    public void setApplyYawToPlayerView(boolean applyYaw) {
        this.applyYawToPlayerView = applyYaw;
    }

    /** Controls whether applying this rotation also replaces the stored player-view pitch. */
    public void setApplyPitchToPlayerView(boolean applyPitch) {
        this.applyPitchToPlayerView = applyPitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitchSpeed() {
        return this.pitchSpeed;
    }

    public void setVisualYaw(float visualYaw) {
        this.visualYaw = visualYaw;
    }

    public boolean isCustomVisual() {
        return this.customVisual;
    }

    public void setTargetEntity(Entity targetEntity) {
        this.targetEntity = targetEntity;
    }

    public void setVisualPitchFollows(boolean visualPitchFollows) {
        this.visualPitchFollows = visualPitchFollows;
    }

    public void setVisualPitch(float visualPitch) {
        this.visualPitch = visualPitch;
    }

    public void update(float targetYaw, float targetPitch, float yawSpeed, float pitchSpeed) {
        this.setTarget(targetYaw, targetPitch, yawSpeed, pitchSpeed);
        if (yawSpeed >= 180.0F) {
            this.yaw = this.targetYaw;
        }

        if (pitchSpeed >= 180.0F) {
            this.pitch = targetPitch;
        }
    }

    public RotationPriority getPriority() {
        return this.priority;
    }

    public void setCustomVisual(boolean customVisual) {
        this.customVisual = customVisual;
    }

    public void smoothTowards(float startYaw, float startPitch, float partialTick) {
        float yawDelta = Mth.wrapDegrees(this.targetYaw - startYaw) * partialTick;
        float pitchDelta = (this.targetPitch - startPitch) * partialTick;
        double angularDistance = Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
        if (angularDistance == 0.0) {
            this.setRotation(
                    startYaw + yawDelta, Math.clamp(startPitch + pitchDelta, -90.0F, 90.0F));
        } else {
            double yawFraction = Math.abs(yawDelta / angularDistance);
            double pitchFraction = Math.abs(pitchDelta / angularDistance);
            float yawStep =
                    (float)
                            Math.clamp(
                                    yawDelta,
                                    -this.yawSpeed * yawFraction,
                                    this.yawSpeed * yawFraction);
            float pitchStep =
                    (float)
                            Math.clamp(
                                    pitchDelta,
                                    -this.pitchSpeed * pitchFraction,
                                    this.pitchSpeed * pitchFraction);
            this.setRotation(startYaw + yawStep, Math.clamp(startPitch + pitchStep, -90.0F, 90.0F));
        }
    }

    private void updateVisual(float yaw, float pitch) {
        if (this.visualPitchFollows) {
            this.visualPitch = pitch;
        }

        if (!this.customVisual) {
            this.visualYaw = yaw;
            this.visualPitch = pitch;
        }
    }

    public float getLastPitch() {
        return this.lastPitch;
    }

    public void setTickStepped(boolean tickStepped) {
        this.tickStepped = tickStepped;
    }

    public MoveFixMode getMoveFix() {
        return this.moveFix;
    }

    /** Returns whether this rotation should replace the stored player-view pitch. */
    public boolean isApplyPitchToPlayerView() {
        return this.applyPitchToPlayerView;
    }

    /** Returns whether this rotation should replace the stored player-view yaw. */
    public boolean isApplyYawToPlayerView() {
        return this.applyYawToPlayerView;
    }

    public String getTag() {
        return this.tag;
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.updateVisual(yaw, pitch);
    }

    static {
        numericConstants = MetadataState.getInstance();
    }
}
