package astralis.util.render.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MsdfFontData {
    private MsdfAtlasInfo atlas;
    private MsdfFontMetrics metrics;
    private List<MsdfGlyphData> glyphs;
    private List<MsdfKerningPair> kerningPairs;

    public MsdfFontData(
            MsdfAtlasInfo atlas,
            MsdfFontMetrics metrics,
            List<MsdfGlyphData> glyphs,
            List<MsdfKerningPair> kerningPairs) {
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kerningPairs = kerningPairs;
    }

    public List<MsdfKerningPair> getKerningPairs() {
        return this.kerningPairs;
    }

    public List<MsdfGlyphData> getGlyphs() {
        return this.glyphs;
    }

    public static MsdfFontData fromJson(JsonObject jsonObject) {

        MsdfAtlasInfo atlasValue;
        if (jsonObject.has("atlas")) {

            atlasValue = MsdfAtlasInfo.fromJson(jsonObject.getAsJsonObject("atlas"));
        } else {
            atlasValue = null;
        }

        MsdfAtlasInfo atlas = atlasValue;

        MsdfFontMetrics metricsValue;
        if (jsonObject.has("metrics")) {

            metricsValue = MsdfFontMetrics.fromJson(jsonObject.getAsJsonObject("metrics"));
        } else {
            metricsValue = null;
        }

        MsdfFontMetrics metrics = metricsValue;
        ArrayList glyphs = new ArrayList();

        if (jsonObject.has("glyphs")) {

            for (JsonElement jsonElement : jsonObject.getAsJsonArray("glyphs")) {
                glyphs.add(MsdfGlyphData.fromJson(jsonElement.getAsJsonObject()));
            }
        }

        ArrayList kerningPairs = new ArrayList();

        if (jsonObject.has("kerning")) {

            for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("kerning")) {
                kerningPairs.add(MsdfKerningPair.fromJson(jsonElement2.getAsJsonObject()));
            }
        }

        return new MsdfFontData(atlas, metrics, glyphs, kerningPairs);
    }

    public MsdfFontMetrics getMetrics() {
        return this.metrics;
    }

    public MsdfAtlasInfo getAtlas() {
        return this.atlas;
    }
}
