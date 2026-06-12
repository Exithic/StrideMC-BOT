package Paqlio.me.Listeners;

import Paqlio.me.Addons.DiscordChatBridge;
import Paqlio.me.BOT;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class Chat implements Listener {

    /*
     * ignoreCancelled = true -> Jeśli plugin od sklepu, mutowania lub logowania
     * anulował event, ta metoda w ogóle się nie wykona.
     * priority = HIGHEST -> Czekamy na ostateczny werdykt innych pluginów.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        // 1. Fail-fast: Jeśli bot jest offline, nie obciążamy serwera dalszym kodem
        if (BOT.getJda() == null) return;

        var player = event.getPlayer();

        // 2. Paper API: Pobieramy nowoczesny Component i zamieniamy go na czysty tekst (String)
        // Pozbywa się to wszelkich ukrytych formatowań, które mogłyby zepsuć wygląd na Discordzie
        var message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 3. Dodatkowe zabezpieczenie: nie wysyłamy pustych wiadomości (np. samych spacji)
        if (message.isBlank()) return;

        // 4. Wysyłka do mostu (Wykonuje się asynchronicznie, bo sam event jest Async)
        DiscordChatBridge.sendMessageToDiscord(player, message);
    }
}