package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.render.WorldToScreen;
import com.google.common.base.Suppliers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector4d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class Nametags extends Module {

    public static final Nametags INSTANCE = new Nametags();

    private Nametags() {
        super("Nametags", Category.RENDER);
    }

    private final BoolSetting showItems = boolSetting("ShowItems", false);
    private final BoolSetting showHealthText = boolSetting("ShowHealthText", true);
    private final BoolSetting showMobs = boolSetting("ShowMobs", true);
    private final IntSetting visualRange = intSetting("VisualRange", 20,0,256,1);
    private final ColorSetting backgroundColor = colorSetting("BackgroundColor", new Color(0, 0, 0, 140));
    private final ColorSetting textColor = colorSetting("TextColor", Color.WHITE);

    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);
    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);

    private final List<TagInfo> tags = new ArrayList<>();

    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {
        if (nullCheck()) return;
        if (tags.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        TextRenderer textRenderer = textRendererSupplier.get();

        for (TagInfo tag : tags) {
            if (showItems.getValue() && !tag.items().isEmpty()) {
                float itemRowW = (tag.items().size() * 16.0f + (tag.items().size() - 1) * 2.0f) * tag.scale();
                float itemsLeft = tag.x() - itemRowW * 0.5f;
                float itemY = tag.y() - (textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f) - 8.0f - 7.0f * tag.scale() - 2.0f;

                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(itemsLeft, itemY);
                guiGraphics.pose().scale(tag.scale(), tag.scale());
                guiGraphics.pose().translate(-itemsLeft, -itemY);

                int seed = 0;
                for (int i = 0; i < tag.items().size(); i++) {
                    ItemStack stack = tag.items().get(i);
                    if (stack == null || stack.isEmpty()) continue;
                    guiGraphics.renderItem(mc.player, stack, (int) (itemsLeft + i * 18.0f), (int) itemY, seed++);
                    guiGraphics.renderItemDecorations(mc.font, stack, (int) (itemsLeft + i * 18.0f), (int) itemY);
                }

                guiGraphics.pose().popMatrix();
            }
            //我去谁书写的一大坨狗屎给我鸡巴都吓软了
            float prefixW = textRenderer.getWidth(tag.prefix(), tag.scale(), StaticFontLoader.REGULAR);
            float nameW = textRenderer.getWidth(tag.name(), tag.scale(), StaticFontLoader.REGULAR);
            float hpW = showHealthText.getValue()
                    ? textRenderer.getWidth(tag.healthText(), tag.scale(), StaticFontLoader.REGULAR)
                    : 0.0f;

            float spacing = 4.0f;

            float totalW = prefixW + nameW;
            if (showHealthText.getValue() && hpW > 0.0f) {
                totalW += spacing + hpW;
            }

            totalW += spacing;

            float leftX = tag.x() - totalW * 0.5f;

            float textY = tag.y() - textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR)
                    + 8.0f - 8.0f + 4.0f - 9;

            roundRectRenderer.addRoundRect(
                    leftX - 4.0f,
                    tag.y() - textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8 - 15,
                    totalW + 8.0f,
                    textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f,
                    6.0f * tag.scale(),
                    backgroundColor.getValue()
            );

            float cursorX = leftX;

            textRenderer.addText(
                    tag.prefix(),
                    cursorX,
                    textY,
                    tag.scale(),
                    tag.prefixColor(),
                    StaticFontLoader.REGULAR
            );
            cursorX += prefixW + spacing;

            textRenderer.addText(
                    tag.name(),
                    cursorX,
                    textY,
                    tag.scale(),
                    textColor.getValue(),
                    StaticFontLoader.REGULAR
            );
            cursorX += nameW;

            if (showHealthText.getValue() && hpW > 0.0f) {
                cursorX += spacing;

                textRenderer.addText(
                        tag.healthText(),
                        cursorX,
                        textY,
                        tag.scale(),
                        tag.healthColor(),
                        StaticFontLoader.REGULAR
                );
            }
        }

        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }


    @SubscribeEvent
    private void onRenderAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck()) return;

        tags.clear();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        float guiWidth = (float) mc.getWindow().getGuiScaledWidth();
        float guiHeight = (float) mc.getWindow().getGuiScaledHeight();


        for (Entity mob : StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).toList())
            if (mob instanceof LivingEntity entity) {
                Vec3 playerPos = entity.getPosition(partialTick);
                float dist = (float) playerPos.distanceTo(cameraPos);
                if (dist > visualRange.getValue()) continue;

                Vector4d screenPos = WorldToScreen.getEntityPositionsOn2D(entity, partialTick);

                float screenX = (float) screenPos.x;
                float screenY = (float) screenPos.y;

                if (screenX < -64.0f || screenY < -64.0f || screenX > guiWidth + 64.0f || screenY > guiHeight + 64.0f)
                    continue;

                String prefix = "[" + (entity instanceof Player ? "P" : "E") + "]";
                String name = entity.getName().getString();

                Color prefixColor = Color.CYAN;
                float scale = Math.max(0.65f, 1.0f - (dist / 256) * 0.35f);

                float maxHealth = entity.getMaxHealth();
                float health = entity.getHealth() + entity.getAbsorptionAmount();
                String hpText = String.format("%.1f", health);
                Color hpColor = getHealthColor(maxHealth > 0.0f ? health / maxHealth : 0.0f);

                List<ItemStack> items = new ArrayList<>();
                if (showItems.getValue()) {
                    ItemStack off = entity.getOffhandItem();
                    if (!off.isEmpty()) items.add(off);

                    ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
                    ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
                    ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
                    ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);

                    if (!head.isEmpty()) items.add(head);
                    if (!chest.isEmpty()) items.add(chest);
                    if (!legs.isEmpty()) items.add(legs);
                    if (!feet.isEmpty()) items.add(feet);

                    ItemStack main = entity.getMainHandItem();
                    if (!main.isEmpty()) items.add(main);
                }
                if(!showMobs.getValue() && !(entity instanceof Player)) continue;
                tags.add(new TagInfo(prefix, name, hpText, prefixColor, hpColor, items, screenX, screenY, scale));
            }

    }

    private record TagInfo(
            String prefix,
            String name,
            String healthText,
            Color prefixColor,
            Color healthColor,
            List<ItemStack> items,
            float x, float y,
            float scale
    ) {}

    private static Color getHealthColor(float frac) {
        frac = Mth.clamp(frac, 0.0f, 1.0f);
        if (frac > 0.5f) {
            float t = (frac - 0.5f) * 2.0f;
            return lerpColor(new Color(255, 255, 0), new Color(0, 255, 0), t);
        } else {
            float t = frac * 2.0f;
            return lerpColor(new Color(255, 0, 0), new Color(255, 255, 0), t);
        }
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

}
