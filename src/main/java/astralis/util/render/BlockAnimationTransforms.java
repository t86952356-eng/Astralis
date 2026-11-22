package astralis.util.render;

import lombok.Generated;

import net.minecraft.util.Mth;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BlockAnimationTransforms {
    private static Matrix4fc inverseBaseTransform;

    public static Matrix4f createBlockTransform(
            String animationName, float swingProgress, float equipProgress, long timeMillis) {
        Matrix4f matrix4f = new Matrix4f();
        float swingSine = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        String animationSelector = animationName;
        switch (animationSelector) {
            case "Exhibition":
                matrix4f.translate(0.0F, equipProgress * 0.3F, 0.0F);
                applyItemSwingTransform(matrix4f, 0.0F);
                matrix4f.translate(0.0F, 0.3F, 0.0F);
                rotateDegreesAroundAxis(matrix4f, -swingSine * 31.0F, 1.0F, 0.0F, 2.0F);
                rotateDegreesAroundAxis(matrix4f, -swingSine * 33.0F, 1.5F, swingSine / 1.1F, 0.0F);
                applySwordHoldPose(matrix4f);
                break;
            case "Tap":
                float tapSwingSine = Mth.sin(Mth.sqrt(swingProgress) * 3.0F);
                matrix4f.translate(-0.06F, 0.1F + equipProgress * 0.36F, 0.0F);
                applyItemSwingTransform(matrix4f, 0.0F);
                rotateDegreesAroundAxis(
                        matrix4f, -tapSwingSine * 40.0F / 2.0F, tapSwingSine / 2.0F, 1.0F, 4.0F);
                rotateDegreesAroundAxis(
                        matrix4f, -tapSwingSine * 30.0F, 1.0F, tapSwingSine / 3.0F, 0.0F);
                applySwordHoldPose(matrix4f);
                break;
            case "Zero Day":
                float zeroDaySwingSine = Mth.sin(Mth.sqrt(swingProgress) * 3.0F) * 1.75F;
                matrix4f.translate(0.0F, equipProgress * 0.36F, 0.0F);
                applyItemSwingTransform(matrix4f, 1.0F);
                matrix4f.translate(
                        -0.06F, (float) (Math.sin(timeMillis / 150.0) * 0.15 + 0.42), 0.0F);
                rotateDegreesAroundAxis(matrix4f, -zeroDaySwingSine * 20.0F, 3.2F, 2.0F, 8.0F);
                rotateDegreesAroundAxis(
                        matrix4f,
                        -zeroDaySwingSine * 10.0F - 10.0F,
                        1.0F,
                        zeroDaySwingSine / 3.5F,
                        0.0F);
                applySwordHoldPose(matrix4f);
                break;
            case "Slide":
                matrix4f.translate(0.0F, equipProgress * 0.6F, 0.0F);
                applySwordHoldPose(applyItemSwingTransform(matrix4f, 0.0F));
                rotateDegreesAroundAxis(matrix4f, -40.0F * swingSine, 1.0F, 0.0F, 1.0F);
                break;
            case "Xiv":
                float xivSwingSine = Mth.sin((float) (Mth.sqrt(swingProgress) * Math.PI));
                float xivSwingSquaredSine =
                        Mth.sin((float) (swingProgress * swingProgress * Math.PI));
                matrix4f.translate(0.0F, equipProgress * 0.2F, 0.0F);
                applySwordHoldPose(applyItemSwingTransform(matrix4f, 0.0F));
                matrix4f.rotateY(-xivSwingSquaredSine * 20.0F * (float) (Math.PI / 180.0));
                matrix4f.rotateZ(-xivSwingSine * 20.0F * (float) (Math.PI / 180.0));
                matrix4f.rotateX(-xivSwingSine * 80.0F * (float) (Math.PI / 180.0));
                break;
            default:
                throw new IllegalArgumentException("Unknown block animation: " + animationName);
        }

        return matrix4f.mul(inverseBaseTransform);
    }

    private static Matrix4f applyItemSwingTransform(Matrix4f matrix4f, float swingProgress) {
        float squaredSwingSine = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingSine = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        return matrix4f.rotateY((float) (Math.PI / 4))
                .rotateY(-squaredSwingSine * 20.0F * (float) (Math.PI / 180.0))
                .rotateZ(-swingSine * 20.0F * (float) (Math.PI / 180.0))
                .rotateX(-swingSine * 80.0F * (float) (Math.PI / 180.0))
                .scale(0.4F);
    }

    private static Matrix4f applySwordHoldPose(Matrix4f matrix4f) {
        return matrix4f.translate(-0.5F, 0.2F, 0.0F)
                .rotateY((float) (Math.PI / 6))
                .rotateX((float) (-Math.PI * 4.0 / 9.0))
                .rotateY((float) (Math.PI / 3));
    }

    private static void rotateDegreesAroundAxis(
            Matrix4f matrix4f, float degrees, float axisX, float axisY, float axisZ) {
        matrix4f.rotate(
                new Quaternionf()
                        .rotationAxis(
                                degrees * (float) (Math.PI / 180.0),
                                new Vector3f(axisX, axisY, axisZ).normalize()));
    }

    @Generated
    private BlockAnimationTransforms() {}

    static {
        inverseBaseTransform =
                applySwordHoldPose(applyItemSwingTransform(new Matrix4f(), 0.0F)).invert();
    }
}
