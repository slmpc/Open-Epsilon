package com.github.epsilon.managers.network;

import com.github.epsilon.Epsilon;
import com.github.epsilon.events.WorldEvent;
import com.github.epsilon.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.concurrent.LinkedBlockingQueue;

import static com.github.epsilon.Epsilon.mc;

@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class ClientboundPacketManager {
    public static LinkedBlockingQueue<Packet> packets = new LinkedBlockingQueue<>();

    public static void flush() {
        if (mc.getConnection() != null)
            while (!packets.isEmpty()) {
                try {
                    packets.poll().handle(mc.getConnection().getConnection().getPacketListener());
                } catch (Exception e) {
                    ChatUtils.addChatMessage("failed to flush clientbound packets: " + e.getMessage());
                }
            }
    }

    public static boolean isDisallowedPacket(Packet packet) {
        return !(packet instanceof ClientboundSystemChatPacket)
                && !(packet instanceof ClientboundPlayerChatPacket)
                && !(packet instanceof ClientboundSetDisplayObjectivePacket)
                && !(packet instanceof ClientboundSetEquipmentPacket)
                && !(packet instanceof ClientboundClearTitlesPacket)
                && !(packet instanceof ClientboundSetTitleTextPacket)
                && !(packet instanceof ClientboundSetSubtitleTextPacket)
                && !(packet instanceof ClientboundSetActionBarTextPacket)
                && !(packet instanceof ClientboundBossEventPacket)
                && !(packet instanceof ClientboundAddEntityPacket)
                && !(packet instanceof ClientboundRemoveEntitiesPacket)
                && !(packet instanceof ClientboundDamageEventPacket)
                && !(packet instanceof ClientboundSoundPacket)
                && !(packet instanceof ClientboundSoundEntityPacket)
                && !(packet instanceof ClientboundSetEntityDataPacket)
                && !(packet instanceof ClientboundSetHealthPacket)
                && !(packet instanceof ClientboundContainerSetContentPacket)
                && !(packet instanceof ClientboundSetObjectivePacket)
                && !(packet instanceof ClientboundResetScorePacket)
                && !(packet instanceof ClientboundSetScorePacket);
    }

    @SubscribeEvent
    private static void onWorldChange(WorldEvent event) {
        shouldFlush = true;
        tracking = false;
    }

    public static void startTracking() {
        tracking = true;
    }

    public static boolean tracking = false;
    public static boolean shouldFlush = false;

    public static boolean onPacketReceive(Packet<?> packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        if (shouldFlush) {
            flush();
            shouldFlush = false;
            return false;
        }
        if (!isDisallowedPacket(packet) && tracking) {
            packets.add(packet);
            return true;
        }
        return false;
    }

}
