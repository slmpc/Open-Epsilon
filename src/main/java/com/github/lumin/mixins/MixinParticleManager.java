package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MixinParticleManager {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCreateParticle(ParticleOptions particleOptions, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (!shouldCancel(particleOptions)) {
            return;
        }
        cir.setReturnValue(null);
        cir.cancel();
    }

    @Inject(method = "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCreateTrackingEmitter(Entity entity, ParticleOptions particleOptions, int lifetime, CallbackInfo ci) {
        if (!shouldCancel(particleOptions)) {
            return;
        }
        ci.cancel();
    }

    private boolean shouldCancel(ParticleOptions particleOptions) {
        if (!NoRender.INSTANCE.isEnabled() || !NoRender.INSTANCE.explosions.getValue() || particleOptions == null) {
            return false;
        }
        return particleOptions.getType() == ParticleTypes.EXPLOSION
                || particleOptions.getType() == ParticleTypes.EXPLOSION_EMITTER
                || particleOptions.getType() == ParticleTypes.POOF
                || particleOptions.getType() == ParticleTypes.SMOKE
                || particleOptions.getType() == ParticleTypes.LARGE_SMOKE
                || particleOptions.getType() == ParticleTypes.CLOUD
                || NoRender.INSTANCE.fireworks.getValue() && (particleOptions.getType() == ParticleTypes.FIREWORK || particleOptions.getType() == ParticleTypes.FLASH)
                || NoRender.INSTANCE.portal.getValue() && (particleOptions.getType() == ParticleTypes.PORTAL || particleOptions.getType() == ParticleTypes.REVERSE_PORTAL)
                || NoRender.INSTANCE.totems.getValue() && particleOptions.getType() == ParticleTypes.TOTEM_OF_UNDYING;
    }

}
