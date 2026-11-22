package astralis.module.impl.flight.mode;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.MoveInputEvent;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.TickEvent;
import astralis.event.impl.WorldChangeEvent;
import astralis.module.impl.Flight;
import astralis.module.impl.combat.Velocity;
import astralis.rotation.RotationManager;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.NumberSetting;
import astralis.ui.animation.Animation;
import astralis.util.Blink;
import astralis.util.BlinkDirection;
import astralis.util.ChatUtil;
import astralis.util.Easing;
import astralis.util.InventoryUtil;
import astralis.util.MoveUtil;
import astralis.util.PacketUtil;
import astralis.util.inventory.DelayedSlotRestorer;
import astralis.util.render.ColorUtil;
import astralis.util.render.FontManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RoundedRectShape;
import astralis.util.render.ShapePaint;
import astralis.util.render.font.SizedFont;
import astralis.util.render.font.TextDrawRequest;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.Color;

public class FlightWatchdogFireball extends Mode<Flight> {
    private final NumberSetting delay;
    private final BooleanSetting hideJump;
    private final BooleanSetting rotate;
    private int fireballStage;
    private int previousSlot;
    private boolean fireballUsed;
    private boolean leftGround;
    private boolean landed;
    private boolean knockbackPending;
    private long fireballUseTime;
    private long knockbackTime;
    private long landTime;
    private int ticksSinceKnockback;
    private int estimatedFallTicks;
    private float progress;
    private boolean jumpPending;
    private double groundEyeY;
    private final Animation barOpacityAnimation;
    private final Animation progressAnimation;
    private boolean velocityWasEnabled;

    public FlightWatchdogFireball(Flight flight) {

        super("Watchdog Fireball", flight);

        this.delay =
                new NumberSetting("a199e39660a601d1f6a199bbcbd51ae6", 800.0F, 0.0F, 1500.0F, 10.0F);

        this.hideJump = new BooleanSetting("26a93874f336c0b0ca25773459d943a9", true);

        this.rotate = new BooleanSetting("b98098b227fadc157649fbdb9926f64b", false);
        this.fireballStage = 0;
        this.previousSlot = -1;
        this.fireballUsed = false;
        this.leftGround = false;
        this.landed = false;
        this.knockbackPending = false;

        this.fireballUseTime = -1L;

        this.knockbackTime = -1L;

        this.landTime = -1L;
        this.estimatedFallTicks = -1;
        this.jumpPending = false;
        this.groundEyeY = Double.NaN;

        this.barOpacityAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);

