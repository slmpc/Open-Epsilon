package com.github.epsilon.mixins.packet;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.managers.network.ClientboundPacketManager;
import com.github.epsilon.managers.network.ServerboundPacketManager;
import com.github.epsilon.utils.network.PacketUtils;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Connection.class)
public class MixinConnection {

    @Shadow
    private void sendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
    }

    @Shadow
    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
    }

    @Redirect(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void onReceivePacket(Packet<?> packet, PacketListener listener) {
        if (ClientboundPacketManager.onPacketReceive(packet)) {
            return;
        }
        PacketEvent.Receive event = NeoForge.EVENT_BUS.post(new PacketEvent.Receive(packet));
        if (!event.isCanceled()) {
            genericsFtw(event.getPacket(), listener);
        }
    }

    @Redirect(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"))
    private void onSendPacket(Connection instance, Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        if (ServerboundPacketManager.onPacket(packet)) {
            return;
        }
        if (PacketUtils.bypassPackets.contains(packet)) {
            PacketUtils.bypassPackets.remove(packet);
            sendPacket(packet, listener, flush);
        } else {
            PacketEvent.Send event = new PacketEvent.Send(packet);
            if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
                this.sendPacket(event.getPacket(), listener, flush);
            }
        }
    }

}
