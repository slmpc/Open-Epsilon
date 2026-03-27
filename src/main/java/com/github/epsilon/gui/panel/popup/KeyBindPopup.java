package com.github.epsilon.gui.panel.popup;

import com.github.epsilon.gui.panel.PanelLayout;
import com.github.epsilon.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class KeyBindPopup implements PanelPopupHost.Popup {

    private final PanelLayout.Rect bounds;
    private final Module module;

    public KeyBindPopup(PanelLayout.Rect bounds, Module module) {
        this.bounds = bounds;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public PanelLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void extractGui(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return false;
    }
}
