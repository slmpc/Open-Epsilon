package com.github.epsilon.modules.impl.render;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.graphics.text.StaticFontLoader;
import com.github.epsilon.managers.ModuleManager;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.google.common.base.Suppliers;
import net.minecraft.client.DeltaTracker;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModuleList extends HudModule {

    public static final ModuleList INSTANCE = new ModuleList();

    private ModuleList() {
        super("ModuleList", Category.RENDER, 0f, 0f, 50f, 50f);
    }

    private final DoubleSetting scale = doubleSetting("Scale", 1.0, 0.5, 2.0, 0.1);
    private final ColorSetting shadowColor = colorSetting("ShadowColor", new Color(68, 0, 0, 94));
    private final BoolSetting showCategory = boolSetting("ShowCategory", false);
    private final BoolSetting showIcon = boolSetting("ShowIcon", true);
    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);
    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);

    @Override
    public void render(DeltaTracker delta) {
        if (nullCheck()) return;

        List<Module> enabledModules = ModuleManager.INSTANCE.getModules().stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());

        if (enabledModules.isEmpty()) return;

        enabledModules.sort(Comparator.comparingInt(m -> -getTextWidth(m)));

        TextRenderer textRenderer = textRendererSupplier.get();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();

        float moduleScale = scale.getValue().floatValue();

        List<ItemInfo> items = new ArrayList<>();

        for (Module module : enabledModules) {
            String text = module.getTranslatedName();
            if (showCategory.getValue() && module.category != null) {
                text += " [" + module.category.getName() + "]";
            }
            float textWidth = textRenderer.getWidth(text, moduleScale);
            float boxWidth = textWidth + 4.0f * moduleScale * 2;
            float boxHeight = 16.0f * moduleScale;
            float totalWidth = boxWidth;
            if (showIcon.getValue() && module.category != null) {
                totalWidth += boxHeight + 2.0f * moduleScale;
            }
            items.add(new ItemInfo(module, text, boxWidth, boxHeight, totalWidth));
        }

        float maxTotalWidth = 0;
        float totalHeight = 0;
        for (ItemInfo item : items) {
            if (item.totalWidth() > maxTotalWidth) maxTotalWidth = item.totalWidth();
            totalHeight += item.boxHeight() + 2.0f * moduleScale;
        }
        this.width = maxTotalWidth + 4.0f * moduleScale;
        this.height = totalHeight;

        float currentY = this.y;

        for (ItemInfo item : items) {
            float totalX = this.x + this.width - item.totalWidth();
            float boxY = currentY;

            float textBoxX = totalX;
            float iconBoxX = totalX + item.boxWidth() + 2.0f * moduleScale;

            roundRectRenderer.addRoundRect(textBoxX, boxY, item.boxWidth(), item.boxHeight(), 6.0f * moduleScale, shadowColor.getValue());

            float textX = textBoxX + 4.0f * moduleScale - 1.5f;
            float textY = boxY + (item.boxHeight() - textRenderer.getHeight(moduleScale)) / 5.0f;
            textRenderer.addText(item.text(), textX + 1, textY, moduleScale, new Color(255, 255, 255, 126));

            if (showIcon.getValue() && item.module().category != null) {
                roundRectRenderer.addRoundRect(iconBoxX, boxY, item.boxHeight(), item.boxHeight(), 6.0f * moduleScale, shadowColor.getValue());

                String iconChar = item.module().category.icon;
                float iconScale = moduleScale * 0.8f;
                float iconWidth = textRenderer.getWidth(iconChar, iconScale, StaticFontLoader.ICONS);
                float iconHeight = textRenderer.getHeight(iconScale, StaticFontLoader.ICONS);
                float iconX = iconBoxX + (item.boxHeight() - iconWidth) / 3.0f;
                float iconY = boxY + (item.boxHeight() - iconHeight) / 5.0f;
                textRenderer.addText(iconChar, iconX, iconY, iconScale, new Color(255, 255, 255, 92), StaticFontLoader.ICONS);
            }

            currentY += item.boxHeight() + 2.0f * moduleScale;
        }

        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private int getTextWidth(Module module) {
        TextRenderer textRenderer = textRendererSupplier.get();
        String text = module.getTranslatedName();
        if (showCategory.getValue() && module.category != null) {
            text += " [" + module.category.getName() + "]";
        }
        return (int) textRenderer.getWidth(text, scale.getValue().floatValue());
    }

    private record ItemInfo(Module module, String text, float boxWidth, float boxHeight, float totalWidth) {
    }
}
