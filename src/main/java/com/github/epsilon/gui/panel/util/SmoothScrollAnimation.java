package com.github.epsilon.gui.panel.util;

public final class SmoothScrollAnimation {

    private static final float SCROLL_SPEED = 0.12f;
    private static final float MOMENTUM_FRICTION = 0.935f;
    private static final float BOUNCE_STIFFNESS = 0.08f;
    private static final float BOUNCE_DAMPING = 0.7f;
    private static final float VELOCITY_THRESHOLD = 0.3f;
    private static final float SETTLE_THRESHOLD = 0.01f;
    private static final float MAX_OVERSCROLL = 60.0f;
    private static final float OVERSCROLL_RESISTANCE = 0.4f;

    private float currentScroll;
    private float targetScroll;
    private float velocity;
    private float overscroll;
    private float bounceVelocity;
    private boolean isDragging;
    private boolean hasMomentum;
    private long lastUpdateTime;

    public SmoothScrollAnimation() {
        this.currentScroll = 0.0f;
        this.targetScroll = 0.0f;
        this.velocity = 0.0f;
        this.overscroll = 0.0f;
        this.bounceVelocity = 0.0f;
        this.isDragging = false;
        this.hasMomentum = false;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public float getCurrentScroll() {
        return currentScroll;
    }

    public void setCurrentScroll(float scroll) {
        this.currentScroll = scroll;
        this.targetScroll = scroll;
        this.velocity = 0.0f;
        this.overscroll = 0.0f;
        this.bounceVelocity = 0.0f;
        this.hasMomentum = false;
    }

    public void startDrag() {
        this.isDragging = true;
        this.hasMomentum = false;
        this.velocity = 0.0f;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void updateDrag(float delta, float maxScroll) {
        if (!isDragging) return;

        long currentTime = System.currentTimeMillis();
        float dt = Math.max(1.0f, (currentTime - lastUpdateTime) / 16.667f);
        lastUpdateTime = currentTime;

        float previousScroll = currentScroll;
        float adjustedDelta = delta;

        if (currentScroll < 0 || currentScroll > maxScroll) {
            adjustedDelta *= OVERSCROLL_RESISTANCE;
        }

        currentScroll -= adjustedDelta;

        velocity = (currentScroll - previousScroll) / dt;

        if (currentScroll < 0) {
            overscroll = Math.min(-currentScroll, MAX_OVERSCROLL);
            currentScroll = -overscroll;
        } else if (currentScroll > maxScroll) {
            overscroll = Math.min(currentScroll - maxScroll, MAX_OVERSCROLL);
            currentScroll = maxScroll + overscroll;
        } else {
            overscroll = 0;
        }
    }

    public void endDrag(float maxScroll) {
        this.isDragging = false;

        if (overscroll > SETTLE_THRESHOLD) {
            hasMomentum = false;
        } else if (Math.abs(velocity) > VELOCITY_THRESHOLD) {
            hasMomentum = true;
        } else {
            velocity = 0.0f;
            hasMomentum = false;
        }
    }

    public void addImpulse(float delta, float maxScroll) {
        if (isDragging) return;

        float adjustedDelta = delta;

        if (currentScroll < 0 || currentScroll > maxScroll) {
            adjustedDelta *= OVERSCROLL_RESISTANCE;
        }

        targetScroll = currentScroll - adjustedDelta;
        targetScroll = Math.max(-MAX_OVERSCROLL, Math.min(targetScroll, maxScroll + MAX_OVERSCROLL));

        velocity += (targetScroll - currentScroll) * SCROLL_SPEED;
        hasMomentum = true;
        lastUpdateTime = System.currentTimeMillis();
    }

    public boolean update(float maxScroll) {
        if (isDragging) return false;

        long currentTime = System.currentTimeMillis();
        float dt = Math.max(1.0f, (currentTime - lastUpdateTime) / 16.667f);
        lastUpdateTime = currentTime;

        boolean changed = false;

        if (overscroll > SETTLE_THRESHOLD) {
            changed = updateBounce(maxScroll, dt);
        } else if (hasMomentum && Math.abs(velocity) > VELOCITY_THRESHOLD) {
            changed = updateMomentum(maxScroll, dt);
        } else {
            velocity = 0.0f;
            hasMomentum = false;
            if (currentScroll < 0) {
                currentScroll = 0;
                overscroll = 0;
            } else if (currentScroll > maxScroll) {
                currentScroll = maxScroll;
                overscroll = 0;
            }
        }

        return changed;
    }

    private boolean updateMomentum(float maxScroll, float dt) {
        velocity *= (float) Math.pow(MOMENTUM_FRICTION, dt);

        currentScroll += velocity * dt;

        if (currentScroll < 0) {
            overscroll = Math.min(-currentScroll, MAX_OVERSCROLL);
            currentScroll = -overscroll;
            velocity *= -0.2f;
            hasMomentum = false;
        } else if (currentScroll > maxScroll) {
            overscroll = Math.min(currentScroll - maxScroll, MAX_OVERSCROLL);
            currentScroll = maxScroll + overscroll;
            velocity *= -0.2f;
            hasMomentum = false;
        } else {
            overscroll = 0;
        }

        if (Math.abs(velocity) < VELOCITY_THRESHOLD) {
            velocity = 0.0f;
            hasMomentum = false;
            return overscroll > SETTLE_THRESHOLD;
        }

        return true;
    }

    private boolean updateBounce(float maxScroll, float dt) {
        float targetOverscroll = 0.0f;
        float springForce = (targetOverscroll - overscroll) * BOUNCE_STIFFNESS;
        bounceVelocity += springForce * dt;
        bounceVelocity *= BOUNCE_DAMPING;
        overscroll += bounceVelocity * dt;

        if (currentScroll < 0) {
            currentScroll = -overscroll;
        } else if (currentScroll > maxScroll) {
            currentScroll = maxScroll + overscroll;
        }

        if (Math.abs(overscroll) < SETTLE_THRESHOLD && Math.abs(bounceVelocity) < SETTLE_THRESHOLD) {
            overscroll = 0.0f;
            bounceVelocity = 0.0f;
            if (currentScroll < 0) {
                currentScroll = 0;
            } else if (currentScroll > maxScroll) {
                currentScroll = maxScroll;
            }
            return false;
        }

        return true;
    }

    public float getOverscroll() {
        return overscroll;
    }

    public boolean isAnimating() {
        return hasMomentum || overscroll > SETTLE_THRESHOLD || Math.abs(velocity) > VELOCITY_THRESHOLD;
    }

    public void reset() {
        currentScroll = 0.0f;
        targetScroll = 0.0f;
        velocity = 0.0f;
        overscroll = 0.0f;
        bounceVelocity = 0.0f;
        isDragging = false;
        hasMomentum = false;
    }
}
