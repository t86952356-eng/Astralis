package astralis.util.render;

import astralis.event.Event;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.HumanoidArm;

/**
 * Posted while submitting the held sword arm when Animations is enabled, so the arm transform can
 * be replaced.
 */
public record SwordArmRenderEvent(
        PoseStack poseStack, float swingProgress, HumanoidArm arm, float equipProgress)
        implements Event {
    public SwordArmRenderEvent(PoseStack poseStack, float swingProgress, HumanoidArm arm) {
        this(poseStack, swingProgress, arm, 0.0F);
    }

    public float getEquipProgress() {
        return this.equipProgress;
    }

    public float getSwingProgress() {
        return this.swingProgress;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public HumanoidArm getArm() {
        return this.arm;
    }
}
