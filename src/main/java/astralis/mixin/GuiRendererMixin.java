package astralis.mixin;

import astralis.Astralis;
import astralis.event.EventBus;
import astralis.event.impl.ContainerScreenRenderEvent;
import astralis.event.impl.PreRender2DEvent;
import astralis.event.impl.Render2DEvent;
import astralis.module.ModuleManager;
import astralis.module.impl.Effects;
import astralis.module.impl.Scoreboard;
import astralis.module.impl.Theme;
import astralis.ui.CustomButtonRenderer;
import astralis.ui.CustomSliderRenderer;
import astralis.ui.TooltipOverlay;
import astralis.ui.component.SapphireScreen;
import astralis.util.render.BlurShadowManager;
import astralis.util.render.Render2DContext;
import astralis.util.render.RenderContextHolder;

import com.astralis.scripting.InstanceAccess;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.Util;
import net.minecraft.world.scores.DisplaySlot;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin implements InstanceAccess {
    @Shadow @Final private GuiRenderState renderState;
    @Shadow @Final private StagedVertexBuffer vertexBuffer;
    @Shadow @Final private List draws;
    @Shadow private int firstDrawIndexAfterBlur;

    @Shadow
    private void prepare() {}

    @Shadow
    private void draw() {}

    @Inject(method = "render", at = @At("HEAD"))
    private void postRender2DEvents(CallbackInfo ci) {
        Window window = mc.getWindow();
        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        int width = window.getWidth();
        int height = window.getHeight();
        float guiScale = window.getGuiScale();
        ModuleManager moduleManager = Astralis.getInstance().getModuleManager();
        if (moduleManager != null && !moduleManager.isEmpty()) {
            RenderContextHolder contextHolder = Astralis.getInstance().getFramebufferManager();
            contextHolder.updateRenderContext(mainTarget, width, height, guiScale);
            Render2DContext context = contextHolder.getRenderContext();
            BlurShadowManager renderEffects = Astralis.getInstance().getRenderEffects();
            EventBus eventBus = Astralis.getInstance().getEventBus();
            Effects effects = moduleManager.getModule(Effects.class);
            Theme theme = moduleManager.getModule(Theme.class);
            boolean inMainMenu =
                    mc.gui.overlay() == null && mc.gui.screen() != null && mc.level == null;
            boolean inMenuScreen =
                    mc.gui.overlay() == null
                            && mc.gui.screen() != null
                            && (mc.level == null || mc.gui.screen() instanceof LevelLoadingScreen);
            boolean customMainMenu = inMenuScreen && theme.getCustomMainMenu().getValue();
            boolean shadowEnabled = effects.isEnabled() && effects.shadow.getValue();
            boolean hasCustomWidgets =
                    CustomButtonRenderer.hasRegisteredButtons()
                            || CustomSliderRenderer.hasRegisteredSliders();
            boolean customWidgetsOnMenu = inMainMenu && hasCustomWidgets && !customMainMenu;
            if (customMainMenu) {
                context.getSandboxRenderer()
                        .render(
                                0.0F,
                                0.0F,
                                window.getGuiScaledWidth(),
                                window.getGuiScaledHeight(),
                                (float) Util.getMillis() / 1000.0F,
                                Astralis.getInstance().getPrimaryColor(),
                                Astralis.getInstance().getSecondaryColor(),
                                1.0F);
            }

            eventBus.post(new PreRender2DEvent(context));
            if (!customWidgetsOnMenu) {
                if ((!customMainMenu || !(mc.gui.screen() instanceof TitleScreen))
                        && (!hasCustomWidgets
                                || inMainMenu && mc.gui.screen() instanceof TitleScreen)) {
                    renderEffects.beginFrame(context);
                } else {
                    renderEffects.updateBlurSnapshot(
                            context,
                            effects.blurIterations.getValue().intValue(),
                            effects.blurDownsampleFactor.getValue(),
                            hasCustomWidgets);
                }
            }

            if (inMainMenu && mc.gui.screen() instanceof SapphireScreen sapphireScreen) {
                sapphireScreen.renderSapphireBackground(context);
            }

            Scoreboard scoreboard = moduleManager.getModule(Scoreboard.class);
            boolean scoreboardVisible =
                    !mc.gui.hud.isHidden()
                            && scoreboard.boardWidth > 0.0F
                            && scoreboard.boardHeight > 0.0F
                            && mc.level != null
                            && mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR)
                                    != null;
            if (shadowEnabled) {
                context.beginRecording();
            }

            if (Effects.isScoreboardBlur()
                    && scoreboard.isEnabled()
                    && scoreboard.getCustomScoreboard().getValue()
                    && scoreboardVisible) {
                renderEffects.blurRoundedRectangle(
                        scoreboard.boardX,
                        scoreboard.boardY,
                        scoreboard.boardWidth,
                        scoreboard.boardHeight,
                        6.0F);
            }

            scoreboard.renderCustomBackground(
                    context,
                    scoreboard.boardX,
                    scoreboard.boardY,
                    scoreboard.boardWidth,
                    scoreboard.boardHeight);
            if (!mc.gui.hud.isHidden()) {
                eventBus.post(new Render2DEvent(context));
            }

            if (shadowEnabled) {
                context.endRecording();
                renderEffects.renderRecordedShadows();
                context.replayCommands();
            }

            context.endFrame();
        }
    }

    @Inject(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V",
                            shift = Shift.BEFORE))
    private void extractSapphireItemsBeforePrepare(CallbackInfo ci) {
        Screen screen2 = mc.gui.screen();
        if (screen2 instanceof SapphireScreen sapphireScreen) {
            sapphireScreen.extractSapphireItems(
                    new GuiGraphicsExtractor(mc, this.renderState, 0, 0));
        }
    }

    @Redirect(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/CubeMap;render(FF)V"))
    private void redirectPanoramaRender(CubeMap cubeMap, float pitch, float yaw) {
        Astralis astralisInstance = Astralis.getInstance();
        ModuleManager moduleManager = astralisInstance.getModuleManager();
        if (mc.gui.screen() == null
                || mc.level != null && !(mc.gui.screen() instanceof LevelLoadingScreen)
                || moduleManager == null
                || moduleManager.isEmpty()
                || !moduleManager.getModule(Theme.class).getCustomMainMenu().getValue()) {
            cubeMap.render(pitch, yaw);
            if (mc.gui.screen() != null
                    && (mc.level == null || mc.gui.screen() instanceof LevelLoadingScreen)
                    && moduleManager != null
                    && !moduleManager.isEmpty()
                    && !moduleManager.getModule(Theme.class).getCustomMainMenu().getValue()
                    && (CustomButtonRenderer.hasRegisteredButtons()
                            || CustomSliderRenderer.hasRegisteredSliders())) {
                Render2DContext context =
                        astralisInstance.getFramebufferManager().getRenderContext();
                astralisInstance.getRenderEffects().invalidateCache();
                Effects effects =
                        Astralis.getInstance().getModuleManager().getModule(Effects.class);
                astralisInstance
                        .getRenderEffects()
                        .updateBlurSnapshot(
                                context,
                                effects.blurIterations.getValue().intValue(),
                                effects.blurDownsampleFactor.getValue(),
                                true);
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderRegisteredOverlays(CallbackInfo ci) {
        if (mc.gui.overlay() != null) {
            CustomButtonRenderer.clearRegistered();
            CustomSliderRenderer.clearRegistered();
            TooltipOverlay.clearPendingTooltip();
        } else {
            this.renderSapphireScreen();
            Render2DContext containerContext =
                    Astralis.getInstance().getFramebufferManager().getRenderContext();
            if (mc.gui.screen() instanceof AbstractContainerScreen && containerContext != null) {
                Astralis.getInstance()
                        .getEventBus()
                        .post(new ContainerScreenRenderEvent(containerContext));
                containerContext.endFrame();
            }

            if (mc.gui.screen() instanceof SapphireScreen
                    || CustomButtonRenderer.hasRegisteredButtons()
                    || CustomSliderRenderer.hasRegisteredSliders()
                    || TooltipOverlay.hasPendingTooltip()) {
                Render2DContext overlayContext =
                        Astralis.getInstance().getFramebufferManager().getRenderContext();
                if (overlayContext == null) {
                    CustomButtonRenderer.clearRegistered();
                    CustomSliderRenderer.clearRegistered();
                    TooltipOverlay.clearPendingTooltip();
                } else {
                    CustomButtonRenderer.renderRegistered(overlayContext);
                    CustomSliderRenderer.renderRegistered(overlayContext);
                    TooltipOverlay.renderRegistered(overlayContext);
                    overlayContext.endFrame();
                }
            }
        }
    }

    @Unique
    private void renderSapphireScreen() {
        Screen screen2 = mc.gui.screen();
        if (screen2 instanceof SapphireScreen sapphireScreen) {
            Render2DContext context =
                    Astralis.getInstance().getFramebufferManager().getRenderContext();
            if (context != null) {
                sapphireScreen.renderSapphire(context);
                context.endFrame();
                sapphireScreen.extractSapphireItems(
                        new GuiGraphicsExtractor(mc, this.renderState, 0, 0));
                if (!this.renderState.getItemModelIdentities().isEmpty()) {
                    this.prepare();
                    this.vertexBuffer.upload();
                    this.draw();
                    this.vertexBuffer.endDraw();
                    this.vertexBuffer.endFrame();
                    this.draws.clear();
                    this.renderState.reset();
                    this.firstDrawIndexAfterBlur = Integer.MAX_VALUE;
                }
            }
        }
    }
}
