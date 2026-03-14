package Paqlio.me.Listeners;

import Paqlio.me.Addons.DiscordChatBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class Chat implements Listener {

    /*
     * ignoreCancelled = true -> Jeśli plugin od sklepu anulował event (żeby cena nie była widoczna dla wszystkich),
     * ta metoda w ogóle się nie wykona.
     * priority = EventPriority.HIGHEST -> Czekamy, aż inne pluginy zdecydują, co zrobić z wiadomością.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var message = event.getMessage();

        // Wywołujemy wysyłkę na Discord tylko dla wiadomości, które przeszły przez filtry innych pluginów
        DiscordChatBridge.sendMessageToDiscord(player, message);
    }
}