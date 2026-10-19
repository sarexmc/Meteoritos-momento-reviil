package com.meteoritos;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.particle.DustParticleEffect;

/**
 * Hongo de humo estilo bomba atomica, dibujado con particulas:
 * bola de fuego en la base, tallo que sube, sombrero en forma de toroide y onda de choque en el suelo.
 */
public final class Cloud {
    private final double x, y, z;
    private final int lvl;
    private int age = 0;

    public Cloud(double x, double y, double z, int lvl) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lvl = lvl;
    }

    private double groundY(ServerWorld w, double px, double pz) {
        if (Fx.loaded(w, px, pz)) {
            return w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(px), (int) Math.floor(pz));
        }
        return y;
    }

    /** @return true cuando termina */
    public boolean tick(ServerWorld w) {
        age++;
        Random r = w.random;
        double s = 0.8 + 0.2 * lvl;          // 1.0 .. 1.8
        double H = 58.0 * s;                 // altura del tallo
        double capMax = 22.0 * s;            // tamaño del sombrero

        double f = Math.min(1.0, age / 90.0);
        double eased = 1.0 - (1.0 - f) * (1.0 - f);
        double top = H * eased;

        // 1) Bola de fuego en la base
        if (age < 70) {
            double fr = 4.5 * s * (1.0 - age / 90.0);
            Fx.p(w, Fx.ORANGE, x, y + 2 * s, z, 14, fr, fr * 0.7, fr, 0);
            Fx.p(w, Fx.YELLOW, x, y + 2 * s, z, 6, fr * 0.6, fr * 0.5, fr * 0.6, 0);
            Fx.p(w, ParticleTypes.FLAME, x, y + 2 * s, z, 20, fr, fr * 0.7, fr, 0.08);
        }

        // 2) Tallo: fuego abajo, humo oscuro arriba
        for (int j = 0; j < 5; j++) {
            double yy = r.nextDouble() * top;
            double frac = yy / H;
            double rad = (2.2 + 3.0 * (1.0 - frac)) * s;
            if (frac < 0.4 && age < 130) {
                DustParticleEffect c = r.nextBoolean() ? Fx.ORANGE : Fx.RED;
                Fx.p(w, c, x, y + yy, z, 5, rad, 1.5, rad, 0);
            }
            Fx.p(w, ParticleTypes.LARGE_SMOKE, x, y + yy, z, 4, rad * 0.8, 1.5, rad * 0.8, 0.01);
            if (j < 2) {
                Fx.p(w, Fx.SOOT, x, y + yy, z, 3, rad * 0.7, 1.5, rad * 0.7, 0);
            }
        }

        // 3) Sombrero (toroide) que se infla arriba del tallo
        double g = Math.max(0.0, Math.min(1.0, (age - 45) / 70.0));
        if (g > 0) {
            double ge = 1.0 - (1.0 - g) * (1.0 - g);
            double M = 0.55 * capMax * ge;          // radio mayor
            double m = 0.42 * capMax * ge;          // radio del tubo
            double drift = age > 90 ? Math.min(14.0 * s, (age - 90) * 0.05 * s) : 0.0;
            double cy = y + top + drift + m * 0.3;
            int K = age < 200 ? 10 : 5;
            boolean hot = age < 170;

            for (int k = 0; k < K; k++) {
                double th = r.nextDouble() * Math.PI * 2.0;
                double ph = r.nextDouble() * Math.PI * 2.0;
                double rr = M + m * Math.cos(ph);
                double px = x + rr * Math.cos(th);
                double pz = z + rr * Math.sin(th);
                double py = cy + m * 0.75 * Math.sin(ph);
                if (hot) {
                    double sn = Math.sin(ph);
                    DustParticleEffect c = sn > 0.3 ? Fx.YELLOW : (sn > -0.4 ? Fx.ORANGE : Fx.RED);
                    Fx.p(w, c, px, py, pz, 3, 1.2, 1.0, 1.2, 0);
                }
                Fx.p(w, ParticleTypes.LARGE_SMOKE, px, py, pz, 2, 1.5, 1.2, 1.5, 0.01);
                if (k % 3 == 0) {
                    Fx.p(w, hot ? Fx.SOOT : Fx.ASHGREY, px, py, pz, 2, 1.5, 1.2, 1.5, 0);
                }
            }

            // Corona de arriba y falda de abajo para que se vea llena, no hueca
            for (int k = 0; k < 4; k++) {
                double th = r.nextDouble() * Math.PI * 2.0;
                double rr = Math.sqrt(r.nextDouble()) * M;
                double px = x + rr * Math.cos(th);
                double pz = z + rr * Math.sin(th);
                Fx.p(w, ParticleTypes.LARGE_SMOKE, px, cy - m * 0.35, pz, 2, 1.5, 0.8, 1.5, 0.005);
                if (hot) {
                    Fx.p(w, Fx.ORANGE, px, cy + m * 0.45, pz, 2, 1.5, 0.8, 1.5, 0);
                }
            }
            if (age % 2 == 0) {
                Fx.p(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, cy, z, 1, M * 0.8, m * 0.5, M * 0.8, 0);
            }
        }

        // 4) Onda de choque que se expande por el suelo
        if (age < 45) {
            double rad = age * 1.8 * s;
            for (int i = 0; i < 10; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                double px = x + rad * Math.cos(a);
                double pz = z + rad * Math.sin(a);
                double py = groundY(w, px, pz) + 0.5;
                Fx.p(w, ParticleTypes.CLOUD, px, py, pz, 3, 1.5, 0.6, 1.5, 0.02);
                Fx.p(w, Fx.ASHGREY, px, py + 0.5, pz, 2, 1.5, 0.8, 1.5, 0);
            }
        }

        return age >= 380;
    }
}
