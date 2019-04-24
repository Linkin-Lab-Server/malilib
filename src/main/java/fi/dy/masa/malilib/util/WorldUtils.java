package fi.dy.masa.malilib.util;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class WorldUtils
{
    /**
     * Best name. Returns the integrated server world for the current dimension
     * in single player, otherwise just the client world.
     * @param mc
     * @return
     */
    @Nullable
    public static World getBestWorld(MinecraftClient mc)
    {
        IntegratedServer server = mc.getServer();

        if (mc.world != null && server != null)
        {
            return server.getWorld(mc.world.dimension.getType());
        }
        else
        {
            return mc.world;
        }
    }

    /**
     * Returns the requested chunk from the integrated server, if it's available.
     * Otherwise returns the client world chunk.
     * @param chunkX
     * @param chunkZ
     * @param mc
     * @return
     */
    @Nullable
    public static WorldChunk getBestChunk(int chunkX, int chunkZ, MinecraftClient mc)
    {
        IntegratedServer server = mc.getServer();
        WorldChunk chunk = null;

        if (mc.world != null && server != null)
        {
            ServerWorld world = server.getWorld(mc.world.dimension.getType());
            chunk = world.getChunkFutureSyncOnMainThread(chunkX, chunkZ, false).getNow(null);
        }

        if (chunk != null)
        {
            return chunk;
        }

        return mc.world.method_8497(chunkX, chunkZ);
    }
}
