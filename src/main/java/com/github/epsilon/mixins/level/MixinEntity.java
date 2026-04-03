package com.github.epsilon.mixins.level;

import com.github.epsilon.events.RayTraceEvent;
import com.github.epsilon.events.StrafeEvent;
import com.github.epsilon.modules.impl.combat.AimAssist;
import com.github.epsilon.modules.impl.combat.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
        if ((Entity) (Object) this == Minecraft.getInstance().player) {
            if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.entityPush.getValue()) {
                args.set(0, 0d);
                args.set(1, 0d);
                args.set(2, 0d);
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