        this.progressAnimation = new Animation(Easing.EASE_OUT_QUINT, 210L);
        this.velocityWasEnabled = false;
        this.addSettings(this.delay, this.hideJump, this.rotate);
    }

    private int findFireChargeSlot() {
        int slot = 0;

        while (true) {

            if (slot >= 9) {
                return -1;
            }

            ItemStack itemStack = mc.player.getInventory().getItem(slot);
            if (itemStack.is(Items.FIRE_CHARGE)) {
                return slot;
            }

            slot++;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        long elapsedSinceLand;

        if (this.knockbackTime != -1L && mc.player != null) {
            float displayedProgress;
            double targetOpacity;
            label32:
            {
                long now = System.currentTimeMillis();
                this.progressAnimation.animateTo(this.landed ? 1.0 : this.progress);
                displayedProgress =
                        Math.clamp((float) this.progressAnimation.getValue(), 0.0F, 1.0F);
                if (this.landed) {
                    elapsedSinceLand = now - this.landTime;

                    if (elapsedSinceLand >= 120L) {
                        targetOpacity = 0.0;
                        break label32;
                    }
                }

                targetOpacity = 1.0;
            }

            this.barOpacityAnimation.animateTo(targetOpacity);
            float barOpacity = Math.clamp((float) this.barOpacityAnimation.getValue(), 0.0F, 1.0F);
            if (!(barOpacity <= 0.01F)) {
                SizedFont font = FontManager.getThemedFont("Product Sans Medium", 7.0F);
                float barX = (mc.getWindow().getGuiScaledWidth() - 116.0F) * 0.5F;
                float barY = mc.getWindow().getGuiScaledHeight() * 0.5F + 31.0F;
                float fillWidth = 116.0F * displayedProgress;
                Color color = Astralis.getInstance().getPrimaryColor();
                Color color2 = Astralis.getInstance().getSecondaryColor();
                String percentText = Math.round(displayedProgress * 100.0F) + "%";
                Render2DContext backgroundContext = render2DEvent.getContext();
                RoundedRectShape backgroundShape =
                        RoundedRectShape.obtain()
                                .setBounds(barX, barY, 116.0F, 12.0F)
                                .setRadius(6.0F)
                                .setShadow(true, Math.round(barOpacity * 255.0F));

                backgroundContext.drawRect(
                        backgroundShape.setColor(
                                new Color(20, 23, 30, Math.round(84.0F * barOpacity))));
                if (fillWidth > 0.5F) {
                    render2DEvent
                            .getContext()
                            .drawRect(
                                    RoundedRectShape.obtain()
                                            .setBounds(barX, barY, fillWidth, 12.0F)
                                            .setRadius(Math.min(12.0F, fillWidth) * 0.5F)
                                            .setPaint(
                                                    ShapePaint.obtain()
                                                            .setHorizontalGradient(
                                                                    ColorUtil.withAlphaFactor(
                                                                                    color,
                                                                                    barOpacity)
                                                                            .getRGB(),
                                                                    ColorUtil.withAlphaFactor(
                                                                                    color2,
                                                                                    barOpacity)
                                                                            .getRGB())));
                }

                render2DEvent
                        .getContext()
                        .drawText(
                                TextDrawRequest.obtain()
                                        .setFont(font.getMsdfFont())
                                        .setText(percentText)
                                        .setPosition(
                                                barX
                                                        + (116.0F
                                                                        - font.getStringWidth(
                                                                                percentText))
                                                                * 0.5F,
                                                barY + (12.0F - font.getLineHeight()) * 0.5F)
                                        .setFontSize(font.getSize())
                                        .enableShadow()
                                        .setColor(
                                                ColorUtil.withAlphaFactor(Color.WHITE, barOpacity)
                                                        .getRGB()));
            }
        }
    }

    @EventTarget
    public void onWorldChange(WorldChangeEvent worldChangeEvent) {
        Blink.stop();
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        long elapsedSinceLand;

        if (tickEvent.getStage() == EventStage.PRE) {
            long now = System.currentTimeMillis();
            if (this.landed) {

                if (this.fireballUseTime == -1L) {
                    elapsedSinceLand = now - this.landTime;

                    if (elapsedSinceLand >= 350L) {
                        this.getParent().toggle();
                    }
                }

                return;
            }

            if (this.hideJump.getValue() && mc.player.onGround()) {
                this.groundEyeY = mc.player.getEyeY();
            }

            if (!mc.player.onGround()) {
                this.leftGround = true;
                if (this.knockbackPending) {
                    this.knockbackPending = false;
                    this.knockbackTime = now;
                    this.ticksSinceKnockback = 0;
                    this.progress = 0.0F;
                } else {

                    if (this.knockbackTime != -1L) {
                        this.ticksSinceKnockback++;
                    }
                }

                if (this.knockbackTime != -1L) {
                    this.estimatedFallTicks = this.estimateFallTicks();
                    this.progress =
                            Math.max(
                                    this.progress,
                                    Math.min(
                                            0.99F,
                                            (float) this.ticksSinceKnockback
                                                    / (this.ticksSinceKnockback
                                                            + Math.max(
                                                                    1, this.estimatedFallTicks))));
                }
            }

            if (this.knockbackTime != -1L && this.leftGround && mc.player.onGround()) {
                this.landed = true;
                this.landTime = now;
                this.estimatedFallTicks = 0;
                return;
            }

            int fireChargeSlot = this.findFireChargeSlot();
            if (fireChargeSlot == -1 && this.fireballStage == 0) {

                ChatUtil.print("§cNo fireball found.");
                this.getParent().toggle();
                return;
            }

            byte downwardStageLimit = 4;
            if (this.fireballStage >= 1 && this.fireballStage <= downwardStageLimit) {
                RotationManager.getInstance().submit(mc.player.getYRot(), 90.0F, 180.0F, 180.0F);
            } else if (this.fireballStage <= 7) {
                RotationManager.getInstance().submit(mc.player.getYRot(), 90.0F, 180.0F, 180.0F);
            } else if (this.fireballUsed && !mc.player.onGround() && this.rotate.getValue()) {
                float yaw = MoveUtil.getMoveYaw() + 45.0F;
                float pitch = mc.player.getXRot();
                RotationManager.getInstance().submit(yaw, pitch, 180.0F, 180.0F);
            } else {
                RotationManager.getInstance()
                        .submit(mc.player.getYRot(), mc.player.getXRot(), 180.0F, 180.0F);
            }

            switch (this.fireballStage) {
                case 0:
                    this.previousSlot = mc.player.getInventory().getSelectedSlot();
                    if (this.previousSlot != fireChargeSlot) {
                        InventoryUtil.setSlot(fireChargeSlot);
                    } else {
                        this.previousSlot = -1;
                    }
                    break;
                case 1:
                    PacketUtil.sendSilent(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    PacketUtil.sendSilent(
                            new ServerboundUseItemPacket(
                                    InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90.0F));
                    if (this.previousSlot != -1) {
                        DelayedSlotRestorer.scheduleSlotRestore(this.previousSlot);
                        this.previousSlot = -1;
                    }

                    this.fireballUseTime = System.currentTimeMillis();
                    this.fireballUsed = true;
                    break;
                case 7:
                    this.jumpPending = true;
            }

            this.fireballStage++;
        } else {

            if (this.fireballUseTime == -1L) {
                return;
            }

            long elapsed = System.currentTimeMillis() - this.fireballUseTime;
            if ((float) elapsed >= this.delay.getValue()) {
                Blink.stop();
                Blink.start(BlinkDirection.INCOMING);

                this.fireballUseTime = -1L;
            }
        }
    }

    public double getGroundEyeY() {
        return this.groundEyeY;
    }

    private int estimateFallTicks() {

        double spaceBelow =
                mc.player.getAvailableSpaceBelow(
                        mc.player.getBoundingBox().minY - mc.level.getMinY() + 1.0);
        double remainingHeight = spaceBelow;
        double verticalSpeed = mc.player.getDeltaMovement().y;
        double gravity = mc.player.getGravity();
        MobEffectInstance mobEffectInstance = mc.player.getEffect(MobEffects.LEVITATION);
        boolean slowFalling = mc.player.hasEffect(MobEffects.SLOW_FALLING);
        int tick = 1;

        while (true) {

            if (tick > 1200) {

                return 1200;
            }

            remainingHeight += verticalSpeed;
            if (remainingHeight <= 0.0) {
                return tick;
            }

            if (mobEffectInstance != null) {
                verticalSpeed +=
                        (0.05 * (mobEffectInstance.getAmplifier() + 1) - verticalSpeed) * 0.2;
            } else {
                verticalSpeed -=
                        slowFalling && verticalSpeed <= 0.0 ? Math.min(gravity, 0.01) : gravity;
            }

            verticalSpeed *= 0.98;
            tick++;
        }
    }

    public boolean isHideJumpEnabled() {
        return this.hideJump.getValue();
    }

    public double getHideJumpEyeY() {
        return this.groundEyeY;
    }

    public BooleanSetting getHideJump() {
        return this.hideJump;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent moveInputEvent) {
        if (this.jumpPending && mc.player.onGround()) {
            moveInputEvent.setJump(true);
            this.jumpPending = false;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.RECEIVE) {
            Packet packet = packetEvent.getPacket();
            if (mc.player != null && this.fireballUsed) {

                if (this.knockbackTime == -1L
                        && (packet
                                                instanceof
                                                ClientboundSetEntityMotionPacket
                                                        clientboundSetEntityMotionPacket
                                        && clientboundSetEntityMotionPacket.id()
                                                == mc.player.getId()
                                        && clientboundSetEntityMotionPacket.movement().lengthSqr()
                                                > 0.0
                                || packet
                                                instanceof
                                                ClientboundExplodePacket clientboundExplodePacket
                                        && clientboundExplodePacket
                                                .playerKnockback()
                                                .filter(vector3 -> vector3.lengthSqr() > 0.0)
                                                .isPresent())) {
                    this.knockbackPending = true;
                    return;
                }
            }

            if (packet instanceof ClientboundAddEntityPacket clientboundAddEntityPacket) {
                String entityKey =
                        EntityType.getKey(clientboundAddEntityPacket.getType()).toString();

                if (!entityKey.equals("minecraft:fireball")) {

                    if (!entityKey.equals("minecraft:small_fireball")) {
                        return;
                    }
                }

                packetEvent.setCancelled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        Blink.stop();
        if (this.velocityWasEnabled) {
            Velocity velocity = Astralis.getInstance().getModuleManager().getModule(Velocity.class);
            if (velocity != null && !velocity.isEnabled()) {
                velocity.toggle();
            }

            this.velocityWasEnabled = false;
        }
    }

    @Override
    public void onEnable() {
        Velocity velocity = Astralis.getInstance().getModuleManager().getModule(Velocity.class);
        if (velocity != null && velocity.isEnabled()) {
            this.velocityWasEnabled = true;
            velocity.toggle();
        } else {
            this.velocityWasEnabled = false;
        }

        this.fireballStage = 0;
        this.previousSlot = -1;
        this.fireballUsed = false;
        this.leftGround = false;
        this.knockbackPending = false;
        this.landed = false;

        this.fireballUseTime = -1L;

        this.knockbackTime = -1L;

        this.landTime = -1L;
        this.ticksSinceKnockback = 0;
        this.estimatedFallTicks = -1;
        this.progress = 0.0F;
        this.progressAnimation.setValue(0.0);
        this.progressAnimation.setTarget(0.0);
        this.jumpPending = false;
        this.groundEyeY = mc.player != null ? mc.player.getEyeY() : Double.NaN;
        Blink.start(BlinkDirection.INCOMING);
    }
}
