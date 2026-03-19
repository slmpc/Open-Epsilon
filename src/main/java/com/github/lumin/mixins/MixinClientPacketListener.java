package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyArg(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;finalizeExplosion(Z)V"), index = 0, require = 0)
    private boolean lumin$noExplosionParticles(boolean spawnParticles) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.explosions.getValue()) {
            return false;
        }
        return spawnParticles;
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"), require = 0)
    private void lumin$noExplosionSound(net.minecraft.client.multiplayer.ClientLevel level, double x, double y, double z, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch, boolean distanceDelay) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.explosions.getValue()) {
            return;
        }
        level.playLocalSound(x, y, z, sound, source, volume, pitch, distanceDelay);
    }
}
