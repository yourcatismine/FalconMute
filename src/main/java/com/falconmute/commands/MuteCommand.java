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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final FalconMute plugin;

    public MuteCommand(FalconMute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("falconmute.mute")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (args.length < 1) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }
        
        if (!args[0].equalsIgnoreCase("list") && args.length < 3) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
            plugin.sendMuteList(sender, page, label, false);
            return true;
        }

        if (!type.equals("chat") && !type.equals("voice")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String targetName = args[1];
        String durationStr = args[2];
        String reason = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                : "No Reason Provided";

        long durationMs = parseDuration(durationStr);
        if (durationMs <= 0) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        long expiry = System.currentTimeMillis() + durationMs;
        
        plugin.runAsync(() -> {
            Player target = Bukkit.getPlayer(targetName);
            UUID targetUUID;
            String finalTargetName;

            if (target != null) {
                targetUUID = target.getUniqueId();
                finalTargetName = target.getName();
            } else {
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetName);
                if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                    String msg = ChatColor.RED + "That player does not exist.";
                    sender.sendMessage(msg);
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                        ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                    return;
                }
                targetUUID = offlinePlayer.getUniqueId();
                finalTargetName = targetName;
            }

            if (type.equals("voice")) {
                if (!sender.hasPermission("falconmute.voicemute")) {
                    if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (plugin.isVoiceMuted(targetUUID)) {
                    sender.sendMessage(ChatColor.RED + "That player is already voice muted.");
                    if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                plugin.setVoiceMuted(targetUUID, true, expiry, reason, false);
            } else {
                if (!sender.hasPermission("falconmute.chatmute")) {
                    if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (plugin.isChatMuted(targetUUID)) {
                    sender.sendMessage(ChatColor.RED + "That player is already chat muted.");
                    if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                plugin.setChatMuted(targetUUID, true, expiry, reason, false);
            }

            String typeStr = type.equals("chat") ? "chat" : "voice";
            String adminMsg = ChatColor.translateAlternateColorCodes('&',
                    "&7You muted &d" + finalTargetName + "&7's " + typeStr + " for &f" + durationStr + "&7 Reason:&c " + reason);
            sender.sendMessage(adminMsg);
            if (sender instanceof Player) {
                ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(adminMsg));
            }

            if (target != null && target.isOnline()) {
                String targetMsg = ChatColor.translateAlternateColorCodes('&',
                        "&7Your " + typeStr + " has been muted for &f" + durationStr + "&7 Reason:&c " + reason);
                target.sendMessage(targetMsg);
                target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(targetMsg));
                target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
        });

        return true;
    }

    private long parseDuration(String s) {
        if (s == null || s.isEmpty())
            return 0;
        try {
            String numberStr = s.replaceAll("[^0-9]", "");
            if (numberStr.isEmpty())
                return 0;

            long time = Long.parseLong(numberStr);
            String unit = s.replaceAll("[0-9]", "").toLowerCase();

            if (unit.equals("s"))
                return time * 1000L;
            if (unit.equals("m"))
                return time * 60000L;
            if (unit.equals("h"))
                return time * 3600000L;
            if (unit.equals("d"))
                return time * 86400000L;
            if (unit.equals("y"))
                return time * 31536000000L;

            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("chat", "voice", "list").stream()
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
        if (args.length == 3) {
            return Arrays.asList("10s", "1m", "1d", "1y").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
