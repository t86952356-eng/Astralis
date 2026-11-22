package astralis.module.impl.bedwars.mode;

import astralis.setting.BooleanSetting;

public record AlertItem(
        boolean armor,
        boolean byDisplayName,
        String itemKey,
        String label,
        BooleanSetting setting,
        String dedupeKey) {

    static AlertItem forItem(String itemKey, String label, BooleanSetting booleanSetting) {

        return new AlertItem(false, false, itemKey, label, booleanSetting, "item:" + itemKey);
    }

    static AlertItem forDisplayName(
            String displayName, String label, BooleanSetting booleanSetting) {

        return new AlertItem(
                false, true, displayName, label, booleanSetting, "display:" + displayName);
    }

    public String getDedupeKey() {
        return this.dedupeKey;
    }

    boolean matchesItemKey(String candidateKey) {
        String normalizedKey = candidateKey == null ? "" : candidateKey;
        if (!this.itemKey.equals(normalizedKey)) {

            if (this.itemKey.equals("fire_charge")) {

                if (normalizedKey.equals("fireball")) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public BooleanSetting getSetting() {
        return this.setting;
    }

    boolean matchesName(String name) {
        return this.itemKey.equals(name);
    }

    public String getItemKey() {
        return this.itemKey;
    }

    public boolean isByDisplayName() {
        return this.byDisplayName;
    }

    public boolean isArmor() {
        return this.armor;
    }

    public String getLabel() {
        return this.label;
    }

    static AlertItem forArmor(String itemKey, String label, BooleanSetting booleanSetting) {

        return new AlertItem(true, false, itemKey, label, booleanSetting, "item:" + itemKey);
    }
}
