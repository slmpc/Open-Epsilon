package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class PacketEat extends Module {

    public static final PacketEat INSTANCE = new PacketEat();

    private PacketEat() {
        super("Packet Eat", Category.PLAYER);
    }

    private ItemStack item;

    @SubscribeEvent
    private void onClientTickPost(ClientTickEvent.Post event) {
        if (nullCheck()) return;
        if (mc.player.isUsingItem()) {
            item = mc.player.getUseItem();
        }
    }

    @SubscribeEvent
    private void onPacket(PacketEvent.Send event) {
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet && packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            if (item.get(DataComponents.FOOD).canAlwaysEat()) {
                event.setCanceled(true);
            }
        }
    }

}
