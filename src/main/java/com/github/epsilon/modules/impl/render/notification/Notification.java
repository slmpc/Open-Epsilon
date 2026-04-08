package com.github.epsilon.modules.impl.render.notification;

import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;

public class Notification {

    private final String title;
    private final String subTitle;
    private final NotificationMode mode;
    private final long createTime;
    private final int displayDuration;
    private final boolean isModule;

    private Animation yAnimation;
    private float currentY;
    private float targetY;

    public Notification(String title, String subTitle, NotificationMode mode, int displayTime, float initialY, boolean isModule) {
        this.title = title;
        this.subTitle = subTitle;
        this.mode = mode;
        this.createTime = System.currentTimeMillis();
        this.displayDuration = displayTime;
        this.isModule = isModule;
        this.yAnimation = new Animation(Easing.EASE_OUT_EXPO, 300L);
        this.yAnimation.setStartValue(initialY);
        this.currentY = initialY;
        this.targetY = initialY;
    }

    public void update() {
        if (yAnimation != null) {
            yAnimation.run(targetY);
            currentY = yAnimation.getValue();
        }
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createTime > displayDuration + 500L;
    }

    public String getTitle() { return title; }
    public String getSubTitle() { return subTitle; }
    public NotificationMode getMode() { return mode; }
    public int getDisplayDuration() { return displayDuration; }
    public float getCurrentY() { return currentY; }
    public boolean isModule() { return isModule; }
    public long getCreateTime() { return createTime; }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public Animation getYAnimation() {
        return yAnimation;
    }
}
