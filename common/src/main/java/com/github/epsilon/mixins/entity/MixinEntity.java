package com.github.epsilon.mixins.entity;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.player.RaytraceEvent;
import com.github.epsilon.events.movement.StrafeEvent;
import com.github.epsilon.modules.impl.combat.AimAssist;
import com.github.epsilon.modules.impl.combat.Velocity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public class MixinEntity {

    @WrapOperation(method = "getViewVector", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectGetViewYRot(Entity instance, float xRot, float yRot, Operation<Vec3> original) {
        if (instance == Minecraft.getInstance().player) {
            RaytraceEvent event = EpsilonEventBus.INSTANCE.post(new RaytraceEvent(instance, yRot, xRot));
            return original.call(instance, event.getPitch(), event.getYaw());
        }
        return original.call(instance, xRot, yRot);
    }

    @WrapOperation(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
    private float redirectGetYRotInMoveRelative(Entity instance, Operation<Float> original) {
        if (instance == Minecraft.getInstance().player) {
            StrafeEvent event = EpsilonEventBus.INSTANCE.post(new StrafeEvent(instance.getYRot()));
            return event.getYaw();
        }
        return original.call(instance);
    }

    @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void pushAwayFromHook(Args args) {
        if ((Entity) (Object) this == Minecraft.getInstance().player) {
            if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.entityPush.getValue()) {
                args.set(0, 0.0);
                args.set(1, 0.0);
                args.set(2, 0.0);
            }
        }
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double turnHook0(double value) {
        if ((Entity) (Object) this == Minecraft.getInstance().player && shouldBlockMouse()) {
            return 0d;
        }
        return value;
    }

    @ModifyVariable(method = "turn", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double turnHook1(double value) {
        if ((Entity) (Object) this == Minecraft.getInstance().player && shouldBlockMouse()) {
            return 0d;
        }
        return value;
    }

    private boolean shouldBlockMouse() {
        return AimAssist.INSTANCE.shouldBlockMouse();
    }

}
