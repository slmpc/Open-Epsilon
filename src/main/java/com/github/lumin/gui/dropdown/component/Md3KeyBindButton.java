package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3KeyBindButton {

    private final DropdownLayout.Rect bounds;
    private String label;

    public Md3KeyBindButton(DropdownLayout.Rect bounds, String label) {
        this.bounds = bounds;
        this.label = label;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
