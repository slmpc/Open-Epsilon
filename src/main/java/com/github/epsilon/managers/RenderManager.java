package com.github.epsilon.managers;

import java.util.ArrayList;
import java.util.function.Consumer;

import net.minecraft.client.DeltaTracker;

public class RenderManager {

	public static final RenderManager INSTANCE = new RenderManager();
	
	private final ArrayList<Consumer<DeltaTracker>> renderQueue = new ArrayList<>();
	
	private RenderManager() {
	}
	
	public void applyRenderAfterFrame(Consumer<DeltaTracker> func) {
		renderQueue.add(func);
	}

	public void applyRenderAfterFrame(Runnable func) {
		renderQueue.add((delta) -> func.run());
	}
	
	public void callAndClear(DeltaTracker t) {
		if (renderQueue.isEmpty()) {
			return;
		}
		ArrayList<Consumer<DeltaTracker>> pending = new ArrayList<>(renderQueue);
		renderQueue.clear();
		pending.forEach((func) -> func.accept(t));
	}
	
}
