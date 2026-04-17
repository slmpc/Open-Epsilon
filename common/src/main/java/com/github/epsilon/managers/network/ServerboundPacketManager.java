package com.github.epsilon.managers.network;

import com.github.epsilon.events.bus.EpsilonEventBus;
import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.world.WorldEvent;
import com.github.epsilon.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

import java.util.concurrent.LinkedBlockingQueue;

import static com.github.epsilon.Epsilon.mc;

public class ServerboundPacketManager {

    public static final ServerboundPacketManager INSTANCE = new ServerboundPacketManager();

    public static LinkedBlockingQueue<Packet> packets = new LinkedBlockingQueue<>();

    private ServerboundPacketManager() {
        EpsilonEventBus.INSTANCE.subscribe(this);
    }

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

    @EventHandler
    private void onWorldChange(WorldEvent event) {
        forceFlush = true;
        blinking = false;
    }

    public static void stopBlinking() { blinking = false; }
    public static void startBlinking() { blinking = true; }

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
