package astralis.util.render.font;

import com.google.gson.JsonObject;

public class MsdfFontMetrics {
    private float emSize;
    private float lineHeight;
    private float ascender;
    private float descender;
    private float underlineY;
    private float underlineThickness;

    public MsdfFontMetrics(
            float emSize,
            float lineHeight,
            float ascender,
            float descender,
            float underlineY,
            float underlineThickness) {
        this.emSize = emSize;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.descender = descender;
        this.underlineY = underlineY;
        this.underlineThickness = underlineThickness;
    }

    public float getLineHeight() {
        return this.lineHeight;
    }

    public float getAscender() {
        return this.ascender;
    }

    public float getUnderlineY() {
        return this.underlineY;
    }

    public float getDescender() {
        return this.descender;
    }

    public float getEmSize() {
        return this.emSize;
    }

    public float getUnderlineThickness() {
        return this.underlineThickness;
    }

    public static MsdfFontMetrics fromJson(JsonObject jsonObject) {
        MsdfFontMetrics metrics;

        float emSize;
        if (jsonObject.has("emSize")) {

            emSize = jsonObject.get("emSize").getAsFloat();
        } else {
            emSize = 0.0F;
        }

        float lineHeight;
        if (jsonObject.has("lineHeight")) {

            lineHeight = jsonObject.get("lineHeight").getAsFloat();
        } else {
            lineHeight = 0.0F;
        }

        float ascender;
        if (jsonObject.has("ascender")) {

            ascender = jsonObject.get("ascender").getAsFloat();
        } else {
            ascender = 0.0F;
        }

        float descender;
        if (jsonObject.has("descender")) {

            descender = jsonObject.get("descender").getAsFloat();
        } else {
            descender = 0.0F;
        }

        float underlineY;
        if (jsonObject.has("underlineY")) {

            underlineY = jsonObject.get("underlineY").getAsFloat();
        } else {
            underlineY = 0.0F;
        }

        float underlineThickness;
        if (jsonObject.has("underlineThickness")) {

            underlineThickness = jsonObject.get("underlineThickness").getAsFloat();
        } else {
            underlineThickness = 0.0F;
        }

        metrics =
                new MsdfFontMetrics(
                        emSize, lineHeight, ascender, descender, underlineY, underlineThickness);
        return metrics;
    }
}
