package com.github.epsilon.modules.impl.player;

import com.github.epsilon.events.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/* 
* Author Moli
*/

public class PacketEat extends Module {

    public static final PacketEat INSTANCE = new PacketEat();

    private final BoolSetting desync = boolSetting("Desync", false);
    private final BoolSetting noRelease = boolSetting("NoRelease", true);

    private PacketEat() {
        super("PacketEat", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || !desync.getValue()) return;
        if (!mc.player.isUsingItem()) return;
        if (!isFood(mc.player.getUseItem())) return;
        
        var packet = new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
            mc.player.getUsedItemHand(),
            0,
            mc.player.getYRot(),
            mc.player.getXRot()
        );
        mc.getConnection().send(packet);
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Send event) {
        if (nullCheck() || !noRelease.getValue()) return;
        if (!(event.getPacket() instanceof net.minecraft.network.protocol.game.ServerboundPlayerActionPacket packet)) return;
        if (packet.getAction() != net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) return;
        if (!isFood(mc.player.getUseItem())) return;
        
        event.setCanceled(true);
    }

    private boolean isFood(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || 
               stack.is(Items.ENCHANTED_GOLDEN_APPLE) || 
               stack.is(Items.POTION) ||
               stack.is(Items.BAKED_POTATO) ||
               stack.is(Items.COOKED_BEEF) ||
               stack.is(Items.COOKED_PORKCHOP) ||
               stack.is(Items.COOKED_CHICKEN) ||
               stack.is(Items.COOKED_MUTTON) ||
               stack.is(Items.COOKED_RABBIT) ||
               stack.is(Items.COOKED_COD) ||
               stack.is(Items.COOKED_SALMON) ||
               stack.is(Items.BREAD) ||
               stack.is(Items.APPLE) ||
               stack.is(Items.CARROT) ||
               stack.is(Items.POTATO) ||
               stack.is(Items.BEETROOT) ||
               stack.is(Items.MELON_SLICE) ||
               stack.is(Items.COOKIE) ||
               stack.is(Items.PUMPKIN_PIE) ||
               stack.is(Items.CAKE);
    }
}
