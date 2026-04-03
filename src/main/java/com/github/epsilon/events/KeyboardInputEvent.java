package com.github.epsilon.events;

import net.neoforged.bus.api.Event;

public class KeyboardInputEvent extends Event {

    private float left;
    private float forward;

    public KeyboardInputEvent(float left, float forward) {
        this.forward = forward;
        this.left = left;
    }

    public float getLeft() {
        return this.left;
    }

    public float getForward() {
        return this.forward;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

}
