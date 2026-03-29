package com.github.epsilon.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;

public class CollisionEvent extends Event {

    private BlockState blockState;
    private final BlockPos blockPos;

    public CollisionEvent(BlockState blockState, BlockPos blockPos) {
        this.blockState = blockState;
        this.blockPos = blockPos;
    }

    public BlockState getState() {
        return blockState;
    }

    public BlockPos getPos() {
        return blockPos;
    }

    public void setState(BlockState blockState) {
        this.blockState = blockState;
    }

}
