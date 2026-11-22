package astralis.module.impl.combat.velocity;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ChatReceiveEvent;
import astralis.event.impl.HandleKeybindsEvent;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.PostMotionEvent;
import astralis.manager.AttackManager;
import astralis.mixin.LocalPlayerAccessor;
import astralis.module.impl.Flight;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.combat.Velocity;
import astralis.module.impl.world.Scaffold;
import astralis.rotation.MoveFixMode;
import astralis.rotation.Rotation;
import astralis.rotation.RotationManager;
import astralis.rotation.RotationPriority;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.util.MoveUtil;
import astralis.util.PacketUtil;
import astralis.util.RotationUtil;
import astralis.util.SwingType;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.LinkedBlockingDeque;

/** Delays eligible incoming packets and coordinates attack reduction with knockback input. */
public class VelocityWatchdog2 extends Mode<Velocity> {
    private final NumberSetting packetDelayTicks;
    private static LinkedBlockingDeque<Packet<ClientGamePacketListener>> delayedPackets;
    private static boolean forwardKeyForced;
    private static boolean delayingIncomingPackets;
    private static boolean forwardInputRequested;
    private static boolean attackReductionActive;
    private static boolean knockbackKeysOverridden;
    private static int attackReductionDurationTicks;
    private static int elapsedDelayTicks;
    private static int ticksSinceKnockback;
    private static Rotation knockbackOriginRotation;

    public VelocityWatchdog2(Velocity parent) {

        super("Watchdog 2", parent);

        this.packetDelayTicks =
                new NumberSetting(PACKET_DELAY_TICKS_SETTING_ID, 4.0F, 0.0F, 10.0F, 1.0F);
        this.addSettings(this.packetDelayTicks);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getStage() == EventStage.RECEIVE) {
            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket
                    && motionPacket.id() == mc.player.getId()
                    && !Astralis.getInstance()
                            .getModuleManager()
                            .getModule(Flight.class)
                            .isEnabled()) {
                double knockbackX = motionPacket.movement().x;
                double knockbackZ = motionPacket.movement().z;
                double knockbackY = motionPacket.movement().y;
                double horizontalSpeed = new Vec3(knockbackX, 0.0, knockbackZ).length();
                if (knockbackY <= 0.0
                        || horizontalSpeed < 0.1
                        || mc.player.onClimbable()
                        || mc.player.isInLiquid()) {
                    return;
                }

                Vec3 directionToKnockbackOrigin =
                        new Vec3(-knockbackX, 0.0, -knockbackZ).normalize();
                attackReductionDurationTicks =
                        this.computeReduceTicks(
                                (int) (knockbackX * 8000.0), (int) (knockbackZ * 8000.0));
                Vec3 knockbackOriginPosition =
                        mc.player
                                .position()
                                .add(
                                        directionToKnockbackOrigin.x,
                                        0.0,
                                        directionToKnockbackOrigin.z);
                float[] originAngles = RotationUtil.getRotationsTo(knockbackOriginPosition);
                knockbackOriginRotation =
                        new Rotation(
                                originAngles[0],
                                RotationManager.getInstance().getPitch(),
                                360.0F,
                                360.0F,
                                RotationPriority.STANDARD,
                                MoveFixMode.STRICT);
                if (!delayingIncomingPackets) {
                    KillAura killAura =
                            Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                    Scaffold scaffold =
                            Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
                    if (this.packetDelayTicks.getValue() != 0.0F) {
                        if (!this.hasTargetInRange(killAura)
                                && this.findFarthestDistantPlayer() == null) {
                            if (MoveUtil.isLastSentOnGround()
                                    && (scaffold == null || !scaffold.isEnabled())) {
                                this.rotateTowardsKnockbackOrigin(killAura);
                                forwardInputRequested = true;
                            }

                            attackReductionActive = true;
                            ticksSinceKnockback = 0;
                        } else if (!mc.player.isSprinting()
                                && !((LocalPlayerAccessor) mc.player).isWasSprinting()) {
                            delayingIncomingPackets = true;
                            elapsedDelayTicks = 0;
                        } else {
                            if (MoveUtil.isLastSentOnGround()
                                    && (scaffold == null || !scaffold.isEnabled())) {
                                this.rotateTowardsKnockbackOrigin(killAura);
                                forwardInputRequested = true;
                            }

                            attackReductionActive = true;
                            ticksSinceKnockback = 0;
                        }
                    } else {
                        if (MoveUtil.isLastSentOnGround()
                                && (scaffold == null || !scaffold.isEnabled())) {
                            this.rotateTowardsKnockbackOrigin(killAura);
                            forwardInputRequested = true;
                        }

                        ticksSinceKnockback = 0;
                        attackReductionActive = true;
                    }
                }
            }

