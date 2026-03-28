package com.github.epsilon.modules.impl.player;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;

public class MovementFix extends Module {

    public static final MovementFix INSTANCE = new MovementFix();

    private MovementFix() {
        super("MovementFix", Category.PLAYER);
    }

}

