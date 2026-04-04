package com.github.epsilon.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class TotemPopEvent extends Event {

    private final Player player;
    private final int pops;

    public TotemPopEvent(Player player, int pops) {
        this.player = player;
        this.pops = pops;
    }

    public Player getPlayer() {
        return this.player;
    }

    public int getPops() {
        return this.pops;
    }

}
