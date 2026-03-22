package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.settings.impl.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class ColorPickerPopup implements DropdownPopupHost.Popup {

    private final DropdownLayout.Rect bounds;
    private final ColorSetting setting;

    public ColorPickerPopup(DropdownLayout.Rect bounds, ColorSetting setting) {
        this.bounds = bounds;
        this.setting = setting;
    }

    public ColorSetting getSetting() {
        return setting;
    }

    @Override
    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y());
    }
}
