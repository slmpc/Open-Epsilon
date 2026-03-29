package com.github.epsilon.gui.panel.util;

import com.github.epsilon.graphics.renderers.RoundRectRenderer;
import com.github.epsilon.gui.panel.MD3Theme;
import com.github.epsilon.gui.panel.PanelLayout;

public final class ScrollBarUtil {

    private static final float WIDTH = 3.0f;
    private static final float PADDING = 2.0f;
    private static final float MIN_THUMB_HEIGHT = 12.0f;

    /** Total horizontal space the scrollbar occupies (width + padding on each side). */
    public static final float TOTAL_WIDTH = WIDTH + PADDING * 2;

    private ScrollBarUtil() {
    }

    public static void draw(RoundRectRenderer renderer, PanelLayout.Rect viewport, float scroll, float maxScroll, float contentHeight) {
        if (maxScroll <= 0 || contentHeight <= viewport.height()) {
            return;
        }
        float trackHeight = viewport.height() - PADDING * 2;
        float thumbHeight = Math.max(MIN_THUMB_HEIGHT, (viewport.height() / contentHeight) * trackHeight);
        float thumbTravel = trackHeight - thumbHeight;
        float scrollRatio = maxScroll > 0 ? scroll / maxScroll : 0;
        float thumbY = viewport.y() + PADDING + scrollRatio * thumbTravel;
        float thumbX = viewport.right() - WIDTH - PADDING;
        renderer.addRoundRect(thumbX, thumbY, WIDTH, thumbHeight, WIDTH / 2.0f, MD3Theme.withAlpha(MD3Theme.OUTLINE, 80));
    }

}

