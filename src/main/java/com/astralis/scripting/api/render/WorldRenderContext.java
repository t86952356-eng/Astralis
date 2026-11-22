package com.astralis.scripting.api.render;

import astralis.util.render.RenderUtil;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer.Draw;
import net.minecraft.client.renderer.StagedVertexBuffer.ExecuteInfo;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class WorldRenderContext implements InstanceAccess, AutoCloseable {
    private final PoseStack poseStack;
    private int operationCount;
    private int quadCount;
    private int lineSegmentCount;
    private boolean closed;

    public WorldRenderContext(Object poseStackObject) {
        if (poseStackObject instanceof PoseStack poseStack) {
            this.poseStack = poseStack;
        } else {
            throw new IllegalArgumentException("Expected a world pose stack.");
        }
    }

    public float getPartialTicks() {
        this.ensureActive();
        return Math.clamp(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), 0.0F, 1.0F);
    }

    public void quad(
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            double x3,
            double y3,
            double z3,
            double x4,
            double y4,
            double z4,
            int fillColor,
            int outlineColor,
            double outlineWidth) {
        this.ensureActive();
        validateCoordinates(new double[] {x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4}, 12);
        if (!Double.isFinite(outlineWidth)) {
            throw new IllegalArgumentException("Outline width must be finite.");
        }

        if (!(outlineWidth < 0.0) && !(outlineWidth > 16.0)) {
            this.countDrawOperation();
            Vec3 vector3 = mc.gameRenderer.mainCamera().position();
            this.poseStack.pushPose();
            this.poseStack.translate(-vector3.x, -vector3.y, -vector3.z);

            try {
                if (fillColor >>> 24 != 0) {
                    RenderType renderType = RenderUtil.filledNoDepthRenderType;
                    StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
                    Draw draw =
                            stagedVertexBuffer.appendDraw(
                                    renderType.format(), renderType.primitiveTopology());
                    VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
                    Matrix4f matrix4f = this.poseStack.last().pose();
                    vertexConsumer
                            .addVertex(matrix4f, (float) x1, (float) y1, (float) z1)
                            .setColor(fillColor);
                    vertexConsumer
                            .addVertex(matrix4f, (float) x2, (float) y2, (float) z2)
                            .setColor(fillColor);
                    vertexConsumer
                            .addVertex(matrix4f, (float) x3, (float) y3, (float) z3)
                            .setColor(fillColor);
                    vertexConsumer
                            .addVertex(matrix4f, (float) x4, (float) y4, (float) z4)
                            .setColor(fillColor);
                    uploadAndDraw(renderType, stagedVertexBuffer, draw, true);
                }

                if (outlineColor >>> 24 != 0 && outlineWidth > 0.0) {
                    RenderType renderType2 = RenderUtil.linesNoDepthRenderType;
                    StagedVertexBuffer stagedVertexBuffer2 = RenderUtil.getStagedVertexBuffer();
                    Draw draw2 =
                            stagedVertexBuffer2.appendDraw(
                                    renderType2.format(), renderType2.primitiveTopology());
                    VertexConsumer vertexConsumer2 = stagedVertexBuffer2.getVertexBuilder(draw2);
                    Pose pose2 = this.poseStack.last();
                    double[][] corners =
                            new double[][] {
                                {x1, y1, z1},
                                {x2, y2, z2},
                                {x3, y3, z3},
                                {x4, y4, z4}
                            };
                    Vector3f vector3f = new Vector3f();

                    for (int i = 0; i < corners.length; i++) {
                        double[] startCorner = corners[i];
                        double[] endCorner = corners[(i + 1) % corners.length];
                        vector3f.set(
                                        (float) (endCorner[0] - startCorner[0]),
                                        (float) (endCorner[1] - startCorner[1]),
                                        (float) (endCorner[2] - startCorner[2]))
                                .normalize();
                        vertexConsumer2
                                .addVertex(
                                        pose2,
                                        (float) startCorner[0],
                                        (float) startCorner[1],
                                        (float) startCorner[2])
                                .setColor(outlineColor)
                                .setNormal(pose2, vector3f)
                                .setLineWidth((float) outlineWidth);
                        vertexConsumer2
                                .addVertex(
                                        pose2,
                                        (float) endCorner[0],
                                        (float) endCorner[1],
                                        (float) endCorner[2])
                                .setColor(outlineColor)
                                .setNormal(pose2, vector3f)
                                .setLineWidth((float) outlineWidth);
                    }

                    uploadAndDraw(renderType2, stagedVertexBuffer2, draw2, true);
                }
            } finally {
                this.poseStack.popPose();
            }
        } else {
            throw new IllegalArgumentException("Outline width must be between 0 and 16.");
        }
    }

    public void quads(double[] coordinates, int[] colors, int count) {
        this.ensureActive();
        if (coordinates != null
                && colors != null
                && count >= 1
                && count <= 4096
                && coordinates.length >= count * 12
                && colors.length >= count * 4) {
            validateCoordinates(coordinates, count * 12);
            if (this.quadCount + count > 16384) {
                throw new IllegalStateException("World render batched quad limit exceeded.");
            }

            this.countDrawOperation();
            this.quadCount += count;
            Vec3 vector3 = mc.gameRenderer.mainCamera().position();
            this.poseStack.pushPose();
            this.poseStack.translate(-vector3.x, -vector3.y, -vector3.z);

            try {
                RenderType renderType = RenderUtil.filledNoDepthRenderType;
                StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
                Draw draw =
                        stagedVertexBuffer.appendDraw(
                                renderType.format(), renderType.primitiveTopology());
                VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
                Matrix4f matrix4f = this.poseStack.last().pose();

                for (int vertexIndex = 0; vertexIndex < count * 4; vertexIndex++) {
                    vertexConsumer
                            .addVertex(
                                    matrix4f,
                                    (float) coordinates[vertexIndex * 3],
                                    (float) coordinates[vertexIndex * 3 + 1],
                                    (float) coordinates[vertexIndex * 3 + 2])
                            .setColor(colors[vertexIndex]);
                }

                uploadAndDraw(renderType, stagedVertexBuffer, draw, true);
            } finally {
                this.poseStack.popPose();
            }
        } else {
            throw new IllegalArgumentException(
                    "A world quad batch requires between 1 and 4,096 complete quads.");
        }
    }

    public void line(
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double lineWidth,
            int color) {
        this.ensureActive();
        validateCoordinates(new double[] {startX, startY, startZ, endX, endY, endZ}, 6);
        if (!Double.isFinite(lineWidth) || lineWidth < 0.0 || lineWidth > 16.0) {
            throw new IllegalArgumentException("Line width must be between 0 and 16.");
        }

        if (color >>> 24 != 0 && lineWidth != 0.0) {
            Vector3f vector3f =
                    new Vector3f(
                            (float) (endX - startX),
                            (float) (endY - startY),
                            (float) (endZ - startZ));
            if (vector3f.lengthSquared() != 0.0F) {
                this.countDrawOperation();
                Vec3 vector3 = mc.gameRenderer.mainCamera().position();
                this.poseStack.pushPose();
                this.poseStack.translate(-vector3.x, -vector3.y, -vector3.z);

                try {
                    RenderType renderType = RenderUtil.linesNoDepthRenderType;
                    StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
                    Draw draw =
                            stagedVertexBuffer.appendDraw(
                                    renderType.format(), renderType.primitiveTopology());
                    VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
                    Pose pose2 = this.poseStack.last();
                    vector3f.normalize();
                    vertexConsumer
                            .addVertex(pose2, (float) startX, (float) startY, (float) startZ)
                            .setColor(color)
                            .setNormal(pose2, vector3f)
                            .setLineWidth((float) lineWidth);
                    vertexConsumer
                            .addVertex(pose2, (float) endX, (float) endY, (float) endZ)
                            .setColor(color)
                            .setNormal(pose2, vector3f)
                            .setLineWidth((float) lineWidth);
                    uploadAndDraw(renderType, stagedVertexBuffer, draw, true);
                } finally {
                    this.poseStack.popPose();
                }
            }
        }
    }

    public void polyline(
            double[] coordinates, int pointCount, double lineWidth, int color, boolean closeLoop) {
        this.ensureActive();
        if (coordinates != null
                && pointCount >= 2
                && pointCount <= 512
                && coordinates.length >= pointCount * 3) {
            validateCoordinates(coordinates, pointCount * 3);
            if (!Double.isFinite(lineWidth) || lineWidth < 0.0 || lineWidth > 16.0) {
                throw new IllegalArgumentException("Line width must be between 0 and 16.");
            }

            if (color >>> 24 != 0 && lineWidth != 0.0) {
                int segmentCount = closeLoop ? pointCount : pointCount - 1;
                if (this.operationCount >= 1024) {
                    throw new IllegalStateException("World render operation limit exceeded.");
                }

                if (this.lineSegmentCount > 16384 - segmentCount) {
                    throw new IllegalStateException(
                            "World render batched line segment limit exceeded.");
                }

                this.operationCount++;
                this.lineSegmentCount += segmentCount;
                Vec3 vector3 = mc.gameRenderer.mainCamera().position();
                this.poseStack.pushPose();
                this.poseStack.translate(-vector3.x, -vector3.y, -vector3.z);

                try {
                    RenderType renderType = RenderUtil.linesNoDepthRenderType;
                    StagedVertexBuffer stagedVertexBuffer = RenderUtil.getStagedVertexBuffer();
                    Draw draw =
                            stagedVertexBuffer.appendDraw(
                                    renderType.format(), renderType.primitiveTopology());
                    VertexConsumer vertexConsumer = stagedVertexBuffer.getVertexBuilder(draw);
                    Pose pose2 = this.poseStack.last();
                    Vector3f vector3f = new Vector3f();
                    boolean drewSegments = false;

                    for (int segment = 0; segment < segmentCount; segment++) {
                        int startOffset = segment * 3;
                        int endOffset = (segment + 1) % pointCount * 3;
                        vector3f.set(
                                (float) (coordinates[endOffset] - coordinates[startOffset]),
                                (float) (coordinates[endOffset + 1] - coordinates[startOffset + 1]),
                                (float)
                                        (coordinates[endOffset + 2]
                                                - coordinates[startOffset + 2]));
                        if (vector3f.lengthSquared() != 0.0F) {
                            vector3f.normalize();
                            vertexConsumer
                                    .addVertex(
                                            pose2,
                                            (float) coordinates[startOffset],
                                            (float) coordinates[startOffset + 1],
                                            (float) coordinates[startOffset + 2])
                                    .setColor(color)
                                    .setNormal(pose2, vector3f)
                                    .setLineWidth((float) lineWidth);
                            vertexConsumer
                                    .addVertex(
                                            pose2,
                                            (float) coordinates[endOffset],
                                            (float) coordinates[endOffset + 1],
                                            (float) coordinates[endOffset + 2])
                                    .setColor(color)
                                    .setNormal(pose2, vector3f)
                                    .setLineWidth((float) lineWidth);
                            drewSegments = true;
                        }
                    }

                    uploadAndDraw(renderType, stagedVertexBuffer, draw, drewSegments);
                } finally {
                    this.poseStack.popPose();
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "A world polyline requires between 2 and 512 complete points.");
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    private void ensureActive() {
        if (this.closed) {
            throw new IllegalStateException("World render context is closed.");
        }
    }

    private static void validateCoordinates(double[] coordinates, int count) {
        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(coordinates[i]) || Math.abs(coordinates[i]) > 3.0E7) {
                throw new IllegalArgumentException(
                        "World render coordinates must be finite and within world bounds.");
            }
        }
    }

    private void countDrawOperation() {
        if (++this.operationCount > 1024) {
            throw new IllegalStateException("World render operation limit exceeded.");
        }
    }

    private static void uploadAndDraw(
            RenderType renderType,
            StagedVertexBuffer stagedVertexBuffer,
            Draw draw,
            boolean shouldDraw) {
        if (shouldDraw) {
            stagedVertexBuffer.upload();
            PreparedRenderType preparedRenderType = renderType.prepare();
            ExecuteInfo executeInfo = stagedVertexBuffer.getExecuteInfo(draw);
            if (executeInfo != null) {
                preparedRenderType.drawFromBuffer(executeInfo);
            }
        }

        stagedVertexBuffer.endDraw();
    }
}
