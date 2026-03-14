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
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ButtonsInteractions extends ListenerAdapter {
    private final BOT plugin;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Warsaw"));

    public ButtonsInteractions(BOT plugin) {
        this.plugin = plugin;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        var guild = event.getGuild();
        if (guild == null) return;

        switch (event.getComponentId()) {
            case "off" -> {
                var embed = createEmbed("`📨`〢TICKET",
                        "`⚠️` Czy na pewno chcesz usunąć ticket?\nPo usunięciu nie będzie możliwości jego odzyskania.")
                        .setColor(Color.RED)
                        .setFooter(guild.getName(), guild.getIconUrl())
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(Button.danger("off1", "⚠️ | Usuń Ticket"))
                        .queue();
            }
            case "off1" -> {
                event.replyEmbeds(createEmbed("`📨`〢TICKET", "Ticket zostanie usunięty za `⌚` **5** sekund!").build())
                        .queue(hook -> hook.retrieveOriginal().queue(msg ->
                                startCountdown(msg, (TextChannel) event.getChannel(), event.getUser())
                        ));
            }
        }
    }

    private void startCountdown(Message message, TextChannel channel, User deleter) {
        var secondsLeft = new AtomicInteger(5);

        var future = scheduler.scheduleAtFixedRate(() -> {
            int current = secondsLeft.getAndDecrement();
            if (current > 0) {
                var embed = createEmbed("`📨`〢TICKET", "Ticket zostanie usunięty za `⌚` **" + current + "** sekund!").build();
                message.editMessageEmbeds(embed).queue(null, throwable -> {});
            } else {
                archiveAndDelete(channel, deleter);
                throw new RuntimeException("Stop Task");
            }
        }, 0, 1, TimeUnit.SECONDS);

        scheduler.schedule(() -> future.cancel(false), 7, TimeUnit.SECONDS);
    }

    private void archiveAndDelete(TextChannel channel, User deleter) {
        channel.getHistory().retrievePast(100).queue(messages -> {
            Collections.reverse(messages);
            var info = extractTicketInfo(messages);
            var logFile = generateLogFile(channel, deleter, messages);

            if (logFile != null) {
                sendLogAndClose(channel, logFile, deleter, info, messages.size());
            } else {
                channel.delete().queue();
            }
        });
    }

    private File generateLogFile(TextChannel channel, User deleter, List<Message> messages) {
        try {
            var tempFile = File.createTempFile("ticket-log-", ".txt");
            var content = new StringBuilder();
            content.append("==================================================\n")
                    .append("TRANSKRYPCJA TICKETU: ").append(channel.getName()).append("\n")
                    .append("ZAMKNIĘTY PRZEZ: ").append(deleter.getName()).append(" (").append(deleter.getId()).append(")\n")
                    .append("==================================================\n\n");

            for (var m : messages) {
                var time = formatter.format(m.getTimeCreated());
                content.append(String.format("[%s] %s: %s\n", time, m.getAuthor().getName(), m.getContentDisplay()));
            }

            Files.writeString(tempFile.toPath(), content.toString());
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendLogAndClose(TextChannel channel, File file, User deleter, TicketInfo info, int msgCount) {
        // Pobieranie ID z configu
        var logChannelId = plugin.getConfig().getLong("channels.logs");
        var logChannel = channel.getGuild().getTextChannelById(logChannelId);

        if (logChannel == null) {
            channel.delete().queue();
            file.delete();
            return;
        }

        try (var is = Files.newInputStream(file.toPath())) {
            var embed = new EmbedBuilder()
                    .setTitle("`📝` | Ticket Zamknięty")
                    .setColor(Constants.defaultcolor)
                    .addField("> `\uD83D\uDD90` Ticket:", "```" + channel.getName() + "```", false)
                    .addField("> `🗑️` Zamknięte przez:", deleter.getAsMention(), false)
                    .addField("> `💬` Liczba wiadomości:", "```" + msgCount + "```", true)
                    .setThumbnail(channel.getGuild().getIconUrl())
                    .setFooter(channel.getGuild().getName(), channel.getGuild().getIconUrl());

            if (info.temat() != null) embed.addField("> `📋` Temat:", "```" + info.temat() + "```", false);

            logChannel.sendMessageEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(is, "transcript-" + channel.getName() + ".txt"))
                    .queue(success -> {
                        channel.delete().queue();
                        file.delete();
                    }, error -> {
                        channel.delete().queue();
                        file.delete();
                    });
        } catch (IOException e) {
            channel.delete().queue();
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
        return new TicketInfo(null, null);
    }

    private String parseField(String input, String field) {
        try {
            if (!input.contains(field)) return null;
            var part = input.split(field)[1];
            return part.split("```")[1].trim();
        } catch (Exception e) {
            return null;
        }
    }

    private EmbedBuilder createEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Constants.defaultcolor)
                .setAuthor("StrideMC - Ticket System", Constants.link);
    }

    // Rekord Javy 17 - czysty kontener na dane
    private record TicketInfo(String temat, String tresc) {}
}