package com.github.epsilon.managers;

import com.github.epsilon.Epsilon;
import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.events.TotemPopEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;

@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class SyncManager {

    public static int serverSlot = -1;

    private static final HashMap<String, Integer> popList = new HashMap<>();

    @SubscribeEvent
    private static void onSyncUpdateSelectedSlot(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundSetCarriedItemPacket packet) {
            serverSlot = packet.getSlot();
        }
    }

    @SubscribeEvent
    private static void onSyncUpdateSelectedSlot(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet && packet.getContainerId() == -2) {
            serverSlot = packet.getSlot();
        }
    }

    @SubscribeEvent
    private static void onPacketReceive(PacketEvent.Receive event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
            if (packet.getEntity(mc.level) instanceof Player player) {
                int pops = popList.merge(player.getName().getString(), 1, Integer::sum);
                NeoForge.EVENT_BUS.post(new TotemPopEvent(player, pops));
            }
        }
    }

}
