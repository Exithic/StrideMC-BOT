package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Locale;
import java.util.stream.Collectors;

public class Server extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mc")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        // 1. ODRACZANIE ODPOWIEDZI (Zabezpieczenie przed błędem timeoutu)
        // Daje botowi aż 15 minut na odpowiedź. Gracz widzi "Bot myśli..."
        event.deferReply().queue();

        // 2. SYNCHRONIZACJA Z WĄTKIEM BUKKITA (Zapobiega ConcurrentModificationException)
        Bukkit.getScheduler().runTask(BOT.getInstance(), () -> {
            try {
                // Teraz bezpiecznie pobieramy dane z serwera Minecraft
                var version = Bukkit.getBukkitVersion().split("-")[0];
                var onlineCount = Bukkit.getOnlinePlayers().size();
                var maxPlayers = Bukkit.getMaxPlayers();

                // Pobieranie administracji online
                var staffOnline = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.isOp() || p.hasPermission("stridemc.staff"))
                        .map(p -> "`🛡️` " + p.getName())
                        .collect(Collectors.joining("\n"));

                if (staffOnline.isEmpty()) staffOnline = "*Brak administracji online*";

                // Obliczanie TPS i formatowanie
                var tps = Bukkit.getTPS()[0];
                // Używamy Locale.US, aby uniknąć przecinka zamiast kropki w polskim systemie (np. 19,99 zamiast 19.99)
                var formattedTps = String.format(Locale.US, "%.2f", tps);
                var statusEmoji = (tps >= 18.0) ? "🟢 Stabilny" : (tps >= 15.0) ? "🟡 Obciążony" : "🔴 Problemy";

                // 3. BUDOWANIE EMBEDA
                var embed = new EmbedBuilder()
                        .setAuthor("Status Serwera Minecraft", Constants.LINK, guild.getIconUrl())
                        .setTitle("`🎮`〢 StrideMC.pl")
                        .setThumbnail(guild.getIconUrl())
                        .setColor(Constants.DEFAULT_COLOR)
                        .setDescription("""
                                > Poniżej znajdziesz aktualne statystyki na żywo.
                                > Dołącz do nas i twórz z nami wspaniałą społeczność!
                                """)
                        .addField("`🌐` Adres IP", "```\nstridemc.pl\n```", false)
                        .addField("`📈` Gracze", "`%d / %d`".formatted(onlineCount, maxPlayers), true)
                        .addField("`⚡` Wersja", "`%s`".formatted(version), true)
                        .addField("`📶` Wydajność (TPS)", "`%s (%s)`".formatted(statusEmoji, formattedTps), true)
                        .addField("`👑` Administracja na serwerze:", staffOnline, false)
                        .setFooter("Zażądano przez: " + event.getUser().getEffectiveName(), event.getUser().getEffectiveAvatarUrl())
                        .setTimestamp(Instant.now());

                // 4. WYSYŁANIE ODPOWIEDZI Z WYKORZYSTANIEM HOOKA
                event.getHook().sendMessageEmbeds(embed.build())
                        .addActionRow(
                                Button.link(Constants.LINK, "🌐 Strona WWW"),
                                Button.link("https://stridemc.pl/mapa", "🗺️ Mapa Serwera") // Zmień URL jeśli macie mapkę WWW, lub usuń tę linijkę
                        )
                        .queue();

            } catch (Exception e) {
                event.getHook().sendMessage("> `❌` Wystąpił błąd podczas komunikacji z serwerem Minecraft!").queue();
                e.printStackTrace();
            }
        });
    }
}