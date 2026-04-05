package com.github.epsilon.modules.impl.player;

import com.github.epsilon.managers.SyncManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.player.InvUtils;
import com.github.epsilon.utils.timer.TimerUtils;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/*
 * Author Moli
 */

public class PacketXP extends Module {

    public static final PacketXP INSTANCE = new PacketXP();

    private final IntSetting packets = intSetting("Packets", 16, 1, 100, 1);
    private final BoolSetting silentSwitch = boolSetting("SilentSwitch", true);
    private final IntSetting delay = intSetting("Delay", 0, 0, 500, 50);

    private final TimerUtils timer = new TimerUtils();

    private PacketXP() {
        super("PacketXP", Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        timer.reset();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck()) return;
        if (!timer.passedMillise(delay.getValue())) return;

        InteractionHand hand = mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;

        int prevServerSlot = SyncManager.serverSlot;
        boolean switched = false;

        if (hand == InteractionHand.MAIN_HAND && !mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)) {
            var result = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            if (!result.found()) return;

            if (silentSwitch.getValue()) {
                InvUtils.swap(result.slot(), true);
                switched = true;
            } else {
                InvUtils.swap(result.slot(), false);
            }
        }

        for (int i = 0; i < packets.getValue(); i++) {
            mc.getConnection().send(new ServerboundUseItemPacket(hand, 0, mc.player.getYRot(), mc.player.getXRot()));
        }

        mc.player.swing(hand);

        if (switched && prevServerSlot != SyncManager.serverSlot) {
            InvUtils.swapBack();
        }

        timer.reset();
    }
}
