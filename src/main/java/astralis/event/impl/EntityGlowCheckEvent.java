package astralis.event.impl;

import astralis.event.CancellableEvent;

import net.minecraft.world.entity.Entity;

/**
 * Replaces the glowing check in {@code Minecraft.shouldEntityAppearGlowing}; the entity glows only
 * when this event is cancelled.
 */
public class EntityGlowCheckEvent extends CancellableEvent {
    private final Entity entity;

    public EntityGlowCheckEvent(Entity entity2) {
        this.entity = entity2;
    }

    public Entity getEntity() {
        return this.entity;
    }
}
