package astralis.event.impl;

import astralis.event.CancellableEvent;

import net.minecraft.client.resources.sounds.SoundInstance;

/** Posted when the sound engine plays a sound; cancelling suppresses it. */
public class PlaySoundEvent extends CancellableEvent {
    private final SoundInstance sound;

    public PlaySoundEvent(SoundInstance soundInstance) {
        this.sound = soundInstance;
    }

    public SoundInstance getSound() {
        return this.sound;
    }
}
