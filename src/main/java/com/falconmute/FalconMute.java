package com.falconmute;

import com.falconmute.commands.MuteCommand;
import com.falconmute.commands.UnmuteCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FalconMute extends JavaPlugin {

    public static class MuteInfo {
        public final long expiry;
        public final String reason;
        public final boolean isIpMute;
        public MuteInfo(long expiry, String reason, boolean isIpMute) {
            this.expiry = expiry;
            this.reason = reason;
            this.isIpMute = isIpMute;
        }
    }

    private final Map<UUID, MuteInfo> voiceMutes = new ConcurrentHashMap<>();
    private final Map<UUID, MuteInfo> chatMutes = new ConcurrentHashMap<>();

    private com.falconmute.listeners.IPTracker ipTracker;

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

        ipTracker = new com.falconmute.listeners.IPTracker(this);
        getServer().getPluginManager().registerEvents(ipTracker, this);

        getCommand("ipmute").setExecutor(new com.falconmute.commands.IPMuteCommand(this));
        getCommand("ipmute").setTabCompleter(new com.falconmute.commands.IPMuteCommand(this));

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
                        boolean isIpMute = config.getBoolean("isIpMute", false);
                        chatMutes.put(uuid, new MuteInfo(expiry, reason, isIpMute));
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
                        boolean isIpMute = config.getBoolean("isIpMute", false);
                        voiceMutes.put(uuid, new MuteInfo(expiry, reason, isIpMute));
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
        config.set("isIpMute", info.isIpMute);
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

    public void setVoiceMuted(UUID uuid, boolean muted, long expiry, String reason, boolean isIpMute) {
        if (muted) {
            MuteInfo info = new MuteInfo(expiry, reason, isIpMute);
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

    public void setChatMuted(UUID uuid, boolean muted, long expiry, String reason, boolean isIpMute) {
        if (muted) {
            MuteInfo info = new MuteInfo(expiry, reason, isIpMute);
            chatMutes.put(uuid, info);
            saveMute(uuid, false, info);
        } else {
            chatMutes.remove(uuid);
            saveMute(uuid, false, null);
        }
    }

    public com.falconmute.listeners.IPTracker getIPTracker() {
        return ipTracker;
    }

    public void runAsync(Runnable runnable) {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            getServer().getAsyncScheduler().runNow(this, task -> runnable.run());
        } catch (ClassNotFoundException e) {
            getServer().getScheduler().runTaskAsynchronously(this, runnable);
        }
    }

    public java.util.Set<UUID> getAllMutedPlayers() {
        java.util.Set<UUID> muted = new java.util.HashSet<>();
        for (UUID uuid : chatMutes.keySet()) {
            if (isChatMuted(uuid) && !chatMutes.get(uuid).isIpMute) muted.add(uuid);
        }
        for (UUID uuid : voiceMutes.keySet()) {
            if (isVoiceMuted(uuid) && !voiceMutes.get(uuid).isIpMute) muted.add(uuid);
        }
        return muted;
    }

    public java.util.Set<UUID> getAllIpMutedPlayers() {
        java.util.Set<UUID> muted = new java.util.HashSet<>();
        for (UUID uuid : chatMutes.keySet()) {
            if (isChatMuted(uuid) && chatMutes.get(uuid).isIpMute) muted.add(uuid);
        }
        for (UUID uuid : voiceMutes.keySet()) {
            if (isVoiceMuted(uuid) && voiceMutes.get(uuid).isIpMute) muted.add(uuid);
        }
        return muted;
    }

    public void sendMuteList(org.bukkit.command.CommandSender sender, int page, String label, boolean isIpMuteList) {
        runAsync(() -> {
            java.util.Set<UUID> mutedUuids = isIpMuteList ? getAllIpMutedPlayers() : getAllMutedPlayers();
            java.util.List<String> names = new java.util.ArrayList<>();
            for (UUID u : mutedUuids) {
                String name = org.bukkit.Bukkit.getOfflinePlayer(u).getName();
                if (name == null) name = u.toString();
                names.add(name);
            }
            java.util.Collections.sort(names);

            int pageSize = 10;
            int totalPlayers = names.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);
            if (totalPages == 0) totalPages = 1;

            int finalPage = page;
            if (finalPage < 1) finalPage = 1;
            if (finalPage > totalPages) finalPage = totalPages;

            int start = (finalPage - 1) * pageSize;
            int end = Math.min(start + pageSize, totalPlayers);

            sender.sendMessage(org.bukkit.ChatColor.GRAY + "" + finalPage + " - [" + totalPlayers + "] ========================");

            for (int i = start; i < end; i++) {
                sender.sendMessage(org.bukkit.ChatColor.WHITE + "" + (i + 1) + ". " + org.bukkit.ChatColor.LIGHT_PURPLE + names.get(i));
            }

            if (totalPages == 1) {
                sender.sendMessage(org.bukkit.ChatColor.GRAY + "==========================");
            } else if (finalPage == 1) {
                net.md_5.bungee.api.chat.TextComponent next = new net.md_5.bungee.api.chat.TextComponent("[Next] ");
                next.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                next.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (finalPage + 1)));
                
                net.md_5.bungee.api.chat.TextComponent lines = new net.md_5.bungee.api.chat.TextComponent("=======================");
                lines.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                
                net.md_5.bungee.api.chat.TextComponent footer = new net.md_5.bungee.api.chat.TextComponent("");
                footer.addExtra(next);
                footer.addExtra(lines);
                
                if (sender instanceof org.bukkit.entity.Player) {
                    ((org.bukkit.entity.Player) sender).spigot().sendMessage(footer);
                } else {
                    sender.sendMessage(footer.toLegacyText());
                }
            } else if (finalPage > 1 && finalPage < totalPages) {
                net.md_5.bungee.api.chat.TextComponent prev = new net.md_5.bungee.api.chat.TextComponent("[Previous] ");
                prev.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                prev.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (finalPage - 1)));
                
                net.md_5.bungee.api.chat.TextComponent lines = new net.md_5.bungee.api.chat.TextComponent("==================");
                lines.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                
                net.md_5.bungee.api.chat.TextComponent next = new net.md_5.bungee.api.chat.TextComponent(" [Next]");
                next.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                next.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (finalPage + 1)));

                net.md_5.bungee.api.chat.TextComponent footer = new net.md_5.bungee.api.chat.TextComponent("");
                footer.addExtra(prev);
                footer.addExtra(lines);
                footer.addExtra(next);
                
                if (sender instanceof org.bukkit.entity.Player) {
                    ((org.bukkit.entity.Player) sender).spigot().sendMessage(footer);
                } else {
                    sender.sendMessage(footer.toLegacyText());
                }
            } else if (finalPage == totalPages) {
                net.md_5.bungee.api.chat.TextComponent prev = new net.md_5.bungee.api.chat.TextComponent("[Previous] ");
                prev.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                prev.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (finalPage - 1)));
                
                net.md_5.bungee.api.chat.TextComponent lines = new net.md_5.bungee.api.chat.TextComponent("==================");
                lines.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                
                net.md_5.bungee.api.chat.TextComponent footer = new net.md_5.bungee.api.chat.TextComponent("");
                footer.addExtra(prev);
                footer.addExtra(lines);
                
                if (sender instanceof org.bukkit.entity.Player) {
                    ((org.bukkit.entity.Player) sender).spigot().sendMessage(footer);
                } else {
                    sender.sendMessage(footer.toLegacyText());
                }
            }
        });
    }
}
