package com.github.epsilon.modules.impl.render.notification;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.HudModule;
import com.github.epsilon.settings.impl.ColorSetting;
import com.github.epsilon.settings.impl.DoubleSetting;
import com.google.common.base.Suppliers;
import net.minecraft.client.DeltaTracker;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Notifications extends HudModule {

    public static final Notifications INSTANCE = new Notifications();

    private final DoubleSetting scale = doubleSetting("Scale", 1.0, 0.5, 2.0, 0.1);
    private final DoubleSetting animSpeed = doubleSetting("Animation Speed", 10.0, 1.0, 20.0, 0.5);
    private final ColorSetting bgColor = colorSetting("Background Color", new Color(20, 20, 20, 150));

    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);
    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);

    private Notifications() {
        super("Notifications", Category.RENDER, 10f, 10f, 150f, 35f);
    }

    @Override
    protected void updateBounds(DeltaTracker delta) {
        float moduleScale = scale.getValue().floatValue();
        float spacing = 38.0f * moduleScale;

        long activeCount = NotificationManager.INSTANCE.getNotifications().stream()
                .filter(n -> n.getAlpha() > 0.001f).count();

        setBounds(150f * moduleScale, Math.max(35f * moduleScale, activeCount * spacing));
    }

    @Override
    public void render(DeltaTracker delta) {
        float frameTime = delta == null ? 0.05f : delta.getGameTimeDeltaTicks() / 20.0f;
        NotificationManager.INSTANCE.update(animSpeed.getValue().floatValue(), frameTime);

        List<Notification> list = new ArrayList<>(NotificationManager.INSTANCE.getNotifications());
        if (list.isEmpty()) return;

        Collections.reverse(list);

        TextRenderer textRenderer = textRendererSupplier.get();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();

        float moduleScale = scale.getValue().floatValue();
        float spacing = 38.0f * moduleScale;
        float currentY = this.y;

        HorizontalAnchor hAnchor = getHorizontalAnchor();
        VerticalAnchor vAnchor = getVerticalAnchor();

        for (Notification n : list) {
            float alpha = n.getAlpha();
            if (alpha <= 0.001f) continue;

            float boxWidth = 140.0f * moduleScale;
            float boxHeight = 32.0f * moduleScale;

            float itemX = switch (hAnchor) {
                case Right -> this.x + this.width - boxWidth;
                case Center -> this.x + (this.width - boxWidth) / 2.0f;
                default -> this.x;
            };

            float xOffset = (1.0f - alpha) * 20.0f * (hAnchor == HorizontalAnchor.Right ? 1 : -1);
            float renderX = itemX + xOffset;

            Color baseBg = bgColor.getValue();
            Color animatedBg = new Color(baseBg.getRed(), baseBg.getGreen(), baseBg.getBlue(), (int) (baseBg.getAlpha() * alpha));
            roundRectRenderer.addRoundRect(renderX, currentY, boxWidth, boxHeight, 6.0f * moduleScale, animatedBg);

            Color dotColor = getModeColor(n.getMode(), alpha);
            float dotSize = 6.0f * moduleScale;
            float dotX = renderX + 8.0f * moduleScale;
            float dotY = currentY + (boxHeight - dotSize) / 2.0f;
            roundRectRenderer.addRoundRect(dotX, dotY, dotSize, dotSize, dotSize / 2.0f, dotColor);

            float textX = dotX + dotSize + 8.0f * moduleScale;
            textRenderer.addText(n.getTitle(), textX, currentY + 5.0f * moduleScale, moduleScale * 0.85f, new Color(255, 255, 255, (int) (255 * alpha)));
            textRenderer.addText(n.getSubTitle(), textX, currentY + 18.0f * moduleScale, moduleScale * 0.75f, new Color(200, 200, 200, (int) (200 * alpha)));

            if (vAnchor == VerticalAnchor.Bottom) {
                currentY -= spacing;
            } else {
                currentY += spacing;
            }
        }
        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private Color getModeColor(NotificationMode mode, float alpha) {
        int a = (int) (255 * alpha);
        return switch (mode) {
            case Success -> new Color(85, 255, 85, a);
            case Error -> new Color(255, 85, 85, a);
            case Question -> new Color(255, 255, 85, a);
            case Info -> new Color(85, 170, 255, a);
        };
    }

}