package astralis.util.render;

import net.minecraft.client.model.geom.ModelPart;

public final class ModelPartPose {
    public static final ModelPartPose ZERO = new ModelPartPose(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public final float x;
    public final float y;
    public final float z;
    public final float xRot;
    public final float yRot;
    public final float zRot;

    public ModelPartPose(float x, float y, float z, float xRot, float yRot, float zRot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
    }

    public static ModelPartPose fromModelPart(ModelPart modelPart) {
        return new ModelPartPose(
                modelPart.x,
                modelPart.y,
                modelPart.z,
                modelPart.xRot,
                modelPart.yRot,
                modelPart.zRot);
    }
}
