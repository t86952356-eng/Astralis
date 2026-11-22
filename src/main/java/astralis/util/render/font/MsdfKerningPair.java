package astralis.util.render.font;

import com.google.gson.JsonObject;

import lombok.Generated;

public class MsdfKerningPair {
    private int firstUnicode;
    private int secondUnicode;
    private float advance;

    public static MsdfKerningPair fromJson(JsonObject jsonObject) {

        int firstUnicodeValue;
        if (jsonObject.has("unicode1")) {

            firstUnicodeValue = jsonObject.get("unicode1").getAsInt();
        } else {
            firstUnicodeValue = 0;
        }

        int firstUnicode = firstUnicodeValue;

        int secondUnicodeValue;
        if (jsonObject.has("unicode2")) {

            secondUnicodeValue = jsonObject.get("unicode2").getAsInt();
        } else {
            secondUnicodeValue = 0;
        }

        int secondUnicode = secondUnicodeValue;

        float advanceValue;
        if (jsonObject.has("advance")) {

            advanceValue = jsonObject.get("advance").getAsFloat();
        } else {
            advanceValue = 0.0F;
        }

        float advance = advanceValue;
        return new MsdfKerningPair(firstUnicode, secondUnicode, advance);
    }

    @Generated
    public int getFirstUnicode() {
        return this.firstUnicode;
    }

    @Generated
    public int getSecondUnicode() {
        return this.secondUnicode;
    }

    @Generated
    public float getAdvance() {
        return this.advance;
    }

    @Generated
    public MsdfKerningPair(int firstUnicode, int secondUnicode, float advance) {
        this.firstUnicode = firstUnicode;
        this.secondUnicode = secondUnicode;
        this.advance = advance;
    }
}
