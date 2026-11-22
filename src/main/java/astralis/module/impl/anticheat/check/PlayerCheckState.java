package astralis.module.impl.anticheat.check;

import astralis.module.impl.anticheat.ScaffoldPlacementSample;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Mutable observations, detection buffers, sample windows, and reporting history for one player.
 * The fields are public because the module and checks share this state directly.
 */
public class PlayerCheckState {
    /** Whether the first player snapshot has been recorded. */
    public boolean hasSnapshot;

    /** Previous snapshot X coordinate. */
    public double previousX;

    /** Previous snapshot Y coordinate. */
    public double previousY;

    /** Previous snapshot Z coordinate. */
    public double previousZ;

    /** Previous checked horizontal displacement length. */
    public double previousHorizontalSpeed;

    /** Previous checked yaw, or initial yaw before the first check. */
    public float previousYaw;

    /** Previous checked pitch, or initial pitch before the first check. */
    public float previousPitch;

    /** Hurt time from the previous player snapshot. */
    public int previousHurtTime;

    /** Consecutive airborne observations. */
    public int airborneTicks;

    /** Ticks of check suppression after server corrections, motion updates, or teleports. */
    public int checkSuppressionTicks;

    /** Movement-exemption countdown following a server entity-motion packet. */
    public int motionExemptionTicks;

    /** Grounded state from the previous player snapshot. */
    public boolean previousOnGround;

    /** Swinging state from the previous player snapshot. */
    public boolean previousSwinging;

    /** Sneak state from the latest outgoing input packet. */
    public boolean outgoingSneaking;

    /** Whether the previous scaffold observation lacked center support collision. */
    public boolean lackedCenterSupport;

    /** Whether the previous scaffold observation lacked nearby support collision. */
    public boolean lackedNearbySupport;

    /** Speed evidence buffer. */
    public double speedBuffer;

    /** Fly evidence buffer. */
    public double flyBuffer;

    /** KillAura evidence shared by the acceleration and missed-ray checks. */
    public double accelRayMissBuffer;

    /** KillAura aim-snap evidence buffer. */
    public double aimSnapBuffer;

    /** KillAura persistent-aim observation count. */
    public int stickyAimCount;

    /** Observed simultaneous blocking/swinging evidence buffer. */
    public double blockSwingBuffer;

    /** Low-knockback velocity evidence buffer. */
    public double lowKnockbackBuffer;

    /** Observed reach evidence buffer. */
    public double reachBuffer;

    /** Outgoing attack-packet reach evidence buffer. */
    public double packetReachBuffer;

    /** Observed sneak-bridge evidence buffer. */
    public double sneakBridgeBuffer;

    /** Observed bridge/tower placement-cadence evidence buffer. */
    public double placementCadenceBuffer;

    /** Local block-use without rotation evidence buffer. */
    public double noRotationPlaceBuffer;

    /** Linked-placement rotation-speed evidence buffer. */
    public double rotationSpeedBuffer;

    /** Observed 45-degree yaw-snap evidence buffer. */
    public double yawSnap45Buffer;

    /** Observed jumping placement-line evidence buffer. */
    public double jumpPlaceLineBuffer;

    /** Observed telly rotation/speed evidence buffer. */
    public double tellyBuffer;

    /** Large yaw-snap followed by opposite return evidence buffer. */
    public double tellySnapReturnBuffer;

    /** Remaining observations in the seven-observation yaw-return window. */
    public int yawReturnWindowLeft;

    /** Absolute initial yaw snap in the active return window. */
    public float snapYawMagnitude;

    /** Accumulated opposite-direction yaw return in the active window. */
    public float yawReturnTotal;

    /** Sign of the initial wrapped yaw snap in the active return window. */
    public float snapYawSign;

    /** Observations since inferred scaffold placement, capped at forty. */
    public int observationsSincePlacement;

    /** Local tick of the latest outgoing packet containing rotation. */
    public int lastRotationPacketTick;

    /** Local tick when outgoing input most recently began sneaking. */
    public int sneakStartTick;

