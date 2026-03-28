package com.github.epsilon.events;

import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class TravelEvent extends Event implements ICancellableEvent {

    private final Vec3 input;

    public TravelEvent(Vec3 input) {
        this.input = input;
    }

    public Vec3 getInput() {
        return this.input;
    }

}
