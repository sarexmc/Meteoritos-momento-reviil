package com.meteoritos;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/** Cráter encendido: echa fuego, lava y humo durante un buen rato. */
public final class Ember {
    private final double x, y, z, radius;
    private int left;

    public Ember(double x, double y, double z, double radius, int left) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.left = left;
    }

    /** @return true cuando termina */
    public boolean tick(ServerWorld w, long time) {
        left--;
        if (time % 4 == 0) {
            Fx.p(w, ParticleTypes.LAVA, x, y + 0.5, z, 3, radius * 0.6, 0.4, radius * 0.6, 0);
            Fx.p(w, ParticleTypes.FLAME, x, y + 0.5, z, 6, radius * 0.7, 0.3, radius * 0.7, 0.01);
            Fx.p(w, ParticleTypes.LARGE_SMOKE, x, y + 1.5, z, 4, radius * 0.7, 1.0, radius * 0.7, 0.02);
        }
        if (time % 12 == 0) {
            Fx.p(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 1.0, z, 2, radius * 0.5, 0.5, radius * 0.5, 0.01);
        }
        return left <= 0;
    }
}
