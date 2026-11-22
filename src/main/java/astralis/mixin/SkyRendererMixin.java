package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.SkyColorEvent;
import astralis.event.impl.SkyboxExtractEvent;
import astralis.module.impl.Ambience;
import astralis.util.render.Render2DContext;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    private static final long TIME_WRAP_MILLIS = 1000000L;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void postSkyboxExtractEvent(
            ClientLevel clientLevel,
            float partialTick,
            Camera camera,
            SkyRenderState skyRenderState,
            CallbackInfo callbackInfo) {
        SkyboxExtractEvent skyboxExtractEvent = new SkyboxExtractEvent();
        Astralis.getInstance().getEventBus().post(skyboxExtractEvent);
        if (skyboxExtractEvent.getSkybox() != null) {
            skyRenderState.skybox = skyboxExtractEvent.getSkybox();
        }
    }

    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void renderAmbienceSkyDisc(int skyColor, CallbackInfo callbackInfo) {
        Ambience ambience = Astralis.getInstance().getModuleManager().getModule(Ambience.class);
        if (ambience.isEnabled() && ambience.customSkyEnabled.getValue()) {
            callbackInfo.cancel();
            Render2DContext renderContext =
                    Astralis.getInstance().getFramebufferManager().getRenderContext();
            Color color = ambience.customSkyColor.getValue();
            Color color2 = ambience.customSkyColor.getSecondaryColor();
            Matrix4f matrix4f =
                    Minecraft.getInstance()
                            .gameRenderer
                            .mainCamera()
                            .getViewRotationProjectionMatrix(new Matrix4f())
                            .invert();
            float time =
                    (float)
                            (System.currentTimeMillis()
                                    % TIME_WRAP_MILLIS
                                    / 1000.0
                                    * ambience.customSkySpeed.getValue().floatValue());
            renderContext
                    .getSkyRenderer()
                    .render(
                            color,
                            color2,
                            time,
                            1.0F,
                            matrix4f,
                            ambience.useSavedShader.getValue(),
                            ambience.shader2D.getValue());
        }
    }

    @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
    private void cancelDarkDisc(CallbackInfo callbackInfo) {
        if (astralis.util.render.sky.PackSkyManager.getInstance().isSkyFullyReplaced()) {
            callbackInfo.cancel();
        }

        Ambience ambience = Astralis.getInstance().getModuleManager().getModule(Ambience.class);
        if (ambience.isEnabled() && ambience.customSkyEnabled.getValue()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void handleSunMoonAndStars(
            PoseStack poseStack,
            float sunAngle,
            float moonAngle,
            float starAngle,
            MoonPhase moonPhase,
            float rainBrightness,
            float starBrightness,
            CallbackInfo callbackInfo) {
        astralis.util.render.sky.PackSkyManager packSkyManager =
                astralis.util.render.sky.PackSkyManager.getInstance();
        if (packSkyManager.hasPendingSubmits()) {
            packSkyManager.flushSubmits();
        }

        Ambience ambience = Astralis.getInstance().getModuleManager().getModule(Ambience.class);
        if (ambience.isEnabled() && ambience.customSkyEnabled.getValue()) {
            callbackInfo.cancel();
        }

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel != null
                && clientLevel.dimension() == Level.NETHER
                && packSkyManager.hasSkiesFor(Level.NETHER)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderEndSky", at = @At("TAIL"))
    private void flushSkySubmitsAfterEndSky(CallbackInfo callbackInfo) {
        astralis.util.render.sky.PackSkyManager packSkyManager =
                astralis.util.render.sky.PackSkyManager.getInstance();
        if (packSkyManager.hasPendingSubmits()) {
            packSkyManager.flushSubmits();
        }
    }

    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void cancelSkyDiscForCustomSky(int skyColor, CallbackInfo callbackInfo) {
        if (astralis.util.render.sky.PackSkyManager.getInstance().isSkyFullyReplaced()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void cancelSunriseAndSunset(
            PoseStack poseStack, float sunAngle, int color, CallbackInfo callbackInfo) {
        Ambience ambience = Astralis.getInstance().getModuleManager().getModule(Ambience.class);
        if (ambience.isEnabled() && ambience.customSkyEnabled.getValue()) {
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(method = "renderSkyDisc", at = @At("HEAD"), argsOnly = true)
    private int modifySkyDiscColor(int skyColor) {
        SkyColorEvent skyColorEvent = new SkyColorEvent();
        skyColorEvent.setColor(skyColor);
        Astralis.getInstance().getEventBus().post(skyColorEvent);
        return skyColorEvent.getColor();
    }

    static {
    }
}
