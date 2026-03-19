package com.github.lumin.utils.tasks;

import com.github.lumin.Lumin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

@EventBusSubscriber(modid = Lumin.MODID, value = Dist.CLIENT)
public final class TaskScheduler {

    private static final Queue<ScheduledTask> TASKS = new ArrayDeque<>();

    private TaskScheduler() {
    }

    public static void schedule(Runnable runnable, int delayTicks) {
        if (runnable == null) return;
        TASKS.add(new ScheduledTask(runnable, Math.max(0, delayTicks)));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (TASKS.isEmpty()) return;
        Iterator<ScheduledTask> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            if (task.ticks-- <= 0) {
                try {
                    task.runnable.run();
                } catch (Exception ignored) {
                }
                iterator.remove();
            }
        }
    }

    private static final class ScheduledTask {
        private final Runnable runnable;
        private int ticks;

        private ScheduledTask(Runnable runnable, int ticks) {
            this.runnable = runnable;
            this.ticks = ticks;
        }
    }
}
