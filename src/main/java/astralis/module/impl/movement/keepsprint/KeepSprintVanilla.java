package astralis.module.impl.movement.keepsprint;

import astralis.module.impl.movement.KeepSprint;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;

/** Restricts sprint preservation to the selected Ground, Air, or Both environment. */
public final class KeepSprintVanilla extends Mode<KeepSprint> {
    private final ModeSetting activeEnvironment;

    public KeepSprintVanilla(KeepSprint parent) {

        super("Vanilla", parent);

        ModeSetting environmentSetting = new ModeSetting(ACTIVE_IF_SETTING_ID);
        String[] environments = new String[3];

        environments[0] = "Ground";

        environments[1] = "Air";

        environments[2] = "Both";
        ModeSetting configuredEnvironmentSetting = environmentSetting.addModes(environments);
        this.activeEnvironment = configuredEnvironmentSetting.setDefault("Both");
        this.addSettings(this.activeEnvironment);
    }

    public boolean shouldKeepSprint() {
        String environmentName = ((Mode) this.activeEnvironment.getValue()).getName();
        int environmentIndex = -1;
        switch (environmentName.hashCode()) {
            case 2141373863:
                {
                    if (!environmentName.equals("Ground")) break;
                    environmentIndex = 0;
                    break;
                }
            case 65834:
                {
                    if (!environmentName.equals("Air")) break;
                    environmentIndex = 1;
                    break;
                }
            case 0x1FAFA1:
                {
                    if (!environmentName.equals("Both")) break;
                    environmentIndex = 2;
                }
        }
        boolean keepSprint =
                switch (environmentIndex) {
                    case 0 -> KeepSprintVanilla.mc.player.onGround();
                    case 1 -> {
                        if (!KeepSprintVanilla.mc.player.onGround()) {
                            yield true;
                        }
                        yield false;
                    }
                    case 2 -> true;
                    default -> false;
                };
        return keepSprint;
    }

    private static final String ACTIVE_IF_SETTING_ID = "c56d75bbfee5f56c092592c8cd8ac7bd";
}
