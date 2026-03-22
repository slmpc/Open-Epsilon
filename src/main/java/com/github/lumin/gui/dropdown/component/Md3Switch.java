package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3Switch {

    private final DropdownLayout.Rect bounds;
    private boolean checked;

    public Md3Switch(DropdownLayout.Rect bounds, boolean checked) {
        this.bounds = bounds;
        this.checked = checked;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
