package com.github.lumin.gui.dropdown;

public class DropdownAnimations {

    public float approach(float current, float target, float speed) {
        if (Math.abs(target - current) <= 0.0001f) {
            return target;
        }
        float step = Math.max(0.001f, speed);
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    public float easeOutCubic(float progress) {
        float clamped = Math.max(0.0f, Math.min(1.0f, progress));
        float inverse = 1.0f - clamped;
        return 1.0f - inverse * inverse * inverse;
    }
}
