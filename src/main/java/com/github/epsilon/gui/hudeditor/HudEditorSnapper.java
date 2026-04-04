package com.github.epsilon.gui.hudeditor;

import com.github.epsilon.modules.HudLayoutHelper;
import com.github.epsilon.modules.HudModule;
import net.minecraft.util.Mth;

import java.util.List;

final class HudEditorSnapper {

    private static final float SNAP_THRESHOLD = 8.0f;

    private HudEditorSnapper() {
    }

    static SnapPosition snapPosition(HudModule module, float renderX, float renderY, int screenWidth, int screenHeight, List<HudModule> hudModules) {
        AxisSnap screenXSnap = snapAxis(
                renderX,
                module.width,
                screenWidth,
                new float[]{0.0f, screenWidth / 3.0f, screenWidth / 2.0f, screenWidth * 2.0f / 3.0f, screenWidth},
                true
        );
        AxisSnap screenYSnap = snapAxis(
                renderY,
                module.height,
                screenHeight,
                new float[]{0.0f, screenHeight / 3.0f, screenHeight / 2.0f, screenHeight * 2.0f / 3.0f, screenHeight},
                false
        );
        AxisSnap moduleXSnap = snapAxisToModules(module, renderX, renderY, screenWidth, screenHeight, hudModules, true);
        AxisSnap moduleYSnap = snapAxisToModules(module, renderX, renderY, screenWidth, screenHeight, hudModules, false);
        AxisSnap xSnap = pickCloserSnap(screenXSnap, moduleXSnap);
        AxisSnap ySnap = pickCloserSnap(screenYSnap, moduleYSnap);

        return new SnapPosition(xSnap.renderPosition(), ySnap.renderPosition(), xSnap.guide(), ySnap.guide());
    }

    private static AxisSnap snapAxis(float renderPosition, float size, int screenSize, float[] targets, boolean horizontal) {
        float clampedPosition = Mth.clamp(renderPosition, 0.0f, Math.max(0.0f, screenSize - size));
        float bestPosition = clampedPosition;
        Float bestGuide = null;
        float bestDistance = SNAP_THRESHOLD + 1.0f;

        if (horizontal) {
            for (HudModule.HorizontalAnchor anchor : HudModule.HorizontalAnchor.values()) {
                float offset = HudLayoutHelper.getHorizontalAnchorOffset(anchor, size);
                for (float target : targets) {
                    float candidate = Mth.clamp(target - offset, 0.0f, Math.max(0.0f, screenSize - size));
                    if (HudLayoutHelper.resolveHorizontalAnchor(candidate, size, screenSize) != anchor) {
                        continue;
                    }

                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        } else {
            for (HudModule.VerticalAnchor anchor : HudModule.VerticalAnchor.values()) {
                float offset = HudLayoutHelper.getVerticalAnchorOffset(anchor, size);
                for (float target : targets) {
                    float candidate = Mth.clamp(target - offset, 0.0f, Math.max(0.0f, screenSize - size));
                    if (HudLayoutHelper.resolveVerticalAnchor(candidate, size, screenSize) != anchor) {
                        continue;
                    }

                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        }

        return new AxisSnap(bestPosition, bestGuide, bestDistance);
    }

    private static AxisSnap snapAxisToModules(HudModule module, float renderX, float renderY, int screenWidth, int screenHeight, List<HudModule> hudModules, boolean horizontal) {
        float size = horizontal ? module.width : module.height;
        int screenSize = horizontal ? screenWidth : screenHeight;
        float crossSize = horizontal ? module.height : module.width;
        int crossScreenSize = horizontal ? screenHeight : screenWidth;
        float clampedPosition = Mth.clamp(horizontal ? renderX : renderY, 0.0f, Math.max(0.0f, screenSize - size));
        float clampedCrossPosition = Mth.clamp(horizontal ? renderY : renderX, 0.0f, Math.max(0.0f, crossScreenSize - crossSize));
        float bestPosition = clampedPosition;
        Float bestGuide = null;
        float bestDistance = SNAP_THRESHOLD + 1.0f;
        float[] selfOffsets = new float[]{0.0f, size / 2.0f, size};
        float selfCrossEnd = clampedCrossPosition + crossSize;

        for (HudModule otherHud : hudModules) {
            if (otherHud == module) {
                continue;
            }

            float otherCrossStart = horizontal ? otherHud.y : otherHud.x;
            float otherCrossEnd = otherCrossStart + (horizontal ? otherHud.height : otherHud.width);
            if (!isCrossAxisCompatible(clampedCrossPosition, selfCrossEnd, otherCrossStart, otherCrossEnd)) {
                continue;
            }

            float[] targets = horizontal
                    ? new float[]{otherHud.x, otherHud.x + otherHud.width / 2.0f, otherHud.x + otherHud.width}
                    : new float[]{otherHud.y, otherHud.y + otherHud.height / 2.0f, otherHud.y + otherHud.height};

            for (float selfOffset : selfOffsets) {
                for (float target : targets) {
                    float candidate = Mth.clamp(target - selfOffset, 0.0f, Math.max(0.0f, screenSize - size));
                    float distance = Math.abs(candidate - clampedPosition);
                    if (distance <= SNAP_THRESHOLD && distance < bestDistance) {
                        bestDistance = distance;
                        bestPosition = candidate;
                        bestGuide = target;
                    }
                }
            }
        }

        return new AxisSnap(bestPosition, bestGuide, bestDistance);
    }

    private static boolean isCrossAxisCompatible(float selfStart, float selfEnd, float otherStart, float otherEnd) {
        return getAxisGap(selfStart, selfEnd, otherStart, otherEnd) <= SNAP_THRESHOLD;
    }

    private static float getAxisGap(float selfStart, float selfEnd, float otherStart, float otherEnd) {
        if (selfEnd < otherStart) {
            return otherStart - selfEnd;
        }
        if (otherEnd < selfStart) {
            return selfStart - otherEnd;
        }
        return 0.0f;
    }

    private static AxisSnap pickCloserSnap(AxisSnap primary, AxisSnap secondary) {
        if (secondary.guide() != null && (primary.guide() == null || secondary.distance() < primary.distance())) {
            return secondary;
        }

        return primary;
    }

    private record AxisSnap(float renderPosition, Float guide, float distance) {
    }

    record SnapPosition(float renderX, float renderY, Float guideX, Float guideY) {
    }

}
