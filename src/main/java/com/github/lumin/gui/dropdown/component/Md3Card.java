package com.github.lumin.gui.dropdown.component;

import com.github.lumin.gui.dropdown.DropdownLayout;

import java.awt.*;

public class Md3Card {

    private DropdownLayout.Rect bounds;
    private Color color;
    private float radius;

    public Md3Card(DropdownLayout.Rect bounds, Color color, float radius) {
        this.bounds = bounds;
        this.color = color;
        this.radius = radius;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public Color getColor() {
        return color;
    }

    public float getRadius() {
        return radius;
    }
}
