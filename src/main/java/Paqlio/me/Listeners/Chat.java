package Paqlio.me.Listeners;

import Paqlio.me.Addons.DiscordChatBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Chat implements Listener {

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        // Wywołaj nową statyczną metodę, która zajmie się całą logiką
        DiscordChatBridge.sendMessageToDiscord(event.getPlayer(), event.getMessage());
    }
}
