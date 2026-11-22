package astralis.manager;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;
import astralis.module.impl.movement.KeepSprint;
import astralis.util.BreakUtil;
import astralis.util.Listener;
import astralis.util.PacketUtil;
import astralis.util.SwingType;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class AttackManager extends Listener {
    private static AtomicBoolean attackedThisTick;
    private static AtomicBoolean swungThisTick;
    private static Random random;
    private static long clickIntervalNanos;
    private static long lastAttackNanos;
    private static double pendingClicks;

    public static void attackAtFixedCps(
            SwingType swingType, LivingEntity livingEntity, double cps) {
        long now = System.nanoTime();
        long elapsedNanos = now - lastAttackNanos;
        lastAttackNanos = now;
        long intervalNanos = (long) (1.0E9 / cps);
        if (elapsedNanos > intervalNanos * 3L) {
            elapsedNanos = intervalNanos;
        }

        if (clickIntervalNanos == 0L) {
            clickIntervalNanos = intervalNanos;
        }

        for (pendingClicks = pendingClicks + (double) elapsedNanos / clickIntervalNanos;
                pendingClicks >= 1.0;
                clickIntervalNanos = intervalNanos) {
            attackTarget(swingType, livingEntity);
            pendingClicks--;
        }
    }

    private static long randomClickIntervalNanos(double minCps, double maxCps) {
        return (long) (1.0E9 / randomCps(minCps, maxCps));
    }

    public static void resetLastAttackTime() {
        lastAttackNanos = System.nanoTime();
    }

    public static void attackTarget(SwingType swingType, Entity entity2) {
        attackEntity(swingType, entity2, false);
    }

    public static void resetTickFlags() {
        attackedThisTick.set(false);
        swungThisTick.set(false);
    }

    public static void attackAtRandomCps(
            SwingType swingType, LivingEntity livingEntity, double minCps, double maxCps) {
        long now = System.nanoTime();
        long elapsedNanos = now - lastAttackNanos;
        lastAttackNanos = now;
        if (clickIntervalNanos == 0L) {
            clickIntervalNanos = randomClickIntervalNanos(minCps, maxCps);
        }

        for (pendingClicks = pendingClicks + (double) elapsedNanos / clickIntervalNanos;
                pendingClicks >= 1.0;
                clickIntervalNanos = randomClickIntervalNanos(minCps, maxCps)) {
            attackTarget(swingType, livingEntity);
            pendingClicks--;
        }
    }

    public static boolean tryClaimSwing() {
        return swungThisTick.compareAndSet(false, true);
    }

    public static boolean isAttackCooldownReady(Entity entity2, double minStrength) {
        if (entity2 == null) {
            return false;
        }

        double threshold = Math.clamp(minStrength, 0.0, 1.0);
        return mc.player.getAttackStrengthScale(0.0F) >= threshold;
    }

    private static double randomCps(double minCps, double maxCps) {
        double low = Math.min(maxCps, minCps);
        double high = Math.max(low, maxCps);
        if (low == high) {
            high++;
        }

        return random.nextDouble(low, high);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            resetTickFlags();
        }
    }

    public static boolean tryClaimAttack() {
        return attackedThisTick.compareAndSet(false, true);
    }

    public static void resetClickTimer() {
        pendingClicks = 0.0;
        clickIntervalNanos = 0L;
        lastAttackNanos = System.nanoTime();
    }

    public static void attackEntity(SwingType swingType, Entity entity2, boolean ignoreKeepSprint) {
        if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            if (tryClaimAttack()) {
                KeepSprint keepSprint =
                        Astralis.getInstance().getModuleManager().getModule(KeepSprint.class);
                boolean keepSprintUniversal =
                        !ignoreKeepSprint
                                && keepSprint.isEnabled()
                                && keepSprint.getMode().is("Universal");
                switch (swingType) {
                    case CLICK:
                        BreakUtil.clickLeftMouse();
                        break;
                    case PACKET:
                        mc.gameMode.attack(mc.player, entity2);
                        if (tryClaimSwing()) {
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }

                        mc.player.resetAttackStrengthTicker();
                        break;
                    case ONEDOTEIGHT:
                        if (!keepSprintUniversal) {
                            mc.gameMode.attack(mc.player, entity2);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        } else {
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            mc.gameMode.attack(mc.player, entity2);
                        }
                        break;
                    case FULL_PACKET:
                        PacketUtil.send(new ServerboundAttackPacket(entity2.getId()));
                        PacketUtil.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                        if (!keepSprintUniversal) {
                            mc.player.setDeltaMovement(
                                    mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                            mc.player.setSprinting(false);
                        }
                        break;
                    case FULL_PACKET_RENDER_SWING:
                        PacketUtil.send(new ServerboundAttackPacket(entity2.getId()));
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        if (!keepSprintUniversal) {
                            mc.player.setDeltaMovement(
                                    mc.player.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                            mc.player.setSprinting(false);
                        }
                }
            }
        }
    }

    public static boolean attackIfCooldownReady(
            SwingType swingType, LivingEntity livingEntity, double minStrength) {
        if (!isAttackCooldownReady(livingEntity, minStrength)) {
            return false;
        }

        attackTarget(swingType, livingEntity);
        return true;
    }

    static {
        attackedThisTick = new AtomicBoolean(false);
        swungThisTick = new AtomicBoolean(false);
        random = new Random();
        clickIntervalNanos = 0L;
        lastAttackNanos = System.nanoTime();
        pendingClicks = 0.0;
    }
}
