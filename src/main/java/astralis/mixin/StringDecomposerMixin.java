package astralis.mixin;

import astralis.Astralis;
import astralis.module.impl.NameHider;

import com.astralis.scripting.InstanceAccess;

import net.minecraft.util.StringDecomposer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {

    @ModifyVariable(method = "iterate", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static String hideNameInIterate(String text) {
        return replacePlayerName(text);
    }

    @ModifyVariable(method = "iterateFormatted*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static String hideNameInIterateFormatted(String text) {
        return replacePlayerName(text);
    }

    @Unique
    private static String replacePlayerName(String text) {
        if (text != null && InstanceAccess.mc.player != null) {
            String result = text;
            NameHider nameHider =
                    Astralis.getInstance().getModuleManager().getModule(NameHider.class);
            if (nameHider.isEnabled()
                    && result.contains(InstanceAccess.mc.getGameProfile().name())) {
                result =
                        result.replace(
                                InstanceAccess.mc.getGameProfile().name(),
                                nameHider.getReplacementName().getValue());
            }

            return result;
        } else {
            return text;
        }
    }
}
