package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.Module;
import astralis.module.impl.anticheat.ExternalTagLookup;
import astralis.module.impl.anticheat.check.AutoBlockCheck;
import astralis.module.impl.anticheat.check.BotCheck;
import astralis.module.impl.anticheat.check.Check;
import astralis.module.impl.anticheat.check.CheckContext;
import astralis.module.impl.anticheat.check.FlagReporter;
import astralis.module.impl.anticheat.check.FlyCheck;
import astralis.module.impl.anticheat.check.KillAuraCheck;
import astralis.module.impl.anticheat.check.PlayerCheckState;
import astralis.module.impl.anticheat.check.ReachCheck;
import astralis.module.impl.anticheat.check.ScaffoldCheck;
import astralis.module.impl.anticheat.check.ScaffoldPacketCheck;
import astralis.module.impl.anticheat.check.SpeedCheck;
import astralis.module.impl.anticheat.check.VelocityCheck;
import astralis.setting.BooleanSetting;
import astralis.setting.NumberSetting;
import astralis.setting.Setting;
import astralis.util.PlayerListManager;
import astralis.util.player.TeamUtil;

import com.google.gson.JsonObject;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.player.Player;

import pub.astralis.client.module.core.Category;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player movement history and dispatches enabled client-side detection checks.
 */
public class AntiCheat extends Module {
    private final BooleanSetting checkKillAura;
    private final BooleanSetting checkVelocity;
    private final BooleanSetting checkSpeed;
    private final BooleanSetting checkFlight;
    private final BooleanSetting checkReach;
    private final BooleanSetting checkBots;
    private final BooleanSetting checkScaffold;
    private final BooleanSetting checkAutoBlock;
    private final BooleanSetting checkExternalTags;
    private final NumberSetting sensitivity;
    private final BooleanSetting autoMarkHacker;
    private final BooleanSetting autoTarget;
    private final BooleanSetting showFlagToasts;
    private final BooleanSetting checkSelf;
    private final FlagReporter flagReporter;
    private final BotCheck botFilter;
    private final ExternalTagLookup externalTagLookup;
    private final List<Check> checks;
    private final Map<UUID, PlayerCheckState> playerStates;

    public AntiCheat() {

        super(MODULE_ID, "", "", Category.Misc);

        this.checkKillAura = new BooleanSetting(KILL_AURA_SETTING_ID, true);

        this.checkVelocity = new BooleanSetting(VELOCITY_SETTING_ID, true);

        this.checkSpeed = new BooleanSetting(SPEED_SETTING_ID, true);

        this.checkFlight = new BooleanSetting(FLY_SETTING_ID, true);

        this.checkReach = new BooleanSetting(REACH_SETTING_ID, true);

        this.checkBots = new BooleanSetting(BOT_SETTING_ID, true);

        this.checkScaffold = new BooleanSetting(SCAFFOLD_SETTING_ID, true);

        this.checkAutoBlock = new BooleanSetting(AUTO_BLOCK_SETTING_ID, true);

        this.checkExternalTags = new BooleanSetting(URCHIN_SERAPH_SETTING_ID, true);

        this.sensitivity = new NumberSetting(SENSITIVITY_SETTING_ID, 5.0F, 1.0F, 10.0F, 1.0F);

        this.autoMarkHacker = new BooleanSetting(AUTO_MARK_HACKER_SETTING_ID, true);

        this.autoTarget = new BooleanSetting(AUTO_TARGET_SETTING_ID, false);

        this.showFlagToasts = new BooleanSetting(TOAST_ON_FLAG_SETTING_ID, true);

        this.checkSelf = new BooleanSetting(CHECK_SELF_SETTING_ID, false);
        this.flagReporter =
                new FlagReporter(this.showFlagToasts, this.autoMarkHacker, this.autoTarget);
        this.botFilter = new BotCheck(this, this.checkBots);
        this.externalTagLookup =
                new ExternalTagLookup(this, this.checkExternalTags, this.showFlagToasts);
        // Preserve check order: evidence and flag reporting can share player state.
        this.checks =
                List.of(
                        new KillAuraCheck(this, this.checkKillAura),
                        new VelocityCheck(this, this.checkVelocity),
                        new SpeedCheck(this, this.checkSpeed),
                        new FlyCheck(this, this.checkFlight),
                        new ReachCheck(this, this.checkReach),
                        new ScaffoldCheck(this, this.checkScaffold),
                        new ScaffoldPacketCheck(this, this.checkScaffold),
                        new AutoBlockCheck(this, this.checkAutoBlock));
        this.playerStates = new ConcurrentHashMap<>();

        Setting[] orderedSettings =
                new Setting[] {
                    this.checkKillAura,
                    this.checkVelocity,
                    this.checkSpeed,
                    this.checkFlight,
                    this.checkReach,
                    this.checkBots,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                };

        orderedSettings[6] = this.checkScaffold;

        orderedSettings[7] = this.checkAutoBlock;

        orderedSettings[8] = this.checkExternalTags;

        orderedSettings[9] = this.sensitivity;

        orderedSettings[10] = this.autoMarkHacker;

        orderedSettings[11] = this.autoTarget;

        orderedSettings[12] = this.showFlagToasts;

        orderedSettings[13] = this.checkSelf;
        this.addSettings(orderedSettings);
    }

