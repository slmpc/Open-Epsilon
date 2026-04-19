package com.github.epsilon.managers;

import com.github.epsilon.gui.hudeditor.HudEditorScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.function.Consumer;

public class RenderManager {

    public static final RenderManager INSTANCE = new RenderManager();

    private final ArrayList<Consumer<DeltaTracker>> renderAfterFrameQueue = new ArrayList<>();
    private final ArrayList<Consumer<DeltaTracker>> renderHudQueue = new ArrayList<>();
    private final ArrayList<Consumer<DeltaTracker>> renderAfterWorldQueue = new ArrayList<>();

    private RenderManager() {
    }

    public void applyRenderAfterWorld(Runnable func) {
        renderAfterWorldQueue.add(_ -> func.run());
    }

    public void applyRenderAfterWorld(Consumer<DeltaTracker> func) {
        renderAfterWorldQueue.add(func);
    }

    public void applyRenderHud(Runnable func) {
        renderHudQueue.add(_ -> func.run());
    }

    public void applyRenderHud(Consumer<DeltaTracker> func) {
        renderHudQueue.add(func);
    }

    public void applyRender(Consumer<DeltaTracker> func) {
        func.accept(Minecraft.getInstance().getDeltaTracker());
    }

    public void applyRender(Runnable func) {
        func.run();
    }

    public void applyRenderAfterFrame(Consumer<DeltaTracker> func) {
        renderAfterFrameQueue.add(func);
    }

    public void applyRenderAfterFrame(Runnable func) {
        renderAfterFrameQueue.add(_ -> func.run());
    }

    public void callAfterFrame(DeltaTracker tracker) {
        if (!renderAfterFrameQueue.isEmpty()) {
            renderAfterFrameQueue.forEach(func -> func.accept(tracker));
        }
    }

    public void callInGameGui(DeltaTracker tracker) {
        final var screen = Minecraft.getInstance().gui.screen();

        if (!renderAfterWorldQueue.isEmpty()) {
            ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderAfterWorldQueue);
            pending.forEach(func -> func.accept(tracker));
        }

        if (!renderHudQueue.isEmpty() && screen != HudEditorScreen.INSTANCE) {
            ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderHudQueue);
            pending.forEach(func -> func.accept(tracker));
        }
    }

    public void clear() {
        renderAfterFrameQueue.clear();
        renderHudQueue.clear();
        renderAfterWorldQueue.clear();
    }

}
