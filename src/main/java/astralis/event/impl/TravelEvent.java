package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Posted before the local player's {@code LivingEntity.travel} call in {@code aiStep}; cancelling
 * skips travel for that tick.
 */
public class TravelEvent extends CancellableEvent {}
