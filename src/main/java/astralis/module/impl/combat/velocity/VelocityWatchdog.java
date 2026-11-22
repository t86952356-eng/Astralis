package astralis.module.impl.combat.velocity;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.VelocityEvent;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.combat.Velocity;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.util.MoveUtil;
import astralis.util.RotationUtil;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/** Combines an optional jump reset with an attack reduction window after knockback. */
public class VelocityWatchdog extends Mode<Velocity> {
    private final BooleanSetting jumpReset;
    private final BooleanSetting attackReduce;
    private int remainingAttackReductionTicks;
    private boolean sentHeldItemChange;
    private boolean sentInteract;
    private boolean sentSwing;
    private boolean sentUseItemOrAction;
    private boolean sentContainerAction;
    private boolean sentPlayerAction;
    private boolean jumpResetPending;
    private boolean releaseForwardNextInput;

    public VelocityWatchdog(Velocity parent) {

        super("Watchdog", parent);

        this.jumpReset = new BooleanSetting(JUMP_RESET_SETTING_ID, false);

        this.attackReduce = new BooleanSetting(ATTACK_REDUCE_SETTING_ID, true);
        this.addSettings(this.jumpReset, this.attackReduce);
    }

    @EventTarget
    public void onVelocity(VelocityEvent event) {
        if (this.getParent().shouldApply() && !event.isExplosion()) {
            if (this.attackReduce.getValue()) {
                this.remainingAttackReductionTicks =
                        this.computeReduceTicks(event.getX(), event.getZ());
            }

            if (this.jumpReset.getValue()
                    && event.getY() > 0.0
                    && (mc.player.onGround() || mc.player.fallDistance < 0.1F)) {
                this.jumpResetPending = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        // Action packets block another reduction attempt until a movement packet resets the flags.
        if (!event.isCancelled() && event.getStage() == EventStage.SEND) {
            Packet packet = event.getPacket();
            if (packet instanceof ServerboundSetCarriedItemPacket) {
                this.sentHeldItemChange = true;
            } else if (packet instanceof ServerboundSwingPacket) {
                this.sentSwing = true;
            } else if (packet instanceof ServerboundInteractPacket) {
                this.sentInteract = true;
            } else if (packet instanceof ServerboundUseItemOnPacket) {
                this.sentUseItemOrAction = true;
            } else if (packet instanceof ServerboundPlayerActionPacket) {
                this.sentUseItemOrAction = true;
                this.sentPlayerAction = true;
            } else if (packet instanceof ServerboundContainerClickPacket
                    || packet instanceof ServerboundContainerClosePacket) {
                this.sentContainerAction = true;
            } else if (packet instanceof ServerboundMovePlayerPacket) {
                this.resetSentPacketFlags();
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.jumpReset.getValue()) {
            if (this.jumpResetPending) {

                if (mc.player.hurtTime >= 10 && !event.isJump()) {
                    event.setJump(true);
                }
            }

            if (!mc.player.onGround() && this.jumpResetPending) {

                if (mc.player.hurtTime < 9) {
                    this.jumpResetPending = false;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getStage() == EventStage.PRE) {
            if (this.attackReduce.getValue()) {
                if (this.remainingAttackReductionTicks > 0) {
                    if (this.getParent().shouldApply()) {
                        this.remainingAttackReductionTicks--;
                        KillAura killAura =
                                Astralis.getInstance().getModuleManager().getModule(KillAura.class);
                        if (killAura.isEnabled()
                                && killAura.target != null
                                && killAura.target != mc.player) {
                            if (mc.player.isSprinting()) {
                                if (MoveUtil.isMoving()) {
                                    if (!this.hasSentActionPacket()) {
                                        if (RotationUtil.rayTraceHitsEntityCurrent(
                                                killAura.target,
                                                killAura.getAttackRange().getValue(),
                                                false)) {
                                            mc.gameMode.attack(mc.player, killAura.target);
                                            mc.player.swing(InteractionHand.MAIN_HAND);
                                        }

                                        Vec3 vector3 = mc.player.getDeltaMovement();
                                        this.releaseForwardNextInput = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasSentActionPacket() {
        return this.sentHeldItemChange
                || this.sentInteract
                || this.sentSwing
                || this.sentUseItemOrAction
                || this.sentContainerAction
                || this.sentPlayerAction;
    }

    private void resetSentPacketFlags() {
        this.sentHeldItemChange = false;
        this.sentInteract = false;
        this.sentSwing = false;
        this.sentUseItemOrAction = false;
        this.sentContainerAction = false;
        this.sentPlayerAction = false;
    }

    @EventTarget
    public void onMoveInput2(MoveInputEvent event) {
        // The attack attempt schedules one forward-input release on the next input event.
        if (this.releaseForwardNextInput) {
            event.setForward(false);
            this.releaseForwardNextInput = false;
        }
    }

    @Override
    public void onDisable() {
        this.remainingAttackReductionTicks = 0;
        this.jumpResetPending = false;
        this.releaseForwardNextInput = false;
        this.resetSentPacketFlags();
    }

    private int computeReduceTicks(double knockbackX, double knockbackZ) {
        double encodedHorizontalSpeed = Math.hypot(knockbackX, knockbackZ) * 8000.0;
        double estimatedTicks = 6.43153527E-4 * encodedHorizontalSpeed + 2.9419087136;
        int roundedTicks = (int) Math.round(estimatedTicks);

        return Mth.clamp(roundedTicks, 1, 10);
    }

    @Override
    public void onEnable() {
        this.remainingAttackReductionTicks = 0;
        this.jumpResetPending = false;
        this.releaseForwardNextInput = false;
        this.resetSentPacketFlags();
    }

    private static final String JUMP_RESET_SETTING_ID = "e5f42784a8fc08d76bf5ac57cfb11dcd";
    private static final String ATTACK_REDUCE_SETTING_ID = "8a7cff8a0e5c7bfe90de561a7322a360";
}
