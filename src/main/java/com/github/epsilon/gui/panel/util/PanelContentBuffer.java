package com.github.epsilon.gui.panel.util;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.ShadowRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.PanelLayout;

public final class PanelContentBuffer {

    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final RoundRectRenderer scrollBarRenderer = new RoundRectRenderer();

    private boolean pending;

    public RoundRectRenderer roundRectRenderer() {
        return roundRectRenderer;
    }

    public RectRenderer rectRenderer() {
        return rectRenderer;
    }

    public ShadowRenderer shadowRenderer() {
        return shadowRenderer;
    }

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public void clear() {
        clearContent();
        scrollBarRenderer.clear();
        pending = false;
    }

    private void clearContent() {
        shadowRenderer.clear();
        roundRectRenderer.clear();
        rectRenderer.clear();
        textRenderer.clear();
    }

    public void queueViewport(PanelLayout.Rect viewport, int guiHeight, float scroll, float maxScroll, float contentHeight) {
        PanelScissor.apply(viewport, rectRenderer, roundRectRenderer, shadowRenderer, textRenderer, guiHeight);
        scrollBarRenderer.clear();
        ScrollBarUtil.draw(scrollBarRenderer, viewport, scroll, maxScroll, contentHeight);
        pending = true;
    }

    public void flush() {
        if (!pending) {
            return;
        }
        shadowRenderer.draw();
        roundRectRenderer.draw();
        rectRenderer.draw();
        textRenderer.draw();
        PanelScissor.clear(rectRenderer, roundRectRenderer, shadowRenderer, textRenderer);
        scrollBarRenderer.drawAndClear();
        pending = false;
    }

    public void flushAndClear() {
        flush();
        clearContent();
    }

}
