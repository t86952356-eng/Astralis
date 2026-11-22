package com.astralis.scripting.api.event;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public final class ScriptPacketHandle {
    private static final int MAX_HELD_PACKETS = 512;
    private static final AtomicInteger HELD_PACKET_COUNT = new AtomicInteger();
    private final BooleanSupplier validSupplier;
    private final AtomicReference<Runnable> releaseAction;
    private final AtomicBoolean held = new AtomicBoolean();

    public ScriptPacketHandle(BooleanSupplier validSupplier, Runnable releaseAction) {
        this.validSupplier = Objects.requireNonNull(validSupplier);
        this.releaseAction = new AtomicReference<>(Objects.requireNonNull(releaseAction));
    }

    boolean tryHold() {
        if (!this.validSupplier.getAsBoolean()) {
            this.releaseAction.set(null);
            return false;
        }

        if (!this.held.compareAndSet(false, true)) {
            return this.isPending();
        }

        int currentCount;
        do {
            currentCount = HELD_PACKET_COUNT.get();
            if (currentCount >= 512) {
                this.releaseAction.set(null);
                return false;
            }
        } while (!HELD_PACKET_COUNT.compareAndSet(currentCount, currentCount + 1));

        CompletableFuture.delayedExecutor(30L, TimeUnit.SECONDS).execute(this::discard);
        return true;
    }

    public boolean release() {
        if (this.held.get() && this.validSupplier.getAsBoolean()) {
            Runnable action = this.releaseAction.getAndSet(null);
            if (action == null) {
                return false;
            }

            HELD_PACKET_COUNT.decrementAndGet();
            action.run();
            return true;
        } else {
            this.discard();
            return false;
        }
    }

    public boolean discard() {
        if (this.releaseAction.getAndSet(null) == null) {
            return false;
        }

        if (this.held.get()) {
            HELD_PACKET_COUNT.decrementAndGet();
        }

        return true;
    }

    public boolean isPending() {
        return this.held.get()
                && this.releaseAction.get() != null
                && this.validSupplier.getAsBoolean();
    }
}
