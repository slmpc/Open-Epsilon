package com.github.epsilon.managers.network;

import com.github.epsilon.Epsilon;
import com.github.epsilon.events.WorldEvent;
import com.github.epsilon.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.concurrent.LinkedBlockingQueue;

import static com.github.epsilon.Epsilon.mc;

//BlinkManager
@EventBusSubscriber(modid = Epsilon.MODID, value = Dist.CLIENT)
public class ServerboundPacketManager {

    public static LinkedBlockingQueue<Packet> packets = new LinkedBlockingQueue<>();

    public static void flush() {
        if (mc.getConnection() != null)
            while (!packets.isEmpty()) {
                try {
                    mc.getConnection().send(packets.poll());
                } catch (Exception e) {
                    ChatUtils.addChatMessage("failed to flush serverbound packets: " + e.getMessage());
                }
            }
    }

    public static boolean blinking = false;
    static boolean forceFlush;

    @SubscribeEvent
    private static void onWorldChange(WorldEvent event) {
        forceFlush = true;
        blinking = false;
    }

    public static void stopBlinking() {
        blinking = false;
    }

    public static void startBlinking() {
        blinking = true;
    }

    public static boolean onPacket(Packet packet) {
        Minecraft mc = Minecraft.getInstance();
        if (forceFlush) {
            flush();
            forceFlush = false;
            return false;
        }
        if (!blinking) return false;
        if (mc.player == null || mc.level == null) return false;
        packets.add(packet);
        return true;
    }

}
