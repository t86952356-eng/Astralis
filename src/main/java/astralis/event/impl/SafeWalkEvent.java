package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Posted when the local player checks {@code isStayingOnGroundSurface}; cancelling keeps the
 * player on block edges.
 */
public class SafeWalkEvent extends CancellableEvent {}
