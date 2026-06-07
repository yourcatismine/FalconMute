# FalconMute

FalconMute is a standalone, lightweight, and Folia-supported mute management plugin for Minecraft servers. It completely handles both traditional text chat muting and integration with **Simple Voice Chat** to prevent players from talking in-game when punished. 

# Version Support
- **1.21+** / **26.1** / **Folia Supported**

## Features
- **Dual Muting System:** Mute a player's text chat or voice chat independently.
- **Voice Chat Hook:** Automatically hooks into [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) to cancel voice packets for muted players.
- **Duration Support:** Standardized duration parsing (e.g., `10s`, `1m`, `1d`, `1y`) allows you to easily issue temporary mutes.
- **Data Persistence:** Mutes are saved individually per-player in YAML files (`plugins/FalconMute/data/chat/` and `plugins/FalconMute/data/voice/`) preventing data loss on server restarts.
- **Smart Chat Filtering:** Blocks regular chat as well as private messaging commands (e.g., `/msg`, `/tell`, `/w`, `/r`, `/reply`, `/pm`, `/whisper`, `/me`) for muted players.
- **IP Tracker & Muting:** Safely tracks player IPs in the background and allows admins to mute all accounts associated with an IP address or offline player instantly.
- **Interactive Mute Lists:** View all currently muted players using interactive, paginated lists with clickable chat navigation.
- **Strict Validation:** Automatically verifies if a player has joined the server before and gracefully prevents errors when applying mutes.
- **Folia Support:** Built against the modern Paper/Spigot 26.1 API. Heavy operations like offline player lookups and mass IP muting run 100% asynchronously to ensure zero TPS drops.

## Commands

### `/mute`
**Usage:** `/mute <chat|voice|list> <player|page> [duration] [reason]`
**Description:** Mute a player for a specific duration, or view the paginated list of normally muted players. The reason defaults to "No Reason Provided" if left blank.
**Tab-Completion:** Fully supported for type, online players, and common duration formats.

### `/unmute`
**Usage:** `/unmute <chat|voice> <player>`
**Description:** Instantly removes a player's mute and deletes their persistent mute data.

### `/ipmute`
**Usage:** `/ipmute <chat|voice|remove|list> <player|ip|page> [duration] [reason]`
**Description:** Mute an IP address or all accounts associated with a player. Can target chat or voice, remove all mutes for the given IP, or list players caught by an IP mute with interactive pagination.

## Permissions

- `falconmute.mute` - Allows usage of the base `/mute` command.
- `falconmute.chatmute` - Allows muting a player's chat.
- `falconmute.voicemute` - Allows muting a player's voice.
- `falconmute.unmute` - Allows usage of the base `/unmute` command.
- `falconmute.chatunmute` - Allows unmuting a player's chat.
- `falconmute.voiceunmute` - Allows unmuting a player's voice.
- `falconmute.ipmute` - Allows usage of the base `/ipmute` command.

## Installation
1. Place `FalconMute-1.0.jar` into your server's `plugins/` directory.
2. *(Optional but Recommended)* Ensure [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) is installed to utilize the voice muting features.
3. Restart or start your server.

## Developer
Developed by **Kiarers**.
