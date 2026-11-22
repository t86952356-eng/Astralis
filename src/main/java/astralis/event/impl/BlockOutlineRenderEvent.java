package astralis.event.impl;

import astralis.event.CancellableEvent;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;

/**
 * Posted before the block outline is submitted; handlers may change its colour and line width or
 * cancel vanilla rendering.
 */
public class BlockOutlineRenderEvent extends CancellableEvent {
    private final BlockOutlineRenderState renderState;
    private final double cameraX;
    private final double cameraY;
    private final double cameraZ;
    private final PoseStack poseStack;
    private final SubmitNodeCollector nodeCollector;
    private int color;
    private float lineWidth;

    public BlockOutlineRenderEvent(
            BlockOutlineRenderState blockOutlineRenderState,
            double cameraX,
            double cameraY,
            double cameraZ,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int color,
            float lineWidth) {
        super();
        this.renderState = blockOutlineRenderState;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.poseStack = poseStack;
        this.nodeCollector = submitNodeCollector;
        this.color = color;
        this.lineWidth = lineWidth;
    }

    public BlockOutlineRenderState getRenderState() {
        return this.renderState;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public double getCameraX() {
        return this.cameraX;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public float getLineWidth() {
        return this.lineWidth;
    }

    public int getColor() {
        return this.color;
    }

    public double getCameraZ() {
        return this.cameraZ;
    }

    public SubmitNodeCollector getNodeCollector() {
        return this.nodeCollector;
    }

    public double getCameraY() {
        return this.cameraY;
    }
}
