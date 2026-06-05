package com.falconmute.voice;

import com.falconmute.FalconMute;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.Bukkit;

public class VoiceChatRegistrar {
    
    public static void register(FalconMute plugin) {
        BukkitVoicechatService service = Bukkit.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new VoiceChatIntegration(plugin));
        }
    }
}
