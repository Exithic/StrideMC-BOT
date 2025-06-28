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
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ButtonsInteractions extends ListenerAdapter {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Warsaw"));
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        var guild = event.getGuild();
        switch (event.getComponentId()) {
            case "off" -> event.replyEmbeds(createEmbed(
                            "`📨`〢TICKET",
                            "`⚠️`Czy na pewno chcesz usunąć ticket?\n" +
                                    "Po usunięciu nie będzie możliwości jego odzyskania.\n" +
                                    "Aby kontynuować, kliknij przycisk poniżej. 👋")
                            .setAuthor("StrideMC - Ticket System", Constants.link, guild.getIconUrl())
                            .setColor(Color.RED)
                            .setFooter(guild.getName(), guild.getIconUrl())
                            .setThumbnail(guild.getIconUrl())
                            .build())
                    .addActionRow(Button.danger("off1", "⚠️ | Usuń Ticket"))
                    .queue();

            case "off1" -> {
                User deleter = event.getUser();
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
        String channelTopic = ticketChannel.getTopic();
        if (channelTopic != null && channelTopic.contains("Opened by:")) {
            try {
                String openerId = channelTopic.split("Opened by:")[1].trim();
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

    private void archiveAndDelete(TextChannel ticketChannel, User deleter) {
        ticketChannel.getHistory().retrievePast(100).queue(messages -> {
                    Collections.reverse(messages);

                    var logContent = new StringBuilder();
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
                        sendToLogChannelAndCleanup(ticketChannel, logFile);

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

    private void sendToLogChannelAndCleanup(TextChannel ticketChannel, File logFile) {
        var logChannel = ticketChannel.getGuild().getTextChannelById(1388505635251028070L);
        if (logChannel == null) {
            System.err.println("Nie znaleziono kanału o ID 1388505635251028070 - upewnij się, że jest poprawny i bot ma do niego dostęp. Usuwam kanał ticketa.");
            ticketChannel.delete().queue(null, error -> {
                System.err.println("Nie udało się usunąć kanału ticketu " + ticketChannel.getName() + " po nie znalezieniu kanału logów: " + error.getMessage());
            });
            if (logFile != null && logFile.exists()) {
                logFile.delete();
            }
            return;
        }

        if (logFile.exists() && logFile.length() > 0) {
            var ref = new Object() {
                net.dv8tion.jda.api.utils.FileUpload logChannelFileUpload = null;
            };
            try {
                ref.logChannelFileUpload = net.dv8tion.jda.api.utils.FileUpload.fromData(new FileInputStream(logFile), logFile.getName());
                logChannel.sendMessage("📁 Log rozmowy z ticketa: `" + ticketChannel.getName() + "`")
                        .addFiles(ref.logChannelFileUpload)
                        .queue(success -> {
                            ticketChannel.delete().queue(null, error -> {});
                            if (logFile != null && logFile.exists()) {
                                logFile.delete();
                            }
                            try { if (ref.logChannelFileUpload.getData() != null) ref.logChannelFileUpload.getData().close(); } catch (IOException ignored) {}
                        }, error -> {
                            error.printStackTrace();
                            System.err.println("Nie udało się wysłać pliku logów do kanału logów: " + error.getMessage());
                            ticketChannel.delete().queue(null, err -> {});
                            if (logFile != null && logFile.exists()) {
                                logFile.delete();
                            }
                            try { if (ref.logChannelFileUpload.getData() != null) ref.logChannelFileUpload.getData().close(); } catch (IOException ignored) {}
                        });
            } catch (IOException e) {
                System.err.println("Błąd podczas tworzenia FileUpload dla kanału logów: " + e.getMessage());
                ticketChannel.delete().queue(null, err -> {});
                if (logFile != null && logFile.exists()) {
                    logFile.delete();
                }
            }
        } else {
            ticketChannel.delete().queue(null, err -> {});
            if (logFile != null && logFile.exists()) {
                logFile.delete();
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
}