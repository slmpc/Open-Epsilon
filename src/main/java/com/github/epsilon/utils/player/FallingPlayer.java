package com.github.epsilon.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class FallingPlayer {
    public double x;
    public double y;
    public double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private final float yaw;
    private final float strafe;
    private final float forward;
    private float jumpMovementFactor;
    private Minecraft mc = Minecraft.getInstance();

    public FallingPlayer(double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float strafe, float forward) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.yaw = yaw;
        this.strafe = strafe;
        this.forward = forward;
    }

    public FallingPlayer(Player player) {
        this(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getDeltaMovement().x,
                player.getDeltaMovement().y,
                player.getDeltaMovement().z,
                player.getYRot(),
                player.xxa,
                player.zza
        );
        float f = player.level().getBlockState(player.blockPosition()).getBlock().getJumpFactor();
        float f1 = player.level().getBlockState(player.getOnPos()).getBlock().getJumpFactor();
        float jumpingVelocity = 0.42F * ((double) f == 1.0 ? f1 : f) + player.getJumpBoostPower();
        this.jumpMovementFactor = jumpingVelocity;
    }

    private void calculateForTick2() {
        float sr = this.strafe;
        float fw = this.forward;
        float v = sr * sr + fw * fw;
        if (v >= 1.0E-4F) {
            v = Mth.sqrt(v);
            if (v < 1.0F) {
                v = 1.0F;
            }

            float fixedJumpFactor = this.jumpMovementFactor;
            if (this.mc.player.isSprinting()) {
                fixedJumpFactor *= 1.3F;
            }

            v = fixedJumpFactor / v;
            sr *= v;
            fw *= v;
            float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0F);
            float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0F);
            this.motionX += (double) (sr * f2 - fw * f1);
            this.motionZ += (double) (fw * f2 + sr * f1);
        }

        this.motionY -= 0.08;
        this.motionY *= 0.98F;
        this.x = this.x + this.motionX;
        this.y = this.y + this.motionY;
        this.z = this.z + this.motionZ;
    }

    private void calculateForTick() {
        float sr = this.strafe * 0.98F;
        float fw = this.forward * 0.98F;
        float v = sr * sr + fw * fw;
        if (v >= 1.0E-4F) {
            v = Mth.sqrt(v);
            if (v < 1.0F) {
                v = 1.0F;
            }

            float fixedJumpFactor = this.jumpMovementFactor;
            if (this.mc.player.isSprinting()) {
                fixedJumpFactor *= 1.3F;
            }

            v = fixedJumpFactor / v;
            sr *= v;
            fw *= v;
            float f1 = Mth.sin(this.yaw * (float) Math.PI / 180.0F);
            float f2 = Mth.cos(this.yaw * (float) Math.PI / 180.0F);
            this.motionX += (double) (sr * f2 - fw * f1);
            this.motionZ += (double) (fw * f2 + sr * f1);
        }

        this.motionY -= 0.08;
        this.motionY *= 0.98F;
        this.x = this.x + this.motionX;
        this.y = this.y + this.motionY;
        this.z = this.z + this.motionZ;
        this.motionX *= 0.91;
        this.motionZ *= 0.91;
    }

    public void calculateMLG(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.calculateForTick2();
        }
    }

    public void calculate(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.calculateForTick();
        }
    }

}
