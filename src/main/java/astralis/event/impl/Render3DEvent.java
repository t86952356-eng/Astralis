package astralis.event.impl;

import astralis.event.Event;

import com.mojang.blaze3d.vertex.PoseStack;

public class Render3DEvent implements Event {
    private PoseStack poseStack;

    public Render3DEvent(PoseStack poseStack2) {
        this.poseStack = poseStack2;
    }

    public void setPoseStack(PoseStack poseStack2) {
        this.poseStack = poseStack2;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }
}