    private PlayerCheckState findStateByEntityId(int entityId) {
        return mc.level.getEntity(entityId) instanceof Player player
                ? this.playerStates.get(player.getUUID())
                : null;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE && mc.player != null && mc.level != null) {
            PlayerListManager playerLists =
                    Astralis.getInstance().getManagerRegistry().getManager(PlayerListManager.class);
            HashSet presentPlayerIds = new HashSet();

            for (Player player : mc.level.players()) {
                presentPlayerIds.add(player.getUUID());
                boolean isLocalPlayer = player == mc.player;
                if ((!isLocalPlayer || this.checkSelf.getValue())
                        && player.isAlive()
                        && (isLocalPlayer || !player.isInvisible())) {
                    if (!isLocalPlayer) {
                        this.externalTagLookup.queue(player.getName().getString());
                    }

                    if (isLocalPlayer
                            || !playerLists.isFriend(player) && !TeamUtil.isTeammate(player)) {
                        PlayerCheckState state =
                                this.playerStates.computeIfAbsent(
                                        player.getUUID(), playerId -> new PlayerCheckState());
                        // Refresh filtered/uninitialized players without running detections.
                        if (!isLocalPlayer && this.botFilter.isBot(player, state)) {
                            state.recordSnapshot(player);
                        } else if (!state.hasSnapshot) {
                            state.recordSnapshot(player);
                        } else {
                            double deltaX = player.getX() - state.previousX;
                            double deltaY = player.getY() - state.previousY;
                            double deltaZ = player.getZ() - state.previousZ;
                            // Teleports and large position jumps suspend checks while history
                            // resets.
                            if (state.checkSuppressionTicks <= 0
                                    && !(Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) > 3.0)
                                    && !(Math.abs(deltaY) > 3.0)) {
                                if (state.motionExemptionTicks > 0) {
                                    state.motionExemptionTicks--;
                                }

                                state.airborneTicks =
                                        player.onGround() ? 0 : state.airborneTicks + 1;
                                CheckContext checkContext =
                                        CheckContext.create(
                                                player,
                                                state,
                                                isLocalPlayer,
                                                this.sensitivity.getValue());

                                for (Check check : this.checks) {
                                    check.inspectPlayer(checkContext);
                                }

                                state.previousHorizontalSpeed =
                                        checkContext.getHorizontalDistance();
                                state.previousYaw = player.getYRot();
                                state.previousPitch = player.getXRot();
                                state.recordSnapshot(player);
                            } else {
                                state.checkSuppressionTicks =
                                        Math.max(0, state.checkSuppressionTicks - 1);
                                state.relaxMovementBuffers();
                                state.recordSnapshot(player);
                            }
                        }
                    }
                }
            }

