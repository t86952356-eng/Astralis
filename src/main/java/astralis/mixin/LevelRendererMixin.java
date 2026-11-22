package astralis.mixin;

import astralis.Astralis;
import astralis.event.impl.BlockOutlineRenderEvent;
import astralis.module.impl.AntiClutter;
import astralis.module.impl.BlockOverlay;
import astralis.module.impl.ESP;
import astralis.module.impl.Particles;

import com.astralis.scripting.InstanceAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChain.TargetBundle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.Color;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements InstanceAccess {
    @Unique private int astralis$blockOutlineColor;
    @Unique private float astralis$blockOutlineWidth;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            CameraRenderState cameraRenderState2,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean renderSky,
            CallbackInfo ci) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        if (particles != null) {
            particles.prepareGlowTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }

        BlockOverlay blockOverlay =
                Astralis.getInstance().getModuleManager().getModule(BlockOverlay.class);
        if (blockOverlay != null) {
            blockOverlay.clearOutlineTarget();
        }
    }

    @Inject(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V",
                            shift = Shift.AFTER))
    private void onFrameGraphExecuted(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            CameraRenderState cameraRenderState2,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean renderSky,
            CallbackInfo ci) {
        Particles particles = Astralis.getInstance().getModuleManager().getModule(Particles.class);
        if (particles != null) {
            particles.blendIntoGlowTarget(mc.gameRenderer.mainRenderTarget());
        }

        BlockOverlay blockOverlay =
                Astralis.getInstance().getModuleManager().getModule(BlockOverlay.class);
        if (blockOverlay != null) {
            blockOverlay.renderGlow();
        }
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void postBlockOutlineRenderEvent(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        BlockOutlineRenderState blockOutlineRenderState2 = levelRenderState.blockOutlineRenderState;
        if (blockOutlineRenderState2 != null) {
            this.astralis$blockOutlineColor =
                    blockOutlineRenderState2.highContrast() ? -11010079 : ARGB.black(102);
            this.astralis$blockOutlineWidth =
                    mc.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
            Vec3 vector3 = levelRenderState.cameraRenderState.pos;
            BlockOutlineRenderEvent event =
                    new BlockOutlineRenderEvent(
                            blockOutlineRenderState2,
                            vector3.x,
                            vector3.y,
                            vector3.z,
                            poseStack,
                            submitNodeCollector,
                            this.astralis$blockOutlineColor,
                            this.astralis$blockOutlineWidth);
            Astralis.getInstance().getEventBus().post(event);
            this.astralis$blockOutlineColor = event.getColor();
            this.astralis$blockOutlineWidth = event.getLineWidth();
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @ModifyArgs(
            method = "submitBlockOutline",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",
                            ordinal = 0))
    private void forceWhiteHitOutlineColor(Args args) {
        args.set(4, Color.white.getRGB());
    }

    @ModifyArgs(
            method = "submitBlockOutline",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;submitHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/state/level/BlockOutlineRenderState;IFZ)V",
                            ordinal = 1))
    private void applyEventBlockOutlineStyle(Args args) {
        args.set(4, this.astralis$blockOutlineColor);
        args.set(5, this.astralis$blockOutlineWidth);
    }

    @Inject(method = "submitBlockOutline", at = @At("RETURN"))
    private void resetBlockOutlineState(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        this.astralis$blockOutlineColor = 0;
        this.astralis$blockOutlineWidth = 0.0F;
    }

    @ModifyVariable(method = "addSkyPass", at = @At("HEAD"), argsOnly = true)
    private CameraRenderState disableMobEffectSkyBlock(CameraRenderState cameraRenderState2) {
        AntiClutter antiClutter =
                Astralis.getInstance().getModuleManager().getModule(AntiClutter.class);
        if (antiClutter.isEnabled()
                && (antiClutter.blindness.getValue() || antiClutter.darkness.getValue())) {
            cameraRenderState2.entityRenderState.doesMobEffectBlockSky = false;
        }

        return cameraRenderState2;
    }

    @Inject(method = "addSkyPass", at = @At("HEAD"))
    private void updateCustomSky(
            FrameGraphBuilder frameGraphBuilder,
            CameraRenderState cameraState,
            GpuBufferSlice fogBuffer,
            CallbackInfo ci) {
        astralis.util.render.sky.PackSkyManager.getInstance()
                .buildSubmits(mc.level, mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    @WrapOperation(
            method = "addSkyPass",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/renderer/state/level/SkyRenderState;skybox:Lnet/minecraft/world/level/dimension/DimensionType$Skybox;",
                            opcode = 180))
    private Skybox forceOverworldSkyboxInNether(SkyRenderState skyRenderState, Operation original) {
        return mc.level != null
                        && mc.level.dimension() == Level.NETHER
                        && astralis.util.render.sky.PackSkyManager.getInstance()
                                .hasSkiesFor(Level.NETHER)
                ? Skybox.OVERWORLD
                : (Skybox) original.call(skyRenderState);
    }

    @Redirect(
            method = "render",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/PostChain;addToFrame(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;IILnet/minecraft/client/renderer/PostChain$TargetBundle;)V",
                            ordinal = 0))
    private void skipEntityOutlinePostChain(
            PostChain postChain,
            FrameGraphBuilder frameGraphBuilder,
            int width,
            int height,
            TargetBundle targetBundle) {
        ESP esp = Astralis.getInstance().getModuleManager().getModule(ESP.class);
        if (esp == null || !esp.isEntityEffectActive()) {
            postChain.addToFrame(frameGraphBuilder, width, height, targetBundle);
        }
    }

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void cancelEntityOutline(CallbackInfo ci) {
        ESP esp = Astralis.getInstance().getModuleManager().getModule(ESP.class);
        if (esp != null && esp.isEntityEffectActive()) {
            ci.cancel();
        }
    }
}
