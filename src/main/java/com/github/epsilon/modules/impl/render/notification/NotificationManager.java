package com.github.epsilon.modules.impl.render.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();
    private static final int MAX_NOTIFICATIONS = 5;

    private final Queue<Notification> notifications = new ArrayDeque<>();

    public void post(String title, String subTitle, NotificationMode mode, int displayTime) {
        makeRoomIfNeeded();
        notifications.add(new Notification(title, subTitle, mode, displayTime, getScreenHeight(), false));
    }

    public void postModuleNotification(String moduleName, boolean enabled, int displayTime) {
        makeRoomIfNeeded();
        String title = Component.translatable("epsilon.modules.notifications." + (enabled ? "enabled" : "disabled")).getString();
        NotificationMode mode = enabled ? NotificationMode.Success : NotificationMode.Error;
        notifications.add(new Notification(title, moduleName, mode, displayTime, getScreenHeight(), true));
    }

    public void update() {
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            notification.update();
            if (notification.isExpired()) {
                iterator.remove();
            }
        }
    }

    public Queue<Notification> getNotifications() { return notifications; }
    public boolean isEmpty() { return notifications.isEmpty(); }
    public void clear() { notifications.clear(); }

    private void makeRoomIfNeeded() {
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            Notification oldest = notifications.peek();
            if (oldest != null) {
                notifications.poll();
            }
        }
    }

    private float getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }
}
