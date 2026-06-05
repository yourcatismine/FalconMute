package com.falconmute;

import com.falconmute.commands.MuteCommand;
import com.falconmute.commands.UnmuteCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class FalconMute extends JavaPlugin {

    public static class MuteInfo {
        public final long expiry;
        public final String reason;
        public MuteInfo(long expiry, String reason) {
            this.expiry = expiry;
            this.reason = reason;
        }
    }

    private final Map<UUID, MuteInfo> voiceMutes = new HashMap<>();
    private final Map<UUID, MuteInfo> chatMutes = new HashMap<>();

    @Override
    public void onEnable() {
        loadMutes();

        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("mute").setTabCompleter(new MuteCommand(this));
        
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("unmute").setTabCompleter(new UnmuteCommand(this));

        getServer().getPluginManager().registerEvents(new com.falconmute.listeners.ChatListener(this), this);

        if (getServer().getPluginManager().getPlugin("voicechat") != null) {
            try {
                com.falconmute.voice.VoiceChatRegistrar.register(this);
            } catch (Exception e) {
                getLogger().warning("Failed to register VoiceChatIntegration: " + e.getMessage());
            }
        }

        printStartupBanner();
    }

    private void loadMutes() {
        java.io.File chatDir = new java.io.File(getDataFolder(), "data/chat");
        if (chatDir.exists() && chatDir.isDirectory()) {
            for (java.io.File file : chatDir.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    try {
                        UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                        long expiry = config.getLong("expiry", 0);
                        String reason = config.getString("reason", "No Reason Provided");
                        chatMutes.put(uuid, new MuteInfo(expiry, reason));
                    } catch (Exception e) {
                        getLogger().warning("Failed to load chat mute: " + file.getName());
                    }
                }
            }
        }

        java.io.File voiceDir = new java.io.File(getDataFolder(), "data/voice");
        if (voiceDir.exists() && voiceDir.isDirectory()) {
            for (java.io.File file : voiceDir.listFiles()) {
                if (file.getName().endsWith(".yml")) {
                    try {
                        UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                        long expiry = config.getLong("expiry", 0);
                        String reason = config.getString("reason", "No Reason Provided");
                        voiceMutes.put(uuid, new MuteInfo(expiry, reason));
                    } catch (Exception e) {
                        getLogger().warning("Failed to load voice mute: " + file.getName());
                    }
                }
            }
        }
    }

    private void saveMute(UUID uuid, boolean isVoice, MuteInfo info) {
        java.io.File dir = new java.io.File(getDataFolder(), "data/" + (isVoice ? "voice" : "chat"));
        if (!dir.exists()) dir.mkdirs();
        java.io.File file = new java.io.File(dir, uuid.toString() + ".yml");
        
        if (info == null) {
            if (file.exists()) file.delete();
            return;
        }
        
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        config.set("expiry", info.expiry);
        config.set("reason", info.reason);
        try {
            config.save(file);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void printStartupBanner() {
        org.bukkit.command.ConsoleCommandSender console = getServer().getConsoleSender();
        String version = getDescription().getVersion();
        String author = getDescription().getAuthors().isEmpty() ? "Kiarers" : String.join(", ", getDescription().getAuthors());

        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &b&l[FalconMute] &bv" + version));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &fAuthor: &b" + author));
        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', " &fStatus: &a&lEnabling Modules..."));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  &lMODULE STATUS:"));

        if (getServer().getPluginManager().getPlugin("voicechat") != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fVoiceChat Hook: &a&lCONNECTED"));
        } else {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [-] &fVoiceChat Hook: &c&lNOT FOUND"));
        }
        
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8&m--------------------------------------------------"));
    }

    public MuteInfo getChatMuteInfo(UUID uuid) {
        if (!chatMutes.containsKey(uuid)) return null;
        MuteInfo info = chatMutes.get(uuid);
        if (System.currentTimeMillis() > info.expiry && info.expiry != 0) { 
            chatMutes.remove(uuid);
            saveMute(uuid, false, null);
            return null;
        }
        return info;
    }

    public boolean isVoiceMuted(UUID uuid) {
        if (!voiceMutes.containsKey(uuid)) return false;
        MuteInfo info = voiceMutes.get(uuid);
        if (info.expiry > 0 && System.currentTimeMillis() > info.expiry) {
            voiceMutes.remove(uuid);
            saveMute(uuid, true, null);
            return false;
        }
        return true;
    }

    public void setVoiceMuted(UUID uuid, boolean muted, long expiry, String reason) {
        if (muted) {
            MuteInfo info = new MuteInfo(expiry, reason);
            voiceMutes.put(uuid, info);
            saveMute(uuid, true, info);
        } else {
            voiceMutes.remove(uuid);
            saveMute(uuid, true, null);
        }
    }

    public boolean isChatMuted(UUID uuid) {
        return getChatMuteInfo(uuid) != null;
    }

    public void setChatMuted(UUID uuid, boolean muted, long expiry, String reason) {
        if (muted) {
            MuteInfo info = new MuteInfo(expiry, reason);
            chatMutes.put(uuid, info);
            saveMute(uuid, false, info);
        } else {
            chatMutes.remove(uuid);
            saveMute(uuid, false, null);
        }
    }
}
