package com.github.lumin.modules.impl.visual;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;

public class Glow extends Module {
    public static final Glow INSTANCE = new Glow();
    public Glow() {
        super("发光","",Category.VISUAL);
    }
}
