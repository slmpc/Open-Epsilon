package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3SearchField {

    private final DropdownLayout.Rect bounds;
    private String text = "";

    public Md3SearchField(DropdownLayout.Rect bounds) {
        this.bounds = bounds;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
