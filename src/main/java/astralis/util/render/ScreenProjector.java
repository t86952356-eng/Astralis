package astralis.util.render;

import com.astralis.scripting.InstanceAccess;

import lombok.Generated;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Vector4d;
import org.joml.Vector4f;

public final class ScreenProjector implements InstanceAccess {
    private static final double INVALID_DEPTH = -1.0;
    private static Matrix4f modelViewMatrix;
    private static Matrix4f projectionMatrix;
    private static Frustum frustum;

    private ScreenProjector() {}

    public static Vec3 worldToScreen(Vec3 vector3) {
        if (modelViewMatrix != null && projectionMatrix != null) {
            Camera camera = mc.gameRenderer.mainCamera();
            if (camera == null) {
                return invalidPosition();
            }

            Vec3 vector32 = vector3.subtract(camera.position());
            Vector4f vector4f =
                    new Vector4f((float) vector32.x, (float) vector32.y, (float) vector32.z, 1.0F);
            modelViewMatrix.transform(vector4f);
            projectionMatrix.transform(vector4f);
            if (!(vector4f.w <= 1.0E-4F) && Float.isFinite(vector4f.w)) {
                vector4f.div(vector4f.w);
                if (Float.isFinite(vector4f.x)
                        && Float.isFinite(vector4f.y)
                        && Float.isFinite(vector4f.z)) {
                    float screenWidth = mc.getWindow().getScreenWidth();
                    float screenHeight = mc.getWindow().getScreenHeight();
                    float guiScale = mc.getWindow().getGuiScale();
                    return new Vec3(
                            (vector4f.x + 1.0F) * screenWidth * 0.5F / guiScale,
                            (1.0F - vector4f.y) * screenHeight * 0.5F / guiScale,
                            vector4f.z);
                } else {
                    return invalidPosition();
                }
            } else {
                return invalidPosition();
            }
        } else {
            return invalidPosition();
        }
    }

    public static ScreenPoint projectToScreen(Vec3 vector3) {
        if (modelViewMatrix != null && projectionMatrix != null) {
            Camera camera = mc.gameRenderer.mainCamera();
            if (camera == null) {
                return ScreenPoint.invalid();
            }

            Vec3 vector32 = vector3.subtract(camera.position());
            Vector4f vector4f =
                    new Vector4f((float) vector32.x, (float) vector32.y, (float) vector32.z, 1.0F);
            modelViewMatrix.transform(vector4f);
            projectionMatrix.transform(vector4f);
            if (Float.isFinite(vector4f.w) && !(Math.abs(vector4f.w) <= 1.0E-7F)) {
                boolean behindCamera = vector4f.w <= 1.0E-4F;
                vector4f.div(vector4f.w);
                if (Float.isFinite(vector4f.x)
                        && Float.isFinite(vector4f.y)
                        && Float.isFinite(vector4f.z)) {
                    behindCamera = behindCamera || vector4f.z >= 1.0003684F;
                    float screenWidth = mc.getWindow().getScreenWidth();
                    float screenHeight = mc.getWindow().getScreenHeight();
                    float guiScale = mc.getWindow().getGuiScale();
                    return new ScreenPoint(
                            (vector4f.x + 1.0F) * screenWidth * 0.5F / guiScale,
                            (1.0F - vector4f.y) * screenHeight * 0.5F / guiScale,
                            vector4f.z,
                            behindCamera,
                            true);
                } else {
                    return ScreenPoint.invalid();
                }
            } else {
                return ScreenPoint.invalid();
            }
        } else {
            return ScreenPoint.invalid();
        }
    }

    public static Vector4d getEntityScreenBounds(Entity entity2) {
        Camera camera = mc.gameRenderer.mainCamera();
        if (camera != null && !entity2.getBoundingBox().contains(camera.position())) {
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double interpolatedX = entity2.xo + (entity2.getX() - entity2.xo) * partialTick;
            double interpolatedY = entity2.yo + (entity2.getY() - entity2.yo) * partialTick;
            double interpolatedZ = entity2.zo + (entity2.getZ() - entity2.zo) * partialTick;
            AABB boundingBox = entity2.getBoundingBox();
            AABB boundingBox2 =
                    new AABB(
                            boundingBox.minX - entity2.getX() + interpolatedX - 0.1,
                            boundingBox.minY - entity2.getY() + interpolatedY,
                            boundingBox.minZ - entity2.getZ() + interpolatedZ - 0.1,
                            boundingBox.maxX - entity2.getX() + interpolatedX + 0.1,
                            boundingBox.maxY - entity2.getY() + interpolatedY + 0.12,
                            boundingBox.maxZ - entity2.getZ() + interpolatedZ + 0.1);
            Vector4d bounds = null;

            for (double cornerX : new double[] {boundingBox2.minX, boundingBox2.maxX}) {
                for (double cornerY : new double[] {boundingBox2.minY, boundingBox2.maxY}) {
                    for (double cornerZ : new double[] {boundingBox2.minZ, boundingBox2.maxZ}) {
                        Vec3 vector3 = worldToScreen(new Vec3(cornerX, cornerY, cornerZ));
                        if (!(vector3.z <= -1.0) && !(vector3.z >= 1.0)) {
                            if (bounds == null) {
                                bounds = new Vector4d(vector3.x, vector3.y, vector3.x, vector3.y);
                            } else {
                                bounds.x = Math.min(bounds.x, vector3.x);
                                bounds.y = Math.min(bounds.y, vector3.y);
                                bounds.z = Math.max(bounds.z, vector3.x);
                                bounds.w = Math.max(bounds.w, vector3.y);
                            }
                        }
                    }
                }
            }

            if (bounds == null) {
                return null;
            }

            float guiScale = mc.getWindow().getGuiScale();
            double scaledWidth = mc.getWindow().getScreenWidth() / guiScale;
            double scaledHeight = mc.getWindow().getScreenHeight() / guiScale;
            bounds.x = Math.clamp(bounds.x, 0.0, scaledWidth);
            bounds.y = Math.clamp(bounds.y, 0.0, scaledHeight);
            bounds.z = Math.clamp(bounds.z, 0.0, scaledWidth);
            bounds.w = Math.clamp(bounds.w, 0.0, scaledHeight);
            return bounds.x < bounds.z && bounds.y < bounds.w ? bounds : null;
        } else {
            return null;
        }
    }

    private static Vec3 invalidPosition() {
        return new Vec3(0.0, 0.0, -1.0);
    }

    @Generated
    public static Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    @Generated
    public static void setModelViewMatrix(Matrix4f matrix4f) {
        modelViewMatrix = matrix4f;
    }

    @Generated
    public static Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    @Generated
    public static void setProjectionMatrix(Matrix4f matrix4f) {
        projectionMatrix = matrix4f;
    }

    @Generated
    public static Frustum getFrustum() {
        return frustum;
    }
}
