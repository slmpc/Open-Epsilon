package com.github.lumin.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class InvUtils {

    private static final Minecraft mc = Minecraft.getInstance();

    public static int previousSlot = -1;
    public static int[] invSlots;

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandItem());
    }

    public static boolean testInMainHand(Item... items) {
        return testInMainHand(itemStack -> {
            for (var item : items) {
                if (itemStack.is(item)) return true;
            }
            return false;
        });
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffhandItem());
    }

    public static boolean testInOffHand(Item... items) {
        return testInOffHand(itemStack -> {
            for (var item : items) {
                if (itemStack.is(item)) return true;
            }
            return false;
        });
    }

    public static boolean testInHands(Predicate<ItemStack> predicate) {
        return testInMainHand(predicate) || testInOffHand(predicate);
    }

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
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
        }

        if (testInMainHand(isGood)) {
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
        if (mc.player == null) return new FindItemResult(0, 0, 0);
        return find(isGood, 0, mc.player.getInventory().getContainerSize());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (mc.player == null) return new FindItemResult(0, 0, 0);

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

    public static FindItemResult findFastestTool(BlockState state, Boolean inv) {
        float bestScore = 1;
        int slot = -1;

        for (int i = 0; i < (inv ? mc.player.getInventory().getContainerSize() : 9); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isCorrectToolForDrops(state)) continue;

            float score = stack.getDestroySpeed(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1, 1);
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == 40) return true;
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
        else if (!swapBack) previousSlot = -1;

        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.ensureHasSentCarriedItem();
        return true;
    }

    public static void swapBack() {
        if (previousSlot == -1) return;
        if (previousSlot == 40) return;
        if (previousSlot < 0 || previousSlot > 8) return;

        mc.player.getInventory().setSelectedSlot(previousSlot);
        previousSlot = -1;
    }

    public static boolean invSwap(int slot) {
        if (slot >= 0) {
            int containerSlot = slot;
            if (slot < 9) containerSlot += 36;
            else if (slot == 40) containerSlot = 45;

            AbstractContainerMenu handler = mc.player.containerMenu;
            int selectedSlot = mc.player.getInventory().getSelectedSlot();

            mc.gameMode.handleContainerInput(handler.containerId, containerSlot, selectedSlot, ContainerInput.SWAP, mc.player);

            invSlots = new int[]{containerSlot, selectedSlot};
            return true;
        }
        return false;
    }

    public static void invSwapBack() {
        if (invSlots == null || invSlots.length < 2) return;
        AbstractContainerMenu handler = mc.player.containerMenu;

        mc.gameMode.handleContainerInput(handler.containerId, invSlots[0], invSlots[1], ContainerInput.SWAP, mc.player);
    }

}
