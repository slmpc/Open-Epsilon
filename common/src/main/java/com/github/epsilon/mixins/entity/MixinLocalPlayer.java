package com.github.epsilon.mixins.entity;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.movement.MotionEvent;
import com.github.epsilon.events.movement.SlowdownEvent;
import com.github.epsilon.modules.impl.combat.Velocity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {

    @Unique
    private MotionEvent lumin$motionEvent;

    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void onPreSendPosition(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        lumin$motionEvent = EpsilonEventBus.INSTANCE.postCancellable(new MotionEvent(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()));
        if (lumin$motionEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectPosition(LocalPlayer instance, Operation<Vec3> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return new Vec3(lumin$motionEvent.getX(), lumin$motionEvent.getY(), lumin$motionEvent.getZ());
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double redirectGetX(LocalPlayer instance, Operation<Double> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.getX();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double redirectGetY(LocalPlayer instance, Operation<Double> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.getY();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double redirectGetZ(LocalPlayer instance, Operation<Double> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.getZ();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float redirectGetYRot(LocalPlayer instance, Operation<Float> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.getYaw();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float redirectGetXRot(LocalPlayer instance, Operation<Float> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.getPitch();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean redirectOnGround(LocalPlayer instance, Operation<Boolean> original) {
        if (lumin$motionEvent == null) {
            return original.call(instance);
        }
        return lumin$motionEvent.isOnGround();
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocksHook(double x, double d, CallbackInfo info) {
        if (Velocity.INSTANCE.isEnabled() && Velocity.INSTANCE.blockPush.getValue()) {
            info.cancel();
        }
    }

    @WrapOperation(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    public boolean onSlowdown(LocalPlayer localPlayer, Operation<Boolean> original) {
        SlowdownEvent event = EpsilonEventBus.INSTANCE.post(new SlowdownEvent(original.call(localPlayer)));
        return event.isSlowdown();
    }
}
