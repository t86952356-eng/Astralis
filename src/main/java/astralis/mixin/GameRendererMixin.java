package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.HurtCameraEvent;
import astralis.event.impl.LevelRenderEvent;
import astralis.event.impl.Render3DEvent;
import astralis.event.impl.WorldOverlayRenderEvent;
import astralis.module.impl.AntiClutter;
import astralis.module.impl.BlockOverlay;
import astralis.module.impl.HandView;
import astralis.module.impl.world.Scaffold;
import astralis.rotation.RotationManager;
import astralis.ui.imgui.ImGuiManager;
import astralis.ui.imgui.ImGuiScreen;
import astralis.ui.screen.ImGuiClickGuiScreen;
import astralis.util.render.Render2DContext;
import astralis.util.render.ScreenProjector;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import imgui.ImGui;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private Camera mainCamera;
    @Unique private HitResult savedHitResult = null;

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        Astralis.getInstance().getRenderEffects().recreateOutputTarget(width, height);
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    void postHurtCameraEvent(
            CameraRenderState cameraRenderState2, PoseStack poseStack, CallbackInfo ci) {
        HurtCameraEvent event = new HurtCameraEvent();
        Astralis.getInstance().getEventBus().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
                            shift = Shift.AFTER))
    private void afterRenderLevelUnused(
            DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {}

    @Inject(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                            ordinal = 2))
    public void postLevelRenderEvent(
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local(name = "worldPartialTicks") float worldPartialTicks,
            @Local(name = "bobStack") PoseStack bobStack) {
        Astralis.getInstance()
                .getEventBus()
                .post(new LevelRenderEvent(bobStack, worldPartialTicks, this.mainCamera));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTailUnused(
            DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {}

    @Inject(method = "render", at = @At("RETURN"))
    private void renderImGuiScreen(
            DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (this.minecraft.gui.screen() instanceof ImGuiScreen screen) {
            ImGuiManager.beginFrame();
            if (Render2DContext.isOpenGlBackend()) {
                screen.renderImGui(ImGui.getIO());
            }

            ImGuiManager.endFrame();
        }
    }

    @Inject(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V",
                            ordinal = 1))
    private void postRender3DEvents(
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local(name = "projectionMatrix") Matrix4f projectionMatrix) {
        CameraRenderState cameraRenderState2 =
                this.gameRenderState.levelRenderState.cameraRenderState;
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(cameraRenderState2.viewRotationMatrix);
        Astralis.getInstance().getEventBus().post(new Render3DEvent(poseStack));
        Astralis.getInstance()
                .getFramebufferManager()
                .updateRenderContext(
                        this.minecraft.gameRenderer.mainRenderTarget(),
                        this.minecraft.getWindow().getWidth(),
                        this.minecraft.getWindow().getHeight(),
                        this.minecraft.getWindow().getGuiScale());
        if (!this.minecraft.gui.hud.isHidden()) {
            Astralis.getInstance()
                    .getEventBus()
                    .post(
                            new WorldOverlayRenderEvent(
                                    Astralis.getInstance()
                                            .getFramebufferManager()
                                            .getRenderContext(),
                                    cameraRenderState2));
        }

        ScreenProjector.setModelViewMatrix(new Matrix4f(poseStack.last().pose()));
        ScreenProjector.setProjectionMatrix(projectionMatrix);
    }

    @ModifyExpressionValue(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float suppressNauseaDistortion(float original) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        return (!antiClutter.isEnabled() || !antiClutter.nausea.getValue())
                        && !(InstanceAccess.mc.gui.screen() instanceof ImGuiClickGuiScreen)
                ? original
                : 0.0F;
    }

    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void cancelTotemAnimation(ItemStack itemStack, CallbackInfo ci) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled() && antiClutter.totemAnimation.getValue()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/GameRenderer;shouldRenderBlockOutline()Z"))
    private boolean forceBlockOutlineRender(boolean original) {
        BlockOverlay blockOverlay =
                Astralis.getInstance().getModuleManager().getModule(BlockOverlay.class);
        return original || blockOverlay.isEnabled();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"))
    private void beginHandViewTarget(
            CameraRenderState cameraRenderState2,
            float partialTick,
            Matrix4fc modelViewMatrix,
            CallbackInfo ci) {
        HandView handView = Astralis.getInstance().getModuleManager().getModule(HandView.class);
        if (handView.isGlowActive() && InstanceAccess.mc.options.getCameraType().isFirstPerson()) {
            int width = InstanceAccess.mc.getWindow().getWidth();
            int height = InstanceAccess.mc.getWindow().getHeight();
            RenderTarget renderTarget = handView.getOrCreateGlowTarget(width, height);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (renderTarget.getColorTexture() != null) {
                commandEncoder.clearColorTexture(
                        renderTarget.getColorTexture(), new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
            }

            if (renderTarget.getDepthTexture() != null) {
                commandEncoder.clearDepthTexture(renderTarget.getDepthTexture(), 0.0);
            }

            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("TAIL"))
    private void endHandViewTarget(
            CameraRenderState cameraRenderState2,
            float partialTick,
            Matrix4fc modelViewMatrix,
            CallbackInfo ci) {
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
        HandView handView = Astralis.getInstance().getModuleManager().getModule(HandView.class);
        if (handView.isGlowActive() && InstanceAccess.mc.options.getCameraType().isFirstPerson()) {
            RenderTarget renderTarget = handView.getGlowTarget();
            if (renderTarget != null) {
                renderTarget.blitAndBlendToTexture(
                        InstanceAccess.mc.gameRenderer.mainRenderTarget().getColorTextureView(),
                        InstanceAccess.mc.gameRenderer.mainRenderTarget().getDepthTextureView());
            }
        }
    }

    @Inject(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract(Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/Camera;F)V"))
    private void useScaffoldHitResult(
            DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        BlockOverlay blockOverlay =
                Astralis.getInstance().getModuleManager().getModule(BlockOverlay.class);
        Scaffold scaffold = Astralis.getInstance().getModuleManager().getModule(Scaffold.class);
        if (blockOverlay.isEnabled()
                && scaffold.isEnabled()
                && InstanceAccess.mc.level != null
                && InstanceAccess.mc.player != null) {
            float yaw = RotationManager.getInstance().getYaw();
            float pitch = RotationManager.getInstance().getPitch();
            float yawRad = (float) Math.toRadians(-yaw - 90.0F);
            float pitchRad = (float) Math.toRadians(-pitch);
            float cosYaw = (float) Math.cos(yawRad);
            float sinYaw = (float) Math.sin(yawRad);
            float cosPitch = (float) Math.cos(pitchRad);
            float sinPitch = (float) Math.sin(pitchRad);
            Vec3 vector3 = new Vec3(cosYaw * cosPitch, sinPitch, sinYaw * cosPitch);
            Vec3 vector32 = InstanceAccess.mc.player.getEyePosition(1.0F);
            Vec3 vector33 = vector32.add(vector3.scale(4.5));
            BlockHitResult blockHitResult =
                    InstanceAccess.mc.level.clip(
                            new ClipContext(
                                    vector32,
                                    vector33,
                                    Block.OUTLINE,
                                    Fluid.NONE,
                                    InstanceAccess.mc.player));
            if (blockHitResult.getType() != Type.MISS) {
                this.savedHitResult = InstanceAccess.mc.hitResult;
                InstanceAccess.mc.hitResult = blockHitResult;
            }
        }
    }

    @Inject(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract(Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/Camera;F)V",
                            shift = Shift.AFTER))
    private void restoreOriginalHitResult(
            DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (this.savedHitResult != null) {
            InstanceAccess.mc.hitResult = this.savedHitResult;
            this.savedHitResult = null;
        }
    }
}
