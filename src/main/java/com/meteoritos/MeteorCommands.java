package com.meteoritos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

public final class MeteorCommands {
    private MeteorCommands() {}

    private static final String[] NAMES = {"Leve", "Moderada", "Fuerte", "Extrema", "APOCALIPSIS"};

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("meteoritos")
                .requires(src -> src.hasPermissionLevel(2))

                .then(CommandManager.literal("iniciar")
                        .executes(ctx -> {
                            MeteorManager.start(ctx.getSource().getServer(), 2);
                            return 1;
                        })
                        .then(CommandManager.argument("minutos", IntegerArgumentType.integer(1, 180))
                                .executes(ctx -> {
                                    int min = IntegerArgumentType.getInteger(ctx, "minutos");
                                    MeteorManager.start(ctx.getSource().getServer(), min);
                                    return 1;
                                })))

                .then(CommandManager.literal("detener")
                        .executes(ctx -> {
                            MeteorManager.stop(ctx.getSource().getServer());
                            return 1;
                        }))

                .then(CommandManager.literal("intensidad")
                        .then(CommandManager.argument("nivel", IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> {
                                    int n = IntegerArgumentType.getInteger(ctx, "nivel");
                                    MeteorManager.intensity = n;
                                    ctx.getSource().getServer().getPlayerManager().broadcast(
                                            Text.literal("Intensidad de meteoritos: " + n + " - " + NAMES[n - 1])
                                                    .formatted(Formatting.RED, Formatting.BOLD), false);
                                    return 1;
                                })))

                .then(CommandManager.literal("destruir")
                        .then(CommandManager.argument("activado", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean v = BoolArgumentType.getBool(ctx, "activado");
                                    MeteorManager.destroy = v;
                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                            v ? "Las explosiones ahora rompen bloques." : "Las explosiones ya no rompen bloques."), true);
                                    return 1;
                                })))

                .then(CommandManager.literal("uno")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity p = src.getPlayerOrThrow();
                            ServerWorld w = src.getWorld();
                            Vec3d look = p.getRotationVec(1.0f);
                            double hx = look.x, hz = look.z;
                            double len = Math.sqrt(hx * hx + hz * hz);
                            if (len < 1.0e-4) { hx = 1; hz = 0; len = 1; }
                            hx /= len;
                            hz /= len;
                            double min = MeteorManager.safeDistance(MeteorManager.intensity) + 8.0;
                            boolean ok = false;
                            double[] dists = {min + 30.0, min + 15.0, min};
                            for (double d : dists) {
                                double yaw = w.random.nextDouble() * Math.PI * 2.0;
                                if (MeteorManager.spawnAt(w, p.getX() + hx * d, p.getZ() + hz * d, yaw, MeteorManager.intensity)) {
                                    ok = true;
                                    break;
                                }
                            }
                            final boolean fok = ok;
                            src.sendFeedback(() -> Text.literal(fok
                                    ? "Meteorito en camino, mira al frente."
                                    : "No hay chunks cargados en esa direccion; prueba mirando hacia otro lado."), false);
                            return 1;
                        }))

                .then(CommandManager.literal("estado")
                        .executes(ctx -> {
                            String msg = "Lluvia: " + (MeteorManager.isActive() ? "activa (" + MeteorManager.secondsLeft() + " s restantes)" : "apagada")
                                    + " | Intensidad: " + MeteorManager.intensity
                                    + " | Romper bloques: " + (MeteorManager.destroy ? "si" : "no")
                                    + " | Meteoritos en vuelo: " + MeteorManager.flying();
                            ctx.getSource().sendFeedback(() -> Text.literal(msg), false);
                            return 1;
                        })));
    }
}
