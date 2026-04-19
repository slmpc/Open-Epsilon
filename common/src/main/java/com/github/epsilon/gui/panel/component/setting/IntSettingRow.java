package com.github.epsilon.gui.panel.component.setting;

import com.github.epsilon.graphics.renderers.RectRenderer;
import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.graphics.renderers.TextRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.gui.panel.component.SettingRow;
import com.github.epsilon.settings.impl.IntSetting;
import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.awt.*;

public class IntSettingRow extends SettingRow<IntSetting> {

    private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private final Animation pressAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation indicatorAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private final TextRenderer measureTextRenderer = TextRenderer.create();
    private boolean dragging;
    private boolean focused;
    private String inputBuffer;
    private int cursorIndex;

    public IntSettingRow(IntSetting setting) {
        super(setting);
        hoverAnimation.setStartValue(0.0f);
        pressAnimation.setStartValue(0.0f);
        indicatorAnimation.setStartValue(0.0f);
    }

    @Override
    public void render(GuiGraphicsExtractor GuiGraphicsExtractor, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, PanelLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        hoverAnimation.run(dragging ? 1.0f : hoverProgress);
        pressAnimation.run(dragging ? 1.0f : 0.0f);
        indicatorAnimation.run((dragging || hoverProgress > 0.01f) ? 1.0f : 0.0f);

        float animatedHover = hoverAnimation.getValue();
        float animatedPress = pressAnimation.getValue();
        float indicatorProgress = indicatorAnimation.getValue();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), MD3Theme.CARD_RADIUS, MD3Theme.lerp(MD3Theme.SURFACE_CONTAINER, MD3Theme.SURFACE_CONTAINER_HIGH, animatedHover));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + MD3Theme.ROW_CONTENT_INSET, labelY, labelScale, MD3Theme.TEXT_PRIMARY);

        PanelLayout.Rect trackBounds = getTrackBounds(bounds);
        PanelLayout.Rect fieldBounds = getFieldBounds(bounds);
        float progress = getProgress();
        float handleWidth = 4.0f - animatedPress * 2.0f;
        float handleHeight = 14.0f;
        float handleX = trackBounds.x() + trackBounds.width() * progress - handleWidth / 2.0f;
        float handleY = trackBounds.centerY() - handleHeight / 2.0f;
        float handleGap = 4.0f;
        float activeWidth = Math.max(2.0f, trackBounds.width() * progress - handleWidth / 2.0f - handleGap);
        float inactiveX = handleX + handleWidth + handleGap;
        float inactiveWidth = Math.max(2.0f, trackBounds.right() - inactiveX);

        if (shouldDrawSteps()) {
            drawSteps(rectRenderer, trackBounds, progress);
        }

        roundRectRenderer.addRoundRect(trackBounds.x(), trackBounds.y(), activeWidth, trackBounds.height(), 3.0f, 0.0f, 0.0f, 3.0f, MD3Theme.PRIMARY);
        roundRectRenderer.addRoundRect(inactiveX, trackBounds.y(), inactiveWidth, trackBounds.height(), 0.0f, 3.0f, 3.0f, 0.0f, MD3Theme.SECONDARY_CONTAINER);
        roundRectRenderer.addRoundRect(handleX, handleY, handleWidth, handleHeight, 2.0f, MD3Theme.PRIMARY);

        if (indicatorProgress > 0.01f) {
            String label = formatValue();
            float textScale = 0.62f;
            float bubbleWidth = textRenderer.getWidth(label, textScale) + 16.0f;
            float bubbleHeight = 18.0f;
            float bubbleX = handleX + handleWidth / 2.0f - bubbleWidth / 2.0f;
            float bubbleY = bounds.y() - 22.0f;
            int bubbleAlpha = (int) (255 * indicatorProgress);
            roundRectRenderer.addRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 9.0f, MD3Theme.withAlpha(MD3Theme.INVERSE_SURFACE, bubbleAlpha));
            float textWidth = textRenderer.getWidth(label, textScale);
            float textHeight = textRenderer.getHeight(textScale);
            float textX = bubbleX + (bubbleWidth - textWidth) / 2.0f;
            float textY = bubbleY + (bubbleHeight - textHeight) / 2.0f - 1.0f;
            textRenderer.addText(label, textX, textY, textScale, MD3Theme.withAlpha(MD3Theme.INVERSE_ON_SURFACE, bubbleAlpha));
        }

        Color fieldBase = MD3Theme.isLightTheme() ? MD3Theme.SURFACE_CONTAINER : MD3Theme.SURFACE_CONTAINER_LOW;
        Color fieldHover = MD3Theme.isLightTheme() ? MD3Theme.SURFACE_CONTAINER_HIGHEST : MD3Theme.SURFACE_CONTAINER_HIGHEST;
        Color fieldColor = focused
                ? MD3Theme.INVERSE_SURFACE
                : MD3Theme.lerp(fieldBase, fieldHover, animatedHover * 0.85f);
        Color fieldTextColor = focused ? MD3Theme.INVERSE_ON_SURFACE : MD3Theme.TEXT_PRIMARY;
        roundRectRenderer.addRoundRect(fieldBounds.x(), fieldBounds.y(), fieldBounds.width(), fieldBounds.height(), 7.0f, fieldColor);

        String display = focused ? getDisplayBuffer() : formatValue();
        float displayScale = 0.60f;
        float textWidth = textRenderer.getWidth(display, displayScale);
        float textHeight = textRenderer.getHeight(displayScale);
        float textX = fieldBounds.x() + (fieldBounds.width() - textWidth) / 2.0f;
        float textY = fieldBounds.y() + (fieldBounds.height() - textHeight) / 2.0f - 1.0f;
        textRenderer.addText(display, textX, textY, displayScale, fieldTextColor);
        if (focused) {
            float caretX = textX + textRenderer.getWidth(display.substring(0, Math.min(cursorIndex, display.length())), displayScale);
            rectRenderer.addRect(caretX, fieldBounds.y() + 4.0f, 1.0f, fieldBounds.height() - 8.0f, MD3Theme.INVERSE_ON_SURFACE);
        }
    }

    public PanelLayout.Rect getTrackBounds(PanelLayout.Rect bounds) {
        return new PanelLayout.Rect(bounds.right() - MD3Theme.ROW_TRAILING_INSET - 116.0f, bounds.y() + 12.0f, 72.0f, 6.0f);
    }

    public PanelLayout.Rect getFieldBounds(PanelLayout.Rect bounds) {
        return new PanelLayout.Rect(bounds.right() - MD3Theme.ROW_TRAILING_INSET - 40.0f, bounds.y() + 4.0f, 40.0f, 18.0f);
    }

    @Override
    public boolean mouseClicked(PanelLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        PanelLayout.Rect fieldBounds = getFieldBounds(bounds);
        if (event.button() == 0 && fieldBounds.contains(event.x(), event.y())) {
            dragging = false;
            focused = true;
            inputBuffer = formatPlainValue();
            cursorIndex = getCursorIndex(event.x(), fieldBounds);
            return true;
        }
        if (event.button() != 0 || !getInteractiveBounds(bounds).contains(event.x(), event.y())) {
            return false;
        }
        focused = false;
        dragging = true;
        updateFromMouse(bounds, event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(PanelLayout.Rect bounds, MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!focused) {
            return false;
        }
        return switch (event.key()) {
            case 257, 335 -> {
                commitInput();
                focused = false;
                yield true;
            }
            case 256 -> {
                focused = false;
                inputBuffer = null;
                yield true;
            }
            case 259 -> {
                if (inputBuffer != null && cursorIndex > 0) {
                    inputBuffer = inputBuffer.substring(0, cursorIndex - 1) + inputBuffer.substring(cursorIndex);
                    cursorIndex--;
                }
                yield true;
            }
            case 261 -> {
                if (inputBuffer != null && cursorIndex < inputBuffer.length()) {
                    inputBuffer = inputBuffer.substring(0, cursorIndex) + inputBuffer.substring(cursorIndex + 1);
                }
                yield true;
            }
            case 263 -> {
                cursorIndex = Math.max(0, cursorIndex - 1);
                yield true;
            }
            case 262 -> {
                cursorIndex = Math.min(getDisplayBuffer().length(), cursorIndex + 1);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) {
            return false;
        }
        String value = event.codepointAsString();
        if (!value.matches("[0-9-]")) {
            return false;
        }
        String current = getDisplayBuffer();
        inputBuffer = current.substring(0, cursorIndex) + value + current.substring(cursorIndex);
        cursorIndex++;
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused && this.focused) {
            commitInput();
            inputBuffer = null;
        }
        this.focused = focused;
        if (focused && inputBuffer == null) {
            inputBuffer = formatPlainValue();
            cursorIndex = inputBuffer.length();
        }
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean hasActiveAnimation() {
        return !hoverAnimation.isFinished() || !pressAnimation.isFinished() || !indicatorAnimation.isFinished();
    }

    public void updateFromMouse(PanelLayout.Rect bounds, double mouseX) {
        PanelLayout.Rect trackBounds = getTrackBounds(bounds);
        double progress = (mouseX - trackBounds.x()) / trackBounds.width();
        progress = Math.max(0.0, Math.min(1.0, progress));
        double rawValue = setting.getMin() + (setting.getMax() - setting.getMin()) * progress;
        int step = Math.max(1, setting.getStep());
        int snapped = setting.getMin() + (int) Math.round((rawValue - setting.getMin()) / step) * step;
        setting.setValue(snapped);
    }

    public boolean isDragging() {
        return dragging;
    }

    private PanelLayout.Rect getInteractiveBounds(PanelLayout.Rect bounds) {
        PanelLayout.Rect track = getTrackBounds(bounds);
        return new PanelLayout.Rect(track.x(), track.y() - 6.0f, track.width(), track.height() + 12.0f);
    }

    private float getProgress() {
        if (setting.getMax() <= setting.getMin()) {
            return 0.0f;
        }
        return (float) ((setting.getValue() - setting.getMin()) / (double) (setting.getMax() - setting.getMin()));
    }

    private boolean shouldDrawSteps() {
        int step = Math.max(1, setting.getStep());
        int range = setting.getMax() - setting.getMin();
        return range > 0 && (double) step / (double) range > 0.08;
    }

    private void drawSteps(RectRenderer rectRenderer, PanelLayout.Rect trackBounds, float progress) {
        int step = Math.max(1, setting.getStep());
        int range = setting.getMax() - setting.getMin();
        int steps = Math.max(1, range / step);
        float dotSize = 1.5f;
        for (int i = 0; i <= steps; i++) {
            float stepProgress = i / (float) steps;
            if (Math.abs(stepProgress - progress) < (1.0f / steps) * 0.5f) {
                continue;
            }
            float x = trackBounds.x() + trackBounds.width() * stepProgress - dotSize / 2.0f;
            x = Mth.clamp(x, trackBounds.x(), trackBounds.right() - dotSize);
            float y = trackBounds.centerY() - dotSize / 2.0f;
            rectRenderer.addRect(x, y, dotSize, dotSize, stepProgress <= progress ? MD3Theme.ON_PRIMARY : MD3Theme.ON_SECONDARY_CONTAINER);
        }
    }

    private String formatValue() {
        return setting.isPercentageMode() ? setting.getValue() + "%" : Integer.toString(setting.getValue());
    }

    private String formatPlainValue() {
        return Integer.toString(setting.getValue());
    }

    private String getDisplayBuffer() {
        return inputBuffer == null ? formatPlainValue() : inputBuffer;
    }

    private void commitInput() {
        if (inputBuffer == null || inputBuffer.isBlank() || "-".equals(inputBuffer)) {
            inputBuffer = formatPlainValue();
            cursorIndex = inputBuffer.length();
            return;
        }
        try {
            setting.setValue(Integer.parseInt(inputBuffer));
        } catch (NumberFormatException ignored) {
        }
        inputBuffer = formatPlainValue();
        cursorIndex = inputBuffer.length();
    }

    private int getCursorIndex(double mouseX, PanelLayout.Rect fieldBounds) {
        String text = getDisplayBuffer();
        float scale = 0.60f;
        float textWidth = measureTextRenderer.getWidth(text, scale);
        float textStart = fieldBounds.x() + (fieldBounds.width() - textWidth) / 2.0f;
        for (int i = 0; i <= text.length(); i++) {
            float width = measureTextRenderer.getWidth(text.substring(0, i), scale);
            if (mouseX <= textStart + width) {
                return i;
            }
        }
        return text.length();
    }

}
