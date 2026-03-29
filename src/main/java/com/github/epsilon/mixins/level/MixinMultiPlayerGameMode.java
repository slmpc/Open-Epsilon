package com.github.epsilon.mixins.level;

import com.github.epsilon.events.AttackBlockEvent;
import com.github.epsilon.events.DestroyBlockEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.BreakCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Shadow
    private int destroyDelay;

    @Unique
    private float savedYaw;

    @Unique
    private float savedPitch;

    @Unique
    private boolean rotationModified;

    @Inject(method = "destroyBlock", at = @At("RETURN"), cancellable = true)
    public void hookDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        DestroyBlockEvent event = NeoForge.EVENT_BUS.post(new DestroyBlockEvent(pos));
        if (event.isCanceled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        AttackBlockEvent event = NeoForge.EVENT_BUS.post(new AttackBlockEvent(blockPos, direction));
        if (event.isCanceled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void preUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (RotationManager.INSTANCE.isActive() && RotationManager.INSTANCE.rotations != null) {
            savedYaw = player.getYRot();
            savedPitch = player.getXRot();
            player.setYRot(RotationManager.INSTANCE.getYaw());
            player.setXRot(RotationManager.INSTANCE.getPitch());
            rotationModified = true;
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void postUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (rotationModified) {
            player.setYRot(savedYaw);
            player.setXRot(savedPitch);
            rotationModified = false;
        }
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChange(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChange2(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

}
