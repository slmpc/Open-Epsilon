package com.github.epsilon.mixins.level;


import com.github.epsilon.Epsilon;
import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.world.EntityJoinWorldEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {

    @Redirect(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void hookTickNonPassenger(Entity instance) {
        if (Epsilon.skipTicks > 0 && instance == Minecraft.getInstance().player) {
            Epsilon.skipTicks--;
        } else {
            instance.tick();
        }
    }

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        EpsilonEventBus.INSTANCE.post(new EntityJoinWorldEvent(entity));
    }
}
