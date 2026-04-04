package com.github.epsilon.modules;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public abstract class HudModule extends Module {

    public enum HorizontalAnchor {
        Left,
        Center,
        Right
    }

    public enum VerticalAnchor {
        Top,
        Center,
        Bottom
    }

    public float x, y, width, height;

    private HorizontalAnchor horizontalAnchor = HorizontalAnchor.Left;
    private VerticalAnchor verticalAnchor = VerticalAnchor.Top;
    private float anchorX;
    private float anchorY;

    public HudModule(String name, Category category) {
        this(name, category, 0f, 0f, 20f, 20f);
    }

    public HudModule(String name, Category category, float width, float height) {
        this(name, category, 0f, 0f, width, height);
    }

    public HudModule(String name, Category category, float x, float y, float width, float height) {
        super(name, category);

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.anchorX = x;
        this.anchorY = y;
    }

    protected void updateBounds(DeltaTracker deltaTracker) {
    }

    public final void updateLayout(DeltaTracker deltaTracker) {
        updateBounds(deltaTracker);
        applyRenderPosition(getAnchoredRenderX(), getAnchoredRenderY(), false);
    }

    protected final void setBounds(float width, float height) {
        this.width = Math.max(0.0f, width);
        this.height = Math.max(0.0f, height);
    }

    public final boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public final void moveTo(float x, float y) {
        applyRenderPosition(x, y, true);
    }

    public final void moveBy(float deltaX, float deltaY) {
        moveTo(x + deltaX, y + deltaY);
    }

    public final void loadLegacyPosition(float renderX, float renderY) {
        horizontalAnchor = HorizontalAnchor.Left;
        verticalAnchor = VerticalAnchor.Top;
        applyRenderPosition(renderX, renderY, false);
    }

    public final void setAnchorState(HorizontalAnchor horizontalAnchor, VerticalAnchor verticalAnchor, float anchorX, float anchorY) {
        this.horizontalAnchor = horizontalAnchor == null ? HorizontalAnchor.Left : horizontalAnchor;
        this.verticalAnchor = verticalAnchor == null ? VerticalAnchor.Top : verticalAnchor;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        applyRenderPosition(getAnchoredRenderX(), getAnchoredRenderY(), false);
    }

    public final HorizontalAnchor getHorizontalAnchor() {
        return horizontalAnchor;
    }

    public final VerticalAnchor getVerticalAnchor() {
        return verticalAnchor;
    }

    public final float getAnchorX() {
        return anchorX;
    }

    public final float getAnchorY() {
        return anchorY;
    }

    private void applyRenderPosition(float renderX, float renderY, boolean updateAnchors) {
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        float clampedX = Mth.clamp(renderX, 0.0f, Math.max(0.0f, screenWidth - width));
        float clampedY = Mth.clamp(renderY, 0.0f, Math.max(0.0f, screenHeight - height));

        if (updateAnchors) {
            horizontalAnchor = getHorizontalAnchorFor(clampedX, screenWidth);
            verticalAnchor = getVerticalAnchorFor(clampedY, screenHeight);
        }

        this.x = clampedX;
        this.y = clampedY;
        this.anchorX = toAnchorX(clampedX, screenWidth);
        this.anchorY = toAnchorY(clampedY, screenHeight);
    }

    private float getAnchoredRenderX() {
        int screenWidth = getScreenWidth();
        return switch (horizontalAnchor) {
            case Left -> anchorX;
            case Center -> screenWidth / 2.0f - width / 2.0f + anchorX;
            case Right -> screenWidth - width + anchorX;
        };
    }

    private float getAnchoredRenderY() {
        int screenHeight = getScreenHeight();
        return switch (verticalAnchor) {
            case Top -> anchorY;
            case Center -> screenHeight / 2.0f - height / 2.0f + anchorY;
            case Bottom -> screenHeight - height + anchorY;
        };
    }

    private float toAnchorX(float renderX, int screenWidth) {
        return switch (horizontalAnchor) {
            case Left -> renderX;
            case Center -> renderX + width / 2.0f - screenWidth / 2.0f;
            case Right -> renderX + width - screenWidth;
        };
    }

    private float toAnchorY(float renderY, int screenHeight) {
        return switch (verticalAnchor) {
            case Top -> renderY;
            case Center -> renderY + height / 2.0f - screenHeight / 2.0f;
            case Bottom -> renderY + height - screenHeight;
        };
    }

    private HorizontalAnchor getHorizontalAnchorFor(float renderX, int screenWidth) {
        float splitLeft = screenWidth / 3.0f;
        float splitRight = splitLeft * 2.0f;

        boolean left = renderX <= splitLeft;
        boolean right = renderX + width >= splitRight;

        if ((left && right) || (!left && !right)) {
            return HorizontalAnchor.Center;
        }

        return left ? HorizontalAnchor.Left : HorizontalAnchor.Right;
    }

    private VerticalAnchor getVerticalAnchorFor(float renderY, int screenHeight) {
        float splitTop = screenHeight / 3.0f;
        float splitBottom = splitTop * 2.0f;

        boolean top = renderY <= splitTop;
        boolean bottom = renderY + height >= splitBottom;

        if ((top && bottom) || (!top && !bottom)) {
            return VerticalAnchor.Center;
        }

        return top ? VerticalAnchor.Top : VerticalAnchor.Bottom;
    }

    private static int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private static int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public abstract void render(DeltaTracker deltaTracker);

}
