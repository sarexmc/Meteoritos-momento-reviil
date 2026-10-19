package com.meteoritos;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.joml.Vector3f;

/** Utilidades de particulas: se envian a distancia larga (force) para que se vean desde lejos. */
public final class Fx {
    private Fx() {}

    public static final DustParticleEffect ORANGE = new DustParticleEffect(new Vector3f(1.0f, 0.42f, 0.06f), 4.0f);
    public static final DustParticleEffect YELLOW = new DustParticleEffect(new Vector3f(1.0f, 0.78f, 0.22f), 4.0f);
    public static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(0.72f, 0.07f, 0.03f), 4.0f);
    public static final DustParticleEffect SOOT = new DustParticleEffect(new Vector3f(0.20f, 0.17f, 0.15f), 4.0f);
    public static final DustParticleEffect ASHGREY = new DustParticleEffect(new Vector3f(0.48f, 0.44f, 0.40f), 4.0f);
    public static final DustParticleEffect HAZE = new DustParticleEffect(new Vector3f(0.85f, 0.18f, 0.05f), 4.0f);

    private static final double RANGE = 450.0;

    public static <T extends ParticleEffect> void p(ServerWorld w, T type, double x, double y, double z,
                                                    int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayerEntity pl : w.getPlayers()) {
            double ax = pl.getX() - x;
            double ay = pl.getY() - y;
            double az = pl.getZ() - z;
            if (ax * ax + ay * ay + az * az < RANGE * RANGE) {
                w.spawnParticles(pl, type, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    public static boolean loaded(ServerWorld w, double x, double z) {
        int cx = ((int) Math.floor(x)) >> 4;
        int cz = ((int) Math.floor(z)) >> 4;
        return w.getChunkManager().isChunkLoaded(cx, cz);
    }
}
