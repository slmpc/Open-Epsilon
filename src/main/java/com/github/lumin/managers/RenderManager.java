package com.github.lumin.managers;

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
	
	public void callAndClear(DeltaTracker t) {
		renderQueue.forEach((func) -> {
			func.accept(t);
		});
		renderQueue.clear();
	}
	
}
