package com.falconmute.commands;

import com.falconmute.FalconMute;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IPMuteCommand implements CommandExecutor, TabCompleter {

    private final FalconMute plugin;
    private static final Pattern IP_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public IPMuteCommand(FalconMute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("falconmute.ipmute")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (args.length < 1) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (!args[0].equalsIgnoreCase("list") && args.length < 2) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String type = args[0].toLowerCase();
        
        if (type.equals("list")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            }
            plugin.sendMuteList(sender, page, label, true);
            return true;
        }

        if (!type.equals("chat") && !type.equals("voice") && !type.equals("remove")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (!type.equals("remove") && args.length < 3) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String targetStr = args[1];
        boolean isIp = IP_PATTERN.matcher(targetStr).matches();

        long durationMs = 0;
        String reason = "No Reason Provided";
        long expiry = 0;

        if (!type.equals("remove")) {
            String durationStr = args[2];
            reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "No Reason Provided";
            
            durationMs = parseDuration(durationStr);
            if (durationMs <= 0) {
                if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            expiry = System.currentTimeMillis() + durationMs;
        }

        final long finalExpiry = expiry;
        final String finalReason = reason;

        plugin.runAsync(() -> {
            Set<UUID> targetUuids = new HashSet<>();

            if (isIp) {
                targetUuids.addAll(plugin.getIPTracker().getUuids(targetStr));
            } else {
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetStr);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    String msg = ChatColor.RED + "That player does not exist.";
                    sender.sendMessage(msg);
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                    return;
                }

                UUID playerUuid = offlinePlayer.getUniqueId();
                Set<String> ips = plugin.getIPTracker().getIps(playerUuid);
                
                if (ips.isEmpty()) {
                    String msg = ChatColor.RED + "Unable to find this player IP.";
                    sender.sendMessage(msg);
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                    return;
                } else {
                    for (String ip : ips) {
                        targetUuids.addAll(plugin.getIPTracker().getUuids(ip));
                    }
                }
            }

            if (targetUuids.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No accounts found to " + (type.equals("remove") ? "unmute" : "mute") + ".");
                return;
            }

            List<String> affectedNames = new ArrayList<>();

            for (UUID uuid : targetUuids) {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                if (name == null) name = uuid.toString();
                affectedNames.add(name);

                if (type.equals("remove")) {
                    plugin.setChatMuted(uuid, false, 0, null, false);
                    plugin.setVoiceMuted(uuid, false, 0, null, false);
                } else if (type.equals("chat")) {
                    plugin.setChatMuted(uuid, true, finalExpiry, finalReason, true);
                } else if (type.equals("voice")) {
                    plugin.setVoiceMuted(uuid, true, finalExpiry, finalReason, true);
                }

                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    if (type.equals("remove")) {
                        online.sendMessage(ChatColor.GREEN + "You have been unmuted by an admin.");
                    } else {
                        String msg = ChatColor.translateAlternateColorCodes('&', 
                            "&7Your " + type + " has been muted. Reason:&c " + finalReason);
                        online.sendMessage(msg);
                        online.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    }
                }
            }

            sender.sendMessage(ChatColor.GREEN + "Successfully " + (type.equals("remove") ? "removed IP mute" : "applied IP mute") + " for " + targetStr + ". Affected accounts:");
            
            StringBuilder sb = new StringBuilder();
            sb.append(ChatColor.GRAY).append("========================\n");
            for (int i = 0; i < affectedNames.size(); i++) {
                sb.append(ChatColor.WHITE).append(i + 1).append(". ").append(ChatColor.LIGHT_PURPLE).append(affectedNames.get(i)).append("\n");
            }
            sb.append(ChatColor.GRAY).append("========================");
            
            sender.sendMessage(sb.toString());
        });

        return true;
    }

    private long parseDuration(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            String numberStr = s.replaceAll("[^0-9]", "");
            if (numberStr.isEmpty()) return 0;

            long time = Long.parseLong(numberStr);
            String unit = s.replaceAll("[0-9]", "").toLowerCase();

            if (unit.equals("s")) return time * 1000L;
            if (unit.equals("m")) return time * 60000L;
            if (unit.equals("h")) return time * 3600000L;
            if (unit.equals("d")) return time * 86400000L;
            if (unit.equals("y")) return time * 31536000000L;

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("chat", "voice", "remove", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        if (args.length == 3 && !args[0].equalsIgnoreCase("remove")) {
            return Arrays.asList("10s", "1m", "1d", "1y").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
