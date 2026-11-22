package astralis.mixin;

import astralis.module.impl.AntiExploit;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.commands.Grammar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(Grammar.class)
public abstract class GrammarMixin {
    @WrapOperation(
            method = "parse",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/util/parsing/packrat/ParseState;parseTopRule(Lnet/minecraft/util/parsing/packrat/NamedRule;)Ljava/util/Optional;"))
    private Optional guardDeepNbtParse(
            ParseState parseState, NamedRule namedRule, Operation original)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!AntiExploit.isNbtDepthProtectionEnabled()) {
            return (Optional) original.call(parseState, namedRule);
        }

        try {
            return (Optional) original.call(parseState, namedRule);
        } catch (StackOverflowError | IllegalStateException ignored) {
            AntiExploit.reportBlockedExploit("deep NBT/parser data");
            throw new SimpleCommandExceptionType(Component.literal("NBT data is too deep"))
                    .create();
        }
    }
}
