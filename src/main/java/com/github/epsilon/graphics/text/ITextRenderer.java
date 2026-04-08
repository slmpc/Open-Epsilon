package com.github.epsilon.graphics.text;

import com.github.epsilon.graphics.text.ttf.TtfFontLoader;

import java.awt.*;

public interface ITextRenderer {

    void addText(String text, float x, float y, float scale, Color color, TtfFontLoader fontLoader);

    void draw();

    void clear();

    void close();

    float getHeight(float scale, TtfFontLoader fontLoader);

    float getLineHeight(float scale, TtfFontLoader fontLoader);

    float getWidth(String text, float scale, TtfFontLoader fontLoader);

    default void setScissor(int x, int y, int width, int height) {
    }

    default void clearScissor() {
    }

}