            this.playerStates
                    .keySet()
                    .removeIf(
                            playerIdToRemove -> {
                                return !presentPlayerIds.contains(playerIdToRemove);
                            });
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        this.playerStates.clear();
        this.externalTagLookup.resetLookupState();
    }

    @Override
    public void onDisable() {
        this.playerStates.clear();
        this.externalTagLookup.resetLookupState();
    }

    /** Compatibility accessor for the shared flag reporter. */
    public FlagReporter getFlagReporter() {
        return this.flagReporter;
    }

    /** Compatibility accessor for an immutable snapshot of players marked by the reporter. */
    public Set<UUID> getMarkedPlayerIds() {
        return this.flagReporter.getMarkedPlayerIds();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        Packet packet = packetEvent.getPacket();
        if (packetEvent.getStage() != EventStage.SEND) {
            if (packetEvent.getStage() == EventStage.RECEIVE) {
                if (mc.player != null && mc.level != null) {
                    mc.execute(() -> this.handleServerPacket(packet));
                } else {
                    this.playerStates.clear();
                }
            }
        } else if (this.checkSelf.getValue() && mc.player != null && mc.level != null) {
            PlayerCheckState localPlayerState =
                    this.playerStates.computeIfAbsent(
                            mc.player.getUUID(), playerId -> new PlayerCheckState());

            for (Check check : this.checks) {
                check.inspectOutgoingPacket(packet, localPlayerState);
            }
        }
    }

    private void handleServerPacket(Packet<?> packet) {
        if (mc.player != null && mc.level != null) {
            if (packet instanceof ClientboundPlayerPositionPacket positionPacket
                    && positionPacket.id() == mc.player.getId()) {
                PlayerCheckState localPlayerState =
                        this.playerStates.computeIfAbsent(
                                mc.player.getUUID(), playerId -> new PlayerCheckState());

                // Retain the six-tick grace period after a local position correction.
                localPlayerState.checkSuppressionTicks =
                        Math.max(localPlayerState.checkSuppressionTicks, 6);
                localPlayerState.relaxMovementBuffers();
            } else if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
                PlayerCheckState motionState = this.findStateByEntityId(motionPacket.id());
                if (motionState != null) {

                    motionState.motionExemptionTicks =
                            Math.max(motionState.motionExemptionTicks, 8);
                    motionState.checkSuppressionTicks =
                            Math.max(motionState.checkSuppressionTicks, 2);
                }
            } else {
                if (packet instanceof ClientboundTeleportEntityPacket teleportPacket) {
                    PlayerCheckState teleportedState =
                            this.findStateByEntityId(teleportPacket.id());
                    if (teleportedState != null) {
                        teleportedState.checkSuppressionTicks =
                                Math.max(teleportedState.checkSuppressionTicks, 4);
                        teleportedState.relaxMovementBuffers();
                    }
                }
            }
        } else {
            this.playerStates.clear();
        }
    }

    /** Passes externally supplied Seraph/Urchin statistics to the tag lookup helper. */
    public void receiveExternalStats(String playerName, JsonObject statsJson) {
        this.externalTagLookup.handleStats(playerName, statsJson);
    }

    private static final String MODULE_ID = "61e9585ddaff2d78f4275a42d78a6879";
    private static final String KILL_AURA_SETTING_ID = "30fce9b154b3089ad70ce3c477cf8681";
    private static final String VELOCITY_SETTING_ID = "1093d5746b84fa467dea623c026db251";
    private static final String SPEED_SETTING_ID = "c0dec90b94b5effec3c8dc92dcb1aa81";
    private static final String FLY_SETTING_ID = "fa51940dbb4c023dd8604af49e29d09f";
    private static final String REACH_SETTING_ID = "6ca426030e3215abae95fef1bc326538";
    private static final String BOT_SETTING_ID = "c8b9857e17fccaeb75d8a8a4a9ea3d36";
    private static final String SCAFFOLD_SETTING_ID = "074047925307c6c6571bff82fc8050fd";
    private static final String AUTO_BLOCK_SETTING_ID = "ba7ab304b736729986347fc4457dae96";
    private static final String URCHIN_SERAPH_SETTING_ID = "e3d24b6a9729638c368004851f3d6bf2";
    private static final String SENSITIVITY_SETTING_ID = "f65a01bf5c3a814d0d99082ccad0ee79";
    private static final String AUTO_MARK_HACKER_SETTING_ID = "865ff9c0e09efe2d8ed87916dc7f07bd";
    private static final String AUTO_TARGET_SETTING_ID = "f06267646a621ba0624d5181d4d533c2";
    private static final String TOAST_ON_FLAG_SETTING_ID = "6bc7ce0d1f9991438e8677f81dbf9ef7";
    private static final String CHECK_SELF_SETTING_ID = "f9dd515c670561cfea8993ce4c7962b5";
}
