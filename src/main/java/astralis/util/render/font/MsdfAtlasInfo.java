package astralis.util.render.font;

import com.google.gson.JsonObject;

public class MsdfAtlasInfo {
    private float distanceRange;
    private float width;
    private float height;

    public MsdfAtlasInfo(float distanceRange, float width, float height) {
        this.distanceRange = distanceRange;
        this.width = width;
        this.height = height;
    }

    public static MsdfAtlasInfo fromJson(JsonObject jsonObject) {

        float distanceRangeValue;
        if (jsonObject.has("distanceRange")) {

            distanceRangeValue = jsonObject.get("distanceRange").getAsFloat();
        } else {
            distanceRangeValue = 0.0F;
        }

        float distanceRange = distanceRangeValue;

        float widthValue;
        if (jsonObject.has("width")) {

            widthValue = jsonObject.get("width").getAsFloat();
        } else {
            widthValue = 0.0F;
        }

        float width = widthValue;

        float heightValue;
        if (jsonObject.has("height")) {

            heightValue = jsonObject.get("height").getAsFloat();
        } else {
            heightValue = 0.0F;
        }

        float height = heightValue;
        return new MsdfAtlasInfo(distanceRange, width, height);
    }

    public float getWidth() {
        return this.width;
    }

    public float getDistanceRange() {
        return this.distanceRange;
    }

    public float getHeight() {
        return this.height;
    }
}
