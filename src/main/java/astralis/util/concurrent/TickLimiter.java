package astralis.util.concurrent;

import astralis.Astralis;
import astralis.event.EventStage;
import astralis.event.EventTarget;
import astralis.event.impl.TickEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class TickLimiter {
    private static final AtomicBoolean firstClaimed = new AtomicBoolean(false);
    private static final AtomicBoolean secondClaimed = new AtomicBoolean(false);

    public static void register() {
        Astralis.getInstance().getEventBus().register(new TickLimiter());
    }

    public static boolean tryClaimFirst() {
        return firstClaimed.compareAndSet(false, true);
    }

    public static boolean tryClaimSecond() {
        return secondClaimed.compareAndSet(false, true);
    }

    @EventTarget
    public void onTick(TickEvent tickEvent) {
        if (tickEvent.getStage() == EventStage.PRE) {
            resetClaims();
        }
    }

    public static void resetClaims() {
        firstClaimed.set(false);
        secondClaimed.set(false);
    }
}
