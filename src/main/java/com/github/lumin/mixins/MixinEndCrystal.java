package com.github.lumin.mixins;

import com.github.lumin.ducks.EndCrystalAccess;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystal.class)
public class MixinEndCrystal implements EndCrystalAccess {

    @Unique
    private long lumin$spawnTime;

    @Unique
    private boolean lumin$mioAttacked;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lumin$initSpawnTime(CallbackInfo ci) {
        if (lumin$spawnTime == 0L) {
            lumin$spawnTime = System.currentTimeMillis();
        }
    }

    @Override
    public long lumin$getSpawnTime() {
        return lumin$spawnTime;
    }

    @Override
    public void lumin$setSpawnTime(long timeMs) {
        this.lumin$spawnTime = timeMs;
    }

    @Override
    public boolean lumin$isMioAttacked() {
        return lumin$mioAttacked;
    }

    @Override
    public void lumin$setMioAttacked(boolean attacked) {
        this.lumin$mioAttacked = attacked;
    }
}
