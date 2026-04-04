package com.github.epsilon.mixins.level;

import com.github.epsilon.events.AttackBlockEvent;
import com.github.epsilon.events.DestroyBlockEvent;
import com.github.epsilon.managers.RotationManager;
import com.github.epsilon.modules.impl.player.BreakCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import org.apache.commons.lang3.mutable.MutableObject;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    @Shadow
    private int destroyDelay;

    @Final
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private GameType localPlayerMode;

    @Shadow
    protected abstract void startPrediction(ClientLevel level, PredictiveAction predictiveAction);

    /**
     * @author L3MonKe178
     * @reason Skill issue
     */
    @Overwrite
    public InteractionResult useItem(Player player, InteractionHand hand) {
        if (this.localPlayerMode == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else {
            ((MultiPlayerGameMode) (Object) this).ensureHasSentCarriedItem();
            MutableObject<InteractionResult> interactionResult = new MutableObject<>();
            this.startPrediction(this.minecraft.level, sequence -> {
                ServerboundUseItemPacket packet = new ServerboundUseItemPacket(hand, sequence, player == minecraft.player ? RotationManager.INSTANCE.getYaw() : player.getYRot(), player == minecraft.player ? RotationManager.INSTANCE.getPitch() : player.getXRot());
                ItemStack itemStack = player.getItemInHand(hand);
                if (player.getCooldowns().isOnCooldown(itemStack)) {
                    interactionResult.setValue(InteractionResult.PASS);
                    return packet;
                } else {
                    InteractionResult cancelResult = CommonHooks.onItemRightClick(player, hand);
                    if (cancelResult != null) {
                        interactionResult.setValue(cancelResult);
                        return packet;
                    } else {
                        InteractionResult resultHolder = itemStack.use(this.minecraft.level, player, hand);
                        ItemStack result;
                        if (resultHolder instanceof InteractionResult.Success success) {
                            result = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> player.getItemInHand(hand));
                        } else {
                            result = player.getItemInHand(hand);
                        }

                        if (result != itemStack) {
                            player.setItemInHand(hand, result);
                            if (result.isEmpty()) {
                                EventHooks.onPlayerDestroyItem(player, itemStack, hand);
                            }
                        }

                        interactionResult.setValue(resultHolder);
                        return packet;
                    }
                }
            });
            return interactionResult.get();
        }
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
