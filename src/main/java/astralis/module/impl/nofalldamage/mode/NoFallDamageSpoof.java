package astralis.module.impl.nofalldamage.mode;

import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.NoFallDamage;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;

public class NoFallDamageSpoof extends Mode<NoFallDamage> {
    private final ModeSetting groundMode;

    public NoFallDamageSpoof(NoFallDamage noFallDamage) {

        super("Spoof", noFallDamage);

        ModeSetting modeSetting = new ModeSetting("38193054d7346dc3be8748fcd89c8f2c");
        String[] modeNames = new String[2];

        modeNames[0] = "Off Ground";

        modeNames[1] = "On Ground";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.groundMode = modeSetting2.setDefault("On Ground");
        this.addSettings(this.groundMode);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        String groundModeName = this.groundMode.getValue().getName();
        switch (groundModeName) {
            case "Off Ground":
                motionEvent.setOnGround(false);
                break;
            case "On Ground":
                motionEvent.setOnGround(true);
        }
    }
}
