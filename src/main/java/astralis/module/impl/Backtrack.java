package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.TickEvent;
import astralis.mixin.ClientboundMoveEntityPacketAccessor;
import astralis.module.Module;
import astralis.module.impl.combat.KillAura;
import astralis.setting.BooleanSetting;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;
import astralis.setting.NumberSetting;
import astralis.util.MathUtil;
import astralis.util.PacketUtil;
import astralis.util.render.RenderUtil;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
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
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Queue;

public class Backtrack extends Module {
    private volatile Player targetPlayer;
    private final Queue<Packet<?>> delayedPackets;
    private volatile Vec3 serverPosition;
    private volatile boolean delaying;
    private Vec3 ghostPosition;
    private long lastRenderTimeNanos;
    private final VecDeltaCodec positionCodec;
    private static final float GHOST_SMOOTHING_SPEED = 13.0F;
    private long delayStartTime;
    private final ModeSetting backtrackMode;
    private final NumberSetting maxDelay;
    private final ColorSetting ghostColor;
    private final BooleanSetting smart;
    private final BooleanSetting rangeLimit;
    private final NumberSetting maxRange;

    public Backtrack() {

        super(MODULE_ID, "", "", Category.Misc);
        this.delayedPackets = new ArrayDeque<>();
        this.positionCodec = new VecDeltaCodec();

        ModeSetting modeSetting = new ModeSetting(MODE_SETTING_ID);
        String[] modes = new String[2];

        modes[0] = "Limited";

        modes[1] = "Limitless";
        ModeSetting modeSetting2 = modeSetting.addModes(modes);
        this.backtrackMode = modeSetting2.setDefault("Limitless");

        this.maxDelay = new NumberSetting(MAX_DELAY_SETTING_ID, 200.0F, 50.0F, 1000.0F, 10.0F);

        this.ghostColor = new ColorSetting(GHOST_SETTING_ID, Color.white);

        this.smart = new BooleanSetting(SMART_SETTING_ID, true);

        this.rangeLimit = new BooleanSetting(RANGE_LIMIT_SETTING_ID, true);

        this.maxRange = new NumberSetting(MAX_RANGE_SETTING_ID, 3.0F, 0.1F, 6.0F, 0.1F);
        this.maxDelay.hideIf(() -> !this.backtrackMode.is("Limited"));
        this.maxRange.hideIf(() -> !this.rangeLimit.getValue());

        this.addSettings(
                this.backtrackMode,
                this.maxDelay,
                this.ghostColor,
                this.smart,
                this.rangeLimit,
                this.maxRange);
    }

