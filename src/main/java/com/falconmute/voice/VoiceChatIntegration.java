package com.falconmute.voice;

import com.falconmute.FalconMute;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class VoiceChatIntegration implements VoicechatPlugin {

    private final FalconMute plugin;

    public VoiceChatIntegration(FalconMute plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPluginId() {
        return "falconmute";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private final Map<UUID, Long> lastActionBarTime = new ConcurrentHashMap<>();

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) {
            return;
        }

        try {
            UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
            
            if (plugin.isVoiceMuted(playerUuid)) {
                event.cancel();
                
                long now = System.currentTimeMillis();
                if (now - lastActionBarTime.getOrDefault(playerUuid, 0L) > 2000L) {
                    lastActionBarTime.put(playerUuid, now);
                    Player bukkitPlayer = Bukkit.getPlayer(playerUuid);
                    if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                        String msg = ChatColor.translateAlternateColorCodes('&', "&cYou are voice muted and cannot speak!");
                        bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors here to not spam console
        }
    }
}
