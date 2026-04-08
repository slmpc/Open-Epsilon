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
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

public class ModuleList extends HudModule {

    private static final float ROW_HEIGHT = 16.0f;
    private static final float ROW_SPACING = 2.0f;
    private static final float ICON_GAP = 2.0f;

    public static final ModuleList INSTANCE = new ModuleList();
    private final Map<Module, Float> moduleAlphaMap = new HashMap<>();

    private ModuleList() {
        super("Module List", Category.RENDER, 0f, 0f, 50f, 50f);
    }

    private final DoubleSetting scale = doubleSetting("Scale", 1.0, 0.5, 2.0, 0.1);
    private final DoubleSetting textScaleOffset = doubleSetting("Text Scale Offset", -0.2, -0.5, 0.5, 0.05);
    private final DoubleSetting horizontalPadding = doubleSetting("Horizontal Padding", 4.0, 0.0, 15.0, 1.0);
    private final DoubleSetting animSpeed = doubleSetting("Animation Speed", 10.0, 1.0, 20.0, 0.5);

    private final ColorSetting shadowColor = colorSetting("Shadow Color", new Color(20, 20, 20, 120));
    private final BoolSetting showCategory = boolSetting("Show Category", false);
    private final BoolSetting showIcon = boolSetting("Show Icon", true);

    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);
    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);

    @Override
    protected void updateBounds(DeltaTracker delta) {
        if (nullCheck()) return;

        List<ItemInfo> items = collectItems(delta);
        if (items.isEmpty()) {
            setBounds(0.0f, 0.0f);
            return;
        }

        float moduleScale = scale.getValue().floatValue();
        float maxTotalWidth = 0.0f;
        float totalHeight = 0.0f;

        for (ItemInfo item : items) {
            if (item.alpha() > 0.001f) {
                if (item.totalWidth() > maxTotalWidth) maxTotalWidth = item.totalWidth();

                totalHeight += (item.boxHeight() + ROW_SPACING * moduleScale) * item.alpha();
            }
        }

        setBounds(maxTotalWidth, totalHeight);
    }

    @Override
    public void render(DeltaTracker delta) {
        if (nullCheck()) return;

        List<ItemInfo> items = collectItems(delta);
        if (items.isEmpty()) return;

        TextRenderer textRenderer = textRendererSupplier.get();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();

        float moduleScale = scale.getValue().floatValue();
        float renderScale = moduleScale + textScaleOffset.getValue().floatValue();
        float hPadding = horizontalPadding.getValue().floatValue();

        HorizontalAnchor hAnchor = getHorizontalAnchor();
        float currentY = this.y;

        for (ItemInfo item : items) {
            if (item.alpha() <= 0.001f) continue;

            float alpha = Mth.clamp(item.alpha(), 0.0f, 1.0f);
            float boxHeight = item.boxHeight();
            float boxWidth = item.boxWidth();

            float itemX;
            switch (hAnchor) {
                case Right -> itemX = this.x + this.width - item.totalWidth();
                case Center -> itemX = this.x + (this.width - item.totalWidth()) / 2.0f;
                default -> itemX = this.x;
            }

            Color baseShadow = shadowColor.getValue();
            Color animatedShadow = new Color(baseShadow.getRed(), baseShadow.getGreen(), baseShadow.getBlue(), (int) (baseShadow.getAlpha() * alpha));
            Color animatedText = new Color(255, 255, 255, (int) (220 * alpha));

            roundRectRenderer.addRoundRect(itemX, currentY, boxWidth, boxHeight, 6.0f * moduleScale, animatedShadow);

            float textX = itemX + hPadding * moduleScale;
            float textY = currentY + (boxHeight - textRenderer.getHeight(renderScale)) / 2.0f;
            textRenderer.addText(item.text(), textX - 1, textY - 1, renderScale, animatedText);

            if (showIcon.getValue() && item.module().category != null) {
                float iconBoxX = itemX + boxWidth + ICON_GAP * moduleScale;
                roundRectRenderer.addRoundRect(iconBoxX, currentY, boxHeight, boxHeight, 6.0f * moduleScale, animatedShadow);

                String iconChar = item.module().category.icon;
                float iconScale = moduleScale * 0.8f;
                float iconWidth = textRenderer.getWidth(iconChar, iconScale, StaticFontLoader.ICONS);
                float iconHeight = textRenderer.getHeight(iconScale, StaticFontLoader.ICONS);

                float iconX = iconBoxX + (boxHeight - iconWidth) / 2.0f - 1;
                float iconY = currentY + (boxHeight - iconHeight) / 2.0f - 2;

                textRenderer.addText(iconChar, iconX, iconY, iconScale, new Color(255, 255, 255, (int) (180 * alpha)), StaticFontLoader.ICONS);
            }
            currentY += (boxHeight + ROW_SPACING * moduleScale) * alpha;
        }
        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private List<ItemInfo> collectItems(DeltaTracker delta) {
        List<Module> allModules = ModuleManager.INSTANCE.getModules();
        float frameTime = delta == null ? 0.05f : delta.getGameTimeDeltaTicks() / 20.0f;
        float speed = animSpeed.getValue().floatValue();

        for (Module module : allModules) {
            float target = module.isEnabled() ? 1.0f : 0.0f;
            float current = moduleAlphaMap.getOrDefault(module, 0.0f);

            if (Math.abs(current - target) > 0.001f) {
                current = Mth.lerp(speed * frameTime, current, target);
                moduleAlphaMap.put(module, current);
            } else {
                moduleAlphaMap.put(module, target);
            }
        }

        List<Module> activeModules = allModules.stream()
                .filter(m -> moduleAlphaMap.getOrDefault(m, 0.0f) > 0.001f)
                .sorted(Comparator.comparingInt(m -> -getTextWidth(m)))
                .toList();

        TextRenderer textRenderer = textRendererSupplier.get();
        float moduleScale = scale.getValue().floatValue();
        float renderScale = moduleScale + textScaleOffset.getValue().floatValue();
        float hPadding = horizontalPadding.getValue().floatValue();

        List<ItemInfo> items = new ArrayList<>();
        for (Module module : activeModules) {
            String text = getFormattedName(module);
            float alpha = moduleAlphaMap.get(module);

            float textWidth = textRenderer.getWidth(text, renderScale);
            float boxWidth = textWidth + (hPadding * moduleScale * 2.0f);
            float boxHeight = ROW_HEIGHT * moduleScale;
            float totalWidth = boxWidth;

            if (showIcon.getValue() && module.category != null) {
                totalWidth += boxHeight + ICON_GAP * moduleScale;
            }

            items.add(new ItemInfo(module, text, boxWidth, boxHeight, totalWidth, alpha));
        }

        return items;
    }

    private int getTextWidth(Module module) {
        TextRenderer textRenderer = textRendererSupplier.get();
        float renderScale = scale.getValue().floatValue() + textScaleOffset.getValue().floatValue();
        return (int) textRenderer.getWidth(getFormattedName(module), renderScale);
    }

    private String getFormattedName(Module module) {
        String text = module.getTranslatedName();
        if (showCategory.getValue() && module.category != null) {
            text += " [" + module.category.getName() + "]";
        }
        return text;
    }

    private record ItemInfo(Module module, String text, float boxWidth, float boxHeight, float totalWidth,
                            float alpha) {
    }
}