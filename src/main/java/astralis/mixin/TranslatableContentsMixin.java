package astralis.mixin;

import astralis.integration.mixin.DepthLimitingContentConsumer;
import astralis.integration.mixin.DepthLimitingStyledConsumer;
import astralis.module.impl.AntiExploit;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.network.chat.FormattedText.ContentConsumer;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin {

    @WrapMethod(
            method =
                    "visit(Lnet/minecraft/network/chat/FormattedText$ContentConsumer;)Ljava/util/Optional;")
    private Optional wrapVisitWithGuard(ContentConsumer contentConsumer, Operation original) {
        if (!AntiExploit.isComponentExpansionProtectionEnabled()) {
            return (Optional) original.call(contentConsumer);
        }

        try {
            return (Optional) original.call(new DepthLimitingContentConsumer(contentConsumer));
        } catch (IllegalArgumentException e) {
            return contentConsumer.accept("...");
        }
    }

    @WrapMethod(
            method =
                    "visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;")
    private Optional wrapStyledVisitWithGuard(
            StyledContentConsumer styledContentConsumer, Style style, Operation original) {
        if (!AntiExploit.isComponentExpansionProtectionEnabled()) {
            return (Optional) original.call(styledContentConsumer, style);
        }

        try {
            return (Optional)
                    original.call(new DepthLimitingStyledConsumer(styledContentConsumer), style);
        } catch (IllegalArgumentException e) {
            return styledContentConsumer.accept(style, "...");
        }
    }
}
