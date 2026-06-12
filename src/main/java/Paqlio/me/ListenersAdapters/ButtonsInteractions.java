package Paqlio.me.ListenersAdapters;

import Paqlio.me.BOT;
import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ButtonsInteractions extends ListenerAdapter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Warsaw"));

    // Pobieramy ID z configu (najlepiej dodaj "bot.log-channel: '1388505635251028070'" do config.yml)
    // Jeśli nie znajdzie, użyje Twojego domyślnego.
    private final String logChannelId = BOT.getInstance().getConfig().getString("bot.log-channel", "1388505635251028070");

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) return;

        switch (event.getComponentId()) {
            case "off" -> {
                var eb = new EmbedBuilder()
                        .setTitle("`📨`〢 TICKET")
                        .setDescription("""
                                `⚠️` **Czy na pewno chcesz usunąć ticket?**
                                Po usunięciu nie będzie możliwości jego odzyskania.
                                """)
                        .setAuthor("StrideMC - Ticket System", Constants.LINK, guild.getIconUrl())
                        .setColor(Color.RED)
                        .setFooter(guild.getName(), guild.getIconUrl());

                event.replyEmbeds(eb.build())
                        .addActionRow(Button.danger("off1", "⚠️ | Usuń Ticket"))
                        .queue();
            }

            case "off1" -> {
                var deleter = event.getUser();
                var channel = (TextChannel) event.getChannel();

                // Odpowiadamy od razu i wyłapujemy wiadomość do edycji
                event.replyEmbeds(new EmbedBuilder()
                                .setTitle("`📨`〢 TICKET")
                                .setDescription("Ticket zostanie usunięty za `⌚` **5** sekund!")
                                .setColor(Constants.DEFAULT_COLOR)
                                .build())
                        .queue(hook -> hook.retrieveOriginal().queue(message ->
                                startCountdown(message, channel, deleter)
                        ));
            }
        }
    }

    private void startCountdown(Message message, TextChannel channel, User deleter) {
        var secondsLeft = new AtomicInteger(5);

        // Używamy Asynchronicznego Schedulera Bukkita - idealnie współgra z Minecraftem!
        Bukkit.getScheduler().runTaskTimerAsynchronously(BOT.getInstance(), task -> {
            int current = secondsLeft.decrementAndGet();

            if (current > 0) {
                // Aktualizowanie wiadomości
                var eb = new EmbedBuilder()
                        .setTitle("`📨`〢 TICKET")
                        .setDescription("Ticket zostanie usunięty za `⌚` **" + current + "** sekund!")
                        .setColor(Constants.DEFAULT_COLOR)
                        .setThumbnail(message.getGuild().getIconUrl())
                        .build();

                message.editMessageEmbeds(eb).queue(null, err -> {});
            } else {
                // Gdy stoper dobije do 0, anulujemy taska w bezpieczny sposób i archiwizujemy
                task.cancel();
                archiveAndDelete(channel, deleter);
            }
        }, 20L, 20L); // 20 ticków = 1 sekunda opóźnienia, co 1 sekundę
    }

    private void archiveAndDelete(TextChannel channel, User deleter) {
        channel.getHistory().retrievePast(100).queue(messages -> {
            Collections.reverse(messages);
            var info = extractTicketInfo(messages);

            try {
                // Generowanie pliku tymczasowego
                var logFile = File.createTempFile("ticket-archive-", ".txt");

                // Usunięcie pliku w momencie wyłączenia maszyny (Gdyby coś zacięło się po drodze)
                logFile.deleteOnExit();

                var content = new StringBuilder();
                content.append("ARCHIWUM TICKETU: ").append(channel.getName()).append("\n")
                        .append("ZAMKNIĘTY PRZEZ: ").append(deleter.getName()).append(" (ID: ").append(deleter.getId()).append(")\n")
                        .append("-".repeat(50)).append("\n\n");

                for (var msg : messages) {
                    var time = formatter.format(msg.getTimeCreated());
                    content.append(String.format("[%s] %s: %s\n", time, msg.getAuthor().getName(), msg.getContentDisplay()));
                }

                Files.writeString(logFile.toPath(), content.toString());

                // Wysyłanie logów na kanał
                sendLogToChannel(channel, logFile, deleter, info, messages.size());

            } catch (IOException e) {
                BOT.getInstance().getLogger().warning("Nie udało się stworzyć archiwum dla: " + channel.getName());
                channel.delete().queue();
            }
        }, error -> channel.delete().queue());
    }

    private void sendLogToChannel(TextChannel ticketChannel, File logFile, User deleter, TicketInfo info, int msgCount) {
        var logChannel = ticketChannel.getGuild().getTextChannelById(logChannelId);

        if (logChannel == null) {
            BOT.getInstance().getLogger().warning("Nie znaleziono kanału logów o ID: " + logChannelId);
            ticketChannel.delete().queue();
            logFile.delete();
            return;
        }

        var eb = new EmbedBuilder()
                .setTitle("`📑`〢 Ticket Zarchiwizowany")
                .setColor(Constants.DEFAULT_COLOR)
                .setThumbnail(ticketChannel.getGuild().getIconUrl())
                .addField("> `📩` Kanał:", "```" + ticketChannel.getName() + "```", true)
                .addField("> `💬` Wiadomości:", "```" + msgCount + "```", true)
                .addField("> `🗑️` Zamknął(a):", deleter.getAsMention(), false);

        if (info.temat() != null) {
            eb.addField("> `📋` Temat / Powód:", "```" + info.temat() + "```", false);
        }

        eb.setFooter("ID Kanału: " + ticketChannel.getId()).setTimestamp(java.time.Instant.now());

        // Kluczowe: Najpierw wysyłamy logi, potem usuwamy kanał (dodałem czyszczenie pliku w finally!)
        logChannel.sendMessageEmbeds(eb.build())
                .addFiles(FileUpload.fromData(logFile, "transcript-" + ticketChannel.getName() + ".txt"))
                .queue(
                        success -> finishDeletion(ticketChannel, logFile),
                        error -> finishDeletion(ticketChannel, logFile)
                );
    }

    private void finishDeletion(TextChannel channel, File file) {
        channel.delete().queue(null, err -> {});
        if (file.exists()) {
            file.delete();
        }
    }

    private TicketInfo extractTicketInfo(List<Message> messages) {
        for (var m : messages) {
            if (!m.getEmbeds().isEmpty()) {
                var desc = m.getEmbeds().get(0).getDescription();
                if (desc != null && desc.contains("TICKET")) {
                    return new TicketInfo(parseField(desc, "Temat:"), parseField(desc, "Treść:"));
                }
            }
        }
        return new TicketInfo("Brak danych", "Brak danych");
    }

    private String parseField(String input, String field) {
        try {
            if (!input.contains(field)) return null;
            return input.split(field)[1].split("```")[1].trim();
        } catch (Exception e) {
            return null;
        }
    }

    private record TicketInfo(String temat, String tresc) {}
}