package astralis.module.impl.airstuck.mode;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.TravelEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.module.impl.AirStuck;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;

import java.util.Locale;

public class AirStuckWatchdog extends Mode<AirStuck> {
    private final BooleanSetting resend;
    private final NumberSetting delaySeconds;
    private final NumberSetting releaseTicks;
    private long stuckStartTime;
    private int releaseTicksLeft;
    private boolean positionSyncPending;

    public AirStuckWatchdog(AirStuck airStuck) {

        super("Watchdog", airStuck);

        this.resend = new BooleanSetting("f9924f91dc1b73899e17045d6b341f06", true);

        this.delaySeconds =
                new NumberSetting("bf2eca0aa554ea402c47749d03df186b", 1.0F, 1.0F, 4.0F, 0.1F);

        this.releaseTicks =
                new NumberSetting("493c1171883283eb97c52cddf6a3c5c9", 4.0F, 1.0F, 20.0F, 1.0F);
        this.delaySeconds.hideIf(() -> !this.resend.getValue());
        this.releaseTicks.hideIf(() -> !this.resend.getValue());
        this.addSettings(this.resend, this.delaySeconds, this.releaseTicks);
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.resend.getValue()) {
            this.releaseTicksLeft = 0;
        } else if (this.releaseTicksLeft > 0) {
            if (--this.releaseTicksLeft == 0) {
                this.stuckStartTime = System.currentTimeMillis();
            }
        } else if ((float) (System.currentTimeMillis() - this.stuckStartTime)
                >= this.delaySeconds.getValue() * 1000.0F) {
            this.releaseTicksLeft = this.releaseTicks.getValue().intValue();
        }
    }

    @EventTarget
    public void onVanillaHudRender(VanillaHudRenderEvent event) {
        if (mc.options.getCameraType().isFirstPerson() && this.resend.getValue()) {
            long delayMillis = (long) (this.delaySeconds.getValue() * 1000.0F);
            long remainingMillis =
                    !this.resend.getValue()
                            ? 0L
                            : (this.releaseTicksLeft > 0
                                    ? 0L
                                    : Math.max(
                                            0L,
                                            delayMillis
                                                    - (System.currentTimeMillis()
                                                            - this.stuckStartTime)));

            String remainingText = String.format(Locale.ROOT, "%.1fs", remainingMillis / 1000.0);
            GuiGraphicsExtractor guiGraphicsExtractor = event.getGraphics();
            Font font2 = mc.font;
            int centerX = event.getGraphics().guiWidth() / 2;
            int centerY = event.getGraphics().guiHeight() / 2;
            guiGraphicsExtractor.centeredText(font2, remainingText, centerX, centerY + 18, -1);
        }
    }

    @EventTarget
    public void onTravel(TravelEvent event) {
        if (this.releaseTicksLeft == 0) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE
                && packetEvent.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.positionSyncPending = true;
        } else if (packetEvent.getStage() == EventStage.SEND
                && packetEvent.getPacket()
                        instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            if (this.positionSyncPending) {
                this.positionSyncPending = false;
            } else {
                if (this.releaseTicksLeft == 0 && !(serverboundMovePlayerPacket instanceof Rot)) {
                    packetEvent.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        moveInputEvent.setForward(false);
        moveInputEvent.setBackward(false);
        moveInputEvent.setLeft(false);
        moveInputEvent.setRight(false);
        moveInputEvent.setJump(false);
    }

    @Override
    public void onEnable() {
        this.stuckStartTime = System.currentTimeMillis();
        this.releaseTicksLeft = 0;
        this.positionSyncPending = false;
    }
}
