package com.meteoritos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class MeteoritosMod implements ModInitializer {
    public static final String MOD_ID = "meteoritos";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                MeteorCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(MeteorManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MeteorManager.reset());
    }
}
