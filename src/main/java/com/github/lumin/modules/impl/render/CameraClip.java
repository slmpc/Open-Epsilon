package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;

public class CameraClip extends Module {

    public static final CameraClip INSTANCE = new CameraClip();

    private CameraClip() {
        super("CameraClip", Category.RENDER);
    }

}
