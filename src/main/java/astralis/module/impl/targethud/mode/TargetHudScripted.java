package astralis.module.impl.targethud.mode;

import astralis.event.impl.Render2DEvent;
import astralis.module.impl.TargetHUD;

import com.astralis.scripting.Script;
import com.astralis.scripting.api.entity.ScriptEntity;
import com.astralis.scripting.api.hud.ScriptDraggable;
import com.astralis.scripting.api.hud.TargetHudRenderer;
import com.astralis.scripting.api.render.RenderContext;

import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.List;

public final class TargetHudScripted extends TargetHudMode {
    private final Script script;
    private final ScriptDraggable draggable;
    private final TargetHudRenderer hudRenderer;
    private final LivingEntity[] targets = new LivingEntity[4];

    public TargetHudScripted(
            String name,
            TargetHUD targetHUD,
            Script script,
            ScriptDraggable draggable,
            TargetHudRenderer hudRenderer) {
        super(name, targetHUD);
        this.script = script;
        this.draggable = draggable;
        this.hudRenderer = hudRenderer;
    }

    @Override
    public void updateTargets() {
        List displayTargets = this.parent.getDisplayTargets(this.targets[0]);
        int i = 0;

        while (i < 4) {
            this.targets[i] =
                    i < displayTargets.size() ? (LivingEntity) displayTargets.get(i) : null;
            i++;
        }
    }

    @Override
    public void renderSlot(Render2DEvent render2DEvent, int slot) {
        LivingEntity livingEntity = this.targets[slot];
        if (livingEntity != null && this.draggable.isVisible()) {
            float width = this.draggable.getWidth();
            float height = this.draggable.getHeight();
            float scale = this.draggable.getScale();
            float spacing = 4.0F * scale;
            float x =
                    this.draggable.getX()
                            + (slot != 1 && slot != 3 ? 0.0F : width * scale + spacing);
            float y =
                    this.draggable.getY()
                            + (slot != 2 && slot != 3 ? 0.0F : height * scale + spacing);

            try (RenderContext renderContext =
                    new RenderContext(render2DEvent.getContext(), null, 0.0, 0.0, 0.0)) {
                renderContext.push();
                renderContext.translate(x, y);
                renderContext.scale(scale);

                try {
                    this.hudRenderer.render(
                            renderContext, new ScriptEntity(livingEntity, this.script), slot);
                } catch (Throwable e) {
                    this.script.reportError("targetHud." + this.getName(), e);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        Arrays.fill(this.targets, null);
    }
}
