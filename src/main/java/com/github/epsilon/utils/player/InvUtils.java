package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class InvUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static int previousSlot = -1;
    public static int[] invSlots;

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandItem());
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffhandItem());
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (testInOffHand(isGood)) {
            return new FindItemResult(40, mc.player.getOffhandItem().getCount(), mc.player.getOffhandItem().getMaxStackSize());
        } else if (testInMainHand(isGood)) {
            return new FindItemResult(mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getCount(), mc.player.getMainHandItem().getMaxStackSize());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        return find(isGood, 0, mc.player.getInventory().getContainerSize());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        int slot = -1, count = 0, maxCount = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
                maxCount += stack.getMaxStackSize();
            }
        }

        return new FindItemResult(slot, count, maxCount);
    }

    public static boolean swap(int slot, boolean saveSwap) {
        if (mc.player.getInventory().getSelectedSlot() == slot) { // TODO: 我觉得这里应该不会跟服务端同步，或许开个AlwaysListening监听一下数据包？
            return true;
        }
        if (saveSwap && previousSlot == -1) {
            previousSlot = mc.player.getInventory().getSelectedSlot();
        } else if (!saveSwap) {
            previousSlot = -1;
        }
        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.ensureHasSentCarriedItem();
        return true;
    }

    public static void swapBack() {
        if (previousSlot == -1) return;
        mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }

    public static boolean invSwap(int slot) {
        if (slot >= 0) {
            int containerSlot = slot;
            if (slot < 9) containerSlot += 36;
            else if (slot == 40) containerSlot = 45;

            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, containerSlot, selectedSlot, ContainerInput.SWAP, mc.player);
            invSlots = new int[]{containerSlot, selectedSlot};
            return true;
        }
        return false;
    }

    public static void invSwapBack() {
        if (invSlots == null || invSlots.length < 2) return;
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, invSlots[0], invSlots[1], ContainerInput.SWAP, mc.player);
    }

}
