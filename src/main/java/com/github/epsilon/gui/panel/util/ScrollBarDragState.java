package com.github.epsilon.gui.panel.util;

import com.github.epsilon.gui.panel.PanelLayout;

/**
 * Encapsulates scroll bar drag state for a single scroll region.
 * Once dragging starts, it continues regardless of whether the cursor
 * is still within the scroll bar bounds until the mouse is released.
 */
public final class ScrollBarDragState {

    private boolean dragging;
    private float dragOffset;

    public boolean isDragging() {
        return dragging;
    }

    /**
     * Try to begin a scrollbar drag. Returns true if the click was on the
     * scrollbar track (thumb or empty track area).
     */
    public boolean mouseClicked(double mouseX, double mouseY, PanelLayout.Rect viewport,
                                float scroll, float maxScroll) {
        if (maxScroll <= 0) {
            return false;
        }
        float contentHeight = maxScroll + viewport.height();
        ScrollBarUtil.ThumbGeometry thumb = ScrollBarUtil.computeThumb(viewport, scroll, maxScroll, contentHeight);
        if (thumb == null) {
            return false;
        }
        if (!thumb.trackContains(mouseX, mouseY)) {
            return false;
        }
        if (thumb.thumbContains(mouseX, mouseY)) {
            dragging = true;
            dragOffset = (float) mouseY - thumb.thumbY();
            return true;
        }
        // Clicked on the track but not the thumb — center thumb on click position
        dragging = true;
        dragOffset = thumb.thumbHeight() / 2.0f;
        return true;
    }

    /**
     * Update scroll based on the current mouse Y during a drag.
     * Returns the new absolute scroll value, or -1 if not dragging.
     */
    public float mouseDragged(double mouseY, PanelLayout.Rect viewport, float maxScroll) {
        if (!dragging || maxScroll <= 0) {
            return -1;
        }
        float contentHeight = maxScroll + viewport.height();
        return ScrollBarUtil.scrollFromMouseY((float) mouseY - dragOffset, viewport, maxScroll, contentHeight);
    }

    /**
     * End dragging. Returns true if a drag was active.
     */
    public boolean mouseReleased() {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public void reset() {
        dragging = false;
    }
}

