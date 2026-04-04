package com.github.epsilon.managers;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.function.Consumer;

public class RenderManager {

    public static final RenderManager INSTANCE = new RenderManager();

    private final ArrayList<Consumer<DeltaTracker>> renderGuiQueue = new ArrayList<>();
    private final ArrayList<Consumer<DeltaTracker>> renderHudQueue = new ArrayList<>();
    private final ArrayList<Consumer<DeltaTracker>> renderWorldHudQueue = new ArrayList<>();

    private RenderManager() {
    }

    public void applyRenderWorldHud(Runnable func) {
        renderWorldHudQueue.add(_ -> func.run());
    }

    public void applyRenderWorldHud(Consumer<DeltaTracker> func) {
        renderWorldHudQueue.add(func);
    }

    public void applyRenderHud(Runnable func) {
        renderHudQueue.add(_ -> func.run());
    }

    public void applyRenderHud(Consumer<DeltaTracker> func) {
        renderHudQueue.add(func);
    }

    public void applyRenderAfterFrame(Consumer<DeltaTracker> func) {
        renderGuiQueue.add(func);
    }

    public void applyRenderAfterFrame(Runnable func) {
        renderGuiQueue.add(_ -> func.run());
    }

    public void callAndClear(DeltaTracker tracker) {
        final var screen = Minecraft.getInstance().screen;

        if (!renderWorldHudQueue.isEmpty() && screen == null) {
            ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderWorldHudQueue);
            pending.forEach(func -> func.accept(tracker));
        }

        if (!renderHudQueue.isEmpty() && screen == null) {
            ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderHudQueue);
            pending.forEach(func -> func.accept(tracker));
        }

        if (!renderGuiQueue.isEmpty()) {
            ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderGuiQueue);
            pending.forEach(func -> func.accept(tracker));
        }

        renderWorldHudQueue.clear();
        renderHudQueue.clear();
        renderGuiQueue.clear();
    }

}
