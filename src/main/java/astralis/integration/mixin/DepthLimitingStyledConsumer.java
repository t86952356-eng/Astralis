package astralis.integration.mixin;

import astralis.module.impl.AntiExploit;

import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class DepthLimitingStyledConsumer<T> implements StyledContentConsumer<T> {
    private final StyledContentConsumer<T> delegate;
    private int depth;

    public DepthLimitingStyledConsumer(StyledContentConsumer<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<T> accept(Style style, String text) {

        int currentDepth = this.depth++;

        if (currentDepth > 32) {

            AntiExploit.reportBlockedExploit("recursive component expansion");

            throw new IllegalArgumentException("Translatable component expansion is too deep");
        } else {
            return this.delegate.accept(style, text);
        }
    }
}
