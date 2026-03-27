package com.github.lumin.mixins;

import com.github.lumin.events.RayTraceEvent;
import com.github.lumin.events.StrafeEvent;
import com.github.lumin.modules.impl.player.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public abstract Vec3 calculateViewVector(float xRot, float yRot);

    @Redirect(method = "getViewVector", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectGetViewYRot(Entity instance, float xRot, float yRot) {
        if (instance == Minecraft.getInstance().player) {
            RayTraceEvent event = NeoForge.EVENT_BUS.post(new RayTraceEvent(instance, yRot, xRot));
            return this.calculateViewVector(event.getPitch(), event.getYaw());
        }
        return this.calculateViewVector(xRot, yRot);
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float redirectGetYRotInMoveRelative(Entity instance) {
        if (instance == Minecraft.getInstance().player) {
            StrafeEvent event = NeoForge.EVENT_BUS.post(new StrafeEvent(instance.getYRot()));
            return event.getYaw();
        }
        return instance.getYRot();
    }

    @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void pushAwayFromHook(Args args) {
        if (Entity.class.cast(this) == Minecraft.getInstance().player) {
            if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.entityPush.getValue()) {
                args.set(0, 0d);
                args.set(1, 0d);
                args.set(2, 0d);
            }
        }
    }

}
