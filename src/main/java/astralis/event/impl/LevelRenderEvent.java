package astralis.event.impl;

import astralis.event.Event;

import com.mojang.blaze3d.vertex.PoseStack;

import lombok.Generated;

import net.minecraft.client.Camera;

/**
 * Posted during {@code GameRenderer.renderLevel} with the view-bobbing pose stack, partial tick
 * and main camera.
 */
public class LevelRenderEvent implements Event {
    private final PoseStack poseStack;
    private final float partialTick;
    private final Camera camera;

    @Generated
    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    @Generated
    public float getPartialTick() {
        return this.partialTick;
    }

    @Generated
    public Camera getCamera() {
        return this.camera;
    }

    @Generated
    public LevelRenderEvent(PoseStack poseStack, float partialTick, Camera camera) {
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        this.camera = camera;
    }
}
