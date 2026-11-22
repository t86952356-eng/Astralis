package astralis.event.impl;

import astralis.event.CancellableEvent;

/**
 * Posted when {@code canStartSprinting} checks item-use slowdown; cancelling allows sprinting
 * while using an item.
 */
public class ItemUseSprintEvent extends CancellableEvent {}