    /** Local tick when outgoing input most recently stopped sneaking. */
    public int sneakStopTick;

    /** Yaw from the latest outgoing rotation packet. */
    public float lastPacketYaw;

    /** Pitch from the latest outgoing rotation packet. */
    public float lastPacketPitch;

    /** Outgoing placement/rotation mismatch evidence buffer. */
    public double packetRotationMismatchBuffer;

    /** Outgoing placement at a 45-degree yaw multiple evidence buffer. */
    public double packetYawSnap45Buffer;

    /** Outgoing ascending jump-placement evidence buffer. */
    public double packetJumpPlaceBuffer;

    /** Outgoing placement near a sneak transition evidence buffer. */
    public double packetSneakPlaceBuffer;

    /** Outgoing bridge-chain or repeated nearby-placement evidence buffer. */
    public double packetBridgeChainBuffer;

    /** Length of the current nearby outgoing-placement chain. */
    public int placementChainLength;

    /** Block position inferred from the latest outgoing placement packet. */
    public BlockPos lastPlacedPosition;

    /** Outgoing same-tick attacks against different players evidence buffer. */
    public double multiTargetAttackBuffer;

    /** Outgoing attacks while blocking evidence buffer. */
    public double blockingAttackBuffer;

    /** Local tick of the latest outgoing attack on a player. */
    public int lastAttackTick;

    /** Entity ID of the latest outgoing player-attack target; initially -1. */
    public int lastAttackTargetId;

    /** Absolute yaw changes retained at inferred placements, up to forty samples. */
    public final List<Float> placementYawChanges;

    /** Recent yaw values, up to sixty observations. */
    public final List<Float> recentYaws;

    /** Current/estimated previous block positions at inferred placements, up to twelve samples. */
    public final List<ScaffoldPlacementSample> placementSamples;

    /** Underfoot positions at inferred placements, up to six samples. */
    public final List<BlockPos> underfootPositions;

    /** Reported violation count for each check category. */
    public final Map<CheckType, Integer> flagCounts;

    /** Last reported flag time in milliseconds for each check category. */
    public final Map<CheckType, Long> lastFlagTimes;

    /** Initializes the sample windows and sentinel values. */
    public PlayerCheckState() {
        super();
        this.observationsSincePlacement = 40;
        this.lastRotationPacketTick = -100;
        this.sneakStartTick = -100;
        this.sneakStopTick = -100;
        this.lastAttackTick = -100;
        this.lastAttackTargetId = -1;
        this.placementYawChanges = new LinkedList<>();
        this.recentYaws = new LinkedList<>();
        this.placementSamples = new LinkedList<>();
        this.underfootPositions = new LinkedList<>();
        this.flagCounts = new EnumMap<>(CheckType.class);
        this.lastFlagTimes = new EnumMap<>(CheckType.class);
    }

    /** Records position/hurt/swing/ground state, initializing rotations only for the first snapshot. */
    public void recordSnapshot(Player player) {
        this.previousX = player.getX();
        this.previousY = player.getY();
        this.previousZ = player.getZ();
        this.previousOnGround = player.onGround();
        this.previousSwinging = player.swinging;
        this.previousHurtTime = player.hurtTime;
        if (!this.hasSnapshot) {
            this.previousYaw = player.getYRot();
            this.previousPitch = player.getXRot();
        }

        this.hasSnapshot = true;
    }

    /** Relaxes movement buffers and resets motion history during a correction or skipped check. */
    public void relaxMovementBuffers() {
        this.speedBuffer = Math.max(0.0, this.speedBuffer - 2.0);
        this.flyBuffer = Math.max(0.0, this.flyBuffer - 2.0);
        this.lowKnockbackBuffer = Math.max(0.0, this.lowKnockbackBuffer - 1.0);
        this.previousHorizontalSpeed = 0.0;
        this.airborneTicks = 0;
        this.motionExemptionTicks = Math.max(0, this.motionExemptionTicks - 1);
    }
}
