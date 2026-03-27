package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class KeyBindPopup implements DropdownPopupHost.Popup {

    private final DropdownLayout.Rect bounds;
    private final Module module;

    public KeyBindPopup(DropdownLayout.Rect bounds, Module module) {
        this.bounds = bounds;
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    @Override
    public DropdownLayout.Rect getBounds() {
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
