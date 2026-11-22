package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.PlayerUpdateEvent;
import astralis.event.impl.TickEvent;
import astralis.manager.AttackManager;
import astralis.module.Module;
import astralis.module.impl.movement.NoSlowdown;
import astralis.setting.BooleanSetting;
import astralis.setting.RangeSetting;
import astralis.util.InventoryUtil;
import astralis.util.SwingType;

import net.minecraft.world.phys.HitResult.Type;

import org.lwjgl.glfw.GLFW;

import pub.astralis.client.module.core.Category;

public class AutoClicker extends Module {
    private final RangeSetting cps;
    private final BooleanSetting onlyOnHold;
    private final BooleanSetting weaponOnly;
    private final BooleanSetting allowMining;
    private final BooleanSetting watchdog;

    public AutoClicker() {

        super(MODULE_ID, "", "", Category.Misc);

        this.cps = new RangeSetting(CPS_SETTING_ID, 10.0F, 14.0F, 1.0F, 20.0F, 1.0F);

        this.onlyOnHold = new BooleanSetting(ONLY_ON_HOLD_SETTING_ID, true);

        this.weaponOnly = new BooleanSetting(WEAPON_ONLY_SETTING_ID, false);

        this.allowMining = new BooleanSetting(ALLOW_MINING_SETTING_ID, true);

        this.watchdog = new BooleanSetting(WATCHDOG_SETTING_ID, false);
        this.addSettings(
                this.cps, this.onlyOnHold, this.weaponOnly, this.allowMining, this.watchdog);
    }

    @Override
    public String getSuffix() {
        return (int) this.cps.getLow() + "-" + (int) this.cps.getHigh();
    }

    @EventTarget
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (mc.gui.screen() == null) {
            boolean mouseHeld = GLFW.glfwGetMouseButton(mc.getWindow().handle(), 0) == 1;
            if (this.onlyOnHold.getValue() && !mouseHeld) {
                AttackManager.resetClickTimer();
            } else if (this.allowMining.getValue()
                    && mouseHeld
                    && mc.hitResult != null
                    && mc.hitResult.getType() == Type.BLOCK) {
                mc.options.keyAttack.setDown(true);
                AttackManager.resetClickTimer();
            } else if (this.weaponOnly.getValue() && !InventoryUtil.isHoldingWeapon()) {
                AttackManager.resetClickTimer();
            } else {
                AttackManager.attackAtRandomCps(
                        SwingType.CLICK, null, this.cps.getLow(), this.cps.getHigh());
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            if (this.watchdog.getValue()) {
                NoSlowdown noSlowdown =
                        Astralis.getInstance().getModuleManager().getModule(NoSlowdown.class);
                if (noSlowdown != null && noSlowdown.isEnabled()) {
                    if (noSlowdown.getWatchdogAMode().isReblockPending()) {
                        AttackManager.attackTarget(SwingType.CLICK, null);
                    }
                }
            }
        }
    }

    private static final String MODULE_ID = "fc237e5147c27c0e7b0411012a2ec6be";
    private static final String CPS_SETTING_ID = "0166102c4140a86b5fedd00337449734";
    private static final String ONLY_ON_HOLD_SETTING_ID = "81cdeb696a666396efeceaf159c244b6";
    private static final String WEAPON_ONLY_SETTING_ID = "236efa0c075671c350fbcc64c59193f7";
    private static final String ALLOW_MINING_SETTING_ID = "63a9159a042474181dc358680e4cca98";
    private static final String WATCHDOG_SETTING_ID = "f3b5c6ba0034302f7e59e058320a1fb3";
}
