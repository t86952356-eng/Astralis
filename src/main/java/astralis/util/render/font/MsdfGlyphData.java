package astralis.util.render.font;

import com.google.gson.JsonObject;

public class MsdfGlyphData {
    private int unicode;
    private float advance;
    private MsdfBounds planeBounds;
    private MsdfBounds atlasBounds;

    public MsdfGlyphData(
            int unicode, float advance, MsdfBounds planeBounds, MsdfBounds atlasBounds) {
        this.unicode = unicode;
        this.advance = advance;
        this.planeBounds = planeBounds;
        this.atlasBounds = atlasBounds;
    }

    public int getUnicode() {
        return this.unicode;
    }

    public float getAdvance() {
        return this.advance;
    }

    public MsdfBounds getAtlasBounds() {
        return this.atlasBounds;
    }

    public MsdfBounds getPlaneBounds() {
        return this.planeBounds;
    }

    public static MsdfGlyphData fromJson(JsonObject jsonObject) {

        int unicodeValue;
        if (jsonObject.has("unicode")) {

            unicodeValue = jsonObject.get("unicode").getAsInt();
        } else {
            unicodeValue = 0;
        }

        int unicode = unicodeValue;

        float advanceValue;
        if (jsonObject.has("advance")) {

            advanceValue = jsonObject.get("advance").getAsFloat();
        } else {
            advanceValue = 0.0F;
        }

        MsdfBounds parsedPlaneBounds, parsedAtlasBounds;
        float advance;
        label42:
        {
            advance = advanceValue;

            if (jsonObject.has("planeBounds")) {

                if (!jsonObject.get("planeBounds").isJsonNull()) {

                    parsedPlaneBounds =
                            MsdfBounds.fromJson(jsonObject.getAsJsonObject("planeBounds"));
                    break label42;
                }
            }

            parsedPlaneBounds = null;
        }

        MsdfBounds planeBounds;
        label37:
        {
            planeBounds = parsedPlaneBounds;

            if (jsonObject.has("atlasBounds")) {

                if (!jsonObject.get("atlasBounds").isJsonNull()) {

                    parsedAtlasBounds =
                            MsdfBounds.fromJson(jsonObject.getAsJsonObject("atlasBounds"));
                    break label37;
                }
            }

            parsedAtlasBounds = null;
        }

        MsdfBounds atlasBounds = parsedAtlasBounds;
        return new MsdfGlyphData(unicode, advance, planeBounds, atlasBounds);
    }
}
