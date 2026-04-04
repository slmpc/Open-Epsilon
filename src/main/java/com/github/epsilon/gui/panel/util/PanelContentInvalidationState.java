package com.github.epsilon.gui.panel.util;

import com.github.epsilon.gui.panel.PanelLayout;

public final class PanelContentInvalidationState {

    private boolean dirty = true;
    private boolean hasActiveAnimations;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private int lastGuiHeight = -1;
    private PanelLayout.Rect lastBounds;

    public void markDirty() {
        dirty = true;
    }

    public void beginRebuild() {
        hasActiveAnimations = false;
    }

    public void noteAnimation(boolean active) {
        hasActiveAnimations = hasActiveAnimations || active;
    }

    public boolean hasActiveAnimations() {
        return hasActiveAnimations;
    }

    public boolean needsRebuild(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight) {
        return dirty
                || hasActiveAnimations
                || lastBounds == null
                || !lastBounds.equals(bounds)
                || lastGuiHeight != guiHeight
                || lastMouseX != mouseX
                || lastMouseY != mouseY;
    }

    public void rememberSnapshot(PanelLayout.Rect bounds, int mouseX, int mouseY, int guiHeight) {
        lastBounds = bounds;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastGuiHeight = guiHeight;
        dirty = false;
    }

}
