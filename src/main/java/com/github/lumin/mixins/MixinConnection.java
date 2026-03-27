package com.github.lumin.mixins;

import com.github.lumin.events.PacketEvent;
import com.github.lumin.utils.network.PacketUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

@Mixin(Connection.class)
public class MixinConnection {

    @Shadow
    public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                if (NeoForge.EVENT_BUS.post(new PacketEvent.Receive(it.next())).isCanceled()) {
                    it.remove();
                }
            }
        } else if (NeoForge.EVENT_BUS.post(new PacketEvent.Receive(packet)).isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener sendListener, CallbackInfo ci) {
        if (PacketUtils.bypassPackets.contains(packet)) {
            PacketUtils.bypassPackets.remove(packet);
            send(packet, sendListener);
        } else {
            if (NeoForge.EVENT_BUS.post(new PacketEvent.Send(packet)).isCanceled()) {
                ci.cancel();
            }
        }
    }

}
