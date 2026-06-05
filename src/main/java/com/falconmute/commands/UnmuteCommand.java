package com.falconmute.commands;

import com.falconmute.FalconMute;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnmuteCommand implements CommandExecutor, TabCompleter {

    private final FalconMute plugin;

    public UnmuteCommand(FalconMute plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("falconmute.unmute")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        if (args.length < 2) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String type = args[0].toLowerCase();
        if (!type.equals("chat") && !type.equals("voice")) {
            if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return true;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUUID;
        String finalTargetName;

        if (target != null) {
            targetUUID = target.getUniqueId();
            finalTargetName = target.getName();
        } else {
            targetUUID = Bukkit.getOfflinePlayer(targetName).getUniqueId();
            finalTargetName = targetName;
        }

        boolean isVoice = type.equals("voice");
        if (isVoice) {
            if (!sender.hasPermission("falconmute.voiceunmute")) {
                if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            if (!plugin.isVoiceMuted(targetUUID)) {
                sender.sendMessage(ChatColor.RED + finalTargetName + " is not voice muted.");
                if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            plugin.setVoiceMuted(targetUUID, false, 0, null);
        } else {
            if (!sender.hasPermission("falconmute.chatunmute")) {
                if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            if (!plugin.isChatMuted(targetUUID)) {
                sender.sendMessage(ChatColor.RED + finalTargetName + " is not chat muted.");
                if (sender instanceof Player) ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }

            plugin.setChatMuted(targetUUID, false, 0, null);
        }

        String typeStr = isVoice ? "voice " : "";
        String adminMsg = ChatColor.translateAlternateColorCodes('&', "&d" + finalTargetName + "&7 has been " + typeStr + "unmuted.");
        sender.sendMessage(adminMsg);
        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(adminMsg));
        }

        if (target != null && target.isOnline()) {
            String targetMsg = ChatColor.translateAlternateColorCodes('&', "&7You have been " + typeStr + "unmuted.");
            target.sendMessage(targetMsg);
            target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(targetMsg));
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("chat", "voice").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }
}
