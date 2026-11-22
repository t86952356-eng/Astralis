package astralis.util.render;

import org.joml.Matrix4f;

public enum SkyboxFace {
    BOTTOM(0, 0, new Matrix4f().rotateY((float) Math.toRadians(90.0))),
    TOP(
            1,
            0,
            new Matrix4f()
                    .rotateX((float) Math.toRadians(180.0))
                    .rotateY((float) Math.toRadians(-90.0))),
    EAST(
            2,
            0,
            new Matrix4f()
                    .rotateX((float) Math.toRadians(90.0))
                    .rotateZ((float) Math.toRadians(90.0))),
    SOUTH(
            0,
            1,
            new Matrix4f()
                    .rotateX((float) Math.toRadians(90.0))
                    .rotateZ((float) Math.toRadians(180.0))),
    WEST(
            1,
            1,
            new Matrix4f()
                    .rotateX((float) Math.toRadians(90.0))
                    .rotateZ((float) Math.toRadians(-90.0))),
    NORTH(2, 1, new Matrix4f().rotateX((float) Math.toRadians(90.0)));

    public final Matrix4f faceTransform;
    public final int atlasColumn;
    public final int atlasRow;

    SkyboxFace(int column, int row, Matrix4f matrix4f) {
        this.atlasColumn = column;
        this.atlasRow = row;
        this.faceTransform = matrix4f;
    }
}
