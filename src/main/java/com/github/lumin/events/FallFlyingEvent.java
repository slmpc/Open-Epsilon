package com.github.lumin.events;

import net.neoforged.bus.api.Event;

public class FallFlyingEvent extends Event {

    private float pitch;

    public FallFlyingEvent(float pitch) {
        this.pitch = pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {
        return this.pitch;
    }

}
