package com.meteoritos;

import net.minecraft.util.math.Vec3d;

/** Un meteorito en vuelo. No es una entidad: se dibuja solo con particulas. */
public final class Meteor {
    public final Vec3d start;
    public final Vec3d end;
    public final int total;
    public final int lvl;
    public int age = 0;

    public Meteor(Vec3d start, Vec3d end, int total, int lvl) {
        this.start = start;
        this.end = end;
        this.total = total;
        this.lvl = lvl;
    }

    public Vec3d pos() {
        return start.lerp(end, (double) age / (double) total);
    }

    public Vec3d dir() {
        return end.subtract(start).normalize();
    }
}
