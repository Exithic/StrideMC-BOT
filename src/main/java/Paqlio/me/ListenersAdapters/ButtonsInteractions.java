package Paqlio.me.ListenersAdapters;

import Paqlio.me.Configurations.Constants;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ButtonsInteractions extends ListenerAdapter {
    private static final long LOG_CHANNEL_ID = 1388505635251028070L;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Warsaw"));

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) return;

        switch (event.getComponentId()) {
            case "off" -> event.replyEmbeds(createEmbed(
                            "`📨`〢TICKET",
                            """
                                    `⚠️` Czy na pewno chcesz usunąć ticket?
                                    Po usunięciu nie będzie możliwości jego odzyskania.
                                    Aby kontynuować, kliknij przycisk poniżej. 👋""")
                            .setAuthor("StrideMC - Ticket System", Constants.link, guild.getIconUrl())
                            .setColor(Color.RED)
                            .setFooter(guild.getName(), guild.getIconUrl())
                            .setThumbnail(guild.getIconUrl())
                            .build())
                    .addActionRow(Button.danger("off1", "⚠️ | Usuń Ticket"))
                    .queue();

            case "off1" -> {
                var deleter = event.getUser();
                event.replyEmbeds(createEmbed("`📨`〢TICKET",
                                "Ticket zostanie usunięty za `⌚` **10** sekund!")
                                .setThumbnail(guild.getIconUrl()).build())
                        .queue(response ->
                                response.retrieveOriginal().queue(message ->
                                        startCountdown(message, (TextChannel) event.getChannel(), deleter)
                                )
                        );
            }
        }
    }

    private User getTicketOpenerFromChannel(TextChannel ticketChannel) {
        var channelTopic = ticketChannel.getTopic();
        if (channelTopic != null && channelTopic.contains("Opened by:")) {
            try {
                var openerId = channelTopic.split("Opened by:")[1].trim();
                return ticketChannel.getJDA().retrieveUserById(openerId).complete();
            } catch (Exception e) {
                System.err.println("Błąd podczas parsowania ID otwierającego z tematu kanału " + ticketChannel.getName() + ": " + e.getMessage());
                return null;
            }
        }
        System.err.println("Nie znaleziono ID otwierającego w temacie kanału " + ticketChannel.getName() + ". Upewnij się, że format tematu jest poprawny (np. 'Opened by: [ID_UŻYTKOWNIKA]').");
        return null;
    }

    private void startCountdown(Message message, TextChannel ticketChannel, User deleter) {
        final Runnable[] runnable = new Runnable[1];
        ScheduledFuture<?>[] future = new ScheduledFuture[1];

        runnable[0] = new Runnable() {
            int secondsLeft = 10;

            @Override
            public void run() {
                var guild = message.getGuild();
                if (secondsLeft > 0) {
                    var embed = createEmbed("`📨`〢TICKET",
                            "Ticket zostanie usunięty za `⌚` **" + secondsLeft + "** sekund!")
                            .setThumbnail(guild.getIconUrl())
                            .build();
                    message.editMessageEmbeds(embed).queue(null, error -> {
                        if (error instanceof ErrorResponseException && ((ErrorResponseException) error).getErrorCode() == 10008) {
                            System.err.println("Wiadomość odliczająca została usunięta. Zatrzymuję odliczanie dla ticketu " + ticketChannel.getName());
                            if (future[0] != null) future[0].cancel(false);
                        }
                    });
                    secondsLeft--;
                } else {
                    archiveAndDelete(ticketChannel, deleter);
                    if (future[0] != null) future[0].cancel(false);
                }
            }
        };
        future[0] = scheduler.scheduleAtFixedRate(runnable[0], 0, 1, TimeUnit.SECONDS);
    }

    private TicketInfo extractTicketInfo(List<Message> messages) {
        var info = new TicketInfo();

        for (var msg : messages) {
            if (!msg.getEmbeds().isEmpty()) {
                var embed = msg.getEmbeds().get(0);
                var desc = embed.getDescription();

                if (desc != null && desc.contains("TICKET")) {
                    try {
                        if (desc.contains("Temat:")) {
                            var afterTemat = desc.split("Temat:")[1];
                            var startIdx = afterTemat.indexOf("```");
                            if (startIdx != -1) {
                                var afterFirstTick = afterTemat.substring(startIdx + 3);
                                var endIdx = afterFirstTick.indexOf("```");
                                if (endIdx != -1) {
                                    info.temat = afterFirstTick.substring(0, endIdx).trim();
                                }
                            }
                        }

                        if (desc.contains("Treść:")) {
                            var afterTresc = desc.split("Treść:")[1];
                            var startIdx = afterTresc.indexOf("```");
                            if (startIdx != -1) {
                                var afterFirstTick = afterTresc.substring(startIdx + 3);
                                var endIdx = afterFirstTick.indexOf("```");
                                if (endIdx != -1) {
                                    info.tresc = afterFirstTick.substring(0, endIdx).trim();
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Błąd parsowania embeda ticketu: " + e.getMessage());
                    }

                    if (info.temat != null || info.tresc != null) {
                        break;
                    }
                }
            }
        }

        return info;
    }

    private void archiveAndDelete(TextChannel ticketChannel, User deleter) {
        ticketChannel.getHistory().retrievePast(100).queue(messages -> {
                    Collections.reverse(messages);

                    var ticketInfo = extractTicketInfo(messages);
                    var messageCount = messages.size();

                    var logContent = new StringBuilder();

                    logContent.append("=".repeat(50)).append("\n");
                    logContent.append("INFORMACJE O TICKECIE\n");
                    logContent.append("=".repeat(50)).append("\n");
                    logContent.append("Kanał: ").append(ticketChannel.getName()).append("\n");
                    logContent.append("ID Kanału: ").append(ticketChannel.getId()).append("\n");
                    if (ticketInfo.temat != null) {
                        logContent.append("Temat: ").append(ticketInfo.temat).append("\n");
                    }
                    if (ticketInfo.tresc != null) {
                        logContent.append("Treść: ").append(ticketInfo.tresc).append("\n");
                    }
                    logContent.append("Liczba wiadomości: ").append(messageCount).append("\n");
                    logContent.append("Zamknięty przez: ").append(deleter.getName()).append(" (").append(deleter.getId()).append(")\n");
                    logContent.append("=".repeat(50)).append("\n");
                    logContent.append("HISTORIA KONWERSACJI\n");
                    logContent.append("=".repeat(50)).append("\n\n");

                    for (var msg : messages) {
                        logContent.append("[").append(formatter.format(msg.getTimeCreated().toInstant())).append("] ")
                                .append(msg.getAuthor().getName()).append(": ")
                                .append(msg.getContentDisplay()).append("\n");
                    }

                    File logFile = null;
                    try {
                        logFile = File.createTempFile("ticket-", ".txt");
                        try (var writer = new FileWriter(logFile)) {
                            writer.write(logContent.toString());
                        }

                        if (!logFile.exists() || !logFile.canRead()) {
                            System.err.println("Utworzony plik logów nie istnieje lub nie można go odczytać: " + logFile.getAbsolutePath());
                            ticketChannel.delete().queue(null, err -> {});
                            if (logFile.exists()) logFile.delete();
                            return;
                        }

                        sendToLogChannelAndCleanup(ticketChannel, logFile, deleter, ticketInfo, messageCount);

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Krytyczny błąd podczas tworzenia pliku logów: " + e.getMessage());
                        ticketChannel.delete().queue(null, err -> {});
                        if (logFile != null && logFile.exists()) {
                            logFile.delete();
                        }
                    }
                },
                error -> {
                    if (error instanceof ErrorResponseException && ((ErrorResponseException) error).getErrorCode() == 10003) {
                        System.err.println("Kanał ticketu " + ticketChannel.getId() + " został już usunięty, zanim archiwizacja mogła się zakończyć.");
                    } else {
                        error.printStackTrace();
                    }
                    ticketChannel.delete().queue(null, deleteError -> {
                        System.err.println("Nie udało się usunąć kanału ticketu " + ticketChannel.getName() + " po niepowodzeniu pobierania historii: " + deleteError.getMessage());
                    });
                });
    }

    private void sendToLogChannelAndCleanup(TextChannel ticketChannel, File logFile, User deleter, TicketInfo ticketInfo, int messageCount) {
        var logChannel = ticketChannel.getGuild().getTextChannelById(LOG_CHANNEL_ID);
        if (logChannel == null) {
            System.err.println("Nie znaleziono kanału o ID " + LOG_CHANNEL_ID + " - upewnij się, że jest poprawny i bot ma do niego dostęp. Usuwam kanał ticketa.");
            ticketChannel.delete().queue(null, error -> {
                System.err.println("Nie udało się usunąć kanału ticketu " + ticketChannel.getName() + " po nie znalezieniu kanału logów: " + error.getMessage());
            });
            if (logFile != null && logFile.exists()) {
                logFile.delete();
            }
            return;
        }

        if (!logFile.exists() || logFile.length() == 0) {
            ticketChannel.delete().queue(null, err -> {});
            if (logFile.exists()) {
                logFile.delete();
            }
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(logFile);
            var fileUpload = FileUpload.fromData(fis, logFile.getName());

            var embedBuilder = new EmbedBuilder()
                    .setTitle("📝 | Ticket Zamknięty")
                    .setDescription("**Ticket:** `" + ticketChannel.getName() + "`")
                    .addField("Closed by", deleter.getAsMention(), false);

            if (ticketInfo.temat != null) {
                embedBuilder.addField("📋 Temat", "```" +ticketInfo.temat + "```", false);
            }
            if (ticketInfo.tresc != null) {
                var tresc = ticketInfo.tresc;
                if (tresc.length() > 1024) {
                    tresc = tresc.substring(0, 1021) + "...";
                }
                embedBuilder.addField("📨 Treść","```" + tresc + "```", false)
                        .addField("Channel ID", ticketChannel.getId(), true);
            }

            embedBuilder.addField("💬 Liczba wiadomości", String.valueOf(messageCount), true);
            embedBuilder.setColor(Constants.defaultcolor)
                    .setFooter(Constants.name, Constants.img);

            var finalFis = fis;
            logChannel.sendMessageEmbeds(embedBuilder.build())
                    .addFiles(fileUpload)
                    .queue(success -> {
                        ticketChannel.delete().queue(null, error -> {});
                        cleanupFile(logFile);
                        closeStream(finalFis);
                    }, error -> {
                        error.printStackTrace();
                        System.err.println("Nie udało się wysłać pliku logów do kanału logów: " + error.getMessage());
                        ticketChannel.delete().queue(null, err -> {});
                        cleanupFile(logFile);
                        closeStream(finalFis);
                    });
        } catch (IOException e) {
            System.err.println("Błąd podczas tworzenia FileUpload dla kanału logów: " + e.getMessage());
            ticketChannel.delete().queue(null, err -> {});
            cleanupFile(logFile);
            closeStream(fis);
        }
    }

    private void cleanupFile(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                System.err.println("Nie udało się usunąć pliku tymczasowego: " + file.getAbsolutePath());
            }
        }
    }

    private void closeStream(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                System.err.println("Nie udało się zamknąć strumienia: " + e.getMessage());
            }
        }
    }

    private EmbedBuilder createEmbed(String title, String... fields) {
        var embed = new EmbedBuilder()
                .setColor(Constants.defaultcolor)
                .setAuthor(Constants.name, Constants.link);
        if (title != null) embed.setTitle(title);
        for (var field : fields) embed.addField("", field, false);
        return embed;
    }

    private static class TicketInfo {
        String temat;
        String tresc;
    }
}