package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3Chip {

    private final DropdownLayout.Rect bounds;
    private final String label;
    private boolean selected;

    public Md3Chip(DropdownLayout.Rect bounds, String label) {
        this.bounds = bounds;
        this.label = label;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
