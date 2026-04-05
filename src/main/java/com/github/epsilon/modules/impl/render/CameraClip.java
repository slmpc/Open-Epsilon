package com.github.epsilon.modules.impl.render;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.settings.impl.DoubleSetting;

public class CameraClip extends Module {

    public static final CameraClip INSTANCE = new CameraClip();

    private CameraClip() {
        super("Camera Clip", Category.RENDER);
    }

    public final DoubleSetting distance = doubleSetting("Distance", 3.5, 1.0, 20.0, 0.5);

    public final BoolSetting action = boolSetting("Action", true);
    private final DoubleSetting interpolation = doubleSetting("Interpolation", 0.05, 0.01, 1.0, 0.01, action::getValue);

}
