package astralis.setting;

import astralis.metadata.MetadataState;
import astralis.module.Module;

import java.util.List;

public class BezierSetting extends Setting<float[]> {
    public BezierSetting(String id, Module module2, float[] points) {
        super(id, module2, validatePoints(points));
    }

    public BezierSetting(String id, float[] points) {
        super(id, validatePoints(points));
    }

    public BezierSetting(String id, Module module2) {
        super(id, module2, defaultPoints());
    }

    private static float[] defaultPoints() {

        MetadataState metadata = MetadataState.getInstance();

        float[] points = new float[] {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F};

        points[2] = metadata.getFloatParameter("bezier_c1x");

        points[3] = metadata.getFloatParameter("bezier_c1y");

        points[4] = metadata.getFloatParameter("bezier_c2x");

        points[5] = metadata.getFloatParameter("bezier_c2y");

        points[6] = 1.0F;

        points[7] = 1.0F;
        return points;
    }

    private static float[] validatePoints(float[] points) {
        if (points != null) {
            int length = points.length;

            if (length == 8) {
                return points;
            }
        }

        return defaultPoints();
    }

    @Override
    public List<Setting<?>> getChildren() {
        return null;
    }

    public float getPointX(int pointIndex) {
        return this.getValue()[pointIndex * 2];
    }

    public float getPointY(int pointIndex) {
        return this.getValue()[pointIndex * 2 + 1];
    }
}
