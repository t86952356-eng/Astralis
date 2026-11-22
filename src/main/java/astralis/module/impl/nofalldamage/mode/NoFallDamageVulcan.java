package astralis.module.impl.nofalldamage.mode;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.MotionEvent;
import astralis.module.impl.NoFallDamage;
import astralis.module.impl.world.Scaffold;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.BreakUtil;

public class NoFallDamageVulcan extends Mode<NoFallDamage> {
    private final ModeSetting mode;
    private final NumberSetting motion;
    private int fallTicks;

    public NoFallDamageVulcan(NoFallDamage noFallDamage) {

        super("Vulcan", noFallDamage);

        ModeSetting modeSetting = new ModeSetting("5e7a9015ea584af4a04a409297fa5600");
        String[] modeNames = new String[2];

        modeNames[0] = "Vulcan A";

        modeNames[1] = "Vulcan B";
        ModeSetting modeSetting2 = modeSetting.addModes(modeNames);
        this.mode = modeSetting2.setDefault("Vulcan A");
        this.motion =
                new NumberSetting("04b3179de5d88360b561d10861012a5c", 10.0F, 0.1F, 10.0F, 0.1F);
        this.motion.hideIf(() -> !this.mode.is("Vulcan A"));
        this.addSettings(this.mode, this.motion);
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        String modeName = this.mode.getValue().getName();
        switch (modeName) {
            case "Vulcan A":
                if (mc.player.onGround()) {
                    this.fallTicks = 0;
                }

                if (mc.player.fallDistance - mc.player.getDeltaMovement().y > 3.0
                        && !Astralis.getInstance()
                                .getModuleManager()
                                .getModule(Scaffold.class)
                                .isEnabled()) {
                    this.fallTicks++;
                    if (this.fallTicks > 1) {
                        motionEvent.setOnGround(true);
                    }

                    if (this.fallTicks > 2) {
                        BreakUtil.setMotionY(-this.motion.getValue());
                    }
                }
                break;
            case "Vulcan B":
                if (mc.player.fallDistance >= 3.0) {
                    motionEvent.setOnGround(true);
                    mc.player.setDeltaMovement(
                            mc.player.getDeltaMovement().x,
                            -0.09800000190735147,
                            mc.player.getDeltaMovement().z);
                    mc.player.fallDistance = 0.0;
                }
        }
    }
}
