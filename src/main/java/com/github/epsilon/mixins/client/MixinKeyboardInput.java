package com.github.epsilon.mixins.client;

import com.github.epsilon.events.KeyboardInputEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Shadow
    private static float calculateImpulse(boolean positive, boolean negative) {
        return 0;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input redirectKeyPresses(Input original) {
        float left = calculateImpulse(original.left(), original.right());
        float forward = calculateImpulse(original.forward(), original.backward());
        KeyboardInputEvent event = NeoForge.EVENT_BUS.post(new KeyboardInputEvent(left, forward));
        return new Input(
                event.getForward() > 0,
                event.getForward() < 0,
                event.getLeft() > 0,
                event.getLeft() < 0,
                original.jump(),
                original.shift(),
                original.sprint()
        );
    }

}