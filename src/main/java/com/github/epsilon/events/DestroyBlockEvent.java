package com.github.epsilon.events;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class DestroyBlockEvent extends Event implements ICancellableEvent {

    private final BlockPos pos;

    public DestroyBlockEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

}
