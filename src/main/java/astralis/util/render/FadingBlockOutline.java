package astralis.util.render;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FadingBlockOutline {
    public final Vec3 position;
    public final VoxelShape shape;
    public float alpha;

    public FadingBlockOutline(Vec3 vector3, VoxelShape voxelShape, float alpha) {
        this.position = vector3;
        this.shape = voxelShape;
        this.alpha = alpha;
    }
}
