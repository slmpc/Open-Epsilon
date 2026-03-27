package com.github.lumin.mixins;

import com.github.lumin.modules.impl.player.ElytraFly;
import com.github.lumin.modules.impl.player.NoRotate;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener extends ClientCommonPacketListenerImpl {

    protected MixinClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("noRotateYaw") LocalFloatRef yawRef, @Share("noRotatePitch") LocalFloatRef pitchRef) {
        if (!NoRotate.INSTANCE.isEnabled() || minecraft.player == null) return;
        yawRef.set(minecraft.player.getYRot());
        pitchRef.set(minecraft.player.getXRot());
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void onHandleMovePlayerReturn(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Share("noRotateYaw") LocalFloatRef yawRef, @Share("noRotatePitch") LocalFloatRef pitchRef) {
        if (!NoRotate.INSTANCE.isEnabled() || minecraft.player == null) return;

        float savedYaw = yawRef.get();
        float savedPitch = pitchRef.get();

        // 玩家不会注意到，但会强制服务器更新。
        minecraft.player.setYRot(savedYaw + 0.000001f);
        minecraft.player.setXRot(savedPitch + 0.000001f);
        minecraft.player.yHeadRot = savedYaw;
        minecraft.player.yBodyRot = savedYaw;
    }

    @Inject(method = "handleSoundEvent", at = @At("HEAD"), cancellable = true)
    private void onHandleSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.level == null) return;
        String soundId = Objects.requireNonNull(BuiltInRegistries.SOUND_EVENT.getKey(packet.getSound().value())).toString();
        if (ElytraFly.INSTANCE.shouldVisualSpoof() && soundId.startsWith("minecraft:item.armor.equip_")) {
            ci.cancel();
        }
    }

}
