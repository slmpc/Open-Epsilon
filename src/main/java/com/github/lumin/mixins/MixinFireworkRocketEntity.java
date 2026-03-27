package com.github.lumin.mixins;

import com.github.lumin.modules.impl.player.ElytraFly;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public class MixinFireworkRocketEntity {

    @Shadow
    private int life;

    @Shadow
    private int lifetime;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        FireworkRocketEntity firework = (FireworkRocketEntity) (Object) this;
        if (ElytraFly.INSTANCE.isFirework(firework) && this.life > this.lifetime) {
            firework.discard();
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHitEntity(EntityHitResult result, CallbackInfo ci) {
        FireworkRocketEntity firework = (FireworkRocketEntity) (Object) this;
        if (ElytraFly.INSTANCE.isFirework(firework)) {
            firework.discard();
            ci.cancel();
        }
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void onHitBlock(BlockHitResult result, CallbackInfo ci) {
        FireworkRocketEntity firework = (FireworkRocketEntity) (Object) this;
        if (ElytraFly.INSTANCE.isFirework(firework)) {
            firework.discard();
            ci.cancel();
        }
    }

}
