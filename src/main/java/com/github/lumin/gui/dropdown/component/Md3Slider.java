package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

public class Md3Slider {

    private final DropdownLayout.Rect bounds;
    private double value;
    private final double min;
    private final double max;

    public Md3Slider(DropdownLayout.Rect bounds, double value, double min, double max) {
        this.bounds = bounds;
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}
