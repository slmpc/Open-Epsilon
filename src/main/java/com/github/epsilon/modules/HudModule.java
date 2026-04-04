package com.github.epsilon.modules;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public abstract class HudModule extends Module {

    public float x, y, width, height;

    public HudModule(String name, Category category) {
        this(name, category, 0f, 0f, 20f, 20f);
    }

    public HudModule(String name, Category category, float width, float height) {
        this(name, category, 0f, 0f, width, height);
    }

    public HudModule(String name, Category category, float x, float y, float width, float height) {
        super(name, category);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateBounds(DeltaTracker deltaTracker) {
    }

    protected final void setBounds(float width, float height) {
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
    }

    public final boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public final void moveTo(float x, float y) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        this.x = clamp(x, 0.0f, Math.max(0.0f, screenWidth - width));
        this.y = clamp(y, 0.0f, Math.max(0.0f, screenHeight - height));
    }

    public final void moveBy(float deltaX, float deltaY) {
        moveTo(x + deltaX, y + deltaY);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    abstract public void render(DeltaTracker deltaTracker);

}
