package com.falconmute.listeners;

import com.falconmute.FalconMute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IPTracker implements Listener {

    private final FalconMute plugin;
    private final File file;
    private YamlConfiguration config;

    private final Map<UUID, Set<String>> playerIps = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> ipPlayers = new ConcurrentHashMap<>();

    public IPTracker(FalconMute plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/ips.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create ips.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        
        if (config.contains("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> ips = config.getStringList("players." + uuidStr);
                    Set<String> ipSet = ConcurrentHashMap.newKeySet();
                    ipSet.addAll(ips);
                    playerIps.put(uuid, ipSet);
                    
                    for (String ip : ips) {
                        ipPlayers.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore invalid UUIDs
                }
            }
        }
    }

    private void saveAsync() {
        plugin.runAsync(() -> {
            YamlConfiguration toSave = new YamlConfiguration();
            for (Map.Entry<UUID, Set<String>> entry : playerIps.entrySet()) {
                toSave.set("players." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
            }
            try {
                toSave.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save ips.yml");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getAddress() == null) return;
        
        UUID uuid = event.getPlayer().getUniqueId();
        String ip = event.getPlayer().getAddress().getAddress().getHostAddress();

        boolean updated = false;
        
        Set<String> ips = playerIps.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
        if (ips.add(ip)) {
            updated = true;
        }

        Set<UUID> uuids = ipPlayers.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet());
        if (uuids.add(uuid)) {
            updated = true;
        }

        if (updated) {
            saveAsync();
        }
    }

    public Set<String> getIps(UUID uuid) {
        return playerIps.getOrDefault(uuid, Collections.emptySet());
    }

    public Set<UUID> getUuids(String ip) {
        return ipPlayers.getOrDefault(ip, Collections.emptySet());
    }
}
