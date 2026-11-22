package astralis.module.impl.esp;

import astralis.util.render.ModelPartPose;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;

public class PlayerPoseSnapshot {
    public double x;
    public double y;
    public double z;
    public float scale = 1.0F;
    public float bodyRot;
    public float deathTime;
    public Pose pose = Pose.STANDING;
    public Direction bedOrientation;
    public boolean upsideDown;
    public float xRot;
    public float swimAmount;
    public boolean inWater;
    public boolean visuallySwimming;
    public boolean fallFlying;
    public boolean autoSpinAttack;
    public float fallFlyingScale;
    public boolean applyFlyingYRot;
    public float flyingYRot;
    public ModelPartPose headPose = ModelPartPose.ZERO;
    public ModelPartPose bodyPose = ModelPartPose.ZERO;
    public ModelPartPose rightArmPose = ModelPartPose.ZERO;
    public ModelPartPose leftArmPose = ModelPartPose.ZERO;
    public ModelPartPose rightLegPose = ModelPartPose.ZERO;
    public ModelPartPose leftLegPose = ModelPartPose.ZERO;
    public long captureTime;
}
