package astralis.event.impl;

import astralis.event.CancellableEvent;

/** Posted at the start of {@code LocalPlayer.tick}; cancelling skips the player tick. */
public class PlayerUpdateEvent extends CancellableEvent {}
