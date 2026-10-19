package com.meteoritos;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MeteorManager {
    private MeteorManager() {}

    // ----- configuracion (se cambia con comandos) -----
    public static int intensity = 3;       // 1..5
    public static boolean destroy = true;  // si las explosiones rompen bloques

    // Potencia de la explosion por nivel (TNT = 4)
    private static final float[] POWER = {6f, 9f, 13f, 18f, 25f};
    // Ticks entre meteoritos por nivel
    private static final int[] INTERVAL = {240, 160, 100, 60, 35};

    // ----- estado -----
    private static boolean active = false;
    private static int ticksLeft = 0;
    private static int totalTicks = 1;
    private static int spawnTimer = 0;
    private static long time = 0;
    private static ServerBossBar bar = null;

    private static final List<Meteor> meteors = new ArrayList<>();
    private static final List<Cloud> clouds = new ArrayList<>();
    private static final List<Ember> embers = new ArrayList<>();
    private static final Map<UUID, Shake> shakes = new HashMap<>();

    private static final class Shake {
        double amp;
        int ticks;
    }

    public static boolean isActive() { return active; }
    public static int secondsLeft() { return ticksLeft / 20; }
    public static int flying() { return meteors.size(); }

    public static float power(int lvl) { return POWER[MathHelper.clamp(lvl, 1, 5) - 1]; }
    public static double safeDistance(int lvl) { return power(lvl) * 2.0 + 20.0; }

    // ======================= control =======================

    public static void start(MinecraftServer server, int minutes) {
        active = true;
        totalTicks = Math.max(1, minutes * 1200);
        ticksLeft = totalTicks;
        spawnTimer = 40;
        if (bar == null) {
            bar = new ServerBossBar(Text.literal("LLUVIA DE METEORITOS"), BossBar.Color.RED, BossBar.Style.PROGRESS);
        }
        bar.setPercent(1.0f);
        bar.setVisible(true);
        updateBar(server);
        ServerWorld w = server.getOverworld();
        if (w != null) {
            for (ServerPlayerEntity p : w.getPlayers()) {
                w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_SPAWN,
                        SoundCategory.MASTER, 0.8f, 0.6f);
            }
        }
        server.getPlayerManager().broadcast(
                Text.literal("Lluvia de meteoritos iniciada (" + minutes + " min).").formatted(Formatting.YELLOW), false);
    }

    private static void closeBar() {
        if (bar != null) {
            bar.clearPlayers();
            bar.setVisible(false);
        }
    }

    public static void finish(MinecraftServer server) {
        active = false;
        closeBar();
        server.getPlayerManager().broadcast(
                Text.literal("La lluvia de meteoritos ha terminado.").formatted(Formatting.YELLOW), false);
    }

    public static void stop(MinecraftServer server) {
        active = false;
        closeBar();
        meteors.clear();
        clouds.clear();
        embers.clear();
        shakes.clear();
        server.getPlayerManager().broadcast(
                Text.literal("Lluvia de meteoritos detenida.").formatted(Formatting.YELLOW), false);
    }

    public static void reset() {
        active = false;
        closeBar();
        bar = null;
        meteors.clear();
        clouds.clear();
        embers.clear();
        shakes.clear();
    }

    // ======================= tick =======================

    public static void tick(MinecraftServer server) {
        ServerWorld w = server.getOverworld();
        if (w == null) return;
        time++;

        if (active) tickRain(server, w);
        if (!meteors.isEmpty()) tickMeteors(w);
        if (!clouds.isEmpty()) tickClouds(w);
        if (!embers.isEmpty()) tickEmbers(w);
        if (!shakes.isEmpty()) tickShakes(w);
        if (time % 5 == 0 && !meteors.isEmpty()) hud(w);
    }

    private static void updateBar(MinecraftServer server) {
        if (bar == null) return;
        int sec = Math.max(0, ticksLeft / 20);
        bar.setName(Text.literal(String.format("LLUVIA DE METEORITOS %02d:%02d", sec / 60, sec % 60))
                .formatted(Formatting.RED, Formatting.BOLD));
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            bar.addPlayer(p);
        }
    }

    private static void tickRain(MinecraftServer server, ServerWorld w) {
        ticksLeft--;
        spawnTimer--;

        if (spawnTimer <= 0) {
            int base = INTERVAL[intensity - 1];
            spawnTimer = base + w.random.nextInt(base / 3 + 1);
            int n = intensity == 5 ? 2 : 1;
            for (int i = 0; i < n; i++) spawnRandom(server, w, intensity);
        }

        if (bar != null) bar.setPercent(Math.max(0f, (float) ticksLeft / (float) totalTicks));
        if (time % 20 == 0) updateBar(server);
        if (time % 4 == 0) ambient(w);
        if (ticksLeft <= 0) finish(server);
    }

    /** Neblina roja en el cielo y ceniza, mas un rugido lejano de vez en cuando. */
    private static void ambient(ServerWorld w) {
        for (ServerPlayerEntity p : w.getPlayers()) {
            w.spawnParticles(p, Fx.HAZE, true, p.getX(), p.getY() + 45, p.getZ(), 30, 70, 14, 70, 0);
            w.spawnParticles(p, ParticleTypes.ASH, true, p.getX(), p.getY() + 4, p.getZ(), 15, 25, 8, 25, 0);
        }
        if (time % 120 == 0 && w.random.nextInt(3) == 0) {
            for (ServerPlayerEntity p : w.getPlayers()) {
                w.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 1.5f, 0.5f);
            }
        }
    }

    // ======================= generar meteoritos =======================

    /** Elige un punto de impacto al azar, siempre lejos de todos los jugadores (apto para hardcore). */
    public static boolean spawnRandom(MinecraftServer server, ServerWorld w, int lvl) {
        List<ServerPlayerEntity> players = w.getPlayers();
        if (players.isEmpty()) return false;

        double safe = safeDistance(lvl);
        double maxD = Math.min(server.getPlayerManager().getViewDistance() * 16.0 - 24.0, 220.0);
        double minD = safe + 10.0;
        if (maxD < minD) maxD = minD + 20.0;

        for (int attempt = 0; attempt < 30; attempt++) {
            ServerPlayerEntity anchor = players.get(w.random.nextInt(players.size()));
            double ang = w.random.nextDouble() * Math.PI * 2.0;
            double d = minD + w.random.nextDouble() * (maxD - minD);
            double ix = anchor.getX() + Math.cos(ang) * d;
            double iz = anchor.getZ() + Math.sin(ang) * d;

            boolean ok = true;
            for (ServerPlayerEntity p : players) {
                double dx = p.getX() - ix;
                double dz = p.getZ() - iz;
                if (dx * dx + dz * dz < safe * safe) { ok = false; break; }
            }
            if (!ok) continue;

            double yaw = w.random.nextDouble() * Math.PI * 2.0;
            if (spawnAt(w, ix, iz, yaw, lvl)) return true;
        }
        return false;
    }

    /** Crea un meteorito que cae en diagonal y aterriza en (ix, iz). */
    public static boolean spawnAt(ServerWorld w, double ix, double iz, double yawRad, int lvl) {
        if (!Fx.loaded(w, ix, iz)) return false;
        int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(ix), (int) Math.floor(iz));
        double H = 150.0;
        double horiz = H / Math.tan(Math.toRadians(58.0));
        Vec3d end = new Vec3d(ix, top, iz);
        Vec3d start = new Vec3d(ix - Math.cos(yawRad) * horiz, top + H, iz - Math.sin(yawRad) * horiz);
        meteors.add(new Meteor(start, end, 88, lvl));
        return true;
    }

    // ======================= vuelo =======================

    private static void tickMeteors(ServerWorld w) {
        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor m = it.next();
            m.age++;
            if (m.age >= m.total) {
                it.remove();
                impact(w, m.end, m.lvl);
                continue;
            }
            Vec3d p = m.pos();
            Vec3d d = m.dir();
            double sc = 1.0 + 0.15 * m.lvl;

            // Cabeza: bola de fuego enorme
            Fx.p(w, Fx.ORANGE, p.x, p.y, p.z, 14, 1.3 * sc, 1.3 * sc, 1.3 * sc, 0);
            Fx.p(w, Fx.YELLOW, p.x, p.y, p.z, 6, 0.8 * sc, 0.8 * sc, 0.8 * sc, 0);
            Fx.p(w, ParticleTypes.FLAME, p.x, p.y, p.z, 30, 1.8 * sc, 1.8 * sc, 1.8 * sc, 0.04);
            Fx.p(w, ParticleTypes.LAVA, p.x, p.y, p.z, 4, 1.2 * sc, 1.2 * sc, 1.2 * sc, 0);
            Fx.p(w, ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 10, 2.0 * sc, 2.0 * sc, 2.0 * sc, 0.02);
            Fx.p(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, p.x, p.y, p.z, 2, 0.5, 0.5, 0.5, 0);

            // Estela larga de fuego y humo oscuro
            for (int i = 1; i <= 12; i++) {
                Vec3d q = p.subtract(d.multiply(i * 3.5));
                double sp = Math.max(0.5, 1.7 * sc - i * 0.09);
                if (i <= 7) {
                    Fx.p(w, ParticleTypes.FLAME, q.x, q.y, q.z, Math.max(3, 16 - i * 2), sp, sp, sp, 0.02);
                    if (i <= 4) Fx.p(w, Fx.ORANGE, q.x, q.y, q.z, 4, sp, sp, sp, 0);
                }
                Fx.p(w, ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 3, sp, sp, sp, 0.01);
                if (i % 2 == 0) Fx.p(w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, q.x, q.y, q.z, 1, sp * 0.5, sp * 0.5, sp * 0.5, 0);
            }

            if (m.age % 3 == 0) {
                w.playSound(null, p.x, p.y, p.z, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.AMBIENT, 12f, 0.5f);
            }
            if (m.age % 6 == 0) {
                w.playSound(null, p.x, p.y, p.z, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 12f, 0.5f);
            }
        }
    }

    // ======================= impacto =======================

    private static void impact(ServerWorld w, Vec3d e, int lvl) {
        double x = e.x, y = e.y, z = e.z;
        float power = power(lvl);

        BlockState ground = w.getBlockState(BlockPos.ofFloored(x, y - 1, z));
        if (ground.isAir() || !ground.getFluidState().isEmpty()) ground = Blocks.STONE.getDefaultState();

        // Sonido y destello
        w.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 60f, 0.5f);
        w.playSound(null, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 60f, 0.4f);
        w.playSound(null, x, y, z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.MASTER, 40f, 0.5f);
        Fx.p(w, ParticleTypes.FLASH, x, y + 2, z, 3, 0, 0, 0, 0);
        Fx.p(w, ParticleTypes.EXPLOSION_EMITTER, x, y + 1, z, 6, 4, 1, 4, 0);
        Fx.p(w, ParticleTypes.FLAME, x, y + 2, z, 250, 2, 2, 2, 0.45);
        Fx.p(w, ParticleTypes.LAVA, x, y + 1, z, 100, 4, 2, 4, 0);
        Fx.p(w, new BlockStateParticleEffect(ParticleTypes.BLOCK, ground), x, y + 1, z, 300, 3, 1, 3, 0.9);

        // Explosion real
        w.createExplosion(null, x, y + 1, z, power, true,
                destroy ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE);

        if (destroy) decorate(w, x, z, power, lvl);

        // Temblor de camara segun la distancia
        double range = 170.0 + 55.0 * lvl;
        for (ServerPlayerEntity p : w.getPlayers()) {
            double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= range) continue;
            double k = Math.pow(1.0 - dist / range, 1.1);
            double amp = Math.min(5.0, (1.2 + 0.9 * lvl) * k);
            if (amp < 0.15) continue;
            int dur = (int) (30 + 12 * lvl + 40 * k);
            Shake s = shakes.computeIfAbsent(p.getUuid(), id -> new Shake());
            s.amp = Math.max(s.amp * 0.5, amp);
            s.ticks = Math.max(s.ticks, dur);
        }

        // Hongo nuclear y crater ardiendo
        clouds.add(new Cloud(x, y, z, lvl));
        embers.add(new Ember(x, y, z, power * 0.6, 800));
    }

    /** Magma y fuego alrededor del crater. */
    private static void decorate(ServerWorld w, double cx, double cz, float power, int lvl) {
        int n = 20 + lvl * 15;
        for (int i = 0; i < n; i++) {
            double a = w.random.nextDouble() * Math.PI * 2.0;
            double rad = w.random.nextDouble() * power * 1.1;
            double bx = cx + Math.cos(a) * rad;
            double bz = cz + Math.sin(a) * rad;
            if (!Fx.loaded(w, bx, bz)) continue;
            int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(bx), (int) Math.floor(bz));
            BlockPos pos = new BlockPos((int) Math.floor(bx), top - 1, (int) Math.floor(bz));
            BlockState st = w.getBlockState(pos);
            if (st.isAir() || !st.getFluidState().isEmpty() || st.isOf(Blocks.BEDROCK)) continue;
            if (w.random.nextFloat() < 0.5f) {
                w.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState());
            }
            BlockPos above = pos.up();
            if (w.random.nextFloat() < 0.6f && w.getBlockState(above).isAir()) {
                w.setBlockState(above, Blocks.FIRE.getDefaultState());
            }
        }
    }

    // ======================= animaciones =======================

    private static void tickClouds(ServerWorld w) {
        Iterator<Cloud> it = clouds.iterator();
        while (it.hasNext()) {
            if (it.next().tick(w)) it.remove();
        }
    }

    private static void tickEmbers(ServerWorld w) {
        Iterator<Ember> it = embers.iterator();
        while (it.hasNext()) {
            if (it.next().tick(w, time)) it.remove();
        }
    }

    /** Sacude la camara con paquetes de rotacion relativa (no mueve al jugador). */
    private static void tickShakes(ServerWorld w) {
        int ph = (int) (time % 4);
        for (ServerPlayerEntity p : w.getPlayers()) {
            Shake s = shakes.get(p.getUuid());
            if (s == null) continue;
            if (s.ticks <= 0) {
                shakes.remove(p.getUuid());
                continue;
            }
            s.ticks--;
            double a = s.amp * Math.min(1.0, s.ticks / 25.0);
            double b = a * 0.6;
            float yaw, pitch;
            switch (ph) {
                case 0: yaw = (float) a;  pitch = (float) b;  break;
                case 1: yaw = (float) -a; pitch = (float) -b; break;
                case 2: yaw = (float) -a; pitch = (float) b;  break;
                default: yaw = (float) a; pitch = (float) -b; break;
            }
            p.networkHandler.sendPacket(new PlayerPositionLookS2CPacket(
                    0.0, 0.0, 0.0, yaw, pitch, EnumSet.allOf(PositionFlag.class), -1));
            if (time % 6 == 0 && s.amp > 1.0) {
                w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE,
                        SoundCategory.AMBIENT, 0.4f, 0.5f);
            }
        }
    }

    // ======================= HUD =======================

    private static final String[] ARROWS = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

    private static void hud(ServerWorld w) {
        for (ServerPlayerEntity p : w.getPlayers()) {
            Meteor best = null;
            double bestD = 400.0;
            Vec3d bestPos = null;
            for (Meteor m : meteors) {
                Vec3d mp = m.pos();
                double d = mp.distanceTo(p.getPos());
                if (d < bestD) {
                    bestD = d;
                    best = m;
                    bestPos = mp;
                }
            }
            if (best == null) continue;

            double dx = bestPos.x - p.getX();
            double dz = bestPos.z - p.getZ();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = MathHelper.wrapDegrees(targetYaw - p.getYaw());
            int idx = Math.floorMod(Math.round(rel / 45.0f), 8);

            Text msg = Text.literal("METEORITO ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(Math.round(bestD) + " m ").formatted(Formatting.WHITE, Formatting.BOLD))
                    .append(Text.literal(ARROWS[idx]).formatted(Formatting.WHITE, Formatting.BOLD));
            p.sendMessage(msg, true);
        }
    }
}
