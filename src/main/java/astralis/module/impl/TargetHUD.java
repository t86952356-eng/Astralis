package astralis.module.impl;

import astralis.Astralis;
import astralis.event.EventTarget;
import astralis.event.impl.Render2DEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.module.Module;
import astralis.module.impl.combat.KillAura;
import astralis.module.impl.targethud.mode.TargetHudAdjust;
import astralis.module.impl.targethud.mode.TargetHudAstralis;
import astralis.module.impl.targethud.mode.TargetHudLuna;
import astralis.module.impl.targethud.mode.TargetHudLunaV2;
import astralis.module.impl.targethud.mode.TargetHudMode;
import astralis.module.impl.targethud.mode.TargetHudPulsive;
import astralis.setting.ColorSetting;
import astralis.setting.ModeSetting;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.LivingEntity;

import pub.astralis.client.module.core.Category;

import java.awt.Color;
import java.util.List;

public class TargetHUD extends Module {
    private static final int MAX_TARGETS = 4;
    private final ModeSetting mode;
    private final ColorSetting colors;
    private final List<LivingEntity> displayTargets;

    public TargetHUD() {

        super(MODULE_ID, "", "", Category.Misc);

        ModeSetting modeSetting =
                new ModeSetting(MODE_SETTING_ID, this)
                        .addValues(new TargetHudAstralis(this))
                        .addValues(new TargetHudLuna(this))
                        .addValues(new TargetHudLunaV2(this))
                        .addValues(new TargetHudPulsive(this))
                        .addValues(new TargetHudAdjust(this));
        this.mode = modeSetting.setDefault("Astralis");

        this.colors = new ColorSetting(COLORS_SETTING_ID, Color.WHITE);
        this.displayTargets = new java.util.ArrayList<>(4);
        ModeSetting modeSetting2 = this.colors.getColorMode();

        modeSetting2.setDefault("Client");
        this.addSettings(this.mode, this.colors);
    }

    @EventTarget
    public void onVanillaHudRender(VanillaHudRenderEvent event) {
        if (this.mode.getValue() instanceof TargetHudMode targetMode) {
            targetMode.renderVanillaHud(event);
        }
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getName();
    }

    @EventTarget
    public void onRender2D(Render2DEvent render2DEvent) {
        if (this.mode.getValue() instanceof TargetHudMode targetMode) {
            targetMode.updateTargets();

            for (int slot = 0; slot < 4; slot++) {
                targetMode.renderSlot(render2DEvent, slot);
            }
        }
    }

    public List<LivingEntity> getDisplayTargets(LivingEntity livingEntity) {
        this.displayTargets.clear();
        KillAura killAura = Astralis.getInstance().getModuleManager().getModule(KillAura.class);
        if (mc.gui.screen() instanceof ChatScreen) {
            this.displayTargets.add(mc.player);
            return this.displayTargets;
        }

        LivingEntity livingEntity2 = killAura != null ? killAura.target : null;
        if (livingEntity2 == null) {
            return this.displayTargets;
        }

        if (killAura.targets != null && !killAura.targets.isEmpty()) {
            List<LivingEntity> targets = killAura.targets;
            LivingEntity livingEntity3 =
                    livingEntity != null && targets.contains(livingEntity)
                            ? livingEntity
                            : livingEntity2;
            this.displayTargets.add(livingEntity3);

            for (LivingEntity livingEntity4 : targets) {
                if (this.displayTargets.size() >= 4) {
                    break;
                }

                if (livingEntity4 != livingEntity3
                        && !this.displayTargets.contains(livingEntity4)) {
                    this.displayTargets.add(livingEntity4);
                }
            }

            return this.displayTargets;
        } else {
            this.displayTargets.add(livingEntity2);
            return this.displayTargets;
        }
    }

    public ColorSetting getColors() {
        return this.colors;
    }

    private static final String MODULE_ID = "a9d6bc67e56fd35a9740ec3e906e3634";
    private static final String MODE_SETTING_ID = "5e7a9015ea584af4a04a409297fa5600";
    private static final String COLORS_SETTING_ID = "f63ecee06b053c9e652c74133284944b";
}
