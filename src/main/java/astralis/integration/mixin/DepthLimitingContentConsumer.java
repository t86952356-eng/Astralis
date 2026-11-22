package astralis.integration.mixin;

import astralis.module.impl.AntiExploit;

import net.minecraft.network.chat.FormattedText.ContentConsumer;

import java.util.Optional;

public final class DepthLimitingContentConsumer<T> implements ContentConsumer<T> {
    private final ContentConsumer<T> delegate;
    private int depth;

    public DepthLimitingContentConsumer(ContentConsumer<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<T> accept(String text) {

        int currentDepth = this.depth++;

        if (currentDepth > 32) {

            AntiExploit.reportBlockedExploit("recursive component expansion");

            throw new IllegalArgumentException("Translatable component expansion is too deep");
        } else {
            return this.delegate.accept(text);
        }
    }
}
