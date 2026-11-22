package astralis.module.impl.movement.noslow;

import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.ItemUseSlowdownEvent;
import astralis.event.impl.ItemUseSprintEvent;
import astralis.event.impl.TickEvent;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.BooleanSetting;
import astralis.setting.Mode;
import astralis.setting.ModeSetting;
import astralis.util.InventoryUtil;
import astralis.util.PacketUtil;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

public class NoSlowGrim extends Mode<NoSlowdown> {
    private final ModeSetting grimMode;
    private final BooleanSetting fullWithSword18Only;
    private int useTicks;

    public NoSlowGrim(NoSlowdown noSlowdown) {

        super("Grim", noSlowdown);

        ModeSetting modeSetting = new ModeSetting(GRIM_MODE_SETTING_ID);
        ModeSetting modeSetting2 = modeSetting.addMode("2 Tick");
        ModeSetting modeSetting3 = modeSetting2.addMode("1.19");
        this.grimMode = modeSetting3.setDefault("2 Tick");
        this.fullWithSword18Only = new BooleanSetting(FULL_WITH_SWORD18_ONLY_SETTING_ID, true);
        this.fullWithSword18Only.hideIf(() -> !this.grimMode.is("2 Tick"));
        this.addSettings(this.grimMode, this.fullWithSword18Only);
    }

    @EventTarget
    public void onItemUseSprint(ItemUseSprintEvent event) {
        if (this.parent.shouldNoSlow()) {
            if (!InventoryUtil.isHoldingSword()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {

        if (tickEvent.getStage() == EventStage.PRE) {
            this.useTicks = mc.player.isUsingItem() ? this.useTicks + 1 : 0;

            if (this.grimMode.is("1.19")
                    && mc.player.isUsingItem()
                    && this.parent.shouldNoSlow()
                    && this.useTicks > 3) {

                if (this.useTicks < 7) {
                    boolean usingMainHand =
                            mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
                    PacketUtil.sendPrediction(
                            sequence -> {
                                return new ServerboundUseItemPacket(
                                        usingMainHand
                                                ? InteractionHand.OFF_HAND
                                                : InteractionHand.MAIN_HAND,
                                        sequence,
                                        mc.player.getYRot(),
                                        mc.player.getXRot());
                            });
                }
            }
        }
    }

    @EventTarget
    public void onItemUseSlowdown(ItemUseSlowdownEvent event) {
        if (this.parent.shouldNoSlow() || mc.player.isUsingItem()) {
            switch (this.grimMode.getValue().getName()) {
                case "2 tick":
                    if (InventoryUtil.isHoldingSword()
                            && this.fullWithSword18Only.getValue()
                            && this.parent.shouldNoSlow()) {
                        event.setCancelled(true);
                    } else if (this.useTicks >= 2) {
                        event.setCancelled(true);
                        this.useTicks = 0;
                    }
                    break;
                case "1.19":
                    if (this.useTicks > 2) {
                        event.setCancelled(true);
                    }
            }
        }
    }

    private static final String GRIM_MODE_SETTING_ID = "47bddca4943a0a03ab608de4b860ef93";
    private static final String FULL_WITH_SWORD18_ONLY_SETTING_ID =
            "10d40fc92d54ff7bd3e0709ca0c6fce6d0b108c29b4b58b157b44d9f118fae16";
}
