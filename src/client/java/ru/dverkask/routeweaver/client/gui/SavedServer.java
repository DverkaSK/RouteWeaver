package ru.dverkask.routeweaver.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import ru.dverkask.routeweaver.routing.ServerAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An entry from the vanilla multiplayer list together with its parsed address.
 */
record SavedServer(ServerInfo info, ServerAddress address) {
    String name() {
        return info.name;
    }

    /** The address string exactly as saved in {@code servers.dat}. */
    String rawAddress() {
        return info.address;
    }

    /**
     * Reads {@code servers.dat}, skipping entries whose address repeats an earlier one.
     */
    static List<SavedServer> load(MinecraftClient client) {
        ServerList serverList = new ServerList(client);
        serverList.loadFile();

        List<SavedServer> servers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < serverList.size(); i++) {
            ServerInfo info = serverList.get(i);
            if (info.address == null || info.address.isBlank()) {
                continue;
            }
            if (seen.add(info.address.toLowerCase(Locale.ROOT))) {
                net.minecraft.client.network.ServerAddress parsed =
                        net.minecraft.client.network.ServerAddress.parse(info.address);
                servers.add(new SavedServer(info, new ServerAddress(parsed.getAddress(), parsed.getPort())));
            }
        }
        return servers;
    }
}
