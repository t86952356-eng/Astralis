package astralis.module.impl.arraylist;

import astralis.module.Module;

public record ArrayListEntry(Module module, String displayText, String unusedText, float width) {
    public Module getModule() {
        return this.module;
    }

    public String getUnusedText() {
        return this.unusedText;
    }

    public float getWidth() {
        return this.width;
    }
}
