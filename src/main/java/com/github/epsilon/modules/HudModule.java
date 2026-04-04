package com.github.epsilon.modules;

import net.minecraft.client.DeltaTracker;

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

    abstract public void render(DeltaTracker deltaTracker);

}
