package com.github.epsilon.events;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class MoveEvent extends Event implements ICancellableEvent {

    private final MoverType moverType;
    private double x, y, z;

    public MoveEvent(MoverType moverType, Vec3 delta) {
        this.moverType = moverType;
        this.x = delta.x;
        this.y = delta.y;
        this.z = delta.z;
    }

    public MoverType getMoverType() {
        return this.moverType;
    }

    public Vec3 getDelta() {
        return new Vec3(x, y, z);
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }

}
