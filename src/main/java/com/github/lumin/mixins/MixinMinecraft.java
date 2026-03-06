package com.github.lumin.mixins;

import com.github.lumin.managers.Managers;
import com.github.lumin.modules.impl.visual.Glow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(
            method = {"shouldEntityAppearGlowing"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
        if (Glow.INSTANCE.isEnabled() && pEntity instanceof Player) {
            cir.setReturnValue(true);
        }
    }

}
