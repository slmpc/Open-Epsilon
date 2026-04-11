package com.github.epsilon.modules.impl.render.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.*;

public class NotificationManager {

    public static final NotificationManager INSTANCE = new NotificationManager();

    private static final int MAX_NOTIFICATIONS = 5;

    private final Queue<Notification> notifications = new ArrayDeque<>();
    private final Map<Integer, Notification> hashCodeMap = new HashMap<>();

    public void post(String title, String subTitle, NotificationMode mode, int displayTime) {
        makeRoomIfNeeded();
        Notification notification = new Notification(title, subTitle, mode, displayTime, getScreenHeight(), false);
        notifications.add(notification);
    }

    public void postModuleNotification(String moduleName, boolean enabled, int displayTime) {
        int hashCode = moduleName.hashCode();

        // 检查是否已存在相同模块的通知（且不在退出动画中）
        Notification existing = hashCodeMap.get(hashCode);
        if (existing != null && !existing.isExiting()) {
            // 已存在且未在退出，更新状态
            String newTitle = Component.translatable("epsilon.modules.notifications." + (enabled ? "enabled" : "disabled")).getString();
            NotificationMode mode = enabled ? NotificationMode.Success : NotificationMode.Error;
            existing.updateModuleState(newTitle, moduleName, mode, displayTime);
            return;
        }

        // 不存在，创建新的
        makeRoomIfNeeded();
        String title = Component.translatable("epsilon.modules.notifications." + (enabled ? "enabled" : "disabled")).getString();
        NotificationMode mode = enabled ? NotificationMode.Success : NotificationMode.Error;
        Notification notification = new Notification(hashCode, title, moduleName, mode, displayTime, getScreenHeight(), true);
        notifications.add(notification);
        hashCodeMap.put(hashCode, notification);
    }

    public void update() {
        Iterator<Notification> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            notification.update();
            if (notification.isExpired()) {
                iterator.remove();
                hashCodeMap.remove(notification.getHashCode());
            }
        }
    }

    public Queue<Notification> getNotifications() {
        return notifications;
    }

    public boolean isEmpty() {
        return notifications.isEmpty();
    }

    public void clear() {
        notifications.clear();
        hashCodeMap.clear();
    }

    private void makeRoomIfNeeded() {
        if (notifications.size() >= MAX_NOTIFICATIONS) {
            Notification oldest = notifications.poll();
            if (oldest != null) {
                hashCodeMap.remove(oldest.getHashCode());
            }
        }
    }

    private float getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

}
