package com.github.epsilon.mixins.level;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.player.PlayerInteractEvent;
import com.github.epsilon.events.world.AttackBlockEvent;
import com.github.epsilon.events.world.DestroyBlockEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.BreakCooldown;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Final
    @Shadow
    private Minecraft minecraft;

    @WrapOperation(method = "lambda$useItem$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float redirectUseItemYaw(Player player, Operation<Float> original) {
        if (player == this.minecraft.player) {
            EpsilonEventBus.INSTANCE.post(new PlayerInteractEvent.RightClickItem());
            return RotationManager.INSTANCE.getYaw();
        }
        return original.call(player);
    }

    @WrapOperation(method = "lambda$useItem$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private float redirectUseItemPitch(Player player, Operation<Float> original) {
        if (player == this.minecraft.player) {
            return RotationManager.INSTANCE.getPitch();
        }
        return original.call(player);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), cancellable = true)
    public void hookDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        DestroyBlockEvent event = EpsilonEventBus.INSTANCE.postCancellable(new DestroyBlockEvent(pos));
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        AttackBlockEvent event = EpsilonEventBus.INSTANCE.postCancellable(new AttackBlockEvent(blockPos, direction));
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(MultiPlayerGameMode instance, int value, Operation<Void> original) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        int newValue = breakCooldown.isEnabled() ? breakCooldown.cooldown.getValue() : value;
        original.call(instance, newValue);
    }

    @WrapOperation(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChangeOne(MultiPlayerGameMode instance, int value, Operation<Void> original) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        int newValue = breakCooldown.isEnabled() ? breakCooldown.cooldown.getValue() : value;
        original.call(instance, newValue);
    }

    @WrapOperation(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChangeTwo(MultiPlayerGameMode instance, int value, Operation<Void> original) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        int newValue = breakCooldown.isEnabled() ? breakCooldown.cooldown.getValue() : value;
        original.call(instance, newValue);
    }

}
