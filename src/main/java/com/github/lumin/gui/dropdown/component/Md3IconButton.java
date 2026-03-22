package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3IconButton {

    private final DropdownLayout.Rect bounds;
    private final String icon;

    public Md3IconButton(DropdownLayout.Rect bounds, String icon) {
        this.bounds = bounds;
        this.icon = icon;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public String getIcon() {
        return icon;
    }
}
