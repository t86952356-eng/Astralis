package astralis.event.impl;

import astralis.event.CancellableEvent;

/** Posted when {@code Minecraft.startAttack} begins; cancelling prevents the attack. */
public class StartAttackEvent extends CancellableEvent {}
