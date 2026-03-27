package com.github.lumin.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class InvHelper {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );

    public static boolean shouldDisableFeatures() {
        return getAllItems().stream().anyMatch(item -> {
            if (item.isEmpty()) {
                return false;
            } else {
                String string = item.getDisplayName().getString();
                return string.contains("长按点击") || string.contains("点击使用") || string.contains("离开游戏") || string.contains("选择一个队伍") || string.contains("再来一局");
            }
        });
    }

    public static boolean isGoldenHead(ItemStack e) {
        if (e.isEmpty()) {
            return false;
        } else {
            if (e.getItem() instanceof BlockItem item) {
                return item.getBlock() instanceof SkullBlock;
            }

            return false;
        }
    }

    public static boolean isSharpnessAxe(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (!stack.is(ItemTags.AXES)) {
            return false;
        } else {
            int itemEnchantmentLevel = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
            return itemEnchantmentLevel >= 8 && itemEnchantmentLevel < 50;
        }
    }

    public static boolean isGodAxe(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack.getItem() == Items.GOLDEN_AXE && EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SHARPNESS) > 100;
        }
    }

    public static boolean isEnchantedGApple(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isEndCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.END_CRYSTAL;
    }

    public static boolean isKBBall(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack.getItem() == Items.SLIME_BALL && EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.KNOCKBACK) > 1;
        }
    }

    public static boolean isKBStick(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            return stack.getItem() == Items.STICK && EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.KNOCKBACK) > 1;
        }
    }

    public static int findEmptyInventory() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public static int findEmptySlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public static Integer findItemHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() == item) {
                return i;
            }
        }

        return null;
    }

    public static int getPunchLevel(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PUNCH);
    }

    public static int getPowerLevel(ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.POWER);
    }

    public static List<ItemStack> getAllItems() {
        List<ItemStack> list = new ArrayList<>(40);
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            list.add(mc.player.getInventory().getItem(i));
        }
        return list;
    }

    public static float getBestArmorScore(EquipmentSlot slot) {
        return getAllItems()
                .stream()
                .filter(item -> {
                    if (item.isEmpty() || !item.is(ItemTags.ARMOR_ENCHANTABLE)) return false;
                    var equippable = item.get(DataComponents.EQUIPPABLE);
                    return equippable != null && equippable.slot() == slot;
                })
                .map(InvHelper::getProtection)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static float getCurrentArmorScore(EquipmentSlot slot) {
        return getProtection(mc.player.getItemBySlot(slot));
    }

    public static float getBestSwordDamage() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.SWORDS))
                .map(InvHelper::getSwordDamage)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestSword() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.SWORDS))
                .max(Comparator.comparingInt(s -> (int) (getSwordDamage(s) * 100.0F)))
                .orElse(null);
    }

    public static int getItemStackSlot(ItemStack stack) {
        if (stack == null) {
            return -1;
        } else {
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getItem(i) == stack) {
                    return i;
                }
            }

            return -1;
        }
    }

    public static boolean isItemValid(ItemStack s) {
        if (!s.isEmpty()) {
            if (s.getItem() instanceof PlayerHeadItem) {
                return false;
            }

            String string = s.getDisplayName().getString();
            if (string.contains("Click")) {
                return false;
            }

            if (string.contains("Right")) {
                return false;
            }

            if (string.contains("点击")) {
                return false;
            }

            if (string.contains("Teleport")) {
                return false;
            }

            if (string.contains("使用")) {
                return false;
            }

            if (string.contains("传送")) {
                return false;
            }

            return !string.contains("再来");
        }

        return true;
    }

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!isItemValid(stack)) {
            return false;
        } else if (stack.has(DataComponents.CUSTOM_NAME)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FlowerPotBlock || block instanceof NetherFungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return !(block instanceof SlabBlock) && !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    public static int getItemSlot(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = mc.player.getInventory().getItem(i);
            if (itemStack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static ItemStack getBestProjectile() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL) && isItemValid(item))
                .max(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    public static ItemStack getFishingRod() {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() instanceof FishingRodItem && isItemValid(item)).findAny().orElse(null);
    }

    public static int getBlockCountInInventory() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && isValidStack(item) && isItemValid(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    public static ItemStack getWorstProjectile() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && (item.getItem() == Items.EGG || item.getItem() == Items.SNOWBALL))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    public static ItemStack getWorstArrow() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof ArrowItem && isItemValid(item))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    public static ItemStack getWorstBlock() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && isValidStack(item) && isItemValid(item))
                .min(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    public static ItemStack getBestBlock() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BlockItem && isValidStack(item) && isItemValid(item))
                .max(Comparator.comparingInt(ItemStack::getCount))
                .orElse(null);
    }

    public static float getBestPickaxeScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.PICKAXES) && isItemValid(item))
                .map(InvHelper::getToolScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestPickaxe() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.PICKAXES) && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100.0F)))
                .orElse(null);
    }

    public static float getBestAxeScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.AXES) && !isSharpnessAxe(item) && isItemValid(item))
                .map(InvHelper::getToolScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestAxe() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.AXES) && !isSharpnessAxe(item) && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100.0F)))
                .orElse(null);
    }

    public static ItemStack getBestShapeAxe() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(net.minecraft.tags.ItemTags.AXES) && isSharpnessAxe(item) && isItemValid(item) && !isGodAxe(item))
                .max(Comparator.comparingInt(s -> (int) (getAxeDamage(s) * 100.0F)))
                .orElse(null);
    }

    public static float getBestShovelScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.SHOVELS) && isItemValid(item))
                .map(InvHelper::getToolScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestShovel() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.is(ItemTags.SHOVELS) && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getToolScore(s) * 100.0F)))
                .orElse(null);
    }

    public static float getBestCrossbowScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
                .map(InvHelper::getCrossbowScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestCrossbow() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof CrossbowItem && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getCrossbowScore(s) * 100.0F)))
                .orElse(null);
    }

    public static float getBestPunchBowScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
                .map(InvHelper::getPunchBowScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestPunchBow() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getPunchBowScore(s) * 100.0F)))
                .orElse(null);
    }

    public static float getBestPowerBowScore() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
                .map(InvHelper::getPowerBowScore)
                .max(Float::compareTo)
                .orElse(0.0F);
    }

    public static ItemStack getBestPowerBow() {
        return getAllItems()
                .stream()
                .filter(item -> !item.isEmpty() && item.getItem() instanceof BowItem && isItemValid(item))
                .max(Comparator.comparingInt(s -> (int) (getPowerBowScore(s) * 100.0F)))
                .orElse(null);
    }

    public static boolean isPunchBow(ItemStack stack) {
        return getPunchBowScore(stack) > 10.0F && isItemValid(stack);
    }

    public static boolean isPowerBow(ItemStack stack) {
        return getPowerBowScore(stack) > 10.0F && isItemValid(stack);
    }

    public static boolean hasItem(Item checkItem) {
        return getAllItems().stream().anyMatch(item -> !item.isEmpty() && item.getItem() == checkItem);
    }

    public static int getItemCount(Item checkItem) {
        return getAllItems().stream().filter(item -> !item.isEmpty() && item.getItem() == checkItem).mapToInt(ItemStack::getCount).sum();
    }

    public static float getPunchBowScore(ItemStack stack) {
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else if (stack.getItem() instanceof BowItem) {
            float valence = 10.0F;
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PUNCH);
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.INFINITY);
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FLAME);
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.POWER) / 10.0F;
            return valence + (float) stack.getDamageValue() / (float) stack.getMaxDamage();
        } else {
            return 0.0F;
        }
    }

    public static float getPowerBowScore(ItemStack stack) {
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else if (stack.getItem() instanceof BowItem) {
            float valence = 10.0F;
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PUNCH) / 10.0F;
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.INFINITY);
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.FLAME);
            valence += (float) EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.POWER);
            return valence + (float) stack.getDamageValue() / (float) stack.getMaxDamage();
        } else {
            return 0.0F;
        }
    }

    public static float getToolScore(ItemStack stack) {
        float valence = 0.0F;
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else if (isGodItem(stack)) {
            return 0.0F;
        } else if (isSharpnessAxe(stack)) {
            return 0.0F;
        } else {
            if (stack.is(ItemTags.PICKAXES)) {
                valence += stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
            } else if (stack.is(net.minecraft.tags.ItemTags.AXES)) {
                valence += stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState());
            } else {
                if (!stack.is(ItemTags.SHOVELS)) {
                    return 0.0F;
                }

                valence += stack.getDestroySpeed(Blocks.DIRT.defaultBlockState());
            }

            int efficiency = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
            if (efficiency > 0) {
                valence += (float) efficiency * 0.0075F;
            }

            return valence;
        }
    }

    public static float getAxeDamage(ItemStack stack) {
        float valence = 0.0F;
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else {
            if (stack.is(ItemTags.AXES) && isSharpnessAxe(stack)) {
                Item axe = stack.getItem();
                if (axe == Items.WOODEN_AXE) {
                    valence += 4.0F;
                } else if (axe == Items.STONE_AXE) {
                    valence += 5.0F;
                } else if (axe == Items.IRON_AXE) {
                    valence += 6.0F;
                } else if (axe == Items.GOLDEN_AXE) {
                    valence += 4.0F;
                } else if (axe == Items.DIAMOND_AXE) {
                    valence += 7.0F;
                }
            }

            int itemEnchantmentLevel = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
            if (itemEnchantmentLevel > 0) {
                float damageBonus = 0.5F + 0.5F * itemEnchantmentLevel;
                valence += damageBonus;
            }

            return valence;
        }
    }

    public static float getSwordDamage(ItemStack stack) {
        float valence = 0.0F;
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else {
            if (stack.is(ItemTags.SWORDS)) {
                Item item = stack.getItem();
                if (item == Items.WOODEN_SWORD || item == Items.GOLDEN_SWORD) valence += 4.0F;
                else if (item == Items.STONE_SWORD) valence += 5.0F;
                else if (item == Items.IRON_SWORD) valence += 6.0F;
                else if (item == Items.DIAMOND_SWORD) valence += 7.0F;
                else if (item == Items.NETHERITE_SWORD) valence += 8.0F;
                else valence += 5.0F;
            }

            int itemEnchantmentLevel = EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
            if (itemEnchantmentLevel > 0) {
                float damageBonus = 0.5F + 0.5F * itemEnchantmentLevel;
                valence += damageBonus;
            }

            return valence;
        }
    }

    public static float getProtection(ItemStack itemStack) {
        if (itemStack == null) {
            return 0.0F;
        } else if (itemStack.isEmpty()) {
            return 0.0F;
        } else if (!itemStack.is(ItemTags.ARMOR_ENCHANTABLE)) {
            return 0.0F;
        } else {
            float armor = 0.0F;
            float toughness = 0.0F;
            float knockbackResistance = 0.0F;

            ItemAttributeModifiers attrComp = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (attrComp != null) {
                for (var entry : attrComp.modifiers()) {
                    if (entry.attribute().value() == Attributes.ARMOR.value()) {
                        armor += (float) entry.modifier().amount();
                    } else if (entry.attribute().value() == Attributes.ARMOR_TOUGHNESS.value()) {
                        toughness += (float) entry.modifier().amount();
                    } else if (entry.attribute().value() == Attributes.KNOCKBACK_RESISTANCE.value()) {
                        knockbackResistance += (float) entry.modifier().amount();
                    }
                }
            }

            int protection = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.PROTECTION);
            int blastProtection = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.BLAST_PROTECTION);
            int fireProtection = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.FIRE_PROTECTION);
            int projectileProtection = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.PROJECTILE_PROTECTION);
            int featherFalling = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.FEATHER_FALLING);
            int thorns = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.THORNS);
            int unbreaking = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.UNBREAKING);
            int mending = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.MENDING);
            int bindingCurse = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.BINDING_CURSE);
            int vanishingCurse = EnchantmentUtils.getEnchantmentLevel(itemStack, Enchantments.VANISHING_CURSE);

            float durabilityScore = 0.0F;
            if (itemStack.isDamageableItem() && itemStack.getMaxDamage() > 0) {
                float remaining = 1.0F - ((float) itemStack.getDamageValue() / (float) itemStack.getMaxDamage());
                durabilityScore = remaining * 0.75F;
            }

            float enchantScore = protection * 4.0F + (blastProtection + fireProtection + projectileProtection) * 3.0F + featherFalling * 2.5F + thorns * 0.5F + unbreaking * 0.25F + mending * 1.5F - (bindingCurse + vanishingCurse) * 50.0F;

            return armor * 10.0F + toughness * 8.0F + knockbackResistance * 30.0F + durabilityScore + enchantScore;
        }
    }

    public static float getCrossbowScore(ItemStack stack) {
        int valence = 0;
        if (stack == null) {
            return 0.0F;
        } else if (stack.isEmpty()) {
            return 0.0F;
        } else {
            if (stack.getItem() instanceof CrossbowItem) {
                valence += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.QUICK_CHARGE);
                valence += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.MULTISHOT);
                valence += EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.PIERCING);
            }

            return (float) valence;
        }
    }

    public static boolean isGodItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (stack.getItem() == Items.GOLDEN_AXE
                && EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.SHARPNESS) > 100) {
            return true;
        } else if (stack.getItem() == Items.SLIME_BALL && EnchantmentUtils.getEnchantmentLevel(stack, Enchantments.KNOCKBACK) > 1) {
            return true;
        } else {
            return stack.getItem() == Items.TOTEM_OF_UNDYING || stack.getItem() == Items.END_CRYSTAL;
        }
    }

    public static boolean isCommonItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        } else {
            Item item = stack.getItem();
            if (item instanceof BlockItem block) {
                if (block.getBlock() == Blocks.ENCHANTING_TABLE) {
                    return false;
                }

                return block.getBlock() != Blocks.COBWEB;
            } else {
                if (item == Items.BOOK || item == Items.ENCHANTED_BOOK || item == Items.WRITTEN_BOOK || item == Items.WRITABLE_BOOK) {
                    return false;
                }

                if (item instanceof ExperienceBottleItem) {
                    return false;
                }

                if (item instanceof FireworkRocketItem) {
                    return false;
                }

                if (item == Items.WHEAT_SEEDS || item == Items.BEETROOT_SEEDS || item == Items.MELON_SEEDS || item == Items.PUMPKIN_SEEDS) {
                    return false;
                }

                return item != Items.FLINT_AND_STEEL;
            }
        }
    }

}
