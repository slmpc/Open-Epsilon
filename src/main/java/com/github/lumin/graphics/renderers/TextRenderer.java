package com.github.lumin.graphics.renderers;

import com.github.lumin.graphics.LuminRenderSystem;
import com.github.lumin.graphics.text.ITextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.graphics.text.ttf.TtfFontLoader;
import com.github.lumin.graphics.text.ttf.TtfTextRenderer;

import java.awt.*;

public class TextRenderer implements IRenderer {

    private final ITextRenderer textRenderer;

    public TextRenderer(long bufferSize) {
        textRenderer = new TtfTextRenderer(bufferSize);
    }

    public TextRenderer() {
        textRenderer = new TtfTextRenderer();
    }

    public void addText(String text, float x, float y, float scale, Color color, TtfFontLoader fontLoader) {
        textRenderer.addText(text, x, y, scale, color, fontLoader);
    }

    public void addText(String text, float x, float y, float scale, Color color) {
        textRenderer.addText(text, x, y, scale, color, StaticFontLoader.DEFAULT);
    }

    public void addText(String text, float x, float y, Color color, TtfFontLoader fontLoader) {
        textRenderer.addText(text, x, y, 1.0f, color, fontLoader);
    }

    public void addText(String text, float x, float y, Color color) {
        textRenderer.addText(text, x, y, 1.0f, color, StaticFontLoader.DEFAULT);
    }

    public void addGlowingText(String text, float x, float y, float scale, Color color, float glowRadius, int intensity, TtfFontLoader fontLoader) {
        float radius = Math.max(0.5f, glowRadius);
        int quality = Math.max(1, intensity);
        int rings = Math.max(10, quality * 8);
        float sigma = 0.45f;
        int centerAlpha = Math.max(1, Math.min(255, Math.round(color.getAlpha() * 0.18f)));
        Color centerGlow = new Color(color.getRed(), color.getGreen(), color.getBlue(), centerAlpha);
        textRenderer.addText(text, x, y, scale, centerGlow, fontLoader);

        for (int ring = 1; ring <= rings; ring++) {
            float t = ring / (float) rings;
            float ringRadius = radius * t * t;
            int samples = Math.max(14, Math.round((float) (Math.PI * 2.0 * ringRadius * 2.0)));
            float weight = (float) Math.exp(-(t * t) / (2.0f * sigma * sigma));
            float alphaScale = 0.70f * weight / (float) Math.sqrt(samples);
            int sampleAlpha = Math.max(1, Math.min(255, Math.round(color.getAlpha() * alphaScale)));
            Color sampleColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), sampleAlpha);

            for (int i = 0; i < samples; i++) {
                double angle = (Math.PI * 2.0 * i) / samples;
                float offsetX = (float) (Math.cos(angle) * ringRadius);
                float offsetY = (float) (Math.sin(angle) * ringRadius);
                textRenderer.addText(text, x + offsetX, y + offsetY, scale, sampleColor, fontLoader);
            }
        }
        textRenderer.addText(text, x, y, scale, color, fontLoader);
    }

    public void addGlowingText(String text, float x, float y, float scale, Color color, float glowRadius, int intensity) {
        addGlowingText(text, x, y, scale, color, glowRadius, intensity, StaticFontLoader.DEFAULT);
    }

    public void addGlowingText(String text, float x, float y, float scale, Color color, float glowRadius) {
        addGlowingText(text, x, y, scale, color, glowRadius, 1, StaticFontLoader.DEFAULT);
    }

    public float getHeight(float scale) {
        return textRenderer.getHeight(scale, StaticFontLoader.DEFAULT);
    }

    public float getHeight(float scale, TtfFontLoader fontLoader) {
        return textRenderer.getHeight(scale, fontLoader);
    }

    public float getWidth(String text, float scale) {
        return textRenderer.getWidth(text, scale, StaticFontLoader.DEFAULT);
    }

    public float getWidth(String text, float scale, TtfFontLoader fontLoader) {
        return textRenderer.getWidth(text, scale, fontLoader);
    }

    public void setScissor(int x, int y, int width, int height) {
        textRenderer.setScissor(x, y, width, height);
    }

    public void clearScissor() {
        textRenderer.clearScissor();
    }

    @Override
    public void draw() {
        LuminRenderSystem.applyOrthoProjection();

        textRenderer.draw();
    }

    @Override
    public void clear() {
        textRenderer.clear();
    }

    @Override
    public void close() {
        textRenderer.close();
    }
}
