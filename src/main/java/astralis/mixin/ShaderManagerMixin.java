package astralis.mixin;

import astralis.metadata.MetadataState;
import astralis.util.render.sky.SkyShaderManager;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.ShaderManager.Configs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map.Entry;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SHADER_CATALOG_TIMEOUT_MS = 30000L;

    @Inject(method = "prepare*", at = @At("RETURN"), cancellable = true)
    private void injectLiveShaderSources(
            ResourceManager resourceManager,
            ProfilerFiller profilerFiller,
            CallbackInfoReturnable callbackInfoReturnable) {
        MetadataState metadataState = MetadataState.getInstance();
        if (metadataState.hasShaderResources()
                || metadataState.awaitResources(SHADER_CATALOG_TIMEOUT_MS)
                        && metadataState.hasShaderResources()) {
            HashMap shaderSources =
                    new HashMap<>(
                            ((Configs) callbackInfoReturnable.getReturnValue()).shaderSources());

            try {
                Class sourceKeyClass =
                        Class.forName(
                                "net.minecraft.client.renderer.ShaderManager$ShaderSourceKey");
                Constructor sourceKeyConstructor =
                        sourceKeyClass.getDeclaredConstructor(Identifier.class, ShaderType.class);
                sourceKeyConstructor.setAccessible(true);

                for (Entry entry : metadataState.getResources().entrySet()) {
                    String resourceName = (String) entry.getKey();
                    if (resourceName.startsWith("shader:")) {
                        Identifier identifier =
                                Identifier.parse(resourceName.substring("shader:".length()));
                        ShaderType shaderType = ShaderType.byLocation(identifier);
                        if (shaderType != null) {
                            Identifier identifier2 = shaderType.idConverter().fileToId(identifier);
                            shaderSources.put(
                                    sourceKeyConstructor.newInstance(identifier2, shaderType),
                                    entry.getValue());
                        }
                    }
                }

                String skyShaderSource =
                        metadataState.getResource(
                                "shader:astralis-recode:shaders/misc/sky_shader.fsh");
                if (skyShaderSource == null || skyShaderSource.isBlank()) {
                    throw new IllegalStateException("Missing live sky shader");
                }

                shaderSources.put(
                        sourceKeyConstructor.newInstance(
                                SkyShaderManager.customSkyShaderId, ShaderType.FRAGMENT),
                        SkyShaderManager.loadActiveShaderSource(skyShaderSource));
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject shaders", e);
            }

            callbackInfoReturnable.setReturnValue(
                    new Configs(
                            shaderSources,
                            ((Configs) callbackInfoReturnable.getReturnValue()).postChains()));
        } else {
            throw new IllegalStateException("Missing live shader catalog");
        }
    }

    static {
    }
}
