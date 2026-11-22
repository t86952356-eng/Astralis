package astralis.module.impl.targethud.mode;

import astralis.event.impl.Render2DEvent;
import astralis.event.impl.VanillaHudRenderEvent;
import astralis.module.impl.TargetHUD;
import astralis.setting.Mode;
import astralis.ui.hud.Draggable;

import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

public abstract class TargetHudMode extends Mode<TargetHUD> {
    protected static final int MAX_TARGETS = 4;
    protected static final float SLOT_SPACING = 4.0F;

    protected TargetHudMode(String name, TargetHUD targetHUD) {
        super(name, targetHUD);
    }

    public abstract void renderSlot(Render2DEvent render2DEvent, int slot);

    protected Color getHurtTint(LivingEntity livingEntity, int alpha) {
        float hurtProgress =
                livingEntity.hurtTime == 0
                        ? 0.0F
                        : this.clampToUnit(
                                (livingEntity.hurtTime
                                                - mc.getDeltaTracker()
                                                        .getGameTimeDeltaPartialTick(true))
                                        * 0.3F);
        int tintChannel = (int) (255.0F + -84.0F * hurtProgress);

        return new Color(255, tintChannel, tintChannel, Math.max(0, Math.min(255, alpha)));
    }

    protected Color[] getThemeColors() {
        return this.parent.getColors().getColors();
    }

    public void renderVanillaHud(VanillaHudRenderEvent event) {}

    protected float[] getSlotPosition(Draggable draggable, int slot, float width, float height) {
        float scale = draggable.getScale();
        float spacing = 4.0F * scale;
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;
        float x = draggable.getX();
        float y = draggable.getY();

        return switch (slot) {
            case 0 -> new float[] {x, y};
            case 1 -> new float[] {x + scaledWidth + spacing, y};
            case 2 -> new float[] {x, y + scaledHeight + spacing};
            case 3 -> new float[] {x + scaledWidth + spacing, y + scaledHeight + spacing};
            default -> new float[] {x, y};
        };
    }

    protected float clampToUnit(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    public abstract void updateTargets();
}