            // Queue the triggering motion packet too when it starts a delay.
            if (delayingIncomingPackets && !isExemptFromDelay(event)) {
                event.setCancelled(true);
                Packet packet = event.getPacket();
                delayedPackets.add(packet);
            }
        }
    }

    private static boolean isExemptFromDelay(PacketEvent event) {
        Packet packet = event.getPacket();
        return packet instanceof ClientboundSetHealthPacket
                || packet instanceof ClientboundPlayerPositionPacket
                || packet instanceof ClientboundSoundPacket
                || packet instanceof ClientboundSoundEntityPacket
                || packet instanceof ClientboundPlayerChatPacket
                || packet instanceof ClientboundSystemChatPacket
                || packet instanceof ClientboundPlayerCombatKillPacket
                || packet instanceof ClientboundContainerClosePacket
                || packet instanceof ClientboundHurtAnimationPacket
                || packet instanceof ClientboundSetTitleTextPacket
                || packet instanceof ClientboundSetSubtitleTextPacket
                || packet instanceof ClientboundSetPlayerTeamPacket
                || packet instanceof ClientboundAnimatePacket animationPacket
                        && animationPacket.getId() != mc.player.getId();
    }

    private LivingEntity getAttackTargetInRange(KillAura killAura) {
        double attackRange = killAura.getAttackRange().getValue().floatValue();
        if (!killAura.isEnabled()) {
            EntityHitResult hitResult = RotationUtil.rayTraceEntityCurrent(attackRange, false);
            return hitResult != null
                            && hitResult.getEntity() instanceof LivingEntity hitEntity
                            && this.isValidTargetInRange(hitEntity, attackRange)
                    ? hitEntity
                    : null;
        } else {
            return this.isValidTargetInRange(killAura.target, attackRange)
                            && killAura.canHitTarget(killAura.target)
                    ? killAura.target
                    : null;
        }
    }

    private LivingEntity findClosestPlayerInRange(double range) {
        LivingEntity closestPlayer = null;
        double closestDistance = range;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity livingEntity
                    && livingEntity != mc.player
                    && !(livingEntity instanceof ArmorStand)
                    && this.isPlayer(livingEntity)) {
                double distance = RotationUtil.distanceToBox(livingEntity);
                if (distance <= closestDistance) {
                    closestDistance = distance;
                    closestPlayer = livingEntity;
                }
            }
        }

        return closestPlayer;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (knockbackKeysOverridden || delayingIncomingPackets || forwardInputRequested) {
            event.setForward(true);
        }
    }

    /** Preserves this mode's fallback: the farthest player at least eleven blocks away. */
    private LivingEntity findFarthestDistantPlayer() {
        LivingEntity farthestPlayer = null;
        double farthestDistance = 0.0;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity livingEntity
                    && livingEntity != mc.player
                    && !(livingEntity instanceof ArmorStand)
                    && this.isPlayer(livingEntity)) {
                double distance = RotationUtil.distanceToBox(livingEntity);
                if (!(distance < 11.0) && distance > farthestDistance) {
                    farthestDistance = distance;
                    farthestPlayer = livingEntity;
                }
            }
        }

        return farthestPlayer;
    }

    private boolean hasTargetInRange(KillAura killAura) {
        double attackRange = killAura.getAttackRange().getValue().floatValue();
        if (killAura.isEnabled()) {
            return this.isValidTargetInRange(killAura.target, attackRange);
        }

        EntityHitResult hitResult = RotationUtil.rayTraceEntityCurrent(attackRange, false);
        return hitResult != null
                && hitResult.getEntity() instanceof LivingEntity hitEntity
                && this.isValidTargetInRange(hitEntity, attackRange);
    }

    /** Requests jump/forward during early knockback ticks, then restores physical key state. */
    private void updateOverriddenKeys() {
        if (!(mc.gui.screen() instanceof InventoryScreen)
                && !(mc.gui.screen() instanceof AbstractContainerScreen)) {
            Scaffold scaffold = Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
            if (ticksSinceKnockback >= 0 && (scaffold == null || !scaffold.isEnabled())) {
                knockbackKeysOverridden = true;
                if (ticksSinceKnockback <= 2 && MoveUtil.isLastSentOnGround()) {
                    mc.options.keyJump.setDown(true);
                }

                if (ticksSinceKnockback <= 3) {
                    mc.options.keyUp.setDown(true);
                    forwardKeyForced = true;
                }
            }

            if (ticksSinceKnockback >= 4) {

                if (ticksSinceKnockback <= 9) {
                    mc.options.keyJump.setDown(
                            InputConstants.isKeyDown(
                                    mc.getWindow(), mc.options.keyJump.getDefaultKey().getValue()));
                    if (forwardKeyForced) {
                        mc.options.keyUp.setDown(
                                InputConstants.isKeyDown(
                                        mc.getWindow(),
                                        mc.options.keyUp.getDefaultKey().getValue()));
                        forwardKeyForced = false;
                    }

                    knockbackKeysOverridden = false;
                }
            }
        }
    }

    private void rotateTowardsKnockbackOrigin(KillAura killAura) {
        LivingEntity closestPlayer =
                this.findClosestPlayerInRange(killAura.getAttackRange().getValue().floatValue());
        float targetPitch =
                closestPlayer != null
                        ? RotationUtil.getRotationsTo(
                                RotationUtil.getClosestPointOnHitbox(closestPlayer))[1]
                        : knockbackOriginRotation.getTargetPitch();
        RotationManager.getInstance()
                .submit(
                        new Rotation(
                                knockbackOriginRotation.getTargetYaw(),
                                targetPitch,
                                360.0F,
                                360.0F,
                                RotationPriority.STANDARD,
                                MoveFixMode.STRICT));
    }

    @EventTarget
    public void onChatReceive(ChatReceiveEvent event) {
        String message = event.getMessage().getString();

        if (message.contains("failed")) {

            if (message.contains("Hitboxes") && ticksSinceKnockback >= 0) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onPostMotion(PostMotionEvent event) {
        if (delayingIncomingPackets) {
            KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
            if (elapsedDelayTicks >= this.packetDelayTicks.getValue()
                    || mc.player.isSprinting()
                    || ((LocalPlayerAccessor) mc.player).isWasSprinting()
                    || mc.player.hasEffect(MobEffects.SLOWNESS)
                    || MoveUtil.isLastSentOnGround()
                    || !this.hasTargetInRange(killAura)
                            && this.findFarthestDistantPlayer() == null) {
                // Replay in queue order before starting the reduction and rotation state.
                while (!delayedPackets.isEmpty()) {
                    PacketUtil.handle(delayedPackets.poll());
                }

                attackReductionActive = true;
                ticksSinceKnockback = 0;
                Scaffold scaffold =
                        Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
                if (MoveUtil.isLastSentOnGround() && (scaffold == null || !scaffold.isEnabled())) {
                    this.rotateTowardsKnockbackOrigin(killAura);
                    forwardInputRequested = true;
                }

                delayingIncomingPackets = false;
                elapsedDelayTicks = -1;
            }
        }
    }

    private boolean isPlayer(LivingEntity entity) {
        return entity instanceof Player;
    }

    private int computeReduceTicks(int encodedKnockbackX, int encodedKnockbackZ) {
        double estimatedTicks =
                6.43153527E-4 * Math.hypot(encodedKnockbackX, encodedKnockbackZ) + 2.9419087136;
        return (int) Math.clamp(Math.round(estimatedTicks), 1L, 10L);
    }

    @EventTarget(1)
    public void onHandleKeybinds(HandleKeybindsEvent handleKeybindsEvent) {
        // These counters advance on keybind handling, including ticks spent buffering packets.
        if (ticksSinceKnockback >= 0) {
            ticksSinceKnockback++;
        }

        if (elapsedDelayTicks >= 0) {
            elapsedDelayTicks++;
        }

        if (ticksSinceKnockback >= 10) {
            if (attackReductionActive) {
                attackReductionActive = false;
            }

            forwardInputRequested = false;
            ticksSinceKnockback = -1;
        }

        if (ticksSinceKnockback >= attackReductionDurationTicks) {
            if (attackReductionActive) {
                attackReductionActive = false;
            }

            forwardInputRequested = false;
        }

        this.updateOverriddenKeys();
        if (delayingIncomingPackets || knockbackKeysOverridden) {
            forwardInputRequested = true;
        }

        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        LivingEntity attackTarget = this.getAttackTargetInRange(killAura);
        LivingEntity distantPlayer = this.findFarthestDistantPlayer();
        if ((attackTarget != null || distantPlayer != null) && attackReductionActive) {
            forwardInputRequested = true;
        }

        if (attackReductionActive
                && (mc.player.isSprinting()
                        || ((LocalPlayerAccessor) mc.player).isWasSprinting())) {
            if (attackTarget != null) {
                AttackManager.attackEntity(
                        killAura.isEnabled() && attackTarget == killAura.target
                                ? SwingType.FULL_PACKET_RENDER_SWING
                                : SwingType.FULL_PACKET,
                        attackTarget,
                        true);
            } else if (distantPlayer != null) {
                if (Astralis.getInstance().hasHypixelLocation()) {
                    AttackManager.attackEntity(SwingType.FULL_PACKET, distantPlayer, true);
                } else {
                    mc.player.setDeltaMovement(
                            mc.player.getDeltaMovement().multiply(0.1, 1.0, 0.1));
                }
            }
        }
    }

    private boolean isValidTargetInRange(LivingEntity target, double range) {
        return target != null
                && target != mc.player
                && this.isPlayer(target)
                && RotationUtil.distanceToBox(target) <= range;
    }

    static {
        delayedPackets = new LinkedBlockingDeque<>();
        elapsedDelayTicks = -1;
        ticksSinceKnockback = -1;
    }

    private static final String PACKET_DELAY_TICKS_SETTING_ID = "0b75fbbd2ffc1d0779c11e44797b1202";
}
