package astralis.module.impl.movement.noslow;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ItemUseSlowdownEvent;
import astralis.event.impl.ItemUseSprintEvent;
import astralis.event.impl.MotionEvent;
import astralis.event.impl.PlayerAiStepEvent;
import astralis.event.impl.PostMotionEvent;
import astralis.event.impl.StartUseItemEvent;
import astralis.event.impl.TickEvent;
import astralis.mixin.MinecraftAccessor;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.Mode;
import astralis.util.Blink;
import astralis.util.InventoryUtil;

import net.minecraft.core.component.DataComponents;

public class NoSlowWatchdogA extends Mode<NoSlowdown> {
    private boolean awaitingReblock;
    private boolean blocking;
    private boolean justReleased;
    private boolean blinking;
    private boolean reblockPending;
    private boolean internalStartUse;

    public NoSlowWatchdogA(NoSlowdown noSlowdown) {
        super("Watchdog A", noSlowdown);
    }

    private void resetState() {
        this.reblockPending = false;
        if (this.blinking) {
            Blink.stop();
            this.blinking = false;
        }

        if (this.blocking) {
            mc.gameMode.releaseUsingItem(mc.player);
            this.blocking = false;
        }
    }

    @EventTarget
    public void onPostMotion(PostMotionEvent event) {
        if (!this.shouldBlock()) {
            this.resetState();
        } else {
            if (this.awaitingReblock) {
                this.reblockPending = true;
            } else if (this.justReleased) {
                this.awaitingReblock = true;
                this.justReleased = false;
            }
        }
    }

    private boolean shouldNoSlow() {
        return this.shouldBlock()
                || mc.player != null
                        && InventoryUtil.isHoldingSword()
                        && (mc.player.isUsingItem() || this.blocking);
    }

    @EventTarget
    public void onStartUseItem(StartUseItemEvent startUseItemEvent) {
        if (!this.internalStartUse && InventoryUtil.isHoldingSword()) {
            startUseItemEvent.setCancelled(true);
        }
    }

    private boolean isKillAuraBlocking() {
        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        return killAura != null && killAura.isEnabled() && killAura.isAutoBlocking();
    }

    @EventTarget
    public void onItemUseSprint(ItemUseSprintEvent event) {
        if (this.shouldNoSlow()) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onPlayerAiStep(PlayerAiStepEvent event) {
        if (mc.player.isUsingItem()
                && mc.player.getUseItem().has(DataComponents.FOOD)
                && this.parent.getFood().getValue()) {
            mc.player.setSprinting(true);
        }
    }

    @EventTarget
    public void onMotion(MotionEvent motionEvent) {
        if (!this.shouldBlock()) {
            this.resetState();
        } else if (this.awaitingReblock) {
            this.reblockPending = true;
        } else {
            this.reblockPending = false;
            Blink.startOutgoing();
            this.blinking = true;
            mc.gameMode.releaseUsingItem(mc.player);
            this.blocking = false;
            this.justReleased = true;
        }
    }

    @EventTarget
    public void onItemUseSlowdown(ItemUseSlowdownEvent event) {
        if (this.shouldNoSlow()) {
            event.setCancelled(true);
            event.setMultiplier(0.0F);
        }
    }

    public boolean isReblockPending() {
        return this.reblockPending;
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (this.reblockPending) {
                this.internalStartUse = true;
                ((MinecraftAccessor) mc).callStartUseItem();
                this.internalStartUse = false;
                this.blocking = true;
                Blink.stop();
                this.blinking = false;
                this.awaitingReblock = false;
                this.reblockPending = false;
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.blinking) {
            Blink.stop();
        }

        this.awaitingReblock = false;
        this.blocking = false;
        this.justReleased = false;
        this.blinking = false;
        this.reblockPending = false;
    }

    private boolean shouldBlock() {
        return !InventoryUtil.isHoldingSword()
                ? false
                : this.isKillAuraBlocking() || mc.options.keyUse.isDown();
    }
}
