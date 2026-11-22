package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.AntiClutter;
import astralis.module.impl.Scoreboard;
import astralis.ui.screen.DropdownClickGuiScreen;
import astralis.ui.screen.ImGuiClickGuiScreen;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Objective;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin implements InstanceAccess {
    @Unique private boolean astralisRecode$pushedScoreboardMatrix;

    @Inject(
            method =
                    "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void applyScoreboardTransform(
            GuiGraphicsExtractor guiGraphicsExtractor, Objective objective, CallbackInfo ci) {
        this.astralisRecode$pushedScoreboardMatrix = false;
        Scoreboard scoreboard =
                Astralis.getInstance().getModuleManager().getModule(Scoreboard.class);
        if (scoreboard.isEnabled()) {
            if (scoreboard.getRemoveScoreboard().getValue()
                    && !scoreboard.getCustomScoreboard().getValue()) {
                ci.cancel();
                return;
            }

            if (scoreboard.getHudWidget() != null) {
                guiGraphicsExtractor.pose().pushMatrix();
                this.astralisRecode$pushedScoreboardMatrix = true;
                float scale = scoreboard.getHudWidget().getScale();
                float offsetX =
                        scoreboard.getHudWidget().getX()
                                - guiGraphicsExtractor.guiWidth()
                                + scoreboard.boardWidth * scale
                                + 3.0F;
                float offsetY = scoreboard.getHudWidget().getY() - scoreboard.vanillaTopY;
                guiGraphicsExtractor.pose().translate(offsetX, offsetY);
                if (scale != 1.0F) {
                    guiGraphicsExtractor
                            .pose()
                            .translate(
                                    guiGraphicsExtractor.guiWidth() - scoreboard.boardWidth,
                                    scoreboard.vanillaTopY);
                    guiGraphicsExtractor.pose().scale(scale, scale);
                    guiGraphicsExtractor
                            .pose()
                            .translate(
                                    -(guiGraphicsExtractor.guiWidth() - scoreboard.boardWidth),
                                    -scoreboard.vanillaTopY);
                }
            }
        }
    }

    @Inject(
            method =
                    "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("RETURN"))
    private void popScoreboardTransform(
            GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (this.astralisRecode$pushedScoreboardMatrix) {
            graphics.pose().popMatrix();
            this.astralisRecode$pushedScoreboardMatrix = false;
        }
    }

    @ModifyVariable(
            method =
                    "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
            at = @At(value = "STORE", ordinal = 0))
    private NumberFormat useBlankScoreboardNumberFormat(
            NumberFormat numberFormat,
            GuiGraphicsExtractor guiGraphicsExtractor,
            Objective objective) {
        Scoreboard scoreboard =
                Astralis.getInstance().getModuleManager().getModule(Scoreboard.class);
        return scoreboard.isEnabled() && scoreboard.getRemoveScore().getValue()
                ? BlankFormat.INSTANCE
                : numberFormat;
    }

    @Redirect(
            method = "displayScoreboardSidebar",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                            ordinal = 0))
    private void captureScoreboardTopBounds(
            GuiGraphicsExtractor guiGraphicsExtractor,
            int minX,
            int minY,
            int maxX,
            int maxY,
            int color) {
        Scoreboard scoreboard =
                Astralis.getInstance().getModuleManager().getModule(Scoreboard.class);
        scoreboard.vanillaTopY = minY - 2;
        if (scoreboard.getHudWidget() != null) {
            float offsetX =
                    scoreboard.getHudWidget().getX()
                            - guiGraphicsExtractor.guiWidth()
                            + (maxX - minX)
                            + 3.0F;
            float offsetY = scoreboard.getHudWidget().getY() - scoreboard.vanillaTopY;
            scoreboard.boardX = minX + offsetX;
            scoreboard.boardY = minY - 2 + offsetY;
            scoreboard.boardWidth = maxX - minX;
            scoreboard.getHudWidget().setWidth(scoreboard.boardWidth);
        } else {
            scoreboard.boardX = minX;
            scoreboard.boardY = minY - 2;
            scoreboard.boardWidth = maxX - minX;
        }

        if (!scoreboard.isEnabled() || !scoreboard.getCustomScoreboard().getValue()) {
            guiGraphicsExtractor.fill(
                    minX,
                    minY,
                    maxX,
                    maxY,
                    scoreboard.isEnabled() ? scoreboard.getBackgroundColor() : color);
        }
    }

    @Redirect(
            method = "displayScoreboardSidebar",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                            ordinal = 1))
    private void captureScoreboardBottom(
            GuiGraphicsExtractor graphics, int minX, int minY, int maxX, int maxY, int color) {
        Scoreboard scoreboard =
                Astralis.getInstance().getModuleManager().getModule(Scoreboard.class);
        scoreboard.boardHeight = maxY - scoreboard.vanillaTopY;
        if (scoreboard.getHudWidget() != null) {
            scoreboard.getHudWidget().setHeight(scoreboard.boardHeight);
        }

        if (!scoreboard.isEnabled() || !scoreboard.getCustomScoreboard().getValue()) {
            graphics.fill(
                    minX,
                    minY,
                    maxX,
                    maxY,
                    scoreboard.isEnabled() ? scoreboard.getBackgroundColor() : color);
        }
    }

    @Redirect(
            method = "displayScoreboardSidebar",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
                            ordinal = 1))
    private void transformScoreboardLineText(
            GuiGraphicsExtractor guiGraphicsExtractor,
            Font font,
            Component component,
            int x,
            int y,
            int color,
            boolean dropShadow) {
        Scoreboard scoreboard =
                Astralis.getInstance().getModuleManager().getModule(Scoreboard.class);
        component = scoreboard.applyCustomIp(component);
        guiGraphicsExtractor.text(font, component, x, y, color, dropShadow);
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelConfusionOverlay(
            GuiGraphicsExtractor guiGraphicsExtractor, float strength, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.nausea.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelPortalOverlay(GuiGraphicsExtractor graphics, float alpha, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.portalOverlay.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void cancelVignetteOverlay(
            GuiGraphicsExtractor graphics, Entity entity, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.vignetteShadow.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void cancelEffectsOverlay(
            GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.statusEffectsHud.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void cancelCrosshair(
            GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.crosshair.getValue()
                || mc.gui.screen() instanceof ImGuiClickGuiScreen
                || mc.gui.screen() instanceof DropdownClickGuiScreen) {
            ci.cancel();
        }
    }
}
