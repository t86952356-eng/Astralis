package astralis.util.render.font;

import com.google.gson.JsonObject;

public class MsdfBounds {
    private float left;
    private float top;
    private float right;
    private float bottom;

    public MsdfBounds(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float getTop() {
        return this.top;
    }

    public float getLeft() {
        return this.left;
    }

    public float getBottom() {
        return this.bottom;
    }

    public float getRight() {
        return this.right;
    }

    public static MsdfBounds fromJson(JsonObject jsonObject) {
        MsdfBounds bounds;

        float left;
        if (jsonObject.has("left")) {

            left = jsonObject.get("left").getAsFloat();
        } else {
            left = 0.0F;
        }

        float top;
        if (jsonObject.has("top")) {

            top = jsonObject.get("top").getAsFloat();
        } else {
            top = 0.0F;
        }

        float right;
        if (jsonObject.has("right")) {

            right = jsonObject.get("right").getAsFloat();
        } else {
            right = 0.0F;
        }

        float bottom;
        if (jsonObject.has("bottom")) {

            bottom = jsonObject.get("bottom").getAsFloat();
        } else {
            bottom = 0.0F;
        }

        bounds = new MsdfBounds(left, top, right, bottom);
        return bounds;
    }
}
