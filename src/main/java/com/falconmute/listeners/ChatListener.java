package com.falconmute.listeners;

import com.falconmute.FalconMute;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    private final FalconMute plugin;
    private final List<String> blockedCommands = Arrays.asList(
            "/msg", "/tell", "/w", "/r", "/reply", "/whisper", "/pm", "/me"
    );

    public ChatListener(FalconMute plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.isChatMuted(uuid)) {
            event.setCancelled(true);
            sendMuteMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.isChatMuted(uuid)) {
            String message = event.getMessage().toLowerCase();
            for (String cmd : blockedCommands) {
                if (message.startsWith(cmd + " ") || message.equals(cmd)) {
                    event.setCancelled(true);
                    sendMuteMessage(player);
                    return;
                }
            }
        }
    }

    private void sendMuteMessage(Player player) {
        FalconMute.MuteInfo muteInfo = plugin.getChatMuteInfo(player.getUniqueId());
        if (muteInfo == null) return;

        String reason = muteInfo.reason;
        if (reason == null || reason.isEmpty()) {
            reason = "No Reason Provided";
        }
        
        long expiry = muteInfo.expiry;
        String durationLeft = "Permanent";
        if (expiry > 0) {
            long totalSeconds = (expiry - System.currentTimeMillis()) / 1000;
            if (totalSeconds < 0) totalSeconds = 0;
            durationLeft = formatDuration(totalSeconds);
        }

        String msg = ChatColor.translateAlternateColorCodes('&',
                "&7You have been muted for &f" + durationLeft + "&7 Reason:&c " + reason);

        player.sendMessage(msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
      //  player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "Expired";
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0) sb.append(s).append("s");

        return sb.toString().trim();
    }
}
