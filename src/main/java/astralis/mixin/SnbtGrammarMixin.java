package astralis.mixin;

import astralis.module.impl.AntiExploit;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Definitions;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.util.parsing.packrat.Term;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SnbtGrammar.class)
public abstract class SnbtGrammarMixin {
    private static final Term INCREASE_DEPTH =
            (parseState, scope, control) -> {
                if (!AntiExploit.isNbtDepthProtectionEnabled()) {
                    return true;
                } else if (((astralis.integration.mixin.ScopeDepthExtension) (Object) scope)
                                .astralis$increaseDepth()
                        > 512) {
                    parseState
                            .errorCollector()
                            .store(
                                    parseState.mark(),
                                    new IllegalStateException("NBT data is too deep"));
                    return false;
                } else {
                    return true;
                }
            };
    private static final Term DECREASE_DEPTH =
            (parseState, scope, control) -> {
                if (AntiExploit.isNbtDepthProtectionEnabled()) {
                    ((astralis.integration.mixin.ScopeDepthExtension) (Object) scope)
                            .astralis$decreaseDepth();
                }

                return true;
            };

    @WrapOperation(method = "createParser", at = @At("MIXINEXTRAS:EXPRESSION"))
    @Definitions({
        @Definition(
                id = "sequence",
                method =
                        "Lnet/minecraft/util/parsing/packrat/Term;sequence([Lnet/minecraft/util/parsing/packrat/Term;)Lnet/minecraft/util/parsing/packrat/Term;"),
        @Definition(
                id = "character",
                method =
                        "Lnet/minecraft/util/parsing/packrat/commands/StringReaderTerms;character(C)Lnet/minecraft/util/parsing/packrat/Term;"),
        @Definition(id = "Term", type = Term.class)
    })
    @Expression("sequence(new Term[]{character('['), ?, character(']')})")
    private static Term wrapListDepthTracking(Term[] terms, Operation original) {
        return (Term)
                original.call(
                        (Object)
                                new Term[] {
                                    terms[0],
                                    getIncreaseDepthTerm(),
                                    terms[1],
                                    getDecreaseDepthTerm(),
                                    terms[2]
                                });
    }

    @WrapOperation(method = "createParser", at = @At("MIXINEXTRAS:EXPRESSION"))
    @Definitions({
        @Definition(
                id = "sequence",
                method =
                        "Lnet/minecraft/util/parsing/packrat/Term;sequence([Lnet/minecraft/util/parsing/packrat/Term;)Lnet/minecraft/util/parsing/packrat/Term;"),
        @Definition(
                id = "character",
                method =
                        "Lnet/minecraft/util/parsing/packrat/commands/StringReaderTerms;character(C)Lnet/minecraft/util/parsing/packrat/Term;"),
        @Definition(id = "Term", type = Term.class)
    })
    @Expression("sequence(new Term[]{character('{'), ?, character('}')})")
    private static Term wrapCompoundDepthTracking(Term[] terms, Operation original) {
        return (Term)
                original.call(
                        (Object)
                                new Term[] {
                                    terms[0],
                                    getIncreaseDepthTerm(),
                                    terms[1],
                                    getDecreaseDepthTerm(),
                                    terms[2]
                                });
    }

    private static Term getIncreaseDepthTerm() {
        return INCREASE_DEPTH;
    }

    private static Term getDecreaseDepthTerm() {
        return DECREASE_DEPTH;
    }
}