    @Override
    public String getSuffix() {

        String suffix;
        if (this.backtrackMode.is("Limited")) {
            String delayText = String.valueOf(this.maxDelay.getValue());

            suffix = delayText + " ms";
        } else {
            suffix = this.backtrackMode.getValue().getName();
        }

        return suffix;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() != EventStage.POST) {
            if (!mc.player.isDeadOrDying()
                    && (mc.gui.screen() == null
                            || Astralis.getInstance()
                                    .getModuleManager()
                                    .getModule(ClickGui.class)
                                    .isClickGuiScreen(mc.gui.screen()))
                    && !(mc.gui.screen() instanceof LevelLoadingScreen)) {

                if (this.backtrackMode.is("Limited")) {
                    synchronized (this.delayedPackets) {
                        if (!this.delayedPackets.isEmpty()
                                && System.currentTimeMillis() - this.delayStartTime
                                        >= this.maxDelay.getValue().doubleValue()) {
                            this.flushDelayedPackets();
                        }
                    }
                }

                KillAura killAura =
                        Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                Player player2 = null;
                if (killAura.isEnabled() && killAura.target != null) {
                    if (killAura.target instanceof Player player3) {
                        player2 = player3;
                    }
                } else {
                    for (AbstractClientPlayer abstractClientPlayer : mc.level.players()) {
                        if (abstractClientPlayer != mc.player
                                && abstractClientPlayer.isAlive()
                                && mc.player.closerThan(abstractClientPlayer, 4.0)) {
                            player2 = abstractClientPlayer;
                        }
                    }
                }

                synchronized (this.delayedPackets) {
                    if (this.targetPlayer != player2) {
                        this.targetPlayer = player2;
                        this.flushDelayedPackets();
                    }
                }

                if (player2 != null) {
                    if (this.serverPosition != null
                            && !this.isDelayBeneficial(player2, this.serverPosition)) {
                        this.flushDelayedPackets();
                    }

                    if (this.rangeLimit.getValue() && this.serverPosition != null) {
                        double distance = player2.position().distanceTo(this.serverPosition);
                        if (distance > this.maxRange.getValue().floatValue()) {
                            this.flushDelayedPackets();
                        }
                    }
                }
            } else {
                this.releaseTarget();
            }
        }
    }

    private boolean isDelayBeneficial(Player player2, Vec3 vector3) {
        return !this.smart.getValue()
                || mc.player.distanceToSqr(player2.position()) < mc.player.distanceToSqr(vector3);
    }

    public void flushDelayedPackets() {
        synchronized (this.delayedPackets) {
            this.delaying = false;
            this.serverPosition = null;
            this.ghostPosition = null;
            this.lastRenderTimeNanos = 0L;
            this.delayStartTime = 0L;
            Packet packet;
            while ((packet = this.delayedPackets.poll()) != null) {
                PacketUtil.receiveNoEvent(packet);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        Player player2 = this.targetPlayer;
        Vec3 vector3 = this.serverPosition;
        if (this.delaying && player2 != null && vector3 != null) {
            long now = System.nanoTime();
            double deltaSeconds =
                    this.lastRenderTimeNanos == 0L ? 0.0 : (now - this.lastRenderTimeNanos) / 1.0E9;
            this.lastRenderTimeNanos = now;
            if (this.ghostPosition != null
                    && !(this.ghostPosition.distanceToSqr(vector3) > 400.0)) {
                float smoothing = MathUtil.smoothingFactor(13.0F, deltaSeconds);
                this.ghostPosition =
                        new Vec3(
                                Mth.lerp(smoothing, this.ghostPosition.x, vector3.x),
                                Mth.lerp(smoothing, this.ghostPosition.y, vector3.y),
                                Mth.lerp(smoothing, this.ghostPosition.z, vector3.z));
            } else {
                this.ghostPosition = vector3;
            }

            double width = player2.getBbWidth();
            double height = player2.getBbHeight();
            AABB boundingBox =
                    new AABB(
                            this.ghostPosition.x - width / 2.0,
                            this.ghostPosition.y,
                            this.ghostPosition.z - width / 2.0,
                            this.ghostPosition.x + width / 2.0,
                            this.ghostPosition.y + height,
                            this.ghostPosition.z + width / 2.0);
            RenderUtil.drawSettingBox(render3DEvent.getPoseStack(), boundingBox, this.ghostColor);
        } else {
            this.ghostPosition = null;
            this.lastRenderTimeNanos = 0L;
        }
    }

    @Override
    public void onDisable() {
        this.releaseTarget();
        this.ghostPosition = null;
        this.lastRenderTimeNanos = 0L;
    }

    private Vec3 getPositionFromPacket(Packet<?> packet, Player player2) {
        if (packet instanceof ClientboundAddEntityPacket clientboundAddEntityPacket
                && clientboundAddEntityPacket.getId() == player2.getId()) {
            return new Vec3(
                    clientboundAddEntityPacket.getX(),
                    clientboundAddEntityPacket.getY(),
                    clientboundAddEntityPacket.getZ());
        } else if (packet
                        instanceof
                        ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket
                && clientboundEntityPositionSyncPacket.id() == player2.getId()) {
            return clientboundEntityPositionSyncPacket.values().position();
        } else if (packet instanceof ClientboundTeleportEntityPacket clientboundTeleportEntityPacket
                && clientboundTeleportEntityPacket.id() == player2.getId()) {
            Vec3 vector3 = this.serverPosition == null ? player2.position() : this.serverPosition;
            PositionMoveRotation positionMoveRotation =
                    new PositionMoveRotation(vector3, Vec3.ZERO, 0.0F, 0.0F);
            return PositionMoveRotation.calculateAbsolute(
                            positionMoveRotation,
                            clientboundTeleportEntityPacket.change(),
                            clientboundTeleportEntityPacket.relatives())
                    .position();
        } else if (packet instanceof ClientboundMoveEntityPacket clientboundMoveEntityPacket
                && ((ClientboundMoveEntityPacketAccessor) clientboundMoveEntityPacket).getEntityId()
                        == player2.getId()) {
            Vec3 vector32 = this.serverPosition == null ? player2.position() : this.serverPosition;
            this.positionCodec.setBase(vector32);
            return this.positionCodec.decode(
                    clientboundMoveEntityPacket.getXa(),
                    clientboundMoveEntityPacket.getYa(),
                    clientboundMoveEntityPacket.getZa());
        } else {
            return this.serverPosition;
        }
    }

    private boolean isLocalDamagePacket(Packet<?> packet) {
        return packet instanceof ClientboundHurtAnimationPacket
                ? true
                : packet instanceof ClientboundDamageEventPacket clientboundDamageEventPacket
                        && clientboundDamageEventPacket.entityId() == mc.player.getId();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE) {
            Packet packet = packetEvent.getPacket();
            if (!(packet instanceof ClientboundLoginPacket)
                    && !(packet instanceof ClientboundRespawnPacket)
                    && !(packet instanceof ClientboundPlayerPositionPacket)
                    && !(packet instanceof ClientboundSetHealthPacket clientboundSetHealthPacket
                            && clientboundSetHealthPacket.getHealth() <= 0.0F)
                    && !(packet
                                    instanceof
                                    ClientboundPlayerCombatKillPacket
                                            clientboundPlayerCombatKillPacket
                            && clientboundPlayerCombatKillPacket.playerId() == mc.player.getId())) {
                if (this.targetPlayer != null) {
                    if (this.isLocalDamagePacket(packet)) {
                        this.flushDelayedPackets();
                        return;
                    }

                    if (packet instanceof ClientboundDisconnectPacket
                            || packet instanceof ClientboundSetScorePacket
                            || packet instanceof ClientboundResetScorePacket
                            || packet instanceof ClientboundSetObjectivePacket
                            || packet instanceof ClientboundSetDisplayObjectivePacket
                            || packet instanceof ClientboundSetPlayerTeamPacket
                            || packet instanceof ClientboundPlayerChatPacket
                            || packet instanceof ClientboundSystemChatPacket
                            || packet instanceof ClientboundPlayerInfoUpdatePacket
                            || packet instanceof ClientboundSetHealthPacket
                            || packet instanceof ClientboundSetExperiencePacket
                            || packet instanceof ClientboundSetTimePacket
                            || packet instanceof ClientboundGameEventPacket
                            || packet instanceof ClientboundCooldownPacket
                            || packet instanceof ClientboundLevelChunkWithLightPacket
                            || packet instanceof ClientboundLightUpdatePacket
                            || packet instanceof ClientboundBlockUpdatePacket
                            || packet instanceof ClientboundSectionBlocksUpdatePacket
                            || packet instanceof ClientboundBlockEventPacket
                            || packet instanceof ClientboundExplodePacket
                            || packet instanceof ClientboundLevelParticlesPacket
                            || packet instanceof ClientboundContainerSetSlotPacket
                            || packet instanceof ClientboundContainerSetContentPacket
                            || packet instanceof ClientboundSetTitleTextPacket
                            || packet instanceof ClientboundSetSubtitleTextPacket
                            || packet instanceof ClientboundSetTitlesAnimationPacket
                            || packet instanceof ClientboundSoundPacket
                            || packet instanceof ClientboundStopSoundPacket
                            || packet instanceof ClientboundRemoveEntitiesPacket
                            || packet instanceof ClientboundUpdateTagsPacket
                            || packet instanceof ClientboundTrackedWaypointPacket
                            || packet instanceof ClientboundEntityEventPacket) {
                        return;
                    }

                    synchronized (this.delayedPackets) {
                        Player player2 = this.targetPlayer;
                        if (player2 == null) {
                            return;
                        }

                        if (this.delayedPackets.isEmpty()) {
                            this.delayStartTime = System.currentTimeMillis();
                        }

                        Vec3 vector3 = this.getPositionFromPacket(packet, player2);
                        if (vector3 != null && !this.isDelayBeneficial(player2, vector3)) {
                            this.flushDelayedPackets();
                            this.serverPosition = vector3;
                            this.positionCodec.setBase(vector3);
                            return;
                        }

                        if (vector3 != null) {
                            this.serverPosition = vector3;
                            this.positionCodec.setBase(vector3);
                        }

                        packetEvent.setCancelled(true);
                        this.delayedPackets.add(packet);
                        this.delaying = true;
                    }
                }
            } else {
                this.releaseTarget();
            }
        }
    }

    private void releaseTarget() {
        synchronized (this.delayedPackets) {
            this.targetPlayer = null;
            this.flushDelayedPackets();
        }
    }

    private static final String MODULE_ID = "69881df3582a5e5d34dd9e71d434756a";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String MAX_DELAY_SETTING_ID = "3be6618cb4870949e9d15a943c4a7c57";
    private static final String GHOST_SETTING_ID = "5aaff00d6f20e78676e59dce6f760334";
    private static final String SMART_SETTING_ID = "5087f86f2fb2ea5621cb4980df5bee24";
    private static final String RANGE_LIMIT_SETTING_ID = "4c428e462f8c22760f1ee47370c4343e";
    private static final String MAX_RANGE_SETTING_ID = "39c936e96e6bdbfab0bfe93cf9deeed8";
}
