package com.github.lumin.ducks;

public interface EndCrystalAccess {
    long lumin$getSpawnTime();

    void lumin$setSpawnTime(long timeMs);

    boolean lumin$isMioAttacked();

    void lumin$setMioAttacked(boolean attacked);
}
