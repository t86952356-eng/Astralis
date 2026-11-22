package astralis.module.impl;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.AttackEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.VelocityEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.Module;
import astralis.module.impl.knockbackdelay.DelayedPacketAction;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import pub.astralis.client.module.core.Category;

import java.util.concurrent.ConcurrentLinkedQueue;

public class KnockbackDelay extends Module {
    private final NumberSetting airDelay;
    private final NumberSetting groundDelay;
    private final BooleanSetting dontDelayOnGround;
    private final ConcurrentLinkedQueue<DelayedPacketAction> delayedPackets;
    private long delayStartTime;
    private boolean startedOnGround;

    public KnockbackDelay() {

        super(MODULE_ID, "", "", Category.Misc);

        this.airDelay = new NumberSetting(AIR_DELAY_SETTING_ID, 100.0F, 0.0F, 1000.0F, 10.0F);

        this.groundDelay = new NumberSetting(GROUND_DELAY_SETTING_ID, 150.0F, 0.0F, 1000.0F, 10.0F);

        this.dontDelayOnGround = new BooleanSetting(DON_T_DELAY_ON_GROUND_SETTING_ID, false);
        this.delayedPackets = new ConcurrentLinkedQueue<>();

        this.delayStartTime = -1L;
        this.startedOnGround = false;
        this.addSettings(this.airDelay, this.groundDelay, this.dontDelayOnGround);
    }

    @EventTarget
    public void onAttack(AttackEvent attackEvent) {
        if (attackEvent.getStage() == EventStage.PRE) {}
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {

        if (this.delayStartTime != -1L && packetEvent.getStage() == EventStage.RECEIVE) {
            if (!(packetEvent.getPacket() instanceof ClientboundDisconnectPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetScorePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundResetScorePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetObjectivePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetDisplayObjectivePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetPlayerTeamPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundPlayerChatPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSystemChatPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundPlayerInfoUpdatePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetHealthPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetExperiencePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetTimePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundGameEventPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundCooldownPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundLevelChunkWithLightPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundLightUpdatePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundBlockUpdatePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSectionBlocksUpdatePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundBlockEventPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundExplodePacket)
                    && !(packetEvent.getPacket() instanceof ClientboundLevelParticlesPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundContainerSetSlotPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundContainerSetContentPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetTitleTextPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetSubtitleTextPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSetTitlesAnimationPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundSoundPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundStopSoundPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundAddEntityPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundRemoveEntitiesPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundUpdateTagsPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundEntityEventPacket)
                    && !(packetEvent.getPacket() instanceof ClientboundBossEventPacket)) {
                packetEvent.setCancelled(true);
                Packet packet = packetEvent.getPacket();
                this.delayedPackets.add(() -> PacketUtil.receiveNoEvent(packet));
            }
        }
    }

    @EventTarget
    public void onVelocity(VelocityEvent velocityEvent) {
        this.beginDelay();
    }

    @Override
    public String getSuffix() {
        return this.airDelay.getValue().toString();
    }

    private void beginDelay() {

        if (this.delayStartTime == -1L) {
            this.startedOnGround = mc.player.onGround();
            if (!this.dontDelayOnGround.getValue() || !this.startedOnGround) {
                this.delayStartTime = System.currentTimeMillis();
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.releaseDelayedPackets();
    }

    private void releaseDelayedPackets() {

        this.delayStartTime = -1L;

        while (!this.delayedPackets.isEmpty()) {
            DelayedPacketAction action = this.delayedPackets.poll();
            if (action != null) {
                action.execute();
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.PRE) {

            if (this.delayStartTime != -1L) {
                long elapsed = System.currentTimeMillis() - this.delayStartTime;
                float requiredDelay =
                        this.startedOnGround
                                ? this.groundDelay.getValue()
                                : this.airDelay.getValue();
                if ((float) elapsed >= requiredDelay) {
                    this.releaseDelayedPackets();
                }

                return;
            }
        }
    }

    @Override
    public void onDisable() {
        this.releaseDelayedPackets();
    }

    private static final String MODULE_ID = "e8da4488786c0ab8e3180972300a8714";
    private static final String AIR_DELAY_SETTING_ID = "5bbd48f58cc7bace69a5154cc2efda37";
    private static final String GROUND_DELAY_SETTING_ID = "d6612da409d45f4bc35702cfe842d5c9";
    private static final String DON_T_DELAY_ON_GROUND_SETTING_ID =
            "53fdd6c7995a83277ce8f897fc9d71fc6de2f62d74083fdac147ecab03033151";
}
