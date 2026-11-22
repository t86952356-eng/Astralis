package astralis.module.impl.targethud.mode;

public record AdjustCardInfo(
        String name, String healthDiffText, float width, float healthDiffWidth) {
    public String getName() {
        return this.name;
    }

    public String getHealthDiffText() {
        return this.healthDiffText;
    }

    public float getWidth() {
        return this.width;
    }

    public float getHealthDiffWidth() {
        return this.healthDiffWidth;
    }
}
