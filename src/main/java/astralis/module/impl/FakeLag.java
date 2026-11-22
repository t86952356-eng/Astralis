package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PacketEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.TickEvent;
import astralis.module.Module;
import astralis.module.ModuleManager;
import astralis.module.impl.world.Fucker;
import astralis.module.impl.world.Scaffold;
import astralis.setting.ColorSetting;
import astralis.setting.NumberSetting;
import astralis.util.BreakUtil;
import astralis.util.MathUtil;
import astralis.util.PacketDelay;
import astralis.util.render.RenderUtil;
import astralis.util.target.TargetSortOrder;

import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;

public class FakeLag extends Module {
    private final NumberSetting delayMs;
    private final ColorSetting serverPosColor;
    private Vec3 serverPosition;
    private long lastRenderTimeNanos;
    private boolean catchingUp;
    private static final float NORMAL_FOLLOW_SPEED = 13.0F;
    private static final float CATCH_UP_FOLLOW_SPEED = 30.0F;
    private static final double CATCH_UP_DONE_DISTANCE_SQR = 0.01;

    public FakeLag() {

        super(MODULE_ID, "", "", Category.Misc);

        this.delayMs = new NumberSetting(DELAY_MS_SETTING_ID, 100.0F, 0.0F, 1000.0F, 10.0F);

        this.serverPosColor =
                new ColorSetting(SERVER_POS_COLOR_SETTING_ID, new Color(255, 0, 0, 100));
        this.serverPosition = null;
        this.catchingUp = false;
        this.addSettings(this.delayMs, this.serverPosColor);
    }

    @EventTarget
    public void onRender3D(Render3DEvent render3DEvent) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (!moduleManager.getModule(Fucker.class).isEnabled()
                && !moduleManager.getModule(Scaffold.class).isEnabled()
                && !moduleManager.getModule(AirStuck.class).isEnabled()
                && mc.gui.screen() == null
                && (!astralis.util.Blink.isActive() || !mc.player.isUsingItem())) {
            Vec3 vector3 = PacketDelay.getLastSentPosition();
            if (vector3 != null && !mc.options.getCameraType().isFirstPerson()) {
                long nowNanos = System.nanoTime();
                double deltaSeconds =
                        this.lastRenderTimeNanos == 0L
                                ? 0.0
                                : (nowNanos - this.lastRenderTimeNanos) / 1.0E9;
                this.lastRenderTimeNanos = nowNanos;
                if (this.serverPosition != null
                        && !(this.serverPosition.distanceToSqr(vector3) > 400.0)) {
                    float followSpeed = this.catchingUp ? 30.0F : 13.0F;
                    float factor = MathUtil.smoothingFactor(followSpeed, deltaSeconds);
                    this.serverPosition =
                            new Vec3(
                                    Mth.lerp(factor, this.serverPosition.x, vector3.x),
                                    Mth.lerp(factor, this.serverPosition.y, vector3.y),
                                    Mth.lerp(factor, this.serverPosition.z, vector3.z));
                    if (this.catchingUp && this.serverPosition.distanceToSqr(vector3) < 0.01) {
                        this.catchingUp = false;
                    }
                } else {
                    this.serverPosition = vector3;
                    this.catchingUp = false;
                }

                double boxWidth = 0.6;
                double boxHeight = 2.0;
                AABB boundingBox =
                        new AABB(
                                this.serverPosition.x - boxWidth / 2.0,
                                this.serverPosition.y,
                                this.serverPosition.z - boxWidth / 2.0,
                                this.serverPosition.x + boxWidth / 2.0,
                                this.serverPosition.y + boxHeight,
                                this.serverPosition.z + boxWidth / 2.0);
                RenderUtil.drawSettingBox(
                        render3DEvent.getPoseStack(), boundingBox, this.serverPosColor);
            } else {
                this.serverPosition = null;
                this.lastRenderTimeNanos = 0L;
                this.catchingUp = false;
            }
        }
    }

    @Override
    public String getSuffix() {
        String delayText = String.valueOf(this.delayMs.getValue());

        return delayText + " ms";
    }

    private void resetDelay() {
        if (PacketDelay.isDelaying()) {
            PacketDelay.stopAndFlush();
        }

        this.serverPosition = null;
        this.lastRenderTimeNanos = 0L;
        this.catchingUp = false;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (!moduleManager.getModule(Fucker.class).isEnabled()
                && !moduleManager.getModule(Scaffold.class).isEnabled()
                && !moduleManager.getModule(AirStuck.class).isEnabled()
                && mc.gui.screen() == null
                && (!astralis.util.Blink.isActive() || !mc.player.isUsingItem())) {
            if (tickEvent.getStage() != EventStage.POST) {
                if (PacketDelay.isDelaying()) {
                    Vec3 vector3 = PacketDelay.getLastSentPosition();
                    if (vector3 != null) {
                        List targets =
                                BreakUtil.getTargets(
                                        false,
                                        false,
                                        true,
                                        true,
                                        true,
                                        6.0F,
                                        TargetSortOrder.DISTANCE,
                                        true);
                        if (targets != null && !targets.isEmpty()) {
                            LivingEntity livingEntity = (LivingEntity) targets.getFirst();
                            if (vector3.distanceToSqr(livingEntity.position())
                                    < mc.player.distanceToSqr(livingEntity.position())) {
                                PacketDelay.stopAndFlush();
                            }
                        }
                    }
                }

                if (!PacketDelay.isDelaying()) {
                    PacketDelay.startDelay(this.delayMs.getValue().longValue());
                }
            }
        } else {
            this.resetDelay();
        }
    }

    @Override
    public void onDisable() {
        this.resetDelay();
    }

    @EventTarget
    public void onPacket(PacketEvent packetEvent) {
        if (packetEvent.getStage() == EventStage.SEND) {
            if (packetEvent.getPacket() instanceof ServerboundAttackPacket) {
                if (PacketDelay.isDelaying()) {
                    PacketDelay.stopAndFlush();
                }

                this.catchingUp = true;
            }
        }
    }

    private static final String MODULE_ID = "d9fbf0dd79a0069a1001c772d4584596";
    private static final String DELAY_MS_SETTING_ID = "a199e39660a601d1f6a199bbcbd51ae6";
    private static final String SERVER_POS_COLOR_SETTING_ID = "b4a38a4d904d9e036b758cbfeda2c6a1";
}
