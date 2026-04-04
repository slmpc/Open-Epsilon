package com.github.epsilon.mixins.level;

import com.github.epsilon.events.AttackBlockEvent;
import com.github.epsilon.events.DestroyBlockEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.BreakCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Shadow
    private int destroyDelay;

    @Final
    @Shadow
    private Minecraft minecraft;

    @Redirect(method = "lambda$useItem$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"))
    private float redirectUseItemYaw(Player player) {
        if (player == this.minecraft.player) {
            return RotationManager.INSTANCE.getYaw();
        }
        return player.getYRot();
    }

    @Redirect(method = "lambda$useItem$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"))
    private float redirectUseItemPitch(Player player) {
        if (player == this.minecraft.player) {
            return RotationManager.INSTANCE.getPitch();
        }
        return player.getXRot();
    }

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

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChangeOne(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChangeTwo(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

}
