package astralis.metadata;

public class SettingMetadata {
    private final String moduleId;
    private final String settingId;
    private final String settingName;

    public SettingMetadata(String moduleId, String settingId, String settingName) {
        super();
        this.moduleId = moduleId;
        this.settingId = settingId;
        this.settingName = settingName;
    }

    public String getModuleId() {
        return this.moduleId;
    }

    public String getSettingName() {
        return this.settingName;
    }

    public String getSettingId() {
        return this.settingId;
    }
}
