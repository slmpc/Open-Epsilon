package com.github.epsilon.mixins.accessors;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface ILocalPlayerAccessor {
    @Accessor("positionReminder")
    int getPositionUpdateTick();
}
