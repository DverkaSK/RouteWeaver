package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import ru.dverkask.routeweaver.routing.ServerAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An entry from the vanilla multiplayer list together with its parsed address.
 */
record SavedServer(ServerData info, ServerAddress address) {
    String name() {
        return info.name;
    }

    /** The address string exactly as saved in {@code servers.dat}. */
    String rawAddress() {
        return info.ip;
    }

    /**
     * Reads {@code servers.dat}, skipping entries whose address repeats an earlier one.
     */
    static List<SavedServer> load(Minecraft minecraft) {
        ServerList serverList = new ServerList(minecraft);
        serverList.load();

        List<SavedServer> servers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < serverList.size(); i++) {
            ServerData info = serverList.get(i);
            if (info.ip == null || info.ip.isBlank()) {
                continue;
            }
            if (seen.add(info.ip.toLowerCase(Locale.ROOT))) {
                net.minecraft.client.multiplayer.resolver.ServerAddress parsed =
                        net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(info.ip);
                servers.add(new SavedServer(info, new ServerAddress(parsed.getHost(), parsed.getPort())));
            }
        }
        return servers;
    }
}
