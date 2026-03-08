package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.LineRenderer;
import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.google.common.base.Suppliers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.awt.*;
import java.util.function.Supplier;

public class RenderTest extends Module {

    public static final RenderTest INSTANCE = new RenderTest();

    private RenderTest() {
        super("渲染测试", "idk", Category.RENDER);
    }

    private final Supplier<RectRenderer> rectRendererSupplier = Suppliers.memoize(RectRenderer::new);
    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);
    private final Supplier<LineRenderer> lineRendererSupplier = Suppliers.memoize(LineRenderer::new);
    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);

    private long startTime = System.currentTimeMillis();

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        RectRenderer rectRenderer = rectRendererSupplier.get();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        LineRenderer lineRenderer = lineRendererSupplier.get();
        TextRenderer textRenderer = textRendererSupplier.get();

        float baseX = 50;
        float baseY = 50;
        long elapsed = System.currentTimeMillis() - startTime;
        float anim = (float) Math.sin(elapsed / 1000.0) * 10;

        rectRenderer.addRect(baseX, baseY, 100, 100, new Color(255, 100, 100, 200));
        rectRenderer.addRect(baseX + 120, baseY + anim, 80, 60, new Color(100, 255, 100, 180));

        roundRectRenderer.addRoundRect(baseX, baseY + 130, 200, 80, 15, new Color(100, 100, 255, 180));
        roundRectRenderer.addRoundRect(baseX + 220, baseY + 130, 150, 80, 25, new Color(255, 200, 100, 200));

        lineRenderer.addRectOutline(baseX - 5, baseY - 5, 210, 240, 2, new Color(255, 255, 255, 150));
        lineRenderer.addLine(baseX + 250, baseY, baseX + 400 + anim, baseY + 100, 2, new Color(255, 100, 255));
        lineRenderer.addLine(baseX + 250, baseY + 100, baseX + 400 - anim, baseY, 2, new Color(100, 255, 255));

        rectRenderer.addHorizontalGradient(baseX, baseY + 230, 200, 40, new Color(255, 0, 0, 180), new Color(0, 255, 0, 180));
        rectRenderer.addVerticalGradient(baseX + 220, baseY + 230, 200, 40, new Color(0, 0, 255, 180), new Color(255, 255, 0, 180));

        textRenderer.addGlowingText("Lumin Render Test", baseX, baseY + 290, 1.5f, new Color(255, 255, 255), 3.0f);
        textRenderer.addText("矩形、圆角矩形、线条、圆形、渐变", baseX, baseY + 320, 1.0f, new Color(200, 200, 200));
        textRenderer.addGlowingText("动画: " + String.format("%.2f", anim), baseX, baseY + 345, 1.0f, new Color(150, 255, 150), 1.5f);

        rectRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        lineRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    @Override
    protected void onEnable() {
        startTime = System.currentTimeMillis();
    }

}
