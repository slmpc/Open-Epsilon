package com.github.epsilon.modules.impl.render.notification;

import com.github.epsilon.utils.render.animation.Animation;
import com.github.epsilon.utils.render.animation.Easing;

public class Notification {

    private final int hashCode;
    private String title;
    private String subTitle;
    private NotificationMode mode;
    private long createTime;
    private int displayDuration;
    private final boolean isModule;
    private boolean skipIntroAnimation = false;

    private float currentY;
    private float targetY;

    private final Animation yAnimation;

    public Notification(int hashCode, String title, String subTitle, NotificationMode mode, int displayTime, float initialY, boolean isModule) {
        this.hashCode = hashCode;
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

    public Notification(String title, String subTitle, NotificationMode mode, int displayTime, float initialY, boolean isModule) {
        this(0, title, subTitle, mode, displayTime, initialY, isModule);
    }

    public void update() {
        if (yAnimation != null) {
            yAnimation.run(targetY);
            currentY = yAnimation.getValue();
        }
    }

    public void updateModuleState(String newTitle, String newSubTitle, NotificationMode newMode, int newDisplayDuration) {
        this.title = newTitle;
        this.subTitle = newSubTitle;
        this.mode = newMode;
        this.displayDuration = newDisplayDuration;
        this.createTime = System.currentTimeMillis();
        this.skipIntroAnimation = true;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createTime > displayDuration + 500L;
    }

    public boolean isExiting() {
        return System.currentTimeMillis() - createTime > displayDuration;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public NotificationMode getMode() {
        return mode;
    }

    public int getDisplayDuration() {
        return displayDuration;
    }

    public float getCurrentY() {
        return currentY;
    }

    public boolean isModule() {
        return isModule;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public Animation getYAnimation() {
        return yAnimation;
    }

    public int getHashCode() {
        return hashCode;
    }

    public boolean shouldSkipIntroAnimation() {
        return skipIntroAnimation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Notification that = (Notification) obj;
        return hashCode == that.hashCode;
    }

}
