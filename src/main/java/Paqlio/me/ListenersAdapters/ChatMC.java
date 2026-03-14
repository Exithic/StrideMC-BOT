package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ChatMC extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // 1. Szybkie wykluczenie botów i webhooków
        if (event.getAuthor().isBot() || event.isWebhookMessage()) return;

        // 2. Weryfikacja kanału (użycie getId() dla stałych ze Stringiem)
        if (!event.getChannel().getId().equals(Constants.MinecraftChannel)) return;

        // 3. Pobranie danych (EffectiveName pokaże Nick z Discorda, jeśli go ma)
        var author = event.getAuthor().getEffectiveName();
        var message = event.getMessage().getContentDisplay();

        // 4. Walidacja treści (nie wysyłaj pustych wiadomości, np. samych załączników)
        if (message.isBlank()) return;

        // 5. Formatowanie i wysyłka
        // Używamy & do kolorów (jeśli masz plugin od kolorów) lub §
        var formatted = String.format("§7(§bDiscord§7) §f%s§7: §f%s", author, message);

        // broadcastMessage wysyła do wszystkich graczy i do konsoli automatycznie
        Bukkit.broadcastMessage(formatted);
    }
}