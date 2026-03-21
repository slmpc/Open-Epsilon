package com.github.lumin.gui.dropdown;

public final class DropdownLayout {

    private DropdownLayout() {
    }

    public static Layout compute(int screenWidth, int screenHeight) {
        return compute(screenWidth, screenHeight, 58.0f);
    }

    public static Layout compute(int screenWidth, int screenHeight, float railWidth) {
        float panelWidth = Math.min(704.0f, screenWidth - 80.0f);
        float panelHeight = Math.min(404.0f, screenHeight - 84.0f);

        float x = (screenWidth - panelWidth) / 2.0f;
        float y = (screenHeight - panelHeight) / 2.0f;

        float gap = DropdownTheme.SECTION_GAP;
        float columnHeight = panelHeight - DropdownTheme.OUTER_PADDING * 2.0f;
        float railX = x + DropdownTheme.OUTER_PADDING;
        float modulesX = railX + railWidth + gap;
        float maxContentRight = x + panelWidth - DropdownTheme.OUTER_PADDING;
        float moduleWidth = Math.min(204.0f, panelWidth * 0.31f);
        float detailX = modulesX + moduleWidth + gap;
        float detailWidth = maxContentRight - detailX;

        Rect panel = new Rect(x, y, panelWidth, panelHeight);
        Rect rail = new Rect(railX, y + DropdownTheme.OUTER_PADDING, railWidth, columnHeight);
        Rect modules = new Rect(modulesX, y + DropdownTheme.OUTER_PADDING, moduleWidth, columnHeight);
        Rect detail = new Rect(detailX, y + DropdownTheme.OUTER_PADDING, detailWidth, columnHeight);

        return new Layout(panel, rail, modules, detail);
    }

    public record Layout(Rect panel, Rect rail, Rect modules, Rect detail) {
    }

    public record Rect(float x, float y, float width, float height) {
        public float right() {
            return x + width;
        }

        public float bottom() {
            return y + height;
        }

        public float centerX() {
            return x + width / 2.0f;
        }

        public float centerY() {
            return y + height / 2.0f;
        }

        public boolean contains(double px, double py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }

        public Rect inset(float amount) {
            return new Rect(x + amount, y + amount, width - amount * 2.0f, height - amount * 2.0f);
        }
    }

}
