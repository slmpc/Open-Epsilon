package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;

public class NoRotate extends Module {

    public static final NoRotate INSTANCE = new NoRotate();

    private NoRotate() {
        super("NoRotate", Category.PLAYER);
    }

}
