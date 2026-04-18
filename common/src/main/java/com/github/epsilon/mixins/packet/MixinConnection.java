package com.github.epsilon.mixins.packet;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.network.PacketEvent;
import com.github.epsilon.managers.network.ClientboundPacketManager;
import com.github.epsilon.managers.network.ServerboundPacketManager;
import com.github.epsilon.utils.network.PacketUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Connection.class)
public class MixinConnection {

    @WrapOperation(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
    private void onReceivePacket(Packet<?> packet, PacketListener listener, Operation<Void> original) {
        if (ClientboundPacketManager.onPacketReceive(packet)) {
            return;
        }
        PacketEvent.Receive event = EpsilonEventBus.INSTANCE.postCancellable(new PacketEvent.Receive(packet));
        if (!event.isCancelled()) {
            original.call(event.getPacket(), listener);
        }
    }

    @WrapOperation(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"))
    private void onSendPacket(Connection instance, Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if (ServerboundPacketManager.onPacket(packet)) {
            return;
        }
        if (PacketUtils.bypassPackets.contains(packet)) {
            PacketUtils.bypassPackets.remove(packet);
            original.call(instance, packet, listener, flush);
        } else {
            PacketEvent.Send event = new PacketEvent.Send(packet);
            if (!EpsilonEventBus.INSTANCE.postCancellable(event).isCancelled()) {
                original.call(instance, event.getPacket(), listener, flush);
            }
        }
    }
}
