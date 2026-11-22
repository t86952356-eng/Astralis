package astralis.module.impl.criticals.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.Criticals;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.PacketDelay;

public class CriticalsLag extends Mode<Criticals> {
    private final NumberSetting delay;

    public CriticalsLag(Criticals criticals) {

        super("Lag", criticals);

        this.delay =
                new NumberSetting(
                        "960f91ff141f21b3427418599a8603ec", 150.0F, 50.0F, 1000.0F, 50.0F);
        this.addSettings(this.delay);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            if (this.getParent().shouldSkipCrit()) {
                if (PacketDelay.isDelaying()) {
                    PacketDelay.stopAndFlush();
                }
            } else {
                boolean falling =
                        !mc.player.onGround()
                                && mc.player.getDeltaMovement().y < 0.0
                                && mc.player.fallDistance > 0.0;
                if (falling && !PacketDelay.isDelaying()) {
                    PacketDelay.startDelay(this.delay.getValue().longValue());
                } else if (!falling && PacketDelay.isDelaying()) {
                    PacketDelay.stopAndFlush();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (PacketDelay.isDelaying()) {
            PacketDelay.stopAndFlush();
        }
    }
}
